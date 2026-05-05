package com.example.financemanager.repository;

import com.example.financemanager.entity.Subcategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SubcategoryRepository extends JpaRepository<Subcategory, Long> {
    // Кастомный метод для поиска всех подкатегорий по ID родительской категории
    List<Subcategory> findByCategoryId(Long categoryId);
}
