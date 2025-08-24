package com.example.demo.controller;

import com.example.demo.dto.ClientDto;
import com.example.demo.dto.ClientProfitDto;
import com.example.demo.dto.ClientStatusRequest;
import com.example.demo.dto.CreateClientRequest;
import com.example.demo.dto.OrderDto;
import com.example.demo.dto.UpdateClientRequest;
import com.example.demo.service.ClientService;
import com.example.demo.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/clients")
@RequiredArgsConstructor
@Validated
public class ClientController {

    private final ClientService clientService;
    private final OrderService orderService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ClientDto create(@Valid @RequestBody CreateClientRequest request) {
        return clientService.create(request);
    }

    @GetMapping("/{id}")
    public ClientDto getById(@PathVariable Long id) {
        return clientService.getById(id);
    }

    @GetMapping
    public List<ClientDto> list(@RequestParam(value = "query", required = false) String query) {
        return clientService.listAllOrSearch(query);
    }

    @PutMapping("/{id}")
    public ClientDto update(@PathVariable Long id,
                            @Valid @RequestBody UpdateClientRequest request) {
        return clientService.update(id, request);
    }

    @PatchMapping("/{id}/status")
    public ClientDto updateStatus(@PathVariable Long id,
                                  @Valid @RequestBody ClientStatusRequest req) {
        return clientService.updateActiveStatus(id, req.getActive());
    }

    @GetMapping("/{id}/orders")
    public List<OrderDto> ordersOfClient(@PathVariable Long id) {
        return orderService.listByClient(id);
    }

    @GetMapping("/{id}/profit")
    public ClientProfitDto profit(@PathVariable Long id) {
        return clientService.getProfit(id);
    }

    @GetMapping("/profit-range")
    public List<ClientProfitDto> findByProfitRange(
            @RequestParam(value = "min", required = false) BigDecimal min,
            @RequestParam(value = "max", required = false) BigDecimal max
    ) {
        return clientService.findClientsByProfitRange(min, max);
    }
}