package com.example.financemanager.repository;

import com.example.financemanager.entity.PlannedExpense;
import com.example.financemanager.entity.User;
import com.example.financemanager.entity.enums.PlannedExpenseStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface PlannedExpenseRepository extends JpaRepository<PlannedExpense, Long> {
    Optional<PlannedExpense> findByIdAndUser(Long id, User user);
    List<PlannedExpense> findAllByUserOrderByPlannedDateAsc(User user);
    List<PlannedExpense> findAllByUserAndStatusOrderByPlannedDateAsc(User user, PlannedExpenseStatus status);
    List<PlannedExpense> findAllByUserAndPlannedDateBetweenOrderByPlannedDateAsc(User user, LocalDate start, LocalDate end);
    List<PlannedExpense> findAllByUserAndStatusAndPlannedDateBeforeOrderByPlannedDateAsc(User user, PlannedExpenseStatus status, LocalDate date);
}
