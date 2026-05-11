package com.example.financemanager.repository;

import com.example.financemanager.entity.User;
import com.example.financemanager.entity.Subcategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SubcategoryRepository extends JpaRepository<Subcategory, Long> {
    List<Subcategory> findByCategoryId(Long categoryId);
    List<Subcategory> findByCategoryIdAndCategoryUser(Long categoryId, User user);
    Optional<Subcategory> findByIdAndCategoryUser(Long id, User user);
}
