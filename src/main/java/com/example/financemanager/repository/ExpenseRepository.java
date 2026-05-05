package com.example.financemanager.repository;

import com.example.financemanager.dto.CategoryExpenseDto;
import com.example.financemanager.entity.Expense;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate; // ИЗМЕНЕНО
import java.util.List;

@Repository
public interface ExpenseRepository extends JpaRepository<Expense, Long>, JpaSpecificationExecutor<Expense> {
    List<Expense> findAllByOrderByDateDesc();
    List<Expense> findByDateBetweenOrderByDateDesc(LocalDate start, LocalDate end); // ИЗМЕНЕНО

    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM Expense e WHERE e.date >= :startDate AND e.date <= :endDate")
    BigDecimal sumAmountByDateBetween(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate); // ИЗМЕНЕНО

    @Query("SELECT new com.example.financemanager.dto.CategoryExpenseDto(e.category.name, e.category.color, SUM(e.amount)) " +
            "FROM Expense e " +
            "WHERE e.date >= :startDate AND e.date <= :endDate " +
            "GROUP BY e.category.name, e.category.color " +
            "ORDER BY SUM(e.amount) DESC")
    List<CategoryExpenseDto> findCategoryExpensesByDateBetween(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );
}
