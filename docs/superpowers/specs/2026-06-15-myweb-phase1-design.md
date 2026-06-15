# MyWeb Phase 1 设计文档

> 用户认证 + 基础文件互传 + 个人空间管理  
> 日期：2026-06-15 | 版本：v1.0

---

## 1. 项目概述

MyWeb 是一个个人文件共享网站，采用 Vue3 + Spring Boot 技术栈，部署在 2核2G 虚拟机，通过 NPS 内网穿透对外提供服务。本文档描述 Phase 1 的完整设计：用户认证系统、文件互传、个人空间管理和文件共享池基础功能。

### 1.1 核心目标

- 邮箱注册登录，每日注册有限制
- 三级用户权限（超级管理员 1 人、管理员若干、普通用户）
- 用户间直接发送文件，支持断点续传
- 个人空间与文件共享池分离管理
- 全局容量由超级管理员统一调配

### 1.2 后续阶段预览

| 阶段 | 内容 |
|------|------|
| Phase 2 | 在线文件预览、压缩包预览、审核流程完善（加急邮件通知） |
| Phase 3 | NPS 在线状态检测、共享池状态自动切换 |

---

## 2. 技术栈

| 层 | 技术 | 说明 |
|----|------|------|
| 前端 | Vue 3 + Vite | 自定义设计，温暖/自然风格 |
| 后端 | Spring Boot 3 | RESTful API，JVM 512MB |
| 数据库 | MySQL 8.0 | 逻辑外键，无物理 FK |
| 文件存储 | NAS 挂载 | `/mnt/nas/myweb-files/` |
| 反向代理 | Nginx | 静态文件 + API 代理 + 限流 |
| 邮件 | QQ/163 SMTP | 验证码发送，每日 50-100 封 |
| 穿透 | NPS | 内网穿透对外暴露 |

---

## 3. 部署架构

```
用户浏览器
    ↓ HTTPS (NPS 穿透)
Nginx :80/:443
    ├── /          → Vue3 静态文件 (/usr/share/nginx/html)
    └── /api/*     → proxy_pass Spring Boot :8080

Spring Boot :8080   (-Xmx512m -Xms256m)
    ├── MySQL :3306  (innodb_buffer_pool_size=256M, 连接池 5-10)
    └── NAS 挂载     (/mnt/nas/myweb-files/)
```

### 3.1 资源分配（2核2G）

| 进程 | 内存预算 | 关键优化 |
|------|----------|----------|
| Nginx | ~50MB | worker_processes 1; worker_connections 512 |
| Spring Boot | ~512MB | 分片流式写盘，大文件不读入 JVM 堆 |
| MySQL | ~400MB | 逻辑外键、skip-name-resolve、关闭 binlog |
| 系统预留 | ~1GB | OS + 文件缓存 + 突发余量 |

---

## 4. 数据模型

### 4.1 users 表（邮箱为主键）

| 字段 | 类型 | 说明 |
|------|------|------|
| **email** | **VARCHAR(255) PK** | **邮箱即主键，登录账号** |
| password_hash | VARCHAR(255) | BCrypt 加密 |
| nickname | VARCHAR(100) | 显示昵称 |
| avatar | VARCHAR(500) | 头像 URL（可选） |
| role | ENUM('USER','ADMIN','SUPER_ADMIN') | 权限角色（继承式：SUPER_ADMIN ⊇ ADMIN ⊇ USER） |
| storage_quota | BIGINT | 个人配额（字节），默认 10GB，管理员可调 |
| storage_used | BIGINT | 已用空间（字节） |
| status | VARCHAR(20) | ACTIVE / DISABLED |
| email_verified | TINYINT(1) | 邮箱是否已验证 |
| created_at | DATETIME | 注册时间 |

