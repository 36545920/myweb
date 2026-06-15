package com.myweb.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("friends")
public class Friend {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String userEmail;
    private String friendEmail;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime acceptedAt;
}
