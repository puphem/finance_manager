package com.example.financemanager.repository;

import com.example.financemanager.entity.Subscription;
import com.example.financemanager.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {
    List<Subscription> findAllByUserOrderByNextChargeDateAsc(User user);
    Optional<Subscription> findByIdAndUser(Long id, User user);
    List<Subscription> findAllByActiveTrueAndNextChargeDateLessThanEqual(LocalDate date);
    List<Subscription> findAllByUserAndNextChargeDateBetweenOrderByNextChargeDateAsc(User user, LocalDate start, LocalDate end);
}
