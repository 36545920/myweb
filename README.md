# 🍃 MyWeb

个人文件共享网站 — 网盘式文件管理 + 用户间互传 + 共享池。

## 技术栈

| 层 | 技术 |
|------|------|
| 前端 | Vue 3 + Element Plus + Vite |
| 后端 | Spring Boot 3 + MyBatis-Plus |
| 数据库 | MySQL 8.0 |
| 文件存储 | 本地虚拟机通过 NPS 穿透（独立文件服务） |
| 代理 | Nginx |

## 功能

### 用户系统
- 邮箱注册登录（QQ SMTP 验证码）
- JWT 双 Token 认证
- 三级权限：超级管理员 / 管理员 / 普通用户（继承式）
- 每日注册限制
- 头像上传（即时预览 + 默认头像）
- 管理员重置密码 / 调整配额 / 启用禁用用户

### 文件管理（网盘式）
- 分片上传 + 断点续传（5MB / 片，并发 3 片）
- 筛选：按类型（图片 / 文档 / 视频 / 压缩包）+ 搜索 + 排序
- 批量操作：多选 + 批量删除
- 右键菜单：下载 / 发送 / 重命名 / 删除
- 过期自动清理（凌晨 3:00 定时任务）

### 文件互传
- 直接发送给指定用户（类似邮箱附件）
- 收件箱 + 已发送记录
- 好友系统（搜索邮箱添加 / 接受 / 删除）

### 文件共享池
- 所有人可见的文件共享空间
- 管理员审核（通过 / 拒绝 + 原因）
- 全局容量由超管统一调配

### 运维
- NAS 健康检查（每 30 秒）
- 存储离线时页面警告横幅
- 独立文件服务器（云端只管元数据，虚拟机管文件）

## 项目结构

```
myweb/
├── backend/                    # Spring Boot 主程序
│   └── src/main/java/com/myweb/
│       ├── config/             # Security / CORS / MyBatis-Plus
│       ├── controller/         # REST 控制器
│       ├── service/            # 业务逻辑
│       ├── repository/         # MyBatis Mapper
│       ├── model/              # Entity / DTO / VO
│       ├── filter/             # JWT 认证过滤器
│       └── util/               # JWT / File / Email
├── frontend/                   # Vue3 前端
│   └── src/
│       ├── api/                # Axios 请求封装
│       ├── views/              # 页面组件（15 个）
│       ├── components/         # 通用组件
│       ├── stores/             # Pinia 状态管理
│       ├── router/             # 路由配置
│       └── utils/              # 格式化 / 校验
├── file-server/                # 文件存储服务（部署在本地虚拟机）
├── nginx/                      # Nginx 配置
├── deploy/                     # 部署配置模板
└── docs/                       # 设计文档 / 实现计划
```

## 本地开发

### 环境要求

- JDK 17+
- MySQL 8.0
- Node.js 18+
- Maven 3.9+

### 后端

```bash
cd backend

# 初始化数据库（仅首次）
mysql -u root -p < src/main/resources/db/schema.sql

# 启动（需要改 application.yml 中的数据库密码）
JAVA_HOME="/path/to/jdk17" mvn spring-boot:run
# 启动在 :10888/api/v1
```

### 前端

```bash
cd frontend
npm install
npm run dev
# 启动在 :5173，自动代理到 :10888
```

### 文件服务器（可选，本地开发不需要）

```bash
cd file-server
JAVA_HOME="/path/to/jdk17" mvn spring-boot:run
# 启动在 :18080
```

## 部署

部署说明见 `deploy/` 目录下的配置模板：

| 文件 | 部署位置 |
|------|----------|
| `deploy/application-template.yml` | 云服务器 `/home/myweb/application.yml` |
| `deploy/file-server-template.yml` | 本地虚拟机 `/home/myweb/file-server.yml` |

### 部署顺序

```
1. 本地虚拟机 → 启动文件服务器 + NPS 客户端
2. 云服务器 → MySQL + 后端 + Nginx + NPS 服务端
3. 配置 NPS 隧道
4. 浏览器访问验证
```

### 默认管理员

| 邮箱 | 密码 |
|------|------|
| admin@myweb.local | admin123 |

**首次登录后务必修改密码。**

## License

MIT
