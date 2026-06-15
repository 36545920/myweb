package com.myweb.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("file_transfers")
public class FileTransfer {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long fileId;
    private String fromEmail;
    private String toEmail;
    private String status;
    private String message;
    private LocalDateTime createdAt;
    private LocalDateTime downloadedAt;
}
