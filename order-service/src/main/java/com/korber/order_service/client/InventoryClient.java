package com.korber.order_service.client;


import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.korber.order_service.dto.OrderDtos;

/**
 * HTTP client for communicating with the Inventory Service.
 */
@Component
@RequiredArgsConstructor
public class InventoryClient {

    private final RestTemplate restTemplate;

    @Value("${inventory.service.url}")
    private String inventoryServiceUrl;

    public OrderDtos.InventoryResponse getInventory(Long productId) {
        String url = inventoryServiceUrl + "/inventory/" + productId;
        return restTemplate.getForObject(url, OrderDtos.InventoryResponse.class);
    }

    public OrderDtos.InventoryUpdateResponse updateInventory(OrderDtos.InventoryUpdateRequest request) {
        String url = inventoryServiceUrl + "/inventory/update";
        return restTemplate.postForObject(url, request, OrderDtos.InventoryUpdateResponse.class);
    }
}

