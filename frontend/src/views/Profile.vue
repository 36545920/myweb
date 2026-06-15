<template>
  <div class="page">
    <el-card class="profile-card">
      <!-- 头像区域占位（Task 8 创建完整 AvatarUpload 组件） -->
      <div class="avatar-section">
        <el-avatar :size="80" class="profile-avatar">
          {{ auth.user?.nickname?.[0] || 'U' }}
        </el-avatar>
        <p class="avatar-hint">头像上传功能即将上线</p>
      </div>

      <el-form :model="form" label-width="90px" class="profile-form">
        <el-form-item label="邮箱">
          <el-input :model-value="auth.user?.email" disabled size="large" />
        </el-form-item>
        <el-form-item label="昵称">
          <el-input v-model="form.nickname" size="large" clearable />
        </el-form-item>
        <el-form-item label="新密码">
          <el-input v-model="form.newPassword" type="password" placeholder="留空不修改" size="large" show-password />
        </el-form-item>
        <el-form-item label="原密码">
          <el-input v-model="form.oldPassword" type="password" placeholder="修改密码时必填" size="large" show-password />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="save" :loading="saving">保存</el-button>
          <el-button @click="reset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { usersApi } from '@/api/users'
import { ElMessage } from 'element-plus'

const auth = useAuthStore()
const form = reactive({
  nickname: auth.user?.nickname || '',
  newPassword: '',
  oldPassword: ''
})
const saving = ref(false)

async function save() {
  saving.value = true
  try {
    const res: any = await usersApi.updateProfile({
      nickname: form.nickname,
      oldPassword: form.oldPassword || undefined,
      newPassword: form.newPassword || undefined
    })
    auth.user = res.data
    ElMessage.success('保存成功')
  } catch (e: any) {
    ElMessage.error(e.message || '保存失败')
  } finally {
    saving.value = false
  }
}

function reset() {
  form.nickname = auth.user?.nickname || ''
  form.newPassword = ''
  form.oldPassword = ''
}
</script>

<style scoped>
.page {
  display: flex;
  justify-content: center;
  padding-top: 40px;
}
.profile-card {
  width: 100%;
  max-width: 480px;
}
.avatar-section {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding-bottom: 8px;
}
.profile-avatar {
  background: var(--primary-light);
  color: var(--primary);
  font-size: 32px;
  font-weight: 700;
}
.avatar-hint {
  font-size: 12px;
  color: var(--text-muted);
}
.profile-form {
  margin-top: 20px;
}
</style>