### 4.2 files 表

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK AUTO_INCREMENT | 文件 ID |
| owner_email | VARCHAR(255) | 上传者（逻辑外键 → users.email），索引 |
| title | VARCHAR(200) NOT NULL | 文件标题 |
| description | VARCHAR(2000) | 文件描述 |
| original_name | VARCHAR(500) | 原始文件名 |
| is_shared_pool | TINYINT(1) | 0=个人空间，1=共享池 |
| review_status | VARCHAR(20) | PENDING / APPROVED / REJECTED（个人空间文件直接 APPROVED） |
| review_comment | VARCHAR(500) | 审核意见 |
| reviewed_by | VARCHAR(255) | 审核人邮箱 |
| file_size | BIGINT | 字节数 |
| mime_type | VARCHAR(100) | 文件 MIME 类型 |
| storage_path | VARCHAR(500) | NAS 实际存储路径 |
| expire_at | DATETIME | 过期时间（NULL = 永久） |
| created_at | DATETIME | 上传日期 |
| updated_at | DATETIME | 最后更新时间 |

### 4.3 file_transfers 表

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK AUTO_INCREMENT | 传输记录 ID |
| file_id | BIGINT | 逻辑外键 → files.id |
| from_email | VARCHAR(255) | 发送者邮箱 |
| to_email | VARCHAR(255) | 接收者邮箱 |
| status | VARCHAR(20) | SENT / RECEIVED / DOWNLOADED |
| message | VARCHAR(500) | 附带留言 |
| created_at | DATETIME | 发送时间 |
| downloaded_at | DATETIME | 下载时间 |

### 4.4 friends 表

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK AUTO_INCREMENT | 好友关系 ID |
| user_email | VARCHAR(255) | 用户邮箱 |
| friend_email | VARCHAR(255) | 好友邮箱 |
| status | VARCHAR(20) | PENDING / ACCEPTED |
| created_at | DATETIME | 请求时间 |
| accepted_at | DATETIME | 接受时间 |

唯一约束：`UNIQUE(user_email, friend_email)`

### 4.5 system_config 表

| 字段 | 类型 | 说明 | 默认值 |
|------|------|------|--------|
| config_key | VARCHAR(50) PK | 配置键 | — |
| config_value | VARCHAR(500) | 配置值 | — |
| updated_by | VARCHAR(255) | 修改者邮箱 | — |
| updated_at | DATETIME | 修改时间 | — |

预置配置项：

| config_key | 默认值 | 说明 |
|------------|--------|------|
| total_user_quota | 53687091200 (50GB) | 用户空间总量上限 |
| total_pool_quota | 32212254720 (30GB) | 共享池容量上限 |
| default_user_quota | 10737418240 (10GB) | 新用户默认配额 |
| daily_register_limit | 50 | 每日注册上限 |
| today_register_count | 0 | 今日已注册数 |
| register_date | — | 计数日期 |

### 4.6 逻辑外键策略

- 不使用 MySQL 物理 FOREIGN KEY 约束
- 所有关联列建立普通索引
- 级联删除在 Service 层 `@Transactional` 中手动处理：
  - 删除用户 → 删除其所有文件（含 NAS 实体）→ 删除传输记录 → 删除好友关系
  - 删除文件 → 删除 NAS 文件实体 → 删除关联传输记录

---

## 5. 权限模型

### 5.1 继承式角色

```
SUPER_ADMIN  ⊇  ADMIN  ⊇  USER
（超级管理员拥有管理员和普通用户的所有权限）
（管理员拥有普通用户的所有权限）
```

### 5.2 权限矩阵（代码用 `role >=` 判断）

| 操作 | USER | ADMIN | SUPER_ADMIN |
|------|:----:|:-----:|:-----------:|
| 登录/注册 | ✅ | ✅ | ✅ |
| 上传文件到个人空间 | ✅ | ✅ | ✅ |
| 上传文件到共享池 | ✅ | ✅ | ✅ |
| 发送文件给其他用户 | ✅ | ✅ | ✅ |
| 接收/下载文件 | ✅ | ✅ | ✅ |
| 删除自己的文件 | ✅ | ✅ | ✅ |
| 管理好友 | ✅ | ✅ | ✅ |
| 搜索用户 | ✅ | ✅ | ✅ |
| 浏览共享池 | ✅ | ✅ | ✅ |
| 审核共享池文件 | ❌ | ✅ | ✅ |
| 管理用户（配额/状态） | ❌ | ✅ | ✅ |
| 设置注册限制 | ❌ | ✅ | ✅ |
| 任命/撤销管理员 | ❌ | ❌ | ✅ |
| 设定全局容量 | ❌ | ❌ | ✅ |

