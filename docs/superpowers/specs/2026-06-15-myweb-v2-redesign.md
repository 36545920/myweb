# MyWeb V2 重构设计文档

> 网盘式布局 + Element Plus 全家桶  
> 日期：2026-06-15 | 版本：v2.0

---

## 1. 概述

在 V1 基础上重构前端 UI 架构，引入 Element Plus 组件库，采用网盘式（百度网盘/蓝奏云）页面布局。后端仅做小幅扩展。

### 1.1 目标

- 替换自定义 UI 为 Element Plus 标准组件（按需引入，全家桶可用）
- 网盘式文件列表（表格视图 + 网格视图切换）
- 新增批量操作、文件筛选排序、右键菜单、拖拽上传
- 保持温暖棕色主题色（通过 Element Plus CSS 变量覆盖）
- 文件预览放在 Phase 2 之后再做

### 1.2 不变部分

- 后端认证/权限/容量管理体系
- 好友系统、文件互传、共享池审核
- MySQL 逻辑外键、NAS 存储
- 部署架构（Nginx + Spring Boot + MySQL + NPS）

---

## 2. UI 框架变更

### 2.1 Element Plus 引入

```bash
npm install element-plus @element-plus/icons-vue
```

```typescript
// main.ts 中全量注册
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import zhCn from 'element-plus/dist/locale/zh-cn.mjs'

app.use(ElementPlus, { locale: zhCn })
```

### 2.2 主题色覆盖

在全局 CSS 中覆盖 Element Plus 默认主题色为温暖棕色：

```css
:root {
  --el-color-primary: #b87b3a;
  --el-color-primary-light-3: #d4b896;
  --el-color-primary-light-5: #e8d5c0;
  --el-color-primary-light-7: #f0e6d3;
  --el-color-primary-light-8: #f5ede0;
  --el-color-primary-light-9: #fdf5ed;
  --el-color-primary-dark-2: #9e672f;
  --el-border-radius-base: 8px;
  --el-border-radius-small: 4px;
}
```

### 2.3 组件映射表

| 功能 | V1（手写） | V2（Element Plus） |
|------|-----------|-------------------|
| 按钮 | 自定义 .btn .btn-primary | el-button |
| 输入框 | 自定义 .input | el-input |
| 表格 | 手写 table + div | el-table（排序/多选/固定列） |
| 分页 | 手写按钮组 | el-pagination |
| 弹窗 | 手写 .modal-overlay | el-dialog |
| 消息提示 | alert() | el-message |
| 确认框 | confirm() | el-message-box |
| 下拉选择 | 手写 radio 组 | el-select |
| 日期选择 | 手写 radio 组 | el-date-picker |
| 右键菜单 | 无 | 自定义 contextmenu 弹层 |
| 标签/徽章 | 手写 .badge | el-tag |
| 拖拽上传 | 手写 drop-zone | el-upload (drag mode) |
| 进度条 | 手写 .bar | el-progress |
| 多选 | 无 | el-table selection 列 |
| 标签页 | 无 | el-tabs（未来 Phase 2 用） |
| 侧边栏 | 自定义 Sidebar | **保留自定义**（温暖风格定制） |

---

## 3. 页面架构

### 3.1 整体布局

