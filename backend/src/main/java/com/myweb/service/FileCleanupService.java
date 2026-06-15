package com.myweb.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.myweb.model.entity.FileEntity;
import com.myweb.model.entity.FileTransfer;
import com.myweb.repository.FileMapper;
import com.myweb.repository.FileTransferMapper;
import com.myweb.repository.UserMapper;
import com.myweb.util.FileUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileCleanupService {

    private final FileMapper fileMapper;
    private final FileTransferMapper transferMapper;
    private final UserMapper userMapper;
    private final FileUtil fileUtil;

    @Scheduled(cron = "0 0 3 * * ?")
    public void cleanExpiredFiles() {
        log.info("开始清理过期文件...");
        List<FileEntity> expired = fileMapper.selectList(
            new LambdaQueryWrapper<FileEntity>()
                .isNotNull(FileEntity::getExpireAt)
                .lt(FileEntity::getExpireAt, LocalDateTime.now())
        );

        int count = 0;
        for (FileEntity file : expired) {
            try {
                fileUtil.deleteFile(Path.of(file.getStoragePath()));
                transferMapper.delete(new LambdaQueryWrapper<FileTransfer>()
                    .eq(FileTransfer::getFileId, file.getId()));
                var user = userMapper.selectById(file.getOwnerEmail());
                if (user != null) {
                    user.setStorageUsed(Math.max(0, user.getStorageUsed() - file.getFileSize()));
                    userMapper.updateById(user);
                }
                fileMapper.deleteById(file.getId());
                count++;
            } catch (Exception e) {
                log.error("清理文件失败: {} (ID: {})", file.getOriginalName(), file.getId(), e);
            }
        }
        log.info("过期文件清理完成，共清理 {} 个文件", count);
    }
}
