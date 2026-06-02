package com.example.financemanager.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class PeriodComparisonDto {
    private BigDecimal currentExpense;
    private BigDecimal previousExpense;
    private BigDecimal delta;
    private BigDecimal deltaPercent;
}
