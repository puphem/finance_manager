package com.example.financemanager.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ScanReceiptRequestDto {
    @NotBlank(message = "QR-код чека обязателен")
    private String qrCodeData;

    private String apiToken;
}
