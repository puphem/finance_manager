package com.example.financemanager.repository;

import com.example.financemanager.entity.User;
import com.example.financemanager.entity.UserCategoryModelStats;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserCategoryModelStatsRepository extends JpaRepository<UserCategoryModelStats, Long> {
    Optional<UserCategoryModelStats> findByUser(User user);
}
