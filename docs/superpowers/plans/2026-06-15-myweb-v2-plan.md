# MyWeb V2 重构实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 引入 Element Plus 重构前端为网盘式布局，后端新增批量操作/筛选/重命名/最近访问，新增头像上传和密码重置

**Architecture:** 后端 Spring Boot 小幅扩展（FileService + FileController），前端全面替换为 Element Plus 组件全家桶，主题色覆盖为温暖棕 #b87b3a，核心页面（MyFiles）重写为网盘 el-table 布局

**Tech Stack:** Vue3 + Element Plus + @element-plus/icons-vue | Spring Boot 3 + MyBatis-Plus | MySQL

---

## 文件变更清单

```
修改:
  backend/src/main/java/com/myweb/model/entity/FileEntity.java   — 新增 3 字段
  backend/src/main/java/com/myweb/service/FileService.java        — 新增 5 方法
  backend/src/main/java/com/myweb/service/UserService.java        — 新增 resetPassword
  backend/src/main/java/com/myweb/controller/FileController.java  — 新增 4 端点 + 参数扩展
  backend/src/main/java/com/myweb/controller/AdminController.java — 新增 reset-password 端点
  backend/src/main/resources/application.yml                      — 新增头像上传路径
  backend/src/main/resources/db/schema.sql                        — ALTER TABLE files

重写:
  frontend/package.json                 — 新增 element-plus 依赖
  frontend/src/main.ts                  — 注册 ElementPlus
  frontend/src/components/AppLayout.vue — 主题色覆盖 CSS
  frontend/src/views/MyFiles.vue        — 核心重写：网盘式 el-table
  frontend/src/views/Dashboard.vue      — 最近操作记录
  frontend/src/views/Profile.vue       — 头像上传 + 裁剪
  frontend/src/views/Login.vue         — el-input/el-button
  frontend/src/views/Register.vue      — el-input/el-button
  frontend/src/views/Upload.vue        — el-upload drag
  frontend/src/views/Inbox.vue         — el-table
  frontend/src/views/Pool.vue          — el-table
  frontend/src/views/Friends.vue       — el-input/el-button
  frontend/src/views/admin/Review.vue  — el-table
  frontend/src/views/admin/Users.vue   — el-table + 重置密码按钮
  frontend/src/views/super-admin/System.vue — el-input/el-button

新增:
  frontend/src/components/AvatarUpload.vue — 头像上传裁剪组件
```

---

### Task 1: 数据库迁移 + 后端 Entity

**Files:** Modify: `FileEntity.java`, `db/schema.sql`, `application.yml`

- [ ] **Step 1: 数据库 ALTER TABLE**

```sql
ALTER TABLE files
ADD COLUMN file_type VARCHAR(20) DEFAULT 'OTHER' COMMENT 'IMAGE/DOCUMENT/VIDEO/ARCHIVE/OTHER',
ADD COLUMN last_access_at DATETIME COMMENT '最近访问时间',
ADD COLUMN last_access_by VARCHAR(255) COMMENT '最近访问者';

-- users 表新增头像字段
ALTER TABLE users ADD COLUMN avatar VARCHAR(500) DEFAULT NULL COMMENT '头像路径';
```

执行：
```bash
mysql -u root -p123456 myweb -e "ALTER TABLE files ADD COLUMN file_type VARCHAR(20) DEFAULT 'OTHER', ADD COLUMN last_access_at DATETIME, ADD COLUMN last_access_by VARCHAR(255); ALTER TABLE users ADD COLUMN avatar VARCHAR(500) DEFAULT NULL;"
```

- [ ] **Step 2: 更新 FileEntity.java**

在 FileEntity.java 中新增 3 个字段：

```java
private String fileType;        // IMAGE/DOCUMENT/VIDEO/ARCHIVE/OTHER
private LocalDateTime lastAccessAt;
private String lastAccessBy;
```

- [ ] **Step 3: application.yml 新增头像上传配置**

```yaml
app:
  avatar-path: /mnt/nas/myweb-files/avatars/
  avatar-max-size: 2097152  # 2MB
```

- [ ] **Step 4: Commit**

```bash
git add backend/ && git commit -m "feat: files 表新增 file_type/access 字段，users 表新增 avatar 字段"
```

---

### Task 2: 后端 FileService 新增方法

**Files:** Modify: `FileService.java`

- [ ] **Step 1: 新增方法到 FileService**

