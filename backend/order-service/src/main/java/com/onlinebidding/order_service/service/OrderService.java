package com.onlinebidding.order_service.service;

import com.onlinebidding.order_service.dto.OrderDto;

import java.util.List;

public interface OrderService {
    OrderDto createOrder(OrderDto orderDto);
    OrderDto getOrderById(Long id);
    List<OrderDto> getOrdersByBuyer(Long buyerId);
    List<OrderDto> getOrdersByDeliveryPerson(Long deliveryPersonId);
    List<OrderDto> getAllOrders();
    OrderDto updateOrderStatus(Long id, String status);
    OrderDto assignDeliveryPerson(Long id, Long deliveryPersonId, String token);
}
