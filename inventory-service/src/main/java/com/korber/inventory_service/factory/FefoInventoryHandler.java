package com.korber.inventory_service.factory;


import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import com.korber.inventory_service.dto.InventoryDtos;
import com.korber.inventory_service.model.InventoryBatch;
import com.korber.inventory_service.repository.InventoryBatchRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * FEFO (First Expiry, First Out) implementation of InventoryHandler.
 * Deducts stock from the batch with the earliest expiry date first —
 * the standard approach for pharmaceutical / perishable goods.
 */
@Component("FEFO")
@RequiredArgsConstructor
public class FefoInventoryHandler implements InventoryHandler {

    private final InventoryBatchRepository repository;

    @Override
    public InventoryDtos.InventoryResponse getInventory(Long productId) {
        List<InventoryBatch> batches = repository.findByProductIdOrderByExpiryDateAsc(productId);

        if (batches.isEmpty()) {
            return null;
        }

        String productName = batches.get(0).getProductName();

        List<InventoryDtos.BatchDto> batchDtos = batches.stream()
                .map(b -> InventoryDtos.BatchDto.builder()
                        .batchId(b.getBatchId())
                        .quantity(b.getQuantity())
                        .expiryDate(b.getExpiryDate())
                        .build())
                .collect(Collectors.toList());

        return InventoryDtos.InventoryResponse.builder()
                .productId(productId)
                .productName(productName)
                .batches(batchDtos)
                .build();
    }

    @Override
    public InventoryDtos.InventoryUpdateResponse updateInventory(InventoryDtos.InventoryUpdateRequest request) {
        List<InventoryBatch> batches = repository.findByProductIdOrderByExpiryDateAsc(request.getProductId());

        int remaining = request.getQuantityToDeduct();
        List<Long> usedBatchIds = new ArrayList<>();

        for (InventoryBatch batch : batches) {
            if (remaining <= 0) break;

            int available = batch.getQuantity();
            if (available <= 0) continue;

            int deduct = Math.min(available, remaining);
            batch.setQuantity(available - deduct);
            remaining -= deduct;
            usedBatchIds.add(batch.getBatchId());
        }

        if (remaining > 0) {
            return InventoryDtos.InventoryUpdateResponse.builder()
                    .success(false)
                    .reservedFromBatchIds(List.of())
                    .message("Insufficient stock for product " + request.getProductId())
                    .build();
        }

        repository.saveAll(batches);

        return InventoryDtos.InventoryUpdateResponse.builder()
                .success(true)
                .reservedFromBatchIds(usedBatchIds)
                .message("Inventory updated successfully.")
                .build();
    }
}
