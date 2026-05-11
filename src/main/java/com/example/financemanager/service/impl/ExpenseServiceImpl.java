package com.example.financemanager.service.impl;

import com.example.financemanager.dto.CategoryExpenseDto;
import com.example.financemanager.dto.ExpenseRequestDto;
import com.example.financemanager.dto.ExpenseResponseDto;
import com.example.financemanager.dto.SubcategoryExpenseDto;
import com.example.financemanager.entity.Category;
import com.example.financemanager.entity.Expense;
import com.example.financemanager.entity.Subcategory;
import com.example.financemanager.entity.User;
import com.example.financemanager.exception.ResourceNotFoundException;
import com.example.financemanager.mapper.ExpenseMapper;
import com.example.financemanager.repository.CategoryRepository;
import com.example.financemanager.repository.ExpenseRepository;
import com.example.financemanager.repository.SubcategoryRepository;
import com.example.financemanager.service.CategoryAssignmentService;
import com.example.financemanager.service.CurrentUserResolver;
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
    private final SubcategoryRepository subcategoryRepository;
    private final CategoryAssignmentService categoryAssignmentService;
    private final ExpenseMapper expenseMapper;
    private final CurrentUserResolver currentUserResolver;

    @Override
    @Transactional
    public ExpenseResponseDto createExpense(ExpenseRequestDto expenseDto) {
        User user = currentUserResolver.getCurrentUser();
        Category category = categoryRepository.findById(expenseDto.getCategoryId())
                .filter(c -> c.getUser().getId().equals(user.getId()))
                .orElseThrow(() -> new ResourceNotFoundException("Категория с ID " + expenseDto.getCategoryId() + " не найдена."));

        Expense expense = expenseMapper.toEntity(expenseDto);
        expense.setCategory(category);
        expense.setUser(user);
        applySubcategory(expense, expenseDto.getSubcategoryId(), category, user);

        expenseRepository.save(expense);
        return expenseMapper.toResponseDto(expense);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExpenseResponseDto> getAllExpenses(String period) {
        User user = currentUserResolver.getCurrentUser();
        LocalDate today = LocalDate.now();
        LocalDate startDate;

        if ("day".equalsIgnoreCase(period)) {
            startDate = today;
        } else if ("week".equalsIgnoreCase(period)) {
            startDate = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        } else if ("month".equalsIgnoreCase(period)) {
            startDate = today.with(TemporalAdjusters.firstDayOfMonth());
        } else {
            return expenseRepository.findAllByUserOrderByDateDesc(user).stream()
                    .map(expenseMapper::toResponseDto)
                    .collect(Collectors.toList());
        }

        return expenseRepository.findByUserAndDateBetweenOrderByDateDesc(user, startDate, today).stream()
                .map(expenseMapper::toResponseDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public ExpenseResponseDto getExpenseById(Long id) {
        User user = currentUserResolver.getCurrentUser();
        Expense expense = expenseRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new ResourceNotFoundException("Расход с ID " + id + " не найден."));
        return expenseMapper.toResponseDto(expense);
    }

    @Override
    @Transactional
    public ExpenseResponseDto updateExpense(Long id, ExpenseRequestDto expenseDto) {
        User user = currentUserResolver.getCurrentUser();
        Expense expense = expenseRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new ResourceNotFoundException("Расход с ID " + id + " не найден."));

        Category category = categoryRepository.findById(expenseDto.getCategoryId())
                .filter(c -> c.getUser().getId().equals(user.getId()))
                .orElseThrow(() -> new ResourceNotFoundException("Категория с ID " + expenseDto.getCategoryId() + " не найдена."));

        if (expense.getReceipt() == null) {
            expense.setAmount(expenseDto.getAmount());
        } else if (expense.getAmount().compareTo(expenseDto.getAmount()) != 0) {
            throw new IllegalStateException("Сумму расхода, добавленного из чека, менять нельзя.");
        }
        expense.setDate(expenseDto.getDate());
        expense.setDescription(expenseDto.getDescription());
        expense.setCategory(category);
        applySubcategory(expense, expenseDto.getSubcategoryId(), category, user);

        Expense updatedExpense = expenseRepository.save(expense);
        return expenseMapper.toResponseDto(updatedExpense);
    }

    @Override
    @Transactional
    public void deleteExpense(Long id) {
        User user = currentUserResolver.getCurrentUser();
        if (!expenseRepository.existsByIdAndUser(id, user)) {
            throw new ResourceNotFoundException("Расход с ID " + id + " не найден.");
        }
        expenseRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryExpenseDto> getCategoryExpenseSummary(String period) {
        User user = currentUserResolver.getCurrentUser();
        LocalDate[] range = resolveDateRange(period);
        return expenseRepository.findCategoryExpensesByUserAndDateBetween(user, range[0], range[1]);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SubcategoryExpenseDto> getSubcategoryExpenseSummary(Long categoryId, String period) {
        User user = currentUserResolver.getCurrentUser();
        if (!categoryRepository.existsByIdAndUser(categoryId, user)) {
            throw new ResourceNotFoundException("Категория с ID " + categoryId + " не найдена.");
        }
        LocalDate[] range = resolveDateRange(period);
        return expenseRepository.findSubcategoryExpensesByUserAndCategoryAndDateBetween(user, categoryId, range[0], range[1]);
    }

    private LocalDate[] resolveDateRange(String period) {
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
        return new LocalDate[]{startDate, endDate};
    }

    private void applySubcategory(Expense expense, Long subcategoryId, Category category, User user) {
        if (subcategoryId != null) {
            Subcategory subcategory = subcategoryRepository.findByIdAndCategoryUser(subcategoryId, user)
                    .filter(s -> s.getCategory().getId().equals(category.getId()))
                    .orElseThrow(() -> new ResourceNotFoundException("Подкатегория с ID " + subcategoryId + " не найдена в выбранной категории."));
            expense.setSubcategory(subcategory);
            return;
        }

        Subcategory autoDetectedSubcategory = categoryAssignmentService.suggestSubcategory(expense.getDescription(), category, user);
        expense.setSubcategory(autoDetectedSubcategory);
    }
}
