package com.example.financemanager.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class DeactivateAccountRequestDto {
    @NotBlank(message = "Пароль обязателен")
    private String currentPassword;
}
