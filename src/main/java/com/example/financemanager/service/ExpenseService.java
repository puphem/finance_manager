package com.example.financemanager.service;

import com.example.financemanager.dto.CategoryExpenseDto;
import com.example.financemanager.dto.CategoryPredictionDto;
import com.example.financemanager.dto.ExpenseRequestDto;
import com.example.financemanager.dto.ExpenseResponseDto;
import com.example.financemanager.dto.SubcategoryExpenseDto;

import java.util.List;

public interface ExpenseService {
    ExpenseResponseDto createExpense(ExpenseRequestDto expenseDto);
    List<ExpenseResponseDto> getAllExpenses(String period);
    ExpenseResponseDto getExpenseById(Long id);
    ExpenseResponseDto updateExpense(Long id, ExpenseRequestDto expenseDto);
    void deleteExpense(Long id);
    List<CategoryExpenseDto> getCategoryExpenseSummary(String period);
    List<SubcategoryExpenseDto> getSubcategoryExpenseSummary(Long categoryId, String period);
    CategoryPredictionDto predictCategory(String description);
}
