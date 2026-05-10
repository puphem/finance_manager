package com.example.financemanager.service;

import com.example.financemanager.entity.Category;
import com.example.financemanager.entity.Expense;
import com.example.financemanager.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CategoryAssignmentService {
    private final CategoryRepository categoryRepository;

    public void assignCategory(Expense expense) {
        String itemName = expense.getDescription() == null ? "" : expense.getDescription().toLowerCase(Locale.ROOT);
        List<Category> categories = categoryRepository.findAll();
        Category defaultCategory = categories.stream()
                .filter(category -> "продукты".equalsIgnoreCase(category.getName()))
                .findFirst()
                .orElse(categories.stream().findFirst().orElse(null));

        if (defaultCategory == null) {
            throw new IllegalStateException("В базе данных нет категорий для назначения.");
        }

        Map<String, List<String>> categoryKeywords = buildCategoryKeywords();
        Category matchedCategory = null;
        int bestScore = 0;

        for (Category category : categories) {
            String categoryName = category.getName() == null ? "" : category.getName().toLowerCase(Locale.ROOT);
            int score = 0;

            for (Map.Entry<String, List<String>> entry : categoryKeywords.entrySet()) {
                if (categoryName.contains(entry.getKey())) {
                    for (String keyword : entry.getValue()) {
                        if (itemName.contains(keyword)) {
                            score++;
                        }
                    }
                }
            }

            if (score == 0 && !categoryName.isBlank() && itemName.contains(categoryName)) {
                score = 1;
            }

            if (score > bestScore) {
                bestScore = score;
                matchedCategory = category;
            }
        }

        expense.setCategory(matchedCategory != null ? matchedCategory : defaultCategory);
    }

    private Map<String, List<String>> buildCategoryKeywords() {
        Map<String, List<String>> keywords = new LinkedHashMap<>();
        keywords.put("продукт", List.of("молок", "сыр", "хлеб", "яйц", "кефир", "масло", "колбас", "мясо", "овощ", "фрукт", "чай", "кофе", "сахар", "круп", "макарон"));
        keywords.put("транспорт", List.of("бенз", "дизел", "метро", "такси", "автобус", "троллейбус", "электричк", "парковк", "каршеринг", "топлив"));
        keywords.put("развлеч", List.of("кино", "театр", "игр", "билет", "квест", "музей", "концерт", "подписк"));
        keywords.put("счет", List.of("жкх", "коммунал", "интернет", "электр", "вода", "газ", "аренд", "штраф", "налог", "капремонт"));
        keywords.put("одеж", List.of("куртк", "футболк", "брюк", "кроссовк", "ботинк", "джинс"));
        keywords.put("здоров", List.of("аптек", "лекар", "витамин", "клиник", "мед"));
        return keywords;
    }
}
