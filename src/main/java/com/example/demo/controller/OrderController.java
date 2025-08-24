package com.example.demo.controller;

import com.example.demo.dto.CreateOrderRequest;
import com.example.demo.dto.OrderDto;
import com.example.demo.dto.UpdateOrderRequest;
import com.example.demo.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Validated
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderDto create(@Valid @RequestBody CreateOrderRequest request) {
        return orderService.create(request);
    }

    @GetMapping("/{id}")
    public OrderDto getById(@PathVariable Long id) {
        return orderService.getById(id);
    }

    @GetMapping
    public List<OrderDto> list(@RequestParam(value = "clientId", required = false) Long clientId) {
        return (clientId == null) ? orderService.listAll() : orderService.listByClient(clientId);
    }

    @PutMapping("/{id}")
    public OrderDto update(@PathVariable Long id, @Valid @RequestBody UpdateOrderRequest request) {
        return orderService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        orderService.delete(id);
    }
}