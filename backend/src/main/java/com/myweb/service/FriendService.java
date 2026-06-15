package com.myweb.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.myweb.model.entity.Friend;
import com.myweb.repository.FriendMapper;
import com.myweb.repository.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FriendService {

    private final FriendMapper friendMapper;
    private final UserMapper userMapper;

    private String currentUserEmail() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    @Transactional
    public Friend sendRequest(String friendEmail) {
        String me = currentUserEmail();
        if (me.equals(friendEmail)) throw new RuntimeException("不能添加自己为好友");
        if (userMapper.selectById(friendEmail) == null) throw new RuntimeException("用户不存在");

        // 检查是否已有好友关系
        Long count = friendMapper.selectCount(new LambdaQueryWrapper<Friend>()
            .eq(Friend::getUserEmail, me).eq(Friend::getFriendEmail, friendEmail));
        if (count > 0) throw new RuntimeException("已发送过好友请求或已为好友");

        Friend friend = new Friend();
        friend.setUserEmail(me);
        friend.setFriendEmail(friendEmail);
        friend.setStatus("PENDING");
        friendMapper.insert(friend);
        return friend;
    }

    @Transactional
    public void handleRequest(Long id, boolean accept) {
        Friend friend = friendMapper.selectById(id);
        if (friend == null) throw new RuntimeException("请求不存在");
        if (!friend.getFriendEmail().equals(currentUserEmail()))
            throw new RuntimeException("无权处理此请求");

        if (accept) {
            friend.setStatus("ACCEPTED");
            friend.setAcceptedAt(LocalDateTime.now());
            friendMapper.updateById(friend);
            // 双向添加
            Friend reverse = new Friend();
            reverse.setUserEmail(friend.getFriendEmail());
            reverse.setFriendEmail(friend.getUserEmail());
            reverse.setStatus("ACCEPTED");
            reverse.setAcceptedAt(LocalDateTime.now());
            friendMapper.insert(reverse);
        } else {
            friendMapper.deleteById(id);
        }
    }

    public List<Friend> listFriends() {
        String me = currentUserEmail();
        return friendMapper.selectList(new LambdaQueryWrapper<Friend>()
            .eq(Friend::getUserEmail, me)
            .eq(Friend::getStatus, "ACCEPTED"));
    }

    @Transactional
    public void deleteFriend(String friendEmail) {
        String me = currentUserEmail();
        friendMapper.delete(new LambdaQueryWrapper<Friend>()
            .eq(Friend::getUserEmail, me).eq(Friend::getFriendEmail, friendEmail));
        friendMapper.delete(new LambdaQueryWrapper<Friend>()
            .eq(Friend::getUserEmail, friendEmail).eq(Friend::getFriendEmail, me));
    }

    public List<String> getFriendEmails(String email) {
        return friendMapper.selectList(new LambdaQueryWrapper<Friend>()
            .eq(Friend::getUserEmail, email)
            .eq(Friend::getStatus, "ACCEPTED"))
            .stream().map(Friend::getFriendEmail).toList();
    }
}
