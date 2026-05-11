package com.example.financemanager.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class ExpenseRequestDto {

    @NotNull(message = "Сумма не может быть пустой")
    @DecimalMin(value = "0.01", message = "Сумма должна быть положительной")
    private BigDecimal amount;

    @NotNull(message = "ID категории не может быть пустым")
    private Long categoryId;

    private Long subcategoryId;

    private String description;

    @NotNull(message = "Дата не может быть пустой")
    @PastOrPresent(message = "Дата не может быть в будущем")
    private LocalDate date;
}
