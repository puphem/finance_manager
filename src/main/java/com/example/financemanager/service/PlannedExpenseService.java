package com.example.financemanager.service;

import com.example.financemanager.dto.PlannedExpenseConvertPreviewDto;
import com.example.financemanager.dto.PlannedExpenseRequestDto;
import com.example.financemanager.dto.PlannedExpenseResponseDto;
import com.example.financemanager.entity.*;
import com.example.financemanager.entity.enums.PlannedExpenseStatus;
import com.example.financemanager.exception.ResourceNotFoundException;
import com.example.financemanager.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PlannedExpenseService {

    private final PlannedExpenseRepository plannedExpenseRepository;
    private final CategoryRepository categoryRepository;
    private final SubcategoryRepository subcategoryRepository;
    private final ExpenseRepository expenseRepository;
    private final CategoryAssignmentService categoryAssignmentService;
    private final CurrentUserResolver currentUserResolver;

    @Transactional(readOnly = true)
    public List<PlannedExpenseResponseDto> list(String filter, PlannedExpenseStatus status) {
        User user = currentUserResolver.getCurrentUser();
        if (status != null) {
            return plannedExpenseRepository.findAllByUserAndStatusOrderByPlannedDateAsc(user, status).stream().map(this::toDto).toList();
        }

        LocalDate today = LocalDate.now();
        if ("today".equalsIgnoreCase(filter)) {
            return plannedExpenseRepository.findAllByUserAndPlannedDateBetweenOrderByPlannedDateAsc(user, today, today)
                    .stream().map(this::toDto).toList();
        }
        if ("week".equalsIgnoreCase(filter)) {
            return plannedExpenseRepository.findAllByUserAndPlannedDateBetweenOrderByPlannedDateAsc(user, today, today.plusDays(7))
                    .stream().map(this::toDto).toList();
        }
        if ("overdue".equalsIgnoreCase(filter)) {
            return plannedExpenseRepository.findAllByUserAndStatusAndPlannedDateBeforeOrderByPlannedDateAsc(user, PlannedExpenseStatus.NEW, today)
                    .stream().map(this::toDto).toList();
        }

        return plannedExpenseRepository.findAllByUserOrderByPlannedDateAsc(user).stream().map(this::toDto).toList();
    }

    @Transactional
    public PlannedExpenseResponseDto create(PlannedExpenseRequestDto request) {
        User user = currentUserResolver.getCurrentUser();
        PlannedExpense item = new PlannedExpense();
        applyRequest(item, request, user);
        item.setUser(user);
        plannedExpenseRepository.save(item);
        return toDto(item);
    }

    @Transactional
    public PlannedExpenseResponseDto update(Long id, PlannedExpenseRequestDto request) {
        User user = currentUserResolver.getCurrentUser();
        PlannedExpense item = plannedExpenseRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new ResourceNotFoundException("Планируемая трата с ID " + id + " не найдена."));
        applyRequest(item, request, user);
        plannedExpenseRepository.save(item);
        return toDto(item);
    }

    @Transactional
    public void delete(Long id) {
        User user = currentUserResolver.getCurrentUser();
        PlannedExpense item = plannedExpenseRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new ResourceNotFoundException("Планируемая трата с ID " + id + " не найдена."));
        plannedExpenseRepository.delete(item);
    }

    @Transactional(readOnly = true)
    public PlannedExpenseConvertPreviewDto previewConvert(Long id) {
        User user = currentUserResolver.getCurrentUser();
        PlannedExpense item = plannedExpenseRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new ResourceNotFoundException("Планируемая трата с ID " + id + " не найдена."));

        Category category = item.getCategory() != null ? item.getCategory() : categoryAssignmentService.suggestCategory(item.getTitle(), user);
        Subcategory subcategory = item.getSubcategory() != null
                ? item.getSubcategory()
                : categoryAssignmentService.suggestSubcategory(item.getTitle(), category, user);

        return new PlannedExpenseConvertPreviewDto(
                item.getTitle(),
                item.getExpectedAmount(),
                item.getPlannedDate(),
                category == null ? null : category.getId(),
                category == null ? null : category.getName(),
                subcategory == null ? null : subcategory.getId(),
                subcategory == null ? null : subcategory.getName()
        );
    }

    @Transactional
    public void convertToExpense(Long id) {
        User user = currentUserResolver.getCurrentUser();
        PlannedExpense item = plannedExpenseRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new ResourceNotFoundException("Планируемая трата с ID " + id + " не найдена."));

        Category category = item.getCategory() != null ? item.getCategory() : categoryAssignmentService.suggestCategory(item.getTitle(), user);
        if (category == null) {
            throw new IllegalStateException("Не удалось определить категорию для конвертации.");
        }

        Expense expense = new Expense();
        expense.setUser(user);
        expense.setAmount(item.getExpectedAmount());
        expense.setDate(item.getPlannedDate());
        expense.setDescription(item.getTitle());
        expense.setCategory(category);

        Subcategory subcategory = item.getSubcategory();
        if (subcategory == null) {
            subcategory = categoryAssignmentService.suggestSubcategory(item.getTitle(), category, user);
        }
        expense.setSubcategory(subcategory);
        expenseRepository.save(expense);

        item.setStatus(PlannedExpenseStatus.DONE);
        plannedExpenseRepository.save(item);
    }

    private void applyRequest(PlannedExpense item, PlannedExpenseRequestDto request, User user) {
        item.setTitle(request.getTitle().trim());
        item.setExpectedAmount(request.getExpectedAmount());
        item.setPlannedDate(request.getPlannedDate());
        item.setPriority(request.getPriority());
        item.setStatus(request.getStatus());
        item.setNotes(request.getNotes());

        Category category = null;
        if (request.getCategoryId() != null) {
            category = categoryRepository.findById(request.getCategoryId())
                    .filter(candidate -> candidate.getUser().getId().equals(user.getId()))
                    .orElseThrow(() -> new ResourceNotFoundException("Категория с ID " + request.getCategoryId() + " не найдена."));
        }

        Subcategory subcategory = null;
        if (request.getSubcategoryId() != null) {
            subcategory = subcategoryRepository.findByIdAndCategoryUser(request.getSubcategoryId(), user)
                    .orElseThrow(() -> new ResourceNotFoundException("Подкатегория с ID " + request.getSubcategoryId() + " не найдена."));
            if (category != null && !subcategory.getCategory().getId().equals(category.getId())) {
                throw new IllegalArgumentException("Подкатегория не принадлежит выбранной категории.");
            }
            if (category == null) {
                category = subcategory.getCategory();
            }
        }

        item.setCategory(category);
        item.setSubcategory(subcategory);
    }

    private PlannedExpenseResponseDto toDto(PlannedExpense item) {
        return new PlannedExpenseResponseDto(
                item.getId(),
                item.getTitle(),
                item.getExpectedAmount(),
                item.getPlannedDate(),
                item.getPriority(),
                item.getStatus(),
                item.getNotes(),
                item.getCategory() == null ? null : item.getCategory().getId(),
                item.getCategory() == null ? null : item.getCategory().getName(),
                item.getSubcategory() == null ? null : item.getSubcategory().getId(),
                item.getSubcategory() == null ? null : item.getSubcategory().getName()
        );
    }
}
