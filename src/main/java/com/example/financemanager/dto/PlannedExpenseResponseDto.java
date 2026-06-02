package com.example.financemanager.dto;

import com.example.financemanager.entity.enums.PlannedExpensePriority;
import com.example.financemanager.entity.enums.PlannedExpenseStatus;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@AllArgsConstructor
public class PlannedExpenseResponseDto {
    private Long id;
    private String title;
    private BigDecimal expectedAmount;
    private LocalDate plannedDate;
    private PlannedExpensePriority priority;
    private PlannedExpenseStatus status;
    private String notes;
    private Long categoryId;
    private String categoryName;
    private Long subcategoryId;
    private String subcategoryName;
}
