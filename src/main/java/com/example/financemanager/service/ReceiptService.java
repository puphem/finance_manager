package com.example.financemanager.service;

import com.example.financemanager.dto.ReceiptResponseDto;
import com.example.financemanager.dto.ScanReceiptRequestDto;

public interface ReceiptService {
    ReceiptResponseDto processAndSaveReceipt(ScanReceiptRequestDto requestDto);
}
