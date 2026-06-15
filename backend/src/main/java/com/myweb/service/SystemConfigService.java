package com.myweb.service;

import com.myweb.model.entity.SystemConfig;
import com.myweb.repository.SystemConfigMapper;
import com.myweb.repository.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SystemConfigService {

    private final SystemConfigMapper configMapper;
    private final UserMapper userMapper;

    public List<SystemConfig> getAllConfigs() {
        return configMapper.selectList(null);
    }

    @Transactional
    public void updateConfig(Map<String, String> configs) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        for (var entry : configs.entrySet()) {
            SystemConfig config = configMapper.selectById(entry.getKey());
            if (config != null) {
                // 校验：total_user_quota 不能小于已分配总量
                if ("total_user_quota".equals(entry.getKey())) {
                    long newValue = Long.parseLong(entry.getValue());
                    long allocated = userMapper.selectList(null).stream()
                        .mapToLong(u -> u.getStorageQuota()).sum();
                    if (newValue < allocated) {
                        throw new RuntimeException("用户空间总量不能小于已分配总量（" +
                            (allocated / 1073741824.0) + " GB）");
                    }
                }
                config.setConfigValue(entry.getValue());
                config.setUpdatedBy(email);
                configMapper.updateById(config);
            }
        }
    }
}
