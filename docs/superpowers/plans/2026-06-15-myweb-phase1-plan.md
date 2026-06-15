# MyWeb Phase 1 实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 构建 MyWeb Phase 1：用户认证 + 文件互传 + 个人空间 + 共享池 + 好友系统

**Architecture:** Vue3 前端通过 Nginx 代理调用 Spring Boot REST API，JWT 无状态认证，MySQL 存储元数据（逻辑外键），NAS 挂载存储文件实体。单机部署 2核2G，Nginx → Spring Boot(:8080) → MySQL(:3306) + NAS。

**Tech Stack:** Vue3 + Vite + Pinia + Axios | Spring Boot 3 + MyBatis-Plus + MySQL | Nginx | QQ SMTP

---

## 文件结构规划

```
myweb/
├── frontend/                          # Vue3 前端
│   ├── package.json
│   ├── vite.config.ts
│   ├── index.html
│   ├── src/
│   │   ├── main.ts                   # 入口，注册 router + pinia
│   │   ├── App.vue                   # 根组件，仅 <router-view>
│   │   ├── router/index.ts           # 路由配置（含 beforeEach 鉴权守卫）
│   │   ├── stores/
│   │   │   ├── auth.ts               # 用户登录状态、token 管理
│   │   │   ├── files.ts              # 文件列表、上传状态
│   │   │   └── pool.ts               # 共享池列表
│   │   ├── api/
│   │   │   ├── client.ts             # Axios 实例，拦截器注入 token
│   │   │   ├── auth.ts               # 登录/注册/验证 API
│   │   │   ├── files.ts              # 文件 CRUD + 上传 API
│   │   │   ├── transfers.ts          # 发送/收件箱 API
│   │   │   ├── friends.ts            # 好友 API
│   │   │   ├── users.ts              # 用户搜索/个人信息 API
│   │   │   └── admin.ts              # 审核/管理/系统配置 API
│   │   ├── views/
│   │   │   ├── Login.vue             # 登录页
│   │   │   ├── Register.vue          # 注册页
│   │   │   ├── VerifyEmail.vue       # 邮箱验证页
│   │   │   ├── Dashboard.vue         # 首页仪表盘
│   │   │   ├── MyFiles.vue           # 我的文件列表
│   │   │   ├── Upload.vue            # 文件上传页（含表单）
│   │   │   ├── Inbox.vue             # 收件箱
│   │   │   ├── Pool.vue              # 共享池浏览
│   │   │   ├── Friends.vue           # 好友列表
│   │   │   ├── Profile.vue           # 个人设置
│   │   │   ├── admin/
│   │   │   │   ├── Review.vue        # 共享池审核
│   │   │   │   ├── Users.vue         # 用户管理
│   │   │   │   └── RegisterLimit.vue # 注册限制
│   │   │   └── super-admin/
│   │   │       ├── Admins.vue        # 管理员任命
│   │   │       └── System.vue        # 系统设置
│   │   ├── components/
│   │   │   ├── AppLayout.vue         # 侧边栏+顶栏+主内容区布局
│   │   │   ├── Sidebar.vue           # 侧边栏导航（按角色动态菜单）
│   │   │   ├── FileCard.vue          # 文件卡片组件
│   │   │   ├── FileUploader.vue      # 分片上传组件（含进度条）
│   │   │   ├── UserSearch.vue        # 邮箱搜索用户弹窗
│   │   │   └── ConfirmDialog.vue     # 确认弹窗
│   │   └── utils/
│   │       ├── format.ts             # 文件大小/日期格式化
│   │       └── validators.ts         # 邮箱/密码校验
│   └── public/
├── backend/                          # Spring Boot 后端
│   ├── pom.xml
│   ├── src/main/java/com/myweb/
│   │   ├── MyWebApplication.java
│   │   ├── config/
│   │   │   ├── SecurityConfig.java   # Spring Security + JWT 无状态配置
│   │   │   ├── CorsConfig.java       # CORS 跨域配置
│   │   │   └── MyBatisPlusConfig.java# 分页插件配置
│   │   ├── filter/
│   │   │   └── JwtAuthFilter.java    # OncePerRequestFilter，解析 token 注入 SecurityContext
│   │   ├── model/
│   │   │   ├── entity/
│   │   │   │   ├── User.java
│   │   │   │   ├── FileEntity.java
│   │   │   │   ├── FileTransfer.java
│   │   │   │   ├── Friend.java
│   │   │   │   └── SystemConfig.java
│   │   │   ├── dto/
│   │   │   │   ├── RegisterRequest.java
│   │   │   │   ├── LoginRequest.java
│   │   │   │   ├── LoginResponse.java
│   │   │   │   ├── UploadInitRequest.java
│   │   │   │   ├── UploadInitResponse.java
│   │   │   │   ├── TransferRequest.java
│   │   │   │   ├── FriendRequest.java
│   │   │   │   ├── UpdateQuotaRequest.java
│   │   │   │   ├── ReviewRequest.java
│   │   │   │   └── ConfigUpdateRequest.java
│   │   │   └── vo/
│   │   │       ├── UserVO.java
│   │   │       ├── FileVO.java
│   │   │       ├── TransferVO.java
│   │   │       └── FriendVO.java
│   │   ├── repository/
│   │   │   ├── UserMapper.java       # MyBatis-Plus BaseMapper
│   │   │   ├── FileMapper.java
│   │   │   ├── FileTransferMapper.java
│   │   │   ├── FriendMapper.java
│   │   │   └── SystemConfigMapper.java
│   │   ├── service/
│   │   │   ├── AuthService.java
│   │   │   ├── UserService.java
│   │   │   ├── FileService.java
│   │   │   ├── FileTransferService.java
│   │   │   ├── FriendService.java
│   │   │   ├── SystemConfigService.java
│   │   │   ├── UploadService.java    # 分片上传状态管理（ConcurrentHashMap）
│   │   │   └── FileCleanupService.java
│   │   ├── controller/
│   │   │   ├── AuthController.java
│   │   │   ├── FileController.java
│   │   │   ├── UploadController.java
│   │   │   ├── FileTransferController.java
│   │   │   ├── FriendController.java
│   │   │   ├── UserController.java
│   │   │   ├── AdminController.java
│   │   │   └── SystemConfigController.java
│   │   └── util/
│   │       ├── JwtUtil.java          # 生成/解析/校验 JWT
│   │       ├── FileUtil.java         # 文件路径计算/MD5校验/目录管理
│   │       └── EmailUtil.java        # QQ SMTP 验证码发送
│   └── src/main/resources/
│       ├── application.yml
│       └── db/schema.sql             # DDL 建表语句
└── nginx/
    └── myweb.conf                    # Nginx 配置
```

---

### Task 1: 后端项目初始化

**Files:** Create: `backend/pom.xml`, `backend/src/main/java/com/myweb/MyWebApplication.java`, `backend/src/main/resources/application.yml`

- [ ] **Step 1: 创建 pom.xml**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.2.0</version>
    </parent>
    <groupId>com.myweb</groupId>
    <artifactId>myweb-backend</artifactId>
    <version>0.1.0</version>
    <name>MyWeb Backend</name>

    <properties>
        <java.version>17</java.version>
        <mybatis-plus.version>3.5.5</mybatis-plus.version>
    </properties>

    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-security</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-mail</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>
        <dependency>
            <groupId>com.baomidou</groupId>
            <artifactId>mybatis-plus-spring-boot3-starter</artifactId>
            <version>${mybatis-plus.version}</version>
        </dependency>
        <dependency>
            <groupId>com.mysql</groupId>
            <artifactId>mysql-connector-j</artifactId>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-api</artifactId>
            <version>0.12.3</version>
        </dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-impl</artifactId>
            <version>0.12.3</version>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-jackson</artifactId>
            <version>0.12.3</version>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
                <configuration>
                    <jvmArguments>-Xmx512m -Xms256m</jvmArguments>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

- [ ] **Step 2: 创建 application.yml**

```yaml
server:
  port: 8080
  servlet:
    context-path: /api/v1

spring:
  datasource:
    url: jdbc:mysql://127.0.0.1:3306/myweb?useUnicode=true&characterEncoding=utf-8&useSSL=false&serverTimezone=Asia/Shanghai
    username: root
    password: ${MYSQL_PASSWORD:root}
    driver-class-name: com.mysql.cj.jdbc.Driver
    hikari:
      maximum-pool-size: 10
      minimum-idle: 2
      connection-timeout: 30000
  mail:
    host: smtp.qq.com
    port: 587
    username: ${MAIL_USERNAME:}
    password: ${MAIL_PASSWORD:}
    properties:
      mail.smtp.auth: true
      mail.smtp.starttls.enable: true
  servlet:
    multipart:
      max-file-size: -1
      max-request-size: -1

mybatis-plus:
  global-config:
    db-config:
      id-type: auto
  configuration:
    map-underscore-to-camel-case: true
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl

jwt:
  secret: ${JWT_SECRET:myweb-jwt-secret-key-change-in-production}
  access-token-expiration: 86400000    # 24h
  refresh-token-expiration: 604800000  # 7d

app:
  storage-path: /mnt/nas/myweb-files/
  upload-tmp-path: /tmp/myweb-uploads/
  email-verification-code-expiration: 300  # 5 min
```

