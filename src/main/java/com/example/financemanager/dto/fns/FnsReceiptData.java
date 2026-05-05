package com.example.financemanager.dto.fns;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class FnsReceiptData {
    public String user; // Название магазина
    public long totalSum; // Сумма в копейках
    public LocalDateTime dateTime;
    public List<FnsItem> items;
}
