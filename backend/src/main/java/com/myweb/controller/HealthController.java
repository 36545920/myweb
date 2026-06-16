package com.myweb.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

@RestController
public class HealthController {

    @Value("${app.storage-path}")
    private String storagePath;

    @GetMapping("/health")
    public ResponseEntity<?> health() {
        boolean nasOk = Files.exists(Path.of(storagePath));
        return ResponseEntity.ok(Map.of(
            "code", 0,
            "data", Map.of(
                "nas", nasOk,
                "status", nasOk ? "ok" : "nas_unavailable"
            )
        ));
    }
}