- [ ] **Step 3: 创建启动类**

```java
package com.myweb;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@MapperScan("com.myweb.repository")
@EnableScheduling
public class MyWebApplication {
    public static void main(String[] args) {
        SpringApplication.run(MyWebApplication.class, args);
    }
}
```

- [ ] **Step 4: 创建数据库 schema**

文件: `backend/src/main/resources/db/schema.sql`

```sql
CREATE DATABASE IF NOT EXISTS myweb DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE myweb;

CREATE TABLE users (
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
) ENGINE=InnoDB;

CREATE TABLE files (
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
) ENGINE=InnoDB;

CREATE TABLE file_transfers (
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
) ENGINE=InnoDB;

CREATE TABLE friends (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_email VARCHAR(255) NOT NULL,
    friend_email VARCHAR(255) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    accepted_at DATETIME,
    UNIQUE INDEX idx_pair (user_email, friend_email)
) ENGINE=InnoDB;

CREATE TABLE system_config (
    config_key VARCHAR(50) PRIMARY KEY,
    config_value VARCHAR(500) NOT NULL,
    updated_by VARCHAR(255),
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- 默认配置
INSERT INTO system_config (config_key, config_value) VALUES
('total_user_quota', '53687091200'),
('total_pool_quota', '32212254720'),
('default_user_quota', '10737418240'),
('daily_register_limit', '50'),
('today_register_count', '0'),
('register_date', CURDATE());

-- 初始超级管理员（密码: admin123，需首次登录修改）
INSERT INTO users (email, password_hash, nickname, role, storage_quota, email_verified, status)
VALUES ('admin@myweb.local', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EHs', '超级管理员', 'SUPER_ADMIN', 10737418240, 1, 'ACTIVE');
```

- [ ] **Step 5: 验证后端项目可启动**

运行: `cd backend && mvn spring-boot:run`
预期: 应用启动在 8080 端口，日志无错误

- [ ] **Step 6: Commit**

```bash
git add backend/
git commit -m "feat: 初始化 Spring Boot 后端项目（MyBatis-Plus + JWT + Mail）"
```

---

### Task 2: 前端项目初始化

**Files:** Create: `frontend/package.json`, `frontend/vite.config.ts`, `frontend/index.html`, `frontend/src/main.ts`, `frontend/src/App.vue`, `frontend/src/router/index.ts`

- [ ] **Step 1: 创建 package.json**

```json
{
  "name": "myweb-frontend",
  "version": "0.1.0",
  "private": true,
  "type": "module",
  "scripts": {
    "dev": "vite",
    "build": "vue-tsc && vite build",
    "preview": "vite preview"
  },
  "dependencies": {
    "vue": "^3.4.0",
    "vue-router": "^4.3.0",
    "pinia": "^2.1.0",
    "axios": "^1.6.0"
  },
  "devDependencies": {
    "@vitejs/plugin-vue": "^5.0.0",
    "typescript": "^5.3.0",
    "vite": "^5.1.0",
    "vue-tsc": "^2.0.0"
  }
}
```

- [ ] **Step 2: 创建 vite.config.ts**

```typescript
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { resolve } from 'path'

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': resolve(__dirname, 'src')
    }
  },
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://127.0.0.1:8080',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/api/, '')
      }
    }
  }
})
```

- [ ] **Step 3: 创建 index.html**

```html
<!DOCTYPE html>
<html lang="zh-CN">
<head>
  <meta charset="UTF-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1.0" />
  <title>MyWeb - 文件共享</title>
  <link rel="icon" type="image/svg+xml" href="/favicon.svg" />
</head>
<body>
  <div id="app"></div>
  <script type="module" src="/src/main.ts"></script>
</body>
</html>
```

- [ ] **Step 4: 创建 main.ts + App.vue + router**

`frontend/src/main.ts`:
```typescript
import { createApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'
import router from './router'

const app = createApp(App)
app.use(createPinia())
app.use(router)
app.mount('#app')
```

`frontend/src/App.vue`:
```vue
<template>
  <router-view />
</template>
```

`frontend/src/router/index.ts`:
```typescript
import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const routes = [
  // 公开路由
  { path: '/login', name: 'Login', component: () => import('@/views/Login.vue') },
  { path: '/register', name: 'Register', component: () => import('@/views/Register.vue') },
  { path: '/verify-email', name: 'VerifyEmail', component: () => import('@/views/VerifyEmail.vue') },
  // 占位路由（后续任务逐步替换为真实页面）
  { path: '/', name: 'Dashboard', component: () => import('@/views/Dashboard.vue'), meta: { requiresAuth: true } },
  { path: '/files', name: 'MyFiles', component: () => import('@/views/MyFiles.vue'), meta: { requiresAuth: true } },
  { path: '/files/upload', name: 'Upload', component: () => import('@/views/Upload.vue'), meta: { requiresAuth: true } },
  { path: '/inbox', name: 'Inbox', component: () => import('@/views/Inbox.vue'), meta: { requiresAuth: true } },
  { path: '/pool', name: 'Pool', component: () => import('@/views/Pool.vue'), meta: { requiresAuth: true } },
  { path: '/friends', name: 'Friends', component: () => import('@/views/Friends.vue'), meta: { requiresAuth: true } },
  { path: '/profile', name: 'Profile', component: () => import('@/views/Profile.vue'), meta: { requiresAuth: true } },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

router.beforeEach((to, _from, next) => {
  const authStore = useAuthStore()
  if (to.meta.requiresAuth && !authStore.isLoggedIn) {
    next({ name: 'Login', query: { redirect: to.fullPath } })
  } else {
    next()
  }
})

export default router
```

- [ ] **Step 5: 创建 auth store + api client**

`frontend/src/stores/auth.ts`:
```typescript
import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { authApi } from '@/api/auth'

export const useAuthStore = defineStore('auth', () => {
  const accessToken = ref(localStorage.getItem('access_token') || '')
  const refreshToken = ref(localStorage.getItem('refresh_token') || '')
  const user = ref<any>(null)

  const isLoggedIn = computed(() => !!accessToken.value)
  const isAdmin = computed(() => user.value?.role === 'ADMIN' || user.value?.role === 'SUPER_ADMIN')
  const isSuperAdmin = computed(() => user.value?.role === 'SUPER_ADMIN')

  async function login(email: string, password: string) {
    const res = await authApi.login({ email, password })
    accessToken.value = res.data.accessToken
    refreshToken.value = res.data.refreshToken
    user.value = res.data.user
    localStorage.setItem('access_token', res.data.accessToken)
    localStorage.setItem('refresh_token', res.data.refreshToken)
  }

  function logout() {
    accessToken.value = ''
    refreshToken.value = ''
    user.value = null
    localStorage.removeItem('access_token')
    localStorage.removeItem('refresh_token')
  }

  return { accessToken, refreshToken, user, isLoggedIn, isAdmin, isSuperAdmin, login, logout }
})
```

`frontend/src/api/client.ts`:
```typescript
import axios from 'axios'
import { useAuthStore } from '@/stores/auth'

const client = axios.create({
  baseURL: '/api/v1',
  timeout: 60000,
})

client.interceptors.request.use((config) => {
  const authStore = useAuthStore()
  if (authStore.accessToken) {
    config.headers.Authorization = `Bearer ${authStore.accessToken}`
  }
  return config
})

client.interceptors.response.use(
  (res) => res.data,
  (error) => {
    if (error.response?.status === 401) {
      const authStore = useAuthStore()
      authStore.logout()
      window.location.href = '/login'
    }
    return Promise.reject(error.response?.data || error)
  }
)

export default client
```

- [ ] **Step 6: 安装依赖并验证启动**

```bash
cd frontend && npm install && npm run dev
```
预期: 开发服务器在 5173 端口启动

- [ ] **Step 7: Commit**

```bash
git add frontend/
git commit -m "feat: 初始化 Vue3 前端项目（Vue Router + Pinia + Axios）"
```

---

### Task 3: 后端实体类与 Mapper

**Files:** Create: 5 个 entity, 5 个 mapper

- [ ] **Step 1: 创建 User 实体**

