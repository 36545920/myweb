package com.myweb.controller;

import com.myweb.model.dto.LoginRequest;
import com.myweb.model.dto.RegisterRequest;
import com.myweb.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/send-code")
    public ResponseEntity<?> sendCode(@RequestBody Map<String, String> body) {
        authService.sendVerificationCode(body.get("email"));
        return ResponseEntity.ok(Map.of("code", 0, "message", "验证码已发送"));
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest req,
                                       @RequestParam String code) {
        try {
            var resp = authService.register(req, code);
            return ResponseEntity.ok(Map.of("code", 0, "message", "success", "data", resp));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("code", 1005, "message", e.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest req) {
        try {
            var resp = authService.login(req);
            return ResponseEntity.ok(Map.of("code", 0, "message", "success", "data", resp));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("code", 1005, "message", e.getMessage()));
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@RequestBody Map<String, String> body) {
        try {
            var resp = authService.refresh(body.get("refreshToken"));
            return ResponseEntity.ok(Map.of("code", 0, "message", "success", "data", resp));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("code", 1002, "message", e.getMessage()));
        }
    }
}
