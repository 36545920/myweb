package com.myweb.controller;

import com.myweb.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<?> profile() {
        return ResponseEntity.ok(Map.of("code", 0, "data", userService.getProfile()));
    }

    @PutMapping("/me")
    public ResponseEntity<?> updateProfile(@RequestBody Map<String, String> body) {
        try {
            var vo = userService.updateProfile(
                body.get("nickname"), body.get("avatar"),
                body.get("oldPassword"), body.get("newPassword"));
            return ResponseEntity.ok(Map.of("code", 0, "data", vo));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("code", 1005, "message", e.getMessage()));
        }
    }

    @GetMapping("/search")
    public ResponseEntity<?> search(@RequestParam String email) {
        return ResponseEntity.ok(Map.of("code", 0, "data", userService.searchUsers(email)));
    }
}
