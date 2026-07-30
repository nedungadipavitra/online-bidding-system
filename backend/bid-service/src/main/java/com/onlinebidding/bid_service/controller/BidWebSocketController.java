package com.onlinebidding.bid_service.controller;

import com.onlinebidding.bid_service.dto.BidDto;
import com.onlinebidding.bid_service.service.BidService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;

import java.util.Map;

@Controller
public class BidWebSocketController {

    private final BidService bidService;
    private final SimpMessagingTemplate messagingTemplate;

    @Autowired
    public BidWebSocketController(BidService bidService, SimpMessagingTemplate messagingTemplate) {
        this.bidService = bidService;
        this.messagingTemplate = messagingTemplate;
    }

    @MessageMapping("/place-bid")
    public void placeBid(BidDto bidDto) {
        // Place the bid
        BidDto placedBid = bidService.placeBid(bidDto);
        // Broadcast to all clients subscribed to this auction
        messagingTemplate.convertAndSend("/topic/auction/" + placedBid.getAuctionId(), placedBid);
    }

    @MessageExceptionHandler
    @SendToUser("/queue/errors")
    public Map<String, String> handleException(Throwable exception) {
        return Map.of("error", exception.getMessage());
    }
}
