package com.onlinebidding.bid_service.controller;

import com.onlinebidding.bid_service.dto.BidDto;
import com.onlinebidding.bid_service.service.BidService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/bids")
public class BidController {

    private final BidService bidService;
    private final SimpMessagingTemplate messagingTemplate;

    @Autowired
    public BidController(BidService bidService, SimpMessagingTemplate messagingTemplate) {
        this.bidService = bidService;
        this.messagingTemplate = messagingTemplate;
    }

    @PostMapping
    public ResponseEntity<?> placeBid(
            @RequestHeader(value = "X-User-Role", required = false) String role,
            @RequestBody BidDto bidDto) {
        if (role == null || !"BUYER".equalsIgnoreCase(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Access denied. Only BUYER can place bids."));
        }
        try {
            BidDto placedBid = bidService.placeBid(bidDto);
            // Broadcast the new bid to all subscribers of this auction
            messagingTemplate.convertAndSend("/topic/auction/" + placedBid.getAuctionId(), placedBid);
            return new ResponseEntity<>(placedBid, HttpStatus.CREATED);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/auction/{auctionId}")
    public ResponseEntity<List<BidDto>> getBidsForAuction(@PathVariable("auctionId") Long auctionId) {
        return ResponseEntity.ok(bidService.getBidsForAuction(auctionId));
    }

    @GetMapping("/auction/{auctionId}/highest")
    public ResponseEntity<?> getHighestBidForAuction(@PathVariable("auctionId") Long auctionId) {
        return bidService.getHighestBidForAuction(auctionId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<BidDto>> getBidsByUser(@PathVariable("userId") Long userId) {
        return ResponseEntity.ok(bidService.getBidsByUser(userId));
    }

    @GetMapping
    public ResponseEntity<List<BidDto>> getAllBids() {
        return ResponseEntity.ok(bidService.getAllBids());
    }
}
