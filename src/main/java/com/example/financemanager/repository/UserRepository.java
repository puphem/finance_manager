package com.example.financemanager.repository;

import com.example.financemanager.entity.User;
import com.example.financemanager.entity.enums.UserStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findByUsernameAndStatus(String username, UserStatus status);
    boolean existsByUsername(String username);
}
