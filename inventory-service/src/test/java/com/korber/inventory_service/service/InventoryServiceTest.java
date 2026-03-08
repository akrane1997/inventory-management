package com.korber.inventory_service.service;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.korber.inventory_service.dto.InventoryDtos;
import com.korber.inventory_service.factory.FefoInventoryHandler;
import com.korber.inventory_service.factory.InventoryHandlerFactory;
import com.korber.inventory_service.model.InventoryBatch;
import com.korber.inventory_service.repository.InventoryBatchRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @Mock
    private InventoryBatchRepository repository;

    private InventoryService inventoryService;

    @BeforeEach
    void setUp() {
        FefoInventoryHandler fefoHandler = new FefoInventoryHandler(repository);
        InventoryHandlerFactory factory = new InventoryHandlerFactory(Map.of("FEFO", fefoHandler));
        inventoryService = new InventoryService(factory);
    }

    @Test
    void getInventory_shouldReturnBatchesSortedByExpiryDate() {
        Long productId = 1005L;
        List<InventoryBatch> batches = List.of(
                batch(5L, productId, "Smartwatch", 39, LocalDate.of(2026, 3, 31)),
                batch(7L, productId, "Smartwatch", 40, LocalDate.of(2026, 4, 24)),
                batch(2L, productId, "Smartwatch", 52, LocalDate.of(2026, 5, 30))
        );
        when(repository.findByProductIdOrderByExpiryDateAsc(productId)).thenReturn(batches);

        InventoryDtos.InventoryResponse response = inventoryService.getInventoryByProductId(productId);

        assertThat(response).isNotNull();
        assertThat(response.getProductId()).isEqualTo(productId);
        assertThat(response.getProductName()).isEqualTo("Smartwatch");
        assertThat(response.getBatches()).hasSize(3);
        assertThat(response.getBatches().get(0).getBatchId()).isEqualTo(5L);
    }

    @Test
    void getInventory_shouldReturnNull_whenProductNotFound() {
        when(repository.findByProductIdOrderByExpiryDateAsc(9999L)).thenReturn(List.of());
        InventoryDtos.InventoryResponse response = inventoryService.getInventoryByProductId(9999L);
        assertThat(response).isNull();
    }

    @Test
    void updateInventory_shouldDeductFromEarliestBatchFirst() {
        Long productId = 1005L;
        InventoryBatch b1 = batch(5L, productId, "Smartwatch", 39, LocalDate.of(2026, 3, 31));
        InventoryBatch b2 = batch(7L, productId, "Smartwatch", 40, LocalDate.of(2026, 4, 24));
        when(repository.findByProductIdOrderByExpiryDateAsc(productId)).thenReturn(List.of(b1, b2));

        InventoryDtos.InventoryUpdateRequest request = InventoryDtos.InventoryUpdateRequest.builder()
                .productId(productId)
                .quantityToDeduct(50)
                .build();

        InventoryDtos.InventoryUpdateResponse response = inventoryService.updateInventory(request);

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getReservedFromBatchIds()).containsExactly(5L, 7L);
        assertThat(b1.getQuantity()).isZero();
        assertThat(b2.getQuantity()).isEqualTo(29);
    }

    @Test
    void updateInventory_shouldFail_whenInsufficientStock() {
        Long productId = 1005L;
        InventoryBatch b1 = batch(5L, productId, "Smartwatch", 10, LocalDate.of(2026, 3, 31));
        when(repository.findByProductIdOrderByExpiryDateAsc(productId)).thenReturn(List.of(b1));

        InventoryDtos.InventoryUpdateRequest request = InventoryDtos.InventoryUpdateRequest.builder()
                .productId(productId)
                .quantityToDeduct(100)
                .build();

        InventoryDtos.InventoryUpdateResponse response = inventoryService.updateInventory(request);

        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getMessage()).contains("Insufficient stock");
    }

    private InventoryBatch batch(Long batchId, Long productId, String name, int qty, LocalDate expiry) {
        return InventoryBatch.builder()
                .batchId(batchId).productId(productId)
                .productName(name).quantity(qty).expiryDate(expiry)
                .build();
    }
}
