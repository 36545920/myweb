package com.myweb.controller;

import com.myweb.model.vo.FileVO;
import com.myweb.service.FileService;
import com.myweb.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
public class AdminController {

    private final UserService userService;
    private final FileService fileService;

    // === 用户管理 ===
    @GetMapping("/admin/users")
    public ResponseEntity<?> listUsers(@RequestParam(defaultValue = "1") int page,
                                        @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(Map.of("code", 0, "data", userService.listUsers(page, size)));
    }

    @PutMapping("/admin/users/{email}/quota")
    public ResponseEntity<?> updateQuota(@PathVariable String email, @RequestBody Map<String, Object> body) {
        try {
            long quota = ((Number) body.get("quota")).longValue();
            userService.updateQuota(email, quota);
            return ResponseEntity.ok(Map.of("code", 0, "message", "配额更新成功"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("code", 1005, "message", e.getMessage()));
        }
    }

    @PutMapping("/admin/users/{email}/status")
    public ResponseEntity<?> updateStatus(@PathVariable String email, @RequestBody Map<String, String> body) {
        try {
            userService.updateStatus(email, body.get("status"));
            return ResponseEntity.ok(Map.of("code", 0, "message", "状态更新成功"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("code", 1005, "message", e.getMessage()));
        }
    }

    @PutMapping("/admin/users/{email}/reset-password")
    public ResponseEntity<?> resetPassword(@PathVariable String email) {
        try {
            String newPassword = userService.resetPassword(email);
            return ResponseEntity.ok(Map.of("code", 0, "message", "密码已重置为: " + newPassword));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("code", 1005, "message", e.getMessage()));
        }
    }

    // === 共享池审核 ===
    @GetMapping("/pool")
    public ResponseEntity<?> listPool(@RequestParam(defaultValue = "1") int page,
                                       @RequestParam(defaultValue = "20") int size) {
        var pageResult = fileService.listPoolFiles(page, size);
        return ResponseEntity.ok(Map.of("code", 0, "data", Map.of(
            "total", pageResult.getTotal(),
            "records", pageResult.getRecords().stream().map(FileVO::from).toList()
        )));
    }

    @GetMapping("/admin/review")
    public ResponseEntity<?> listReview(@RequestParam(defaultValue = "1") int page,
                                         @RequestParam(defaultValue = "20") int size) {
        var pageResult = fileService.listPendingReviews(page, size);
        return ResponseEntity.ok(Map.of("code", 0, "data", Map.of(
            "total", pageResult.getTotal(),
            "records", pageResult.getRecords().stream().map(FileVO::from).toList()
        )));
    }

    @PutMapping("/admin/review/{id}")
    public ResponseEntity<?> review(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        try {
            boolean approved = Boolean.TRUE.equals(body.get("approved"));
            String comment = (String) body.getOrDefault("comment", "");
            fileService.reviewFile(id, approved, comment);
            return ResponseEntity.ok(Map.of("code", 0, "message", "审核完成"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("code", 1005, "message", e.getMessage()));
        }
    }

    // === 超级管理员 ===
    @PutMapping("/super-admin/users/{email}/role")
    public ResponseEntity<?> updateRole(@PathVariable String email, @RequestBody Map<String, String> body) {
        try {
            userService.updateRole(email, body.get("role"));
            return ResponseEntity.ok(Map.of("code", 0, "message", "角色更新成功"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("code", 1005, "message", e.getMessage()));
        }
    }
}
