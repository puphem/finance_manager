package com.example.financemanager.controller;

import com.example.financemanager.dto.AuthResponseDto;
import com.example.financemanager.dto.UpdatePasswordRequestDto;
import com.example.financemanager.dto.UpdateUsernameRequestDto;
import com.example.financemanager.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/account")
@RequiredArgsConstructor
public class AccountController {

    private final AuthService authService;

    @PutMapping("/username")
    public ResponseEntity<AuthResponseDto> updateUsername(@Valid @RequestBody UpdateUsernameRequestDto request) {
        return ResponseEntity.ok(authService.updateUsername(request));
    }

    @PutMapping("/password")
    public ResponseEntity<AuthResponseDto> updatePassword(@Valid @RequestBody UpdatePasswordRequestDto request) {
        return ResponseEntity.ok(authService.updatePassword(request));
    }
}

