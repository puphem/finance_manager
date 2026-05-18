package com.example.financemanager.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Data
public class BackupSnapshotDto {
    private List<BackupCategoryDto> categories = new ArrayList<>();
    private List<BackupExpenseDto> expenses = new ArrayList<>();
    private List<BackupIncomeDto> incomes = new ArrayList<>();

    @Data
    public static class BackupCategoryDto {
        private String name;
        private String color;
        private String icon;
        private List<String> subcategories = new ArrayList<>();
    }

    @Data
    public static class BackupExpenseDto {
        private BigDecimal amount;
        private LocalDate date;
        private String description;
        private String categoryName;
        private String subcategoryName;
    }

    @Data
    public static class BackupIncomeDto {
        private BigDecimal amount;
        private LocalDate date;
        private String description;
    }
}
