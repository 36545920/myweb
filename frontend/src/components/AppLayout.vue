<template>
  <div class="app-layout">
    <Sidebar />
    <main class="main-content">
      <router-view />
    </main>
  </div>
</template>

<script setup lang="ts">
import Sidebar from './Sidebar.vue'
</script>

<style>
* { margin: 0; padding: 0; box-sizing: border-box; }
body { background: #fefaf2; color: #3d2b1f; font-family: 'Georgia', 'Noto Serif SC', 'PingFang SC', serif; }

:root {
  --primary: #b87b3a;
  --primary-light: #d4b896;
  --bg: #fefaf2;
  --bg-card: #fff;
  --border: #f0e6d3;
  --text: #3d2b1f;
  --text-light: #8b7355;
  --danger: #c0392b;
  --success: #6b8e23;
}
</style>

<style scoped>
.app-layout { display: flex; min-height: 100vh; }
.main-content { flex: 1; padding: 28px; overflow-y: auto; }
</style>