---

## 6. API 路由设计

所有 API 前缀：`/api/v1/`，认证方式：JWT Bearer Token（Header: `Authorization: Bearer <token>`）

### 6.1 认证模块

| Method | Path | 说明 | 角色 |
|--------|------|------|------|
| POST | /auth/register | 注册（含邮箱验证码发送） | 公开 |
| POST | /auth/verify-email | 验证邮箱（验证码 + 邮箱） | 公开 |
| POST | /auth/login | 登录，返回 access_token + refresh_token | 公开 |
| POST | /auth/refresh | 刷新 access_token | 公开 |

### 6.2 文件管理模块

| Method | Path | 说明 | 角色 |
|--------|------|------|------|
| POST | /upload/init | 初始化分片上传 | USER+ |
| POST | /upload/{uploadId}/chunk/{index} | 上传第 index 个分片 | USER+ |
| GET | /upload/{uploadId}/status | 查询断点（返回已上传分片列表） | USER+ |
| POST | /upload/{uploadId}/complete | 合并分片，创建文件记录 | USER+ |
| GET | /files | 我的文件列表（分页，pageSize≤20） | USER+ |
| GET | /files/{id} | 文件详情（含元数据） | USER+ |
| DELETE | /files/{id} | 删除文件（含 NAS 实体） | OWNER |
| GET | /files/{id}/download | 下载文件（流式传输） | OWNER/接收者 |

### 6.3 文件互传模块

| Method | Path | 说明 | 角色 |
|--------|------|------|------|
| POST | /transfers | 发送文件给指定用户 | USER+ |
| GET | /transfers/inbox | 收件箱列表 | USER+ |
| GET | /transfers/sent | 已发送列表 | USER+ |
| DELETE | /transfers/{id} | 删除传输记录 | 参与者 |

### 6.4 好友系统模块

| Method | Path | 说明 | 角色 |
|--------|------|------|------|
| POST | /friends | 发送好友请求 | USER+ |
| PUT | /friends/{id} | 接受/拒绝请求 | USER+ |
| GET | /friends | 好友列表 | USER+ |
| DELETE | /friends/{email} | 删除好友 | USER+ |

### 6.5 用户管理模块

| Method | Path | 说明 | 角色 |
|--------|------|------|------|
| GET | /users/me | 当前用户信息 | USER+ |
| PUT | /users/me | 修改个人信息（昵称/头像/密码） | USER+ |
| GET | /users/search | 按邮箱搜索用户（前缀匹配） | USER+ |
| GET | /admin/users | 用户列表（管理视图） | ADMIN+ |
| PUT | /admin/users/{email}/quota | 调整用户配额 | ADMIN+ |
| PUT | /admin/users/{email}/status | 启用/禁用用户 | ADMIN+ |
| PUT | /super-admin/users/{email}/role | 设置用户角色 | SUPER_ADMIN |

### 6.6 共享池模块

| Method | Path | 说明 | 角色 |
|--------|------|------|------|
| GET | /pool | 共享池文件列表（仅 APPROVED） | USER+ |
| GET | /pool/{id} | 共享池文件详情 | USER+ |
| GET | /admin/review | 待审核文件列表 | ADMIN+ |
| PUT | /admin/review/{id} | 审核文件（APPROVED/REJECTED） | ADMIN+ |

### 6.7 系统配置模块

| Method | Path | 说明 | 角色 |
|--------|------|------|------|
| GET | /admin/config | 获取系统配置 | ADMIN+ |
| PUT | /admin/config | 更新系统配置 | SUPER_ADMIN |

---

## 7. 前端路由 & 页面结构

### 7.1 页面树

```
🔓 公开（未登录）
  /login            — 登录页
  /register          — 注册页
  /verify-email      — 邮箱验证回调

🔒 所有用户（USER+）
  /                   — 首页 / 仪表盘
  /files              — 我的文件（个人空间）
  /files/upload       — 上传文件
  /inbox              — 收件箱
  /pool               — 文件共享池（浏览已审核文件）
  /friends            — 好友列表
  /friends/add        — 添加好友
  /profile            — 个人设置

🛡 管理员（ADMIN+，含上述所有）
  /admin/review       — 共享池审核
  /admin/users        — 用户管理
  /admin/register-limit — 注册限制设置

👑 超级管理员（SUPER_ADMIN，含上述所有）
  /super-admin/admins  — 管理员任命/撤销
  /super-admin/system  — 系统设置（全局容量）
```

