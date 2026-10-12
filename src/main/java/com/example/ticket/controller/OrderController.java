package com.example.ticket.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.example.ticket.dto.order.CreateOrderRequest;
import com.example.ticket.dto.order.OrderResponse;
import com.example.ticket.dto.order.OrderSummaryResponse;
import com.example.ticket.enums.OrderStatus;
import com.example.ticket.security.UserPrincipal;
import com.example.ticket.service.OrderService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse createOrder(
            @Valid @RequestBody CreateOrderRequest request,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        return orderService.createOrder(currentUser.getId(), request);
    }

    @GetMapping("/{orderId}")
    public OrderResponse getOrderById(
            @PathVariable Long orderId,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        return orderService.getOrderById(orderId, currentUser.getId(), false);
    }

    @GetMapping("/my")
    public Page<OrderSummaryResponse> getMyOrders(
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return orderService.getMyOrders(currentUser.getId(), status, pageRequest);
    }

    @DeleteMapping("/{orderId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancelOrder(
            @PathVariable Long orderId,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        orderService.cancelOrder(orderId, currentUser.getId());
    }
}
