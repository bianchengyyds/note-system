<!-- 在 NoteEditor.vue 中使用 Tiptap 编辑器的示例 -->
<!-- 
  这是一个参考示例，展示如何将现有的 Vditor 编辑器替换为 Tiptap 编辑器
  你可以根据需要选择使用哪种编辑器
-->

<template>
  <div class="note-editor-page">
    <!-- 顶部工具栏 -->
    <div class="editor-toolbar">
      <div class="title-section">
        <el-button link @click="goBack">
          <el-icon><ArrowLeft /></el-icon>
        </el-button>
        <input 
          v-model="noteTitle" 
          placeholder="无标题文档" 
          class="title-input"
          @blur="handleTitleBlur"
        />
        <span class="note-type-tag">{{ getTypeLabel(noteType) }}</span>
      </div>
      
      <div class="actions">
        <el-button @click="handleSave" :loading="saving">
          <el-icon><Check /></el-icon>
          保存
        </el-button>
        <el-dropdown trigger="click">
          <el-button>
            <el-icon><MoreFilled /></el-icon>
          </el-button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item @click="handleShare">分享</el-dropdown-item>
              <el-dropdown-item @click="handleExport">导出</el-dropdown-item>
              <el-dropdown-item divided @click="handleDelete">删除</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </div>
    
    <!-- 编辑器主体 -->
    <div class="editor-body">
      <!-- 富文本文档：使用 Tiptap 编辑器 -->
      <div v-if="noteType === 'doc'" class="tiptap-wrap">
        <TiptapEditor
          ref="editorRef"
          v-model="noteContent"
          :kb-id="kbId"
          placeholder="开始写作... 点击上方按钮插入表格、画板、思维导图等组件"
          @headings-change="handleHeadingsChange"
        />
      </div>
      
      <!-- 其他类型笔记保持原有逻辑 -->
      <div v-else-if="noteType === 'table'" class="table-editor-container">
        <!-- 表格编辑器 -->
      </div>
      
      <div v-else-if="noteType === 'board'" class="board-editor-container">
        <!-- 画板编辑器 -->
      </div>
      
      <div v-else-if="noteType === 'mind'" class="mind-editor-container">
        <!-- 思维导图编辑器 -->
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft, Check, MoreFilled } from '@element-plus/icons-vue'
import TiptapEditor from '@/components/note/TiptapEditor.vue'
import { getNoteDetail, updateNote, createNote } from '@/api/note'

const route = useRoute()
const router = useRouter()

const noteId = route.params.noteId
const kbId = route.query.kbId
const noteType = route.query.type || 'doc'

const noteTitle = ref('')
const noteContent = ref('')
const saving = ref(false)
const editorRef = ref(null)

// 加载笔记
onMounted(async () => {
  if (noteId) {
    await loadNote()
  } else {
    // 新建笔记
    noteTitle.value = '无标题文档'
  }
})

// 加载笔记详情
const loadNote = async () => {
  try {
    const detail = await getNoteDetail(noteId)
    noteTitle.value = detail.title
    noteContent.value = detail.content || ''
  } catch (error) {
    ElMessage.error('加载笔记失败')
  }
}

// 保存笔记
const handleSave = async () => {
  if (!noteTitle.value.trim()) {
    ElMessage.warning('请输入标题')
    return
  }
  
  saving.value = true
  try {
    const data = {
      title: noteTitle.value,
      content: noteContent.value
    }
    
    if (noteId) {
      await updateNote(noteId, data)
    } else {
      const res = await createNote({
        kbId: kbId,
        ...data,
        type: noteType
      })
      // 跳转到编辑页面
      router.replace({
        name: 'NoteEditor',
        params: { docId: res.id },
        query: { kbId, type: noteType }
      })
    }
    
    ElMessage.success('保存成功')
  } catch (error) {
    ElMessage.error('保存失败')
  } finally {
    saving.value = false
  }
}

// 处理大纲变化
const handleHeadingsChange = (headings) => {
  console.log('大纲更新:', headings)
  // 可以在这里更新侧边栏的大纲显示
}

// 返回
const goBack = () => {
  router.back()
}

// 获取类型标签
const getTypeLabel = (type) => {
  const labels = {
    doc: '文档',
    table: '表格',
    board: '画板',
    mind: '思维导图'
  }
  return labels[type] || type
}

// 其他方法（分享、导出、删除等）保持不变...
</script>

<style scoped>
/* 样式与原有的 NoteEditor.vue 保持一致 */
.note-editor-page {
  display: flex;
  flex-direction: column;
  height: 100vh;
}

.editor-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 32px;
  border-bottom: 1px solid #e5e6eb;
  background: #fff;
}

.title-section {
  display: flex;
  align-items: center;
  gap: 12px;
  flex: 1;
  margin-right: 24px;
}

.title-input {
  border: none;
  outline: none;
  font-size: 24px;
  font-weight: 600;
  width: 100%;
  color: #1f2937;
  background: transparent;
}

.tiptap-wrap {
  flex: 1;
  overflow: hidden;
}
</style>
