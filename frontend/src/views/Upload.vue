<template>
  <div class="page">
    <div class="upload-form">
      <!-- 拖拽上传区域 -->
      <el-upload
        ref="uploadRef"
        class="upload-zone"
        drag
        :auto-upload="false"
        :limit="1"
        :on-change="onFileChange"
        :on-remove="onFileRemove"
        :show-file-list="false"
      >
        <div class="upload-content" @click.stop>
          <el-icon class="upload-icon"><UploadFilled /></el-icon>
          <div class="upload-text" v-if="!selectedFile">
            拖拽文件到此处或<span class="upload-link">点击选择</span>
          </div>
          <div class="upload-file" v-else>
            {{ selectedFile.name }} ({{ formatFileSize(selectedFile.size) }})
          </div>
          <div class="upload-tip">支持任意格式文件</div>
        </div>
      </el-upload>

      <el-input
        v-model="title"
        placeholder="文件标题 *"
        size="large"
        clearable
      />
      <el-input
        v-model="description"
        placeholder="文件描述（可选）"
        type="textarea"
        :rows="3"
      />

      <!-- 共享池切换 -->
      <div class="option-group">
        <span class="option-label">上传目标</span>
        <el-radio-group v-model="isSharedPool" size="default">
          <el-radio :value="false">个人空间（无需审核）</el-radio>
          <el-radio :value="true">文件共享池（需审核）</el-radio>
        </el-radio-group>
      </div>

      <!-- 过期时间 -->
      <div class="option-group">
        <span class="option-label">过期时间</span>
        <el-select v-model="expireOption" size="default" style="width: 200px">
          <el-option label="永久" value="permanent" />
          <el-option label="1 天后" value="1" />
          <el-option label="2 天后" value="2" />
          <el-option label="3 天后" value="3" />
        </el-select>
      </div>

      <!-- 进度条 -->
      <el-progress
        v-if="uploading"
        :percentage="progress"
        :stroke-width="20"
        :text-inside="true"
      />

      <el-button
        type="primary"
        size="large"
        :disabled="!canUpload || uploading"
        :loading="uploading"
        @click="startUpload"
        class="upload-btn"
      >
        {{ uploading ? '上传中...' : '开始上传' }}
      </el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { filesApi } from '@/api/files'
import { formatFileSize } from '@/utils/format'
import { ElMessage } from 'element-plus'
import { UploadFilled } from '@element-plus/icons-vue'

const router = useRouter()
const uploadRef = ref()
const selectedFile = ref<File | null>(null)
const title = ref('')
const description = ref('')
const isSharedPool = ref(false)
const expireOption = ref('permanent')
const uploading = ref(false)
const progress = ref(0)

const canUpload = computed(() => selectedFile.value && title.value.trim())

function onFileChange(file: any) {
  selectedFile.value = file.raw
}

function onFileRemove() {
  selectedFile.value = null
  // el-upload 的 limit=1 会在新文件选择时自动移除旧文件
}

async function startUpload() {
  if (!selectedFile.value || !title.value.trim()) return
  uploading.value = true
  progress.value = 0

  try {
    const file = selectedFile.value
    const initRes: any = await filesApi.uploadInit({ originalName: file.name, fileSize: file.size })
    const { uploadId, totalChunks, chunkSize } = initRes.data

    let completedCount = 0
    const CONCURRENCY = 3
    const chunks: { index: number; blob: Blob }[] = []
    for (let i = 0; i < totalChunks; i++) {
      chunks.push({ index: i, blob: file.slice(i * chunkSize, (i + 1) * chunkSize) })
    }

    for (let i = 0; i < chunks.length; i += CONCURRENCY) {
      const batch = chunks.slice(i, i + CONCURRENCY)
      await Promise.all(batch.map(c =>
        filesApi.uploadChunk(uploadId, c.index, c.blob).then(() => {
          completedCount++
          progress.value = Math.round((completedCount / totalChunks) * 100)
        })
      ))
    }

    const completeRes: any = await filesApi.completeUpload(uploadId)
    const { storagePath, originalName, fileSize } = completeRes.data

    let expireAt: string | null = null
    if (expireOption.value !== 'permanent') {
      const days = parseInt(expireOption.value)
      expireAt = new Date(Date.now() + days * 86400000).toISOString()
    }

    await filesApi.createFile({
      title: title.value,
      description: description.value,
      isSharedPool: isSharedPool.value,
      expireAt,
      storagePath,
      originalName,
      fileSize,
      mimeType: file.type || 'application/octet-stream'
    })

    ElMessage.success('上传成功')
    router.push('/files')
  } catch (e: any) {
    ElMessage.error(e.message || '上传失败')
  } finally {
    uploading.value = false
  }
}
</script>

<style scoped>
.page {
  max-width: 640px;
}
.upload-form {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.upload-zone {
  width: 100%;
}
.upload-zone :deep(.el-upload) {
  width: 100%;
}
.upload-zone :deep(.el-upload-dragger) {
  width: 100%;
  padding: 40px;
  border: 2px dashed #d4b896;
  border-radius: 12px;
  background: #fff;
  transition: border-color 0.2s;
}
.upload-zone :deep(.el-upload-dragger:hover) {
  border-color: #b87b3a;
}
.upload-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  color: #8b7355;
}
.upload-icon {
  font-size: 40px;
  color: #d4b896;
}
.upload-text {
  font-size: 14px;
}
.upload-link {
  color: #b87b3a;
  cursor: pointer;
}
.upload-file {
  font-size: 14px;
  color: #3d2b1f;
  font-weight: 500;
}
.upload-tip {
  font-size: 12px;
  color: #b8a088;
}
.option-group {
  display: flex;
  align-items: center;
  gap: 16px;
}
.option-label {
  font-size: 13px;
  color: #5a3a1a;
  white-space: nowrap;
}
.upload-btn {
  width: 100%;
}
</style>
