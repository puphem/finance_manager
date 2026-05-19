package com.example.financemanager.service;

import com.example.financemanager.dto.AuthResponseDto;
import com.example.financemanager.dto.LoginRequestDto;
import com.example.financemanager.dto.RegisterRequestDto;
import com.example.financemanager.dto.UpdatePasswordRequestDto;
import com.example.financemanager.dto.UpdateUsernameRequestDto;
import com.example.financemanager.entity.Category;
import com.example.financemanager.entity.Subcategory;
import com.example.financemanager.entity.User;
import com.example.financemanager.exception.DuplicateResourceException;
import com.example.financemanager.repository.CategoryRepository;
import com.example.financemanager.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final CurrentUserResolver currentUserResolver;

    @Transactional
    public AuthResponseDto register(RegisterRequestDto request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new DuplicateResourceException("Пользователь с логином '" + request.getUsername() + "' уже существует.");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        userRepository.save(user);

        seedDefaultCategories(user);

        String token = jwtService.generateToken(user);
        return new AuthResponseDto(token, user.getUsername());
    }

    public AuthResponseDto login(LoginRequestDto request) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new BadCredentialsException("Неверный логин или пароль."));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BadCredentialsException("Неверный логин или пароль.");
        }

        String token = jwtService.generateToken(user);
        return new AuthResponseDto(token, user.getUsername());
    }

    @Transactional
    public AuthResponseDto updateUsername(UpdateUsernameRequestDto request) {
        User user = currentUserResolver.getCurrentUser();
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new IllegalArgumentException("Текущий пароль указан неверно.");
        }

        String nextUsername = request.getNewUsername().trim();
        if (!nextUsername.equals(user.getUsername()) && userRepository.existsByUsername(nextUsername)) {
            throw new DuplicateResourceException("Пользователь с логином '" + nextUsername + "' уже существует.");
        }

        user.setUsername(nextUsername);
        userRepository.save(user);
        String token = jwtService.generateToken(user);
        return new AuthResponseDto(token, user.getUsername());
    }

    @Transactional
    public AuthResponseDto updatePassword(UpdatePasswordRequestDto request) {
        User user = currentUserResolver.getCurrentUser();
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new IllegalArgumentException("Текущий пароль указан неверно.");
        }
        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
            throw new IllegalArgumentException("Новый пароль должен отличаться от текущего.");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        String token = jwtService.generateToken(user);
        return new AuthResponseDto(token, user.getUsername());
    }

    private void seedDefaultCategories(User user) {
        List<Category> defaultCategories = List.of(
                createCategory(user, "Продукты", "#27AE60", "fas fa-shopping-basket",
                        "Напитки", "Мясо и рыба", "Молочные продукты", "Сладости", "Овощи и фрукты", "Бытовые продукты", "Прочее"),
                createCategory(user, "Транспорт", "#2980B9", "fas fa-bus",
                        "Общественный транспорт", "Топливо", "Парковка", "Каршеринг", "Обслуживание авто", "Прочее"),
                createCategory(user, "Такси", "#1F8A70", "fas fa-taxi",
                        "Поездки по городу", "Межгород", "Доставка", "Комфорт/бизнес", "Прочее"),
                createCategory(user, "Развлечения", "#F39C12", "fas fa-film",
                        "Кино и сериалы", "Игры", "Концерты", "Хобби", "Подписки", "Прочее"),
                createCategory(user, "Счета", "#C0392B", "fas fa-file-invoice-dollar",
                        "ЖКХ", "Интернет и связь", "Налоги и штрафы", "Аренда/ипотека", "Прочее"),
                createCategory(user, "Дом", "#8E44AD", "fas fa-house-user",
                        "Ремонт", "Мебель", "Бытовая химия", "Техника", "Хозтовары", "Прочее"),
                createCategory(user, "Здоровье", "#E74C3C", "fas fa-heartbeat",
                        "Аптека", "Врачи и анализы", "Стоматология", "Спорт и здоровье", "Прочее"),
                createCategory(user, "Одежда", "#9B59B6", "fas fa-tshirt",
                        "Повседневная одежда", "Обувь", "Аксессуары", "Спорттовары", "Прочее"),
                createCategory(user, "Кафе и рестораны", "#D35400", "fas fa-utensils",
                        "Кафе", "Рестораны", "Фастфуд", "Доставка еды", "Кофейни", "Прочее"),
                createCategory(user, "Образование", "#34495E", "fas fa-graduation-cap",
                        "Курсы", "Книги", "Репетиторы", "Онлайн-платформы", "Прочее"),
                createCategory(user, "Питомцы", "#7F8C8D", "fas fa-paw",
                        "Корм", "Ветклиника", "Аксессуары для питомцев", "Груминг", "Прочее"),
                createCategory(user, "Красота", "#E84393", "fas fa-spa",
                        "Косметика", "Салон", "Уход за собой", "Парфюмерия", "Прочее")
        );
        categoryRepository.saveAll(defaultCategories);
    }

    private Category createCategory(User user, String name, String color, String icon, String... subcategoryNames) {
        Category category = new Category();
        category.setUser(user);
        category.setName(name);
        category.setColor(color);
        category.setIcon(icon);
        List<Subcategory> subcategories = List.of(subcategoryNames).stream().map(subcategoryName -> {
            Subcategory subcategory = new Subcategory();
            subcategory.setName(subcategoryName);
            subcategory.setCategory(category);
            return subcategory;
        }).toList();
        category.setSubcategories(subcategories);
        return category;
    }
}
