# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 回答规范

1. 使用中文回答
2. 称呼我为猫猫
3. 每一句后面要带"喵~"

## 项目概述

MyWeb — 个人文件共享网站，Vue3 + Element Plus + Spring Boot，部署于云服务器 + 本地虚拟机（NPS 穿透做文件存储）。

**GitHub:** https://github.com/36545920/myweb.git

## 设计文档

| 文档 | 路径 |
|------|------|
| V1 设计 | `docs/superpowers/specs/2026-06-15-myweb-phase1-design.md` |
| V2 重构设计（Element Plus 网盘式） | `docs/superpowers/specs/2026-06-15-myweb-v2-redesign.md` |
| V1 实现计划 | `docs/superpowers/plans/2026-06-15-myweb-phase1-plan.md` |
| V2 实现计划 | `docs/superpowers/plans/2026-06-15-myweb-v2-plan.md` |

## 当前进度

**Phase 1 + V2 重构完成**，全部代码在 `main` 分支，前后端编译通过。

### 已完成功能

- 邮箱注册登录（QQ SMTP 验证码）+ JWT 双 Token 认证
- 三级权限（超管/管理员/普通用户，继承式）
- 用户管理（搜索/配额调整/启用禁用/密码重置）
- 头像上传（即时预览 + 默认头像）
- 网盘式文件管理（el-table / 分片上传 / 断点续传 / 右键菜单 / 批量操作 / 筛选排序 / 重命名）
- 文件互传（直接发送 / 收件箱 / 过期自动清理）
- 文件共享池（审核 / 容量管理）
- 好友系统（搜索邮箱添加 / 接受 / 删除）
- NAS 健康检查（每 30 秒检测，离线时页面警告横幅）
- 独立文件服务器模块（`file-server/`，部署在本地虚拟机）

### 未开始

- Phase 2：在线文件预览、压缩包预览、加急邮件通知
- Phase 3：NPS 在线状态自动检测、共享池自动关闭

## 技术栈

| 层 | 技术 |
|----|------|
| 前端 | Vue 3 + Vite + Element Plus + Pinia + Axios |
| 后端 | Spring Boot 3 + MyBatis-Plus + MySQL 8.0 |
| 文件存储 | 独立 Spring Boot 文件服务（部署在本地虚拟机，HTTP 通信） |
| 代理 | Nginx（静态文件 + API 代理 + 注册限流） |
| 穿透 | NPS（云服务器服务端 ↔ 本地虚拟机客户端） |
| 邮件 | QQ SMTP |
| 认证 | JWT（access_token 24h + refresh_token 7d） |

## 项目结构

```
myweb/
├── backend/                    # Spring Boot 主程序（:10888）
│   └── src/main/java/com/myweb/
│       ├── config/             # SecurityConfig / CorsConfig / MyBatisPlusConfig
│       ├── controller/         # Auth / File / Upload / Transfer / Friend / User / Admin / Health
│       ├── service/            # 业务逻辑
│       ├── repository/         # MyBatis Mapper
│       ├── model/entity/       # User / FileEntity / FileTransfer / Friend / SystemConfig
│       ├── filter/             # JwtAuthFilter
│       └── util/               # JwtUtil / FileUtil(HTTP远程调用) / EmailUtil
├── frontend/                   # Vue3 前端（:5173）
│   └── src/
│       ├── api/                # auth / files / transfers / friends / users / admin
│       ├── views/              # 15 个页面组件
│       ├── components/         # AppLayout / Sidebar / AvatarUpload
│       ├── stores/             # auth / files / pool
│       └── router/             # 路由（含 beforeEnter 鉴权守卫）
├── file-server/                # 文件存储服务（:18080，部署在本地虚拟机）
│   └── controller/             # FileStorageController（上传/下载/删除/分片/合并/健康检查）
├── nginx/                      # Nginx 配置
├── deploy/                     # 部署配置模板
└── docs/                       # 设计文档 + 实现计划
```

## 关键架构决策

- **邮箱即用户主键**：`users.email` VARCHAR PK，所有关联表用 email 引用，逻辑外键（无物理 FK）
- **权限继承**：SUPER_ADMIN ⊇ ADMIN ⊇ USER，代码用 `role >=` 判断
- **前后端分离部署**：云服务器跑 Nginx + 主程序 + MySQL，本地虚拟机跑文件服务
- **文件操作通过 HTTP**：云端 FileUtil 调用虚拟机 `/file/*` 接口，X-Auth-Token 认证
- **分片上传**：5MB/片，并发 3 片，UploadService 管理会话（ConcurrentHashMap）
- **Element Plus 主题色**：`:root` CSS 变量覆盖为温暖棕 `#b87b3a`
- **侧边栏保留自定义**：不用 el-menu，保留手写温暖风格 Sidebar.vue
- **JWT + BCrypt**：无状态认证，密码哈希存储

## 常用命令

```bash
# 后端编译
cd backend && JAVA_HOME="C:/Program Files/Java/jdk-17.0.12" mvn compile

# 后端运行
cd backend && JAVA_HOME="C:/Program Files/Java/jdk-17.0.12" mvn spring-boot:run

# 数据库 schema 初始化
mysql -u root -p123456 myweb < backend/src/main/resources/db/schema.sql

# 前端安装依赖
cd frontend && npm install

# 前端开发
cd frontend && npm run dev

# 前端构建
cd frontend && npm run build

# 文件服务器编译
cd file-server && JAVA_HOME="C:/Program Files/Java/jdk-17.0.12" mvn compile

# Git
git -C /c/Users/jiangyeyue/Desktop/myweb <command>
```

## 数据库

- **Host:** 127.0.0.1:3306
- **数据库:** myweb
- **用户:** root
- **开发环境密码:** 123456

### 核心表

| 表 | 说明 |
|----|------|
| users | 用户（email PK，含角色/配额/头像） |
| files | 文件（含 file_type/审核状态/过期时间/最近访问） |
| file_transfers | 传输记录 |
| friends | 好友关系 |
| system_config | 全局配置（容量/注册限制） |

### 默认管理员

- 邮箱：admin@myweb.local
- 密码：admin123

## 注意事项

- FileUtil 已改为 HTTP 远程调用文件服务器，本地开发时如果文件服务器未启动，上传/下载会失败
- 健康检查 `/health` 返回 `nas: true/false`，前端每 30 秒检测一次
- 前端代理配置 `vite.config.ts` 中 `/api` → `http://127.0.0.1:10888`，后端改端口需同步
- `deploy/` 目录下有部署配置模板，需要填写的占位符用【】标出
