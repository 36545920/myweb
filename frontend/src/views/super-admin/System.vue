<template>
  <div class="page">
    <div class="form">
      <div v-for="c in configs" :key="c.configKey" class="row">
        <label>{{ configLabel(c.configKey) }}</label>
        <input v-if="isBytesConfig(c.configKey)" :value="bytesToMb(c.configValue)" @change="e => updateBytes(c.configKey, (e.target as HTMLInputElement).value)" class="input" type="number" step="1" min="1" />
        <input v-else-if="!isReadonly(c.configKey)" :value="pending[c.configKey] !== undefined ? pending[c.configKey] : c.configValue" @change="e => updateValue(c.configKey, (e.target as HTMLInputElement).value)" class="input" />
        <span v-else>{{ c.configValue }}（自动管理）</span>
        <span v-if="isBytesConfig(c.configKey)" class="unit">MB</span>
      </div>
      <p v-if="msg" :class="msgType">{{ msg }}</p>
      <button @click="saveAll" class="btn-primary">保存设置</button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { adminApi } from '@/api/admin'

const configs = ref<any[]>([])
const pending = ref<Record<string, string>>({})
const msg = ref('')
const msgType = ref('success')

const labels: Record<string, string> = {
  total_user_quota: '用户空间总量',
  total_pool_quota: '共享池总量',
  default_user_quota: '新用户默认配额',
  daily_register_limit: '每日注册上限',
  today_register_count: '今日注册数',
  register_date: '计数日期'
}

const bytesConfigs = ['total_user_quota', 'total_pool_quota', 'default_user_quota']

function configLabel(k: string) { return labels[k] || k }
function isBytesConfig(k: string) { return bytesConfigs.includes(k) }
function isReadonly(k: string) { return k === 'today_register_count' || k === 'register_date' }

function bytesToMb(bytes: string): string {
  return (parseInt(bytes) / 1048576).toFixed(0)
}

function updateBytes(k: string, mb: string) {
  const bytes = (parseInt(mb) || 0) * 1048576
  pending.value[k] = String(bytes)
}

function updateValue(k: string, v: string) { pending.value[k] = v }

onMounted(async () => {
  try { const res: any = await adminApi.getConfig(); configs.value = res.data } catch {}
})

async function saveAll() {
  try {
    await adminApi.updateConfig(pending.value)
    pending.value = {}
    msg.value = '保存成功'; msgType.value = 'success'
    const res: any = await adminApi.getConfig(); configs.value = res.data
  } catch (e: any) { msg.value = e.message; msgType.value = 'error' }
}
</script>

<style scoped>
.page { }
.form { display: flex; flex-direction: column; gap: 12px; }
.row { display: flex; justify-content: space-between; align-items: center; gap: 12px; font-size: 14px; }
.row label { color: #5a3a1a; flex: 1; }
.input { padding: 8px 12px; border: 1px solid #e8d5c0; border-radius: 6px; font-size: 13px; background: #fefaf2; outline: none; font-family: inherit; width: 160px; }
.unit { font-size: 12px; color: #8b7355; width: 24px; }
.btn-primary { padding: 10px 20px; background: #b87b3a; color: #fff; border: none; border-radius: 8px; font-size: 14px; cursor: pointer; font-family: inherit; margin-top: 8px; }
.success { color: #6b8e23; font-size: 13px; }
.error { color: #c0392b; font-size: 13px; }
</style>
