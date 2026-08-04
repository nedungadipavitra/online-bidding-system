package com.onlinebidding.order_service.controller;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import com.onlinebidding.order_service.dto.OrderDto;
import com.onlinebidding.order_service.service.OrderService;

@ExtendWith(MockitoExtension.class)
class OrderControllerSecurityTest {

    @Mock
    private OrderService orderService;

    private OrderController orderController;

    @BeforeEach
    void setUp() {
        orderController = new OrderController(orderService);
    }

    @Test
    void getOrdersByBuyer_rejectsAnotherUsersRequest() {
        assertThatThrownBy(() -> orderController.getOrdersByBuyer("10", "BUYER", 20L))
                .isInstanceOf(ResponseStatusException.class);

        verify(orderService, never()).getOrdersByBuyer(20L);
    }

    @Test
    void updateOrderStatus_rejectsUnassignedDeliveryPartner() {
        when(orderService.getOrderById(1L)).thenReturn(OrderDto.builder()
                .deliveryPersonId(20L)
                .build());

        assertThatThrownBy(() -> orderController.updateOrderStatus(
                "10", "DELIVERY", "1", Map.of("status", "DISPATCHED")))
                .isInstanceOf(ResponseStatusException.class);

        verify(orderService, never()).updateOrderStatus(1L, "DISPATCHED");
    }

    @Test
    void assignDeliveryPerson_rejectsSellerWhoDoesNotOwnOrder() {
        when(orderService.getOrderById(1L)).thenReturn(OrderDto.builder()
                .sellerId(20L)
                .build());

        assertThatThrownBy(() -> orderController.assignDeliveryPerson(
                "Bearer token", "10", "SELLER", "1", Map.of("deliveryPersonId", 30L)))
                .isInstanceOf(ResponseStatusException.class);

        verify(orderService, never()).assignDeliveryPerson(1L, 30L, "Bearer token");
    }
}
