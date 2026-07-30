package com.onlinebidding.order_service.service.impl;

import com.onlinebidding.order_service.dto.OrderDto;
import com.onlinebidding.order_service.entity.Order;
import com.onlinebidding.order_service.entity.OrderStatus;
import com.onlinebidding.order_service.exception.ResourceNotFoundException;
import com.onlinebidding.order_service.repository.OrderRepository;
import com.onlinebidding.order_service.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final RestTemplate restTemplate;

    @Autowired
    public OrderServiceImpl(OrderRepository orderRepository, RestTemplate restTemplate) {
        this.orderRepository = orderRepository;
        this.restTemplate = restTemplate;
    }

    @Override
    public OrderDto createOrder(OrderDto orderDto) {
        Order order = new Order();
        order.setProductId(orderDto.getProductId());
        order.setBuyerId(orderDto.getBuyerId());
        order.setSellerId(orderDto.getSellerId());
        order.setDeliveryPersonId(orderDto.getDeliveryPersonId());
        order.setFinalPrice(orderDto.getFinalPrice());
        order.setStatus(orderDto.getStatus() != null ? orderDto.getStatus() : OrderStatus.PENDING);
        order.setCreatedAt(orderDto.getCreatedAt() != null ? orderDto.getCreatedAt() : LocalDateTime.now());

        Order saved = orderRepository.save(order);
        return mapToDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderDto getOrderById(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + id));
        return mapToDto(order);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderDto> getOrdersByBuyer(Long buyerId) {
        return orderRepository.findByBuyerId(buyerId).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderDto> getOrdersByDeliveryPerson(Long deliveryPersonId) {
        return orderRepository.findByDeliveryPersonId(deliveryPersonId).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderDto> getAllOrders() {
        return orderRepository.findAll().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    public OrderDto updateOrderStatus(Long id, String status) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + id));
        
        OrderStatus currentStatus = order.getStatus();
        OrderStatus targetStatus = OrderStatus.valueOf(status.toUpperCase());

        // Validate state transitions
        if (currentStatus == OrderStatus.ASSIGNED && targetStatus != OrderStatus.DISPATCHED) {
            throw new IllegalArgumentException("Invalid status transition: ASSIGNED can only transition to DISPATCHED");
        }
        if (currentStatus == OrderStatus.DISPATCHED && targetStatus != OrderStatus.DELIVERED) {
            throw new IllegalArgumentException("Invalid status transition: DISPATCHED can only transition to DELIVERED");
        }
        if (currentStatus == OrderStatus.DELIVERED) {
            throw new IllegalArgumentException("Invalid status transition: order is already DELIVERED");
        }

        order.setStatus(targetStatus);
        order.setDeliveryStatus(targetStatus.name());
        return mapToDto(orderRepository.save(order));
    }

    @Override
    public OrderDto assignDeliveryPerson(Long id, Long deliveryPersonId, String token) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + id));

        // Fetch delivery person name from User Service
        String deliveryPersonName = "Delivery Partner";
        if (deliveryPersonId != null) {
            try {
                String url = "http://localhost:8081/users/" + deliveryPersonId;
                HttpHeaders headers = new HttpHeaders();
                if (token != null) {
                    headers.set("Authorization", token);
                }
                HttpEntity<Void> entity = new HttpEntity<>(headers);
                ResponseEntity<java.util.Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, java.util.Map.class);
                if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                    deliveryPersonName = (String) response.getBody().get("name");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        order.setDeliveryPersonId(deliveryPersonId);
        order.setDeliveryPersonName(deliveryPersonName);
        order.setAssignedDate(LocalDateTime.now());
        order.setDeliveryStatus("ASSIGNED");
        order.setStatus(OrderStatus.ASSIGNED);
        order.setEstimatedDelivery(LocalDateTime.now().plusDays(3));

        return mapToDto(orderRepository.save(order));
    }

    private OrderDto mapToDto(Order order) {
        return OrderDto.builder()
                .id("ORD-" + (1000 + order.getId()))
                .productId(order.getProductId())
                .buyerId(order.getBuyerId())
                .sellerId(order.getSellerId())
                .deliveryPersonId(order.getDeliveryPersonId())
                .deliveryPersonName(order.getDeliveryPersonName())
                .assignedDate(order.getAssignedDate())
                .deliveryStatus(order.getDeliveryStatus())
                .estimatedDelivery(order.getEstimatedDelivery())
                .finalPrice(order.getFinalPrice())
                .status(order.getStatus())
                .createdAt(order.getCreatedAt())
                .build();
    }
}
