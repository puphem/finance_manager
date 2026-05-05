package com.example.financemanager.controller;

import com.example.financemanager.dto.ReceiptResponseDto;
import com.example.financemanager.dto.ScanReceiptRequestDto;
import com.example.financemanager.service.ReceiptService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/receipts")
@RequiredArgsConstructor
public class ReceiptController {

    private final ReceiptService receiptService;

    @PostMapping("/scan")
    public ResponseEntity<ReceiptResponseDto> scanAndProcessReceipt(@RequestBody ScanReceiptRequestDto requestDto) {
        ReceiptResponseDto responseDto = receiptService.processAndSaveReceipt(requestDto);
        return new ResponseEntity<>(responseDto, HttpStatus.CREATED);
    }
}
