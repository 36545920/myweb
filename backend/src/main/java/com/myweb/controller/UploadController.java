package com.myweb.controller;

import com.myweb.service.UploadService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/upload")
@RequiredArgsConstructor
public class UploadController {

    private final UploadService uploadService;

    @PostMapping("/init")
    public ResponseEntity<?> init(@RequestBody Map<String, Object> body) {
        String originalName = (String) body.get("originalName");
        long fileSize = ((Number) body.get("fileSize")).longValue();
        var resp = uploadService.initUpload(originalName, fileSize);
        return ResponseEntity.ok(Map.of("code", 0, "data", resp));
    }

    @PostMapping("/{uploadId}/chunk/{index}")
    public ResponseEntity<?> chunk(@PathVariable String uploadId,
                                    @PathVariable int index,
                                    HttpServletRequest request) {
        try {
            uploadService.uploadChunk(uploadId, index, request.getInputStream());
            return ResponseEntity.ok(Map.of("code", 0, "message", "ok"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("code", 2001, "message", e.getMessage()));
        }
    }

    @GetMapping("/{uploadId}/status")
    public ResponseEntity<?> status(@PathVariable String uploadId) {
        try {
            var resp = uploadService.getStatus(uploadId);
            return ResponseEntity.ok(Map.of("code", 0, "data", resp));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("code", 2001, "message", e.getMessage()));
        }
    }

    @PostMapping("/{uploadId}/complete")
    public ResponseEntity<?> complete(@PathVariable String uploadId) {
        try {
            var pathData = uploadService.completeUpload(uploadId);
            return ResponseEntity.ok(Map.of("code", 0, "data", Map.of(
                "storagePath", pathData.path().toString(),
                "originalName", pathData.originalName(),
                "fileSize", pathData.fileSize()
            )));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("code", 2001, "message", e.getMessage()));
        }
    }
}
