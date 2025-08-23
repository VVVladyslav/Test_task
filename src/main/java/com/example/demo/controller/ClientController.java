package com.example.demo.controller;

import com.example.demo.dto.ClientDto;
import com.example.demo.dto.OrderDto;
import com.example.demo.dto.UpdateClientRequest;
import com.example.demo.service.ClientService;
import com.example.demo.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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

    /**
     * Создание клиента.
     * Принимает обязательные параметры name и email.
     */
    @PostMapping
    public ClientDto create(@RequestParam String name,
                            @RequestParam String email) {
        return clientService.create(name, email);
    }

    /**
     * Получение клиента по идентификатору.
     */
    @GetMapping("/{id}")
    public ClientDto getById(@PathVariable Long id) {
        return clientService.getById(id);
    }

    /**
     * Список клиентов.
     * Если передан параметр q (минимум 3 символа) — выполняется поиск.
     */
    @GetMapping
    public List<ClientDto> list(@RequestParam(name = "q", required = false) String keyword) {
        if (keyword != null && keyword.trim().length() >= 3) {
            return clientService.search(keyword);
        }
        return clientService.listAll();
    }

    /**
     * Обновление данных клиента (имя, email, статус active).
     */
    @PutMapping("/{id}")
    public ClientDto update(@PathVariable Long id,
                            @Valid @RequestBody UpdateClientRequest request) {
        return clientService.update(id, request);
    }

    /**
     * Активация клиента.
     */
    @PostMapping("/{id}/activate")
    public ClientDto activate(@PathVariable Long id) {
        return clientService.activate(id);
    }

    /**
     * Деактивация клиента.
     */
    @PostMapping("/{id}/deactivate")
    public ClientDto deactivate(@PathVariable Long id) {
        return clientService.deactivate(id);
    }

    /**
     * Текущая суммарная прибыль клиента.
     */
    @GetMapping("/{id}/profit")
    public BigDecimal profit(@PathVariable Long id) {
        return clientService.totalProfit(id);
    }

    /**
     * Все заказы, в которых клиент участвует как поставщик или потребитель.
     */
    @GetMapping("/{id}/orders")
    public List<OrderDto> ordersOfClient(@PathVariable Long id) {
        return orderService.listByClient(id);
    }
}
