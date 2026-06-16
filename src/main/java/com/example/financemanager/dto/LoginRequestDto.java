package com.example.financemanager.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequestDto {

    @NotBlank(message = "Логин не может быть пустым")
    private String username;

    @NotBlank(message = "Пароль не может быть пустым")
    private String password;

    private String mfaCode;
    private String recoveryCode;
}
