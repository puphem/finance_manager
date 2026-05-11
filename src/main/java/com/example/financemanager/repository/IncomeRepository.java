package com.example.financemanager.repository;

import com.example.financemanager.entity.Income;
import com.example.financemanager.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface IncomeRepository extends JpaRepository<Income, Long> {
    List<Income> findAllByUserOrderByDateDesc(User user);
    List<Income> findByUserAndDateBetweenOrderByDateDesc(User user, LocalDate start, LocalDate end);
    Optional<Income> findByIdAndUser(Long id, User user);
    boolean existsByIdAndUser(Long id, User user);

    @Query("SELECT COALESCE(SUM(i.amount), 0) FROM Income i WHERE i.user = :user AND i.date >= :startDate AND i.date <= :endDate")
    BigDecimal sumAmountByUserAndDateBetween(@Param("user") User user, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
}
