package com.example.financemanager.controller;

import com.example.financemanager.dto.AuthResponseDto;
import com.example.financemanager.dto.DeactivateAccountRequestDto;
import com.example.financemanager.dto.UpdateDisplayNameRequestDto;
import com.example.financemanager.dto.UpdatePasswordRequestDto;
import com.example.financemanager.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/account")
@RequiredArgsConstructor
public class AccountController {

    private final AuthService authService;

    @PutMapping("/display-name")
    public ResponseEntity<AuthResponseDto> updateDisplayName(@Valid @RequestBody UpdateDisplayNameRequestDto request) {
        return ResponseEntity.ok(authService.updateDisplayName(request));
    }

    @PutMapping("/password")
    public ResponseEntity<AuthResponseDto> updatePassword(@Valid @RequestBody UpdatePasswordRequestDto request) {
        return ResponseEntity.ok(authService.updatePassword(request));
    }

    @PostMapping("/deactivate")
    public ResponseEntity<Void> deactivate(@Valid @RequestBody DeactivateAccountRequestDto request) {
        authService.softDeleteAccount(request);
        return ResponseEntity.noContent().build();
    }
}
