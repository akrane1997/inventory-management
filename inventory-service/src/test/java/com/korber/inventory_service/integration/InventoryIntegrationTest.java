package com.korber.inventory_service.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.korber.inventory_service.dto.InventoryDtos;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class InventoryIntegrationTest {

    @Autowired MockMvc mockMvc;
    ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();


    @Test
    void getInventory_shouldReturnBatchesSortedByExpiry_forSmartwatch() throws Exception {
        mockMvc.perform(get("/inventory/1005"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId").value(1005))
                .andExpect(jsonPath("$.productName").value("Smartwatch"))
                .andExpect(jsonPath("$.batches").isArray())
                // First batch should be earliest expiry: 2026-03-31 (batchId=5)
                .andExpect(jsonPath("$.batches[0].batchId").value(5))
                .andExpect(jsonPath("$.batches[0].expiryDate").value("2026-03-31"));
    }

    @Test
    void getInventory_shouldReturn404_forUnknownProduct() throws Exception {
        mockMvc.perform(get("/inventory/9999")).andExpect(status().isNotFound());
    }

    @Test
    void updateInventory_shouldSucceed_andDeductStock() throws Exception {
        InventoryDtos.InventoryUpdateRequest req = InventoryDtos.InventoryUpdateRequest.builder()
                .productId(1001L)
                .quantityToDeduct(10)
                .build();

        mockMvc.perform(post("/inventory/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.reservedFromBatchIds").isArray());
    }

    @Test
    void updateInventory_shouldReturn400_whenInsufficientStock() throws Exception {
        InventoryDtos.InventoryUpdateRequest req = InventoryDtos.InventoryUpdateRequest.builder()
                .productId(1001L)
                .quantityToDeduct(99999)
                .build();

        mockMvc.perform(post("/inventory/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }
}
