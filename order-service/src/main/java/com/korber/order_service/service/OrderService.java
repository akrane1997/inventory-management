package com.korber.order_service.service;


import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.korber.order_service.client.InventoryClient;
import com.korber.order_service.dto.OrderDtos;
import com.korber.order_service.model.Order;
import com.korber.order_service.repository.OrderRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final InventoryClient inventoryClient;

    @Transactional
    public OrderDtos.OrderResponse placeOrder(OrderDtos.OrderRequest request) {
        // 1. Check availability in Inventory Service
        OrderDtos.InventoryResponse inventory = inventoryClient.getInventory(request.getProductId());
        if (inventory == null) {
            return OrderDtos.OrderResponse.builder()
                    .productId(request.getProductId())
                    .quantity(request.getQuantity())
                    .status("FAILED")
                    .message("Product not found in inventory.")
                    .build();
        }

        int totalAvailable = inventory.getBatches().stream()
                .mapToInt(OrderDtos.InventoryBatchDto::getQuantity)
                .sum();

        if (totalAvailable < request.getQuantity()) {
            return OrderDtos.OrderResponse.builder()
                    .productId(request.getProductId())
                    .productName(inventory.getProductName())
                    .quantity(request.getQuantity())
                    .status("FAILED")
                    .message("Insufficient stock. Available: " + totalAvailable)
                    .build();
        }

        // 2. Deduct stock from Inventory Service
        OrderDtos.InventoryUpdateRequest updateRequest = OrderDtos.InventoryUpdateRequest.builder()
                .productId(request.getProductId())
                .quantityToDeduct(request.getQuantity())
                .build();

        OrderDtos.InventoryUpdateResponse updateResponse = inventoryClient.updateInventory(updateRequest);

        if (updateResponse == null || !updateResponse.isSuccess()) {
            String msg = updateResponse != null ? updateResponse.getMessage() : "Inventory update failed.";
            return OrderDtos.OrderResponse.builder()
                    .productId(request.getProductId())
                    .productName(inventory.getProductName())
                    .quantity(request.getQuantity())
                    .status("FAILED")
                    .message(msg)
                    .build();
        }

        // 3. Persist the order
        List<Long> batchIds = updateResponse.getReservedFromBatchIds();
        String batchIdsStr = batchIds.stream().map(String::valueOf).collect(Collectors.joining(","));

        Order order = Order.builder()
                .productId(request.getProductId())
                .productName(inventory.getProductName())
                .quantity(request.getQuantity())
                .status("PLACED")
                .orderDate(LocalDate.now())
                .reservedBatchIds(batchIdsStr)
                .build();

        Order saved = orderRepository.save(order);

        return OrderDtos.OrderResponse.builder()
                .orderId(saved.getOrderId())
                .productId(saved.getProductId())
                .productName(saved.getProductName())
                .quantity(saved.getQuantity())
                .status(saved.getStatus())
                .reservedFromBatchIds(batchIds)
                .message("Order placed. Inventory reserved.")
                .build();
    }
}
