<template>
  <div class="auth-page">
    <div class="auth-card">
      <h1 class="title">🍃 MyWeb</h1>
      <p class="subtitle">登录你的账号</p>
      <el-form @submit.prevent="handleLogin" label-position="top" class="form">
        <el-input
          v-model="email"
          type="email"
          placeholder="邮箱"
          size="large"
          clearable
        />
        <el-input
          v-model="password"
          type="password"
          placeholder="密码"
          size="large"
          show-password
          @keyup.enter="handleLogin"
        />
        <el-button
          type="primary"
          size="large"
          :loading="loading"
          @click="handleLogin"
          class="submit-btn"
        >
          {{ loading ? '登录中...' : '登录' }}
        </el-button>
      </el-form>
      <p class="link-text">
        还没有账号？<router-link to="/register" class="link">立即注册</router-link>
      </p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { authApi } from '@/api/auth'
import { ElMessage } from 'element-plus'

const router = useRouter()
const route = useRoute()
const auth = useAuthStore()
const email = ref('')
const password = ref('')
const loading = ref(false)

async function handleLogin() {
  loading.value = true
  try {
    const res: any = await authApi.login({ email: email.value, password: password.value })
    auth.setTokens(res.data.accessToken, res.data.refreshToken, res.data.user)
    router.push((route.query.redirect as string) || '/')
  } catch (e: any) {
    ElMessage.error(e.message || '登录失败')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.auth-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #fefaf2 0%, #faf3e6 50%, #f5ead6 100%);
}
.auth-card {
  background: #fff;
  padding: 48px 40px;
  border-radius: 16px;
  box-shadow: 0 8px 32px rgba(139, 107, 21, 0.08);
  width: 400px;
  max-width: 90vw;
}
.title {
  font-size: 28px;
  color: #b87b3a;
  text-align: center;
  margin-bottom: 4px;
}
.subtitle {
  text-align: center;
  color: #8b7355;
  font-size: 14px;
  margin-bottom: 28px;
}
.form {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.submit-btn {
  width: 100%;
}
.link-text {
  text-align: center;
  margin-top: 16px;
  font-size: 13px;
  color: #8b7355;
}
.link {
  color: #b87b3a;
  text-decoration: none;
}
.link:hover {
  text-decoration: underline;
}
</style>