`backend/src/main/java/com/myweb/model/entity/User.java`:
```java
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
    private String role;        // USER / ADMIN / SUPER_ADMIN
    private Long storageQuota;  // 字节
    private Long storageUsed;   // 字节
    private String status;      // ACTIVE / DISABLED
    private Boolean emailVerified;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

- [ ] **Step 2: 创建 FileEntity**

`backend/src/main/java/com/myweb/model/entity/FileEntity.java`:
```java
package com.myweb.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("files")
public class FileEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String ownerEmail;
    private String title;
    private String description;
    private String originalName;
    private Boolean isSharedPool;
    private String reviewStatus;   // PENDING / APPROVED / REJECTED
    private String reviewComment;
    private String reviewedBy;
    private Long fileSize;
    private String mimeType;
    private String storagePath;
    private LocalDateTime expireAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

- [ ] **Step 3: 创建 FileTransfer, Friend, SystemConfig 实体**

`FileTransfer.java` — 字段: id(Long PK), fileId(Long), fromEmail(String), toEmail(String), status(String), message(String), createdAt(LocalDateTime), downloadedAt(LocalDateTime)

`Friend.java` — 字段: id(Long PK), userEmail(String), friendEmail(String), status(String PENDING/ACCEPTED), createdAt(LocalDateTime), acceptedAt(LocalDateTime)

`SystemConfig.java` — 字段: configKey(String PK), configValue(String), updatedBy(String), updatedAt(LocalDateTime)

- [ ] **Step 4: 创建 5 个 Mapper 接口**

`backend/src/main/java/com/myweb/repository/UserMapper.java`:
```java
package com.myweb.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.myweb.model.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {
}
```

同样创建 FileMapper, FileTransferMapper, FriendMapper, SystemConfigMapper，均继承 BaseMapper 对应实体类。

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/com/myweb/model/entity/ backend/src/main/java/com/myweb/repository/
git commit -m "feat: 添加实体类和 MyBatis Mapper"
```

---

### Task 4: JWT 工具类 + Spring Security 配置

**Files:** Create: `backend/src/main/java/com/myweb/util/JwtUtil.java`, `config/SecurityConfig.java`, `config/CorsConfig.java`, `filter/JwtAuthFilter.java`

- [ ] **Step 1: 创建 JwtUtil**

```java
package com.myweb.util;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtUtil {

    private final SecretKey key;
    private final long accessTokenExpiration;
    private final long refreshTokenExpiration;

    public JwtUtil(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-token-expiration}") long accessTokenExpiration,
            @Value("${jwt.refresh-token-expiration}") long refreshTokenExpiration) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenExpiration = accessTokenExpiration;
        this.refreshTokenExpiration = refreshTokenExpiration;
    }

    public String generateAccessToken(String email, String role) {
        return Jwts.builder()
                .subject(email)
                .claim("role", role)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + accessTokenExpiration))
                .signWith(key)
                .compact();
    }

    public String generateRefreshToken(String email) {
        return Jwts.builder()
                .subject(email)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + refreshTokenExpiration))
                .signWith(key)
                .compact();
    }

    public String getEmailFromToken(String token) {
        return parseClaims(token).getSubject();
    }

    public String getRoleFromToken(String token) {
        return parseClaims(token).get("role", String.class);
    }

    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    private Claims parseClaims(String token) {
        return Jwts.parser().verifyWith(key).build()
                .parseSignedClaims(token).getPayload();
    }
}
```

- [ ] **Step 2: 创建 JwtAuthFilter**

`backend/src/main/java/com/myweb/filter/JwtAuthFilter.java`:
```java
package com.myweb.filter;

import com.myweb.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            if (jwtUtil.validateToken(token)) {
                String email = jwtUtil.getEmailFromToken(token);
                String role = jwtUtil.getRoleFromToken(token);
                UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(
                                email, null,
                                List.of(new SimpleGrantedAuthority("ROLE_" + role)));
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        }
        filterChain.doFilter(request, response);
    }
}
```

- [ ] **Step 3: 创建 SecurityConfig**

`backend/src/main/java/com/myweb/config/SecurityConfig.java`:
```java
package com.myweb.config;

import com.myweb.filter.JwtAuthFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // 公开
                .requestMatchers("/auth/**").permitAll()
                // 普通用户+
                .requestMatchers("/users/me/**", "/users/search/**").hasAnyRole("USER", "ADMIN", "SUPER_ADMIN")
                .requestMatchers("/upload/**", "/files/**").hasAnyRole("USER", "ADMIN", "SUPER_ADMIN")
                .requestMatchers("/transfers/**").hasAnyRole("USER", "ADMIN", "SUPER_ADMIN")
                .requestMatchers("/friends/**").hasAnyRole("USER", "ADMIN", "SUPER_ADMIN")
                .requestMatchers("/pool/**").hasAnyRole("USER", "ADMIN", "SUPER_ADMIN")
                // 管理员+
                .requestMatchers("/admin/**").hasAnyRole("ADMIN", "SUPER_ADMIN")
                // 仅超管
                .requestMatchers("/super-admin/**").hasRole("SUPER_ADMIN")
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
```

- [ ] **Step 4: 创建 CorsConfig**

```java
package com.myweb.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.List;

@Configuration
public class CorsConfig {
    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of("*"));
        config.setAllowedMethods(List.of("*"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }
}
```

- [ ] **Step 5: 验证安全配置**

用 curl 测试未认证请求被拦截：

```bash
curl http://localhost:8080/api/v1/files
```
预期: HTTP 403

- [ ] **Step 6: Commit**

```bash
git add backend/src/main/java/com/myweb/util/JwtUtil.java backend/src/main/java/com/myweb/config/ backend/src/main/java/com/myweb/filter/
git commit -m "feat: JWT 工具类 + Spring Security 无状态配置"
```

---

### Task 5: 认证服务（注册 + 登录 + 邮箱验证）

**Files:** Create: `AuthService.java`, `AuthController.java`, `EmailUtil.java`, DTOs; Modify: MyBatisPlusConfig.java

- [ ] **Step 1: 创建 EmailUtil**

`backend/src/main/java/com/myweb/util/EmailUtil.java`:
```java
package com.myweb.util;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
public class EmailUtil {

    private final JavaMailSender mailSender;
    // 内存存储验证码：email → code
    private final Map<String, VerificationRecord> codes = new ConcurrentHashMap<>();

    public void sendVerificationCode(String toEmail) {
        String code = String.format("%06d", new Random().nextInt(1000000));
        codes.put(toEmail, new VerificationRecord(code, System.currentTimeMillis()));

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(toEmail);
            helper.setSubject("MyWeb 邮箱验证码");
            helper.setText("您的验证码是：" + code + "，5 分钟内有效。", false);
            mailSender.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException("验证码发送失败", e);
        }
    }

    public boolean verifyCode(String email, String code) {
        VerificationRecord record = codes.get(email);
        if (record == null) return false;
        // 5 分钟过期
        if (System.currentTimeMillis() - record.timestamp > 300_000) {
            codes.remove(email);
            return false;
        }
        if (record.code.equals(code)) {
            codes.remove(email);
            return true;
        }
        return false;
    }

    private record VerificationRecord(String code, long timestamp) {}
}
```

- [ ] **Step 2: 创建 DTO**

`RegisterRequest.java`:
```java
package com.myweb.model.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequest {
    @Email @NotBlank
    private String email;
    @NotBlank @Size(min = 6, max = 50)
    private String password;
    @NotBlank @Size(min = 1, max = 50)
    private String nickname;
}
```

`LoginRequest.java`: email + password
`LoginResponse.java`: accessToken, refreshToken, user (UserVO)

- [ ] **Step 3: 创建 UserVO**

```java
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
```

- [ ] **Step 4: 创建 AuthService**

`backend/src/main/java/com/myweb/service/AuthService.java`:
```java
package com.myweb.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.myweb.model.dto.LoginRequest;
import com.myweb.model.dto.LoginResponse;
import com.myweb.model.dto.RegisterRequest;
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
        // 校验验证码
        if (!emailUtil.verifyCode(req.getEmail(), verificationCode)) {
            throw new RuntimeException("验证码错误或已过期");
        }

        // 检查邮箱是否已注册
        if (userMapper.selectById(req.getEmail()) != null) {
            throw new RuntimeException("该邮箱已注册");
        }

        // 检查每日注册上限
        var todayCountConfig = configMapper.selectById("today_register_count");
        var dailyLimitConfig = configMapper.selectById("daily_register_limit");
        var registerDateConfig = configMapper.selectById("register_date");
        int todayCount = Integer.parseInt(todayCountConfig.getConfigValue());
        int dailyLimit = Integer.parseInt(dailyLimitConfig.getConfigValue());

        // 日期变了，重置计数
        if (!LocalDate.now().toString().equals(registerDateConfig.getConfigValue())) {
            todayCount = 0;
            configMapper.updateById(
                new com.myweb.model.entity.SystemConfig() {{
                    setConfigKey("register_date");
                    setConfigValue(LocalDate.now().toString());
                }});
        }

        if (todayCount >= dailyLimit) {
            throw new RuntimeException("今日注册已达上限，请明天再试");
        }

        // 计算配额
        var totalQuotaConfig = configMapper.selectById("total_user_quota");
        var defaultQuotaConfig = configMapper.selectById("default_user_quota");
        long totalQuota = Long.parseLong(totalQuotaConfig.getConfigValue());
        long defaultQuota = Long.parseLong(defaultQuotaConfig.getConfigValue());

        // 计算已分配总量
        Long allocatedTotal = userMapper.selectList(null).stream()
                .mapToLong(User::getStorageQuota).sum();
        long remaining = totalQuota - allocatedTotal;
        long assignedQuota = Math.min(defaultQuota, Math.max(0, remaining));

        // 创建用户
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

        // 更新今日计数
        configMapper.updateById(
            new com.myweb.model.entity.SystemConfig() {{
                setConfigKey("today_register_count");
                setConfigValue(String.valueOf(todayCount + 1));
            }});

        // 返回 token
        String accessToken = jwtUtil.generateAccessToken(user.getEmail(), user.getRole());
        String refreshToken = jwtUtil.generateRefreshToken(user.getEmail());
        return buildLoginResponse(user, accessToken, refreshToken, assignedQuota != defaultQuota ? assignedQuota : null);
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

    private LoginResponse buildLoginResponse(User user, String accessToken, String refreshToken, Long quotaWarning) {
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
            resp.setMessage("由于用户空间有限，您的个人配额为 " + (quotaWarning / 1073741824.0) + " GB，低于默认值");
        }
        return resp;
    }
}
```

- [ ] **Step 5: 创建 AuthController**

```java
package com.myweb.controller;

