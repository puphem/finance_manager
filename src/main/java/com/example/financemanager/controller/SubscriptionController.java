package com.example.financemanager.controller;

import com.example.financemanager.dto.SubscriptionAlertDto;
import com.example.financemanager.dto.SubscriptionRequestDto;
import com.example.financemanager.dto.SubscriptionResponseDto;
import com.example.financemanager.service.SubscriptionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/subscriptions")
@RequiredArgsConstructor
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    @GetMapping
    public ResponseEntity<List<SubscriptionResponseDto>> list() {
        return ResponseEntity.ok(subscriptionService.listCurrentUserSubscriptions());
    }

    @PostMapping
    public ResponseEntity<SubscriptionResponseDto> create(@Valid @RequestBody SubscriptionRequestDto request) {
        return ResponseEntity.ok(subscriptionService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<SubscriptionResponseDto> update(@PathVariable Long id, @Valid @RequestBody SubscriptionRequestDto request) {
        return ResponseEntity.ok(subscriptionService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        subscriptionService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/upcoming")
    public ResponseEntity<List<SubscriptionResponseDto>> upcoming(@RequestParam(defaultValue = "7") int days) {
        return ResponseEntity.ok(subscriptionService.getUpcoming(days));
    }

    @GetMapping("/alerts")
    public ResponseEntity<List<SubscriptionAlertDto>> alerts(@RequestParam(defaultValue = "7") int days) {
        return ResponseEntity.ok(subscriptionService.getUpcomingAlerts(days));
    }

    @GetMapping(value = "/export", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<SubscriptionResponseDto>> exportJson() {
        return ResponseEntity.ok(subscriptionService.listCurrentUserSubscriptions());
    }

    @GetMapping(value = "/export/csv", produces = "text/csv")
    public ResponseEntity<String> exportCsv() {
        List<SubscriptionResponseDto> subscriptions = subscriptionService.listCurrentUserSubscriptions();
        StringBuilder csv = new StringBuilder();
        csv.append("id,source,amount,entryType,periodDays,nextChargeDate,autoPostEnabled,notificationEnabled,active,category,subcategory,description\n");
        for (SubscriptionResponseDto item : subscriptions) {
            csv.append(item.getId()).append(',')
                    .append(escape(item.getSource())).append(',')
                    .append(item.getAmount()).append(',')
                    .append(item.getEntryType()).append(',')
                    .append(item.getPeriodDays()).append(',')
                    .append(item.getNextChargeDate()).append(',')
                    .append(item.isAutoPostEnabled()).append(',')
                    .append(item.isNotificationEnabled()).append(',')
                    .append(item.isActive()).append(',')
                    .append(escape(item.getCategoryName())).append(',')
                    .append(escape(item.getSubcategoryName())).append(',')
                    .append(escape(item.getDescription()))
                    .append('\n');
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=subscriptions.csv")
                .body(csv.toString());
    }

    private String escape(String value) {
        if (value == null) {
            return "";
        }
        String escaped = value.replace("\"", "\"\"");
        return "\"" + escaped + "\"";
    }
}
