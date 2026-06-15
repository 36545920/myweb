<template>
  <div class="page">
    <h2 class="page-title">📥 收件箱</h2>
    <div v-if="items.length === 0" class="empty">暂无收到文件</div>
    <div class="list">
      <div v-for="t in items" :key="t.id" class="item">
        <div class="item-info">
          <div class="item-from">来自：{{ t.fromEmail }}</div>
          <div class="item-msg" v-if="t.message">留言：{{ t.message }}</div>
          <div class="item-time">{{ formatDate(t.createdAt) }}</div>
        </div>
        <div class="item-actions">
          <a :href="filesApi.downloadUrl(t.fileId)" class="btn-sm">⬇ 下载</a>
          <button @click="doDelete(t)" class="btn-sm btn-danger">删除</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { transfersApi } from '@/api/transfers'
import { filesApi } from '@/api/files'
import { formatDate } from '@/utils/format'

const items = ref<any[]>([])
onMounted(async () => {
  try { const res: any = await transfersApi.inbox(); items.value = res.data.records } catch {}
})
async function doDelete(t: any) {
  await transfersApi.remove(t.id)
  items.value = items.value.filter(i => i.id !== t.id)
}
</script>

<style scoped>
.page { max-width: 800px; }
.page-title { font-size: 22px; color: #b87b3a; margin-bottom: 20px; }
.empty { text-align: center; padding: 60px 0; color: #8b7355; font-size: 15px; }
.list { display: flex; flex-direction: column; gap: 8px; }
.item {
  display: flex; justify-content: space-between; align-items: center;
  background: #fff; border: 1px solid #f0e6d3; border-radius: 10px; padding: 14px 18px;
}
.item-from { font-size: 14px; color: #3d2b1f; }
.item-msg { font-size: 12px; color: #8b7355; margin-top: 2px; }
.item-time { font-size: 11px; color: #bbb; margin-top: 4px; }
.item-actions { display: flex; gap: 6px; }
.btn-sm {
  padding: 6px 12px; border: 1px solid #d4b896; background: #fff;
  border-radius: 6px; cursor: pointer; font-size: 12px; color: #5a3a1a;
  text-decoration: none; font-family: inherit;
}
.btn-danger { border-color: #e0c8c8; color: #c0392b; }
</style>
