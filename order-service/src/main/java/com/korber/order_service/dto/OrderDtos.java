package com.korber.order_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

public class OrderDtos {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class OrderRequest {
        private Long productId;
        private Integer quantity;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class OrderResponse {
        private Long orderId;
        private Long productId;
        private String productName;
        private Integer quantity;
        private String status;
        private List<Long> reservedFromBatchIds;
        private String message;
    }

    // --- DTOs mirroring what Inventory Service returns ---

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

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class InventoryBatchDto {
        private Long batchId;
        private Integer quantity;
        private String expiryDate;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class InventoryResponse {
        private Long productId;
        private String productName;
        private List<InventoryBatchDto> batches;
    }
}

