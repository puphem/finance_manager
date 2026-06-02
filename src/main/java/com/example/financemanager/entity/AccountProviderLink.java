package com.example.financemanager.entity;

import com.example.financemanager.entity.enums.LoginProvider;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "account_provider_links", uniqueConstraints = {
        @UniqueConstraint(name = "uk_provider_subject", columnNames = {"provider", "providerUserId"})
})
@Getter
@Setter
@NoArgsConstructor
public class AccountProviderLink {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LoginProvider provider;

    @Column(nullable = false, length = 200)
    private String providerUserId;

    @Column(length = 200)
    private String providerEmail;
}