```java
// 在 createFile 方法中自动识别 file_type
private String detectFileType(String mimeType) {
    if (mimeType == null) return "OTHER";
    if (mimeType.startsWith("image/")) return "IMAGE";
    if (mimeType.startsWith("video/")) return "VIDEO";
    if (mimeType.contains("pdf") || mimeType.contains("word")
        || mimeType.contains("document") || mimeType.contains("text")
        || mimeType.contains("excel") || mimeType.contains("spreadsheet")
        || mimeType.contains("presentation")) return "DOCUMENT";
    if (mimeType.contains("zip") || mimeType.contains("rar")
        || mimeType.contains("tar") || mimeType.contains("gzip")
        || mimeType.contains("7z")) return "ARCHIVE";
    return "OTHER";
}

// 在 createFile 中调用: file.setFileType(detectFileType(mimeType));

// 批量删除
@Transactional
public void batchDelete(List<Long> ids) {
    String email = currentUserEmail();
    for (Long id : ids) {
        FileEntity file = fileMapper.selectById(id);
        if (file == null || !file.getOwnerEmail().equals(email)) continue;
        try { fileUtil.deleteFile(Path.of(file.getStoragePath())); } catch (IOException ignored) {}
        User owner = userMapper.selectById(file.getOwnerEmail());
        if (owner != null) {
            owner.setStorageUsed(Math.max(0, owner.getStorageUsed() - file.getFileSize()));
            userMapper.updateById(owner);
        }
        transferMapper.delete(new LambdaQueryWrapper<FileTransfer>().eq(FileTransfer::getFileId, id));
        fileMapper.deleteById(id);
    }
}

// 最近访问
public Page<FileEntity> listRecent(int size) {
    String email = currentUserEmail();
    return fileMapper.selectPage(new Page<>(1, size),
        new LambdaQueryWrapper<FileEntity>()
            .eq(FileEntity::getLastAccessBy, email)
            .orderByDesc(FileEntity::getLastAccessAt));
}

// 重命名
@Transactional
public void rename(Long id, String title) {
    FileEntity file = fileMapper.selectById(id);
    if (file == null) throw new RuntimeException("文件不存在");
    if (!file.getOwnerEmail().equals(currentUserEmail())) throw new RuntimeException("无权操作");
    file.setTitle(title);
    fileMapper.updateById(file);
}

// 记录访问
public void recordAccess(Long id) {
    FileEntity file = fileMapper.selectById(id);
    if (file != null) {
        file.setLastAccessAt(LocalDateTime.now());
        file.setLastAccessBy(currentUserEmail());
        fileMapper.updateById(file);
    }
}
```

- [ ] **Step 2: 修改 listFiles 支持筛选排序**

```java
public Page<FileEntity> listMyFiles(int page, int size, String type, String sort, String order, String keyword) {
    String email = currentUserEmail();
    LambdaQueryWrapper<FileEntity> wrapper = new LambdaQueryWrapper<FileEntity>()
        .eq(FileEntity::getOwnerEmail, email);

    if (type != null && !type.isEmpty()) wrapper.eq(FileEntity::getFileType, type);
    if (keyword != null && !keyword.isEmpty()) wrapper.like(FileEntity::getTitle, keyword);

    if ("name".equals(sort)) {
        wrapper.orderBy(true, "asc".equals(order), FileEntity::getTitle);
    } else if ("size".equals(sort)) {
        wrapper.orderBy(true, "asc".equals(order), FileEntity::getFileSize);
    } else {
        wrapper.orderByDesc(FileEntity::getCreatedAt);
    }
    return fileMapper.selectPage(new Page<>(page, size), wrapper);
}
```

- [ ] **Step 3: Commit**

```bash
git add backend/src/main/java/com/myweb/service/FileService.java
git commit -m "feat: FileService 新增 batchDelete/recent/rename/recordAccess + 文件类型识别"
```

---

### Task 3: 后端 Controller 新增端点 + 头像上传 + 密码重置

**Files:** Modify: `FileController.java`, `AdminController.java`, `UserService.java`

- [ ] **Step 1: FileController 新增端点**

