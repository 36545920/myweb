package com.myweb.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.myweb.model.entity.FileEntity;
import com.myweb.model.entity.User;
import com.myweb.model.entity.SystemConfig;
import com.myweb.model.entity.FileTransfer;
import com.myweb.repository.FileMapper;
import com.myweb.repository.FileTransferMapper;
import com.myweb.repository.SystemConfigMapper;
import com.myweb.repository.UserMapper;
import com.myweb.util.FileUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FileService {

    private final FileMapper fileMapper;
    private final FileTransferMapper transferMapper;
    private final UserMapper userMapper;
    private final SystemConfigMapper configMapper;
    private final FileUtil fileUtil;

    private String currentUserEmail() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    @Transactional
    public FileEntity createFile(String title, String description, boolean isSharedPool,
                                  LocalDateTime expireAt, Path storagePath,
                                  String originalName, long fileSize, String mimeType) {
        String email = currentUserEmail();
        User user = userMapper.selectById(email);

        long newUsed = user.getStorageUsed() + fileSize;
        if (!isSharedPool && newUsed > user.getStorageQuota()) {
            throw new RuntimeException("个人空间不足（已用 " +
                (user.getStorageUsed() / 1073741824.0) + " GB / 配额 " +
                (user.getStorageQuota() / 1073741824.0) + " GB）");
        }

        if (isSharedPool) {
            SystemConfig poolConfig = configMapper.selectById("total_pool_quota");
            long poolQuota = Long.parseLong(poolConfig.getConfigValue());
            Long poolUsed = fileMapper.selectList(
                new LambdaQueryWrapper<FileEntity>()
                    .eq(FileEntity::getIsSharedPool, true)
                    .eq(FileEntity::getReviewStatus, "APPROVED")
            ).stream().mapToLong(FileEntity::getFileSize).sum();
            if (poolUsed + fileSize > poolQuota) {
                throw new RuntimeException("文件共享池已满，请联系管理员清理");
            }
        }

        FileEntity file = new FileEntity();
        file.setOwnerEmail(email);
        file.setTitle(title);
        file.setDescription(description);
        file.setOriginalName(originalName);
        file.setIsSharedPool(isSharedPool);
        file.setReviewStatus(isSharedPool ? "PENDING" : "APPROVED");
        file.setFileSize(fileSize);
        file.setMimeType(mimeType);
        file.setStoragePath(storagePath.toString());
        file.setExpireAt(expireAt);
        file.setFileType(detectFileType(mimeType));
        fileMapper.insert(file);

        user.setStorageUsed(newUsed);
        userMapper.updateById(user);

        return file;
    }

    public Page<FileEntity> listMyFiles(int page, int size) {
        String email = currentUserEmail();
        return fileMapper.selectPage(
            new Page<>(page, size),
            new LambdaQueryWrapper<FileEntity>()
                .eq(FileEntity::getOwnerEmail, email)
                .orderByDesc(FileEntity::getCreatedAt)
        );
    }

    public Page<FileEntity> listMyFiles(int page, int size, String type, String sort, String order, String keyword) {
        String email = currentUserEmail();
        LambdaQueryWrapper<FileEntity> wrapper = new LambdaQueryWrapper<FileEntity>()
            .eq(FileEntity::getOwnerEmail, email);

        if (type != null && !type.isEmpty()) {
            wrapper.eq(FileEntity::getFileType, type);
        }
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.and(w -> w
                .like(FileEntity::getTitle, keyword)
                .or()
                .like(FileEntity::getOriginalName, keyword)
                .or()
                .like(FileEntity::getDescription, keyword));
        }

        boolean asc = "asc".equalsIgnoreCase(order);
        if (sort != null) {
            switch (sort) {
                case "size":
                    wrapper.orderBy(true, asc, FileEntity::getFileSize);
                    break;
                case "name":
                    wrapper.orderBy(true, asc, FileEntity::getTitle);
                    break;
                case "type":
                    wrapper.orderBy(true, asc, FileEntity::getFileType);
                    break;
                default:
                    wrapper.orderByDesc(FileEntity::getCreatedAt);
                    break;
            }
        } else {
            wrapper.orderByDesc(FileEntity::getCreatedAt);
        }

        return fileMapper.selectPage(new Page<>(page, size), wrapper);
    }

    public Page<FileEntity> listPoolFiles(int page, int size) {
        return fileMapper.selectPage(
            new Page<>(page, size),
            new LambdaQueryWrapper<FileEntity>()
                .eq(FileEntity::getIsSharedPool, true)
                .eq(FileEntity::getReviewStatus, "APPROVED")
                .orderByDesc(FileEntity::getCreatedAt)
        );
    }

    public Page<FileEntity> listPendingReviews(int page, int size) {
        return fileMapper.selectPage(
            new Page<>(page, size),
            new LambdaQueryWrapper<FileEntity>()
                .eq(FileEntity::getIsSharedPool, true)
                .eq(FileEntity::getReviewStatus, "PENDING")
                .orderByAsc(FileEntity::getCreatedAt)
        );
    }

    public Page<FileEntity> listRecent(int size) {
        String email = currentUserEmail();
        return fileMapper.selectPage(new Page<>(1, size),
            new LambdaQueryWrapper<FileEntity>()
                .eq(FileEntity::getLastAccessBy, email)
                .orderByDesc(FileEntity::getLastAccessAt));
    }

    public FileEntity getFile(Long id) {
        return fileMapper.selectById(id);
    }

    @Transactional
    public void rename(Long id, String title) {
        FileEntity file = fileMapper.selectById(id);
        if (file == null) throw new RuntimeException("文件不存在");
        if (!file.getOwnerEmail().equals(currentUserEmail())) throw new RuntimeException("无权操作");
        file.setTitle(title);
        fileMapper.updateById(file);
    }

    public void recordAccess(Long id) {
        FileEntity file = fileMapper.selectById(id);
        if (file != null) {
            file.setLastAccessAt(LocalDateTime.now());
            file.setLastAccessBy(currentUserEmail());
            fileMapper.updateById(file);
        }
    }

    @Transactional
    public void deleteFile(Long id) {
        String email = currentUserEmail();
        FileEntity file = fileMapper.selectById(id);
        if (file == null) throw new RuntimeException("文件不存在");

        User currentUser = userMapper.selectById(email);
        if (!file.getOwnerEmail().equals(email) &&
            !"ADMIN".equals(currentUser.getRole()) &&
            !"SUPER_ADMIN".equals(currentUser.getRole())) {
            throw new RuntimeException("无权删除他人文件");
        }

        try { fileUtil.deleteFile(Path.of(file.getStoragePath())); } catch (IOException ignored) {}
        User owner = userMapper.selectById(file.getOwnerEmail());
        if (owner != null) {
            owner.setStorageUsed(Math.max(0, owner.getStorageUsed() - file.getFileSize()));
            userMapper.updateById(owner);
        }
        transferMapper.delete(new LambdaQueryWrapper<FileTransfer>().eq(FileTransfer::getFileId, id));
        fileMapper.deleteById(id);
    }

    @Transactional
    public void batchDelete(List<Long> ids) {
        String email = currentUserEmail();
        for (Long id : ids) {
            FileEntity file = fileMapper.selectById(id);
            if (file == null || !file.getOwnerEmail().equals(email)) continue;
            try { fileUtil.deleteFile(Path.of(file.getStoragePath())); } catch (IOException ignored) {}
            User owner = userMapper.selectById(file.getOwnerEmail());
            if (owner != null) {
                owner.setStorageUsed(Math.max(0, owner.getStorageUsed() - file.getFileSize()));
                userMapper.updateById(owner);
            }
            transferMapper.delete(new LambdaQueryWrapper<FileTransfer>().eq(FileTransfer::getFileId, id));
            fileMapper.deleteById(id);
        }
    }

    @Transactional
    public void reviewFile(Long id, boolean approved, String comment) {
        String email = currentUserEmail();
        FileEntity file = fileMapper.selectById(id);
        if (file == null) throw new RuntimeException("文件不存在");
        file.setReviewStatus(approved ? "APPROVED" : "REJECTED");
        file.setReviewComment(comment);
        file.setReviewedBy(email);
        fileMapper.updateById(file);
    }

    public InputStream downloadFile(Long id) throws IOException {
        String email = currentUserEmail();
        FileEntity file = fileMapper.selectById(id);
        if (file == null) throw new RuntimeException("文件不存在");

        if (file.getOwnerEmail().equals(email)) {
            return Files.newInputStream(Path.of(file.getStoragePath()));
        }

        Long transferCount = transferMapper.selectCount(
            new LambdaQueryWrapper<FileTransfer>()
                .eq(FileTransfer::getFileId, id)
                .eq(FileTransfer::getToEmail, email));
        if (transferCount > 0) {
            return Files.newInputStream(Path.of(file.getStoragePath()));
        }

        if (file.getIsSharedPool() && "APPROVED".equals(file.getReviewStatus())) {
            return Files.newInputStream(Path.of(file.getStoragePath()));
        }
        throw new RuntimeException("无权下载此文件");
    }

    private String detectFileType(String mimeType) {
        if (mimeType == null) return "OTHER";
        if (mimeType.startsWith("image/")) return "IMAGE";
        if (mimeType.startsWith("video/")) return "VIDEO";
        if (mimeType.contains("pdf") || mimeType.contains("word")
            || mimeType.contains("document") || mimeType.contains("text")
            || mimeType.contains("excel") || mimeType.contains("spreadsheet")
            || mimeType.contains("presentation")) return "DOCUMENT";
        if (mimeType.contains("zip") || mimeType.contains("rar")
            || mimeType.contains("tar") || mimeType.contains("gzip")
            || mimeType.contains("7z")) return "ARCHIVE";
        return "OTHER";
    }
}
