package com.myweb.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.Comparator;
import java.util.UUID;

@Component
public class FileUtil {

    private final Path storagePath;
    private final Path tmpPath;

    public FileUtil(
            @Value("${app.storage-path}") String storagePath,
            @Value("${app.upload-tmp-path}") String tmpPath) {
        this.storagePath = Path.of(storagePath);
        this.tmpPath = Path.of(tmpPath);
        ensureDir(this.storagePath);
        ensureDir(this.tmpPath);
    }

    private void ensureDir(Path path) {
        try { Files.createDirectories(path); } catch (IOException e) { throw new RuntimeException(e); }
    }

    public Path getChunkDir(String uploadId) {
        Path dir = tmpPath.resolve(uploadId);
        try { Files.createDirectories(dir); } catch (IOException e) { throw new RuntimeException(e); }
        return dir;
    }

    public void writeChunk(String uploadId, int index, InputStream inputStream) throws IOException {
        Path chunkFile = getChunkDir(uploadId).resolve(index + ".chunk");
        Files.copy(inputStream, chunkFile, StandardCopyOption.REPLACE_EXISTING);
    }

    public Path mergeChunks(String uploadId, String originalName, int totalChunks) throws IOException {
        String ext = "";
        int dotIndex = originalName.lastIndexOf('.');
        if (dotIndex > 0) ext = originalName.substring(dotIndex);

        String storedName = UUID.randomUUID() + ext;
        Path finalPath = storagePath.resolve(storedName);

        try (OutputStream out = Files.newOutputStream(finalPath)) {
            for (int i = 0; i < totalChunks; i++) {
                Path chunkFile = getChunkDir(uploadId).resolve(i + ".chunk");
                if (!Files.exists(chunkFile)) {
                    throw new IOException("分片 " + i + " 缺失");
                }
                Files.copy(chunkFile, out);
            }
        }
        deleteDir(getChunkDir(uploadId));
        return finalPath;
    }

    public String md5(Path file) throws Exception {
        MessageDigest md = MessageDigest.getInstance("MD5");
        try (InputStream is = Files.newInputStream(file)) {
            byte[] buf = new byte[8192];
            int n;
            while ((n = is.read(buf)) != -1) md.update(buf, 0, n);
        }
        StringBuilder sb = new StringBuilder();
        for (byte b : md.digest()) sb.append(String.format("%02x", b));
        return sb.toString();
    }

    public void deleteFile(Path path) throws IOException {
        Files.deleteIfExists(path);
    }

    public void deleteDir(Path dir) throws IOException {
        if (Files.exists(dir)) {
            try (var stream = Files.walk(dir)) {
                stream.sorted(Comparator.reverseOrder())
                      .forEach(p -> { try { Files.delete(p); } catch (IOException ignored) {} });
            }
        }
    }

    /** 确保目录存在 */
    public Path ensureStorageDir() throws IOException {
        Files.createDirectories(storagePath);
        return storagePath;
    }
}
