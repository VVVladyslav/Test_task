package com.example.demo.repository;

import com.example.demo.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    @Query("""
           select coalesce(sum(case when o.supplier.id = :clientId then o.price else 0 end), 0)
                - coalesce(sum(case when o.consumer.id = :clientId then o.price else 0 end), 0)
           from Order o
           where o.supplier.id = :clientId or o.consumer.id = :clientId
           """)
    BigDecimal computeProfit(Long clientId);
    List<Order> findBySupplierIdOrConsumerId(Long supplierId, Long consumerId);
    Optional<Order> findByTitleIgnoreCaseAndSupplierIdAndConsumerId(String title, Long supplierId, Long consumerId);
    boolean existsByTitleIgnoreCaseAndSupplierIdAndConsumerId(String title, Long supplierId, Long consumerId);
}