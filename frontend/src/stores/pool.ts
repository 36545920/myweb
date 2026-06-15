import { defineStore } from 'pinia'
import { ref } from 'vue'

export const usePoolStore = defineStore('pool', () => {
  const files = ref<any[]>([])
  const total = ref(0)

  function setFiles(data: any) {
    files.value = data.records || []
    total.value = data.total || 0
  }

  return { files, total, setFiles }
})