```java
// POST /files/batch-delete
@PostMapping("/batch-delete")
public ResponseEntity<?> batchDelete(@RequestBody Map<String, List<Long>> body) {
    try {
        fileService.batchDelete(body.get("ids"));
        return ResponseEntity.ok(Map.of("code", 0, "message", "批量删除成功"));
    } catch (RuntimeException e) {
        return ResponseEntity.badRequest().body(Map.of("code", 1005, "message", e.getMessage()));
    }
}

// GET /files/recent
@GetMapping("/recent")
public ResponseEntity<?> recent(@RequestParam(defaultValue = "10") int limit) {
    var page = fileService.listRecent(limit);
    return ResponseEntity.ok(Map.of("code", 0, "data", Map.of("records", page.getRecords())));
}

// PUT /files/{id}/rename
@PutMapping("/{id}/rename")
public ResponseEntity<?> rename(@PathVariable Long id, @RequestBody Map<String, String> body) {
    try {
        fileService.rename(id, body.get("title"));
        return ResponseEntity.ok(Map.of("code", 0, "message", "重命名成功"));
    } catch (RuntimeException e) {
        return ResponseEntity.badRequest().body(Map.of("code", 1005, "message", e.getMessage()));
    }
}

// 修改现有 GET /files 支持筛选参数
@GetMapping
public ResponseEntity<?> list(
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "20") int size,
        @RequestParam(required = false) String type,
        @RequestParam(required = false) String sort,
        @RequestParam(required = false) String order,
        @RequestParam(required = false) String keyword) {
    var pageResult = fileService.listMyFiles(page, size, type, sort, order, keyword);
    return ResponseEntity.ok(Map.of("code", 0, "data", Map.of(
        "total", pageResult.getTotal(),
        "records", pageResult.getRecords().stream().map(FileVO::from).toList()
    )));
}
```

- [ ] **Step 2: 管理员重置密码**

在 AdminController 中新增：
```java
@PutMapping("/admin/users/{email}/reset-password")
public ResponseEntity<?> resetPassword(@PathVariable String email) {
    try {
        String newPassword = userService.resetPassword(email);
        return ResponseEntity.ok(Map.of("code", 0, "message", "密码已重置为: " + newPassword));
    } catch (RuntimeException e) {
        return ResponseEntity.badRequest().body(Map.of("code", 1005, "message", e.getMessage()));
    }
}
```

在 UserService 中新增：
```java
public String resetPassword(String email) {
    User user = userMapper.selectById(email);
    if (user == null) throw new RuntimeException("用户不存在");
    String newPassword = UUID.randomUUID().toString().substring(0, 8);
    user.setPasswordHash(passwordEncoder.encode(newPassword));
    userMapper.updateById(user);
    return newPassword;
}
```

- [ ] **Step 3: 头像上传端点**

在 UserController 中新增：
```java
@PostMapping("/me/avatar")
public ResponseEntity<?> uploadAvatar(@RequestParam("file") MultipartFile file) {
    try {
        if (file.getSize() > 2 * 1024 * 1024) throw new RuntimeException("头像不能超过 2MB");
        String ext = ".jpg";
        String originalName = file.getOriginalFilename();
        if (originalName != null && originalName.contains(".")) {
            ext = originalName.substring(originalName.lastIndexOf("."));
        }
        String filename = "avatar_" + currentUserEmail().replace("@", "_") + ext;
        Path avatarPath = Path.of(avatarDir).resolve(filename);
        Files.createDirectories(avatarPath.getParent());
        file.transferTo(avatarPath.toFile());

        User user = userMapper.selectById(currentUserEmail());
        user.setAvatar("/avatars/" + filename);
        userMapper.updateById(user);

        return ResponseEntity.ok(Map.of("code", 0, "data", Map.of("avatar", "/avatars/" + filename)));
    } catch (Exception e) {
        return ResponseEntity.badRequest().body(Map.of("code", 2001, "message", e.getMessage()));
    }
}
```

- [ ] **Step 4: Commit**

```bash
git add backend/src/main/java/com/myweb/controller/ backend/src/main/java/com/myweb/service/UserService.java
git commit -m "feat: 批量删除/最近访问/重命名/筛选排序 + 管理员重置密码 + 头像上传"
```

---

### Task 4: 前端安装 Element Plus + 主题覆盖

**Files:** Modify: `package.json`, `main.ts`, `AppLayout.vue`

- [ ] **Step 1: 安装依赖**

```bash
cd frontend && npm install element-plus @element-plus/icons-vue
```

- [ ] **Step 2: 注册 Element Plus**

`frontend/src/main.ts`:
```typescript
import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import zhCn from 'element-plus/dist/locale/zh-cn.mjs'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import App from './App.vue'
import router from './router'

const app = createApp(App)
app.use(createPinia())
app.use(router)
app.use(ElementPlus, { locale: zhCn })

// 全局注册图标
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

app.mount('#app')
```

- [ ] **Step 3: 主题色覆盖 + 全局样式**

