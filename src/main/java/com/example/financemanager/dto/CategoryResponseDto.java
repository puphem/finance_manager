package com.example.financemanager.dto;

import lombok.Data;

import java.util.List;

@Data
public class CategoryResponseDto {
    private Long id;
    private String name;
    private String color;
    private String icon;
    private List<SubcategoryResponseDto> subcategories;
}
