package com.korber.order_service.controller;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.korber.order_service.dto.OrderDtos;
import com.korber.order_service.service.OrderService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(OrderController.class)
class OrderControllerTest {

    @Autowired MockMvc mockMvc;
    ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    @MockitoBean OrderService orderService;

    @Test
    void placeOrder_shouldReturn200_onSuccess() throws Exception {
        OrderDtos.OrderResponse mockResp = OrderDtos.OrderResponse.builder()
                .orderId(101L).productId(1002L).productName("Smartphone")
                .quantity(3).status("PLACED").reservedFromBatchIds(List.of(9L))
                .message("Order placed. Inventory reserved.")
                .build();
        when(orderService.placeOrder(any())).thenReturn(mockResp);

        OrderDtos.OrderRequest req = OrderDtos.OrderRequest.builder()
                .productId(1002L).quantity(3).build();

        mockMvc.perform(post("/order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(101))
                .andExpect(jsonPath("$.status").value("PLACED"))
                .andExpect(jsonPath("$.message").value("Order placed. Inventory reserved."));
    }

    @Test
    void placeOrder_shouldReturn400_onFailure() throws Exception {
        OrderDtos.OrderResponse failResp = OrderDtos.OrderResponse.builder()
                .productId(1002L).quantity(9999).status("FAILED")
                .message("Insufficient stock.")
                .build();
        when(orderService.placeOrder(any())).thenReturn(failResp);

        OrderDtos.OrderRequest req = OrderDtos.OrderRequest.builder()
                .productId(1002L).quantity(9999).build();

        mockMvc.perform(post("/order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("FAILED"));
    }
}
