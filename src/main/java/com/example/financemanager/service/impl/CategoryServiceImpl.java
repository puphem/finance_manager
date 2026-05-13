package com.example.financemanager.service.impl;

import com.example.financemanager.dto.CategoryRequestDto;
import com.example.financemanager.dto.CategoryResponseDto;
import com.example.financemanager.dto.SubcategoryRequestDto;
import com.example.financemanager.dto.SubcategoryResponseDto;
import com.example.financemanager.entity.Category;
import com.example.financemanager.entity.Subcategory;
import com.example.financemanager.entity.User;
import com.example.financemanager.exception.DuplicateResourceException;
import com.example.financemanager.exception.ResourceNotFoundException;
import com.example.financemanager.mapper.CategoryMapper;
import com.example.financemanager.mapper.SubcategoryMapper;
import com.example.financemanager.repository.CategoryRepository;
import com.example.financemanager.repository.SubcategoryRepository;
import com.example.financemanager.service.CategoryService;
import com.example.financemanager.service.CurrentUserResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private static final Map<String, List<String>> DEFAULT_SUBCATEGORIES = createDefaultSubcategories();
    private final CategoryRepository categoryRepository;
    private final SubcategoryRepository subcategoryRepository;
    private final CategoryMapper categoryMapper;
    private final SubcategoryMapper subcategoryMapper;
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
        ensureDefaultSubcategories(savedCategory, user);
        return categoryMapper.toResponseDto(savedCategory);
    }

    @Override
    @Transactional
    public SubcategoryResponseDto createSubcategory(SubcategoryRequestDto subcategoryDto) {
        User user = currentUserResolver.getCurrentUser();
        Category category = categoryRepository.findById(subcategoryDto.getCategoryId())
                .filter(c -> c.getUser().getId().equals(user.getId()))
                .orElseThrow(() -> new ResourceNotFoundException("Категория с ID " + subcategoryDto.getCategoryId() + " не найдена."));

        if (subcategoryRepository.existsByCategoryIdAndCategoryUserAndNameIgnoreCase(category.getId(), user, subcategoryDto.getName())) {
            throw new DuplicateResourceException("Подкатегория с названием '" + subcategoryDto.getName() + "' уже существует.");
        }

        Subcategory subcategory = new Subcategory();
        subcategory.setName(subcategoryDto.getName().trim());
        subcategory.setCategory(category);
        Subcategory savedSubcategory = subcategoryRepository.save(subcategory);
        return subcategoryMapper.toResponseDto(savedSubcategory);
    }

    @Override
    @Transactional
    public List<CategoryResponseDto> getAllCategories() {
        User user = currentUserResolver.getCurrentUser();
        List<Category> categories = categoryRepository.findAllByUser(user);
        categories.forEach(category -> ensureDefaultSubcategories(category, user));
        return categories.stream()
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

    private void ensureDefaultSubcategories(Category category, User user) {
        List<String> defaults = resolveDefaultSubcategoryNames(category.getName());
        if (defaults.isEmpty()) {
            return;
        }

        Set<String> existingNames = subcategoryRepository.findByCategoryIdAndCategoryUser(category.getId(), user).stream()
                .map(Subcategory::getName)
                .filter(name -> name != null && !name.isBlank())
                .map(name -> name.toLowerCase(Locale.ROOT).trim())
                .collect(Collectors.toSet());

        for (String defaultName : defaults) {
            String normalized = defaultName.toLowerCase(Locale.ROOT).trim();
            if (existingNames.contains(normalized)) {
                continue;
            }
            Subcategory subcategory = new Subcategory();
            subcategory.setName(defaultName);
            subcategory.setCategory(category);
            subcategoryRepository.save(subcategory);
            existingNames.add(normalized);
        }
    }

    private List<String> resolveDefaultSubcategoryNames(String categoryName) {
        String normalizedCategoryName = categoryName == null ? "" : categoryName.toLowerCase(Locale.ROOT);
        for (Map.Entry<String, List<String>> template : DEFAULT_SUBCATEGORIES.entrySet()) {
            if (normalizedCategoryName.contains(template.getKey())) {
                return template.getValue();
            }
        }
        return List.of("Прочее");
    }

    private static Map<String, List<String>> createDefaultSubcategories() {
        Map<String, List<String>> templates = new LinkedHashMap<>();
        templates.put("продукт", List.of("Овощи и фрукты", "Молочные продукты", "Мясо и рыба", "Напитки", "Сладости", "Прочее"));
        templates.put("транспорт", List.of("Общественный транспорт", "Такси", "Топливо", "Парковка", "Прочее"));
        templates.put("такси", List.of("Поездки по городу", "Межгород", "Доставка", "Комфорт/Бизнес", "Прочее"));
        templates.put("кафе", List.of("Кафе", "Рестораны", "Фастфуд", "Доставка еды", "Прочее"));
        templates.put("ресторан", List.of("Кафе", "Рестораны", "Фастфуд", "Доставка еды", "Прочее"));
        templates.put("развлеч", List.of("Кино и сериалы", "Игры", "Концерты", "Хобби", "Подписки", "Прочее"));
        templates.put("счет", List.of("ЖКХ", "Интернет и связь", "Налоги и штрафы", "Аренда/Ипотека", "Прочее"));
        templates.put("дом", List.of("Ремонт", "Мебель", "Бытовая химия", "Техника", "Хозтовары", "Прочее"));
        templates.put("здоров", List.of("Аптека", "Врачи и анализы", "Стоматология", "Спорт и здоровье", "Прочее"));
        templates.put("одеж", List.of("Повседневная одежда", "Обувь", "Аксессуары", "Спорттовары", "Прочее"));
        templates.put("образов", List.of("Курсы", "Книги", "Репетиторы", "Онлайн-платформы", "Прочее"));
        templates.put("питом", List.of("Корм", "Ветклиника", "Аксессуары для питомцев", "Груминг", "Прочее"));
        templates.put("красот", List.of("Косметика", "Салон", "Уход за собой", "Парфюмерия", "Прочее"));
        return templates;
    }
}
