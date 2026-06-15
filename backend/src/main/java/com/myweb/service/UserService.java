package com.myweb.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.myweb.model.entity.User;
import com.myweb.model.vo.UserVO;
import com.myweb.repository.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserMapper userMapper;
    private final FriendService friendService;
    private final PasswordEncoder passwordEncoder;

    private String currentUserEmail() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    public UserVO getProfile() {
        User user = userMapper.selectById(currentUserEmail());
        return toVO(user, user.getEmail());
    }

    @Transactional
    public UserVO updateProfile(String nickname, String avatar, String oldPassword, String newPassword) {
        User user = userMapper.selectById(currentUserEmail());
        if (nickname != null && !nickname.isBlank()) user.setNickname(nickname);
        if (avatar != null) user.setAvatar(avatar);
        if (newPassword != null && !newPassword.isBlank()) {
            if (oldPassword == null || !passwordEncoder.matches(oldPassword, user.getPasswordHash()))
                throw new RuntimeException("原密码错误");
            user.setPasswordHash(passwordEncoder.encode(newPassword));
        }
        userMapper.updateById(user);
        return toVO(user, user.getEmail());
    }

    public List<UserVO> searchUsers(String emailPrefix) {
        String me = currentUserEmail();
        List<User> users = userMapper.selectList(new LambdaQueryWrapper<User>()
            .likeRight(User::getEmail, emailPrefix)
            .ne(User::getEmail, me)
            .eq(User::getStatus, "ACTIVE")
            .last("LIMIT 20"));

        List<String> friendEmails = friendService.getFriendEmails(me);

        return users.stream().map(u -> {
            UserVO vo = toVO(u, u.getEmail());
            if (!friendEmails.contains(u.getEmail())) {
                String[] parts = u.getEmail().split("@");
                vo.setEmail(parts[0].substring(0, Math.min(3, parts[0].length())) + "***@" + parts[1]);
            }
            return vo;
        }).toList();
    }

    public Page<User> listUsers(int page, int size) {
        return userMapper.selectPage(new Page<>(page, size),
            new LambdaQueryWrapper<User>().orderByDesc(User::getCreatedAt));
    }

    @Transactional
    public void updateQuota(String email, long quota) {
        User user = userMapper.selectById(email);
        if (user == null) throw new RuntimeException("用户不存在");
        user.setStorageQuota(quota);
        userMapper.updateById(user);
    }

    @Transactional
    public void updateStatus(String email, String status) {
        User user = userMapper.selectById(email);
        if (user == null) throw new RuntimeException("用户不存在");
        if ("SUPER_ADMIN".equals(user.getRole())) throw new RuntimeException("不能禁用超级管理员");
        user.setStatus(status);
        userMapper.updateById(user);
    }

    @Transactional
    public void updateRole(String email, String role) {
        User user = userMapper.selectById(email);
        if (user == null) throw new RuntimeException("用户不存在");
        user.setRole(role);
        userMapper.updateById(user);
    }

    private UserVO toVO(User u, String rawEmail) {
        return UserVO.builder()
            .email(rawEmail).nickname(u.getNickname()).avatar(u.getAvatar())
            .role(u.getRole()).storageQuota(u.getStorageQuota()).storageUsed(u.getStorageUsed())
            .createdAt(u.getCreatedAt()).build();
    }
}
