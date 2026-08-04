package com.onlinebidding.order_service.controller;

import com.onlinebidding.order_service.dto.OrderDto;
import com.onlinebidding.order_service.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;
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
    public ResponseEntity<OrderDto> createOrder(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestHeader(value = "X-User-Role", required = false) String role,
            @RequestBody OrderDto orderDto) {
        requireCreateAccess(userId, role, orderDto);
        return new ResponseEntity<>(orderService.createOrder(orderDto), HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderDto> getOrderById(
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader,
            @RequestHeader(value = "X-User-Role", required = false) String role,
            @PathVariable("id") String id) {
        Long numericId = parseOrderId(id);
        OrderDto order = orderService.getOrderById(numericId);
        requireViewAccess(userIdHeader, role, order);
        return ResponseEntity.ok(order);
    }

    @GetMapping("/buyer/{buyerId}")
    public ResponseEntity<List<OrderDto>> getOrdersByBuyer(
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader,
            @RequestHeader(value = "X-User-Role", required = false) String role,
            @PathVariable("buyerId") Long buyerId) {
        requireSameUserOrAdmin(userIdHeader, role, buyerId);
        return ResponseEntity.ok(orderService.getOrdersByBuyer(buyerId));
    }

    @GetMapping("/seller/{sellerId}")
    public ResponseEntity<List<OrderDto>> getOrdersBySeller(
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader,
            @RequestHeader(value = "X-User-Role", required = false) String role,
            @PathVariable("sellerId") Long sellerId) {
        requireSameUserOrAdmin(userIdHeader, role, sellerId);
        return ResponseEntity.ok(orderService.getOrdersBySeller(sellerId));
    }

    @GetMapping("/delivery/{deliveryPersonId}")
    public ResponseEntity<List<OrderDto>> getOrdersByDeliveryPerson(
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader,
            @RequestHeader(value = "X-User-Role", required = false) String role,
            @PathVariable("deliveryPersonId") Long deliveryPersonId) {
        requireSameUserOrAdmin(userIdHeader, role, deliveryPersonId);
        return ResponseEntity.ok(orderService.getOrdersByDeliveryPerson(deliveryPersonId));
    }

    @GetMapping
    public ResponseEntity<List<OrderDto>> getAllOrders(
            @RequestHeader(value = "X-User-Role", required = false) String role) {
        requireAdmin(role);
        return ResponseEntity.ok(orderService.getAllOrders());
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<OrderDto> updateOrderStatus(
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader,
            @RequestHeader(value = "X-User-Role", required = false) String role,
            @PathVariable("id") String id,
            @RequestBody Map<String, String> payload) {
        Long numericId = parseOrderId(id);
        OrderDto existingOrder = orderService.getOrderById(numericId);
        if (!isAdmin(role) && !("DELIVERY".equalsIgnoreCase(role)
                && sameUser(userIdHeader, existingOrder.getDeliveryPersonId()))) {
            throw forbidden("Only the assigned delivery partner or an administrator can update order status.");
        }
        String status = payload.get("status");
        return ResponseEntity.ok(orderService.updateOrderStatus(numericId, status));
    }

    @PostMapping("/{id}/assign-delivery")
    public ResponseEntity<OrderDto> assignDeliveryPerson(
            @RequestHeader(value = "Authorization", required = false) String token,
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader,
            @RequestHeader(value = "X-User-Role", required = false) String role,
            @PathVariable("id") String id,
            @RequestBody Map<String, Long> payload) {
        Long numericId = parseOrderId(id);
        OrderDto existingOrder = orderService.getOrderById(numericId);
        if (!isAdmin(role) && !("SELLER".equalsIgnoreCase(role)
                && sameUser(userIdHeader, existingOrder.getSellerId()))) {
            throw forbidden("Only the seller or an administrator can assign delivery.");
        }
        Long deliveryPersonId = payload.get("deliveryPersonId");
        return ResponseEntity.ok(orderService.assignDeliveryPerson(numericId, deliveryPersonId, token));
    }

    private void requireCreateAccess(Long userId, String role, OrderDto order) {
        boolean buyerCreatingOwnOrder = "BUYER".equalsIgnoreCase(role)
                && userId != null
                && userId.equals(order.getBuyerId());
        boolean sellerCreatingOwnOrder = "SELLER".equalsIgnoreCase(role)
                && userId != null
                && userId.equals(order.getSellerId());
        if (!isAdmin(role) && !buyerCreatingOwnOrder && !sellerCreatingOwnOrder) {
            throw forbidden("You are not allowed to create this order.");
        }
    }

    private void requireViewAccess(String userIdHeader, String role, OrderDto order) {
        if (!isAdmin(role)
                && !sameUser(userIdHeader, order.getBuyerId())
                && !sameUser(userIdHeader, order.getSellerId())
                && !sameUser(userIdHeader, order.getDeliveryPersonId())) {
            throw forbidden("You are not allowed to view this order.");
        }
    }

    private void requireSameUserOrAdmin(String userIdHeader, String role, Long targetUserId) {
        if (!isAdmin(role) && !sameUser(userIdHeader, targetUserId)) {
            throw forbidden("You are not allowed to access another user's orders.");
        }
    }

    private void requireAdmin(String role) {
        if (!isAdmin(role)) {
            throw forbidden("Administrator access is required.");
        }
    }

    private boolean isAdmin(String role) {
        return "ADMIN".equalsIgnoreCase(role);
    }

    private boolean sameUser(String userIdHeader, Long targetUserId) {
        try {
            return userIdHeader != null && targetUserId != null
                    && Long.valueOf(userIdHeader).equals(targetUserId);
        } catch (NumberFormatException exception) {
            return false;
        }
    }

    private ResponseStatusException forbidden(String message) {
        return new ResponseStatusException(HttpStatus.FORBIDDEN, message);
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
