package com.example.financemanager.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "user_category_model_stats")
@Getter
@Setter
@NoArgsConstructor
public class UserCategoryModelStats {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(nullable = false)
    private Long modelVersion = 1L;

    @Column(nullable = false)
    private Long totalPredictions = 0L;

    @Column(nullable = false)
    private Long acceptedPredictions = 0L;

    @Column(nullable = false)
    private Long correctedPredictions = 0L;

    @Column(nullable = false, precision = 8, scale = 4)
    private BigDecimal acceptRate = BigDecimal.ZERO;

    @Column(nullable = false, precision = 8, scale = 4)
    private BigDecimal precisionMetric = BigDecimal.ZERO;
}
