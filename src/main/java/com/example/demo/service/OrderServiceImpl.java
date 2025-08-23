package com.example.demo.service;

import com.example.demo.dto.CreateOrderRequest;
import com.example.demo.dto.OrderDto;
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
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderServiceImpl implements OrderService {

    private static final BigDecimal MIN_ALLOWED_PROFIT = new BigDecimal("-1000");

    private final OrderRepository orderRepository;
    private final ClientRepository clientRepository;

    @Override
    @Transactional
    public OrderDto create(CreateOrderRequest request) {
        validateCreateRequest(request);

        Client supplier = clientRepository.findById(request.getSupplierId())
                .orElseThrow(() -> new NotFoundException("Supplier not found: id=" + request.getSupplierId()));
        Client consumer = clientRepository.findById(request.getConsumerId())
                .orElseThrow(() -> new NotFoundException("Consumer not found: id=" + request.getConsumerId()));

        if (!supplier.isActive()) {
            throw new IllegalStateException("Supplier is inactive: id=" + supplier.getId());
        }
        if (!consumer.isActive()) {
            throw new IllegalStateException("Consumer is inactive: id=" + consumer.getId());
        }
        if (supplier.getId().equals(consumer.getId())) {
            throw new IllegalArgumentException("Supplier and consumer must be different clients");
        }

        BigDecimal price = request.getPrice();
        BigDecimal newSupplierProfit = supplier.getTotalProfit().add(price);
        BigDecimal newConsumerProfit = consumer.getTotalProfit().subtract(price);

        if (newSupplierProfit.compareTo(MIN_ALLOWED_PROFIT) < 0) {
            throw new IllegalStateException("Supplier profit would drop below " + MIN_ALLOWED_PROFIT);
        }
        if (newConsumerProfit.compareTo(MIN_ALLOWED_PROFIT) < 0) {
            throw new IllegalStateException("Consumer profit would drop below " + MIN_ALLOWED_PROFIT);
        }

        LocalDateTime startedAt = request.getStartedAt() != null ? request.getStartedAt() : LocalDateTime.now();
        LocalDateTime finishedAt = request.getFinishedAt();

        if (finishedAt != null && finishedAt.isBefore(startedAt)) {
            throw new IllegalArgumentException("finishedAt must be after or equal to startedAt");
        }

        Order order = Order.builder()
                .title(request.getTitle().trim())
                .supplier(supplier)
                .consumer(consumer)
                .price(price)
                .startedAt(startedAt)
                .finishedAt(finishedAt)
                .build();

        Order saved = orderRepository.save(order);
        return toDto(saved);
    }

    @Override
    public OrderDto getById(Long id) {
        Order o = orderRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Order not found: id=" + id));
        return toDto(o);
    }

    @Override
    public List<OrderDto> listAll() {
        return orderRepository.findAll().stream()
                .sorted(Comparator.comparing(Order::getCreatedAt))
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<OrderDto> listByClient(Long clientId) {
        Client c = clientRepository.findById(clientId)
                .orElseThrow(() -> new NotFoundException("Client not found: id=" + clientId));

        return orderRepository.findAll().stream()
                .filter(o -> (o.getSupplier() != null && o.getSupplier().getId().equals(c.getId()))
                        || (o.getConsumer() != null && o.getConsumer().getId().equals(c.getId())))
                .sorted(Comparator.comparing(Order::getCreatedAt))
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    private void validateCreateRequest(CreateOrderRequest r) {
        if (r == null) throw new IllegalArgumentException("CreateOrderRequest must not be null");
        if (r.getTitle() == null || r.getTitle().trim().length() < 3) {
            throw new IllegalArgumentException("Order title must be at least 3 characters");
        }
        if (r.getSupplierId() == null) {
            throw new IllegalArgumentException("supplierId must not be null");
        }
        if (r.getConsumerId() == null) {
            throw new IllegalArgumentException("consumerId must not be null");
        }
        if (r.getPrice() == null || r.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("price must be > 0");
        }
    }

    private OrderDto toDto(Order o) {
        return OrderDto.builder()
                .id(o.getId())
                .title(o.getTitle())
                .supplierId(o.getSupplier() != null ? o.getSupplier().getId() : null)
                .consumerId(o.getConsumer() != null ? o.getConsumer().getId() : null)
                .supplierName(o.getSupplier() != null ? o.getSupplier().getName() : null)
                .consumerName(o.getConsumer() != null ? o.getConsumer().getName() : null)
                .price(o.getPrice())
                .startedAt(o.getStartedAt())
                .finishedAt(o.getFinishedAt())
                .build();
    }
}