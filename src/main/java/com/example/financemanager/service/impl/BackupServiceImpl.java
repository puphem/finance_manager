package com.example.financemanager.service.impl;

import com.example.financemanager.dto.BackupSnapshotDto;
import com.example.financemanager.entity.Category;
import com.example.financemanager.entity.Expense;
import com.example.financemanager.entity.Income;
import com.example.financemanager.entity.Subcategory;
import com.example.financemanager.entity.User;
import com.example.financemanager.repository.CategoryRepository;
import com.example.financemanager.repository.ExpenseRepository;
import com.example.financemanager.repository.IncomeRepository;
import com.example.financemanager.repository.ReceiptRepository;
import com.example.financemanager.repository.SubcategoryRepository;
import com.example.financemanager.service.BackupService;
import com.example.financemanager.service.CurrentUserResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BackupServiceImpl implements BackupService {

    private static final String FALLBACK_CATEGORY_NAME = "Прочее";
    private static final String FALLBACK_CATEGORY_COLOR = "#7f8c8d";
    private static final String FALLBACK_CATEGORY_ICON = "fas fa-box-open";

    private final CategoryRepository categoryRepository;
    private final SubcategoryRepository subcategoryRepository;
    private final ExpenseRepository expenseRepository;
    private final IncomeRepository incomeRepository;
    private final ReceiptRepository receiptRepository;
    private final CurrentUserResolver currentUserResolver;

    @Override
    @Transactional(readOnly = true)
    public BackupSnapshotDto exportSnapshot() {
        User user = currentUserResolver.getCurrentUser();
        BackupSnapshotDto snapshot = new BackupSnapshotDto();

        List<Category> categories = categoryRepository.findAllByUser(user);
        List<Expense> expenses = expenseRepository.findAllByUserOrderByDateDesc(user);
        List<Income> incomes = incomeRepository.findAllByUserOrderByDateDesc(user);

        snapshot.setCategories(categories.stream().map(category -> {
            BackupSnapshotDto.BackupCategoryDto dto = new BackupSnapshotDto.BackupCategoryDto();
            dto.setName(category.getName());
            dto.setColor(category.getColor());
            dto.setIcon(category.getIcon());
            dto.setSubcategories((category.getSubcategories() == null ? List.<Subcategory>of() : category.getSubcategories()).stream()
                    .map(Subcategory::getName)
                    .filter(name -> name != null && !name.isBlank())
                    .collect(Collectors.toList()));
            return dto;
        }).collect(Collectors.toList()));

        snapshot.setExpenses(expenses.stream().map(expense -> {
            BackupSnapshotDto.BackupExpenseDto dto = new BackupSnapshotDto.BackupExpenseDto();
            dto.setAmount(expense.getAmount());
            dto.setDate(expense.getDate());
            dto.setDescription(expense.getDescription());
            dto.setCategoryName(expense.getCategory() != null ? expense.getCategory().getName() : FALLBACK_CATEGORY_NAME);
            dto.setSubcategoryName(expense.getSubcategory() != null ? expense.getSubcategory().getName() : null);
            return dto;
        }).collect(Collectors.toList()));

        snapshot.setIncomes(incomes.stream().map(income -> {
            BackupSnapshotDto.BackupIncomeDto dto = new BackupSnapshotDto.BackupIncomeDto();
            dto.setAmount(income.getAmount());
            dto.setDate(income.getDate());
            dto.setDescription(income.getDescription());
            return dto;
        }).collect(Collectors.toList()));

        return snapshot;
    }

    @Override
    @Transactional
    public void importSnapshot(BackupSnapshotDto snapshot) {
        User user = currentUserResolver.getCurrentUser();
        BackupSnapshotDto safeSnapshot = snapshot == null ? new BackupSnapshotDto() : snapshot;

        expenseRepository.deleteAllByUser(user);
        incomeRepository.deleteAllByUser(user);
        receiptRepository.deleteAllByUser(user);
        categoryRepository.deleteAllByUser(user);

        Map<String, Category> categoriesByName = new HashMap<>();
        for (BackupSnapshotDto.BackupCategoryDto categoryDto : safeList(safeSnapshot.getCategories())) {
            String categoryName = trimOrNull(categoryDto.getName());
            if (categoryName == null) {
                continue;
            }
            String key = normalize(categoryName);
            if (categoriesByName.containsKey(key)) {
                continue;
            }

            Category category = new Category();
            category.setName(categoryName);
            category.setColor(trimOrDefault(categoryDto.getColor(), FALLBACK_CATEGORY_COLOR));
            category.setIcon(trimOrDefault(categoryDto.getIcon(), FALLBACK_CATEGORY_ICON));
            category.setUser(user);
            Category savedCategory = categoryRepository.save(category);
            categoriesByName.put(key, savedCategory);

            List<String> names = safeList(categoryDto.getSubcategories());
            for (String subcategoryName : names) {
                String normalizedSubcategoryName = trimOrNull(subcategoryName);
                if (normalizedSubcategoryName == null) {
                    continue;
                }
                if (subcategoryRepository.existsByCategoryIdAndCategoryUserAndNameIgnoreCase(savedCategory.getId(), user, normalizedSubcategoryName)) {
                    continue;
                }
                Subcategory subcategory = new Subcategory();
                subcategory.setName(normalizedSubcategoryName);
                subcategory.setCategory(savedCategory);
                subcategoryRepository.save(subcategory);
            }
        }

        Category fallbackCategory = categoriesByName.get(normalize(FALLBACK_CATEGORY_NAME));
        if (fallbackCategory == null) {
            Category category = new Category();
            category.setName(FALLBACK_CATEGORY_NAME);
            category.setColor(FALLBACK_CATEGORY_COLOR);
            category.setIcon(FALLBACK_CATEGORY_ICON);
            category.setUser(user);
            fallbackCategory = categoryRepository.save(category);
            categoriesByName.put(normalize(FALLBACK_CATEGORY_NAME), fallbackCategory);
        }

        Map<Long, Map<String, Subcategory>> subcategoriesByCategory = new HashMap<>();
        for (Category category : new ArrayList<>(categoriesByName.values())) {
            Map<String, Subcategory> map = subcategoryRepository.findByCategoryIdAndCategoryUser(category.getId(), user).stream()
                    .collect(Collectors.toMap(
                            subcategory -> normalize(subcategory.getName()),
                            subcategory -> subcategory,
                            (left, right) -> left
                    ));
            subcategoriesByCategory.put(category.getId(), map);
        }

        for (BackupSnapshotDto.BackupExpenseDto expenseDto : safeList(safeSnapshot.getExpenses())) {
            if (expenseDto.getAmount() == null || expenseDto.getDate() == null) {
                continue;
            }
            Category category = categoriesByName.getOrDefault(normalize(trimOrDefault(expenseDto.getCategoryName(), FALLBACK_CATEGORY_NAME)), fallbackCategory);

            Expense expense = new Expense();
            expense.setAmount(expenseDto.getAmount());
            expense.setDate(expenseDto.getDate());
            expense.setDescription(trimOrDefault(expenseDto.getDescription(), ""));
            expense.setCategory(category);
            expense.setUser(user);

            String subcategoryName = trimOrNull(expenseDto.getSubcategoryName());
            if (subcategoryName != null) {
                Subcategory subcategory = subcategoriesByCategory
                        .getOrDefault(category.getId(), Map.of())
                        .get(normalize(subcategoryName));
                if (subcategory == null) {
                    subcategory = new Subcategory();
                    subcategory.setName(subcategoryName);
                    subcategory.setCategory(category);
                    subcategory = subcategoryRepository.save(subcategory);
                    subcategoriesByCategory.computeIfAbsent(category.getId(), ignored -> new HashMap<>())
                            .put(normalize(subcategoryName), subcategory);
                }
                expense.setSubcategory(subcategory);
            }

            expenseRepository.save(expense);
        }

        for (BackupSnapshotDto.BackupIncomeDto incomeDto : safeList(safeSnapshot.getIncomes())) {
            if (incomeDto.getAmount() == null || incomeDto.getDate() == null) {
                continue;
            }
            Income income = new Income();
            income.setAmount(incomeDto.getAmount());
            income.setDate(incomeDto.getDate());
            income.setDescription(trimOrDefault(incomeDto.getDescription(), ""));
            income.setUser(user);
            incomeRepository.save(income);
        }
    }

    private static String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).trim();
    }

    private static String trimOrNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static String trimOrDefault(String value, String fallback) {
        String trimmed = trimOrNull(value);
        return trimmed == null ? fallback : trimmed;
    }

    private static <T> List<T> safeList(List<T> source) {
        return source == null ? List.of() : source;
    }
}
