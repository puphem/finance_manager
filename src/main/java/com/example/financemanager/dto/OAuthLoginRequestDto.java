package com.example.financemanager.dto;

import com.example.financemanager.entity.enums.LoginProvider;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class OAuthLoginRequestDto {
    @NotNull(message = "Провайдер обязателен")
    private LoginProvider provider;

    @NotBlank(message = "ID пользователя провайдера обязателен")
    private String providerUserId;

    private String username;
    private String displayName;
    private String email;
}
