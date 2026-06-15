<template>
  <div class="page">
    <div class="header">
      <el-button type="primary" @click="showAdd = true">
        <el-icon style="margin-right:4px"><Plus /></el-icon>
        添加好友
      </el-button>
    </div>

    <!-- 好友列表表格 -->
    <el-table
      :data="friends"
      style="width: 100%"
      size="default"
      empty-text="暂无好友，搜索邮箱添加好友"
      v-loading="loading"
    >
      <el-table-column label="好友邮箱" min-width="300">
        <template #default="{ row }">
          <span class="friend-email">{{ row.friendEmail }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="120" align="center">
        <template #default="{ row }">
          <el-button
            type="danger"
            size="small"
            text
            @click="removeFriend(row)"
          >
            删除
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 添加好友弹窗 -->
    <el-dialog
      v-model="showAdd"
      title="添加好友"
      width="420px"
      :close-on-click-modal="true"
      destroy-on-close
    >
      <el-input
        v-model="searchQuery"
        placeholder="输入邮箱搜索"
        size="large"
        clearable
        @input="onSearch"
      >
        <template #prefix>
          <el-icon><Search /></el-icon>
        </template>
      </el-input>
      <div v-if="searchResults.length" class="search-results">
        <div v-for="u in searchResults" :key="u.email" class="result-item">
          <span class="result-email">{{ u.email }}</span>
          <el-button
            type="primary"
            size="small"
            @click="addFriend(u.email)"
          >
            添加
          </el-button>
        </div>
      </div>
      <div v-else-if="searchQuery && searched" class="search-empty">
        未找到匹配用户
      </div>
      <template #footer>
        <el-button @click="showAdd = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { friendsApi } from '@/api/friends'
import { usersApi } from '@/api/users'
import { ElMessage, ElMessageBox } from 'element-plus'

const friends = ref<any[]>([])
const loading = ref(false)
const showAdd = ref(false)
const searchQuery = ref('')
const searchResults = ref<any[]>([])
const searched = ref(false)
let searchTimer: any = null

onMounted(async () => {
  loading.value = true
  try {
    const res: any = await friendsApi.list()
    friends.value = res.data
  } catch {
    /* ignore */
  } finally {
    loading.value = false
  }
})

function onSearch() {
  clearTimeout(searchTimer)
  searched.value = false
  searchTimer = setTimeout(async () => {
    if (!searchQuery.value) {
      searchResults.value = []
      return
    }
    try {
      const res: any = await usersApi.search(searchQuery.value)
      searchResults.value = res.data
      searched.value = true
    } catch {
      searchResults.value = []
      searched.value = true
    }
  }, 300)
}

async function addFriend(email: string) {
  try {
    await friendsApi.add(email)
    ElMessage.success('好友请求已发送')
    showAdd.value = false
  } catch (e: any) {
    ElMessage.error(e.message || '添加失败')
  }
}

async function removeFriend(f: any) {
  try {
    await ElMessageBox.confirm(
      `确定删除好友 ${f.friendEmail} 吗？`,
      '确认删除',
      { confirmButtonText: '删除', cancelButtonText: '取消', type: 'warning' }
    )
    await friendsApi.remove(f.friendEmail)
    friends.value = friends.value.filter(x => x.id !== f.id)
    ElMessage.success('已删除')
  } catch {
    // 取消操作
  }
}
</script>

<style scoped>
.page {
  max-width: 680px;
}
.header {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  margin-bottom: 20px;
}
.friend-email {
  color: var(--text);
  font-size: 14px;
}
.search-results {
  margin-top: 12px;
  max-height: 240px;
  overflow-y: auto;
}
.result-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 0;
  border-bottom: 1px solid var(--border);
}
.result-item:last-child {
  border-bottom: none;
}
.result-email {
  font-size: 13px;
  color: var(--text);
}
.search-empty {
  margin-top: 12px;
  text-align: center;
  color: var(--text-muted);
  font-size: 13px;
}
</style>
