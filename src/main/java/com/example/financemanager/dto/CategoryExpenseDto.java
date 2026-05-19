package com.example.financemanager.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CategoryExpenseDto {
    private Long categoryId;
    private String categoryName;
    private String categoryColor;
    private BigDecimal totalAmount;
}