import com.myweb.model.dto.LoginRequest;
import com.myweb.model.dto.LoginResponse;
import com.myweb.model.dto.RegisterRequest;
import com.myweb.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/send-code")
    public ResponseEntity<?> sendCode(@RequestBody Map<String, String> body) {
        authService.sendVerificationCode(body.get("email"));
        return ResponseEntity.ok(Map.of("code", 0, "message", "验证码已发送"));
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest req,
                                       @RequestParam String code) {
        try {
            LoginResponse resp = authService.register(req, code);
            return ResponseEntity.ok(Map.of("code", 0, "message", "success", "data", resp));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("code", 1005, "message", e.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest req) {
        try {
            LoginResponse resp = authService.login(req);
            return ResponseEntity.ok(Map.of("code", 0, "message", "success", "data", resp));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("code", 1005, "message", e.getMessage()));
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@RequestBody Map<String, String> body) {
        try {
            LoginResponse resp = authService.refresh(body.get("refreshToken"));
            return ResponseEntity.ok(Map.of("code", 0, "message", "success", "data", resp));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("code", 1002, "message", e.getMessage()));
        }
    }
}
```

- [ ] **Step 6: Commit**

```bash
git add backend/src/main/java/com/myweb/service/AuthService.java backend/src/main/java/com/myweb/controller/AuthController.java backend/src/main/java/com/myweb/util/EmailUtil.java backend/src/main/java/com/myweb/model/
git commit -m "feat: 认证服务（注册+登录+邮箱验证+JWT双Token）"
```

---

### Task 6: 文件上传服务（断点续传）

**Files:** Create: `FileUtil.java`, `UploadService.java`, `UploadController.java`, DTOs

- [ ] **Step 1: 创建 FileUtil**

`backend/src/main/java/com/myweb/util/FileUtil.java`:
```java
package com.myweb.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.UUID;

@Component
public class FileUtil {

    private final Path storagePath;
    private final Path tmpPath;

    public FileUtil(
            @Value("${app.storage-path}") String storagePath,
            @Value("${app.upload-tmp-path}") String tmpPath) {
        this.storagePath = Path.of(storagePath);
        this.tmpPath = Path.of(tmpPath);
        ensureDir(this.storagePath);
        ensureDir(this.tmpPath);
    }

    private void ensureDir(Path path) {
        try { Files.createDirectories(path); } catch (IOException e) { throw new RuntimeException(e); }
    }

    /** 存储分片的临时目录 */
    public Path getChunkDir(String uploadId) {
        Path dir = tmpPath.resolve(uploadId);
        try { Files.createDirectories(dir); } catch (IOException e) { throw new RuntimeException(e); }
        return dir;
    }

    /** 写入单个分片 */
    public void writeChunk(String uploadId, int index, InputStream inputStream) throws IOException {
        Path chunkFile = getChunkDir(uploadId).resolve(index + ".chunk");
        Files.copy(inputStream, chunkFile, StandardCopyOption.REPLACE_EXISTING);
    }

    /** 合并所有分片到最终文件，返回存储路径 */
    public Path mergeChunks(String uploadId, String originalName, int totalChunks) throws IOException {
        String ext = "";
        int dotIndex = originalName.lastIndexOf('.');
        if (dotIndex > 0) ext = originalName.substring(dotIndex);

        String storedName = UUID.randomUUID() + ext;
        Path finalPath = storagePath.resolve(storedName);

        try (OutputStream out = Files.newOutputStream(finalPath)) {
            for (int i = 0; i < totalChunks; i++) {
                Path chunkFile = getChunkDir(uploadId).resolve(i + ".chunk");
                if (!Files.exists(chunkFile)) {
                    throw new IOException("分片 " + i + " 缺失");
                }
                Files.copy(chunkFile, out);
            }
        }

        // 清理临时分片
        deleteDir(getChunkDir(uploadId));
        return finalPath;
    }

    /** MD5 校验文件 */
    public String md5(Path file) throws Exception {
        MessageDigest md = MessageDigest.getInstance("MD5");
        try (InputStream is = Files.newInputStream(file)) {
            byte[] buf = new byte[8192];
            int n;
            while ((n = is.read(buf)) != -1) md.update(buf, 0, n);
        }
        StringBuilder sb = new StringBuilder();
        for (byte b : md.digest()) sb.append(String.format("%02x", b));
        return sb.toString();
    }

    /** 删除文件 */
    public void deleteFile(Path path) throws IOException {
        Files.deleteIfExists(path);
    }

    public void deleteDir(Path dir) throws IOException {
        if (Files.exists(dir)) {
            try (var stream = Files.walk(dir)) {
                stream.sorted(java.util.Comparator.reverseOrder())
                      .forEach(p -> { try { Files.delete(p); } catch (IOException ignored) {} });
            }
        }
    }
}
```

- [ ] **Step 2: 创建 UploadService**

`backend/src/main/java/com/myweb/service/UploadService.java`:
```java
package com.myweb.service;

import com.myweb.util.FileUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class UploadService {

    private final FileUtil fileUtil;
    // uploadId → UploadSession
    private final Map<String, UploadSession> sessions = new ConcurrentHashMap<>();

    public record UploadInitResponse(String uploadId, int chunkSize, int totalChunks, List<Integer> uploadedChunks) {}

    public UploadInitResponse initUpload(String originalName, long fileSize) {
        String uploadId = UUID.randomUUID().toString();
        int chunkSize = 5 * 1024 * 1024; // 5MB
        int totalChunks = (int) Math.ceil((double) fileSize / chunkSize);

        UploadSession session = new UploadSession();
        session.originalName = originalName;
        session.fileSize = fileSize;
        session.totalChunks = totalChunks;
        session.chunkSize = chunkSize;
        session.uploadedChunks = new HashSet<>();
        sessions.put(uploadId, session);

        return new UploadInitResponse(uploadId, chunkSize, totalChunks, List.of());
    }

    public void uploadChunk(String uploadId, int index, InputStream inputStream) throws IOException {
        UploadSession session = sessions.get(uploadId);
        if (session == null) throw new RuntimeException("上传会话不存在或已过期");
        fileUtil.writeChunk(uploadId, index, inputStream);
        session.uploadedChunks.add(index);
    }

    public UploadInitResponse getStatus(String uploadId) {
        UploadSession session = sessions.get(uploadId);
        if (session == null) throw new RuntimeException("上传会话不存在");
        List<Integer> uploaded = new ArrayList<>(session.uploadedChunks);
        List<Integer> missing = new ArrayList<>();
        for (int i = 0; i < session.totalChunks; i++) {
            if (!session.uploadedChunks.contains(i)) missing.add(i);
        }
        return new UploadInitResponse(uploadId, session.chunkSize, session.totalChunks, missing);
    }

    public PathData completeUpload(String uploadId) throws IOException {
        UploadSession session = sessions.get(uploadId);
        if (session == null) throw new RuntimeException("上传会话不存在");
        if (session.uploadedChunks.size() != session.totalChunks) {
            throw new RuntimeException("尚有 " + (session.totalChunks - session.uploadedChunks.size()) + " 个分片未上传");
        }
        Path finalPath = fileUtil.mergeChunks(uploadId, session.originalName, session.totalChunks);
        sessions.remove(uploadId);
        return new PathData(finalPath, session.originalName, session.fileSize);
    }

    public record PathData(Path path, String originalName, long fileSize) {}

    private static class UploadSession {
        String originalName;
        long fileSize;
        int totalChunks;
        int chunkSize;
        Set<Integer> uploadedChunks;
    }
}
```

- [ ] **Step 3: 创建 UploadController**

```java
package com.myweb.controller;

