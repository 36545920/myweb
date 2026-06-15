<template>
  <aside class="sidebar">
    <div class="logo" @click="$router.push('/')">🍃 MyWeb</div>
    <nav class="nav">
      <router-link to="/" class="nav-item">📊 仪表盘</router-link>
      <router-link to="/files" class="nav-item">📁 我的文件</router-link>
      <router-link to="/inbox" class="nav-item">📥 收件箱</router-link>
      <router-link to="/pool" class="nav-item">📦 共享池</router-link>
      <router-link to="/friends" class="nav-item">👥 好友</router-link>
      <div class="divider"></div>
      <template v-if="auth.isAdmin">
        <router-link to="/admin/review" class="nav-item">✅ 审核</router-link>
        <router-link to="/admin/users" class="nav-item">👤 用户管理</router-link>
      </template>
      <template v-if="auth.isSuperAdmin">
        <router-link to="/super-admin/system" class="nav-item">⚙ 系统设置</router-link>
      </template>
      <router-link to="/profile" class="nav-item">🔧 个人设置</router-link>
    </nav>
    <div class="user-info">
      <span class="user-name">{{ auth.user?.nickname || '用户' }}</span>
      <button class="logout-btn" @click="handleLogout">退出</button>
    </div>
  </aside>
</template>

<script setup lang="ts">
import { useAuthStore } from '@/stores/auth'
import { useRouter } from 'vue-router'

const auth = useAuthStore()
const router = useRouter()

function handleLogout() {
  auth.logout()
  router.push('/login')
}
</script>

<style scoped>
.sidebar {
  width: 200px;
  min-height: 100vh;
  background: linear-gradient(180deg, #fefaf2 0%, #faf3e6 100%);
  border-right: 1px solid #f0e6d3;
  display: flex;
  flex-direction: column;
  font-family: 'Georgia', 'Noto Serif SC', serif;
}
.logo {
  padding: 20px 16px;
  font-size: 20px;
  font-weight: bold;
  color: #b87b3a;
  cursor: pointer;
  border-bottom: 1px solid #f0e6d3;
}
.nav { flex: 1; padding: 12px 0; }
.nav-item {
  display: block;
  padding: 10px 20px;
  color: #5a3a1a;
  text-decoration: none;
  font-size: 14px;
  transition: all 0.2s;
  border-left: 3px solid transparent;
}
.nav-item:hover, .nav-item.router-link-active {
  background: #f0e6d3;
  border-left-color: #b87b3a;
  color: #b87b3a;
}
.divider { height: 1px; background: #f0e6d3; margin: 8px 16px; }
.user-info {
  padding: 16px;
  border-top: 1px solid #f0e6d3;
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.user-name { font-size: 13px; color: #5a3a1a; }
.logout-btn {
  background: none; border: 1px solid #d4b896; color: #8b6914;
  padding: 4px 10px; border-radius: 4px; cursor: pointer; font-size: 12px;
}
.logout-btn:hover { background: #f0e6d3; }
</style>
