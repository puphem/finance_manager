package com.example.financemanager.service.impl;

import com.example.financemanager.api.FnsApiClient;
import com.example.financemanager.dto.ReceiptResponseDto;
import com.example.financemanager.dto.ScanReceiptRequestDto;
import com.example.financemanager.dto.fns.FnsReceiptResponse;
import com.example.financemanager.entity.Expense;
import com.example.financemanager.entity.Receipt;
import com.example.financemanager.exception.DuplicateResourceException;
import com.example.financemanager.mapper.ReceiptMapper;
import com.example.financemanager.repository.ReceiptRepository;
import com.example.financemanager.service.CategoryAssignmentService;
import com.example.financemanager.service.ReceiptService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

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
        String receiptKey = buildReceiptKey(requestDto.getQrCodeData());
        if (receiptRepository.existsByReceiptKey(receiptKey)) {
            throw new DuplicateResourceException("Этот чек уже был добавлен в траты.");
        }

        FnsReceiptResponse fnsData = fnsApiClient.getReceiptDetails(requestDto.getQrCodeData());
        FnsReceiptResponse.ReceiptJson receiptJson = fnsData.getData() != null ? fnsData.getData().getJson() : null;
        if (receiptJson == null) {
            throw new IllegalStateException("Не удалось получить данные чека для добавления в траты.");
        }

        LocalDate receiptDate = receiptJson.getDateTime() != null ? receiptJson.getDateTime().toLocalDate() : LocalDate.now();

        Receipt receipt = new Receipt();
        receipt.setStoreName(receiptJson.getUser() == null || receiptJson.getUser().isBlank() ? "Неизвестный магазин" : receiptJson.getUser());
        receipt.setReceiptDate(receiptDate);
        receipt.setTotalAmount(centsToRubles(receiptJson.getTotalSum()));
        receipt.setReceiptKey(receiptKey);

        if (receiptJson.getItems() == null || receiptJson.getItems().isEmpty()) {
            Expense expense = new Expense();
            expense.setDescription("Покупка по чеку");
            expense.setAmount(receipt.getTotalAmount());
            expense.setDate(receiptDate);
            categoryAssignmentService.assignCategory(expense);
            receipt.addExpense(expense);
        } else {
            for (FnsReceiptResponse.Item fnsItem : receiptJson.getItems()) {
                if (fnsItem == null) {
                    continue;
                }

                Expense expense = new Expense();
                expense.setDescription(fnsItem.getName() == null || fnsItem.getName().isBlank() ? "Позиция из чека" : fnsItem.getName());
                expense.setAmount(centsToRubles(fnsItem.getSum()));
                expense.setDate(receiptDate);

                categoryAssignmentService.assignCategory(expense);
                receipt.addExpense(expense);
            }
        }

        Receipt savedReceipt = receiptRepository.save(receipt);

        return receiptMapper.toResponseDto(savedReceipt);
    }

    private String buildReceiptKey(String qrCodeData) {
        if (qrCodeData == null || qrCodeData.isBlank()) {
            throw new IllegalStateException("QR-код чека пустой.");
        }

        Map<String, String> params = Arrays.stream(qrCodeData.split("&"))
                .map(part -> part.split("=", 2))
                .filter(parts -> parts.length == 2)
                .collect(Collectors.toMap(
                        pair -> pair[0].toLowerCase(Locale.ROOT),
                        pair -> pair[1],
                        (existing, replacement) -> existing
                ));

        String fiscalNumber = params.get("fn");
        String fiscalDocumentNumber = params.get("i");
        String fiscalSign = params.get("fp");
        if (fiscalNumber != null && fiscalDocumentNumber != null && fiscalSign != null) {
            return "fn:" + fiscalNumber + "|i:" + fiscalDocumentNumber + "|fp:" + fiscalSign;
        }

        return Arrays.stream(qrCodeData.split("&"))
                .map(String::trim)
                .filter(part -> !part.isBlank())
                .sorted()
                .collect(Collectors.joining("&"));
    }

    private BigDecimal centsToRubles(long valueInCents) {
        return BigDecimal.valueOf(valueInCents)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }
}
