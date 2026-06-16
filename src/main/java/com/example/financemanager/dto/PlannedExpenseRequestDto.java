package com.example.financemanager.dto;

import com.example.financemanager.entity.enums.PlannedExpensePriority;
import com.example.financemanager.entity.enums.PlannedExpenseStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class PlannedExpenseRequestDto {
    @NotBlank
    private String title;

    @NotNull
    @DecimalMin("0.01")
    private BigDecimal expectedAmount;

    @NotNull
    private LocalDate plannedDate;

    private PlannedExpensePriority priority = PlannedExpensePriority.MEDIUM;
    private PlannedExpenseStatus status = PlannedExpenseStatus.NEW;
    private String notes;
    private Long categoryId;
    private Long subcategoryId;
}
