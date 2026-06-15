<template>
  <div class="app-layout">
    <Sidebar />
    <main class="main-content">
      <div class="content-inner">
        <router-view />
      </div>
    </main>
  </div>
</template>

<script setup lang="ts">
import Sidebar from './Sidebar.vue'
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

.btn {
  display: inline-flex; align-items: center; justify-content: center; gap: 6px;
  padding: 8px 18px; border-radius: var(--radius-sm);
  font-size: 13px; font-weight: 500; font-family: inherit; cursor: pointer;
  transition: all 0.15s; text-decoration: none; border: 1px solid var(--border);
  background: var(--bg-card); color: var(--text-secondary);
}
.btn:hover { background: var(--primary-bg); border-color: var(--primary-light); color: var(--primary); }
.btn-primary {
  display: inline-flex; align-items: center; justify-content: center; gap: 6px;
  padding: 8px 20px; border-radius: var(--radius-sm); border: none;
  font-size: 14px; font-weight: 500; font-family: inherit; cursor: pointer;
  background: var(--primary); color: #fff; transition: all 0.15s; text-decoration: none;
}
.btn-primary:hover { background: var(--primary-hover); }
.btn-danger { border-color: #e8c8c8; color: var(--danger); }
.btn-danger:hover { background: var(--danger-bg); border-color: #e8c8c8; }

.input {
  padding: 10px 14px; border: 1px solid var(--border); border-radius: var(--radius-sm);
  font-size: 14px; font-family: inherit; background: var(--bg); color: var(--text);
  outline: none; transition: border-color 0.15s; width: 100%;
}
.input:focus { border-color: var(--primary); box-shadow: 0 0 0 3px rgba(184,123,58,0.08); }
.input:disabled { opacity: 0.5; background: #f5f5f5; }

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
.main-content { flex: 1; overflow-y: auto; background: var(--bg); }
.content-inner { max-width: 960px; padding: 32px 36px; }
</style>