### 7.2 导航布局

- 侧边栏（180px）+ 主内容区
- 侧边栏菜单根据角色动态显示
- 温暖/自然风格：暖色调、柔和圆角、亲和配色

---

## 8. 核心业务流程

### 8.1 用户注册

```
用户提交注册表单（邮箱 + 密码 + 昵称）
  → Nginx 限流检查（10 req/min）
  → 后端校验每日注册上限（today_register_count < daily_register_limit）
  → 发送邮箱验证码（QQ SMTP）
  → 用户输入验证码
  → 验证通过 → 计算配额分配：
    ├─ 剩余总量 ≥ default_user_quota → 正常分配
    ├─ 0 < 剩余 < default_user_quota → 分配剩余，提示不足
    └─ 剩余 ≤ 0 → 配额=0，提示仅可上传共享池
  → 创建用户 → today_register_count + 1
  → 返回 JWT token
```

### 8.2 用户登录

```
用户提交（邮箱 + 密码）
  → BCrypt 验证
  → 检查用户状态（ACTIVE）
  → 生成 access_token（24h）+ refresh_token（7d）
  → 返回 tokens + 用户基本信息
```

### 8.3 文件上传（断点续传）

```
┌─ 初始化 ────────────────────────────────────┐
│ POST /upload/init                            │
│ {title, description, fileSize,              │
│  isSharedPool, expireAt}                     │
│ → 容量检查（个人空间 or 共享池）                │
│ → 创建 uploadId，记录到 ConcurrentHashMap        │
│ ← {uploadId, chunkSize: 5MB, chunkCount}     │
└──────────────────────────────────────────────┘
                    ↓
┌─ 分片上传（前端并发 3 片）─────────────────┐
│ POST /upload/{uploadId}/chunk/{index}        │
│ → 流式写入 NAS 临时目录                       │
│ → 记录已上传分片                              │
│ ← {index, status: OK}                        │
└──────────────────────────────────────────────┘
                    ↓
┌─ 断点续传 ─────────────────────────────────┐
│ GET /upload/{uploadId}/status                │
│ ← {uploadedChunks: [0,1,3], missing: [2]}   │
│ → 仅上传缺失分片                              │
└──────────────────────────────────────────────┘
                    ↓
┌─ 完成合并 ─────────────────────────────────┐
│ POST /upload/{uploadId}/complete             │
│ → 校验所有分片完整性（MD5 校验）               │
│ → 合并为完整文件，移至正式目录                 │
│ → 创建 files 表记录                          │
│ → 更新用户 storage_used                      │
│ ← {fileId, url}                              │
└──────────────────────────────────────────────┘

> **注意**：上传会话存储在内存（ConcurrentHashMap），服务器重启后会话丢失，需重新上传。临时分片文件在 `/tmp/myweb-uploads/{uploadId}/` 目录下，重启后需手动或定时清理未完成的上传临时文件。
```

### 8.4 文件发送流程

```
用户 A 在文件列表点击"发送"
  → 选择接收者（从好友列表选 / 搜索用户邮箱）
  → 可附带留言
  → POST /transfers {fileId, toEmail, message}
  → 创建 file_transfers 记录
  → 用户 B 在收件箱看到新文件
  → 用户 B 点击下载 → GET /files/{id}/download
  → 更新 downloaded_at
```

### 8.5 文件过期清理

```
@Scheduled(cron = "0 0 3 * * ?")  // 每天凌晨 3:00
  → 查询 expire_at < NOW() 的所有文件
  → 逐条处理：
    1. 删除 NAS 文件实体
    2. 删除 file_transfers 关联记录
    3. 删除 files 表记录
    4. 更新 users.storage_used
```

---

## 9. 容量管理细则

### 9.1 两个容量池

| 容量池 | 管理对象 | 设置者 | 默认值 |
|--------|----------|--------|--------|
| 用户空间总量 | 所有用户个人空间之和 | 超级管理员 | 50 GB |
| 共享池容量 | 共享池所有文件之和 | 超级管理员 | 30 GB |

