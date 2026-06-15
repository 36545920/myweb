<template>
  <div class="page">
    <h2>仪表盘</h2>

    <div class="cards">
      <div class="card welcome-card">
        <div class="welcome-avatar">{{ initial }}</div>
        <div class="welcome-info">
          <div class="welcome-name">{{ auth.user?.nickname || '用户' }}</div>
          <div class="welcome-role">{{ roleLabel }}</div>
        </div>
      </div>

      <div class="card storage-card">
        <div class="storage-header">
          <span class="storage-label">存储空间</span>
          <span class="storage-value">{{ usedStr }} / {{ quotaStr }}</span>
        </div>
        <div class="bar-track">
          <div class="bar-fill" :style="{ width: usagePercent + '%' }"></div>
        </div>
      </div>
    </div>

    <div class="quick-actions">
      <h3>快捷操作</h3>
      <div class="action-grid">
        <router-link to="/files/upload" class="action-card">
          <span class="action-icon">📤</span>
          <span class="action-text">上传文件</span>
        </router-link>
        <router-link to="/inbox" class="action-card">
          <span class="action-icon">📥</span>
          <span class="action-text">收件箱</span>
        </router-link>
        <router-link to="/pool" class="action-card">
          <span class="action-icon">📦</span>
          <span class="action-text">共享池</span>
        </router-link>
        <router-link to="/friends" class="action-card">
          <span class="action-icon">👥</span>
          <span class="action-text">好友</span>
        </router-link>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { usersApi } from '@/api/users'
import { formatFileSize } from '@/utils/format'

const auth = useAuthStore()
const initial = computed(() => (auth.user?.nickname || 'U')[0])
const used = computed(() => auth.user?.storageUsed || 0)
const quota = computed(() => auth.user?.storageQuota || 1)
const usedStr = computed(() => formatFileSize(used.value))
const quotaStr = computed(() => formatFileSize(quota.value))
const usagePercent = computed(() => Math.min(100, (used.value / quota.value) * 100))
const roleLabel = computed(() => {
  const map: Record<string, string> = { USER: '普通用户', ADMIN: '管理员', SUPER_ADMIN: '超级管理员' }
  return map[auth.user?.role] || ''
})

onMounted(async () => {
  try { const res: any = await usersApi.getProfile(); auth.user = res.data } catch {}
})
</script>

<style scoped>
.page { max-width: 720px; }

.cards { display: flex; gap: 16px; margin-bottom: 32px; }

.welcome-card {
  display: flex; align-items: center; gap: 16px; padding: 24px;
}
.welcome-avatar {
  width: 48px; height: 48px; border-radius: 50%;
  background: var(--primary-light); color: var(--primary);
  display: flex; align-items: center; justify-content: center;
  font-size: 20px; font-weight: 700; flex-shrink: 0;
}
.welcome-name { font-size: 16px; font-weight: 600; }
.welcome-role { font-size: 13px; color: var(--text-secondary); margin-top: 2px; }

.storage-card { padding: 20px 24px; flex: 1.5; }
.storage-header { display: flex; justify-content: space-between; margin-bottom: 12px; }
.storage-label { font-size: 14px; font-weight: 600; }
.storage-value { font-size: 13px; color: var(--text-secondary); }
.bar-track { height: 10px; background: var(--primary-light); border-radius: 5px; overflow: hidden; }
.bar-fill {
  height: 100%; background: linear-gradient(90deg, var(--primary), #d4a85c);
  border-radius: 5px; transition: width 0.6s ease;
}

.quick-actions { margin-top: 8px; }
.action-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 12px; margin-top: 12px; }
.action-card {
  display: flex; flex-direction: column; align-items: center; gap: 8px;
  padding: 24px 16px; background: var(--bg-card); border: 1px solid var(--border);
  border-radius: var(--radius); text-decoration: none; cursor: pointer;
  transition: all 0.15s; box-shadow: var(--shadow-sm);
}
.action-card:hover { border-color: var(--primary-light); background: var(--bg-card-hover); box-shadow: var(--shadow-md); }
.action-icon { font-size: 28px; }
.action-text { font-size: 13px; color: var(--text-secondary); font-weight: 500; }
</style>
