package com.example.demo.service;

import com.example.demo.dto.CreateOrderRequest;
import com.example.demo.dto.OrderDto;
import com.example.demo.dto.UpdateOrderRequest;
import com.example.demo.exception.BadRequestException;
import com.example.demo.exception.ConflictException;
import com.example.demo.exception.NotFoundException;
import com.example.demo.model.Client;
import com.example.demo.model.Order;
import com.example.demo.repository.ClientRepository;
import com.example.demo.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
@Transactional
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final ClientRepository clientRepository;

    private OrderDto toDto(Order o) {
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

    private Client getClientOr404(Long id, String role) {
        return clientRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(role + " not found: id=" + id));
    }

    @Override
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public OrderDto create(CreateOrderRequest request) {
        if (request.getSupplierId().equals(request.getConsumerId())) {
            throw new BadRequestException("Supplier and consumer must be different");
        }
        if (request.getPrice() == null || request.getPrice().compareTo(BigDecimal.ONE) < 0) {
            throw new BadRequestException("Price must be positive and >= 1");
        }

        Client supplier0 = getClientOr404(request.getSupplierId(), "Supplier");
        Client consumer0 = getClientOr404(request.getConsumerId(), "Consumer");
        if (!supplier0.isActive()) throw new BadRequestException("Supplier is inactive: id=" + supplier0.getId());
        if (!consumer0.isActive()) throw new BadRequestException("Consumer is inactive: id=" + consumer0.getId());

        LocalDateTime started = LocalDateTime.now();
        long delayMillis = ThreadLocalRandom.current().nextLong(1_000, 10_001);
        try {
            Thread.sleep(delayMillis);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
        LocalDateTime finished = LocalDateTime.now();

        Client supplier = clientRepository.findByIdForUpdate(request.getSupplierId())
                .orElseThrow(() -> new NotFoundException("Supplier not found: id=" + request.getSupplierId()));
        Client consumer = clientRepository.findByIdForUpdate(request.getConsumerId())
                .orElseThrow(() -> new NotFoundException("Consumer not found: id=" + request.getConsumerId()));

        if (!supplier.isActive() || (supplier.getDeactivatedAt() != null && !finished.isBefore(supplier.getDeactivatedAt()))) {
            throw new BadRequestException("Supplier became inactive during processing");
        }
        if (!consumer.isActive() || (consumer.getDeactivatedAt() != null && !finished.isBefore(consumer.getDeactivatedAt()))) {
            throw new BadRequestException("Consumer became inactive during processing");
        }

        BigDecimal currentProfitConsumer = orderRepository.computeProfit(consumer.getId());
        BigDecimal profitAfter = currentProfitConsumer.subtract(request.getPrice());
        if (profitAfter.compareTo(BigDecimal.valueOf(-1000)) < 0) {
            throw new BadRequestException("Consumer profit would drop below -1000");
        }

        String normalizedTitle = request.getTitle().trim();
        orderRepository.findByTitleIgnoreCaseAndSupplierIdAndConsumerId(
                normalizedTitle, supplier.getId(), consumer.getId()
        ).ifPresent(o -> { throw new ConflictException("Order with the same title/supplier/consumer already exists"); });

        Order order = Order.builder()
                .title(normalizedTitle)
                .supplier(supplier)
                .consumer(consumer)
                .price(request.getPrice())
                .startedAt(started)
                .finishedAt(finished)
                .build();

        try {
            return toDto(orderRepository.save(order));
        } catch (DataIntegrityViolationException e) {
            // Гонка по уникальному бизнес-ключу (дубликат)
            throw new ConflictException("Order with the same title/supplier/consumer already exists");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public OrderDto getById(Long id) {
        Order o = orderRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Order not found: id=" + id));
        return toDto(o);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderDto> listAll() {
        return orderRepository.findAll().stream()
                .sorted(Comparator.comparing(Order::getId))
                .map(this::toDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderDto> listByClient(Long clientId) {
        return orderRepository.findBySupplierIdOrConsumerId(clientId, clientId).stream()
                .sorted(Comparator.comparing(Order::getId))
                .map(this::toDto)
                .toList();
    }

    @Override
    public OrderDto update(Long id, UpdateOrderRequest request) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Order not found: id=" + id));

        if (request.getPrice() == null || request.getPrice().compareTo(BigDecimal.ONE) < 0) {
            throw new BadRequestException("Price must be positive and >= 1");
        }

        String newTitle = request.getTitle().trim();
        if (!order.getTitle().equalsIgnoreCase(newTitle)) {
            orderRepository.findByTitleIgnoreCaseAndSupplierIdAndConsumerId(
                    newTitle, order.getSupplier().getId(), order.getConsumer().getId()
            ).ifPresent(o -> { throw new ConflictException("Order with the same title/supplier/consumer already exists"); });
            order.setTitle(newTitle);
        }

        order.setPrice(request.getPrice());
        return toDto(orderRepository.save(order));
    }

    @Override
    public void delete(Long id) {
        if (!orderRepository.existsById(id)) {
            throw new NotFoundException("Order not found: id=" + id);
        }
        orderRepository.deleteById(id);
    }
}