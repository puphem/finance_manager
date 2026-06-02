package com.example.financemanager.dto;

import com.example.financemanager.entity.enums.LoginProvider;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class OAuthLinkRequestDto {
    @NotNull(message = "Провайдер обязателен")
    private LoginProvider provider;

    @NotBlank(message = "ID пользователя провайдера обязателен")
    private String providerUserId;

    private String email;
}
