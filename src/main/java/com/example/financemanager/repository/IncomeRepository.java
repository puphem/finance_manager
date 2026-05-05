package com.example.financemanager.repository;

import com.example.financemanager.entity.Income;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface IncomeRepository extends JpaRepository<Income, Long> {
    List<Income> findAllByOrderByDateDesc();
    List<Income> findByDateBetweenOrderByDateDesc(LocalDate start, LocalDate end);

    @Query("SELECT COALESCE(SUM(i.amount), 0) FROM Income i WHERE i.date >= :startDate AND i.date <= :endDate")
    BigDecimal sumAmountByDateBetween(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
}
