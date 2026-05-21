package com.example.financemanager.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateDisplayNameRequestDto {

    @NotBlank(message = "Имя пользователя не может быть пустым")
    @Size(min = 1, max = 80, message = "Имя пользователя должно содержать от 1 до 80 символов")
    private String displayName;
}
