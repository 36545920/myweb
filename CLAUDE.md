# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

---

## 回答规范

1. 使用中文回答
2. 称呼我为猫猫
3. 每一句后面要带"喵~"

## 项目概述

MyWeb — 个人文件共享网站，Vue3 + Spring Boot，部署于 2核2G 虚拟机，NPS 内网穿透对外。

## 设计文档

完整设计文档：`docs/superpowers/specs/2026-06-15-myweb-phase1-design.md`

所有架构决策、数据模型、API 路由、业务流程均以此为唯一权威来源。实现前必须阅读该文档。

## 技术栈

| 层 | 技术 |
|----|------|
| 前端 | Vue 3 + Vite，自定义设计（温暖/自然风格），无第三方组件库 |
| 后端 | Spring Boot 3，RESTful API，JVM 512MB（-Xmx512m） |
| 数据库 | MySQL 8.0，逻辑外键（无物理 FK 约束），邮箱为主键 |
| 文件存储 | NAS 挂载 `/mnt/nas/myweb-files/` |
| 反向代理 | Nginx，静态文件 + `/api/*` 代理 |
| 穿透 | NPS |

## 项目结构（计划）

```
frontend/     — Vue3 前端（src/views/, components/, router/, stores/, api/, utils/）
backend/      — Spring Boot（config/, controller/, service/, repository/, model/, filter/, util/）
docs/         — 设计文档
```

## 关键架构决策

- **邮箱即用户主键**：`users.email` 为 VARCHAR PK，所有关联表用 `owner_email`/`from_email` 等 VARCHAR 列引用，索引覆盖
- **逻辑外键**：不使用 MySQL FOREIGN KEY，级联完整性在 Service 层 `@Transactional` 中保证
- **权限继承**：SUPER_ADMIN ⊇ ADMIN ⊇ USER，代码用 `role >=` 判断
- **断点续传**：前端分片 5MB，并发 3 片，上传状态存 ConcurrentHashMap（不依赖 Redis）
- **双容量池**：用户空间总量 + 共享池容量，均由超管设定。注册时配额不足可降为 0
- **JWT 双 Token**：access_token 24h + refresh_token 7d，无状态认证
- **自定义 UI**：不使用 Element Plus / Ant Design 等组件库，所有界面手写实现

## 开发阶段

当前处于 **Phase 1 设计完成，待实现**：
- 用户认证（邮箱注册/登录/JWT）
- 文件上传（断点续传）与管理
- 文件互传（直接发送/收件箱）
- 文件共享池（上传/审核/浏览）
- 好友系统（添加/接受/删除）
- 用户管理（配额/角色/状态）
- 全局容量管理
