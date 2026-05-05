package com.example.financemanager.dto.fns;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class FnsItem {
    private String name;
    private long price; // Цена в копейках
    private double quantity;
    private long sum; // Сумма в копейках
}
