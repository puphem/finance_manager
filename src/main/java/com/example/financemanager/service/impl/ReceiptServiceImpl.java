package com.example.financemanager.service.impl;

import com.example.financemanager.api.FnsApiClient;
import com.example.financemanager.dto.ReceiptResponseDto;
import com.example.financemanager.dto.ScanReceiptRequestDto;
import com.example.financemanager.dto.fns.FnsReceiptResponse;
import com.example.financemanager.entity.Expense;
import com.example.financemanager.entity.Receipt;
import com.example.financemanager.entity.User;
import com.example.financemanager.exception.DuplicateResourceException;
import com.example.financemanager.mapper.ReceiptMapper;
import com.example.financemanager.repository.ReceiptRepository;
import com.example.financemanager.service.CategoryAssignmentService;
import com.example.financemanager.service.CurrentUserResolver;
import com.example.financemanager.service.ReceiptService;
import com.example.financemanager.util.ReceiptQrUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReceiptServiceImpl implements ReceiptService {

    private final FnsApiClient fnsApiClient;
    private final ReceiptRepository receiptRepository;
    private final CategoryAssignmentService categoryAssignmentService;
    private final ReceiptMapper receiptMapper;
    private final CurrentUserResolver currentUserResolver;

    @Override
    @Transactional
    public ReceiptResponseDto processAndSaveReceipt(ScanReceiptRequestDto requestDto) {
        User user = currentUserResolver.getCurrentUser();
        String receiptKey = ReceiptQrUtils.buildReceiptKey(requestDto.getQrCodeData());
        if (receiptRepository.existsByReceiptKeyAndUser(receiptKey, user)) {
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
        receipt.setUser(user);

        Expense expense = new Expense();
        String expenseDescription = buildExpenseDescription(receiptJson.getUser(), receiptJson.getItems());
        expense.setDescription(expenseDescription);
        expense.setAmount(receipt.getTotalAmount());
        expense.setDate(receiptDate);
        expense.setUser(user);
        categoryAssignmentService.assignCategory(expense, user);
        receipt.addExpense(expense);

        Receipt savedReceipt = receiptRepository.save(receipt);

        return receiptMapper.toResponseDto(savedReceipt);
    }

    private String buildExpenseDescription(String storeName, List<FnsReceiptResponse.Item> items) {
        String normalizedStore = storeName == null || storeName.isBlank() ? "магазин" : storeName.trim();
        if (items == null || items.isEmpty()) {
            return "Покупка в " + normalizedStore;
        }

        List<String> itemNames = items.stream()
                .filter(item -> item != null && item.getName() != null && !item.getName().isBlank())
                .map(item -> item.getName().trim())
                .limit(3)
                .collect(Collectors.toList());

        if (itemNames.isEmpty()) {
            return "Покупка в " + normalizedStore;
        }

        if (itemNames.size() == 1) {
            return itemNames.get(0);
        }

        String joinedItems = String.join(", ", itemNames);
        return "Чек " + normalizedStore + ": " + joinedItems;
    }

    private BigDecimal centsToRubles(long valueInCents) {
        return BigDecimal.valueOf(valueInCents)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }
}
