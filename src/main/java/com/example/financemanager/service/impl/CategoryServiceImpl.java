package com.example.financemanager.service.impl;

import com.example.financemanager.dto.CategoryRequestDto;
import com.example.financemanager.dto.CategoryResponseDto;
import com.example.financemanager.entity.Category;
import com.example.financemanager.entity.User;
import com.example.financemanager.exception.DuplicateResourceException;
import com.example.financemanager.exception.ResourceNotFoundException;
import com.example.financemanager.mapper.CategoryMapper;
import com.example.financemanager.repository.CategoryRepository;
import com.example.financemanager.service.CategoryService;
import com.example.financemanager.service.CurrentUserResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;
    private final CurrentUserResolver currentUserResolver;

    @Override
    @Transactional
    public CategoryResponseDto createCategory(CategoryRequestDto categoryDto) {
        User user = currentUserResolver.getCurrentUser();
        categoryRepository.findByNameAndUser(categoryDto.getName(), user).ifPresent(c -> {
            throw new DuplicateResourceException("Категория с названием '" + categoryDto.getName() + "' уже существует.");
        });

        Category category = categoryMapper.toEntity(categoryDto);
        category.setUser(user);
        Category savedCategory = categoryRepository.save(category);
        return categoryMapper.toResponseDto(savedCategory);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponseDto> getAllCategories() {
        User user = currentUserResolver.getCurrentUser();
        return categoryRepository.findAllByUser(user).stream()
                .map(categoryMapper::toResponseDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public CategoryResponseDto updateCategory(Long id, CategoryRequestDto categoryDto) {
        User user = currentUserResolver.getCurrentUser();
        Category category = categoryRepository.findById(id)
                .filter(c -> c.getUser().getId().equals(user.getId()))
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
        User user = currentUserResolver.getCurrentUser();
        if (!categoryRepository.existsByIdAndUser(id, user)) {
            throw new ResourceNotFoundException("Категория с ID " + id + " не найдена.");
        }
        categoryRepository.deleteById(id);
    }
}
