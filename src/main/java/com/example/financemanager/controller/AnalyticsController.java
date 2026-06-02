package com.example.financemanager.controller;

import com.example.financemanager.dto.AnalyticsOverviewDto;
import com.example.financemanager.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping("/overview")
    public ResponseEntity<AnalyticsOverviewDto> overview(@RequestParam(defaultValue = "month") String period) {
        return ResponseEntity.ok(analyticsService.getOverview(period));
    }
}
