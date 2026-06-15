package com.myweb.model.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("users")
public class User {
    @TableId
    private String email;
    private String passwordHash;
    private String nickname;
    private String avatar;
    private String role;
    private Long storageQuota;
    private Long storageUsed;
    private String status;
    private Boolean emailVerified;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
