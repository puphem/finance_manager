package com.example.financemanager.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SubcategoryExpenseDto {
    private String subcategoryName;
    private String categoryColor;
    private BigDecimal totalAmount;
}