`AppLayout.vue` 的 `<style>` 中新增：
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
  --el-font-family: 'PingFang SC', 'Noto Serif SC', 'Georgia', sans-serif;
}
```

- [ ] **Step 4: 删除旧的全局手写按钮/输入框样式**

删除 AppLayout.vue 中 `.btn` `.btn-primary` `.input` 等手写全局 CSS（Element Plus 已提供）

- [ ] **Step 5: 验证构建**

```bash
npm run build
```
预期：构建成功，打包体积会增加 element-plus

- [ ] **Step 6: Commit**

```bash
git add frontend/package.json frontend/package-lock.json frontend/src/main.ts frontend/src/components/AppLayout.vue
git commit -m "feat: 安装 Element Plus + 主题色覆盖为温暖棕 + 全局图标注册"
```

---

### Task 5: MyFiles.vue 核心重写（网盘式 el-table）

**Files:** Rewrite: `frontend/src/views/MyFiles.vue`

- [ ] **Step 1: MyFiles.vue 完整代码**

```vue
<template>
  <div class="page">
    <!-- 容量 + 工具栏 -->
    <div class="toolbar">
      <div class="storage-card">
        <span>个人空间：{{ formatFileSize(used) }} / {{ formatFileSize(quota) }}</span>
        <el-progress :percentage="usagePercent" :stroke-width="8" :color="'#b87b3a'" />
      </div>
      <div class="toolbar-actions">
        <el-button type="primary" @click="$router.push('/files/upload')">
          <el-icon><Upload /></el-icon> 上传文件
        </el-button>
        <el-button :disabled="!selectedIds.length" @click="batchDelete">
          <el-icon><Delete /></el-icon> 批量删除
        </el-button>
        <el-input v-model="keyword" placeholder="搜索文件..." clearable style="width:220px" @input="onSearch" />
        <el-button @click="viewMode = viewMode === 'table' ? 'grid' : 'table'">
          <el-icon><Grid v-if="viewMode==='table'" /><List v-else /></el-icon>
        </el-button>
      </div>
    </div>

    <!-- 分类筛选 -->
    <div class="filter-bar">
      <el-radio-group v-model="filterType" @change="loadFiles" size="small">
        <el-radio-button value="">全部</el-radio-button>
        <el-radio-button value="IMAGE">图片</el-radio-button>
        <el-radio-button value="DOCUMENT">文档</el-radio-button>
        <el-radio-button value="VIDEO">视频</el-radio-button>
        <el-radio-button value="ARCHIVE">压缩包</el-radio-button>
        <el-radio-button value="OTHER">其他</el-radio-button>
      </el-radio-group>
      <el-select v-model="sortBy" @change="loadFiles" size="small" style="width:120px">
        <el-option label="按日期" value="date" />
        <el-option label="按名称" value="name" />
        <el-option label="按大小" value="size" />
      </el-select>
    </div>

    <!-- 文件表格 -->
    <el-table :data="files" stripe @selection-change="onSelect" @row-contextmenu="onContextMenu"
              v-loading="loading" empty-text="暂无文件">
      <el-table-column type="selection" width="40" />
      <el-table-column label="文件名" sortable="custom">
        <template #default="{ row }">
          <span>{{ fileIcon(row.fileType) }} {{ row.title }}</span>
        </template>
      </el-table-column>
      <el-table-column label="大小" width="100">
        <template #default="{ row }">{{ formatFileSize(row.fileSize) }}</template>
      </el-table-column>
      <el-table-column label="类型" width="80">
        <template #default="{ row }">{{ typeLabel(row.fileType) }}</template>
      </el-table-column>
      <el-table-column label="日期" width="120">
        <template #default="{ row }">{{ formatDate(row.createdAt) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="180" fixed="right">
        <template #default="{ row }">
          <el-button size="small" @click="downloadFile(row)">下载</el-button>
          <el-button size="small" @click="sendFile(row)">发送</el-button>
          <el-button size="small" type="danger" @click="confirmDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div style="display:flex;justify-content:flex-end;margin-top:12px">
      <el-pagination v-model:current-page="page" :page-size="pageSize" :total="total"
                     layout="prev, pager, next, total" @current-change="loadFiles" />
    </div>

    <!-- 右键菜单 -->
    <div v-if="contextMenu.visible" class="context-menu" :style="{ top: contextMenu.y+'px', left: contextMenu.x+'px' }">
      <div @click="downloadFile(contextMenu.row); contextMenu.visible=false">⬇ 下载</div>
      <div @click="sendFile(contextMenu.row); contextMenu.visible=false">📤 发送</div>
      <div @click="renameFile(contextMenu.row); contextMenu.visible=false">✏ 重命名</div>
      <div @click="confirmDelete(contextMenu.row); contextMenu.visible=false" style="color:#F56C6C">🗑 删除</div>
    </div>

    <!-- 重命名弹窗 -->
    <el-dialog v-model="renameVisible" title="重命名" width="360px">
      <el-input v-model="newTitle" placeholder="新文件名" />
      <template #footer>
        <el-button @click="renameVisible=false">取消</el-button>
        <el-button type="primary" @click="doRename">确定</el-button>
      </template>
    </el-dialog>

    <!-- 发送弹窗 -->
    <el-dialog v-model="sendVisible" title="发送文件" width="400px">
      <el-input v-model="sendEmail" placeholder="接收者邮箱" style="margin-bottom:10px" />
      <el-input v-model="sendMessage" placeholder="留言（可选）" />
      <template #footer>
        <el-button @click="sendVisible=false">取消</el-button>
        <el-button type="primary" @click="doSend">发送</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { filesApi } from '@/api/files'
