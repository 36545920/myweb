package com.myweb.controller;

import com.myweb.util.FileUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequiredArgsConstructor
public class HealthController {

    private final FileUtil fileUtil;

    @GetMapping("/health")
    public ResponseEntity<?> health() {
        boolean online = fileUtil.isFileServerOnline();
        return ResponseEntity.ok(Map.of(
            "code", 0,
            "data", Map.of(
                "nas", online,
                "status", online ? "ok" : "nas_unavailable"
            )
        ));
    }
}
