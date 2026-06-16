package com.example.financemanager.service;

import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

@Service
public class TotpService {

    private static final int WINDOW_SECONDS = 30;
    private static final int CODE_DIGITS = 6;
    private static final SecureRandom RANDOM = new SecureRandom();

    public String generateSecret() {
        byte[] bytes = new byte[20];
        RANDOM.nextBytes(bytes);
        return Base64.getEncoder().withoutPadding().encodeToString(bytes);
    }

    public boolean verifyCode(String base64Secret, String inputCode) {
        if (base64Secret == null || base64Secret.isBlank() || inputCode == null || inputCode.isBlank()) {
            return false;
        }
        String normalized = inputCode.trim();
        long nowWindow = Instant.now().getEpochSecond() / WINDOW_SECONDS;
        for (long offset = -1; offset <= 1; offset++) {
            String candidate = generateTotp(base64Secret, nowWindow + offset);
            if (candidate.equals(normalized)) {
                return true;
            }
        }
        return false;
    }

    public List<String> generateRecoveryCodes() {
        List<String> codes = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            byte[] bytes = new byte[5];
            RANDOM.nextBytes(bytes);
            String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
                    .replace("-", "A")
                    .replace("_", "B");
            codes.add(raw.substring(0, Math.min(8, raw.length())).toUpperCase());
        }
        return codes;
    }

    public String buildOtpAuthUrl(String issuer, String username, String secret) {
        String normalizedIssuer = urlEncode(issuer == null || issuer.isBlank() ? "FinanceManager" : issuer);
        String normalizedAccount = urlEncode(username == null ? "user" : username);
        return "otpauth://totp/" + normalizedIssuer + ":" + normalizedAccount
                + "?secret=" + secret
                + "&issuer=" + normalizedIssuer
                + "&digits=" + CODE_DIGITS
                + "&period=" + WINDOW_SECONDS;
    }

    private String generateTotp(String base64Secret, long counter) {
        try {
            byte[] secret = Base64.getDecoder().decode(base64Secret);
            byte[] counterBytes = ByteBuffer.allocate(8).putLong(counter).array();
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(secret, "HmacSHA1"));
            byte[] hash = mac.doFinal(counterBytes);
            int offset = hash[hash.length - 1] & 0xF;
            int binary = ((hash[offset] & 0x7f) << 24)
                    | ((hash[offset + 1] & 0xff) << 16)
                    | ((hash[offset + 2] & 0xff) << 8)
                    | (hash[offset + 3] & 0xff);
            int otp = binary % (int) Math.pow(10, CODE_DIGITS);
            return String.format("%0" + CODE_DIGITS + "d", otp);
        } catch (Exception exception) {
            return "";
        }
    }

    private String urlEncode(String value) {
        return java.net.URLEncoder.encode(value, StandardCharsets.UTF_8)
                .replace("+", "%20");
    }
}
