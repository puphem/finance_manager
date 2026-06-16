package com.example.financemanager.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CategoryPredictionDto {
    private Long categoryId;
    private String categoryName;
    private Long subcategoryId;
    private String subcategoryName;
    private double confidence;
    private String source;
}
