package com.example.financemanager.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RecoveryFlowCompleteRequestDto {
    @NotBlank(message = "Токен обязателен")
    private String token;
}
