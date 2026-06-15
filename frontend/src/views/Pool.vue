<template>
  <div class="page">
    <div class="storage-info">
      <span>共享池：{{ formatFileSize(poolUsed) }} / {{ formatFileSize(poolQuota) }}</span>
      <div class="bar storage-bar">
        <div class="bar-fill" :style="{ width: usagePercent + '%' }"></div>
      </div>
    </div>

    <div v-if="!loading && files.length === 0" class="empty">共享池暂无文件</div>

    <el-table
      v-else
      :data="files"
      stripe
      style="width: 100%"
      v-loading="loading"
    >
      <el-table-column prop="title" label="标题" min-width="180" />
      <el-table-column prop="ownerEmail" label="上传者" width="180" />
      <el-table-column label="大小" width="100">
        <template #default="{ row }">
          {{ formatFileSize(row.fileSize) }}
        </template>
      </el-table-column>
      <el-table-column label="上传日期" width="170">
        <template #default="{ row }">
          {{ formatDate(row.createdAt) }}
        </template>
      </el-table-column>
      <el-table-column label="过期时间" width="170">
        <template #default="{ row }">
          {{ formatDate(row.expireAt) }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="100" fixed="right">
        <template #default="{ row }">
          <a :href="filesApi.downloadUrl(row.id)" class="btn-sm">下载</a>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      v-if="total > pageSize"
      v-model:current-page="currentPage"
      :page-size="pageSize"
      :total="total"
      layout="total, prev, pager, next"
      @current-change="fetchFiles"
      style="margin-top: 16px; justify-content: center"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { adminApi } from '@/api/admin'
import { filesApi } from '@/api/files'
import { formatFileSize, formatDate } from '@/utils/format'

const files = ref<any[]>([])
const loading = ref(false)
const currentPage = ref(1)
const pageSize = ref(20)
const total = ref(0)
const poolQuota = ref(0)
const poolUsed = ref(0)
const usagePercent = computed(() => poolQuota.value > 0 ? Math.min(100, (poolUsed.value / poolQuota.value) * 100) : 0)

onMounted(() => {
  fetchFiles()
  fetchConfig()
})

async function fetchFiles() {
  loading.value = true
  try {
    const res: any = await adminApi.poolList(currentPage.value, pageSize.value)
    files.value = res.data.records ?? res.data ?? []
    total.value = res.data.total ?? 0
    poolUsed.value = files.value.reduce((sum: number, f: any) => sum + f.fileSize, 0)
  } catch {
    ElMessage.error('获取共享池文件失败')
  } finally {
    loading.value = false
  }
}

async function fetchConfig() {
  try {
    const configRes: any = await adminApi.getConfig()
    const poolConfig = configRes.data.find((c: any) => c.configKey === 'total_pool_quota')
    if (poolConfig) poolQuota.value = parseInt(poolConfig.configValue)
  } catch {
    // ignore
  }
}
</script>

<style scoped>
.page { }
.storage-info {
  background: var(--bg-card); border: 1px solid var(--border);
  border-radius: var(--radius); padding: 14px 20px;
  margin-bottom: 20px; box-shadow: var(--shadow-sm);
}
.storage-info span { font-size: 13px; font-weight: 600; color: var(--text-secondary); }
.storage-bar { margin-top: 8px; }
.empty { text-align: center; padding: 60px 0; color: #8b7355; }
.btn-sm {
  padding: 5px 10px;
  border: 1px solid #d4b896;
  background: #fff;
  border-radius: 6px;
  cursor: pointer;
  font-size: 12px;
  color: #5a3a1a;
  text-decoration: none;
  font-family: inherit;
  display: inline-block;
}
</style>
