package com.example.financemanager.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class IncomeDto {
    private Long id;

    @NotNull(message = "Сумма не может быть пустой")
    @DecimalMin(value = "0.01", message = "Сумма должна быть положительной")
    private BigDecimal amount;

    @NotNull(message = "Дата не может быть пустой")
    @PastOrPresent(message = "Дата не может быть в будущем")
    private LocalDate date;

    private String description;
}
