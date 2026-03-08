package com.korber.order_service.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.korber.order_service.dto.OrderDtos;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.RestTemplate;

import java.util.List;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class InventoryIntegrationTest {

    @Autowired MockMvc mockMvc;
    ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    @Autowired RestTemplate restTemplate;

    private MockRestServiceServer mockServer;

    @BeforeEach
    void setUp() {
        mockServer = MockRestServiceServer.createServer(restTemplate);
    }

    @Test
    void placeOrder_shouldSucceed_withMockedInventoryService() throws Exception {
        // Mock GET /inventory/1002
        OrderDtos.InventoryResponse inventoryResponse = OrderDtos.InventoryResponse.builder()
                .productId(1002L).productName("Smartphone")
                .batches(List.of(
                        OrderDtos.InventoryBatchDto.builder().batchId(9L).quantity(29).expiryDate("2026-05-31").build()
                ))
                .build();

        mockServer.expect(requestTo("http://localhost:8081/inventory/1002"))
                .andRespond(withSuccess(objectMapper.writeValueAsString(inventoryResponse), MediaType.APPLICATION_JSON));

        // Mock POST /inventory/update
        OrderDtos.InventoryUpdateResponse updateResponse = OrderDtos.InventoryUpdateResponse.builder()
                .success(true).reservedFromBatchIds(List.of(9L)).message("OK").build();

        mockServer.expect(requestTo("http://localhost:8081/inventory/update"))
                .andRespond(withSuccess(objectMapper.writeValueAsString(updateResponse), MediaType.APPLICATION_JSON));

        OrderDtos.OrderRequest req = OrderDtos.OrderRequest.builder()
                .productId(1002L).quantity(5).build();

        mockMvc.perform(post("/order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PLACED"))
                .andExpect(jsonPath("$.productName").value("Smartphone"))
                .andExpect(jsonPath("$.message").value("Order placed. Inventory reserved."));

        mockServer.verify();
    }

    @Test
    void placeOrder_shouldReturn400_whenInventoryReturnsNull() throws Exception {
        mockServer.expect(requestTo("http://localhost:8081/inventory/9999"))
                .andRespond(withSuccess("null", MediaType.APPLICATION_JSON));

        OrderDtos.OrderRequest req = OrderDtos.OrderRequest.builder()
                .productId(9999L).quantity(1).build();

        mockMvc.perform(post("/order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("FAILED"));
    }
}

