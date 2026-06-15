<template>
  <aside class="sidebar">
    <div class="logo" @click="$router.push('/')">
      <span class="logo-icon">🍃</span>
      <span class="logo-text">MyWeb</span>
    </div>

    <nav class="nav">
      <div class="nav-group">
        <router-link to="/" class="nav-item">仪表盘</router-link>
        <router-link to="/files" class="nav-item">我的文件</router-link>
        <router-link to="/inbox" class="nav-item">收件箱</router-link>
        <router-link to="/pool" class="nav-item">共享池</router-link>
        <router-link to="/friends" class="nav-item">好友</router-link>
      </div>

      <div class="nav-divider"></div>

      <div v-if="auth.isAdmin" class="nav-group">
        <div class="nav-label">管理</div>
        <router-link to="/admin/review" class="nav-item">文件审核</router-link>
        <router-link to="/admin/users" class="nav-item">用户管理</router-link>
      </div>

      <div v-if="auth.isSuperAdmin" class="nav-group">
        <div class="nav-label">系统</div>
        <router-link to="/super-admin/system" class="nav-item">系统设置</router-link>
      </div>

      <div class="nav-divider"></div>

      <router-link to="/profile" class="nav-item">个人设置</router-link>
    </nav>

    <div class="user-info">
      <div class="user-avatar">{{ (auth.user?.nickname || 'U')[0] }}</div>
      <div class="user-detail">
        <div class="user-name">{{ auth.user?.nickname || '用户' }}</div>
        <div class="user-role">{{ roleLabel }}</div>
      </div>
      <button class="logout-btn" @click="handleLogout" title="退出登录">⏻</button>
    </div>
  </aside>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { useRouter } from 'vue-router'

const auth = useAuthStore()
const router = useRouter()

const roleLabel = computed(() => {
  const map: Record<string, string> = { USER: '用户', ADMIN: '管理员', SUPER_ADMIN: '超级管理员' }
  return map[auth.user?.role] || ''
})

function handleLogout() {
  auth.logout()
  router.push('/login')
}
</script>

<style scoped>
.sidebar {
  width: 220px;
  min-height: 100vh;
  background: linear-gradient(180deg, #fffbf5 0%, #fefaf2 40%, #fbf4e8 100%);
  border-right: 1px solid var(--border);
  display: flex;
  flex-direction: column;
  position: sticky;
  top: 0;
}

.logo {
  display: flex; align-items: center; gap: 10px;
  padding: 22px 20px; cursor: pointer;
  border-bottom: 1px solid var(--border);
}
.logo-icon { font-size: 22px; }
.logo-text { font-size: 20px; font-weight: 700; color: var(--primary); letter-spacing: 0.5px; }

.nav { flex: 1; padding: 16px 0; overflow-y: auto; }
.nav-group { margin-bottom: 4px; }
.nav-label {
  padding: 12px 20px 6px; font-size: 11px; font-weight: 600;
  color: var(--text-muted); text-transform: uppercase; letter-spacing: 1px;
}
.nav-item {
  display: flex; align-items: center;
  padding: 9px 20px; margin: 0 10px; border-radius: 8px;
  color: var(--text-secondary); text-decoration: none;
  font-size: 14px; transition: all 0.15s;
}
.nav-item:hover {
  background: var(--primary-bg); color: var(--primary);
}
.nav-item.router-link-active {
  background: var(--primary-bg); color: var(--primary); font-weight: 600;
}
.nav-divider { height: 1px; background: var(--border); margin: 8px 20px; }

.user-info {
  padding: 16px; border-top: 1px solid var(--border);
  display: flex; align-items: center; gap: 10px;
}
.user-avatar {
  width: 34px; height: 34px; border-radius: 50%;
  background: var(--primary-light); color: var(--primary);
  display: flex; align-items: center; justify-content: center;
  font-size: 14px; font-weight: 600; flex-shrink: 0;
}
.user-detail { flex: 1; min-width: 0; }
.user-name { font-size: 13px; font-weight: 600; color: var(--text); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.user-role { font-size: 11px; color: var(--text-muted); margin-top: 1px; }
.logout-btn {
  width: 30px; height: 30px; border-radius: 6px;
  border: 1px solid var(--border); background: var(--bg-card);
  color: var(--text-muted); cursor: pointer; font-size: 14px;
  display: flex; align-items: center; justify-content: center;
  transition: all 0.15s; flex-shrink: 0;
}
.logout-btn:hover { background: var(--danger-bg); border-color: #e8c8c8; color: var(--danger); }
</style>
