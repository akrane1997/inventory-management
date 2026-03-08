package com.korber.order_service.service;


import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.korber.order_service.client.InventoryClient;
import com.korber.order_service.dto.OrderDtos;
import com.korber.order_service.model.Order;
import com.korber.order_service.repository.OrderRepository;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock private OrderRepository orderRepository;
    @Mock private InventoryClient inventoryClient;
    @InjectMocks private OrderService orderService;

    @Test
    void placeOrder_shouldSucceed_whenStockAvailable() {
        OrderDtos.InventoryResponse inventory = OrderDtos.InventoryResponse.builder()
                .productId(1002L).productName("Smartphone")
                .batches(List.of(
                        OrderDtos.InventoryBatchDto.builder().batchId(9L).quantity(29).expiryDate("2026-05-31").build()
                ))
                .build();

        OrderDtos.InventoryUpdateResponse updateResp = OrderDtos.InventoryUpdateResponse.builder()
                .success(true).reservedFromBatchIds(List.of(9L)).message("OK").build();

        Order savedOrder = Order.builder()
                .orderId(101L).productId(1002L).productName("Smartphone")
                .quantity(5).status("PLACED").orderDate(LocalDate.now()).reservedBatchIds("9")
                .build();

        when(inventoryClient.getInventory(1002L)).thenReturn(inventory);
        when(inventoryClient.updateInventory(any())).thenReturn(updateResp);
        when(orderRepository.save(any())).thenReturn(savedOrder);

        OrderDtos.OrderResponse response = orderService.placeOrder(
                OrderDtos.OrderRequest.builder().productId(1002L).quantity(5).build());

        assertThat(response.getStatus()).isEqualTo("PLACED");
        assertThat(response.getOrderId()).isEqualTo(101L);
        assertThat(response.getReservedFromBatchIds()).containsExactly(9L);
    }

    @Test
    void placeOrder_shouldFail_whenProductNotFound() {
        when(inventoryClient.getInventory(9999L)).thenReturn(null);

        OrderDtos.OrderResponse response = orderService.placeOrder(
                OrderDtos.OrderRequest.builder().productId(9999L).quantity(1).build());

        assertThat(response.getStatus()).isEqualTo("FAILED");
        assertThat(response.getMessage()).contains("not found");
        verify(orderRepository, never()).save(any());
    }

    @Test
    void placeOrder_shouldFail_whenInsufficientStock() {
        OrderDtos.InventoryResponse inventory = OrderDtos.InventoryResponse.builder()
                .productId(1001L).productName("Laptop")
                .batches(List.of(
                        OrderDtos.InventoryBatchDto.builder().batchId(1L).quantity(5).expiryDate("2026-06-25").build()
                ))
                .build();

        when(inventoryClient.getInventory(1001L)).thenReturn(inventory);

        OrderDtos.OrderResponse response = orderService.placeOrder(
                OrderDtos.OrderRequest.builder().productId(1001L).quantity(100).build());

        assertThat(response.getStatus()).isEqualTo("FAILED");
        assertThat(response.getMessage()).contains("Insufficient");
        verify(orderRepository, never()).save(any());
    }

    @Test
    void placeOrder_shouldFail_whenInventoryUpdateFails() {
        OrderDtos.InventoryResponse inventory = OrderDtos.InventoryResponse.builder()
                .productId(1001L).productName("Laptop")
                .batches(List.of(
                        OrderDtos.InventoryBatchDto.builder().batchId(1L).quantity(68).expiryDate("2026-06-25").build()
                ))
                .build();

        OrderDtos.InventoryUpdateResponse failResp = OrderDtos.InventoryUpdateResponse.builder()
                .success(false).message("Inventory update failed.").build();

        when(inventoryClient.getInventory(1001L)).thenReturn(inventory);
        when(inventoryClient.updateInventory(any())).thenReturn(failResp);

        OrderDtos.OrderResponse response = orderService.placeOrder(
                OrderDtos.OrderRequest.builder().productId(1001L).quantity(10).build());

        assertThat(response.getStatus()).isEqualTo("FAILED");
        verify(orderRepository, never()).save(any());
    }
}
