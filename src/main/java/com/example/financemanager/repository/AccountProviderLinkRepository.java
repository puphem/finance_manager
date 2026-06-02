package com.example.financemanager.repository;

import com.example.financemanager.entity.AccountProviderLink;
import com.example.financemanager.entity.User;
import com.example.financemanager.entity.enums.LoginProvider;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AccountProviderLinkRepository extends JpaRepository<AccountProviderLink, Long> {
    Optional<AccountProviderLink> findByProviderAndProviderUserId(LoginProvider provider, String providerUserId);
    List<AccountProviderLink> findAllByUser(User user);
    boolean existsByUserAndProvider(User user, LoginProvider provider);
}
