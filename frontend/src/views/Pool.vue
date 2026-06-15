<template>
  <div class="page">
    <h2 class="page-title">文件共享池</h2>
    <div class="capacity-bar">
      <div class="capacity-fill" :style="{ width: usagePercent + '%' }"></div>
      <span class="capacity-text">共享池：{{ formatFileSize(poolUsed) }} / {{ formatFileSize(poolQuota) }}</span>
    </div>
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
import { ref, onMounted, computed } from 'vue'
import { adminApi } from '@/api/admin'
import { filesApi } from '@/api/files'
import { formatFileSize, formatDate } from '@/utils/format'

const files = ref<any[]>([])
const poolQuota = ref(0)
const poolUsed = ref(0)
const usagePercent = computed(() => poolQuota.value > 0 ? Math.min(100, (poolUsed.value / poolQuota.value) * 100) : 0)

onMounted(async () => {
  try {
    const res: any = await adminApi.poolList()
    files.value = res.data.records
    // 从文件列表计算已用空间
    poolUsed.value = files.value.reduce((sum: number, f: any) => sum + f.fileSize, 0)
    // 获取共享池总配额
    const configRes: any = await adminApi.getConfig()
    const poolConfig = configRes.data.find((c: any) => c.configKey === 'total_pool_quota')
    if (poolConfig) poolQuota.value = parseInt(poolConfig.configValue)
  } catch {}
})
</script>

<style scoped>
.page { max-width: 900px; }
.page-title { font-size: 22px; color: #b87b3a; margin-bottom: 20px; }
.capacity-bar {
  position: relative; height: 22px; background: #f0e6d3; border-radius: 11px;
  margin-bottom: 14px; overflow: hidden;
}
.capacity-fill { height: 100%; background: #b87b3a; border-radius: 11px; transition: width 0.5s; }
.capacity-text { position: absolute; top: 50%; left: 50%; transform: translate(-50%,-50%); font-size: 12px; color: #3d2b1f; white-space: nowrap; }
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
