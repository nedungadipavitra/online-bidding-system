package com.onlinebidding.bid_service.service.impl;

import com.onlinebidding.bid_service.dto.BidDto;
import com.onlinebidding.bid_service.entity.Bid;
import com.onlinebidding.bid_service.repository.BidRepository;
import com.onlinebidding.bid_service.service.BidService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class BidServiceImpl implements BidService {

    private final BidRepository bidRepository;

    @Autowired
    public BidServiceImpl(BidRepository bidRepository) {
        this.bidRepository = bidRepository;
    }

    @Override
    public BidDto placeBid(BidDto bidDto) {
        if (bidDto.getAmount() == null && bidDto.getBidAmount() != null) {
            bidDto.setAmount(bidDto.getBidAmount());
        }

        if (bidDto.getAuctionId() == null || bidDto.getBidderId() == null || bidDto.getAmount() == null) {
            throw new IllegalArgumentException("Auction ID, Bidder ID, and Bid Amount are required");
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

        Bid bid = Bid.builder()
                .auctionId(bidDto.getAuctionId())
                .bidderId(bidDto.getBidderId())
                .bidderName(bidDto.getBidderName() != null ? bidDto.getBidderName() : "Anonymous")
                .amount(bidDto.getAmount())
                .bidTime(bidDto.getBidTime() != null ? bidDto.getBidTime() : LocalDateTime.now())
                .build();

        Bid savedBid = bidRepository.save(bid);
        return convertToDto(savedBid);
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
