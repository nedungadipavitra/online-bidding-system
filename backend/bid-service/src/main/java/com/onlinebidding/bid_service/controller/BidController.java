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
import java.util.Arrays;
import java.util.stream.Collectors;

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
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader,
            @RequestBody BidDto bidDto) {
        if (role == null || !"BUYER".equalsIgnoreCase(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Access denied. Only BUYER can place bids."));
        }
        Long authenticatedUserId = parseUserId(userIdHeader);
        if (authenticatedUserId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Authenticated user identity is required."));
        }
        if (bidDto.getBidderId() != null && !authenticatedUserId.equals(bidDto.getBidderId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "A bid can only be placed for the authenticated user."));
        }
        bidDto.setBidderId(authenticatedUserId);
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
    public ResponseEntity<List<BidDto>> getBidsForAuction(
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader,
            @PathVariable("auctionId") Long auctionId) {
        if (parseUserId(userIdHeader) == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(bidService.getBidsForAuction(auctionId));
    }

    @GetMapping("/auction/{auctionId}/highest")
    public ResponseEntity<?> getHighestBidForAuction(
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader,
            @PathVariable("auctionId") Long auctionId) {
        if (parseUserId(userIdHeader) == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return bidService.getHighestBidForAuction(auctionId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/highest")
    public ResponseEntity<Map<Long, BidDto>> getHighestBidsForAuctions(
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader,
            @RequestParam("auctionIds") String auctionIds) {
        if (parseUserId(userIdHeader) == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            List<Long> ids = Arrays.stream(auctionIds.split(","))
                    .map(String::trim)
                    .filter(value -> !value.isEmpty())
                    .map(Long::valueOf)
                    .distinct()
                    .collect(Collectors.toList());
            return ResponseEntity.ok(bidService.getHighestBidsForAuctions(ids));
        } catch (NumberFormatException exception) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<BidDto>> getBidsByUser(
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader,
            @RequestHeader(value = "X-User-Role", required = false) String role,
            @PathVariable("userId") Long userId) {
        Long authenticatedUserId = parseUserId(userIdHeader);
        if (authenticatedUserId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        if (!isAdmin(role) && !authenticatedUserId.equals(userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(bidService.getBidsByUser(userId));
    }

    @GetMapping
    public ResponseEntity<List<BidDto>> getAllBids(
            @RequestHeader(value = "X-User-Role", required = false) String role) {
        if (!isAdmin(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(bidService.getAllBids());
    }

    private boolean isAdmin(String role) {
        return "ADMIN".equalsIgnoreCase(role);
    }

    private Long parseUserId(String userIdHeader) {
        try {
            return userIdHeader == null ? null : Long.valueOf(userIdHeader);
        } catch (NumberFormatException exception) {
            return null;
        }
    }
}
