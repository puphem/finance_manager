package com.example.financemanager.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL) // Не включать в JSON поля с null (например, receipt)
public class ExpenseResponseDto {
    private Long id;
    private BigDecimal amount;
    private LocalDate date;
    private String description;
    private CategoryResponseDto category;
    private SubcategoryResponseDto subcategory;

    private ReceiptInfoInExpenseDto receipt;
}
