package com.example.financemanager.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@AllArgsConstructor
public class PlannedExpenseConvertPreviewDto {
    private String description;
    private BigDecimal amount;
    private LocalDate date;
    private Long suggestedCategoryId;
    private String suggestedCategoryName;
    private Long suggestedSubcategoryId;
    private String suggestedSubcategoryName;
}
