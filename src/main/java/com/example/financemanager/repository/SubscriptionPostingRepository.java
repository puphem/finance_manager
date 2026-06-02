package com.example.financemanager.repository;

import com.example.financemanager.entity.Subscription;
import com.example.financemanager.entity.SubscriptionPosting;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;

public interface SubscriptionPostingRepository extends JpaRepository<SubscriptionPosting, Long> {
    boolean existsBySubscriptionAndPostingDate(Subscription subscription, LocalDate postingDate);
}
