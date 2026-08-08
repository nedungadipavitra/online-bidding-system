package com.onlinebidding.bid_service.service.impl;

import com.onlinebidding.bid_service.dto.BidDto;
import com.onlinebidding.bid_service.dto.ProductSnapshot;
import com.onlinebidding.bid_service.entity.Bid;
import com.onlinebidding.bid_service.repository.BidRepository;
import com.onlinebidding.bid_service.service.BidService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.LinkedHashMap;
import java.util.stream.Collectors;

@Service
@Transactional
public class BidServiceImpl implements BidService {

    /**
     * Auction start/end are LocalDateTime wall-clock values from India datetime-local forms.
     * Compare against Asia/Kolkata explicitly — do not rely on container/JVM default TZ.
     */
    private static final ZoneId AUCTION_ZONE = ZoneId.of("Asia/Kolkata");

    private final BidRepository bidRepository;
    private final RestTemplate restTemplate;
    private final String productServiceUrl;
    private final String walletServiceUrl;
    private final String walletInternalToken;

    @Autowired
    public BidServiceImpl(
            BidRepository bidRepository,
            RestTemplate restTemplate,
            @Value("${product.service-url:http://localhost:8082}") String productServiceUrl,
            @Value("${wallet.service-url:http://localhost:8083}") String walletServiceUrl,
            @Value("${wallet.internal-token}") String walletInternalToken) {
        this.bidRepository = bidRepository;
        this.restTemplate = restTemplate;
        this.productServiceUrl = productServiceUrl;
        this.walletServiceUrl = walletServiceUrl;
        this.walletInternalToken = walletInternalToken;
    }

    @Override
    public BidDto placeBid(BidDto bidDto) {
        if (bidDto.getAmount() == null && bidDto.getBidAmount() != null) {
            bidDto.setAmount(bidDto.getBidAmount());
        }

        if (bidDto.getAuctionId() == null || bidDto.getBidderId() == null || bidDto.getAmount() == null) {
            throw new IllegalArgumentException("Auction ID, Bidder ID, and Bid Amount are required");
        }

        ProductSnapshot product = getProduct(bidDto.getAuctionId());
        if (product.getSellerId() == null) {
            throw new IllegalArgumentException("Auction seller is not configured");
        }
        if (bidDto.getBidderId().equals(product.getSellerId())) {
            throw new IllegalArgumentException("A seller cannot bid on their own auction");
        }
        if (!"ACTIVE".equalsIgnoreCase(product.getStatus())) {
            throw new IllegalArgumentException("This auction is not active");
        }
        LocalDateTime now = LocalDateTime.now(AUCTION_ZONE);
        if (product.getAuctionStartTime() != null && now.isBefore(product.getAuctionStartTime())) {
            throw new IllegalArgumentException("Bidding has not started yet");
        }
        if (product.getAuctionEndTime() != null && now.isAfter(product.getAuctionEndTime())) {
            throw new IllegalArgumentException("This auction has ended");
        }

        // Get current highest bid
        Optional<Bid> highestBidOpt = bidRepository.findFirstByAuctionIdOrderByAmountDesc(bidDto.getAuctionId());
        if (highestBidOpt.isPresent()) {
            Bid highestBid = highestBidOpt.get();
            if (bidDto.getAmount().compareTo(highestBid.getAmount()) <= 0) {
                throw new IllegalArgumentException(
                        "Bid amount must be strictly higher than the current highest bid of " + highestBid.getAmount());
            }
        }

        settleWallets(bidDto, product, highestBidOpt.orElse(null));

        Bid bid = Bid.builder()
                .auctionId(bidDto.getAuctionId())
                .bidderId(bidDto.getBidderId())
                .bidderName(bidDto.getBidderName() != null ? bidDto.getBidderName() : "Anonymous")
                .amount(bidDto.getAmount())
                .bidTime(bidDto.getBidTime() != null ? bidDto.getBidTime() : LocalDateTime.now(AUCTION_ZONE))
                .build();

        Bid savedBid = bidRepository.save(bid);
        return convertToDto(savedBid);
    }

    private ProductSnapshot getProduct(Long auctionId) {
        try {
            ProductSnapshot product = restTemplate.getForObject(
                    productServiceUrl + "/products/" + auctionId,
                    ProductSnapshot.class);
            if (product == null) {
                throw new IllegalArgumentException("Auction not found");
            }
            return product;
        } catch (IllegalArgumentException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalArgumentException("Unable to verify auction details");
        }
    }

    private void settleWallets(BidDto bidDto, ProductSnapshot product, Bid previousBid) {
        Map<String, Object> settlement = new HashMap<>();
        settlement.put("bidderId", bidDto.getBidderId());
        settlement.put("sellerId", product.getSellerId());
        settlement.put("newAmount", bidDto.getAmount());
        if (previousBid != null) {
            settlement.put("previousBidderId", previousBid.getBidderId());
            settlement.put("previousAmount", previousBid.getAmount());
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Internal-Service-Token", walletInternalToken);
        try {
            restTemplate.postForEntity(
                    walletServiceUrl + "/wallets/settle-bid",
                    new HttpEntity<>(settlement, headers),
                    Void.class);
        } catch (Exception exception) {
            throw new IllegalArgumentException("Unable to settle wallet balances for this bid");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<BidDto> getBidsForAuction(Long auctionId) {
        return bidRepository.findByAuctionIdOrderByAmountDesc(auctionId)
                .stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<BidDto> getHighestBidForAuction(Long auctionId) {
        return bidRepository.findFirstByAuctionIdOrderByAmountDesc(auctionId)
                .map(this::convertToDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Long, BidDto> getHighestBidsForAuctions(List<Long> auctionIds) {
        Map<Long, BidDto> highestBids = new LinkedHashMap<>();
        if (auctionIds == null || auctionIds.isEmpty()) {
            return highestBids;
        }

        bidRepository.findByAuctionIdInOrderByAuctionIdAscAmountDesc(auctionIds)
                .forEach(bid -> highestBids.putIfAbsent(bid.getAuctionId(), convertToDto(bid)));
        return highestBids;
    }

    @Override
    @Transactional(readOnly = true)
    public List<BidDto> getBidsByUser(Long bidderId) {
        return bidRepository.findByBidderIdOrderByBidTimeDesc(bidderId)
                .stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<BidDto> getAllBids() {
        return bidRepository.findAll().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    private BidDto convertToDto(Bid bid) {
        return BidDto.builder()
                .id(bid.getId())
                .auctionId(bid.getAuctionId())
                .bidderId(bid.getBidderId())
                .bidderName(bid.getBidderName())
                .amount(bid.getAmount())
                .bidTime(bid.getBidTime())
                .build();
    }
}
