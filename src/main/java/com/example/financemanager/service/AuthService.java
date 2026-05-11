package com.example.financemanager.service;

import com.example.financemanager.dto.AuthResponseDto;
import com.example.financemanager.dto.LoginRequestDto;
import com.example.financemanager.dto.RegisterRequestDto;
import com.example.financemanager.entity.Category;
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

    private void seedDefaultCategories(User user) {
        List<Category> defaultCategories = List.of(
                createCategory(user, "Продукты", "#27AE60", "fas fa-shopping-basket"),
                createCategory(user, "Транспорт", "#2980B9", "fas fa-bus"),
                createCategory(user, "Такси", "#1F8A70", "fas fa-taxi"),
                createCategory(user, "Развлечения", "#F39C12", "fas fa-film"),
                createCategory(user, "Счета", "#C0392B", "fas fa-file-invoice-dollar"),
                createCategory(user, "Дом", "#8E44AD", "fas fa-house-user"),
                createCategory(user, "Здоровье", "#E74C3C", "fas fa-heartbeat"),
                createCategory(user, "Одежда", "#9B59B6", "fas fa-tshirt"),
                createCategory(user, "Кафе и рестораны", "#D35400", "fas fa-utensils"),
                createCategory(user, "Образование", "#34495E", "fas fa-graduation-cap"),
                createCategory(user, "Питомцы", "#7F8C8D", "fas fa-paw"),
                createCategory(user, "Красота", "#E84393", "fas fa-spa")
        );
        categoryRepository.saveAll(defaultCategories);
    }

    private Category createCategory(User user, String name, String color, String icon) {
        Category category = new Category();
        category.setUser(user);
        category.setName(name);
        category.setColor(color);
        category.setIcon(icon);
        return category;
    }
}
