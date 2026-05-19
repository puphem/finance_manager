package com.example.financemanager.repository;

import com.example.financemanager.entity.Category;
import com.example.financemanager.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {
    List<Category> findAllByUser(User user);
    Optional<Category> findByNameAndUser(String name, User user);
    Optional<Category> findByNameIgnoreCaseAndUser(String name, User user);
    boolean existsByIdAndUser(Long id, User user);
    void deleteAllByUser(User user);
}
