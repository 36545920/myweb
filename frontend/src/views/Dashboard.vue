<template>
  <div class="page">
    <h2 class="page-title">📊 仪表盘</h2>
    <div class="cards">
      <div class="card">
        <div class="card-title">👤 {{ auth.user?.nickname || '用户' }}</div>
        <div class="card-role">{{ roleLabel }}</div>
      </div>
      <div class="card">
        <div class="card-title">💾 存储空间</div>
        <div class="bar-bg"><div class="bar" :style="{ width: usagePercent + '%' }"></div></div>
        <div class="card-detail">{{ usedStr }} / {{ quotaStr }}</div>
      </div>
    </div>
    <div class="actions">
      <router-link to="/files/upload" class="btn-primary">📤 上传文件</router-link>
      <router-link to="/inbox" class="btn-secondary">📥 查看收件箱</router-link>
      <router-link to="/pool" class="btn-secondary">📦 浏览共享池</router-link>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { usersApi } from '@/api/users'
import { formatFileSize } from '@/utils/format'

const auth = useAuthStore()
const used = computed(() => auth.user?.storageUsed || 0)
const quota = computed(() => auth.user?.storageQuota || 0)
const usedStr = computed(() => formatFileSize(used.value))
const quotaStr = computed(() => formatFileSize(quota.value))
const usagePercent = computed(() => quota.value > 0 ? Math.min(100, (used.value / quota.value) * 100) : 0)
const roleLabel = computed(() => {
  const map: Record<string, string> = { USER: '普通用户', ADMIN: '管理员', SUPER_ADMIN: '超级管理员' }
  return map[auth.user?.role] || '用户'
})

onMounted(async () => {
  try { const res: any = await usersApi.getProfile(); auth.user = res.data } catch {}
})
</script>

<style scoped>
.page { max-width: 800px; }
.page-title { font-size: 22px; color: #b87b3a; margin-bottom: 20px; }
.cards { display: flex; gap: 16px; margin-bottom: 24px; }
.card {
  flex: 1; background: #fff; border: 1px solid #f0e6d3;
  border-radius: 12px; padding: 20px;
}
.card-title { font-size: 15px; color: #3d2b1f; margin-bottom: 8px; }
.card-role { color: #b87b3a; font-size: 13px; }
.card-detail { font-size: 13px; color: #8b7355; margin-top: 6px; }
.bar-bg { height: 8px; background: #f0e6d3; border-radius: 4px; margin: 8px 0; }
.bar { height: 8px; background: #b87b3a; border-radius: 4px; transition: width 0.5s; }
.actions { display: flex; gap: 12px; }
.btn-primary, .btn-secondary {
  padding: 10px 20px; border-radius: 8px; text-decoration: none; font-size: 14px;
  display: inline-block; font-family: inherit;
}
.btn-primary { background: #b87b3a; color: #fff; }
.btn-primary:hover { background: #9e672f; }
.btn-secondary { background: #fff; color: #b87b3a; border: 1px solid #d4b896; }
.btn-secondary:hover { background: #fefaf2; }
</style>
