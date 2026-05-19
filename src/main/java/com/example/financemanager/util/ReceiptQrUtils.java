package com.example.financemanager.util;

import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

public final class ReceiptQrUtils {

    private ReceiptQrUtils() {
    }

    public static Map<String, String> parseQrParams(String qrCodeData) {
        if (qrCodeData == null || qrCodeData.isBlank()) {
            return Map.of();
        }
        return Arrays.stream(qrCodeData.split("&"))
                .map(part -> part.split("=", 2))
                .filter(parts -> parts.length == 2)
                .collect(Collectors.toMap(
                        pair -> pair[0].toLowerCase(Locale.ROOT),
                        pair -> pair[1],
                        (existing, replacement) -> existing
                ));
    }

    public static String buildReceiptKey(String qrCodeData) {
        if (qrCodeData == null || qrCodeData.isBlank()) {
            throw new IllegalStateException("QR-код чека отсутствует или пустой.");
        }

        Map<String, String> params = parseQrParams(qrCodeData);
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
}
