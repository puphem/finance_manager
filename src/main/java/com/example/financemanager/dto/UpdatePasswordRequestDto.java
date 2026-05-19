package com.example.financemanager.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdatePasswordRequestDto {

    @NotBlank(message = "Текущий пароль не может быть пустым")
    private String currentPassword;

    @NotBlank(message = "Новый пароль не может быть пустым")
    @Size(min = 6, message = "Новый пароль должен содержать не менее 6 символов")
    private String newPassword;
}

