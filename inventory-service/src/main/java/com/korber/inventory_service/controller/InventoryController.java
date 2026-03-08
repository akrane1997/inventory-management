package com.korber.inventory_service.controller;


import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.korber.inventory_service.dto.InventoryDtos;
import com.korber.inventory_service.service.InventoryService;

@RestController
@RequestMapping("/inventory")
@RequiredArgsConstructor
@Tag(name = "Inventory", description = "Inventory management endpoints")
public class InventoryController {

    private final InventoryService inventoryService;

    @GetMapping("/{productId}")
    @Operation(summary = "Get inventory batches for a product sorted by expiry date")
    public ResponseEntity<InventoryDtos.InventoryResponse> getInventory(@PathVariable Long productId) {
        InventoryDtos.InventoryResponse response = inventoryService.getInventoryByProductId(productId);
        if (response == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(response);
    }

    @PostMapping("/update")
    @Operation(summary = "Update (deduct) inventory after an order is placed")
    public ResponseEntity<InventoryDtos.InventoryUpdateResponse> updateInventory(
            @RequestBody InventoryDtos.InventoryUpdateRequest request) {
        InventoryDtos.InventoryUpdateResponse response = inventoryService.updateInventory(request);
        if (!response.isSuccess()) {
            return ResponseEntity.badRequest().body(response);
        }
        return ResponseEntity.ok(response);
    }
}