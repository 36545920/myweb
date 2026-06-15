package com.myweb.model.vo;

import com.myweb.model.entity.FileEntity;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class FileVO {
    private Long id;
    private String ownerEmail;
    private String title;
    private String description;
    private String originalName;
    private Boolean isSharedPool;
    private String reviewStatus;
    private Long fileSize;
    private String mimeType;
    private LocalDateTime expireAt;
    private LocalDateTime createdAt;

    public static FileVO from(FileEntity f) {
        return FileVO.builder()
            .id(f.getId()).ownerEmail(f.getOwnerEmail()).title(f.getTitle())
            .description(f.getDescription()).originalName(f.getOriginalName())
            .isSharedPool(f.getIsSharedPool()).reviewStatus(f.getReviewStatus())
            .fileSize(f.getFileSize()).mimeType(f.getMimeType())
            .expireAt(f.getExpireAt()).createdAt(f.getCreatedAt()).build();
    }
}
