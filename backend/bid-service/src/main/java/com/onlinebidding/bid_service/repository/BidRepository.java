package com.onlinebidding.bid_service.repository;

import com.onlinebidding.bid_service.entity.Bid;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BidRepository extends JpaRepository<Bid, Long> {
    List<Bid> findByAuctionIdOrderByAmountDesc(Long auctionId);
    Optional<Bid> findFirstByAuctionIdOrderByAmountDesc(Long auctionId);
    List<Bid> findByBidderIdOrderByBidTimeDesc(Long bidderId);
}
