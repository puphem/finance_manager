package com.example.financemanager.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BalanceSummaryDto {
    private BigDecimal totalIncomeForPeriod;
    private BigDecimal totalExpenseForPeriod;
    private BigDecimal netPeriodResult; // Чистый итог за период
    private BigDecimal absoluteBalance; // Абсолютный баланс за все время
}
