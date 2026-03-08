package com.korber.order_service.controller;


import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.korber.order_service.dto.OrderDtos;
import com.korber.order_service.service.OrderService;

@RestController
@RequestMapping("/order")
@RequiredArgsConstructor
@Tag(name = "Order", description = "Order management endpoints")
public class OrderController {

	private final OrderService orderService;

    @PostMapping
    @Operation(summary = "Place a new order")
    public ResponseEntity<OrderDtos.OrderResponse> placeOrder(@RequestBody OrderDtos.OrderRequest request) {
        OrderDtos.OrderResponse response = orderService.placeOrder(request);
        if ("FAILED".equals(response.getStatus())) {
            return ResponseEntity.badRequest().body(response);
        }
        return ResponseEntity.ok(response);
    }
}
