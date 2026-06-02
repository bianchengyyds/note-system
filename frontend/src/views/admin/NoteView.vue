<template>
  <div class="note-view-page">
    <!-- 顶栏 -->
    <div class="view-toolbar">
      <div class="title-section">
        <h1 class="title-display">{{ noteTitle }}</h1>
        <span class="note-type-tag">{{ typeLabel }}</span>
      </div>
      <div class="actions">
        <el-button size="small" @click="router.back()">
          <el-icon><ArrowLeft /></el-icon> 返回
        </el-button>
      </div>
    </div>

    <!-- 内容区 -->
    <div class="view-body">
      <div class="view-scroll-container">
        <NoteViewer v-if="note" :note="note" />
        <el-empty v-else-if="!loading" description="笔记不存在" />
      </div>
    </div>

    <!-- 加载中 -->
    <el-dialog v-model="loading" :show-close="false" width="200px" center>
      <div class="loading-content">
        <el-icon class="loading-icon" :size="40"><Loading /></el-icon>
        <p>加载中...</p>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getNoteDetail } from '@/api/note'
import NoteViewer from '@/components/note/NoteViewer.vue'

const route = useRoute()
const router = useRouter()

const noteId = computed(() => route.params.docId)
const noteTitle = ref('加载中...')
const note = ref(null)
const loading = ref(true)

const typeLabel = computed(() => {
  const m = { doc: '文档', table: '表格', board: '画板', mind: '思维导图' }
  return m[note.value?.type] || '笔记'
})

async function loadNote() {
  if (!noteId.value) return

  try {
    loading.value = true
    const res = await getNoteDetail(noteId.value)
    noteTitle.value = res.title || '无标题'
    note.value = res
  } catch (e) {
    console.error('加载笔记失败', e)
    ElMessage.error('加载笔记失败')
    router.back()
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadNote()
})
</script>

<style scoped>
.note-view-page {
  display: flex;
  flex-direction: column;
  height: 100vh;
  background: #fff;
}

.view-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 24px;
  border-bottom: 1px solid #e5e7eb;
  background: #fff;
  flex-shrink: 0;
}

.title-section {
  display: flex;
  align-items: center;
  gap: 12px;
}

.title-display {
  font-size: 18px;
  font-weight: 600;
  color: #1f2937;
  margin: 0;
}

.note-type-tag {
  font-size: 12px;
  color: #6b7280;
  background: #f3f4f6;
  padding: 2px 8px;
  border-radius: 4px;
}

.view-body {
  flex: 1;
  overflow: hidden;
}

.view-scroll-container {
  height: 100%;
  overflow-y: auto;
  padding: 24px;
}

.loading-content {
  text-align: center;
  padding: 20px;
}

.loading-icon {
  animation: spin 1s linear infinite;
  color: #3b82f6;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}
</style>
