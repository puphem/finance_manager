package com.example.financemanager.dto;

import com.example.financemanager.entity.enums.RecurringEntryType;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@AllArgsConstructor
public class SubscriptionResponseDto {
    private Long id;
    private String source;
    private BigDecimal amount;
    private RecurringEntryType entryType;
    private Integer periodDays;
    private LocalDate nextChargeDate;
    private boolean autoPostEnabled;
    private boolean notificationEnabled;
    private boolean active;
    private Long categoryId;
    private String categoryName;
    private Long subcategoryId;
    private String subcategoryName;
    private String description;
}
