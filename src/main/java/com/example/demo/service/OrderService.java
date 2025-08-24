package com.example.demo.service;

import com.example.demo.dto.CreateOrderRequest;
import com.example.demo.dto.OrderDto;
import com.example.demo.dto.UpdateOrderRequest;

import java.util.List;

public interface OrderService {

    OrderDto create(CreateOrderRequest request);
    OrderDto getById(Long id);
    List<OrderDto> listAll();
    List<OrderDto> listByClient(Long clientId);
    OrderDto update(Long id, UpdateOrderRequest request);
    void delete(Long id);
}