package com.example.demo.simulator;

import com.example.demo.dto.ScenarioSummaryDto;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/scenarios")
public class ScenarioController {

    private final ScenarioService scenarioService;

    @PostMapping("/duplicates")
    public ScenarioSummaryDto duplicates(@RequestParam(defaultValue = "10") int n)
            throws InterruptedException {
        return scenarioService.runDuplicates(n);
    }

    @PostMapping("/descending")
    public ScenarioSummaryDto descending(@RequestParam(defaultValue = "10") int n)
            throws InterruptedException {
        return scenarioService.runDescending(n);
    }

    @PostMapping("/deactivation")
    public ScenarioSummaryDto deactivation(@RequestParam(defaultValue = "10") int n,
                                           @RequestParam(name = "deactivateAfterMs", defaultValue = "1000") long deactivateAfterMs)
            throws InterruptedException {
        return scenarioService.runDeactivationRace(n, deactivateAfterMs);
    }
}