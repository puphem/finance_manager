package com.example.financemanager.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class SubscriptionScheduler {

    private final SubscriptionService subscriptionService;

    @Scheduled(cron = "0 10 0 * * *")
    public void processRecurringSubscriptions() {
        try {
            subscriptionService.processDueSubscriptions();
        } catch (Exception exception) {
            log.error("Не удалось обработать автосписания подписок", exception);
        }
    }
}
