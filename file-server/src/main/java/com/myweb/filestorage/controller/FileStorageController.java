package com.myweb.filestorage.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/file")
public class FileStorageController {

    @Value("${app.storage-path}")
    private String storagePath;

    @Value("${app.upload-tmp-path}")
    private String tmpPath;

    @Value("${app.auth-token}")
    private String authToken;

    // 分片上传状态：uploadId → set of uploaded chunk indexes
    private final Map<String, Set<Integer>> chunkStatus = new ConcurrentHashMap<>();

    private void checkAuth(HttpServletRequest request) {
        String token = request.getHeader("X-Auth-Token");
        if (!authToken.equals(token)) {
            throw new RuntimeException("未授权");
        }
    }

    // === 分片上传 ===

    @PostMapping("/chunk")
    public ResponseEntity<?> uploadChunk(@RequestParam String uploadId,
                                          @RequestParam int index,
                                          HttpServletRequest request) throws IOException {
        checkAuth(request);
        Path chunkDir = Path.of(tmpPath, uploadId);
        Files.createDirectories(chunkDir);
        Path chunkFile = chunkDir.resolve(index + ".chunk");
        Files.copy(request.getInputStream(), chunkFile, StandardCopyOption.REPLACE_EXISTING);

        chunkStatus.computeIfAbsent(uploadId, k -> ConcurrentHashMap.newKeySet()).add(index);
        return ResponseEntity.ok(Map.of("status", "ok", "index", index));
    }

    @GetMapping("/chunk-status")
    public ResponseEntity<?> chunkStatus(@RequestParam String uploadId,
                                          HttpServletRequest request) {
        checkAuth(request);
        Set<Integer> uploaded = chunkStatus.getOrDefault(uploadId, Set.of());
        return ResponseEntity.ok(Map.of("uploaded", uploaded.stream().sorted().toList()));
    }

    @PostMapping("/merge")
    public ResponseEntity<?> mergeChunks(@RequestParam String uploadId,
                                          @RequestParam String fileName,
                                          @RequestParam int totalChunks,
                                          HttpServletRequest request) throws IOException {
        checkAuth(request);
        Set<Integer> uploaded = chunkStatus.get(uploadId);
        if (uploaded == null || uploaded.size() != totalChunks) {
            return ResponseEntity.badRequest().body(Map.of("error", "分片不完整"));
        }

        // 生成唯一文件名
        String ext = "";
        int dotIdx = fileName.lastIndexOf('.');
        if (dotIdx > 0) ext = fileName.substring(dotIdx);
        String storedName = UUID.randomUUID() + ext;

        Path chunkDir = Path.of(tmpPath, uploadId);
        Files.createDirectories(Path.of(storagePath));
        Path finalPath = Path.of(storagePath, storedName);

        try (OutputStream out = Files.newOutputStream(finalPath)) {
            for (int i = 0; i < totalChunks; i++) {
                Path chunkFile = chunkDir.resolve(i + ".chunk");
                if (!Files.exists(chunkFile)) {
                    return ResponseEntity.badRequest().body(Map.of("error", "分片 " + i + " 缺失"));
                }
                Files.copy(chunkFile, out);
            }
        }

        // 清理临时分片
        deleteDir(chunkDir);
        chunkStatus.remove(uploadId);

        return ResponseEntity.ok(Map.of(
            "status", "ok",
            "path", storedName,
            "fullPath", finalPath.toString()
        ));
    }

    // === 完整文件上传（小文件用） ===

    @PostMapping("/upload")
    public ResponseEntity<?> uploadFile(@RequestParam String path,
                                         HttpServletRequest request) throws IOException {
        checkAuth(request);
        Path target = Path.of(storagePath, path);
        Files.createDirectories(target.getParent());
        Files.copy(request.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        return ResponseEntity.ok(Map.of("status", "ok", "path", path));
    }

    // === 下载 ===

    @GetMapping("/download")
    public ResponseEntity<?> downloadFile(@RequestParam String path,
                                           HttpServletRequest request) throws IOException {
        checkAuth(request);
        Path file = Path.of(storagePath, path);
        if (!Files.exists(file)) {
            return ResponseEntity.notFound().build();
        }
        byte[] bytes = Files.readAllBytes(file);
        return ResponseEntity.ok()
                .header("Content-Type", "application/octet-stream")
                .header("Content-Length", String.valueOf(bytes.length))
                .body(bytes);
    }

    // === 删除 ===

    @DeleteMapping("/delete")
    public ResponseEntity<?> deleteFile(@RequestParam String path,
                                         HttpServletRequest request) throws IOException {
        checkAuth(request);
        Files.deleteIfExists(Path.of(storagePath, path));
        return ResponseEntity.ok(Map.of("status", "deleted"));
    }

    // === 健康检查 ===

    @GetMapping("/health")
    public ResponseEntity<?> health() {
        boolean ok = Files.exists(Path.of(storagePath));
        return ResponseEntity.ok(Map.of(
            "status", ok ? "ok" : "unavailable",
            "diskFree", new File(storagePath).getFreeSpace()
        ));
    }

    // === 清理 ===

    @DeleteMapping("/cleanup-tmp")
    public ResponseEntity<?> cleanupTmp(HttpServletRequest request) throws IOException {
        checkAuth(request);
        Path tmpDir = Path.of(tmpPath);
        if (Files.exists(tmpDir)) {
            deleteDir(tmpDir);
            Files.createDirectories(tmpDir);
        }
        chunkStatus.clear();
        return ResponseEntity.ok(Map.of("status", "cleaned"));
    }

    private void deleteDir(Path dir) throws IOException {
        if (Files.exists(dir)) {
            try (var stream = Files.walk(dir)) {
                stream.sorted(java.util.Comparator.reverseOrder())
                      .forEach(p -> { try { Files.delete(p); } catch (IOException ignored) {} });
            }
        }
    }
}