```
┌─────────────────────────────────────────────────┐
│  🍃 MyWeb   仪表盘  我的文件  收件箱  共享池  好友 │ ← 侧边栏（保留自定义）
│                管理区  审核  用户管理              │
│                系统区  系统设置                    │
│                 个人设置                          │
├─────────────────────────────────────────────────┤
│  我的文件                                  [用户名] │ ← 顶部 header
├─────────────────────────────────────────────────┤
│  [📤 上传文件]  [⬇ 批量下载]  [🗑 批量删除]       │ ← 工具栏
│  🔍 搜索文件...                         ⊞ 切换视图 │
├─────────────────────────────────────────────────┤
│  全部 | 图片 | 文档 | 视频 | 压缩包 | 其他        │ ← 分类筛选 + 排序
├─────────────────────────────────────────────────┤
│  ☐  文件名 ⬆        大小    类型    修改日期  操作 │ ← el-table
│  ☐  📄 设计文档.pdf   2.5MB   文档    06-15    ⬇📤🗑│
│  ☐  🖼  照片.jpg      5.8MB   图片    06-14    ⬇📤🗑│
├─────────────────────────────────────────────────┤
│  容量：1.2 GB / 10 GB            [1] 2 3 ... 下一页│ ← 进度条 + 分页
└─────────────────────────────────────────────────┘
```

### 3.2 前端路由

| 路由 | 页面 | 变更 |
|------|------|------|
| `/` | Dashboard | 重写：最近操作记录 + 快捷入口 |
| `/files` | MyFiles | **重写**：网盘式 el-table + 工具栏 + 右键菜单 |
| `/files/upload` | Upload | el-upload drag 模式 |
| `/inbox` | Inbox | 改为 el-table |
| `/pool` | Pool | 改为 el-table |
| `/friends` | Friends | 保留结构，换 el-input/el-button |
| `/profile` | Profile | 保留结构，换 el-input/el-button |
| `/admin/review` | Review | 改为 el-table |
| `/admin/users` | Users | 改为 el-table |
| `/super-admin/system` | System | 保留 |
| `/login` `/register` `/verify-email` | 同上 | 换 el-input/el-button |

### 3.3 侧边栏

保留当前自定义 Sidebar.vue，不做 el-menu。理由：温暖风格的 el-menu 定制量大，不如直接保留当前代码。

---

## 4. 数据模型变更

### 4.1 files 表新增字段

```sql
ALTER TABLE files
ADD COLUMN file_type VARCHAR(20) DEFAULT 'OTHER' COMMENT 'IMAGE/DOCUMENT/VIDEO/ARCHIVE/OTHER',
ADD COLUMN last_access_at DATETIME COMMENT '最近访问时间',
ADD COLUMN last_access_by VARCHAR(255) COMMENT '最近访问者';
```

### 4.2 file_type 自动识别

在 FileService.createFile 中根据 MIME type 自动设置：
- `image/*` → IMAGE
- `video/*` → VIDEO
- `application/pdf`, `application/msword`, `application/vnd.*` 等 → DOCUMENT
- `application/zip`, `application/x-rar`, `application/gzip` 等 → ARCHIVE
- 其他 → OTHER

### 4.3 其他表不变

users、file_transfers、friends、system_config 保持不变。

---

## 5. API 变更

### 5.1 新增接口

| Method | Path | 说明 |
|--------|------|------|
| POST | `/files/batch-delete` | 批量删除 `{ids: [1,2,3]}` |
| POST | `/files/batch-download` | 批量打包下载（返回 zip 流） |
| GET | `/files/recent` | 最近访问文件（limit 20） |
| PUT | `/files/{id}/rename` | 重命名 `{title: "新名称"}` |

### 5.2 扩展现有接口

`GET /files` 新增查询参数：
- `type` — 按 file_type 筛选（IMAGE/DOCUMENT/VIDEO/ARCHIVE/OTHER）
- `sort` — 排序字段（date/name/size）
- `order` — 排序方向（asc/desc）
- `keyword` — 文件名搜索（LIKE '%keyword%'）

### 5.3 其余接口不变

认证/传输/好友/管理/配置模块 30+ 接口保持不变。

---

## 6. 后端变更

### 6.1 FileService 新增方法

```java
// 批量删除
@Transactional
public void batchDelete(List<Long> ids) { ... }

// 批量下载（打包为 zip 流）
public InputStream batchDownload(List<Long> ids) { ... }

// 最近访问
public Page<FileEntity> listRecent(int size) { ... }

// 重命名
public void rename(Long id, String title) { ... }

// 记录访问
public void recordAccess(Long id, String email) { ... }
```

