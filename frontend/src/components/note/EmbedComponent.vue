<template>
  <NodeViewWrapper 
    class="embed-component-wrapper"
    :draggable="!isEditable"
  >
    <div 
      class="embed-card-container"
      @mouseenter="handleMouseEnter"
      @mouseleave="handleMouseLeave"
    >
      <!-- 嵌入组件编辑器（始终显示，根据 isEditable 控制交互） -->
      <div class="embed-card" :class="{ 'hovered': isHovered, 'is-editable': isEditable }">
        <div class="embed-header">
          <div v-if="node.attrs.title && node.attrs.title !== '新建组件'" class="embed-title">{{ node.attrs.title }}</div>
        </div>

        <EmbedEditBlock
          ref="editBlockRef"
          :embed-type="nodeType"
          :note-id="noteId"
          :inline="true"
          :is-editable="isEditable"
        />

        <!-- 删除按钮（悬停显示，仅在可编辑模式下显示） -->
        <transition name="fade">
          <div v-if="isHovered && isEditable" class="embed-actions">
            <el-button size="small" type="danger" @click.stop="handleDelete">
              <el-icon><Delete /></el-icon>
              删除
            </el-button>
          </div>
        </transition>
      </div>
    </div>
  </NodeViewWrapper>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount, inject } from 'vue'
import { NodeViewWrapper, nodeViewProps } from '@tiptap/vue-3'
import { Delete } from '@element-plus/icons-vue'
import { ElMessageBox } from 'element-plus'
import EmbedEditBlock from './EmbedEditBlock.vue'
import { embedApps } from '@/utils/vditorEmbed'

const props = defineProps({
  ...nodeViewProps
})

const isHovered = ref(false)
const editBlockRef = ref(null)

// 注入父组件的编辑状态，这是最可靠的状态来源
const isNoteEditing = inject('isNoteEditing', ref(false))

// 综合判断是否可编辑
const isEditable = computed(() => {
  return !!(isNoteEditing.value || props.editor?.isEditable)
})

// 注册到全局 embedApps，以便主编辑器保存时能触发所有嵌套组件的静默保存
onMounted(() => {
  if (noteId.value) {
    // 这里我们存入一个代理对象，模拟 EmbedEditBlock 的 vm
    embedApps.set(String(noteId.value), { 
      vm: { 
        saveEmbed: (options) => editBlockRef.value?.saveEmbed(options) 
      } 
    })
  }
})

onBeforeUnmount(() => {
  if (noteId.value) {
    embedApps.delete(String(noteId.value))
  }
})

// 从 Tiptap 节点获取属性
const nodeType = computed(() => props.node.attrs.type)
const noteId = computed(() => props.node.attrs.noteId)

// 鼠标进入/离开处理
const handleMouseEnter = () => {
  isHovered.value = true
}

const handleMouseLeave = () => {
  isHovered.value = false
}

// 删除按钮点击
const handleDelete = () => {
  ElMessageBox.confirm(
    '确定要删除这个组件吗？',
    '警告',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).then(() => {
    // 删除节点
    props.deleteNode()
  }).catch(() => {
    // 取消操作
  })
}
</script>

<style scoped>
.embed-component-wrapper {
  margin: 16px 0;
  position: relative;
}

.embed-card-container {
  display: block;
}

.embed-card {
  border-radius: 8px;
  padding: 0;
  background: transparent;
  transition: all 0.3s ease;
  position: relative;
}

.embed-card.is-editable {
  border-color: #e5e7eb;
}

.embed-card.is-editable:hover {
  border-color: #25b864;
  box-shadow: 0 4px 12px rgba(37, 184, 100, 0.15);
  background: #fff;
}

.embed-card.loading,
.embed-card.error {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  color: #999;
  min-height: 80px;
}

.embed-card.error {
  color: #f56c6c;
  border-color: #fde2e2;
  background: #fef0f0;
}

.embed-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
  cursor: pointer;
}

.embed-title {
  flex: 1;
  font-size: 14px;
  font-weight: 500;
  color: #262626;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.embed-actions {
  position: absolute;
  top: 8px;
  right: 8px;
  display: flex;
  gap: 8px;
  background: rgba(255, 255, 255, 0.95);
  padding: 4px;
  border-radius: 6px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
