package com.example.financemanager.controller;

import com.example.financemanager.dto.BalanceSummaryDto;
import com.example.financemanager.service.BalanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/balance")
@RequiredArgsConstructor
public class BalanceController {

    private final BalanceService balanceService;

    @GetMapping("/summary")
    public ResponseEntity<BalanceSummaryDto> getSummary(@RequestParam(defaultValue = "month") String period) {
        return ResponseEntity.ok(balanceService.getSummary(period));
    }
}
