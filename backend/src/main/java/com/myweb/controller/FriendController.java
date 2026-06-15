package com.myweb.controller;

import com.myweb.service.FriendService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/friends")
@RequiredArgsConstructor
public class FriendController {

    private final FriendService friendService;

    @PostMapping
    public ResponseEntity<?> add(@RequestBody Map<String, String> body) {
        try {
            var friend = friendService.sendRequest(body.get("friendEmail"));
            return ResponseEntity.ok(Map.of("code", 0, "data", friend));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("code", 1005, "message", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> handle(@PathVariable Long id, @RequestBody Map<String, Boolean> body) {
        try {
            friendService.handleRequest(id, Boolean.TRUE.equals(body.get("accept")));
            return ResponseEntity.ok(Map.of("code", 0, "message", "处理成功"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("code", 1005, "message", e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<?> list() {
        return ResponseEntity.ok(Map.of("code", 0, "data", friendService.listFriends()));
    }

    @DeleteMapping("/{email}")
    public ResponseEntity<?> delete(@PathVariable String email) {
        try {
            friendService.deleteFriend(email);
            return ResponseEntity.ok(Map.of("code", 0, "message", "删除成功"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("code", 1005, "message", e.getMessage()));
        }
    }
}
