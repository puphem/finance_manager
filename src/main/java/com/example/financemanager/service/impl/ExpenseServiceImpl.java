package com.example.financemanager.service.impl;

import com.example.financemanager.dto.CategoryExpenseDto;
import com.example.financemanager.dto.ExpenseRequestDto;
import com.example.financemanager.dto.ExpenseResponseDto;
import com.example.financemanager.entity.Category;
import com.example.financemanager.entity.Expense;
import com.example.financemanager.exception.ResourceNotFoundException;
import com.example.financemanager.mapper.ExpenseMapper;
import com.example.financemanager.repository.CategoryRepository;
import com.example.financemanager.repository.ExpenseRepository;
import com.example.financemanager.service.ExpenseService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExpenseServiceImpl implements ExpenseService {

    private final ExpenseRepository expenseRepository;
    private final CategoryRepository categoryRepository;
    private final ExpenseMapper expenseMapper;

    @Override
    @Transactional
    public ExpenseResponseDto createExpense(ExpenseRequestDto expenseDto) {
        Category category = categoryRepository.findById(expenseDto.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Категория с ID " + expenseDto.getCategoryId() + " не найдена."));

        Expense expense = expenseMapper.toEntity(expenseDto);
        expense.setCategory(category);

        expenseRepository.save(expense);
        return expenseMapper.toResponseDto(expense);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExpenseResponseDto> getAllExpenses(String period) {
        LocalDate today = LocalDate.now();
        LocalDate startDate;

        if ("day".equalsIgnoreCase(period)) {
            startDate = today;
        } else if ("week".equalsIgnoreCase(period)) {
            startDate = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        } else if ("month".equalsIgnoreCase(period)) {
            startDate = today.with(TemporalAdjusters.firstDayOfMonth());
        } else {
            return expenseRepository.findAllByOrderByDateDesc().stream()
                    .map(expenseMapper::toResponseDto)
                    .collect(Collectors.toList());
        }

        return expenseRepository.findByDateBetweenOrderByDateDesc(startDate, today).stream()
                .map(expenseMapper::toResponseDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public ExpenseResponseDto getExpenseById(Long id) {
        Expense expense = expenseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Расход с ID " + id + " не найден."));
        return expenseMapper.toResponseDto(expense);
    }

    @Override
    @Transactional
    public ExpenseResponseDto updateExpense(Long id, ExpenseRequestDto expenseDto) {
        Expense expense = expenseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Расход с ID " + id + " не найден."));

        Category category = categoryRepository.findById(expenseDto.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Категория с ID " + expenseDto.getCategoryId() + " не найдена."));

        if (expense.getReceipt() == null) {
            expense.setAmount(expenseDto.getAmount());
        } else if (expenseDto.getAmount() != null && expense.getAmount().compareTo(expenseDto.getAmount()) != 0) {
            throw new IllegalStateException("Сумму расхода, добавленного из чека, менять нельзя.");
        }
        expense.setDate(expenseDto.getDate());
        expense.setDescription(expenseDto.getDescription());
        expense.setCategory(category);

        Expense updatedExpense = expenseRepository.save(expense);
        return expenseMapper.toResponseDto(updatedExpense);
    }

    @Override
    @Transactional
    public void deleteExpense(Long id) {
        if (!expenseRepository.existsById(id)) {
            throw new ResourceNotFoundException("Расход с ID " + id + " не найден.");
        }
        expenseRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryExpenseDto> getCategoryExpenseSummary(String period) {
        LocalDate today = LocalDate.now();
        LocalDate startDate;
        LocalDate endDate = today;

        if ("day".equalsIgnoreCase(period)) {
            startDate = today;
        } else if ("week".equalsIgnoreCase(period)) {
            startDate = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        } else if ("month".equalsIgnoreCase(period)) {
            startDate = today.with(TemporalAdjusters.firstDayOfMonth());
        } else {
            startDate = LocalDate.of(1970, 1, 1);
            endDate = LocalDate.of(2100, 1, 1);
        }

        return expenseRepository.findCategoryExpensesByDateBetween(startDate, endDate);
    }
}
