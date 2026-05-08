package com.example.financemanager.api;

import com.example.financemanager.dto.fns.FnsReceiptResponse;
import com.example.financemanager.util.ReceiptQrUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class FnsApiClient {

    // Distinguishes epoch milliseconds from epoch seconds while parsing API timestamp fields.
    private static final long EPOCH_MILLIS_THRESHOLD = 9_999_999_999L;

    private final WebClient.Builder webClientBuilder;

    @Value("${fns.api.token:}")
    private String apiToken;

    @Value("${fns.api.base-url:https://proverkacheka.com}")
    private String apiBaseUrl;

    public FnsReceiptResponse getReceiptDetails(String qrCodeData) {
        FnsReceiptResponse apiResponse = tryLoadReceiptFromApi(qrCodeData);
        if (isUsable(apiResponse)) {
            return apiResponse;
        }
        return buildFallbackResponseFromQr(qrCodeData);
    }

    private FnsReceiptResponse tryLoadReceiptFromApi(String qrCodeData) {
        if (apiToken == null || apiToken.isBlank()) {
            return null;
        }

        try {
            Map<?, ?> responseMap = webClientBuilder
                    .baseUrl(apiBaseUrl)
                    .build()
                    .post()
                    .uri("/api/v1/check/get")
                    .bodyValue(Map.of(
                            "token", apiToken,
                            "qrraw", qrCodeData
                    ))
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            return mapToReceiptResponse(responseMap);
        } catch (WebClientResponseException ex) {
            return null;
        } catch (Exception ex) {
            return null;
        }
    }

    private boolean isUsable(FnsReceiptResponse response) {
        return response != null
                && response.getData() != null
                && response.getData().getJson() != null
                && response.getData().getJson().getItems() != null
                && !response.getData().getJson().getItems().isEmpty();
    }

    private FnsReceiptResponse mapToReceiptResponse(Map<?, ?> responseMap) {
        if (responseMap == null || responseMap.isEmpty()) {
            return null;
        }

        Map<?, ?> dataMap = asMap(responseMap.get("data"));
        Map<?, ?> receiptJsonMap = dataMap == null ? null : asMap(dataMap.get("json"));
        if (receiptJsonMap == null || receiptJsonMap.isEmpty()) {
            return null;
        }

        FnsReceiptResponse.ReceiptJson receiptJson = new FnsReceiptResponse.ReceiptJson();
        receiptJson.setUser(asString(receiptJsonMap.get("user"), "Неизвестный магазин"));
        receiptJson.setTotalSum(asLong(receiptJsonMap.get("totalSum"), 0L));
        receiptJson.setDateTime(parseDateTimeValue(receiptJsonMap.get("dateTime"), LocalDateTime.now()));
        receiptJson.setItems(extractItems(receiptJsonMap.get("items")));

        FnsReceiptResponse.FnsReceiptData data = new FnsReceiptResponse.FnsReceiptData();
        data.setJson(receiptJson);

        FnsReceiptResponse response = new FnsReceiptResponse();
        response.setCode((int) asLong(responseMap.get("code"), 1L));
        response.setData(data);
        return response;
    }

    private List<FnsReceiptResponse.Item> extractItems(Object itemsObj) {
        if (!(itemsObj instanceof List<?> rawItems)) {
            return List.of();
        }

        List<FnsReceiptResponse.Item> items = new ArrayList<>();
        for (Object itemObj : rawItems) {
            Map<?, ?> itemMap = asMap(itemObj);
            if (itemMap == null || itemMap.isEmpty()) {
                continue;
            }

            String name = asString(itemMap.get("name"), "Позиция из чека");
            long sum = asLong(itemMap.get("sum"), 0L);
            long price = asLong(itemMap.get("price"), sum);
            double quantity = asDouble(itemMap.get("quantity"), 1.0);
            items.add(new FnsReceiptResponse.Item(name, price, quantity, sum));
        }
        return items;
    }

    private FnsReceiptResponse buildFallbackResponseFromQr(String qrCodeData) {
        Map<String, String> params = ReceiptQrUtils.parseQrParams(qrCodeData);
        long totalSumInCents = parseTotalSumToCents(params.get("s"));
        LocalDateTime dateTime = parseReceiptDate(params.get("t"));
        String documentNumber = params.getOrDefault("i", "неизвестен");

        FnsReceiptResponse.Item item = new FnsReceiptResponse.Item(
                "Покупка по чеку №" + documentNumber,
                totalSumInCents,
                1.0,
                totalSumInCents
        );

        FnsReceiptResponse.ReceiptJson receiptJson = new FnsReceiptResponse.ReceiptJson();
        receiptJson.setUser("Чек без детализации");
        receiptJson.setTotalSum(totalSumInCents);
        receiptJson.setDateTime(dateTime);
        receiptJson.setItems(List.of(item));

        FnsReceiptResponse.FnsReceiptData data = new FnsReceiptResponse.FnsReceiptData();
        data.setJson(receiptJson);

        FnsReceiptResponse response = new FnsReceiptResponse();
        response.setCode(1);
        response.setData(data);
        return response;
    }

    private long parseTotalSumToCents(String rawSum) {
        if (rawSum == null || rawSum.isBlank()) {
            return 0L;
        }
        try {
            BigDecimal rubles = new BigDecimal(rawSum.replace(",", "."));
            return rubles.multiply(BigDecimal.valueOf(100))
                    .setScale(0, RoundingMode.HALF_UP)
                    .longValue();
        } catch (NumberFormatException ex) {
            return 0L;
        }
    }

    private LocalDateTime parseReceiptDate(String rawDate) {
        if (rawDate == null || rawDate.isBlank()) {
            return LocalDateTime.now();
        }

        List<DateTimeFormatter> formatters = List.of(
                DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss"),
                DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmm")
        );
        for (DateTimeFormatter formatter : formatters) {
            try {
                return LocalDateTime.parse(rawDate, formatter);
            } catch (DateTimeParseException ignored) {
            }
        }
        return LocalDateTime.now();
    }

    private LocalDateTime parseDateTimeValue(Object value, LocalDateTime defaultValue) {
        if (value == null) {
            return defaultValue;
        }

        if (value instanceof Number number) {
            long timestamp = number.longValue();
            Instant instant = timestamp > EPOCH_MILLIS_THRESHOLD
                    ? Instant.ofEpochMilli(timestamp)
                    : Instant.ofEpochSecond(timestamp);
            return LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
        }

        String text = String.valueOf(value);
        if (text.isBlank()) {
            return defaultValue;
        }

        try {
            return LocalDateTime.parse(text);
        } catch (DateTimeParseException ignored) {
        }

        try {
            return LocalDateTime.parse(text, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        } catch (DateTimeParseException ignored) {
        }

        try {
            return LocalDateTime.ofInstant(Instant.parse(text), ZoneId.systemDefault());
        } catch (DateTimeParseException ignored) {
        }

        return defaultValue;
    }

    private Map<?, ?> asMap(Object value) {
        return value instanceof Map<?, ?> map ? map : null;
    }

    private String asString(Object value, String defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        String text = String.valueOf(value);
        return text.isBlank() ? defaultValue : text;
    }

    private long asLong(Object value, long defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }

    private double asDouble(Object value, double defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        try {
            return Double.parseDouble(String.valueOf(value));
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }
}
