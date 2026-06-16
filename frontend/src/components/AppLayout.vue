<template>
  <div class="app-layout">
    <Sidebar />
    <main class="main-content">
      <header class="top-header">
        <span class="header-title">{{ pageTitle }}</span>
      </header>
      <div v-if="!nasOnline" class="nas-warning">
        ⚠ 文件存储服务器离线中，上传和下载功能暂不可用。共享池文件仅可查看记录。
      </div>
      <div class="content-inner">
        <router-view />
      </div>
    </main>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, onMounted, onUnmounted } from 'vue'
import { useRoute } from 'vue-router'
import Sidebar from './Sidebar.vue'
import axios from 'axios'

const route = useRoute()

const pageTitles: Record<string, string> = {
  Dashboard: '仪表盘',
  MyFiles: '我的文件',
  Upload: '上传文件',
  Inbox: '收件箱',
  Pool: '文件共享池',
  Friends: '好友',
  Profile: '个人设置',
  AdminReview: '文件审核',
  AdminUsers: '用户管理',
  SuperSystem: '系统设置',
}

const pageTitle = computed(() => {
  const name = route.name as string
  return pageTitles[name] || name || ''
})

// NAS 健康检查
const nasOnline = ref(true)
let healthTimer: any = null

async function checkHealth() {
  try {
    const res: any = await axios.get('/api/v1/health')
    nasOnline.value = res.data?.nas ?? true
  } catch {
    nasOnline.value = false
  }
}

onMounted(() => {
  checkHealth()
  healthTimer = setInterval(checkHealth, 30000)
})
onUnmounted(() => { clearInterval(healthTimer) })
</script>

<style>
* { margin: 0; padding: 0; box-sizing: border-box; }

body {
  background: #fdf8f2;
  color: #3d2b1f;
  font-family: 'PingFang SC', 'Noto Serif SC', 'Georgia', 'Hiragino Sans GB', sans-serif;
  -webkit-font-smoothing: antialiased;
  line-height: 1.6;
}

/* Element Plus 主题色覆盖 */
:root {
  --el-color-primary: #b87b3a;
  --el-color-primary-light-3: #d4b896;
  --el-color-primary-light-5: #e8d5c0;
  --el-color-primary-light-7: #f0e6d3;
  --el-color-primary-light-8: #f5ede0;
  --el-color-primary-light-9: #fdf5ed;
  --el-color-primary-dark-2: #9e672f;
  --el-border-radius-base: 8px;
  --el-border-radius-small: 4px;
  --el-font-family: 'PingFang SC', 'Noto Serif SC', 'Georgia', sans-serif;
}

/* 自定义 CSS 变量（与 Element Plus 共存） */
:root {
  --primary: #b87b3a;
  --primary-hover: #9e672f;
  --primary-light: #e8d5c0;
  --primary-bg: #fdf5ed;
  --bg: #fdf8f2;
  --bg-card: #fff;
  --bg-card-hover: #fefaf6;
  --border: #efe3d4;
  --text: #3d2b1f;
  --text-secondary: #8b7355;
  --text-muted: #b8a088;
  --danger: #c0392b;
  --danger-bg: #fdf5f5;
  --success: #6b8e23;
  --success-bg: #f6faf0;
  --shadow-sm: 0 1px 3px rgba(139, 107, 21, 0.06);
  --shadow-md: 0 4px 16px rgba(139, 107, 21, 0.08);
  --radius-sm: 6px;
  --radius: 10px;
  --radius-lg: 14px;
}

h2 { font-size: 20px; font-weight: 600; color: var(--primary); margin-bottom: 20px; }
h3 { font-size: 16px; font-weight: 600; color: var(--text); margin-bottom: 12px; }

.card {
  background: var(--bg-card); border: 1px solid var(--border);
  border-radius: var(--radius); box-shadow: var(--shadow-sm);
}
.card:hover { box-shadow: var(--shadow-md); }

.empty-state { text-align: center; padding: 48px 0; color: var(--text-muted); font-size: 14px; }

.bar {
  height: 10px; background: var(--primary-light); border-radius: 5px; overflow: hidden;
  position: relative;
}
.bar-fill {
  height: 100%; background: linear-gradient(90deg, var(--primary), #d4a85c);
  border-radius: 5px; transition: width 0.6s ease;
}
.bar-text {
  position: absolute; top: 50%; left: 50%; transform: translate(-50%,-50%);
  font-size: 11px; color: var(--text); white-space: nowrap;
}
</style>

<style scoped>
.app-layout { display: flex; min-height: 100vh; }
.main-content { flex: 1; overflow-y: auto; background: var(--bg); min-width: 0; display: flex; flex-direction: column; }

.top-header {
  display: flex; align-items: center;
  padding: 0 40px; height: 52px; flex-shrink: 0;
  background: var(--bg-card);
  border-bottom: 1px solid var(--border);
}
.header-title {
  font-size: 14px; font-weight: 600;
  color: var(--text-secondary);
}

.nas-warning {
  background: #fef0e0; color: #b87b3a; text-align: center;
  padding: 10px 20px; font-size: 13px; border-bottom: 1px solid #f0d4a0;
  flex-shrink: 0;
}
.content-inner { padding: 28px 40px; flex: 1; width: 100%; }
</style>
