<template>
  <div class="page">
    <div class="header">
      <h2 class="page-title">👥 好友</h2>
      <button @click="showAdd = true" class="btn-primary">➕ 添加好友</button>
    </div>

    <div v-if="friends.length === 0" class="empty">暂无好友，搜索邮箱添加好友</div>
    <div class="friend-list">
      <div v-for="f in friends" :key="f.id" class="friend-card">
        <span>{{ f.friendEmail }}</span>
        <button @click="removeFriend(f)" class="btn-sm btn-danger">删除</button>
      </div>
    </div>

    <div v-if="showAdd" class="modal-overlay" @click.self="showAdd = false">
      <div class="modal">
        <h3>添加好友</h3>
        <input v-model="searchQuery" @input="onSearch" placeholder="输入邮箱搜索" class="input" />
        <div v-if="searchResults.length" class="results">
          <div v-for="u in searchResults" :key="u.email" class="result-item">
            <span>{{ u.email }}</span>
            <button @click="addFriend(u.email)" class="btn-sm">添加</button>
          </div>
        </div>
        <button @click="showAdd = false" class="btn-secondary" style="margin-top:8px">关闭</button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { friendsApi } from '@/api/friends'
import { usersApi } from '@/api/users'

const friends = ref<any[]>([])
const showAdd = ref(false)
const searchQuery = ref('')
const searchResults = ref<any[]>([])
let searchTimer: any = null

onMounted(async () => {
  try { const res: any = await friendsApi.list(); friends.value = res.data } catch {}
})

function onSearch() {
  clearTimeout(searchTimer)
  searchTimer = setTimeout(async () => {
    if (!searchQuery.value) { searchResults.value = []; return }
    try { const res: any = await usersApi.search(searchQuery.value); searchResults.value = res.data } catch {}
  }, 300)
}

async function addFriend(email: string) {
  try {
    await friendsApi.add(email)
    alert('好友请求已发送')
    showAdd.value = false
  } catch (e: any) { alert(e.message) }
}

async function removeFriend(f: any) {
  if (confirm('确定删除好友 ' + f.friendEmail + ' 吗？')) {
    await friendsApi.remove(f.friendEmail)
    friends.value = friends.value.filter(x => x.id !== f.id)
  }
}
</script>

<style scoped>
.page { max-width: 700px; }
.header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
.page-title { font-size: 22px; color: #b87b3a; }
.btn-primary {
  padding: 10px 20px; background: #b87b3a; color: #fff; border: none;
  border-radius: 8px; font-size: 14px; cursor: pointer; font-family: inherit;
}
.empty { text-align: center; padding: 60px 0; color: #8b7355; }
.friend-list { display: flex; flex-direction: column; gap: 8px; }
.friend-card {
  display: flex; justify-content: space-between; align-items: center;
  background: #fff; border: 1px solid #f0e6d3; border-radius: 10px; padding: 14px 18px;
}
.btn-sm {
  padding: 6px 12px; border: 1px solid #d4b896; background: #fff;
  border-radius: 6px; cursor: pointer; font-size: 12px; color: #5a3a1a; font-family: inherit;
}
.btn-danger { border-color: #e0c8c8; color: #c0392b; }
.modal-overlay { position: fixed; inset: 0; background: rgba(0,0,0,0.3); display: flex; align-items: center; justify-content: center; z-index: 100; }
.modal { background: #fff; padding: 24px; border-radius: 12px; width: 400px; display: flex; flex-direction: column; gap: 10px; }
.modal h3 { color: #b87b3a; }
.input {
  padding: 10px 14px; border: 1px solid #e8d5c0; border-radius: 6px; font-size: 14px;
  background: #fefaf2; outline: none; font-family: inherit;
}
.results { max-height: 200px; overflow-y: auto; }
.result-item { display: flex; justify-content: space-between; align-items: center; padding: 8px 0; border-bottom: 1px solid #f0e6d3; font-size: 13px; }
.btn-secondary { padding: 8px 16px; border: 1px solid #d4b896; background: #fff; border-radius: 6px; cursor: pointer; font-family: inherit; }
</style>
