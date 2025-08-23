package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderDto {
    private Long id;

    private String title;

    private Long supplierId;
    private Long consumerId;

    private String supplierName;
    private String consumerName;

    private BigDecimal price;

    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
}