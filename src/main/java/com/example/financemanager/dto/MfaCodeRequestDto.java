package com.example.financemanager.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class MfaCodeRequestDto {
    @NotBlank(message = "Код обязателен")
    private String code;
}
