<template>
  <div class="page">
    <div v-if="!loading && items.length === 0" class="empty">暂无收到文件</div>

    <el-table
      v-else
      :data="items"
      stripe
      style="width: 100%"
      v-loading="loading"
    >
      <el-table-column prop="fromEmail" label="发送者" min-width="180" />
      <el-table-column prop="message" label="留言" min-width="200">
        <template #default="{ row }">
          <span v-if="row.message">{{ row.message }}</span>
          <span v-else style="color: #bbb">-</span>
        </template>
      </el-table-column>
      <el-table-column label="时间" width="180">
        <template #default="{ row }">
          {{ formatDate(row.createdAt) }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="180" fixed="right">
        <template #default="{ row }">
          <a :href="filesApi.downloadUrl(row.fileId)" class="btn-sm">下载</a>
          <el-button type="danger" size="small" @click="doDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      v-if="total > pageSize"
      v-model:current-page="currentPage"
      :page-size="pageSize"
      :total="total"
      layout="total, prev, pager, next"
      @current-change="fetchItems"
      style="margin-top: 16px; justify-content: center"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { transfersApi } from '@/api/transfers'
import { filesApi } from '@/api/files'
import { formatDate } from '@/utils/format'

const items = ref<any[]>([])
const loading = ref(false)
const currentPage = ref(1)
const pageSize = ref(20)
const total = ref(0)

onMounted(() => {
  fetchItems()
})

async function fetchItems() {
  loading.value = true
  try {
    const res: any = await transfersApi.inbox(currentPage.value, pageSize.value)
    items.value = res.data.records ?? res.data ?? []
    total.value = res.data.total ?? 0
  } catch {
    ElMessage.error('获取收件箱数据失败')
  } finally {
    loading.value = false
  }
}

async function doDelete(t: any) {
  try {
    await ElMessageBox.confirm('确定要删除这条收件记录吗？', '确认删除', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    })
    await transfersApi.remove(t.id)
    items.value = items.value.filter(i => i.id !== t.id)
    total.value--
    ElMessage.success('删除成功')
  } catch {
    // 用户取消操作，忽略
  }
}
</script>

<style scoped>
.page { }
.empty { text-align: center; padding: 60px 0; color: #8b7355; font-size: 15px; }
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
  margin-right: 6px;
  display: inline-block;
}
</style>
