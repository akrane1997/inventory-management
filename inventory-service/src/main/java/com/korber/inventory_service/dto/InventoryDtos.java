package com.korber.inventory_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

public class InventoryDtos {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class BatchDto {
        private Long batchId;
        private Integer quantity;
        private LocalDate expiryDate;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class InventoryResponse {
        private Long productId;
        private String productName;
        private List<BatchDto> batches;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class InventoryUpdateRequest {
        private Long productId;
        private Integer quantityToDeduct;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class InventoryUpdateResponse {
        private boolean success;
        private List<Long> reservedFromBatchIds;
        private String message;
    }
}
