<template>
  <div class="page">
    <h2 class="page-title">📦 文件共享池</h2>
    <div v-if="files.length === 0" class="empty">共享池暂无文件</div>
    <div class="file-list">
      <div v-for="f in files" :key="f.id" class="file-card">
        <div class="file-info">
          <div class="file-title">{{ f.title }}</div>
          <div class="file-desc" v-if="f.description">{{ f.description }}</div>
          <div class="file-meta">
            上传者：{{ f.ownerEmail }} · {{ formatFileSize(f.fileSize) }} · {{ formatDate(f.createdAt) }}
            · 过期：{{ formatDate(f.expireAt) }}
          </div>
        </div>
        <a :href="filesApi.downloadUrl(f.id)" class="btn-sm">⬇ 下载</a>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { adminApi } from '@/api/admin'
import { filesApi } from '@/api/files'
import { formatFileSize, formatDate } from '@/utils/format'

const files = ref<any[]>([])
onMounted(async () => {
  try { const res: any = await adminApi.poolList(); files.value = res.data.records } catch {}
})
</script>

<style scoped>
.page { max-width: 900px; }
.page-title { font-size: 22px; color: #b87b3a; margin-bottom: 20px; }
.empty { text-align: center; padding: 60px 0; color: #8b7355; }
.file-list { display: flex; flex-direction: column; gap: 10px; }
.file-card {
  display: flex; justify-content: space-between; align-items: center;
  background: #fff; border: 1px solid #f0e6d3; border-radius: 10px; padding: 16px 20px;
}
.file-title { font-size: 15px; color: #3d2b1f; font-weight: 600; }
.file-desc { font-size: 13px; color: #8b7355; margin-top: 2px; }
.file-meta { font-size: 12px; color: #aaa; margin-top: 4px; }
.btn-sm {
  padding: 6px 12px; border: 1px solid #d4b896; background: #fff;
  border-radius: 6px; cursor: pointer; font-size: 12px; color: #5a3a1a;
  text-decoration: none; font-family: inherit; white-space: nowrap;
}
</style>
