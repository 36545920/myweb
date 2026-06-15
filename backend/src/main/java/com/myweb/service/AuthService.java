package com.myweb.service;

import com.myweb.model.dto.LoginRequest;
import com.myweb.model.dto.LoginResponse;
import com.myweb.model.dto.RegisterRequest;
import com.myweb.model.entity.SystemConfig;
import com.myweb.model.entity.User;
import com.myweb.model.vo.UserVO;
import com.myweb.repository.SystemConfigMapper;
import com.myweb.repository.UserMapper;
import com.myweb.util.EmailUtil;
import com.myweb.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserMapper userMapper;
    private final SystemConfigMapper configMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final EmailUtil emailUtil;

    public void sendVerificationCode(String email) {
        emailUtil.sendVerificationCode(email);
    }

    @Transactional
    public LoginResponse register(RegisterRequest req, String verificationCode) {
        if (!emailUtil.verifyCode(req.getEmail(), verificationCode)) {
            throw new RuntimeException("验证码错误或已过期");
        }
        if (userMapper.selectById(req.getEmail()) != null) {
            throw new RuntimeException("该邮箱已注册");
        }

        SystemConfig todayCountConfig = configMapper.selectById("today_register_count");
        SystemConfig dailyLimitConfig = configMapper.selectById("daily_register_limit");
        SystemConfig registerDateConfig = configMapper.selectById("register_date");
        int todayCount = Integer.parseInt(todayCountConfig.getConfigValue());
        int dailyLimit = Integer.parseInt(dailyLimitConfig.getConfigValue());

        if (!LocalDate.now().toString().equals(registerDateConfig.getConfigValue())) {
            todayCount = 0;
            registerDateConfig.setConfigValue(LocalDate.now().toString());
            configMapper.updateById(registerDateConfig);
        }

        if (todayCount >= dailyLimit) {
            throw new RuntimeException("今日注册已达上限，请明天再试");
        }

        SystemConfig totalQuotaConfig = configMapper.selectById("total_user_quota");
        SystemConfig defaultQuotaConfig = configMapper.selectById("default_user_quota");
        long totalQuota = Long.parseLong(totalQuotaConfig.getConfigValue());
        long defaultQuota = Long.parseLong(defaultQuotaConfig.getConfigValue());

        Long allocatedTotal = userMapper.selectList(null).stream()
                .mapToLong(User::getStorageQuota).sum();
        long remaining = totalQuota - allocatedTotal;
        long assignedQuota = Math.min(defaultQuota, Math.max(0, remaining));

        User user = new User();
        user.setEmail(req.getEmail());
        user.setPasswordHash(passwordEncoder.encode(req.getPassword()));
        user.setNickname(req.getNickname());
        user.setRole("USER");
        user.setStorageQuota(assignedQuota);
        user.setStorageUsed(0L);
        user.setStatus("ACTIVE");
        user.setEmailVerified(true);
        userMapper.insert(user);

        todayCountConfig.setConfigValue(String.valueOf(todayCount + 1));
        configMapper.updateById(todayCountConfig);

        String accessToken = jwtUtil.generateAccessToken(user.getEmail(), user.getRole());
        String refreshToken = jwtUtil.generateRefreshToken(user.getEmail());
        return buildLoginResponse(user, accessToken, refreshToken,
                assignedQuota != defaultQuota ? assignedQuota : null);
    }

    public LoginResponse login(LoginRequest req) {
        User user = userMapper.selectById(req.getEmail());
        if (user == null || !passwordEncoder.matches(req.getPassword(), user.getPasswordHash())) {
            throw new RuntimeException("邮箱或密码错误");
        }
        if ("DISABLED".equals(user.getStatus())) {
            throw new RuntimeException("账号已被禁用");
        }
        String accessToken = jwtUtil.generateAccessToken(user.getEmail(), user.getRole());
        String refreshToken = jwtUtil.generateRefreshToken(user.getEmail());
        return buildLoginResponse(user, accessToken, refreshToken, null);
    }

    public LoginResponse refresh(String refreshToken) {
        if (!jwtUtil.validateToken(refreshToken)) {
            throw new RuntimeException("Token 已过期，请重新登录");
        }
        String email = jwtUtil.getEmailFromToken(refreshToken);
        User user = userMapper.selectById(email);
        if (user == null) throw new RuntimeException("用户不存在");
        String newAccessToken = jwtUtil.generateAccessToken(email, user.getRole());
        return buildLoginResponse(user, newAccessToken, refreshToken, null);
    }

    private LoginResponse buildLoginResponse(User user, String accessToken,
                                              String refreshToken, Long quotaWarning) {
        LoginResponse resp = new LoginResponse();
        resp.setAccessToken(accessToken);
        resp.setRefreshToken(refreshToken);
        resp.setUser(UserVO.builder()
                .email(user.getEmail())
                .nickname(user.getNickname())
                .avatar(user.getAvatar())
                .role(user.getRole())
                .storageQuota(user.getStorageQuota())
                .storageUsed(user.getStorageUsed())
                .createdAt(user.getCreatedAt())
                .build());
        if (quotaWarning != null) {
            resp.setMessage("由于用户空间有限，您的个人配额为 " +
                    (quotaWarning / 1073741824.0) + " GB，低于默认值");
        }
        return resp;
    }
}
