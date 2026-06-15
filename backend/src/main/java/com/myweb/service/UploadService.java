package com.myweb.service;

import com.myweb.util.FileUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class UploadService {

    private final FileUtil fileUtil;
    private final Map<String, UploadSession> sessions = new ConcurrentHashMap<>();

    public record UploadInitResponse(String uploadId, int chunkSize, int totalChunks, List<Integer> missingChunks) {}

    public UploadInitResponse initUpload(String originalName, long fileSize) {
        String uploadId = UUID.randomUUID().toString();
        int chunkSize = 5 * 1024 * 1024;
        int totalChunks = (int) Math.ceil((double) fileSize / chunkSize);

        UploadSession session = new UploadSession();
        session.originalName = originalName;
        session.fileSize = fileSize;
        session.totalChunks = totalChunks;
        session.chunkSize = chunkSize;
        session.uploadedChunks = new HashSet<>();
        sessions.put(uploadId, session);

        List<Integer> allMissing = new ArrayList<>();
        for (int i = 0; i < totalChunks; i++) allMissing.add(i);
        return new UploadInitResponse(uploadId, chunkSize, totalChunks, allMissing);
    }

    public void uploadChunk(String uploadId, int index, InputStream inputStream) throws IOException {
        UploadSession session = sessions.get(uploadId);
        if (session == null) throw new RuntimeException("上传会话不存在或已过期");
        fileUtil.writeChunk(uploadId, index, inputStream);
        session.uploadedChunks.add(index);
    }

    public UploadInitResponse getStatus(String uploadId) {
        UploadSession session = sessions.get(uploadId);
        if (session == null) throw new RuntimeException("上传会话不存在");
        List<Integer> missing = new ArrayList<>();
        for (int i = 0; i < session.totalChunks; i++) {
            if (!session.uploadedChunks.contains(i)) missing.add(i);
        }
        return new UploadInitResponse(uploadId, session.chunkSize, session.totalChunks, missing);
    }

    public PathData completeUpload(String uploadId) throws IOException {
        UploadSession session = sessions.get(uploadId);
        if (session == null) throw new RuntimeException("上传会话不存在");
        if (session.uploadedChunks.size() != session.totalChunks) {
            throw new RuntimeException("尚有 " + (session.totalChunks - session.uploadedChunks.size()) + " 个分片未上传");
        }
        Path finalPath = fileUtil.mergeChunks(uploadId, session.originalName, session.totalChunks);
        sessions.remove(uploadId);
        return new PathData(finalPath, session.originalName, session.fileSize);
    }

    public record PathData(Path path, String originalName, long fileSize) {}

    private static class UploadSession {
        String originalName;
        long fileSize;
        int totalChunks;
        int chunkSize;
        Set<Integer> uploadedChunks;
    }
}