import com.myweb.service.UploadService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/upload")
@RequiredArgsConstructor
public class UploadController {

    private final UploadService uploadService;

    @PostMapping("/init")
    public ResponseEntity<?> init(@RequestBody Map<String, Object> body) {
        String originalName = (String) body.get("originalName");
        long fileSize = ((Number) body.get("fileSize")).longValue();
        var resp = uploadService.initUpload(originalName, fileSize);
        return ResponseEntity.ok(Map.of("code", 0, "data", resp));
    }

    @PostMapping("/{uploadId}/chunk/{index}")
    public ResponseEntity<?> chunk(@PathVariable String uploadId,
                                    @PathVariable int index,
                                    HttpServletRequest request) {
        try {
            uploadService.uploadChunk(uploadId, index, request.getInputStream());
            return ResponseEntity.ok(Map.of("code", 0, "message", "ok"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("code", 2001, "message", e.getMessage()));
        }
    }

    @GetMapping("/{uploadId}/status")
    public ResponseEntity<?> status(@PathVariable String uploadId) {
        try {
            var resp = uploadService.getStatus(uploadId);
            return ResponseEntity.ok(Map.of("code", 0, "data", resp));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("code", 2001, "message", e.getMessage()));
        }
    }

    @PostMapping("/{uploadId}/complete")
    public ResponseEntity<?> complete(@PathVariable String uploadId) {
        try {
            var pathData = uploadService.completeUpload(uploadId);
            return ResponseEntity.ok(Map.of("code", 0, "data", Map.of(
                "storagePath", pathData.path().toString(),
                "originalName", pathData.originalName(),
                "fileSize", pathData.fileSize()
            )));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("code", 2001, "message", e.getMessage()));
        }
    }
}
```

- [ ] **Step 4: Commit**

```bash
git add backend/src/main/java/com/myweb/util/FileUtil.java backend/src/main/java/com/myweb/service/UploadService.java backend/src/main/java/com/myweb/controller/UploadController.java
git commit -m "feat: 断点续传文件上传服务（分片/合并/状态查询）"
```

---

### Task 7: 文件管理服务（CRUD + 容量检查）

**Files:** Create: `FileService.java`, `FileController.java`, DTOs

- [ ] **Step 1: 创建 FileService**

`backend/src/main/java/com/myweb/service/FileService.java`:
```java
package com.myweb.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.myweb.model.entity.FileEntity;
import com.myweb.model.entity.User;
import com.myweb.model.entity.SystemConfig;
import com.myweb.repository.FileMapper;
import com.myweb.repository.SystemConfigMapper;
import com.myweb.repository.UserMapper;
import com.myweb.util.FileUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class FileService {

    private final FileMapper fileMapper;
    private final FileTransferMapper transferMapper;
    private final UserMapper userMapper;
    private final SystemConfigMapper configMapper;
    private final FileUtil fileUtil;

    private String currentUserEmail() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    @Transactional
    public FileEntity createFile(String title, String description, boolean isSharedPool,
                                  LocalDateTime expireAt, Path storagePath,
                                  String originalName, long fileSize, String mimeType) {
        String email = currentUserEmail();
        User user = userMapper.selectById(email);

        // 容量检查
        long newUsed = user.getStorageUsed() + fileSize;
        if (!isSharedPool && newUsed > user.getStorageQuota()) {
            throw new RuntimeException("个人空间不足（已用 " +
                (user.getStorageUsed() / 1073741824.0) + " GB / 配额 " +
                (user.getStorageQuota() / 1073741824.0) + " GB）");
        }

        if (isSharedPool) {
            var poolConfig = configMapper.selectById("total_pool_quota");
            long poolQuota = Long.parseLong(poolConfig.getConfigValue());
            Long poolUsed = fileMapper.selectList(
                new LambdaQueryWrapper<FileEntity>()
                    .eq(FileEntity::getIsSharedPool, true)
                    .eq(FileEntity::getReviewStatus, "APPROVED")
            ).stream().mapToLong(FileEntity::getFileSize).sum();
            if (poolUsed + fileSize > poolQuota) {
                throw new RuntimeException("文件共享池已满，请联系管理员清理");
            }
        }

        FileEntity file = new FileEntity();
        file.setOwnerEmail(email);
        file.setTitle(title);
        file.setDescription(description);
        file.setOriginalName(originalName);
        file.setIsSharedPool(isSharedPool);
        file.setReviewStatus(isSharedPool ? "PENDING" : "APPROVED");
        file.setFileSize(fileSize);
        file.setMimeType(mimeType);
        file.setStoragePath(storagePath.toString());
        file.setExpireAt(expireAt);
        fileMapper.insert(file);

        // 更新用户用量
        user.setStorageUsed(newUsed);
        userMapper.updateById(user);

        return file;
    }

    public Page<FileEntity> listMyFiles(int page, int size) {
        String email = currentUserEmail();
        return fileMapper.selectPage(
            new Page<>(page, size),
            new LambdaQueryWrapper<FileEntity>()
                .eq(FileEntity::getOwnerEmail, email)
                .orderByDesc(FileEntity::getCreatedAt)
        );
    }

    public Page<FileEntity> listPoolFiles(int page, int size) {
        return fileMapper.selectPage(
            new Page<>(page, size),
            new LambdaQueryWrapper<FileEntity>()
                .eq(FileEntity::getIsSharedPool, true)
                .eq(FileEntity::getReviewStatus, "APPROVED")
                .orderByDesc(FileEntity::getCreatedAt)
        );
    }

    public FileEntity getFile(Long id) {
        return fileMapper.selectById(id);
    }

    @Transactional
    public void deleteFile(Long id) {
        String email = currentUserEmail();
        FileEntity file = fileMapper.selectById(id);
        if (file == null) throw new RuntimeException("文件不存在");
        if (!file.getOwnerEmail().equals(email)) {
            User user = userMapper.selectById(email);
            if (!"ADMIN".equals(user.getRole()) && !"SUPER_ADMIN".equals(user.getRole())) {
                throw new RuntimeException("无权删除他人文件");
            }
        }
        // 删除 NAS 实体
        try { fileUtil.deleteFile(Path.of(file.getStoragePath())); } catch (IOException ignored) {}
        // 更新用户用量
        User owner = userMapper.selectById(file.getOwnerEmail());
        owner.setStorageUsed(owner.getStorageUsed() - file.getFileSize());
        userMapper.updateById(owner);
        // 删除数据库记录
        fileMapper.deleteById(id);
    }

    public InputStream downloadFile(Long id) throws IOException {
        String email = currentUserEmail();
        FileEntity file = fileMapper.selectById(id);
        if (file == null) throw new RuntimeException("文件不存在");

        // 文件所有者可下载
        if (file.getOwnerEmail().equals(email)) {
            return Files.newInputStream(Path.of(file.getStoragePath()));
        }
        // 传输接收者可下载
        Long transferCount = transferMapper.selectCount(
            new LambdaQueryWrapper<FileTransfer>()
                .eq(FileTransfer::getFileId, id)
                .eq(FileTransfer::getToEmail, email));
        if (transferCount > 0) {
                new LambdaQueryWrapper<FileTransfer>()
                    .eq(FileTransfer::getFileId, id)
                    .eq(FileTransfer::getToEmail, email));
            if (transfer.getDownloadedAt() == null) {
                transfer.setDownloadedAt(LocalDateTime.now());
                transfer.setStatus("DOWNLOADED");
                transferMapper.updateById(transfer);
            }
            return Files.newInputStream(Path.of(file.getStoragePath()));
        }
        // 共享池已审核文件所有人可下载
        if (file.getIsSharedPool() && "APPROVED".equals(file.getReviewStatus())) {
            return Files.newInputStream(Path.of(file.getStoragePath()));
        }
        throw new RuntimeException("无权下载此文件");
    }
}
```

- [ ] **Step 2: 创建 FileController**

```java
package com.myweb.controller;

import com.myweb.model.entity.FileEntity;
import com.myweb.model.vo.FileVO;
import com.myweb.service.FileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/files")
@RequiredArgsConstructor
public class FileController {

    private final FileService fileService;

