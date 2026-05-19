package com.example.financemanager.service;

import com.example.financemanager.dto.CategoryRequestDto;
import com.example.financemanager.dto.CategoryResponseDto;
import com.example.financemanager.dto.SubcategoryRequestDto;
import com.example.financemanager.dto.SubcategoryResponseDto;

import java.util.List;

public interface CategoryService {
    CategoryResponseDto createCategory(CategoryRequestDto categoryDto);
    SubcategoryResponseDto createSubcategory(SubcategoryRequestDto subcategoryDto);
    List<CategoryResponseDto> getAllCategories();
    CategoryResponseDto updateCategory(Long id, CategoryRequestDto categoryDto);
    void deleteCategory(Long id);
}
