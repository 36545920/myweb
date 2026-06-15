<template>
  <div class="page">
    <!-- 容量 + 工具栏 -->
    <div class="toolbar">
      <div class="storage-card">
        <span>个人空间：{{ formatFileSize(used) }} / {{ formatFileSize(quota) }}</span>
        <el-progress :percentage="usagePercent" :stroke-width="8" color="#b87b3a" />
      </div>
      <div class="toolbar-actions">
        <el-button type="primary" @click="$router.push('/files/upload')">
          <el-icon><Upload /></el-icon> 上传文件
        </el-button>
        <el-button :disabled="selectedIds.length === 0" @click="batchDelete">
          <el-icon><Delete /></el-icon> 批量删除
        </el-button>
        <el-input v-model="keyword" placeholder="搜索文件..." clearable style="width:220px" @input="onSearch" />
        <el-button @click="toggleView">
          <el-icon><Grid v-if="viewMode === 'table'" /><List v-else /></el-icon>
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
    <el-table
      :data="files" stripe @selection-change="onSelect"
      @row-contextmenu="onContextMenu"
      v-loading="loading" empty-text="暂无文件"
      style="width:100%">
      <el-table-column type="selection" width="40" />
      <el-table-column label="文件名" sortable="custom">
        <template #default="{ row }">
          <span style="margin-right:6px">{{ fileIcon(row.fileType) }}</span>
          <span>{{ row.title }}</span>
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
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="{ row }">
          <el-button size="small" @click="downloadFile(row)">下载</el-button>
          <el-button size="small" @click="sendFile(row)">发送</el-button>
          <el-button size="small" type="danger" @click="confirmDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div style="display:flex;justify-content:flex-end;margin-top:14px;">
      <el-pagination
        v-model:current-page="page" :page-size="pageSize" :total="total"
        layout="prev, pager, next, total" @current-change="loadFiles"
        background small />
    </div>

    <!-- 右键菜单 -->
    <div
      v-show="contextMenu.visible"
      class="context-menu"
      :style="{ top: contextMenu.y + 'px', left: contextMenu.x + 'px' }">
      <div class="menu-item" @click="downloadFile(contextMenu.row); contextMenu.visible = false">下载</div>
      <div class="menu-item" @click="sendFile(contextMenu.row); contextMenu.visible = false">发送</div>
      <div class="menu-item" @click="renameFile(contextMenu.row); contextMenu.visible = false">重命名</div>
      <div class="menu-item danger" @click="confirmDelete(contextMenu.row); contextMenu.visible = false">删除</div>
    </div>

    <!-- 发送弹窗 -->
    <el-dialog v-model="sendVisible" title="发送文件" width="400px">
      <el-input v-model="sendToEmail" placeholder="接收者邮箱" style="margin-bottom:12px" />
      <el-input v-model="sendMessageText" placeholder="留言（可选）" />
      <template #footer>
        <el-button @click="sendVisible = false">取消</el-button>
        <el-button type="primary" @click="doSend">发送</el-button>
      </template>
    </el-dialog>

    <!-- 重命名弹窗 -->
    <el-dialog v-model="renameVisible" title="重命名" width="360px">
      <el-input v-model="newTitle" placeholder="新文件名" />
      <template #footer>
        <el-button @click="renameVisible = false">取消</el-button>
        <el-button type="primary" @click="doRename">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, computed, reactive } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { filesApi } from '@/api/files'
import { transfersApi } from '@/api/transfers'
import { formatFileSize, formatDate } from '@/utils/format'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Upload, Delete, Grid, List } from '@element-plus/icons-vue'

const auth = useAuthStore()
const used = computed(() => auth.user?.storageUsed || 0)
const quota = computed(() => auth.user?.storageQuota || 1)
const usagePercent = computed(() => Math.round((used.value / quota.value) * 100))

const files = ref<any[]>([])
const selectedIds = ref<number[]>([])
const page = ref(1); const pageSize = ref(20); const total = ref(0)
const filterType = ref(''); const sortBy = ref('date'); const keyword = ref('')
const loading = ref(false); const viewMode = ref('table')

const contextMenu = reactive({ visible: false, x: 0, y: 0, row: null as any })
const renameVisible = ref(false); const renameRow = ref<any>(null); const newTitle = ref('')
const sendVisible = ref(false); const sendRow = ref<any>(null)
const sendToEmail = ref(''); const sendMessageText = ref('')
let searchTimer: any = null

