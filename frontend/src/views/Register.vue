<template>
  <div class="auth-page">
    <div class="auth-card">
      <h1 class="title">🍃 注册 MyWeb</h1>
      <p class="subtitle">创建你的账号，开始文件共享</p>
      <el-form @submit.prevent="handleRegister" label-position="top" class="form">
        <el-input
          v-model="email"
          type="email"
          placeholder="邮箱"
          size="large"
          clearable
        />
        <div class="code-row">
          <el-input
            v-model="code"
            placeholder="验证码"
            size="large"
            maxlength="6"
            class="code-input"
          />
          <el-button
            type="default"
            size="large"
            :disabled="countdown > 0"
            @click="sendCode"
            class="code-btn"
          >
            {{ countdown > 0 ? countdown + 's' : '发送验证码' }}
          </el-button>
        </div>
        <el-input
          v-model="nickname"
          placeholder="昵称"
          size="large"
          clearable
        />
        <el-input
          v-model="password"
          type="password"
          placeholder="密码（至少6位）"
          size="large"
          show-password
        />
        <el-input
          v-model="password2"
          type="password"
          placeholder="确认密码"
          size="large"
          show-password
        />
        <el-button
          type="primary"
          size="large"
          :loading="loading"
          @click="handleRegister"
          class="submit-btn"
        >
          {{ loading ? '注册中...' : '注册' }}
        </el-button>
      </el-form>
      <p class="link-text">
        已有账号？<router-link to="/login" class="link">立即登录</router-link>
      </p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { authApi } from '@/api/auth'
import { ElMessage } from 'element-plus'

const router = useRouter()
const auth = useAuthStore()
const email = ref('')
const code = ref('')
const nickname = ref('')
const password = ref('')
const password2 = ref('')
const loading = ref(false)
const countdown = ref(0)

async function sendCode() {
  try {
    await authApi.sendCode(email.value)
    countdown.value = 60
    const timer = setInterval(() => {
      countdown.value--
      if (countdown.value <= 0) clearInterval(timer)
    }, 1000)
    ElMessage.success('验证码已发送')
  } catch (e: any) {
    ElMessage.error(e.message || '发送失败')
  }
}

async function handleRegister() {
  if (password.value !== password2.value) {
    ElMessage.error('两次密码不一致')
    return
  }
  loading.value = true
  try {
    const res: any = await authApi.register(
      { email: email.value, password: password.value, nickname: nickname.value },
      code.value
    )
    auth.setTokens(res.data.accessToken, res.data.refreshToken, res.data.user)
    router.push('/')
  } catch (e: any) {
    ElMessage.error(e.message || '注册失败')
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
  width: 420px;
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
  margin-bottom: 24px;
}
.form {
  display: flex;
  flex-direction: column;
  gap: 14px;
}
.code-row {
  display: flex;
  gap: 8px;
}
.code-input {
  flex: 1;
}
.code-btn {
  white-space: nowrap;
}
.submit-btn {
  width: 100%;
}
.link-text {
  text-align: center;
  margin-top: 14px;
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
