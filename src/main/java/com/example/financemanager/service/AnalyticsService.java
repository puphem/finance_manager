package com.example.financemanager.service;

import com.example.financemanager.dto.*;
import com.example.financemanager.entity.Expense;
import com.example.financemanager.entity.User;
import com.example.financemanager.repository.ExpenseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.*;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final ExpenseRepository expenseRepository;
    private final CurrentUserResolver currentUserResolver;

    @Transactional(readOnly = true)
    public AnalyticsOverviewDto getOverview(String period) {
        User user = currentUserResolver.getCurrentUser();
        LocalDate today = LocalDate.now();
        DateRange currentRange = resolveDateRange(period, today);
        int spanDays = (int) (currentRange.end.toEpochDay() - currentRange.start.toEpochDay() + 1);
        DateRange previousRange = new DateRange(currentRange.start.minusDays(spanDays), currentRange.start.minusDays(1));

        List<Expense> currentExpenses = expenseRepository.findByUserAndDateBetweenOrderByDateDesc(user, currentRange.start, currentRange.end);
        List<Expense> previousExpenses = expenseRepository.findByUserAndDateBetweenOrderByDateDesc(user, previousRange.start, previousRange.end);

        BigDecimal currentTotal = sum(currentExpenses);
        BigDecimal previousTotal = sum(previousExpenses);
        BigDecimal delta = currentTotal.subtract(previousTotal);
        BigDecimal deltaPercent = previousTotal.compareTo(BigDecimal.ZERO) == 0
                ? BigDecimal.ZERO
                : delta.multiply(BigDecimal.valueOf(100)).divide(previousTotal, 2, RoundingMode.HALF_UP);

        PeriodComparisonDto comparison = new PeriodComparisonDto(currentTotal, previousTotal, delta, deltaPercent);
        List<CategoryTrendDto> topGrowth = buildCategoryTrends(currentExpenses, previousExpenses);

        BigDecimal baseline = calculateBaseline(user, today, spanDays, 4);
        List<AnalyticsAlertDto> alerts = buildAlerts(currentTotal, baseline, topGrowth);
        List<RecommendationDto> recommendations = buildRecommendations(comparison, topGrowth, alerts);

        return new AnalyticsOverviewDto(comparison, topGrowth, alerts, recommendations);
    }

    private List<CategoryTrendDto> buildCategoryTrends(List<Expense> currentExpenses, List<Expense> previousExpenses) {
        Map<Long, BigDecimal> current = aggregateByCategory(currentExpenses);
        Map<Long, BigDecimal> previous = aggregateByCategory(previousExpenses);
        Map<Long, String> names = new HashMap<>();

        currentExpenses.forEach(expense -> {
            if (expense.getCategory() != null) {
                names.put(expense.getCategory().getId(), expense.getCategory().getName());
            }
        });
        previousExpenses.forEach(expense -> {
            if (expense.getCategory() != null) {
                names.putIfAbsent(expense.getCategory().getId(), expense.getCategory().getName());
            }
        });

        Set<Long> keys = new HashSet<>();
        keys.addAll(current.keySet());
        keys.addAll(previous.keySet());

        return keys.stream()
                .map(categoryId -> {
                    BigDecimal currentAmount = current.getOrDefault(categoryId, BigDecimal.ZERO);
                    BigDecimal previousAmount = previous.getOrDefault(categoryId, BigDecimal.ZERO);
                    return new CategoryTrendDto(
                            categoryId,
                            names.getOrDefault(categoryId, "Без категории"),
                            currentAmount,
                            previousAmount,
                            currentAmount.subtract(previousAmount)
                    );
                })
                .sorted((left, right) -> right.getGrowthAmount().compareTo(left.getGrowthAmount()))
                .limit(5)
                .toList();
    }

    private BigDecimal calculateBaseline(User user, LocalDate today, int spanDays, int historyPeriods) {
        List<BigDecimal> totals = new ArrayList<>();
        LocalDate end = today.minusDays(spanDays);
        for (int i = 0; i < historyPeriods; i++) {
            LocalDate start = end.minusDays(spanDays - 1);
            totals.add(sum(expenseRepository.findByUserAndDateBetweenOrderByDateDesc(user, start, end)));
            end = start.minusDays(1);
        }
        return totals.stream().reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(Math.max(1, totals.size())), 2, RoundingMode.HALF_UP);
    }

    private List<AnalyticsAlertDto> buildAlerts(BigDecimal currentTotal, BigDecimal baseline, List<CategoryTrendDto> trends) {
        List<AnalyticsAlertDto> alerts = new ArrayList<>();
        if (baseline.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal threshold = baseline.multiply(BigDecimal.valueOf(1.35));
            if (currentTotal.compareTo(threshold) > 0) {
                alerts.add(new AnalyticsAlertDto("ANOMALY", "Траты выше базового уровня более чем на 35%"));
            }
        }

        trends.stream()
                .filter(trend -> trend.getGrowthAmount().compareTo(BigDecimal.ZERO) > 0)
                .limit(2)
                .forEach(trend -> alerts.add(new AnalyticsAlertDto(
                        "GROWTH",
                        "Категория «" + trend.getCategoryName() + "» показывает рост на " + trend.getGrowthAmount()
                )));
        return alerts;
    }

    private List<RecommendationDto> buildRecommendations(PeriodComparisonDto comparison, List<CategoryTrendDto> trends, List<AnalyticsAlertDto> alerts) {
        List<RecommendationDto> recommendations = new ArrayList<>();
        if (comparison.getDelta().compareTo(BigDecimal.ZERO) > 0) {
            recommendations.add(new RecommendationDto("warning", "Расходы выросли относительно прошлого периода. Проверьте крупнейшие категории роста."));
        } else {
            recommendations.add(new RecommendationDto("info", "Расходы стабильны или снизились относительно прошлого периода."));
        }

        trends.stream()
                .filter(trend -> trend.getGrowthAmount().compareTo(BigDecimal.ZERO) > 0)
                .findFirst()
                .ifPresent(trend -> recommendations.add(new RecommendationDto(
                        "tip",
                        "Фокус контроля: «" + trend.getCategoryName() + "», прирост " + trend.getGrowthAmount()
                )));

        if (alerts.stream().noneMatch(alert -> "ANOMALY".equals(alert.getType()))) {
            recommendations.add(new RecommendationDto("success", "Сильных аномалий не обнаружено."));
        }

        return recommendations;
    }

    private Map<Long, BigDecimal> aggregateByCategory(List<Expense> expenses) {
        Map<Long, BigDecimal> totals = new HashMap<>();
        for (Expense expense : expenses) {
            if (expense.getCategory() == null) {
                continue;
            }
            Long categoryId = expense.getCategory().getId();
            totals.put(categoryId, totals.getOrDefault(categoryId, BigDecimal.ZERO).add(expense.getAmount()));
        }
        return totals;
    }

    private BigDecimal sum(List<Expense> expenses) {
        return expenses.stream()
                .map(Expense::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private DateRange resolveDateRange(String period, LocalDate today) {
        if ("day".equalsIgnoreCase(period)) {
            return new DateRange(today, today);
        }
        if ("week".equalsIgnoreCase(period)) {
            return new DateRange(today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)), today);
        }
        if ("all".equalsIgnoreCase(period)) {
            return new DateRange(LocalDate.of(1970, 1, 1), today);
        }
        return new DateRange(today.with(TemporalAdjusters.firstDayOfMonth()), today);
    }

    private record DateRange(LocalDate start, LocalDate end) {
    }
}
