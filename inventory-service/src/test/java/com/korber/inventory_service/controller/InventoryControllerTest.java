package com.korber.inventory_service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.korber.inventory_service.dto.InventoryDtos;
import com.korber.inventory_service.service.InventoryService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(InventoryController.class)
class InventoryControllerTest {

    @Autowired MockMvc mockMvc;
    ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    @MockitoBean InventoryService inventoryService;


    @Test
    void getInventory_shouldReturn200_withBatches() throws Exception {
        InventoryDtos.InventoryResponse mockResponse = InventoryDtos.InventoryResponse.builder()
                .productId(1005L)
                .productName("Smartwatch")
                .batches(List.of(
                        InventoryDtos.BatchDto.builder().batchId(5L).quantity(39).expiryDate(LocalDate.of(2026,3,31)).build()
                ))
                .build();
        when(inventoryService.getInventoryByProductId(1005L)).thenReturn(mockResponse);

        mockMvc.perform(get("/inventory/1005"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId").value(1005))
                .andExpect(jsonPath("$.productName").value("Smartwatch"))
                .andExpect(jsonPath("$.batches[0].batchId").value(5));
    }

    @Test
    void getInventory_shouldReturn404_whenProductNotFound() throws Exception {
        when(inventoryService.getInventoryByProductId(9999L)).thenReturn(null);
        mockMvc.perform(get("/inventory/9999")).andExpect(status().isNotFound());
    }

    @Test
    void updateInventory_shouldReturn200_onSuccess() throws Exception {
        InventoryDtos.InventoryUpdateResponse mockResp = InventoryDtos.InventoryUpdateResponse.builder()
                .success(true).reservedFromBatchIds(List.of(5L)).message("Inventory updated successfully.").build();
        when(inventoryService.updateInventory(any())).thenReturn(mockResp);

        InventoryDtos.InventoryUpdateRequest req = InventoryDtos.InventoryUpdateRequest.builder()
                .productId(1005L).quantityToDeduct(10).build();

        mockMvc.perform(post("/inventory/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void updateInventory_shouldReturn400_onInsufficientStock() throws Exception {
        InventoryDtos.InventoryUpdateResponse mockResp = InventoryDtos.InventoryUpdateResponse.builder()
                .success(false).reservedFromBatchIds(List.of()).message("Insufficient stock").build();
        when(inventoryService.updateInventory(any())).thenReturn(mockResp);

        InventoryDtos.InventoryUpdateRequest req = InventoryDtos.InventoryUpdateRequest.builder()
                .productId(1005L).quantityToDeduct(9999).build();

        mockMvc.perform(post("/inventory/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }
}
