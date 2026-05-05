package com.example.financemanager.dto.fns;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;

@Data
public class FnsReceiptResponse {
    private int code;
    private FnsReceiptData data;

    @Data
    public static class FnsReceiptData {
        @JsonProperty("json")
        private ReceiptJson json;
    }

    @Data
    public static class ReceiptJson {
        private String user;
        private long totalSum;
        private LocalDateTime dateTime; // ИЗМЕНЕНО С long НА LocalDateTime
        private List<Item> items;
    }

    @Data
    @AllArgsConstructor
    public static class Item {
        private String name;
        private long price;
        private double quantity;
        private long sum;
    }
}
