package com.example.demo.service;

import com.example.demo.dto.ClientDto;
import com.example.demo.dto.UpdateClientRequest;

import java.math.BigDecimal;
import java.util.List;

public interface ClientService {

    ClientDto create(String name, String email);

    ClientDto getById(Long id);

    List<ClientDto> listAll();

    List<ClientDto> search(String keyword);

    ClientDto update(Long id, UpdateClientRequest request);

    ClientDto activate(Long id);

    ClientDto deactivate(Long id);

    BigDecimal totalProfit(Long clientId);
}