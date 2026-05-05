package com.example.financemanager.service.impl;

import com.example.financemanager.api.FnsApiClient;
import com.example.financemanager.dto.ReceiptResponseDto;
import com.example.financemanager.dto.ScanReceiptRequestDto;
import com.example.financemanager.dto.fns.FnsReceiptResponse;
import com.example.financemanager.entity.Expense;
import com.example.financemanager.entity.Receipt;
import com.example.financemanager.mapper.ReceiptMapper;
import com.example.financemanager.repository.ReceiptRepository;
import com.example.financemanager.service.CategoryAssignmentService;
import com.example.financemanager.service.ReceiptService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class ReceiptServiceImpl implements ReceiptService {

    private final FnsApiClient fnsApiClient;
    private final ReceiptRepository receiptRepository;
    private final CategoryAssignmentService categoryAssignmentService;
    private final ReceiptMapper receiptMapper;

    @Override
    @Transactional
    public ReceiptResponseDto processAndSaveReceipt(ScanReceiptRequestDto requestDto) {
        FnsReceiptResponse fnsData = fnsApiClient.getReceiptDetails(requestDto.getQrCodeData());
        FnsReceiptResponse.ReceiptJson receiptJson = fnsData.getData().getJson();

        // ИСПРАВЛЕНО: Просто конвертируем LocalDateTime в LocalDate
        LocalDate receiptDate = receiptJson.getDateTime().toLocalDate();

        Receipt receipt = new Receipt();
        receipt.setStoreName(receiptJson.getUser());
        receipt.setReceiptDate(receiptDate);
        receipt.setTotalAmount(BigDecimal.valueOf(receiptJson.getTotalSum()).divide(new BigDecimal(100)));

        for (FnsReceiptResponse.Item fnsItem : receiptJson.getItems()) {
            Expense expense = new Expense();
            expense.setDescription(fnsItem.getName());
            expense.setAmount(BigDecimal.valueOf(fnsItem.getSum()).divide(new BigDecimal(100)));
            expense.setDate(receiptDate); // Используем ту же дату для всех позиций

            categoryAssignmentService.assignCategory(expense);

            receipt.addExpense(expense);
        }

        Receipt savedReceipt = receiptRepository.save(receipt);

        return receiptMapper.toResponseDto(savedReceipt);
    }
}
