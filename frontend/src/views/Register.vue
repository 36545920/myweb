<template>
  <div class="auth-page">
    <div class="auth-card">
      <h1 class="title">🍃 注册 MyWeb</h1>
      <p class="subtitle">创建你的账号，开始文件共享</p>
      <form @submit.prevent="handleRegister" class="form">
        <input v-model="email" type="email" placeholder="邮箱" class="input" required />
        <div class="code-row">
          <input v-model="code" placeholder="验证码" class="input code-input" required maxlength="6" />
          <button type="button" class="code-btn" :disabled="countdown > 0" @click="sendCode">
            {{ countdown > 0 ? countdown + 's' : '发送验证码' }}
          </button>
        </div>
        <input v-model="nickname" placeholder="昵称" class="input" required />
        <input v-model="password" type="password" placeholder="密码（至少6位）" class="input" required />
        <input v-model="password2" type="password" placeholder="确认密码" class="input" required />
        <p v-if="error" class="error">{{ error }}</p>
        <p v-if="success" class="success">{{ success }}</p>
        <button type="submit" class="btn-primary" :disabled="loading">
          {{ loading ? '注册中...' : '注册' }}
        </button>
      </form>
      <p class="link-text">已有账号？<router-link to="/login" class="link">立即登录</router-link></p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { authApi } from '@/api/auth'

const router = useRouter()
const auth = useAuthStore()
const email = ref('')
const code = ref('')
const nickname = ref('')
const password = ref('')
const password2 = ref('')
const error = ref('')
const success = ref('')
const loading = ref(false)
const countdown = ref(0)

async function sendCode() {
  try {
    await authApi.sendCode(email.value)
    countdown.value = 60
    const timer = setInterval(() => { countdown.value--; if (countdown.value <= 0) clearInterval(timer) }, 1000)
    success.value = '验证码已发送'
  } catch (e: any) {
    error.value = e.message || '发送失败'
  }
}

async function handleRegister() {
  error.value = ''
  if (password.value !== password2.value) { error.value = '两次密码不一致'; return }
  loading.value = true
  try {
    const res: any = await authApi.register(
      { email: email.value, password: password.value, nickname: nickname.value },
      code.value
    )
    auth.setTokens(res.data.accessToken, res.data.refreshToken, res.data.user)
    router.push('/')
  } catch (e: any) {
    error.value = e.message || '注册失败'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.auth-page {
  min-height: 100vh; display: flex; align-items: center; justify-content: center;
  background: linear-gradient(135deg, #fefaf2 0%, #faf3e6 50%, #f5ead6 100%);
}
.auth-card {
  background: #fff; padding: 48px 40px; border-radius: 16px;
  box-shadow: 0 8px 32px rgba(139, 107, 21, 0.08);
  width: 420px; max-width: 90vw;
}
.title { font-size: 28px; color: #b87b3a; text-align: center; margin-bottom: 4px; }
.subtitle { text-align: center; color: #8b7355; font-size: 14px; margin-bottom: 24px; }
.form { display: flex; flex-direction: column; gap: 12px; }
.input {
  padding: 12px 16px; border: 1px solid #e8d5c0; border-radius: 8px;
  font-size: 14px; background: #fefaf2; color: #3d2b1f; outline: none;
}
.input:focus { border-color: #b87b3a; }
.code-row { display: flex; gap: 8px; }
.code-input { flex: 1; }
.code-btn {
  padding: 0 14px; background: #f0e6d3; color: #6b4e16; border: 1px solid #d4b896;
  border-radius: 8px; font-size: 13px; cursor: pointer; white-space: nowrap; font-family: inherit;
}
.code-btn:hover { background: #e8d5c0; }
.code-btn:disabled { opacity: 0.5; cursor: not-allowed; }
.error { color: #c0392b; font-size: 13px; text-align: center; }
.success { color: #6b8e23; font-size: 13px; text-align: center; }
.btn-primary {
  padding: 12px; background: #b87b3a; color: #fff; border: none;
  border-radius: 8px; font-size: 16px; cursor: pointer; font-family: inherit;
}
.btn-primary:hover { background: #9e672f; }
.btn-primary:disabled { opacity: 0.6; cursor: not-allowed; }
.link-text { text-align: center; margin-top: 14px; font-size: 13px; color: #8b7355; }
.link { color: #b87b3a; text-decoration: none; }
</style>
