package com.example.financemanager.service;

import com.example.financemanager.entity.Category;
import com.example.financemanager.entity.Expense;
import com.example.financemanager.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CategoryAssignmentService {
    private final CategoryRepository categoryRepository;

    public void assignCategory(Expense expense) {
        String itemName = expense.getDescription().toLowerCase();

        // Ищем категорию "Продукты". Если ее нет, используем первую попавшуюся категорию как запасной вариант.
        Category defaultCategory = categoryRepository.findByName("Продукты")
                .orElse(categoryRepository.findAll().stream().findFirst().orElse(null));

        if (defaultCategory == null) {
            // Это критическая ситуация, если в базе вообще нет категорий.
            // В нашем случае DataInitializer их создает, так что этого не произойдет.
            throw new IllegalStateException("В базе данных нет категорий для назначения.");
        }

        if (itemName.contains("молоко") || itemName.contains("сыр") || itemName.contains("хлеб")) {
            expense.setCategory(categoryRepository.findByName("Продукты").orElse(defaultCategory));
        } else {
            expense.setCategory(defaultCategory);
        }
    }
}
