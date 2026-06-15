<template>
  <div class="auth-page">
    <div class="card">
      <h1>✅ 邮箱验证成功！</h1>
      <p>你的邮箱已验证通过。</p>
      <router-link to="/login" class="link">前往登录</router-link>
    </div>
  </div>
</template>

<style scoped>
.auth-page { min-height: 100vh; display: flex; align-items: center; justify-content: center; background: #fefaf2; }
.card { text-align: center; background: #fff; padding: 48px; border-radius: 16px; box-shadow: 0 4px 20px rgba(0,0,0,0.05); }
.card h1 { color: #b87b3a; margin-bottom: 12px; }
.link { color: #b87b3a; }
</style>
