package com.example.financemanager.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateUsernameRequestDto {

    @NotBlank(message = "Новый логин не может быть пустым")
    @Size(min = 3, max = 50, message = "Логин должен содержать от 3 до 50 символов")
    private String newUsername;

    @NotBlank(message = "Текущий пароль не может быть пустым")
    private String currentPassword;
}
