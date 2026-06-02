package com.example.financemanager.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "subscription_postings", uniqueConstraints = {
        @UniqueConstraint(name = "uk_subscription_post_day", columnNames = {"subscription_id", "postingDate"})
})
@Getter
@Setter
@NoArgsConstructor
public class SubscriptionPosting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "subscription_id", nullable = false)
    private Subscription subscription;

    @Column(nullable = false)
    private LocalDate postingDate;

    @Column(length = 20, nullable = false)
    private String status;

    @Column(length = 300)
    private String details;
}
