<template>
  <div class="page">
    <div class="cards">
      <!-- 用户卡片 -->
      <el-card class="welcome-card" shadow="hover">
        <div class="welcome-inner">
          <el-avatar :size="48" class="welcome-avatar">
            {{ initial }}
          </el-avatar>
          <div class="welcome-info">
            <div class="welcome-name">{{ auth.user?.nickname || '用户' }}</div>
            <div class="welcome-role">{{ roleLabel }}</div>
          </div>
        </div>
      </el-card>

      <!-- 存储空间 -->
      <el-card class="storage-card" shadow="hover">
        <div class="storage-header">
          <span class="storage-label">存储空间</span>
          <span class="storage-value">{{ usedStr }} / {{ quotaStr }}</span>
        </div>
        <el-progress
          :percentage="usagePercent"
          :stroke-width="12"
          :show-text="false"
          color="#b87b3a"
          :define-back-color="'#f0e6d3'"
        />
      </el-card>
    </div>

    <!-- 最近操作记录 -->
    <el-card class="recent-card" shadow="hover">
      <template #header>
        <span class="card-title">最近操作</span>
      </template>
      <el-table
        :data="recentFiles"
        style="width: 100%"
        size="default"
        empty-text="暂无最近操作记录"
        v-loading="recentLoading"
      >
        <el-table-column prop="title" label="文件名" min-width="200">
          <template #default="{ row }">
            <span class="file-name">{{ row.title }}</span>
          </template>
        </el-table-column>
        <el-table-column label="大小" width="120" align="right">
          <template #default="{ row }">
            {{ formatFileSize(row.fileSize) }}
          </template>
        </el-table-column>
        <el-table-column label="时间" width="180" align="right">
          <template #default="{ row }">
            {{ formatDate(row.createdAt) }}
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 快捷入口 -->
    <div class="quick-actions">
      <h3>快捷操作</h3>
      <div class="action-grid">
        <router-link to="/files/upload" class="action-card">
          <el-icon :size="28" color="#b87b3a"><Upload /></el-icon>
          <span class="action-text">上传文件</span>
        </router-link>
        <router-link to="/inbox" class="action-card">
          <el-icon :size="28" color="#b87b3a"><Message /></el-icon>
          <span class="action-text">收件箱</span>
        </router-link>
        <router-link to="/pool" class="action-card">
          <el-icon :size="28" color="#b87b3a"><Box /></el-icon>
          <span class="action-text">共享池</span>
        </router-link>
        <router-link to="/friends" class="action-card">
          <el-icon :size="28" color="#b87b3a"><UserFilled /></el-icon>
          <span class="action-text">好友</span>
        </router-link>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { usersApi } from '@/api/users'
import { filesApi } from '@/api/files'
import { formatFileSize, formatDate } from '@/utils/format'
import { ElMessage } from 'element-plus'

const auth = useAuthStore()
const initial = computed(() => (auth.user?.nickname || 'U')[0])
const used = computed(() => auth.user?.storageUsed || 0)
const quota = computed(() => auth.user?.storageQuota || 1)
const usedStr = computed(() => formatFileSize(used.value))
const quotaStr = computed(() => formatFileSize(quota.value))
const usagePercent = computed(() => Math.min(100, Math.round((used.value / quota.value) * 100)))
const roleLabel = computed(() => {
  const map: Record<string, string> = { USER: '普通用户', ADMIN: '管理员', SUPER_ADMIN: '超级管理员' }
  return map[auth.user?.role] || ''
})

const recentFiles = ref<any[]>([])
const recentLoading = ref(false)

onMounted(async () => {
  // 刷新用户信息
  try {
    const res: any = await usersApi.getProfile()
    auth.user = res.data
  } catch { /* ignore */ }

  // 加载最近文件
  recentLoading.value = true
  try {
    const res: any = await filesApi.listRecent(10)
    recentFiles.value = res.data || []
  } catch {
    ElMessage.warning('加载最近记录失败')
  } finally {
    recentLoading.value = false
  }
})
</script>

<style scoped>
.page {
  max-width: 800px;
}
.cards {
  display: flex;
  gap: 16px;
  margin-bottom: 24px;
}
.welcome-card {
  flex: 1;
}
.welcome-card :deep(.el-card__body) {
  padding: 24px;
}
.welcome-inner {
  display: flex;
  align-items: center;
  gap: 16px;
}
.welcome-avatar {
  background: var(--primary-light);
  color: var(--primary);
  font-size: 20px;
  font-weight: 700;
  flex-shrink: 0;
}
.welcome-name {
  font-size: 16px;
  font-weight: 600;
  color: var(--text);
}
.welcome-role {
  font-size: 13px;
  color: var(--text-secondary);
  margin-top: 2px;
}
.storage-card {
  flex: 1.5;
}
.storage-card :deep(.el-card__body) {
  padding: 20px 24px;
}
.storage-header {
  display: flex;
  justify-content: space-between;
  margin-bottom: 14px;
}
.storage-label {
  font-size: 14px;
  font-weight: 600;
  color: var(--text);
}
.storage-value {
  font-size: 13px;
  color: var(--text-secondary);
}
.recent-card {
  margin-bottom: 24px;
}
.card-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--text);
}
.file-name {
  color: var(--text);
}
.quick-actions {
  margin-top: 8px;
}
.quick-actions h3 {
  margin-bottom: 14px;
}
.action-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
}
.action-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  padding: 24px 16px;
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: var(--radius);
  text-decoration: none;
  cursor: pointer;
  transition: all 0.15s;
  box-shadow: var(--shadow-sm);
}
.action-card:hover {
  border-color: var(--primary-light);
  background: var(--bg-card-hover);
  box-shadow: var(--shadow-md);
}
.action-text {
  font-size: 13px;
  color: var(--text-secondary);
  font-weight: 500;
}
</style>
