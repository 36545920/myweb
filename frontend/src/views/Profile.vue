<template>
  <div class="page">
    <h2 class="page-title">🔧 个人设置</h2>
    <div class="form">
      <label class="label">邮箱</label>
      <input :value="auth.user?.email" disabled class="input" />
      <label class="label">昵称</label>
      <input v-model="nickname" class="input" />
      <label class="label">新密码（留空不修改）</label>
      <input v-model="newPassword" type="password" class="input" placeholder="新密码" />
      <input v-model="oldPassword" type="password" class="input" placeholder="原密码（修改密码时必填）" />
      <p v-if="msg" :class="msgType">{{ msg }}</p>
      <button @click="save" class="btn-primary">保存</button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { usersApi } from '@/api/users'

const auth = useAuthStore()
const nickname = ref(auth.user?.nickname || '')
const newPassword = ref('')
const oldPassword = ref('')
const msg = ref('')
const msgType = ref('success')

async function save() {
  try {
    const res: any = await usersApi.updateProfile({
      nickname: nickname.value,
      oldPassword: oldPassword.value || undefined,
      newPassword: newPassword.value || undefined
    })
    auth.user = res.data
    msg.value = '保存成功'; msgType.value = 'success'
  } catch (e: any) { msg.value = e.message; msgType.value = 'error' }
}
</script>

<style scoped>
.page { max-width: 480px; }
.page-title { font-size: 22px; color: #b87b3a; margin-bottom: 20px; }
.form { display: flex; flex-direction: column; gap: 8px; }
.label { font-size: 13px; color: #8b7355; margin-top: 8px; }
.input { padding: 12px 16px; border: 1px solid #e8d5c0; border-radius: 8px; font-size: 14px; background: #fefaf2; outline: none; font-family: inherit; }
.input:focus { border-color: #b87b3a; }
.input:disabled { opacity: 0.6; }
.btn-primary { padding: 12px; background: #b87b3a; color: #fff; border: none; border-radius: 8px; font-size: 16px; cursor: pointer; font-family: inherit; margin-top: 12px; }
.success { color: #6b8e23; font-size: 13px; }
.error { color: #c0392b; font-size: 13px; }
</style>
