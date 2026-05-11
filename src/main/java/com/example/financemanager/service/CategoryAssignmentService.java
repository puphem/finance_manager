package com.example.financemanager.service;

import com.example.financemanager.entity.Category;
import com.example.financemanager.entity.Expense;
import com.example.financemanager.entity.User;
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

    public void assignCategory(Expense expense, User user) {
        expense.setCategory(suggestCategory(expense.getDescription(), user));
    }

    public Category suggestCategory(String expenseDescription, User user) {
        String itemName = expenseDescription == null ? "" : expenseDescription.toLowerCase(Locale.ROOT);
        List<Category> categories = categoryRepository.findAllByUser(user);
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

        return matchedCategory != null ? matchedCategory : defaultCategory;
    }

    private Map<String, List<String>> buildCategoryKeywords() {
        Map<String, List<String>> keywords = new LinkedHashMap<>();
        keywords.put("продукт", List.of("молок", "сыр", "хлеб", "яйц", "кефир", "масло", "колбас", "мясо", "овощ", "фрукт", "чай", "кофе", "сахар", "круп", "макарон", "магазин"));
        keywords.put("транспорт", List.of("бенз", "дизел", "метро", "такси", "автобус", "троллейбус", "электричк", "парковк", "каршеринг", "топлив", "uber", "yandex go", "яндекс go"));
        keywords.put("такси", List.of("такси", "uber", "taxi", "yandex go", "яндекс go", "поездка"));
        keywords.put("кафе", List.of("кафе", "ресторан", "кофейн", "пицц", "бургер", "суши", "доставка"));
        keywords.put("ресторан", List.of("кафе", "ресторан", "кофейн", "пицц", "бургер", "суши", "доставка"));
        keywords.put("развлеч", List.of("кино", "театр", "игр", "билет", "квест", "музей", "концерт", "подписк"));
        keywords.put("счет", List.of("жкх", "коммунал", "интернет", "электр", "вода", "газ", "аренд", "штраф", "налог", "капремонт", "мобильн", "связь"));
        keywords.put("дом", List.of("ремонт", "дом", "мебел", "ламп", "хозтовар", "быт"));
        keywords.put("образов", List.of("курс", "школ", "универс", "книг", "обучен", "репетитор"));
        keywords.put("одеж", List.of("куртк", "футболк", "брюк", "кроссовк", "ботинк", "джинс"));
        keywords.put("красот", List.of("салон", "маникюр", "парикмах", "космет"));
        keywords.put("питом", List.of("вет", "корм", "зоомаг", "питом"));
        keywords.put("здоров", List.of("аптек", "лекар", "витамин", "клиник", "мед"));
        return keywords;
    }
}
