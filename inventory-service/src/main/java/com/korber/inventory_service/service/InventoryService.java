package com.korber.inventory_service.service;


import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.korber.inventory_service.dto.InventoryDtos;
import com.korber.inventory_service.factory.InventoryHandlerFactory;

@Service
@RequiredArgsConstructor
public class InventoryService {

    private final InventoryHandlerFactory handlerFactory;

    /**
     * Returns all inventory batches for the given product, sorted by expiry date (FEFO).
     */
    public InventoryDtos.InventoryResponse getInventoryByProductId(Long productId) {
        return handlerFactory.getHandler("FEFO").getInventory(productId);
    }

    /**
     * Deducts stock using FEFO strategy and returns which batches were consumed.
     */
    @Transactional
    public InventoryDtos.InventoryUpdateResponse updateInventory(InventoryDtos.InventoryUpdateRequest request) {
        return handlerFactory.getHandler("FEFO").updateInventory(request);
    }
}
