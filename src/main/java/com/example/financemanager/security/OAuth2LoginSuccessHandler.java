package com.example.financemanager.security;

import com.example.financemanager.entity.User;
import com.example.financemanager.service.AuthService;
import com.example.financemanager.service.JwtService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final AuthService authService;
    private final JwtService jwtService;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();

        // Получаем данные из Яндекса
        String email = oAuth2User.getAttribute("default_email");
        if (email == null) {
            email = oAuth2User.getAttribute("login") + "@yandex.ru";
        }
        String displayName = oAuth2User.getAttribute("real_name");

        // Создаем или получаем пользователя
        User user = authService.processOAuthUser(email, displayName);

        // Генерируем наш JWT токен
        String token = jwtService.generateToken(user);

        // Перенаправляем на фронтенд с токеном в URL
        String targetUrl = UriComponentsBuilder.fromUriString("/")
                .queryParam("token", token)
                .queryParam("username", URLEncoder.encode(user.getUsername(), StandardCharsets.UTF_8))
                .queryParam("displayName", URLEncoder.encode(user.getDisplayName(), StandardCharsets.UTF_8))
                .build().toUriString();

        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}