package com.example.financemanager.controller;

import com.example.financemanager.dto.*;
import com.example.financemanager.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponseDto> register(@Valid @RequestBody RegisterRequestDto request) {
        return new ResponseEntity<>(authService.register(request), HttpStatus.CREATED);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponseDto> login(@Valid @RequestBody LoginRequestDto request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/oauth/login")
    public ResponseEntity<AuthResponseDto> oauthLogin(@Valid @RequestBody OAuthLoginRequestDto request) {
        return ResponseEntity.ok(authService.oauthLogin(request));
    }

    @PostMapping("/oauth/link")
    public ResponseEntity<Void> linkOAuth(@Valid @RequestBody OAuthLinkRequestDto request) {
        authService.linkOAuth(request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/mfa/setup")
    public ResponseEntity<MfaSetupResponseDto> setupTotp() {
        return ResponseEntity.ok(authService.setupTotp());
    }

    @PostMapping("/mfa/enable")
    public ResponseEntity<AuthResponseDto> enableTotp(@Valid @RequestBody MfaCodeRequestDto request) {
        return ResponseEntity.ok(authService.enableTotp(request));
    }

    @PostMapping("/mfa/disable")
    public ResponseEntity<AuthResponseDto> disableTotp(@Valid @RequestBody MfaCodeRequestDto request) {
        return ResponseEntity.ok(authService.disableTotp(request));
    }

    @PostMapping("/recovery/complete")
    public ResponseEntity<AuthResponseDto> completeRecovery(@Valid @RequestBody RecoveryFlowCompleteRequestDto request) {
        return ResponseEntity.ok(authService.completeRecoveryFlow(request));
    }
}
