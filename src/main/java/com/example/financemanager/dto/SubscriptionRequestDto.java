package com.example.financemanager.dto;

import com.example.financemanager.entity.enums.RecurringEntryType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class SubscriptionRequestDto {
    @NotBlank
    private String source;

    @NotNull
    @DecimalMin("0.01")
    private BigDecimal amount;

    @NotNull
    private RecurringEntryType entryType;

    @NotNull
    @Positive
    private Integer periodDays;

    @NotNull
    private LocalDate nextChargeDate;

    private boolean autoPostEnabled = true;
    private boolean notificationEnabled = true;
    private boolean active = true;

    private Long categoryId;
    private Long subcategoryId;
    private String description;
}
