package com.example.financemanager.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AnalyticsAlertDto {
    private String type;
    private String message;
}