import { transfersApi } from '@/api/transfers'
import { formatFileSize, formatDate } from '@/utils/format'
import { ElMessage, ElMessageBox } from 'element-plus'

const auth = useAuthStore()
const used = computed(() => auth.user?.storageUsed || 0)
const quota = computed(() => auth.user?.storageQuota || 1)
const usagePercent = computed(() => Math.round((used.value / quota.value) * 100))

const files = ref<any[]>([])
const selectedIds = ref<number[]>([])
const page = ref(1); const pageSize = 20; const total = ref(0)
const filterType = ref(''); const sortBy = ref('date'); const keyword = ref('')
const loading = ref(false); const viewMode = ref('table')

const contextMenu = reactive({ visible: false, x: 0, y: 0, row: null as any })
const renameVisible = ref(false); const renameRow = ref<any>(null); const newTitle = ref('')
const sendVisible = ref(false); const sendRow = ref<any>(null)
const sendEmail = ref(''); const sendMessage = ref('')
let searchTimer: any = null

function fileIcon(t: string) { return { IMAGE:'🖼', DOCUMENT:'📄', VIDEO:'🎬', ARCHIVE:'📦' }[t] || '📁' }
function typeLabel(t: string) { return { IMAGE:'图片', DOCUMENT:'文档', VIDEO:'视频', ARCHIVE:'压缩包', OTHER:'其他' }[t] || '其他' }

async function loadFiles() {
  loading.value = true
  try {
    const res: any = await filesApi.listFiles(page.value, pageSize)
    // 注意：这里需要扩展 filesApi.listFiles 支持 type/sort/keyword 参数
    files.value = res.data.records; total.value = res.data.total
  } catch {}
  loading.value = false
}
function onSelect(rows: any[]) { selectedIds.value = rows.map(r => r.id) }

async function batchDelete() {
  try {
    await ElMessageBox.confirm(`确定删除选中的 ${selectedIds.value.length} 个文件吗？`, '确认删除', { type: 'warning' })
    await filesApi.deleteFiles(selectedIds.value)  // 需要扩展 API
    ElMessage.success('批量删除成功')
    loadFiles()
  } catch {}
}

async function downloadFile(row: any) { window.open(filesApi.downloadUrl(row.id)) }
async function confirmDelete(row: any) {
  try {
    await ElMessageBox.confirm('确定删除 "' + row.title + '" 吗？', '确认删除', { type: 'warning' })
    await filesApi.deleteFile(row.id)
    ElMessage.success('删除成功')
    loadFiles()
  } catch {}
}
function sendFile(row: any) { sendRow.value = row; sendEmail.value = ''; sendMessage.value = ''; sendVisible.value = true }
async function doSend() {
  try {
    await transfersApi.send(sendRow.value.id, sendEmail.value, sendMessage.value)
    ElMessage.success('发送成功'); sendVisible.value = false
  } catch (e: any) { ElMessage.error(e.message) }
}
function renameFile(row: any) { renameRow.value = row; newTitle.value = row.title; renameVisible.value = true }
async function doRename() {
  try {
    await filesApi.rename(renameRow.value.id, newTitle.value)
    ElMessage.success('重命名成功'); renameVisible.value = false; loadFiles()
  } catch (e: any) { ElMessage.error(e.message) }
}
function onContextMenu(row: any, _col: any, event: MouseEvent) {
  event.preventDefault()
  contextMenu.visible = true; contextMenu.x = event.clientX; contextMenu.y = event.clientY; contextMenu.row = row
}
function onSearch() {
  clearTimeout(searchTimer)
  searchTimer = setTimeout(loadFiles, 300)
}
// 点击其他区域关闭右键菜单
document.addEventListener('click', () => { contextMenu.visible = false })

onMounted(loadFiles)
</script>

