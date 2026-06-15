package com.myweb.controller;

import com.myweb.model.vo.FileVO;
import com.myweb.service.FileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/files")
@RequiredArgsConstructor
public class FileController {

    private final FileService fileService;

    @PostMapping
    public ResponseEntity<?> create(@RequestBody Map<String, Object> body) {
        try {
            String title = (String) body.get("title");
            String description = (String) body.get("description");
            boolean isSharedPool = Boolean.TRUE.equals(body.get("isSharedPool"));
            String expireAtStr = (String) body.get("expireAt");
            LocalDateTime expireAt = expireAtStr != null ? LocalDateTime.parse(expireAtStr) : null;
            String storagePath = (String) body.get("storagePath");
            String originalName = (String) body.get("originalName");
            long fileSize = ((Number) body.get("fileSize")).longValue();
            String mimeType = (String) body.get("mimeType");

            var file = fileService.createFile(title, description, isSharedPool,
                expireAt, Path.of(storagePath), originalName, fileSize, mimeType);
            return ResponseEntity.ok(Map.of("code", 0, "data", FileVO.from(file)));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("code", 1005, "message", e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<?> list(@RequestParam(defaultValue = "1") int page,
                                   @RequestParam(defaultValue = "20") int size) {
        var pageResult = fileService.listMyFiles(page, size);
        return ResponseEntity.ok(Map.of("code", 0, "data", Map.of(
            "total", pageResult.getTotal(),
            "records", pageResult.getRecords().stream().map(FileVO::from).toList()
        )));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> get(@PathVariable Long id) {
        var file = fileService.getFile(id);
        if (file == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(Map.of("code", 0, "data", FileVO.from(file)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        try {
            fileService.deleteFile(id);
            return ResponseEntity.ok(Map.of("code", 0, "message", "删除成功"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("code", 1005, "message", e.getMessage()));
        }
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<?> download(@PathVariable Long id) {
        try {
            var file = fileService.getFile(id);
            var bytes = fileService.downloadFile(id).readAllBytes();
            return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=\"" + file.getOriginalName() + "\"")
                .header("Content-Type", file.getMimeType() != null ? file.getMimeType() : "application/octet-stream")
                .header("Content-Length", String.valueOf(bytes.length))
                .body(bytes);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("code", 1005, "message", e.getMessage()));
        }
    }
}
