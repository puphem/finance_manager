package com.example.financemanager.service.impl;

import com.example.financemanager.dto.CategoryRequestDto;
import com.example.financemanager.dto.CategoryResponseDto;
import com.example.financemanager.entity.Category;
import com.example.financemanager.exception.DuplicateResourceException;
import com.example.financemanager.exception.ResourceNotFoundException;
import com.example.financemanager.mapper.CategoryMapper;
import com.example.financemanager.repository.CategoryRepository;
import com.example.financemanager.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor // Создает конструктор для всех final полей (Lombok)
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;

    @Override
    @Transactional
    public CategoryResponseDto createCategory(CategoryRequestDto categoryDto) {
        // Проверяем, не существует ли уже категория с таким именем
        categoryRepository.findByName(categoryDto.getName()).ifPresent(c -> {
            throw new DuplicateResourceException("Категория с названием '" + categoryDto.getName() + "' уже существует.");
        });

        // 1. Преобразуем DTO в Entity
        Category category = categoryMapper.toEntity(categoryDto);
        // 2. Сохраняем Entity в базу данных
        Category savedCategory = categoryRepository.save(category);
        // 3. Преобразуем сохраненную Entity обратно в DTO для ответа
        return categoryMapper.toResponseDto(savedCategory);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponseDto> getAllCategories() {
        return categoryRepository.findAll().stream()
                .map(categoryMapper::toResponseDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public CategoryResponseDto updateCategory(Long id, CategoryRequestDto categoryDto) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Категория с ID " + id + " не найдена."));

        category.setName(categoryDto.getName());
        category.setColor(categoryDto.getColor());
        category.setIcon(categoryDto.getIcon());

        Category updatedCategory = categoryRepository.save(category);
        return categoryMapper.toResponseDto(updatedCategory);
    }

    @Override
    @Transactional
    public void deleteCategory(Long id) {
        if (!categoryRepository.existsById(id)) {
            throw new ResourceNotFoundException("Категория с ID " + id + " не найдена.");
        }
        categoryRepository.deleteById(id);
    }
}
