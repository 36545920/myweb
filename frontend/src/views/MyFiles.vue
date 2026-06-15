<template>
  <div class="page">
    <div class="header">
      <h2 class="page-title">📁 我的文件</h2>
      <router-link to="/files/upload" class="btn-primary">📤 上传</router-link>
    </div>

    <div v-if="files.length === 0" class="empty">暂无文件，点击上传开始使用</div>

    <div class="file-list">
      <div v-for="f in files" :key="f.id" class="file-card">
        <div class="file-info">
          <div class="file-title">{{ f.title }}</div>
          <div class="file-desc" v-if="f.description">{{ f.description }}</div>
          <div class="file-meta">
            {{ f.originalName }} · {{ formatFileSize(f.fileSize) }} · {{ formatDate(f.createdAt) }}
            <span v-if="f.isSharedPool" class="badge">{{ reviewLabel(f.reviewStatus) }}</span>
          </div>
        </div>
        <div class="file-actions">
          <a :href="filesApi.downloadUrl(f.id)" class="btn-sm">⬇ 下载</a>
          <button @click="sendFile(f)" class="btn-sm">📤 发送</button>
          <button @click="confirmDelete(f)" class="btn-sm btn-danger">🗑 删除</button>
        </div>
      </div>
    </div>

    <div v-if="total > pageSize" class="pagination">
      <button :disabled="page <= 1" @click="loadPage(page - 1)">上一页</button>
      <span>{{ page }} / {{ Math.ceil(total / pageSize) }}</span>
      <button :disabled="page >= Math.ceil(total / pageSize)" @click="loadPage(page + 1)">下一页</button>
    </div>

    <!-- 发送弹窗 -->
    <div v-if="sendModal" class="modal-overlay" @click.self="sendModal = null">
      <div class="modal">
        <h3>发送文件</h3>
        <input v-model="sendEmail" placeholder="输入接收者邮箱" class="input" />
        <input v-model="sendMessage" placeholder="留言（可选）" class="input" />
        <div class="modal-btns">
          <button @click="sendModal = null" class="btn-secondary">取消</button>
          <button @click="doSend" class="btn-primary">发送</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { filesApi } from '@/api/files'
import { transfersApi } from '@/api/transfers'
import { formatFileSize, formatDate } from '@/utils/format'

const files = ref<any[]>([])
const page = ref(1)
const pageSize = 20
const total = ref(0)
const sendModal = ref<any>(null)
const sendEmail = ref('')
const sendMessage = ref('')

const reviewLabel = (s: string) => s === 'PENDING' ? '待审核' : s === 'APPROVED' ? '已通过' : '已拒绝'

async function loadPage(p: number) { page.value = p; await loadFiles() }

async function loadFiles() {
  try {
    const res: any = await filesApi.listFiles(page.value, pageSize)
    files.value = res.data.records
    total.value = res.data.total
  } catch {}
}

function sendFile(f: any) { sendModal.value = f; sendEmail.value = ''; sendMessage.value = '' }

async function doSend() {
  try {
    await transfersApi.send(sendModal.value.id, sendEmail.value, sendMessage.value)
    sendModal.value = null
    alert('发送成功')
  } catch (e: any) { alert(e.message) }
}

async function confirmDelete(f: any) {
  if (confirm('确定删除 "' + f.title + '" 吗？此操作不可恢复。')) {
    await filesApi.deleteFile(f.id)
    loadFiles()
  }
}

onMounted(loadFiles)
</script>

<style scoped>
.page { max-width: 900px; }
.header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
.page-title { font-size: 22px; color: #b87b3a; }
.btn-primary {
  padding: 10px 20px; background: #b87b3a; color: #fff; border: none;
  border-radius: 8px; font-size: 14px; cursor: pointer; text-decoration: none; font-family: inherit;
}
.empty { text-align: center; padding: 60px 0; color: #8b7355; font-size: 15px; }
.file-list { display: flex; flex-direction: column; gap: 10px; }
.file-card {
  display: flex; justify-content: space-between; align-items: center;
  background: #fff; border: 1px solid #f0e6d3; border-radius: 10px; padding: 16px 20px;
}
.file-title { font-size: 15px; color: #3d2b1f; font-weight: 600; }
.file-desc { font-size: 13px; color: #8b7355; margin-top: 2px; }
.file-meta { font-size: 12px; color: #aaa; margin-top: 4px; }
.badge { background: #f0e6d3; color: #b87b3a; padding: 1px 6px; border-radius: 4px; font-size: 11px; margin-left: 6px; }
.file-actions { display: flex; gap: 6px; white-space: nowrap; }
.btn-sm {
  padding: 6px 12px; border: 1px solid #d4b896; background: #fff;
  border-radius: 6px; cursor: pointer; font-size: 12px; color: #5a3a1a;
  text-decoration: none; font-family: inherit;
}
.btn-sm:hover { background: #fefaf2; }
.btn-danger { border-color: #e0c8c8; color: #c0392b; }
.btn-danger:hover { background: #fdf5f5; }
.pagination { display: flex; justify-content: center; align-items: center; gap: 12px; margin-top: 20px; font-size: 13px; color: #8b7355; }
.pagination button { padding: 6px 14px; border: 1px solid #d4b896; background: #fff; border-radius: 6px; cursor: pointer; font-family: inherit; }
.pagination button:disabled { opacity: 0.4; cursor: not-allowed; }
.modal-overlay { position: fixed; inset: 0; background: rgba(0,0,0,0.3); display: flex; align-items: center; justify-content: center; z-index: 100; }
.modal { background: #fff; padding: 24px; border-radius: 12px; width: 400px; display: flex; flex-direction: column; gap: 12px; }
.modal h3 { color: #b87b3a; }
.modal-btns { display: flex; gap: 8px; justify-content: flex-end; }
.input { padding: 10px 14px; border: 1px solid #e8d5c0; border-radius: 6px; font-size: 14px; background: #fefaf2; outline: none; font-family: inherit; }
.btn-secondary { padding: 8px 16px; border: 1px solid #d4b896; background: #fff; border-radius: 6px; cursor: pointer; font-family: inherit; }
</style>