### 9.2 错误提示场景

| 场景 | HTTP 状态码 | 提示信息 |
|------|------------|----------|
| 个人空间不足 | 413 | "个人空间不足（已用 X / 配额 Y），请清理文件或联系管理员扩容" |
| 共享池已满 | 413 | "文件共享池已满，请联系管理员清理无用文件" |
| 注册时配额为 0 | 200（成功但警告） | "本站用户个人空间已耗尽，新用户仅可上传至文件共享池" |
| 共享池关闭（VM 离线） | 503 | "文件共享池暂不可用，虚拟机离线中" |

---

## 10. 安全设计

| 层面 | 措施 |
|------|------|
| 密码 | BCrypt 加密存储 |
| 认证 | JWT access_token（24h）+ refresh_token（7d） |
| 传输 | HTTPS（Nginx 终止 TLS） |
| 限流 | Nginx limit_req_zone（注册 10r/m）+ 应用层每日注册上限 |
| 文件访问 | 仅文件所有者 + 指定接收者可下载 |
| 邮箱验证 | 注册必须验证邮箱，验证码 5 分钟有效期 |
| 邮箱脱敏 | 搜索结果中非好友用户邮箱脱敏显示（john***@gmail.com） |

---

## 11. 错误处理规范

统一响应格式：

```json
{
  "code": 0,
  "message": "success",
  "data": {}
}
```

| code | 含义 |
|------|------|
| 0 | 成功 |
| 1001 | 参数校验失败 |
| 1002 | 未登录 / Token 过期 |
| 1003 | 权限不足 |
| 1004 | 资源不存在 |
| 1005 | 业务规则限制（容量已满、注册超限等） |
| 2001 | 文件上传失败 |
| 2002 | 审核相关错误 |
| 5000 | 服务器内部错误 |

---

## 12. Nginx 关键配置

```nginx
# 静态文件
location / {
    root /usr/share/nginx/html;
    index index.html;
    try_files $uri $uri/ /index.html;  # Vue History 模式
}

# API 代理
location /api/ {
    proxy_pass http://127.0.0.1:8088;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_request_buffering off;          # 分片上传不缓存
    client_max_body_size 0;               # 不限制上传大小
}

# 注册限流
limit_req_zone $binary_remote_addr zone=register:10m rate=10r/m;
location /api/v1/auth/register {
    limit_req zone=register burst=5 nodelay;
    proxy_pass http://127.0.0.1:8088;
}
```

---

## 13. 项目结构

```
myweb/
├── docs/superpowers/specs/          # 设计文档
├── frontend/                        # Vue3 前端
│   ├── src/
│   │   ├── views/                   # 页面组件
│   │   ├── components/              # 通用组件
│   │   ├── router/                  # 路由配置
│   │   ├── stores/                  # Pinia 状态管理
│   │   ├── api/                     # API 请求封装
│   │   └── utils/                   # 工具函数
│   └── ...
├── backend/                         # Spring Boot 后端
│   ├── src/main/java/
│   │   ├── config/                  # Security, CORS, Scheduled
│   │   ├── controller/              # REST Controller
│   │   ├── service/                 # Business Logic
│   │   ├── repository/              # MyBatis / JPA Mapper
│   │   ├── model/                   # Entity, DTO, VO
│   │   ├── filter/                  # JWT Filter
│   │   └── util/                    # 工具类
│   └── src/main/resources/
│       └── application.yml
└── nginx/
    └── myweb.conf                   # Nginx 配置
```

---

## 14. 设计决策记录

| 决策 | 选项 | 理由 |
|------|------|------|
| 通信方式 | RESTful | NPS 穿透下最稳定，个人项目够用 |
| 组件库 | 自定义设计 | 用户偏好独特视觉风格 |
| 数据库主键 | email VARCHAR PK | 用户搜索高效，标识统一 |
| 外键约束 | 逻辑外键 | 写入性能好，2核2G 下更轻量 |
| 认证方式 | JWT 双 Token | 无状态，适合 REST |
| 分片大小 | 5MB | 平衡网络抖动和请求频率 |
| 用户配额 | 继承式角色 | 管理员也需使用普通功能 |
