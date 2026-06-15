CREATE DATABASE IF NOT EXISTS myweb DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE myweb;

CREATE TABLE IF NOT EXISTS users (
    email VARCHAR(255) PRIMARY KEY,
    password_hash VARCHAR(255) NOT NULL,
    nickname VARCHAR(100) NOT NULL,
    avatar VARCHAR(500),
    role VARCHAR(20) NOT NULL DEFAULT 'USER',
    storage_quota BIGINT NOT NULL DEFAULT 10737418240,
    storage_used BIGINT NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    email_verified TINYINT(1) NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS files (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    owner_email VARCHAR(255) NOT NULL,
    title VARCHAR(200) NOT NULL,
    description VARCHAR(2000),
    original_name VARCHAR(500) NOT NULL,
    is_shared_pool TINYINT(1) NOT NULL DEFAULT 0,
    review_status VARCHAR(20) NOT NULL DEFAULT 'APPROVED',
    review_comment VARCHAR(500),
    reviewed_by VARCHAR(255),
    file_size BIGINT NOT NULL,
    mime_type VARCHAR(100),
    storage_path VARCHAR(500) NOT NULL,
    expire_at DATETIME,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_owner (owner_email),
    INDEX idx_pool_status (is_shared_pool, review_status),
    INDEX idx_expire (expire_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS file_transfers (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    file_id BIGINT NOT NULL,
    from_email VARCHAR(255) NOT NULL,
    to_email VARCHAR(255) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'SENT',
    message VARCHAR(500),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    downloaded_at DATETIME,
    INDEX idx_file (file_id),
    INDEX idx_from (from_email),
    INDEX idx_to (to_email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS friends (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_email VARCHAR(255) NOT NULL,
    friend_email VARCHAR(255) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    accepted_at DATETIME,
    UNIQUE INDEX idx_pair (user_email, friend_email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS system_config (
    config_key VARCHAR(50) PRIMARY KEY,
    config_value VARCHAR(500) NOT NULL,
    updated_by VARCHAR(255),
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 默认配置（使用 INSERT IGNORE 避免重复插入）
INSERT IGNORE INTO system_config (config_key, config_value) VALUES
('total_user_quota', '53687091200'),
('total_pool_quota', '32212254720'),
('default_user_quota', '10737418240'),
('daily_register_limit', '50'),
('today_register_count', '0'),
('register_date', CURDATE());

-- 初始超级管理员（密码: admin123，首次登录务必修改）
INSERT IGNORE INTO users (email, password_hash, nickname, role, storage_quota, email_verified, status)
VALUES ('admin@myweb.local', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EHs', '超级管理员', 'SUPER_ADMIN', 10737418240, 1, 'ACTIVE');
