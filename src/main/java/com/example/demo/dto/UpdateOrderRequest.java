package com.example.demo.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateOrderRequest {

    @NotBlank(message = "title must not be blank")
    @Size(max = 300, message = "title length must be <= 300")
    private String title;

    @NotNull(message = "price is required")
    @DecimalMin(value = "1", inclusive = true, message = "price must be >= 1")
    private BigDecimal price;
}