package com.myweb.model.vo;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class UserVO {
    private String email;
    private String nickname;
    private String avatar;
    private String role;
    private Long storageQuota;
    private Long storageUsed;
    private LocalDateTime createdAt;
}
