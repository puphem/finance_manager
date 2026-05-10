package com.example.financemanager.config;

import com.example.financemanager.entity.Category;
import com.example.financemanager.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final CategoryRepository categoryRepository;

    @Override
    public void run(String... args) throws Exception {
        List<Category> defaultCategories = buildDefaultCategories();
        Set<String> existingNames = new HashSet<>();
        categoryRepository.findAll().forEach(category -> {
            if (category.getName() != null) {
                existingNames.add(category.getName().toLowerCase(Locale.ROOT));
            }
        });

        List<Category> categoriesToCreate = new ArrayList<>();
        for (Category defaultCategory : defaultCategories) {
            if (!existingNames.contains(defaultCategory.getName().toLowerCase(Locale.ROOT))) {
                categoriesToCreate.add(defaultCategory);
            }
        }

        if (!categoriesToCreate.isEmpty()) {
            categoryRepository.saveAll(categoriesToCreate);
            System.out.println(">>> Базовые категории успешно созданы/дополнены!");
        }
    }

    private List<Category> buildDefaultCategories() {
        Category products = createCategory("Продукты", "#27AE60", "fas fa-shopping-basket");
        Category transport = createCategory("Транспорт", "#2980B9", "fas fa-bus");
        Category taxi = createCategory("Такси", "#1F8A70", "fas fa-taxi");
        Category entertainment = createCategory("Развлечения", "#F39C12", "fas fa-film");
        Category bills = createCategory("Счета", "#C0392B", "fas fa-file-invoice-dollar");
        Category home = createCategory("Дом", "#8E44AD", "fas fa-house-user");
        Category health = createCategory("Здоровье", "#E74C3C", "fas fa-heartbeat");
        Category clothes = createCategory("Одежда", "#9B59B6", "fas fa-tshirt");
        Category cafes = createCategory("Кафе и рестораны", "#D35400", "fas fa-utensils");
        Category education = createCategory("Образование", "#34495E", "fas fa-graduation-cap");
        Category pets = createCategory("Питомцы", "#7F8C8D", "fas fa-paw");
        Category beauty = createCategory("Красота", "#E84393", "fas fa-spa");

        return List.of(
                products, transport, taxi, entertainment, bills, home, health, clothes, cafes, education, pets, beauty
        );
    }

    private Category createCategory(String name, String color, String icon) {
        Category category = new Category();
        category.setName(name);
        category.setColor(color);
        category.setIcon(icon);
        return category;
    }
}
