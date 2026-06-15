<template>
  <div class="page">
    <h2 class="page-title">📤 上传文件</h2>
    <div class="upload-form">
      <div class="drop-zone" @dragover.prevent @drop.prevent="onDrop" @click="fileInput?.click()">
        <p v-if="!selectedFile">拖拽文件到此处或点击选择</p>
        <p v-else>📄 {{ selectedFile.name }} ({{ formatFileSize(selectedFile.size) }})</p>
        <input ref="fileInput" type="file" @change="onFileChange" style="display:none" />
      </div>

      <input v-model="title" placeholder="文件标题 *" class="input" required />
      <textarea v-model="description" placeholder="文件描述（可选）" class="input textarea" rows="3"></textarea>

      <div class="options">
        <label class="option"><input type="radio" v-model="isSharedPool" :value="false" /> 个人空间（无需审核）</label>
        <label class="option"><input type="radio" v-model="isSharedPool" :value="true" /> 文件共享池（需审核）</label>
      </div>

      <div class="options">
        <label class="option"><input type="radio" v-model="expireOption" value="permanent" /> 永久</label>
        <label class="option"><input type="radio" v-model="expireOption" value="1" /> 1 天后</label>
        <label class="option"><input type="radio" v-model="expireOption" value="2" /> 2 天后</label>
        <label class="option"><input type="radio" v-model="expireOption" value="3" /> 3 天后</label>
      </div>

      <div v-if="uploading" class="progress-bar">
        <div class="progress-fill" :style="{ width: progress + '%' }"></div>
        <span class="progress-text">{{ progress }}%</span>
      </div>

      <button class="btn-primary" @click="startUpload" :disabled="!canUpload || uploading">
        {{ uploading ? '上传中...' : '开始上传' }}
      </button>
      <p v-if="error" class="error">{{ error }}</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { filesApi } from '@/api/files'
import { formatFileSize } from '@/utils/format'

const router = useRouter()
const fileInput = ref<HTMLInputElement>()
const selectedFile = ref<File | null>(null)
const title = ref('')
const description = ref('')
const isSharedPool = ref(false)
const expireOption = ref('permanent')
const uploading = ref(false)
const progress = ref(0)
const error = ref('')

const canUpload = computed(() => selectedFile.value && title.value.trim())

function onFileChange(e: Event) {
  const file = (e.target as HTMLInputElement).files?.[0]
  if (file) selectedFile.value = file
}
function onDrop(e: DragEvent) {
  const file = e.dataTransfer?.files?.[0]
  if (file) selectedFile.value = file
}

async function startUpload() {
  if (!selectedFile.value || !title.value.trim()) return
  error.value = ''
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
      title: title.value, description: description.value,
      isSharedPool: isSharedPool.value, expireAt,
      storagePath, originalName, fileSize,
      mimeType: file.type || 'application/octet-stream'
    })

    router.push('/files')
  } catch (e: any) {
    error.value = e.message || '上传失败'
  } finally {
    uploading.value = false
  }
}
</script>

<style scoped>
.page { max-width: 640px; }
.page-title { font-size: 22px; color: #b87b3a; margin-bottom: 20px; }
.upload-form { display: flex; flex-direction: column; gap: 14px; }
.drop-zone {
  border: 2px dashed #d4b896; border-radius: 12px; padding: 40px;
  text-align: center; cursor: pointer; background: #fff; color: #8b7355;
  transition: border-color 0.2s;
}
.drop-zone:hover { border-color: #b87b3a; }
.input {
  padding: 12px 16px; border: 1px solid #e8d5c0; border-radius: 8px;
  font-size: 14px; background: #fefaf2; outline: none; font-family: inherit;
}
.input:focus { border-color: #b87b3a; }
.textarea { resize: vertical; }
.options { display: flex; gap: 16px; flex-wrap: wrap; }
.option { font-size: 13px; color: #5a3a1a; cursor: pointer; display: flex; align-items: center; gap: 4px; }
.option input { accent-color: #b87b3a; }
.progress-bar { height: 24px; background: #f0e6d3; border-radius: 12px; position: relative; overflow: hidden; }
.progress-fill { height: 100%; background: #b87b3a; border-radius: 12px; transition: width 0.3s; }
.progress-text { position: absolute; top: 50%; left: 50%; transform: translate(-50%,-50%); font-size: 12px; color: #3d2b1f; }
.btn-primary {
  padding: 12px; background: #b87b3a; color: #fff; border: none;
  border-radius: 8px; font-size: 16px; cursor: pointer; font-family: inherit;
}
.btn-primary:hover { background: #9e672f; }
.btn-primary:disabled { opacity: 0.6; cursor: not-allowed; }
.error { color: #c0392b; font-size: 13px; text-align: center; }
</style>
