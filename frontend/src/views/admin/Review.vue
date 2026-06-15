<template>
  <div class="page">
    <h2 class="page-title">✅ 共享池审核</h2>
    <div v-if="files.length === 0" class="empty">暂无待审核文件</div>
    <div v-for="f in files" :key="f.id" class="card">
      <div class="info">
        <div class="title">{{ f.title }}</div>
        <div class="meta">上传者：{{ f.ownerEmail }} · {{ formatFileSize(f.fileSize) }} · {{ f.originalName }}</div>
        <div class="desc" v-if="f.description">{{ f.description }}</div>
      </div>
      <div class="actions">
        <input v-if="rejectId === f.id" v-model="rejectComment" placeholder="拒绝原因" class="input" />
        <button v-if="rejectId === f.id" @click="doReview(f.id, false)" class="btn-sm btn-danger">确认拒绝</button>
        <button v-if="rejectId !== f.id" @click="doReview(f.id, true)" class="btn-sm btn-approve">通过</button>
        <button v-if="rejectId !== f.id" @click="rejectId = f.id" class="btn-sm btn-danger">拒绝</button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { adminApi } from '@/api/admin'
import { formatFileSize } from '@/utils/format'

const files = ref<any[]>([])
const rejectId = ref<number | null>(null)
const rejectComment = ref('')

onMounted(async () => {
  try { const res: any = await adminApi.reviewList(); files.value = res.data.records } catch {}
})

async function doReview(id: number, approved: boolean) {
  await adminApi.reviewFile(id, approved, approved ? '' : rejectComment.value)
  rejectId.value = null
  rejectComment.value = ''
  files.value = files.value.filter(f => f.id !== id)
}
</script>

<style scoped>
.page { max-width: 800px; }
.page-title { font-size: 22px; color: #b87b3a; margin-bottom: 20px; }
.empty { text-align: center; padding: 60px 0; color: #8b7355; }
.card {
  display: flex; justify-content: space-between; align-items: center; gap: 16px;
  background: #fff; border: 1px solid #f0e6d3; border-radius: 10px; padding: 16px 20px; margin-bottom: 8px;
}
.info { flex: 1; }
.title { font-size: 15px; color: #3d2b1f; font-weight: 600; }
.meta { font-size: 12px; color: #aaa; margin-top: 2px; }
.desc { font-size: 13px; color: #8b7355; margin-top: 4px; }
.actions { display: flex; gap: 6px; align-items: center; }
.input { padding: 6px 10px; border: 1px solid #e8d5c0; border-radius: 4px; font-size: 12px; background: #fefaf2; outline: none; font-family: inherit; width: 140px; }
.btn-sm { padding: 6px 12px; border: 1px solid #d4b896; background: #fff; border-radius: 6px; cursor: pointer; font-size: 12px; font-family: inherit; }
.btn-approve { border-color: #c0d4b8; color: #6b8e23; }
.btn-danger { border-color: #e0c8c8; color: #c0392b; }
</style>
