package com.myweb.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("files")
public class FileEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String ownerEmail;
    private String title;
    private String description;
    private String originalName;
    private Boolean isSharedPool;
    private String reviewStatus;
    private String reviewComment;
    private String reviewedBy;
    private Long fileSize;
    private String mimeType;
    private String storagePath;
    private LocalDateTime expireAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
