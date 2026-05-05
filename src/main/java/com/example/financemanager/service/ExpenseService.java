package com.example.financemanager.service;

import com.example.financemanager.dto.CategoryExpenseDto;
import com.example.financemanager.dto.ExpenseRequestDto;
import com.example.financemanager.dto.ExpenseResponseDto;

import java.util.List;

public interface ExpenseService {
    ExpenseResponseDto createExpense(ExpenseRequestDto expenseDto);
    List<ExpenseResponseDto> getAllExpenses(String period);
    ExpenseResponseDto updateExpense(Long id, ExpenseRequestDto expenseDto);
    void deleteExpense(Long id);
    List<CategoryExpenseDto> getCategoryExpenseSummary(String period);
}