    @PostMapping
    public ResponseEntity<?> create(@RequestBody Map<String, Object> body) {
        try {
            String title = (String) body.get("title");
            String description = (String) body.get("description");
            boolean isSharedPool = Boolean.TRUE.equals(body.get("isSharedPool"));
            String expireAtStr = (String) body.get("expireAt");
            LocalDateTime expireAt = expireAtStr != null ? LocalDateTime.parse(expireAtStr) : null;
            String storagePath = (String) body.get("storagePath");
            String originalName = (String) body.get("originalName");
            long fileSize = ((Number) body.get("fileSize")).longValue();
            String mimeType = (String) body.get("mimeType");

            FileEntity file = fileService.createFile(title, description, isSharedPool,
                expireAt, Path.of(storagePath), originalName, fileSize, mimeType);
            return ResponseEntity.ok(Map.of("code", 0, "data", FileVO.from(file)));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("code", 1005, "message", e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<?> list(@RequestParam(defaultValue = "1") int page,
                                   @RequestParam(defaultValue = "20") int size) {
        var pageResult = fileService.listMyFiles(page, size);
        return ResponseEntity.ok(Map.of("code", 0, "data", Map.of(
            "total", pageResult.getTotal(),
            "records", pageResult.getRecords().stream().map(FileVO::from).toList()
        )));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> get(@PathVariable Long id) {
        FileEntity file = fileService.getFile(id);
        if (file == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(Map.of("code", 0, "data", FileVO.from(file)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        try {
            fileService.deleteFile(id);
            return ResponseEntity.ok(Map.of("code", 0, "message", "删除成功"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("code", 1005, "message", e.getMessage()));
        }
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<?> download(@PathVariable Long id) {
        try {
            FileEntity file = fileService.getFile(id);
            var inputStream = fileService.downloadFile(id);
            return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=\"" + file.getOriginalName() + "\"")
                .header("Content-Type", file.getMimeType() != null ? file.getMimeType() : "application/octet-stream")
                .body(inputStream.readAllBytes());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("code", 1005, "message", e.getMessage()));
        }
    }
}
```

- [ ] **Step 3: 创建 FileVO**

```java
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
```

- [ ] **Step 4: Commit**

```bash
git add backend/src/main/java/com/myweb/service/FileService.java backend/src/main/java/com/myweb/controller/FileController.java backend/src/main/java/com/myweb/model/vo/FileVO.java
git commit -m "feat: 文件管理服务（CRUD + 容量检查 + 下载权限）"
```

---

### Task 8: 文件互传 + 好友系统

**Files:** Create: `FileTransferService.java`, `FileTransferController.java`, `FileTransferMapper.java`, `FriendService.java`, `FriendController.java`, `FriendMapper.java`, DTOs/VOs

- [ ] **Step 1: FileTransferService**

```java
package com.myweb.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.myweb.model.entity.FileTransfer;
import com.myweb.model.entity.FileEntity;
import com.myweb.repository.FileTransferMapper;
import com.myweb.repository.FileMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FileTransferService {

    private final FileTransferMapper transferMapper;
    private final FileMapper fileMapper;

    private String currentUserEmail() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    @Transactional
    public FileTransfer send(Long fileId, String toEmail, String message) {
        String fromEmail = currentUserEmail();
        FileEntity file = fileMapper.selectById(fileId);
        if (file == null) throw new RuntimeException("文件不存在");
        if (!file.getOwnerEmail().equals(fromEmail)) throw new RuntimeException("只能发送自己的文件");

        FileTransfer transfer = new FileTransfer();
        transfer.setFileId(fileId);
        transfer.setFromEmail(fromEmail);
        transfer.setToEmail(toEmail);
        transfer.setStatus("SENT");
        transfer.setMessage(message);
        transferMapper.insert(transfer);
        return transfer;
    }

    public Page<FileTransfer> listInbox(int page, int size) {
        String email = currentUserEmail();
        return transferMapper.selectPage(new Page<>(page, size),
            new LambdaQueryWrapper<FileTransfer>()
                .eq(FileTransfer::getToEmail, email)
                .orderByDesc(FileTransfer::getCreatedAt));
    }

    public Page<FileTransfer> listSent(int page, int size) {
        String email = currentUserEmail();
        return transferMapper.selectPage(new Page<>(page, size),
            new LambdaQueryWrapper<FileTransfer>()
                .eq(FileTransfer::getFromEmail, email)
                .orderByDesc(FileTransfer::getCreatedAt));
    }

    @Transactional
    public void delete(Long id) {
        String email = currentUserEmail();
        FileTransfer transfer = transferMapper.selectById(id);
        if (transfer == null) throw new RuntimeException("记录不存在");
        if (!transfer.getFromEmail().equals(email) && !transfer.getToEmail().equals(email))
            throw new RuntimeException("无权删除");
        transferMapper.deleteById(id);
    }

    /** 检查用户是否是指定文件的传输接收者 */
    public boolean isRecipient(Long fileId, String email) {
        Long count = transferMapper.selectCount(new LambdaQueryWrapper<FileTransfer>()
            .eq(FileTransfer::getFileId, fileId)
            .eq(FileTransfer::getToEmail, email));
        return count > 0;
    }
}
```

- [ ] **Step 2: FileTransferController**

```java
package com.myweb.controller;

import com.myweb.model.dto.TransferRequest;
import com.myweb.service.FileTransferService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/transfers")
@RequiredArgsConstructor
public class FileTransferController {

    private final FileTransferService transferService;

    @PostMapping
    public ResponseEntity<?> send(@Valid @RequestBody TransferRequest req) {
        try {
            var transfer = transferService.send(req.getFileId(), req.getToEmail(), req.getMessage());
            return ResponseEntity.ok(Map.of("code", 0, "data", transfer));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("code", 1005, "message", e.getMessage()));
        }
    }

    @GetMapping("/inbox")
    public ResponseEntity<?> inbox(@RequestParam(defaultValue = "1") int page,
                                    @RequestParam(defaultValue = "20") int size) {
        var pageResult = transferService.listInbox(page, size);
        return ResponseEntity.ok(Map.of("code", 0, "data", pageResult));
    }

    @GetMapping("/sent")
    public ResponseEntity<?> sent(@RequestParam(defaultValue = "1") int page,
                                   @RequestParam(defaultValue = "20") int size) {
        var pageResult = transferService.listSent(page, size);
        return ResponseEntity.ok(Map.of("code", 0, "data", pageResult));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        try {
            transferService.delete(id);
            return ResponseEntity.ok(Map.of("code", 0, "message", "删除成功"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("code", 1005, "message", e.getMessage()));
        }
    }
}
```

- [ ] **Step 3: FriendService + FriendController**

FriendService 方法：
- `sendRequest(friendEmail)` — 创建 PENDING 记录
- `handleRequest(id, accept)` — ACCEPTED 或删除
- `listFriends()` — 查 ACCEPTED 双向好友
- `deleteFriend(friendEmail)` — 双向删除
- `isFriend(email1, email2)` — 检查好友关系

FriendController 路由：POST `/friends` / PUT `/friends/{id}` / GET `/friends` / DELETE `/friends/{email}`

- [ ] **Step 4: Commit**

```bash
git add backend/src/main/java/com/myweb/service/FileTransferService.java backend/src/main/java/com/myweb/service/FriendService.java backend/src/main/java/com/myweb/controller/FileTransferController.java backend/src/main/java/com/myweb/controller/FriendController.java
git commit -m "feat: 文件互传 + 好友系统"
```

---

### Task 8.5: 用户管理 + 共享池审核 + 系统配置

**Files:** Create: `UserService.java`, `UserController.java`, `AdminController.java`, `SystemConfigService.java`, `SystemConfigController.java`, DTOs

- [ ] **Step 1: UserService**

```java
package com.myweb.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.myweb.model.entity.Friend;
import com.myweb.model.entity.User;
import com.myweb.model.vo.UserVO;
import com.myweb.repository.FriendMapper;
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
    private final FriendMapper friendMapper;
    private final PasswordEncoder passwordEncoder;

    private String currentUserEmail() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    public UserVO getProfile() {
        User user = userMapper.selectById(currentUserEmail());
        return toVO(user);
    }

    @Transactional
    public UserVO updateProfile(String nickname, String avatar, String oldPassword, String newPassword) {
        User user = userMapper.selectById(currentUserEmail());
        if (nickname != null) user.setNickname(nickname);
        if (avatar != null) user.setAvatar(avatar);
        if (newPassword != null && !newPassword.isBlank()) {
            if (!passwordEncoder.matches(oldPassword, user.getPasswordHash()))
                throw new RuntimeException("原密码错误");
            user.setPasswordHash(passwordEncoder.encode(newPassword));
        }
        userMapper.updateById(user);
        return toVO(user);
    }

    /** 按邮箱前缀搜索用户，非好友脱敏 */
    public List<UserVO> searchUsers(String emailPrefix) {
        String me = currentUserEmail();
        List<User> users = userMapper.selectList(new LambdaQueryWrapper<User>()
            .likeRight(User::getEmail, emailPrefix)
            .ne(User::getEmail, me)
            .eq(User::getStatus, "ACTIVE")
            .last("LIMIT 20"));

        // 获取好友列表用于脱敏判断
        List<String> friendEmails = friendMapper.selectList(new LambdaQueryWrapper<Friend>()
            .eq(Friend::getUserEmail, me)
            .eq(Friend::getStatus, "ACCEPTED"))
            .stream().map(Friend::getFriendEmail).toList();

        return users.stream().map(u -> {
            UserVO vo = toVO(u);
            if (!friendEmails.contains(u.getEmail())) {
                // 非好友脱敏
                String[] parts = u.getEmail().split("@");
                vo.setEmail(parts[0].substring(0, Math.min(3, parts[0].length())) + "***@" + parts[1]);
            }
            return vo;
        }).toList();
    }

    // 管理员方法
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

    private UserVO toVO(User u) {
        return UserVO.builder()
            .email(u.getEmail()).nickname(u.getNickname()).avatar(u.getAvatar())
            .role(u.getRole()).storageQuota(u.getStorageQuota()).storageUsed(u.getStorageUsed())
            .createdAt(u.getCreatedAt()).build();
    }
}
```

- [ ] **Step 2: UserController**

路由：GET `/users/me` / PUT `/users/me` / GET `/users/search?email=xxx`

AdminController 路由：GET `/admin/users` / PUT `/admin/users/{email}/quota` / PUT `/admin/users/{email}/status`

- [ ] **Step 3: 共享池 + 系统配置 Controller**

AdminController（共享池审核）：GET `/pool` (已审核) / GET `/admin/review` (待审核) / PUT `/admin/review/{id}` (审核)

SystemConfigController：GET `/admin/config` / PUT `/admin/config`（仅 SUPER_ADMIN）
- SystemConfigService 处理全局容量、注册限制等配置的读写
- `updateConfig` 时校验：修改 total_user_quota 不能小于已分配总量

- [ ] **Step 4: Commit**

```bash
git add backend/src/main/java/com/myweb/service/UserService.java backend/src/main/java/com/myweb/service/SystemConfigService.java backend/src/main/java/com/myweb/controller/
git commit -m "feat: 用户管理 + 共享池审核 + 系统配置"
```

---

### Task 9: 定时清理 + MyBatis-Plus 配置 + FileService 修正

**Files:** Create: `FileCleanupService.java`, `MyBatisPlusConfig.java`

- [ ] **Step 1: MyBatisPlusConfig**

```java
package com.myweb.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MyBatisPlusConfig {
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        return interceptor;
    }
}
```

- [ ] **Step 2: FileCleanupService**

```java
package com.myweb.service;

import com.myweb.model.entity.FileEntity;
import com.myweb.repository.FileMapper;
import com.myweb.repository.FileTransferMapper;
import com.myweb.repository.UserMapper;
import com.myweb.util.FileUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileCleanupService {

    private final FileMapper fileMapper;
    private final FileTransferMapper transferMapper;
    private final UserMapper userMapper;
    private final FileUtil fileUtil;

    @Scheduled(cron = "0 0 3 * * ?")  // 每天凌晨 3:00
    public void cleanExpiredFiles() {
        log.info("开始清理过期文件...");
        List<FileEntity> expired = fileMapper.selectList(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<FileEntity>()
                .isNotNull(FileEntity::getExpireAt)
                .lt(FileEntity::getExpireAt, LocalDateTime.now())
        );

        for (FileEntity file : expired) {
            try {
                // 1. 删除 NAS 文件
                fileUtil.deleteFile(Path.of(file.getStoragePath()));
                // 2. 删除传输记录
                transferMapper.delete(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.myweb.model.entity.FileTransfer>()
                        .eq(com.myweb.model.entity.FileTransfer::getFileId, file.getId())
                );
                // 3. 更新用户用量
                var user = userMapper.selectById(file.getOwnerEmail());
                if (user != null) {
                    user.setStorageUsed(user.getStorageUsed() - file.getFileSize());
                    userMapper.updateById(user);
                }
                // 4. 删除文件记录
                fileMapper.deleteById(file.getId());
                log.info("已清理过期文件: {} (ID: {})", file.getOriginalName(), file.getId());
            } catch (Exception e) {
                log.error("清理文件失败: {} (ID: {})", file.getOriginalName(), file.getId(), e);
            }
        }
        log.info("过期文件清理完成，共清理 {} 个文件", expired.size());
    }
}
```

- [ ] **Step 3: Commit**

```bash
git add backend/src/main/java/com/myweb/config/MyBatisPlusConfig.java backend/src/main/java/com/myweb/service/FileCleanupService.java
git commit -m "feat: 定时清理过期文件 + MyBatis-Plus 分页插件"
```

---

### Task 10: 前端 API 层

**Files:** Create: `frontend/src/api/auth.ts`, `files.ts`, `transfers.ts`, `friends.ts`, `users.ts`, `admin.ts`, `frontend/src/utils/format.ts`, `validators.ts`

- [ ] **Step 1: API 文件**

`frontend/src/api/auth.ts`:
```typescript
import client from './client'

export const authApi = {
  sendCode: (email: string) => client.post('/auth/send-code', { email }),
  register: (data: { email: string; password: string; nickname: string }, code: string) =>
    client.post(`/auth/register?code=${code}`, data),
  login: (data: { email: string; password: string }) => client.post('/auth/login', data),
  refresh: (refreshToken: string) => client.post('/auth/refresh', { refreshToken }),
}
```

`frontend/src/api/files.ts`:
```typescript
import client from './client'

export const filesApi = {
  uploadInit: (data: { originalName: string; fileSize: number }) =>
    client.post('/upload/init', data),
  uploadChunk: (uploadId: string, index: number, chunk: Blob) =>
    client.post(`/upload/${uploadId}/chunk/${index}`, chunk, {
      headers: { 'Content-Type': 'application/octet-stream' }
    }),
  getUploadStatus: (uploadId: string) => client.get(`/upload/${uploadId}/status`),
  completeUpload: (uploadId: string) => client.post(`/upload/${uploadId}/complete`),
  createFile: (data: any) => client.post('/files', data),
  listFiles: (page = 1, size = 20) => client.get('/files', { params: { page, size } }),
  getFile: (id: number) => client.get(`/files/${id}`),
  deleteFile: (id: number) => client.delete(`/files/${id}`),
  downloadUrl: (id: number) => `/api/v1/files/${id}/download`,
}
```

`frontend/src/api/transfers.ts`: send, inbox, sent, remove
`frontend/src/api/friends.ts`: add, accept, list, remove
`frontend/src/api/users.ts`: getProfile, updateProfile, search
`frontend/src/api/admin.ts`: listUsers, updateQuota, updateStatus, updateRole, getConfig, updateConfig, poolList, reviewList, reviewFile

- [ ] **Step 2: 工具函数**

`frontend/src/utils/format.ts`:
```typescript
export function formatFileSize(bytes: number): string {
  if (bytes === 0) return '0 B'
  const units = ['B', 'KB', 'MB', 'GB', 'TB']
  const i = Math.floor(Math.log(bytes) / Math.log(1024))
  return (bytes / Math.pow(1024, i)).toFixed(i > 0 ? 1 : 0) + ' ' + units[i]
}

export function formatDate(date: string | null): string {
  if (!date) return '永久'
  return new Date(date).toLocaleDateString('zh-CN')
}

export function maskEmail(email: string, isFriend: boolean): string {
  if (isFriend) return email
  const [name, domain] = email.split('@')
  return name.substring(0, 3) + '***@' + domain
}
```

`frontend/src/utils/validators.ts`:
```typescript
export function isValidEmail(email: string): boolean {
  return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)
}

export function isValidPassword(password: string): boolean {
  return password.length >= 6 && password.length <= 50
}
```

- [ ] **Step 3: Commit**

```bash
git add frontend/src/api/ frontend/src/utils/
git commit -m "feat: 前端 API 层 + 工具函数"
```

---

### Task 11: 前端布局组件 + 登录注册页

**Files:** Create: `AppLayout.vue`, `Sidebar.vue`, `Login.vue`, `Register.vue`, `VerifyEmail.vue`

- [ ] **Step 1: AppLayout.vue**

```vue
<template>
  <div class="app-layout">
    <Sidebar />
    <main class="main-content">
      <router-view />
    </main>
  </div>
</template>

<script setup lang="ts">
import Sidebar from './Sidebar.vue'
</script>

<style scoped>
.app-layout {
  display: flex;
  min-height: 100vh;
  background: #fefaf2;
  font-family: 'Georgia', 'Noto Serif SC', serif;
}
.main-content {
  flex: 1;
  padding: 24px;
  overflow-y: auto;
}
</style>
```

- [ ] **Step 2: Sidebar.vue**

侧边栏组件，包含：
- Logo 区域（"🍃 MyWeb"）
- 菜单项根据 `authStore.role` 动态显示
- 当前选中项高亮（暖棕色 #b87b3a）
- 底部用户信息和退出按钮

菜单项映射：
- 所有用户：仪表盘(/) / 我的文件(/files) / 收件箱(/inbox) / 共享池(/pool) / 好友(/friends) / 设置(/profile)
- 管理员额外：审核(/admin/review) / 用户管理(/admin/users)
- 超管额外：系统设置(/super-admin/system)

- [ ] **Step 3: Login.vue**

登录页：邮箱输入框 + 密码输入框 + 登录按钮 + 跳转注册链接
- 样式：温暖色调，居中卡片布局，圆角柔和
- 调用 `authStore.login()`，成功后跳转 `redirect` 参数或首页

- [ ] **Step 4: Register.vue**

注册页：邮箱 + 验证码（带"发送验证码"按钮 + 倒计时60s） + 密码 + 确认密码 + 昵称 + 注册按钮
- 样式与登录页一致的温暖风格
- 注册成功后自动登录并跳转

- [ ] **Step 5: VerifyEmail.vue**

邮箱验证成功/失败结果展示页（从邮件链接跳转）

- [ ] **Step 6: 更新路由使用 AppLayout**

更新 `router/index.ts`，所有需认证路由包裹在 AppLayout 中：

```typescript
{
  path: '/',
  component: () => import('@/components/AppLayout.vue'),
  meta: { requiresAuth: true },
  children: [
    { path: '', name: 'Dashboard', component: () => import('@/views/Dashboard.vue') },
    { path: 'files', name: 'MyFiles', component: () => import('@/views/MyFiles.vue') },
    // ... 其他页面
  ]
}
```

- [ ] **Step 7: Commit**

```bash
git add frontend/src/components/AppLayout.vue frontend/src/components/Sidebar.vue frontend/src/views/Login.vue frontend/src/views/Register.vue frontend/src/router/
git commit -m "feat: 布局组件 + 登录/注册页面"
```

---

### Task 12: 前端文件上传 + 文件列表 + 收件箱 + 共享池页面

**Files:** Create: `Upload.vue`, `FileUploader.vue`, `MyFiles.vue`, `FileCard.vue`, `Inbox.vue`, `Pool.vue`

- [ ] **Step 1: FileUploader.vue（分片上传组件）**

核心逻辑：
```typescript
async function startUpload(file: File, title: string, description: string,
                           isSharedPool: boolean, expireAt: string | null) {
  // 1. 初始化
  const { uploadId, totalChunks } = await initUpload({
    originalName: file.name, fileSize: file.size
  })

  // 2. 分片上传（并发 3 片）
  const chunkSize = 5 * 1024 * 1024
  const chunks: { index: number; blob: Blob }[] = []
  for (let i = 0; i < totalChunks; i++) {
    chunks.push({
      index: i,
      blob: file.slice(i * chunkSize, (i + 1) * chunkSize)
    })
  }

  // 并发上传
  const CONCURRENCY = 3
  for (let i = 0; i < chunks.length; i += CONCURRENCY) {
    const batch = chunks.slice(i, i + CONCURRENCY)
    await Promise.all(batch.map(c =>
      uploadChunk(uploadId, c.index, c.blob).then(() => {
        progress.value = Math.round((completedCount / totalChunks) * 100)
      })
    ))
  }

  // 3. 完成合并
  const { storagePath, originalName, fileSize } = await completeUpload(uploadId)

  // 4. 创建文件记录
  await filesApi.createFile({
    title, description, isSharedPool, expireAt,
    storagePath, originalName, fileSize
  })
}
```

组件模板包含：文件拖拽区、标题输入框、描述输入框、共享池/个人空间切换、过期时间选择、上传进度条。

- [ ] **Step 2: Upload.vue**

使用 FileUploader 组件，添加页面标题和返回按钮。

- [ ] **Step 3: FileCard.vue**

文件卡片组件，展示：标题、描述摘要、文件大小、上传日期、过期时间、审核状态标签。操作按钮：下载 / 发送 / 删除。

- [ ] **Step 4: MyFiles.vue**

文件列表页：分页加载 `filesApi.listFiles()`，使用 FileCard 渲染，空状态提示。

- [ ] **Step 5: Inbox.vue**

收件箱列表：分页加载 `transfersApi.inbox()`，展示发送者、文件名、发送时间、留言。点击下载。

- [ ] **Step 6: Pool.vue**

共享池列表：分页加载 `adminApi.poolList()`，展示上传者、标题、描述、文件大小、上传日期、过期时间。支持按类型筛选。

- [ ] **Step 7: Commit**

```bash
git add frontend/src/views/Upload.vue frontend/src/components/FileUploader.vue frontend/src/views/MyFiles.vue frontend/src/components/FileCard.vue frontend/src/views/Inbox.vue frontend/src/views/Pool.vue
git commit -m "feat: 文件上传 + 文件列表 + 收件箱 + 共享池页面"
```

---

### Task 13: 前端好友 + 用户搜索 + 管理后台页面

**Files:** Create: `Friends.vue`, `UserSearch.vue`, `ConfirmDialog.vue`, `Dashboard.vue`, `Profile.vue`, `admin/Review.vue`, `admin/Users.vue`, `super-admin/System.vue`

- [ ] **Step 1: Friends.vue**

好友列表 + 添加好友（搜索用户 → 发送请求） + 待处理请求（接受/拒绝）
- 搜索使用 UserSearch 组件
- 列表展示好友昵称、邮箱（好友可见完整邮箱）、添加时间

- [ ] **Step 2: UserSearch.vue**

弹窗组件：输入框搜索邮箱 → 展示匹配结果列表 → 点击选择
- 防抖 300ms
- 非好友邮箱脱敏显示
- 发送好友请求或直接发送文件

- [ ] **Step 3: Dashboard.vue**

首页仪表盘：
- 显示用户信息卡片（昵称、角色、空间使用量/配额）
- 快捷操作入口（上传文件、查看收件箱、浏览共享池）
- 最近文件列表（最多 5 条）

- [ ] **Step 4: Profile.vue**

个人设置页：修改昵称、头像、密码。

- [ ] **Step 5: admin/Review.vue**

共享池审核页：待审核文件列表，每条有"通过"/"拒绝"按钮，拒绝时填写理由。

- [ ] **Step 6: admin/Users.vue**

用户管理页：用户列表（分页），操作列：调整配额（弹窗输入）、启用/禁用切换。

- [ ] **Step 7: super-admin/System.vue**

系统设置页：全局容量设置、默认配额设置、每日注册上限设置、任命/撤销管理员。

- [ ] **Step 8: Commit**

```bash
git add frontend/src/views/
git commit -m "feat: 好友 + 用户搜索 + 仪表盘 + 管理后台页面"
```

---

### Task 14: Nginx 配置 + 集成测试

**Files:** Create: `nginx/myweb.conf`

- [ ] **Step 1: Nginx 配置**

```nginx
server {
    listen 80;
    server_name _;

    # 静态文件
    location / {
        root /usr/share/nginx/html;
        index index.html;
        try_files $uri $uri/ /index.html;
    }

    # API 代理
    location /api/ {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_request_buffering off;
        client_max_body_size 0;
    }

    # 静态资源缓存
    location ~* \.(js|css|png|jpg|svg|ico|woff2?)$ {
        root /usr/share/nginx/html;
        expires 7d;
        add_header Cache-Control "public, immutable";
    }

    # 注册接口限流
    limit_req_zone $binary_remote_addr zone=register:10m rate=10r/m;

    location /api/v1/auth/register {
        limit_req zone=register burst=5 nodelay;
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    }
}
```

- [ ] **Step 2: 验证完整流程**

```bash
# 1. 启动后端
cd backend && mvn spring-boot:run

# 2. 构建前端
cd frontend && npm run build

# 3. 复制到 Nginx 目录
cp -r dist/* /usr/share/nginx/html/

# 4. 配置 Nginx
cp nginx/myweb.conf /etc/nginx/sites-enabled/

# 5. 重载 Nginx
nginx -s reload

# 6. 测试 API
curl http://localhost/api/v1/auth/login -H "Content-Type: application/json" \
  -d '{"email":"admin@myweb.local","password":"admin123"}'
```

- [ ] **Step 3: Commit**

```bash
git add nginx/
git commit -m "feat: Nginx 配置 + 部署验证"
```

---

## 实现顺序建议

任务按编号顺序执行，依赖关系如下：

```
Task 1 (后端初始化)
  └→ Task 3 (实体类 + Mapper)
       └→ Task 4 (JWT + Security)
            └→ Task 5 (认证服务)
                 └→ Task 6 (文件上传)
                      └→ Task 7 (文件管理)
                           ├→ Task 8 (互传 + 好友)
                           └→ Task 8.5 (用户管理 + 审核 + 配置)
                                └→ Task 9 (清理 + 修正)

Task 2 (前端初始化)
  └→ Task 10 (前端 API 层)
       └→ Task 11 (布局 + 登录注册)
            └→ Task 12 (文件/上传/共享池)
                 └→ Task 13 (好友/管理后台)

Task 14 (Nginx + 集成) ← 前后端均完成后
```
