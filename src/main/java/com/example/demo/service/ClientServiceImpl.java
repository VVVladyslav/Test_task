package com.example.demo.service;

import com.example.demo.dto.ClientDto;
import com.example.demo.dto.UpdateClientRequest;
import com.example.demo.exception.NotFoundException;
import com.example.demo.model.Client;
import com.example.demo.repository.ClientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClientServiceImpl implements ClientService {

    private final ClientRepository clientRepository;

    @Override
    @Transactional
    public ClientDto create(String name, String email) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Client name must not be blank");
        }
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Client email must not be blank");
        }
        clientRepository.findByEmail(email).ifPresent(c -> {
            throw new IllegalArgumentException("Client with email already exists: " + email);
        });

        Client client = Client.builder()
                .name(name.trim())
                .email(email.trim())
                .active(true)
                .build();

        Client saved = clientRepository.save(client);
        return toDtoWithProfit(saved);
    }

    @Override
    public ClientDto getById(Long id) {
        Client c = clientRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Client not found: id=" + id));
        return toDtoWithProfit(c);
    }

    @Override
    public List<ClientDto> listAll() {
        return clientRepository.findAll().stream()
                .sorted(Comparator.comparing(Client::getName, String.CASE_INSENSITIVE_ORDER))
                .map(this::toDtoWithProfit)
                .collect(Collectors.toList());
    }

    @Override
    public List<ClientDto> search(String keyword) {
        if (keyword == null || keyword.trim().length() < 3) {
            return List.of();
        }
        String q = keyword.toLowerCase(Locale.ROOT).trim();
        return clientRepository.findAll().stream()
                .filter(c ->
                        (c.getName() != null && c.getName().toLowerCase(Locale.ROOT).contains(q)) ||
                                (c.getEmail() != null && c.getEmail().toLowerCase(Locale.ROOT).contains(q))
                )
                .sorted(Comparator.comparing(Client::getName, String.CASE_INSENSITIVE_ORDER))
                .map(this::toDtoWithProfit)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ClientDto update(Long id, UpdateClientRequest request) {
        Client c = clientRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Client not found: id=" + id));

        if (request.getName() == null || request.getName().isBlank()) {
            throw new IllegalArgumentException("Client name must not be blank");
        }
        if (request.getEmail() == null || request.getEmail().isBlank()) {
            throw new IllegalArgumentException("Client email must not be blank");
        }

        clientRepository.findByEmail(request.getEmail().trim()).ifPresent(other -> {
            if (!other.getId().equals(c.getId())) {
                throw new IllegalArgumentException("Client with email already exists: " + request.getEmail());
            }
        });

        c.setName(request.getName().trim());
        c.setEmail(request.getEmail().trim());
        c.setActive(Boolean.TRUE.equals(request.getActive()));

        Client saved = clientRepository.save(c);
        return toDtoWithProfit(saved);
    }

    @Override
    @Transactional
    public ClientDto activate(Long id) {
        Client c = clientRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Client not found: id=" + id));
        if (!c.isActive()) {
            c.setActive(true);
            clientRepository.save(c);
        }
        return toDtoWithProfit(c);
    }

    @Override
    @Transactional
    public ClientDto deactivate(Long id) {
        Client c = clientRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Client not found: id=" + id));
        if (c.isActive()) {
            c.setActive(false);
            clientRepository.save(c);
        }
        return toDtoWithProfit(c);
    }

    @Override
    public BigDecimal totalProfit(Long clientId) {
        Client c = clientRepository.findById(clientId)
                .orElseThrow(() -> new NotFoundException("Client not found: id=" + clientId));
        return c.getTotalProfit();
    }

    private ClientDto toDtoWithProfit(Client c) {
        return ClientDto.builder()
                .id(c.getId())
                .name(c.getName())
                .email(c.getEmail())
                .active(c.isActive())
                .totalProfit(c.getTotalProfit())
                .build();
    }
}