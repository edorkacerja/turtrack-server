package com.turtrack.server.controller.helcim;

import com.turtrack.server.service.helcim.PlanService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/plans")
@Slf4j
public class PlanController {

    private final PlanService planService;

    /**
     * Fetch all available subscription plans
     */
    @GetMapping
    public ResponseEntity<?> getPlans() {
        try {
            List<Map<String, Object>> plans = planService.getAvailablePlans();
            return ResponseEntity.ok(Map.of("plans", plans));
        } catch (IOException e) {
            log.error("Error fetching subscription plans", e);
            return ResponseEntity.status(500).body(Map.of("error", "Failed to fetch subscription plans"));
        }
    }
}
