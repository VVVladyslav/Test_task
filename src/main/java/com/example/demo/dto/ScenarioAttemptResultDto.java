package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScenarioAttemptResultDto {
    private int index;
    private boolean success;
    private Integer httpStatus;
    private String message;
    private Long orderId;
}