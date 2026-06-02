package com.example.financemanager.entity;

import com.example.financemanager.entity.enums.RecurringEntryType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "subscriptions")
@Getter
@Setter
@NoArgsConstructor
public class Subscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 200)
    private String source;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RecurringEntryType entryType = RecurringEntryType.EXPENSE;

    @Column(nullable = false)
    private Integer periodDays;

    @Column(nullable = false)
    private LocalDate nextChargeDate;

    @Column(nullable = false)
    private boolean autoPostEnabled = true;

    @Column(nullable = false)
    private boolean notificationEnabled = true;

    @Column(nullable = false)
    private boolean active = true;

    @Column
    private LocalDate lastNotifiedFor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subcategory_id")
    private Subcategory subcategory;

    @Column(length = 400)
    private String description;
}
