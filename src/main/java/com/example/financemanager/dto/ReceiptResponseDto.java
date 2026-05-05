package com.example.financemanager.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class ReceiptResponseDto {
    private Long id;
    private String storeName;
    private LocalDate receiptDate;
    private BigDecimal totalAmount;
    private List<ExpenseResponseDto> expenses;
}
