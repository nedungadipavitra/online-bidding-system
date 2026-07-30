package com.onlinebidding.bid_service.service;

import com.onlinebidding.bid_service.dto.BidDto;
import java.util.List;
import java.util.Optional;

public interface BidService {
    BidDto placeBid(BidDto bidDto);
    List<BidDto> getBidsForAuction(Long auctionId);
    Optional<BidDto> getHighestBidForAuction(Long auctionId);
    List<BidDto> getBidsByUser(Long bidderId);
    List<BidDto> getAllBids();
}
