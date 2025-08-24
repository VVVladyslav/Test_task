package com.example.demo.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScenarioSummaryDto {
    private String scenario;
    private int requested;
    private int succeeded;
    private int failed;
    private List<ScenarioAttemptResultDto> attempts;
}