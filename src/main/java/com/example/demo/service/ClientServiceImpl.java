package com.example.demo.service;

import com.example.demo.dto.ClientDto;
import com.example.demo.dto.ClientProfitDto;
import com.example.demo.dto.CreateClientRequest;
import com.example.demo.dto.OrderDto;
import com.example.demo.dto.UpdateClientRequest;
import com.example.demo.exception.BadRequestException;
import com.example.demo.exception.ConflictException;
import com.example.demo.exception.NotFoundException;
import com.example.demo.model.Client;
import com.example.demo.model.Order;
import com.example.demo.repository.ClientRepository;
import com.example.demo.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ClientServiceImpl implements ClientService {

    private final ClientRepository clientRepository;
    private final OrderRepository orderRepository;

    private ClientDto toClientDto(Client c) {
        return ClientDto.builder()
                .id(c.getId())
                .name(c.getName())
                .email(c.getEmail())
                .address(c.getAddress())
                .active(c.isActive())
                .deactivatedAt(c.getDeactivatedAt())
                .build();
    }

    private OrderDto toOrderDto(Order o) {
        return OrderDto.builder()
                .id(o.getId())
                .title(o.getTitle())
                .supplierId(o.getSupplier().getId())
                .consumerId(o.getConsumer().getId())
                .price(o.getPrice())
                .startedAt(o.getStartedAt())
                .finishedAt(o.getFinishedAt())
                .createdAt(o.getCreatedAt())
                .build();
    }

    @Override
    public ClientDto create(CreateClientRequest request) {
        if (request.getName() == null || request.getName().isBlank()) {
            throw new BadRequestException("Client name must not be blank");
        }
        if (request.getEmail() == null || request.getEmail().isBlank()) {
            throw new BadRequestException("Client email must not be blank");
        }

        clientRepository.findByEmailIgnoreCase(request.getEmail().trim())
                .ifPresent(x -> { throw new ConflictException("Email already exists: " + request.getEmail()); });

        Client client = Client.builder()
                .name(request.getName().trim())
                .email(request.getEmail().trim())
                .address(request.getAddress() == null ? null : request.getAddress().trim())
                .active(true)
                .deactivatedAt(null)
                .build();

        return toClientDto(clientRepository.save(client));
    }

    @Override
    @Transactional(readOnly = true)
    public ClientDto getById(Long id) {
        Client c = clientRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Client not found: id=" + id));
        return toClientDto(c);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClientDto> listAllOrSearch(String query) {
        if (query == null || query.trim().isBlank()) {
            return clientRepository.findAll().stream()
                    .sorted(Comparator.comparing(Client::getId))
                    .map(this::toClientDto)
                    .toList();
        }
        String q = query.trim();
        if (q.length() < 3) {
            throw new BadRequestException("Search keyword must be at least 3 characters");
        }
        return clientRepository.searchByKeyword(q).stream()
                .sorted(Comparator.comparing(Client::getId))
                .map(this::toClientDto)
                .toList();
    }

    @Override
    public ClientDto update(Long id, UpdateClientRequest request) {
        if (request.getName() == null || request.getName().isBlank()) {
            throw new BadRequestException("Client name must not be blank");
        }
        if (request.getEmail() == null || request.getEmail().isBlank()) {
            throw new BadRequestException("Client email must not be blank");
        }

        Client client = clientRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Client not found: id=" + id));

        clientRepository.findByEmailIgnoreCase(request.getEmail().trim())
                .filter(other -> !other.getId().equals(id))
                .ifPresent(other -> { throw new ConflictException("Email already exists: " + request.getEmail()); });

        client.setName(request.getName().trim());
        client.setEmail(request.getEmail().trim());
        client.setAddress(request.getAddress() == null ? null : request.getAddress().trim());

        return toClientDto(clientRepository.save(client));
    }

    @Override
    public ClientDto updateActiveStatus(Long id, boolean active) {
        Client client = clientRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Client not found: id=" + id));

        if (active) {
            client.setActive(true);
            client.setDeactivatedAt(null);
        } else {
            if (client.isActive()) {
                client.setActive(false);
                client.setDeactivatedAt(LocalDateTime.now());
            }
        }
        return toClientDto(clientRepository.save(client));
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderDto> listOrdersForClient(Long clientId) {
        clientRepository.findById(clientId)
                .orElseThrow(() -> new NotFoundException("Client not found: id=" + clientId));

        return orderRepository.findBySupplierIdOrConsumerId(clientId, clientId).stream()
                .sorted(Comparator.comparing(Order::getId))
                .map(this::toOrderDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ClientProfitDto getProfit(Long clientId) {
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new NotFoundException("Client not found: id=" + clientId));

        BigDecimal profit = orderRepository.computeProfit(client.getId());
        return ClientProfitDto.builder()
                .clientId(client.getId())
                .name(client.getName())
                .email(client.getEmail())
                .active(client.isActive())
                .profit(profit)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClientProfitDto> findClientsByProfitRange(BigDecimal min, BigDecimal max) {
        if (min != null && max != null && min.compareTo(max) > 0) {
            throw new BadRequestException("min must be <= max");
        }

        return clientRepository.findAll().stream()
                .map(c -> {
                    BigDecimal p = orderRepository.computeProfit(c.getId());
                    return ClientProfitDto.builder()
                            .clientId(c.getId())
                            .name(c.getName())
                            .email(c.getEmail())
                            .active(c.isActive())
                            .profit(p)
                            .build();
                })
                .filter(dto -> {
                    boolean ok = true;
                    if (min != null) ok = ok && dto.getProfit().compareTo(min) >= 0;
                    if (max != null) ok = ok && dto.getProfit().compareTo(max) <= 0;
                    return ok;
                })
                .sorted(Comparator.comparing(ClientProfitDto::getClientId))
                .toList();
    }
}