### 6.2 FileController 变更

- 新增上述 4 个端点
- `/files` GET 扩展 type/sort/order/keyword 参数

### 6.3 文件类型识别

```java
private String detectFileType(String mimeType) {
    if (mimeType == null) return "OTHER";
    if (mimeType.startsWith("image/")) return "IMAGE";
    if (mimeType.startsWith("video/")) return "VIDEO";
    if (mimeType.startsWith("audio/")) return "AUDIO";
    if (mimeType.contains("pdf") || mimeType.contains("word") 
        || mimeType.contains("document") || mimeType.contains("text")
        || mimeType.contains("excel") || mimeType.contains("spreadsheet")
        || mimeType.contains("presentation")) return "DOCUMENT";
    if (mimeType.contains("zip") || mimeType.contains("rar") 
        || mimeType.contains("tar") || mimeType.contains("gzip")
        || mimeType.contains("7z")) return "ARCHIVE";
    return "OTHER";
}
```

---

## 7. 核心 UI 重构

### 7.1 MyFiles.vue（最核心页面）

使用 el-table 替换手写列表：
- **selection 列**：el-table-column type="selection"
- **文件名列**：文件名 + 类型图标（根据 file_type 显示不同图标）
- **大小列**：formatFileSize 格式化
- **类型列**：file_type 中文显示
- **日期列**：formatDate 显示
- **操作列**：下载/发送/删除（el-button size="small"）

工具栏：
- el-button（上传文件/批量下载/批量删除）
- el-input（搜索框，带防抖）
- el-button（视图切换按钮）

右键菜单：
- 自定义 contextmenu 事件，弹出菜单（下载/发送/删除/重命名）

### 7.2 Dashboard.vue

- 最近操作记录列表（el-table，只显示最近 10 条）
- 快捷入口卡片（上传/收件箱/共享池/好友）

### 7.3 其他页面

- Inbox / Pool → 改为 el-table 格式
- Review / Users → 改为 el-table + el-tag（状态标签）
- Login / Register → el-input + el-button + el-message 错误提示
- 所有 alert() → el-message
- 所有 confirm() → el-message-box

---

## 8. 影响范围

| 层级 | 改动文件 | 改动量 |
|------|---------|--------|
| 数据库 | files 表 DDL | 1 条 ALTER |
| 后端 Entity | FileEntity.java | 新增 3 字段 |
| 后端 Service | FileService.java | 新增 4 方法 + 类型识别 |
| 后端 Controller | FileController.java | 新增 3 端点 + 扩展现有 |
| 前端依赖 | package.json | 新增 element-plus |
| 前端入口 | main.ts | 注册 ElementPlus |
| 前端全局 | AppLayout.vue | 主题色覆盖 CSS |
| 前端核心页 | MyFiles.vue | 完全重写 |
| 前端页面 | Inbox/Pool/Review/Users | 改为 el-table |
| 前端页面 | Login/Register/Dashboard/Upload/Profile | 组件替换 |
| 前端页面 | Friends/System | 小幅改动 |

---

## 9. 设计决策记录

| 决策 | 结论 | 理由 |
|------|------|------|
| Element Plus 引入方式 | 全量引入 | 个人项目，体积不是瓶颈，避免按需引入的前期配置复杂度 |
| 主题色 | 覆盖为温暖棕 #b87b3a | 保持 V1 视觉风格，不变成通用后台风 |
| 侧边栏 | 保留自定义 | el-menu 定制成本高 |
| 文件夹 | 不支持 | 个人使用场景，扁平结构够用 |
| 回收站 | 不支持 | 保持简单，硬删除足够 |
| 文件预览 | Phase 2 后再说 | 复杂度高，优先完成网盘式布局 |
| 面包屑 | 不需要 | 无文件夹层级 |
