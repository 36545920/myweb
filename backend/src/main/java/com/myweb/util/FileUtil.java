package com.myweb.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.io.*;
import java.nio.file.*;
import java.util.*;

@Component
public class FileUtil {

    private final String fileServerUrl;
    private final String authToken;
    private final Path tmpPath;
    private final RestTemplate restTemplate;

    public FileUtil(
            @Value("${app.file-server.url}") String fileServerUrl,
            @Value("${app.file-server.auth-token}") String authToken,
            @Value("${app.upload-tmp-path}") String tmpPath) {
        this.fileServerUrl = fileServerUrl;
        this.authToken = authToken;
        this.tmpPath = Path.of(tmpPath);
        this.restTemplate = new RestTemplate();
        ensureTmpDir();
    }

    private void ensureTmpDir() {
        try { Files.createDirectories(tmpPath); } catch (IOException e) { throw new RuntimeException(e); }
    }

    private HttpHeaders authHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Auth-Token", authToken);
        return headers;
    }

    // === 分片上传（存本地临时目录，最终合并后发到文件服务器） ===

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
        // 本机合并临时分片，然后上传到文件服务器
        String ext = "";
        int dotIndex = originalName.lastIndexOf('.');
        if (dotIndex > 0) ext = originalName.substring(dotIndex);
        String storedName = UUID.randomUUID() + ext;
        Path mergedTmp = tmpPath.resolve(storedName);

        try (OutputStream out = Files.newOutputStream(mergedTmp)) {
            for (int i = 0; i < totalChunks; i++) {
                Path chunkFile = getChunkDir(uploadId).resolve(i + ".chunk");
                if (!Files.exists(chunkFile)) throw new IOException("分片 " + i + " 缺失");
                Files.copy(chunkFile, out);
            }
        }

        // 上传完整文件到文件服务器
        try (InputStream is = Files.newInputStream(mergedTmp)) {
            HttpEntity<byte[]> entity = new HttpEntity<>(is.readAllBytes(), authHeaders());
            String url = fileServerUrl + "/file/upload?path=" + storedName;
            ResponseEntity<Map> resp = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);
            if (!resp.getStatusCode().is2xxSuccessful()) {
                throw new IOException("文件服务器上传失败");
            }
        }

        // 清理本地临时文件
        deleteDir(getChunkDir(uploadId));
        Files.deleteIfExists(mergedTmp);

        return Path.of(storedName); // 返回文件服务器上的路径
    }

    // === 下载（从文件服务器） ===

    public InputStream downloadFromServer(String path) throws IOException {
        HttpEntity<Void> entity = new HttpEntity<>(authHeaders());
        String url = fileServerUrl + "/file/download?path=" + path;
        ResponseEntity<byte[]> resp = restTemplate.exchange(url, HttpMethod.GET, entity, byte[].class);
        if (!resp.getStatusCode().is2xxSuccessful() || resp.getBody() == null) {
            throw new IOException("文件下载失败: " + resp.getStatusCode());
        }
        return new ByteArrayInputStream(resp.getBody());
    }

    // === 删除（从文件服务器） ===

    public void deleteFile(Path path) throws IOException {
        HttpEntity<Void> entity = new HttpEntity<>(authHeaders());
        String url = fileServerUrl + "/file/delete?path=" + path.toString();
        restTemplate.exchange(url, HttpMethod.DELETE, entity, Void.class);
    }

    // === 健康检查 ===

    public boolean isFileServerOnline() {
        try {
            ResponseEntity<Map> resp = restTemplate.getForEntity(fileServerUrl + "/file/health", Map.class);
            return resp.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            return false;
        }
    }

    // === 本地辅助方法 ===

    public String md5(Path file) throws Exception {
        java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD5");
        try (InputStream is = Files.newInputStream(file)) {
            byte[] buf = new byte[8192];
            int n;
            while ((n = is.read(buf)) != -1) md.update(buf, 0, n);
        }
        StringBuilder sb = new StringBuilder();
        for (byte b : md.digest()) sb.append(String.format("%02x", b));
        return sb.toString();
    }

    public void deleteDir(Path dir) throws IOException {
        if (Files.exists(dir)) {
            try (var stream = Files.walk(dir)) {
                stream.sorted(Comparator.reverseOrder())
                      .forEach(p -> { try { Files.delete(p); } catch (IOException ignored) {} });
            }
        }
    }

    public Path ensureStorageDir() throws IOException {
        return tmpPath;
    }
}
