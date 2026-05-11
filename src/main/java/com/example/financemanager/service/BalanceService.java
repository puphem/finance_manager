package com.example.financemanager.service;

import com.example.financemanager.dto.BalanceSummaryDto;
import com.example.financemanager.entity.User;
import com.example.financemanager.repository.ExpenseRepository;
import com.example.financemanager.repository.IncomeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;

@Service
@RequiredArgsConstructor
public class BalanceService {

    private final IncomeRepository incomeRepository;
    private final ExpenseRepository expenseRepository;
    private final CurrentUserResolver currentUserResolver;

    public BalanceSummaryDto getSummary(String period) {
        User user = currentUserResolver.getCurrentUser();
        LocalDate today = LocalDate.now();
        LocalDate startDate;
        LocalDate endDate = today;

        if ("day".equalsIgnoreCase(period)) {
            startDate = today;
        } else if ("week".equalsIgnoreCase(period)) {
            startDate = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        } else if ("month".equalsIgnoreCase(period)) {
            startDate = today.with(TemporalAdjusters.firstDayOfMonth());
        } else {
            startDate = LocalDate.of(1970, 1, 1);
            endDate = LocalDate.of(2100, 1, 1);
        }

        BigDecimal totalIncomeForPeriod = incomeRepository.sumAmountByUserAndDateBetween(user, startDate, endDate);
        BigDecimal totalExpenseForPeriod = expenseRepository.sumAmountByUserAndDateBetween(user, startDate, endDate);
        BigDecimal netPeriodResult = totalIncomeForPeriod.subtract(totalExpenseForPeriod);

        BigDecimal totalIncomeAllTime = incomeRepository.sumAmountByUserAndDateBetween(user, LocalDate.of(1970, 1, 1), LocalDate.of(2100, 1, 1));
        BigDecimal totalExpenseAllTime = expenseRepository.sumAmountByUserAndDateBetween(user, LocalDate.of(1970, 1, 1), LocalDate.of(2100, 1, 1));
        BigDecimal absoluteBalance = totalIncomeAllTime.subtract(totalExpenseAllTime);

        return new BalanceSummaryDto(totalIncomeForPeriod, totalExpenseForPeriod, netPeriodResult, absoluteBalance);
    }
}
