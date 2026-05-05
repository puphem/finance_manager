package com.example.financemanager.api;

import com.example.financemanager.dto.fns.FnsReceiptResponse;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;
import java.util.List;

@Component
public class FnsApiClient {

    public FnsReceiptResponse getReceiptDetails(String qrCodeData) {
        System.out.println(">>> ВЫЗВАН ФЕЙКОВЫЙ КЛИЕНТ ФНС. ДАННЫЕ QR: " + qrCodeData);

        // Создаем фейковые позиции
        var item1 = new FnsReceiptResponse.Item("Молоко 'Домик в деревне'", 8990, 1.0, 8990);
        var item2 = new FnsReceiptResponse.Item("Хлеб 'Бородинский'", 5450, 1.0, 5450);
        var item3 = new FnsReceiptResponse.Item("Сыр 'Российский' вес", 25000, 0.3, 7500);

        // Создаем сам фейковый чек
        var receiptJson = new FnsReceiptResponse.ReceiptJson();
        receiptJson.setUser("Пятерочка");
        receiptJson.setTotalSum(8990 + 5450 + 7500);
        receiptJson.setDateTime(LocalDateTime.now().minusDays(1)); // Генерируем LocalDateTime
        receiptJson.setItems(List.of(item1, item2, item3));

        var data = new FnsReceiptResponse.FnsReceiptData();
        data.setJson(receiptJson);

        var response = new FnsReceiptResponse();
        response.setCode(1); // Успешный код
        response.setData(data);

        return response;
    }
}
