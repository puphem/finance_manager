package com.example.financemanager.config;

import com.example.financemanager.entity.Category;
import com.example.financemanager.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final CategoryRepository categoryRepository;

    @Override
    public void run(String... args) throws Exception {
        if (categoryRepository.count() == 0) {
            Category products = new Category();
            products.setName("Продукты");
            products.setColor("#27AE60");
            products.setIcon("fas fa-shopping-basket");

            Category transport = new Category();
            transport.setName("Транспорт");
            transport.setColor("#2980B9");
            transport.setIcon("fas fa-car");

            Category entertainment = new Category();
            entertainment.setName("Развлечения");
            entertainment.setColor("#F39C12");
            entertainment.setIcon("fas fa-film");

            Category bills = new Category();
            bills.setName("Счета");
            bills.setColor("#C0392B");
            bills.setIcon("fas fa-file-invoice-dollar");

            categoryRepository.saveAll(List.of(products, transport, entertainment, bills));
            System.out.println(">>> Базовые категории успешно созданы!");
        }
    }
}
