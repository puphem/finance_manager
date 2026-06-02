package com.example.financemanager.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class AnalyticsOverviewDto {
    private PeriodComparisonDto periodComparison;
    private List<CategoryTrendDto> topGrowthCategories;
    private List<AnalyticsAlertDto> alerts;
    private List<RecommendationDto> recommendations;
}
