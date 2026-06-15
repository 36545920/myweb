<template>
  <div class="avatar-section">
    <div class="avatar-preview" @click="fileInput?.click()">
      <img v-if="previewUrl" :src="previewUrl" class="avatar-img" />
      <div v-else class="avatar-default">{{ initial }}</div>
      <div class="avatar-overlay"><el-icon><Camera /></el-icon></div>
    </div>
    <input ref="fileInput" type="file" accept="image/*" @change="onFileChange" style="display:none" />
    <div v-if="file" class="avatar-info">
      <span>{{ file.name }}</span>
      <el-button size="small" type="primary" @click="upload" :loading="uploading">上传</el-button>
      <el-button size="small" @click="cancel">取消</el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { usersApi } from '@/api/users'
import { ElMessage } from 'element-plus'
import { Camera } from '@element-plus/icons-vue'

const auth = useAuthStore()
const fileInput = ref<HTMLInputElement>()
const file = ref<File | null>(null)
const previewUrl = ref<string | null>(auth.user?.avatar || null)
const uploading = ref(false)
const initial = computed(() => (auth.user?.nickname || 'U')[0])

function onFileChange(e: Event) {
  const f = (e.target as HTMLInputElement).files?.[0]
  if (!f) return
  if (f.size > 2 * 1024 * 1024) { ElMessage.error('头像不能超过 2MB'); return }
  file.value = f
  previewUrl.value = URL.createObjectURL(f)
}

async function upload() {
  if (!file.value) return
  uploading.value = true
  try {
    const formData = new FormData(); formData.append('file', file.value)
    const res: any = await usersApi.uploadAvatar(formData)
    auth.user.avatar = res.data.avatar
    file.value = null
    ElMessage.success('头像上传成功')
  } catch (e: any) { ElMessage.error(e.message || '上传失败') }
  finally { uploading.value = false }
}

function cancel() { file.value = null; previewUrl.value = auth.user?.avatar || null }
</script>

<style scoped>
.avatar-section { display: flex; flex-direction: column; align-items: center; gap: 12px; }
.avatar-preview {
  width: 96px; height: 96px; border-radius: 50%; overflow: hidden;
  cursor: pointer; position: relative; border: 3px solid var(--el-color-primary-light-7);
}
.avatar-img { width: 100%; height: 100%; object-fit: cover; }
.avatar-default {
  width: 100%; height: 100%; background: var(--el-color-primary-light-5);
  display: flex; align-items: center; justify-content: center;
  font-size: 36px; font-weight: 700; color: var(--el-color-primary);
}
.avatar-overlay {
  position: absolute; inset: 0; background: rgba(0,0,0,0.15);
  display: flex; align-items: center; justify-content: center; opacity: 0; transition: opacity 0.2s;
  font-size: 20px; color: #fff;
}
.avatar-preview:hover .avatar-overlay { opacity: 1; }
.avatar-info { display: flex; align-items: center; gap: 8px; font-size: 13px; }
</style>
