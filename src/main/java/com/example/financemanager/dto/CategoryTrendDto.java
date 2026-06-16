package com.example.financemanager.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class CategoryTrendDto {
    private Long categoryId;
    private String categoryName;
    private BigDecimal currentAmount;
    private BigDecimal previousAmount;
    private BigDecimal growthAmount;
}
