package com.example.financemanager.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AuthResponseDto {
    private String token;
    private String username;
    private String displayName;
    private String authState;
}
