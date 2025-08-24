package com.example.demo.service;

import com.example.demo.dto.ClientDto;
import com.example.demo.dto.ClientProfitDto;
import com.example.demo.dto.CreateClientRequest;
import com.example.demo.dto.OrderDto;
import com.example.demo.dto.UpdateClientRequest;

import java.math.BigDecimal;
import java.util.List;

public interface ClientService {

    ClientDto create(CreateClientRequest request);
    ClientDto getById(Long id);
    List<ClientDto> listAllOrSearch(String query);
    ClientDto update(Long id, UpdateClientRequest request);

    ClientDto updateActiveStatus(Long id, boolean active);

    List<OrderDto> listOrdersForClient(Long clientId);
    ClientProfitDto getProfit(Long clientId);
    List<ClientProfitDto> findClientsByProfitRange(BigDecimal min, BigDecimal max);
}