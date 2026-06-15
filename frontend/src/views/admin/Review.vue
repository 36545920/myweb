<template>
  <div class="page">
    <div v-if="!loading && files.length === 0" class="empty">暂无待审核文件</div>

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
      <el-table-column label="状态" width="120">
        <template #default="{ row }">
          <el-tag v-if="row.status === 'APPROVED'" type="success">已通过</el-tag>
          <el-tag v-else-if="row.status === 'REJECTED'" type="danger">已拒绝</el-tag>
          <el-tag v-else type="warning">待审核</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="{ row }">
          <template v-if="row.status === 'PENDING' || !row.status">
            <el-button type="success" size="small" @click="doApprove(row)">通过</el-button>
            <el-button type="danger" size="small" @click="doReject(row)">拒绝</el-button>
          </template>
          <template v-else>
            <span style="color: #bbb; font-size: 12px">已处理</span>
          </template>
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
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { adminApi } from '@/api/admin'
import { formatFileSize } from '@/utils/format'

const files = ref<any[]>([])
const loading = ref(false)
const currentPage = ref(1)
const pageSize = ref(20)
const total = ref(0)

onMounted(() => {
  fetchFiles()
})

async function fetchFiles() {
  loading.value = true
  try {
    const res: any = await adminApi.reviewList(currentPage.value, pageSize.value)
    files.value = res.data.records ?? res.data ?? []
    total.value = res.data.total ?? 0
  } catch {
    ElMessage.error('获取审核列表失败')
  } finally {
    loading.value = false
  }
}

async function doApprove(row: any) {
  try {
    await ElMessageBox.confirm(`确定要通过文件「${row.title}」的审核吗？`, '确认通过', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'info',
    })
    await adminApi.reviewFile(row.id, true, '')
    files.value = files.value.filter(f => f.id !== row.id)
    total.value--
    ElMessage.success('审核通过')
  } catch {
    // 用户取消
  }
}

async function doReject(row: any) {
  try {
    const { value } = await ElMessageBox.prompt('请输入拒绝原因', '拒绝审核', {
      confirmButtonText: '确认拒绝',
      cancelButtonText: '取消',
      inputPlaceholder: '拒绝原因',
      inputType: 'textarea',
    })
    if (value !== null) {
      await adminApi.reviewFile(row.id, false, value || '')
      files.value = files.value.filter(f => f.id !== row.id)
      total.value--
      ElMessage.success('已拒绝该文件')
    }
  } catch {
    // 用户取消或关闭
  }
}
</script>

<style scoped>
.page { }
.empty { text-align: center; padding: 60px 0; color: #8b7355; }
</style>
