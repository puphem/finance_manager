package com.example.financemanager.repository;

import com.example.financemanager.dto.CategoryExpenseDto;
import com.example.financemanager.dto.SubcategoryExpenseDto;
import com.example.financemanager.entity.Expense;
import com.example.financemanager.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ExpenseRepository extends JpaRepository<Expense, Long>, JpaSpecificationExecutor<Expense> {
    List<Expense> findAllByUserOrderByDateDesc(User user);
    List<Expense> findByUserAndDateBetweenOrderByDateDesc(User user, LocalDate start, LocalDate end);
    Optional<Expense> findByIdAndUser(Long id, User user);
    boolean existsByIdAndUser(Long id, User user);
    void deleteAllByUser(User user);

    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM Expense e WHERE e.user = :user AND e.date >= :startDate AND e.date <= :endDate")
    BigDecimal sumAmountByUserAndDateBetween(@Param("user") User user, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Query("SELECT new com.example.financemanager.dto.CategoryExpenseDto(e.category.id, e.category.name, e.category.color, SUM(e.amount)) " +
            "FROM Expense e " +
            "WHERE e.user = :user AND e.date >= :startDate AND e.date <= :endDate " +
            "GROUP BY e.category.id, e.category.name, e.category.color " +
            "ORDER BY SUM(e.amount) DESC")
    List<CategoryExpenseDto> findCategoryExpensesByUserAndDateBetween(
            @Param("user") User user,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Query("SELECT new com.example.financemanager.dto.SubcategoryExpenseDto(COALESCE(s.name, 'Без подкатегории'), e.category.color, SUM(e.amount)) " +
            "FROM Expense e " +
            "LEFT JOIN e.subcategory s " +
            "WHERE e.user = :user AND e.category.id = :categoryId AND e.date >= :startDate AND e.date <= :endDate " +
            "GROUP BY s.name, e.category.color " +
            "ORDER BY SUM(e.amount) DESC")
    List<SubcategoryExpenseDto> findSubcategoryExpensesByUserAndCategoryAndDateBetween(
            @Param("user") User user,
            @Param("categoryId") Long categoryId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );
}
