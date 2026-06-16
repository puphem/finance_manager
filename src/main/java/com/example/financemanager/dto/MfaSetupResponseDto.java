package com.example.financemanager.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class MfaSetupResponseDto {
    private String secret;
    private String otpauthUrl;
    private List<String> recoveryCodes;
}
