package com.korber.inventory_service.factory;

import com.korber.inventory_service.dto.InventoryDtos;

/**
 * Strategy interface for inventory handling logic.
 * New inventory strategies (e.g., FIFO, FEFO, LIFO) can be added
 * by implementing this interface without changing existing code.
 */
public interface InventoryHandler {

    /**
     * Returns sorted inventory batches for a given product.
     */
    InventoryDtos.InventoryResponse getInventory(Long productId);

    /**
     * Deducts stock across batches and returns which batch IDs were used.
     */
    InventoryDtos.InventoryUpdateResponse updateInventory(InventoryDtos.InventoryUpdateRequest request);
}