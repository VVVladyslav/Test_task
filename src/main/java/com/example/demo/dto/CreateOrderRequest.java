package com.example.demo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class CreateOrderRequest {

    @NotBlank
    @Size(min = 3, max = 200)
    private String title;

    @NotNull
    private Long supplierId;

    @NotNull
    private Long consumerId;

    @NotNull
    @Positive
    private BigDecimal price;

    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
}
