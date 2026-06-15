<template>
  <div class="page">
    <h2 class="page-title">👤 用户管理</h2>
    <table class="table">
      <thead><tr><th>邮箱</th><th>昵称</th><th>角色</th><th>配额</th><th>已用</th><th>状态</th><th>操作</th></tr></thead>
      <tbody>
        <tr v-for="u in users" :key="u.email">
          <td>{{ u.email }}</td><td>{{ u.nickname }}</td><td>{{ u.role }}</td>
          <td>{{ formatFileSize(u.storageQuota) }}</td>
          <td>{{ formatFileSize(u.storageUsed) }}</td>
          <td>{{ u.status }}</td>
          <td class="ops">
            <button @click="openQuota(u)" class="btn-xs">配额</button>
            <button v-if="u.role !== 'SUPER_ADMIN'" @click="toggleStatus(u)" class="btn-xs">
              {{ u.status === 'ACTIVE' ? '禁用' : '启用' }}
            </button>
          </td>
        </tr>
      </tbody>
    </table>

    <div v-if="quotaUser" class="modal-overlay" @click.self="quotaUser = null">
      <div class="modal">
        <h3>调整配额 — {{ quotaUser.email }}</h3>
        <input v-model.number="quotaGb" type="number" placeholder="GB" class="input" />
        <div class="modal-btns">
          <button @click="quotaUser = null" class="btn-secondary">取消</button>
          <button @click="saveQuota" class="btn-primary">保存</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { adminApi } from '@/api/admin'
import { formatFileSize } from '@/utils/format'

const users = ref<any[]>([])
const quotaUser = ref<any>(null)
const quotaGb = ref(10)

onMounted(async () => {
  try { const res: any = await adminApi.listUsers(); users.value = res.data.records } catch {}
})

function openQuota(u: any) { quotaUser.value = u; quotaGb.value = Math.round(u.storageQuota / 1073741824) }
async function saveQuota() {
  await adminApi.updateQuota(quotaUser.value.email, quotaGb.value * 1073741824)
  quotaUser.value = null
  const res: any = await adminApi.listUsers(); users.value = res.data.records
}
async function toggleStatus(u: any) {
  const newStatus = u.status === 'ACTIVE' ? 'DISABLED' : 'ACTIVE'
  await adminApi.updateStatus(u.email, newStatus)
  const res: any = await adminApi.listUsers(); users.value = res.data.records
}
</script>

<style scoped>
.page { max-width: 100%; overflow-x: auto; }
.page-title { font-size: 22px; color: #b87b3a; margin-bottom: 20px; }
.table { width: 100%; border-collapse: collapse; font-size: 13px; }
.table th { text-align: left; padding: 10px 12px; background: #fefaf2; border-bottom: 2px solid #f0e6d3; color: #8b7355; }
.table td { padding: 10px 12px; border-bottom: 1px solid #f0e6d3; }
.ops { display: flex; gap: 4px; }
.btn-xs { padding: 4px 8px; border: 1px solid #d4b896; background: #fff; border-radius: 4px; cursor: pointer; font-size: 11px; font-family: inherit; }
.modal-overlay { position: fixed; inset: 0; background: rgba(0,0,0,0.3); display: flex; align-items: center; justify-content: center; z-index: 100; }
.modal { background: #fff; padding: 24px; border-radius: 12px; width: 360px; display: flex; flex-direction: column; gap: 12px; }
.modal h3 { color: #b87b3a; }
.input { padding: 10px; border: 1px solid #e8d5c0; border-radius: 6px; font-size: 14px; background: #fefaf2; outline: none; font-family: inherit; }
.modal-btns { display: flex; gap: 8px; justify-content: flex-end; }
.btn-primary { padding: 8px 16px; background: #b87b3a; color: #fff; border: none; border-radius: 6px; cursor: pointer; font-family: inherit; }
.btn-secondary { padding: 8px 16px; border: 1px solid #d4b896; background: #fff; border-radius: 6px; cursor: pointer; font-family: inherit; }
</style>
