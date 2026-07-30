package com.onlinebidding.order_service.controller;

import com.onlinebidding.order_service.dto.OrderDto;
import com.onlinebidding.order_service.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/orders")
public class OrderController {

    private final OrderService orderService;

    @Autowired
    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public ResponseEntity<OrderDto> createOrder(@RequestBody OrderDto orderDto) {
        return new ResponseEntity<>(orderService.createOrder(orderDto), HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderDto> getOrderById(@PathVariable("id") String id) {
        Long numericId = parseOrderId(id);
        return ResponseEntity.ok(orderService.getOrderById(numericId));
    }

    @GetMapping("/buyer/{buyerId}")
    public ResponseEntity<List<OrderDto>> getOrdersByBuyer(@PathVariable("buyerId") Long buyerId) {
        return ResponseEntity.ok(orderService.getOrdersByBuyer(buyerId));
    }

    @GetMapping("/delivery/{deliveryPersonId}")
    public ResponseEntity<List<OrderDto>> getOrdersByDeliveryPerson(@PathVariable("deliveryPersonId") Long deliveryPersonId) {
        return ResponseEntity.ok(orderService.getOrdersByDeliveryPerson(deliveryPersonId));
    }

    @GetMapping
    public ResponseEntity<List<OrderDto>> getAllOrders() {
        return ResponseEntity.ok(orderService.getAllOrders());
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<OrderDto> updateOrderStatus(@PathVariable("id") String id, @RequestBody Map<String, String> payload) {
        Long numericId = parseOrderId(id);
        String status = payload.get("status");
        return ResponseEntity.ok(orderService.updateOrderStatus(numericId, status));
    }

    @PostMapping("/{id}/assign-delivery")
    public ResponseEntity<OrderDto> assignDeliveryPerson(
            @RequestHeader(value = "Authorization", required = false) String token,
            @PathVariable("id") String id,
            @RequestBody Map<String, Long> payload) {
        Long numericId = parseOrderId(id);
        Long deliveryPersonId = payload.get("deliveryPersonId");
        return ResponseEntity.ok(orderService.assignDeliveryPerson(numericId, deliveryPersonId, token));
    }

    private Long parseOrderId(String id) {
        try {
            if (id.startsWith("ORD-")) {
                return Long.parseLong(id.substring(4)) - 1000;
            }
            return Long.parseLong(id);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid order ID format: " + id);
        }
    }
}
