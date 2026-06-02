package com.example.financemanager.entity;

import com.example.financemanager.entity.enums.PlannedExpensePriority;
import com.example.financemanager.entity.enums.PlannedExpenseStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "planned_expenses")
@Getter
@Setter
@NoArgsConstructor
public class PlannedExpense {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal expectedAmount;

    @Column(nullable = false)
    private LocalDate plannedDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PlannedExpensePriority priority = PlannedExpensePriority.MEDIUM;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PlannedExpenseStatus status = PlannedExpenseStatus.NEW;

    @Column(length = 400)
    private String notes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subcategory_id")
    private Subcategory subcategory;
}
