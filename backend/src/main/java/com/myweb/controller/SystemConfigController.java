package com.myweb.controller;

import com.myweb.service.SystemConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class SystemConfigController {

    private final SystemConfigService configService;

    @GetMapping("/config")
    public ResponseEntity<?> getConfigs() {
        return ResponseEntity.ok(Map.of("code", 0, "data", configService.getAllConfigs()));
    }

    @PutMapping("/config")
    public ResponseEntity<?> updateConfigs(@RequestBody Map<String, String> configs) {
        try {
            configService.updateConfig(configs);
            return ResponseEntity.ok(Map.of("code", 0, "message", "配置更新成功"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("code", 1005, "message", e.getMessage()));
        }
    }
}
