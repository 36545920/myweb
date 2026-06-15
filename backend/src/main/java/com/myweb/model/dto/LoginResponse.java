package com.myweb.model.dto;

import com.myweb.model.vo.UserVO;
import lombok.Data;

@Data
public class LoginResponse {
    private String accessToken;
    private String refreshToken;
    private UserVO user;
    private String message;
}
