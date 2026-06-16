package com.example.financemanager.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@AllArgsConstructor
public class SubscriptionAlertDto {
    private Long subscriptionId;
    private String source;
    private BigDecimal amount;
    private LocalDate nextChargeDate;
    private String message;
}