function fileIcon(t: string) { return { IMAGE: '🖼', DOCUMENT: '📄', VIDEO: '🎬', ARCHIVE: '📦' }[t] || '📁' }
function typeLabel(t: string) { return { IMAGE: '图片', DOCUMENT: '文档', VIDEO: '视频', ARCHIVE: '压缩包', OTHER: '其他' }[t] || '其他' }

async function loadFiles() {
  loading.value = true
  try {
    const res: any = await filesApi.listFiles(page.value, pageSize.value, filterType.value || undefined, sortBy.value, 'desc', keyword.value || undefined)
    files.value = res.data.records
    total.value = res.data.total
  } catch { ElMessage.error('加载失败') }
  finally { loading.value = false }
}

function onSelect(rows: any[]) { selectedIds.value = rows.map((r: any) => r.id) }

async function batchDelete() {
  if (selectedIds.value.length === 0) return
  try {
    await ElMessageBox.confirm(`确定删除选中的 ${selectedIds.value.length} 个文件吗？`, '确认删除', { type: 'warning' })
    await filesApi.deleteFiles(selectedIds.value)
    ElMessage.success('批量删除成功')
    selectedIds.value = []
    loadFiles()
  } catch { /* 用户取消 */ }
}

function downloadFile(row: any) { window.open(filesApi.downloadUrl(row.id)) }

async function confirmDelete(row: any) {
  try {
    await ElMessageBox.confirm(`确定删除 "${row.title}" 吗？`, '确认删除', { type: 'warning' })
    await filesApi.deleteFile(row.id)
    ElMessage.success('删除成功')
    loadFiles()
  } catch { /* 用户取消 */ }
}

function sendFile(row: any) {
  sendRow.value = row; sendToEmail.value = ''; sendMessageText.value = ''; sendVisible.value = true
}

async function doSend() {
  if (!sendToEmail.value) { ElMessage.error('请输入接收者邮箱'); return }
  try {
    await transfersApi.send(sendRow.value.id, sendToEmail.value, sendMessageText.value)
    ElMessage.success('发送成功'); sendVisible.value = false
  } catch (e: any) { ElMessage.error(e.message || '发送失败') }
}

function renameFile(row: any) {
  renameRow.value = row; newTitle.value = row.title; renameVisible.value = true
}

async function doRename() {
  if (!newTitle.value.trim()) { ElMessage.error('文件名不能为空'); return }
  try {
    await filesApi.rename(renameRow.value.id, newTitle.value.trim())
    ElMessage.success('重命名成功'); renameVisible.value = false; loadFiles()
  } catch (e: any) { ElMessage.error(e.message || '重命名失败') }
}

function onContextMenu(row: any, _col: any, event: MouseEvent) {
  event.preventDefault()
  contextMenu.x = event.clientX; contextMenu.y = event.clientY
  contextMenu.row = row; contextMenu.visible = true
}

function onSearch() {
  clearTimeout(searchTimer)
  searchTimer = setTimeout(loadFiles, 300)
}

function toggleView() {
  viewMode.value = viewMode.value === 'table' ? 'grid' : 'table'
}

function closeContextMenu() { contextMenu.visible = false }

onMounted(() => { loadFiles(); document.addEventListener('click', closeContextMenu) })
onUnmounted(() => { document.removeEventListener('click', closeContextMenu) })
</script>

<style scoped>
.toolbar { display: flex; gap: 20px; margin-bottom: 16px; align-items: flex-start; }
.storage-card { flex: 1; background: var(--bg-card); padding: 14px 20px; border-radius: var(--radius); border: 1px solid var(--border); box-shadow: var(--shadow-sm); min-width: 200px; }
.storage-card span { font-size: 13px; font-weight: 600; color: var(--text-secondary); display: block; margin-bottom: 8px; }
.toolbar-actions { display: flex; gap: 8px; align-items: center; flex-wrap: wrap; }
.filter-bar { display: flex; justify-content: space-between; align-items: center; margin-bottom: 14px; }
.context-menu { position: fixed; background: #fff; border: 1px solid #e5e5e5; border-radius: 10px; box-shadow: 0 6px 24px rgba(0,0,0,0.12); z-index: 2000; min-width: 150px; overflow: hidden; }
.menu-item { padding: 10px 18px; cursor: pointer; font-size: 13px; transition: background 0.12s; }
.menu-item:hover { background: #f5f5f5; }
.menu-item.danger { color: var(--el-color-danger); }
.menu-item.danger:hover { background: #fef0f0; }
</style>
