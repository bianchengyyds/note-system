<template>
  <div class="public-note">
    <div v-if="loading" class="loading">加载中...</div>
    <div v-else-if="error" class="error">{{ error }}</div>
    <div v-else-if="needPassword" class="password-box">
      <h2>此笔记需要密码</h2>
      <el-input v-model="password" placeholder="请输入访问密码" @keyup.enter="verifyPassword" />
      <el-button type="primary" @click="verifyPassword">验证</el-button>
    </div>
    <div v-else-if="note" class="content">
      <h1>{{ note.title }}</h1>
      <div class="meta">
        作者：{{ note.creatorName }} · 创建于 {{ formatDate(note.createdAt) }}
      </div>
      <NoteViewer :note="note" />
    </div>
    <el-empty v-else description="笔记不存在或已失效" />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { getPublicNote } from '@/api/note'
import NoteViewer from '@/components/note/NoteViewer.vue'

const route = useRoute()
const shareKey = route.params.shareKey

const loading = ref(true)
const error = ref('')
const note = ref(null)
const needPassword = ref(false)
const password = ref('')

async function fetchNote(pwd = '') {
  loading.value = true
  try {
    const res = await getPublicNote(shareKey, pwd)
    note.value = res
    needPassword.value = false
  } catch (e) {
    const status = e.response?.status
    if (status === 403 || status === 401) {
      needPassword.value = true
    } else {
      error.value = '笔记不存在或密码错误'
    }
  } finally {
    loading.value = false
  }
}

function verifyPassword() {
  fetchNote(password.value)
}

function formatDate(dateStr) {
  if (!dateStr) return ''
  return new Date(dateStr).toLocaleString('zh-CN')
}

onMounted(() => {
  fetchNote()
})
</script>

<style scoped>
.public-note {
  max-width: 900px;
  margin: 40px auto;
  padding: 24px;
  background: #fff;
  border-radius: 8px;
}
.meta { color: #666; margin-bottom: 24px; }
.password-box { text-align: center; margin-top: 40px; }
</style>