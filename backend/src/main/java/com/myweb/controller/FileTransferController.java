package com.myweb.controller;

import com.myweb.service.FileTransferService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/transfers")
@RequiredArgsConstructor
public class FileTransferController {

    private final FileTransferService transferService;

    @PostMapping
    public ResponseEntity<?> send(@RequestBody Map<String, Object> body) {
        try {
            Long fileId = ((Number) body.get("fileId")).longValue();
            String toEmail = (String) body.get("toEmail");
            String message = (String) body.get("message");
            var transfer = transferService.send(fileId, toEmail, message);
            return ResponseEntity.ok(Map.of("code", 0, "data", transfer));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("code", 1005, "message", e.getMessage()));
        }
    }

    @GetMapping("/inbox")
    public ResponseEntity<?> inbox(@RequestParam(defaultValue = "1") int page,
                                    @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(Map.of("code", 0, "data", transferService.listInbox(page, size)));
    }

    @GetMapping("/sent")
    public ResponseEntity<?> sent(@RequestParam(defaultValue = "1") int page,
                                   @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(Map.of("code", 0, "data", transferService.listSent(page, size)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        try {
            transferService.delete(id);
            return ResponseEntity.ok(Map.of("code", 0, "message", "删除成功"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("code", 1005, "message", e.getMessage()));
        }
    }
}
