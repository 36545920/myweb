<template>
  <div class="page">
    <el-table
      :data="users"
      stripe
      style="width: 100%"
      v-loading="loading"
    >
      <el-table-column prop="email" label="邮箱" min-width="200" />
      <el-table-column prop="nickname" label="昵称" width="120" />
      <el-table-column label="角色" width="130">
        <template #default="{ row }">
          <el-tag v-if="row.role === 'SUPER_ADMIN'" type="danger">超级管理员</el-tag>
          <el-tag v-else-if="row.role === 'ADMIN'" type="warning">管理员</el-tag>
          <el-tag v-else type="info">用户</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="配额" width="100">
        <template #default="{ row }">
          {{ formatFileSize(row.storageQuota) }}
        </template>
      </el-table-column>
      <el-table-column label="已用" width="100">
        <template #default="{ row }">
          {{ formatFileSize(row.storageUsed) }}
        </template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag v-if="row.status === 'ACTIVE'" type="success">正常</el-tag>
          <el-tag v-else type="danger">已禁用</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" min-width="260" fixed="right">
        <template #default="{ row }">
          <el-button size="small" @click="openQuota(row)">配额</el-button>
          <el-button
            v-if="row.role !== 'SUPER_ADMIN'"
            size="small"
            :type="row.status === 'ACTIVE' ? 'warning' : 'success'"
            @click="toggleStatus(row)"
          >
            {{ row.status === 'ACTIVE' ? '禁用' : '启用' }}
          </el-button>
          <el-button size="small" type="danger" @click="resetPassword(row)">重置密码</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      v-if="total > pageSize"
      v-model:current-page="currentPage"
      :page-size="pageSize"
      :total="total"
      layout="total, prev, pager, next"
      @current-change="fetchUsers"
      style="margin-top: 16px; justify-content: center"
    />

    <!-- 配额修改弹窗 -->
    <el-dialog
      v-model="quotaDialogVisible"
      title="调整配额"
      width="400px"
    >
      <el-form>
        <el-form-item label="用户">
          <span>{{ quotaUser?.email }}</span>
        </el-form-item>
        <el-form-item label="配额 (GB)">
          <el-input-number
            v-model="quotaGb"
            :min="1"
            :max="10240"
            :step="1"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="quotaDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveQuota">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { adminApi } from '@/api/admin'
import { formatFileSize } from '@/utils/format'

const users = ref<any[]>([])
const loading = ref(false)
const currentPage = ref(1)
const pageSize = ref(20)
const total = ref(0)

const quotaDialogVisible = ref(false)
const quotaUser = ref<any>(null)
const quotaGb = ref(10)

onMounted(() => {
  fetchUsers()
})

async function fetchUsers() {
  loading.value = true
  try {
    const res: any = await adminApi.listUsers(currentPage.value, pageSize.value)
    users.value = res.data.records ?? res.data ?? []
    total.value = res.data.total ?? 0
  } catch {
    ElMessage.error('获取用户列表失败')
  } finally {
    loading.value = false
  }
}

function openQuota(u: any) {
  quotaUser.value = u
  quotaGb.value = Math.round(u.storageQuota / 1073741824)
  quotaDialogVisible.value = true
}

async function saveQuota() {
  try {
    await adminApi.updateQuota(quotaUser.value.email, quotaGb.value * 1073741824)
    quotaDialogVisible.value = false
    ElMessage.success('配额修改成功')
    await fetchUsers()
  } catch {
    ElMessage.error('配额修改失败')
  }
}

async function toggleStatus(u: any) {
  const newStatus = u.status === 'ACTIVE' ? 'DISABLED' : 'ACTIVE'
  const actionLabel = newStatus === 'ACTIVE' ? '启用' : '禁用'
  try {
    await ElMessageBox.confirm(`确定要${actionLabel}用户「${u.email}」吗？`, `确认${actionLabel}`, {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    })
    await adminApi.updateStatus(u.email, newStatus)
    ElMessage.success(`${actionLabel}成功`)
    await fetchUsers()
  } catch {
    // 用户取消
  }
}

async function resetPassword(u: any) {
  try {
    await ElMessageBox.confirm(
      `确定要重置用户「${u.email}」的密码吗？重置后该用户需重新设置密码。`,
      '确认重置密码',
      {
        confirmButtonText: '确定重置',
        cancelButtonText: '取消',
        type: 'warning',
      }
    )
    await adminApi.resetPassword(u.email)
    ElMessage.success('密码已重置')
  } catch {
    // 用户取消
  }
}
</script>

<style scoped>
.page { }
</style>
