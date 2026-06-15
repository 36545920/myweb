package com.myweb.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.myweb.model.entity.FileTransfer;
import com.myweb.model.entity.FileEntity;
import com.myweb.repository.FileTransferMapper;
import com.myweb.repository.FileMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FileTransferService {

    private final FileTransferMapper transferMapper;
    private final FileMapper fileMapper;

    private String currentUserEmail() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    @Transactional
    public FileTransfer send(Long fileId, String toEmail, String message) {
        String fromEmail = currentUserEmail();
        FileEntity file = fileMapper.selectById(fileId);
        if (file == null) throw new RuntimeException("文件不存在");
        if (!file.getOwnerEmail().equals(fromEmail)) throw new RuntimeException("只能发送自己的文件");

        FileTransfer transfer = new FileTransfer();
        transfer.setFileId(fileId);
        transfer.setFromEmail(fromEmail);
        transfer.setToEmail(toEmail);
        transfer.setStatus("SENT");
        transfer.setMessage(message);
        transferMapper.insert(transfer);
        return transfer;
    }

    public Page<FileTransfer> listInbox(int page, int size) {
        String email = currentUserEmail();
        return transferMapper.selectPage(new Page<>(page, size),
            new LambdaQueryWrapper<FileTransfer>()
                .eq(FileTransfer::getToEmail, email)
                .orderByDesc(FileTransfer::getCreatedAt));
    }

    public Page<FileTransfer> listSent(int page, int size) {
        String email = currentUserEmail();
        return transferMapper.selectPage(new Page<>(page, size),
            new LambdaQueryWrapper<FileTransfer>()
                .eq(FileTransfer::getFromEmail, email)
                .orderByDesc(FileTransfer::getCreatedAt));
    }

    @Transactional
    public void delete(Long id) {
        String email = currentUserEmail();
        FileTransfer transfer = transferMapper.selectById(id);
        if (transfer == null) throw new RuntimeException("记录不存在");
        if (!transfer.getFromEmail().equals(email) && !transfer.getToEmail().equals(email))
            throw new RuntimeException("无权删除");
        transferMapper.deleteById(id);
    }
}