<style scoped>
.page { }
.toolbar { display: flex; gap: 24px; margin-bottom: 14px; align-items: flex-start; }
.storage-card { flex: 1; background: var(--bg-card); padding: 14px 20px; border-radius: var(--radius); border: 1px solid var(--border); min-width: 200px; }
.storage-card span { font-size: 13px; font-weight: 600; color: var(--text-secondary); display: block; margin-bottom: 8px; }
.toolbar-actions { display: flex; gap: 8px; align-items: center; flex-wrap: wrap; }
.filter-bar { display: flex; justify-content: space-between; align-items: center; margin-bottom: 14px; }
.context-menu {
  position: fixed; background: #fff; border: 1px solid #e5e5e5; border-radius: 8px;
  box-shadow: 0 4px 16px rgba(0,0,0,0.1); z-index: 2000; min-width: 140px; overflow: hidden;
}
.context-menu div { padding: 10px 16px; cursor: pointer; font-size: 13px; }
.context-menu div:hover { background: #f5f5f5; }
</style>
```

- [ ] **Step 2: 更新 filesApi 支持新参数**

在 `frontend/src/api/files.ts` 中新增方法：

```typescript
deleteFiles: (ids: number[]) => client.post('/files/batch-delete', { ids }),
rename: (id: number, title: string) => client.put(`/files/${id}/rename`, { title }),
listRecent: (limit = 10) => client.get('/files/recent', { params: { limit } }),
// 更新 listFiles 支持筛选参数
listFiles: (page = 1, size = 20, type?: string, sort?: string, order?: string, keyword?: string) =>
  client.get('/files', { params: { page, size, type, sort, order, keyword } }),
```

- [ ] **Step 3: Commit**

```bash
git add frontend/src/views/MyFiles.vue frontend/src/api/files.ts
git commit -m "feat: MyFiles.vue 网盘式重写（el-table + 工具栏 + 筛选 + 右键菜单 + 批量操作）"
```

---

### Task 6: 其他页面改为 el-table

**Files:** Rewrite: `Inbox.vue`, `Pool.vue`, `admin/Review.vue`, `admin/Users.vue`

- [ ] **Step 1: Inbox.vue — 改为 el-table**

```vue
<template>
  <div class="page">
    <el-table :data="items" stripe empty-text="暂无收到文件">
      <el-table-column label="发送者" prop="fromEmail" width="200" />
      <el-table-column label="留言" prop="message" />
      <el-table-column label="时间" width="160"><template #default="{ row }">{{ formatDate(row.createdAt) }}</template></el-table-column>
      <el-table-column label="操作" width="160">
        <template #default="{ row }">
          <el-button size="small" @click="downloadFile(row)">下载</el-button>
          <el-button size="small" type="danger" @click="doDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination v-model:current-page="page" :total="total" layout="prev, pager, next" style="margin-top:12px;justify-content:flex-end" />
  </div>
</template>
```

（不再贴完整 script，参考 V1 Inbox.vue 逻辑 + el-table 属性绑定）

- [ ] **Step 2: Pool.vue — 改为 el-table**

```vue
<el-table :data="files" stripe empty-text="共享池暂无文件">
  <el-table-column label="标题" prop="title" />
  <el-table-column label="上传者" prop="ownerEmail" width="200" />
  <el-table-column label="大小" width="100"><template #default="{ row }">{{ formatFileSize(row.fileSize) }}</template></el-table-column>
  <el-table-column label="日期" width="120"><template #default="{ row }">{{ formatDate(row.createdAt) }}</template></el-table-column>
  <el-table-column label="过期" width="100"><template #default="{ row }">{{ formatDate(row.expireAt) }}</template></el-table-column>
  <el-table-column label="操作" width="100">
    <template #default="{ row }"><el-button size="small" @click="downloadFile(row)">下载</el-button></template>
  </el-table-column>
</el-table>
```

- [ ] **Step 3: Review.vue — 改为 el-table + el-tag**

```vue
<el-table :data="files" stripe empty-text="暂无待审核文件">
  <el-table-column label="标题" prop="title" />
  <el-table-column label="上传者" prop="ownerEmail" width="200" />
  <el-table-column label="大小" width="100"><template #default="{ row }">{{ formatFileSize(row.fileSize) }}</template></el-table-column>
  <el-table-column label="操作" width="240">
    <template #default="{ row }">
      <el-button size="small" type="success" @click="doReview(row, true)">通过</el-button>
      <el-button size="small" type="danger" @click="showReject(row)">拒绝</el-button>
    </template>
  </el-table-column>
</el-table>
```

- [ ] **Step 4: Users.vue — 改为 el-table + 重置密码**

```vue
<el-table :data="users" stripe>
  <el-table-column label="邮箱" prop="email" />
  <el-table-column label="昵称" prop="nickname" />
  <el-table-column label="角色" width="100">
    <template #default="{ row }"><el-tag size="small">{{ roleLabel(row.role) }}</el-tag></template>
  </el-table-column>
  <el-table-column label="配额" width="120"><template #default="{ row }">{{ formatFileSize(row.storageQuota) }}</template></el-table-column>
  <el-table-column label="已用" width="120"><template #default="{ row }">{{ formatFileSize(row.storageUsed) }}</template></el-table-column>
  <el-table-column label="状态" width="80">
    <template #default="{ row }"><el-tag :type="row.status==='ACTIVE'?'success':'danger'" size="small">{{ row.status }}</el-tag></template>
  </el-table-column>
  <el-table-column label="操作" width="240">
    <template #default="{ row }">
      <el-button size="small" @click="openQuota(row)">配额</el-button>
      <el-button size="small" @click="toggleStatus(row)">{{ row.status==='ACTIVE'?'禁用':'启用' }}</el-button>
      <el-button size="small" type="warning" @click="resetPwd(row)">重置密码</el-button>
    </template>
  </el-table-column>
</el-table>
```

新增重置密码逻辑：
```typescript
import { ElMessage, ElMessageBox } from 'element-plus'

async function resetPwd(row: any) {
  try {
    await ElMessageBox.confirm('确定重置用户 ' + row.email + ' 的密码吗？', '确认重置', { type: 'warning' })
    const res: any = await adminApi.resetPassword(row.email)
    ElMessage.success(res.message)
  } catch {}
}
```

adminApi 新增：
```typescript
resetPassword: (email: string) => client.put(`/admin/users/${email}/reset-password`),
```

- [ ] **Step 5: Commit**

```bash
git add frontend/src/views/Inbox.vue frontend/src/views/Pool.vue frontend/src/views/admin/Review.vue frontend/src/views/admin/Users.vue frontend/src/api/admin.ts
git commit -m "feat: Inbox/Pool/Review/Users 改为 el-table + 管理员重置密码"
```

---

### Task 7: 登录/注册/上传/仪表盘/好友/设置 组件替换

**Files:** Rewrite: `Login.vue`, `Register.vue`, `Upload.vue`, `Dashboard.vue`, `Friends.vue`, `Profile.vue`

- [ ] **Step 1: 所有页面组件替换规则**

| 原组件 | 替换为 |
|--------|--------|
| `<input class="input">` | `<el-input>` |
| `<button class="btn-primary">` | `<el-button type="primary">` |
| `<button class="btn-secondary">` | `<el-button>` |
| `<p class="error">` | `<el-alert type="error">` 或直接 ElMessage.error() |
| `alert()` | `ElMessage.success()` / `ElMessage.error()` |
| `confirm()` | `ElMessageBox.confirm()` |

- [ ] **Step 2: Login.vue 完整重写示例**

```vue
<template>
  <div class="auth-page">
    <div class="auth-card">
      <h1>🍃 MyWeb</h1>
      <p class="subtitle">登录你的账号</p>
      <el-form @submit.prevent="handleLogin">
        <el-input v-model="email" placeholder="邮箱" size="large" style="margin-bottom:14px" />
        <el-input v-model="password" type="password" placeholder="密码" size="large" show-password style="margin-bottom:14px" />
        <el-button type="primary" size="large" @click="handleLogin" :loading="loading" style="width:100%">登录</el-button>
      </el-form>
      <p class="link-text">还没有账号？<router-link to="/register">立即注册</router-link></p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { authApi } from '@/api/auth'
import { ElMessage } from 'element-plus'

const router = useRouter(); const route = useRoute(); const auth = useAuthStore()
const email = ref(''); const password = ref(''); const loading = ref(false)

async function handleLogin() {
  loading.value = true
  try {
    const res: any = await authApi.login({ email: email.value, password: password.value })
    auth.setTokens(res.data.accessToken, res.data.refreshToken, res.data.user)
    router.push((route.query.redirect as string) || '/')
  } catch (e: any) { ElMessage.error(e.message || '登录失败') }
  finally { loading.value = false }
}
</script>
```

- [ ] **Step 3: 其他页面同理替换**

Register / Dashboard / Upload / Friends / Profile 全部按同样的规则替换组件。

- [ ] **Step 4: Commit**

```bash
git add frontend/src/views/Login.vue frontend/src/views/Register.vue frontend/src/views/Upload.vue frontend/src/views/Dashboard.vue frontend/src/views/Friends.vue frontend/src/views/Profile.vue
git commit -m "feat: Login/Register/Upload/Dashboard/Friends/Profile 全部替换为 Element Plus 组件"
```

---

### Task 8: 头像上传组件

**Files:** Create: `frontend/src/components/AvatarUpload.vue`, Modify: `Profile.vue`

- [ ] **Step 1: AvatarUpload.vue**

```vue
<template>
  <div class="avatar-section">
    <div class="avatar-preview" @click="fileInput?.click()">
      <img v-if="previewUrl" :src="previewUrl" class="avatar-img" />
      <div v-else class="avatar-default">{{ initial }}</div>
      <div class="avatar-overlay">✏</div>
    </div>
    <input ref="fileInput" type="file" accept="image/*" @change="onFileChange" style="display:none" />
    <div v-if="file" class="avatar-info">
      <span>{{ file.name }} ({{ formatFileSize(file.size) }})</span>
      <el-button size="small" type="primary" @click="upload" :loading="uploading">上传</el-button>
      <el-button size="small" @click="cancel">取消</el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { usersApi } from '@/api/users'
import { formatFileSize } from '@/utils/format'
import { ElMessage } from 'element-plus'

const auth = useAuthStore()
const fileInput = ref<HTMLInputElement>()
const file = ref<File | null>(null)
const previewUrl = ref<string | null>(auth.user?.avatar || null)
const uploading = ref(false)
const initial = computed(() => (auth.user?.nickname || 'U')[0])

function onFileChange(e: Event) {
  const f = (e.target as HTMLInputElement).files?.[0]
  if (!f) return
  if (f.size > 2 * 1024 * 1024) { ElMessage.error('头像不能超过 2MB'); return }
  file.value = f
  previewUrl.value = URL.createObjectURL(f)
}

async function upload() {
  if (!file.value) return
  uploading.value = true
  try {
    const formData = new FormData(); formData.append('file', file.value)
    const res: any = await usersApi.uploadAvatar(formData)
    auth.user.avatar = res.data.avatar
    file.value = null
    ElMessage.success('头像上传成功')
  } catch (e: any) { ElMessage.error(e.message) }
  finally { uploading.value = false }
}

function cancel() { file.value = null; previewUrl.value = auth.user?.avatar || null }
</script>

<style scoped>
.avatar-section { display: flex; flex-direction: column; align-items: center; gap: 12px; }
.avatar-preview {
  width: 96px; height: 96px; border-radius: 50%; overflow: hidden;
  cursor: pointer; position: relative; border: 3px solid var(--el-color-primary-light-7);
}
.avatar-img { width: 100%; height: 100%; object-fit: cover; }
.avatar-default {
  width: 100%; height: 100%; background: var(--el-color-primary-light-5);
  display: flex; align-items: center; justify-content: center;
  font-size: 36px; font-weight: 700; color: var(--el-color-primary);
}
.avatar-overlay {
  position: absolute; inset: 0; background: rgba(0,0,0,0.15);
  display: flex; align-items: center; justify-content: center; opacity: 0; transition: opacity 0.2s;
  font-size: 20px; color: #fff;
}
.avatar-preview:hover .avatar-overlay { opacity: 1; }
.avatar-info { display: flex; align-items: center; gap: 8px; font-size: 13px; }
</style>
```

- [ ] **Step 2: 集成到 Profile.vue**

在 Profile.vue 中引入 `<AvatarUpload />`，配合 nickname/密码修改。

- [ ] **Step 3: usersApi 新增**

```typescript
uploadAvatar: (formData: FormData) => client.post('/users/me/avatar', formData, {
  headers: { 'Content-Type': 'multipart/form-data' }
}),
```

- [ ] **Step 4: Commit**

```bash
git add frontend/src/components/AvatarUpload.vue frontend/src/views/Profile.vue frontend/src/api/users.ts
git commit -m "feat: 头像上传组件（预览/默认头像/状态反馈）"
```

---

### Task 9: 构建验证 + 清理

- [ ] **Step 1: 完整构建**

```bash
cd frontend && npm run build
```
预期：构建成功，Element Plus 组件均正常编译

- [ ] **Step 2: 检查未使用的导入和死代码**

删除不再使用的自定义 CSS 变量和全局样式。

- [ ] **Step 3: 后端编译**

```bash
cd backend && JAVA_HOME="C:/Program Files/Java/jdk-17.0.12" mvn compile
```
预期：编译成功

- [ ] **Step 4: Commit**

```bash
git add -A && git commit -m "chore: V2 构建验证 + 代码清理"
```

---

## 实现顺序

```
Task 1 (数据库) → Task 2 (FileService) → Task 3 (Controller + 头像 + 密码重置)
                                     ↘
Task 4 (Element Plus 安装 + 主题) → Task 5 (MyFiles 核心重写) → Task 6 (el-table 页面)
                                                              → Task 7 (组件替换页面)
                                                              → Task 8 (头像上传)
                                                              → Task 9 (验证清理)
```
