<template>
  <li>
    <div
      class="tree-node"
      :class="{ active: activeNoteId === node.id, 'drag-over': isDragOver }"
      draggable="true"
      @click="handleSelect"
      @dragstart="onDragStart"
      @dragover.prevent="onDragOver"
      @dragleave="onDragLeave"
      @drop.prevent="onDrop"
    >
      <!-- 折叠箭头 -->
      <span class="arrow" v-if="hasChildren" @click.stop="toggle">
        <el-icon><component :is="expanded ? 'ArrowDown' : 'ArrowRight'" /></el-icon>
      </span>
      <span v-else class="arrow-placeholder"></span>

      <!-- 类型图标 -->
      <el-icon class="type-icon"><component :is="typeIcon" /></el-icon>

      <!-- 标题 -->
      <span class="node-title">{{ node.title || '无标题' }}</span>

      <!-- 右侧操作按钮 -->
      <div class="node-actions" @click.stop>
        <!-- 加号按钮（仅可编辑用户可见） -->
        <el-dropdown v-if="canEdit" trigger="click" @command="handleCreate">
          <el-button text size="small" class="action-btn">
            <el-icon><Plus /></el-icon>
          </el-button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="doc">
                <el-icon><Document /></el-icon> 新建子文档
              </el-dropdown-item>
              <el-dropdown-item command="table">
                <el-icon><Grid /></el-icon> 新建子表格
              </el-dropdown-item>
              <el-dropdown-item command="board">
                <el-icon><Picture /></el-icon> 新建子画板
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>

        <!-- 三点菜单 -->
        <el-dropdown trigger="click" @command="handleMenu">
          <el-button text size="small" class="action-btn">
            <el-icon><more-filled /></el-icon>
          </el-button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="move">移动</el-dropdown-item>
              <el-dropdown-item command="delete">删除</el-dropdown-item>
              <el-dropdown-item command="export">导出</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </div>

    <!-- 子节点列表 -->
    <ul v-if="hasChildren && expanded" class="children-list">
      <TreeNodeItem
        v-for="child in node.children"
        :key="child.id"
        :node="child"
        :kbId="kbId"
        :activeNoteId="activeNoteId"
        :canEdit="canEdit"
        @select="$emit('select', $event)"
        @refresh="$emit('refresh')"
        @move-request="emit('move-request', $event)"
      />
    </ul>

    <!-- 移动对话框 -->
    <el-dialog v-model="moveDialogVisible" title="移动到" width="450px">
      <el-form :model="moveForm" label-position="top">
        <el-form-item label="目标知识库">
          <el-select v-model="moveForm.targetKbId" placeholder="请选择知识库" style="width:100%">
            <el-option
              v-for="kb in kbList"
              :key="kb.id"
              :label="kb.name"
              :value="kb.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="目标父笔记ID（选填，不填则移到根目录）">
          <el-input v-model="moveForm.targetParentDocId" placeholder="输入笔记ID" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="moveDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="moving" @click="confirmMove">确定</el-button>
      </template>
    </el-dialog>

    <!-- 导出格式对话框 -->
    <el-dialog v-model="exportDialogVisible" title="选择导出格式" width="300px">
      <el-select v-model="exportFormat" placeholder="请选择格式" style="width:100%">
        <el-option
          v-for="fmt in availableFormats"
          :key="fmt.value"
          :label="fmt.label"
          :value="fmt.value"
        />
      </el-select>
      <template #footer>
        <el-button @click="exportDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleExport">导出</el-button>
      </template>
    </el-dialog>
  </li>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { createNote, deleteNote, moveNote  } from '@/api/note'
import { getMyKbList } from '@/api/kb'
import TreeNodeItem from './NoteTreeItem.vue'

const props = defineProps({
  node: { type: Object, required: true },
  kbId: { type: [String, Number], required: true },
  activeNoteId: { type: Number, default: null },
  canEdit: { type: Boolean, default: true }
})
const emit = defineEmits(['select', 'refresh', 'move-request'])

const router = useRouter()

// 导出相关
const exportDialogVisible = ref(false)
const exportFormat = ref('word')

// ========== 展开/折叠状态持久化 ==========
const STORAGE_KEY = 'note_tree_expanded'
let expandedSet = new Set()
try {
  const stored = localStorage.getItem(STORAGE_KEY)
  if (stored) {
    expandedSet = new Set(JSON.parse(stored))
  }
} catch {}

const expanded = ref(expandedSet.has(props.node.id))   // 使用存储的初始值


const availableFormats = computed(() => {
  switch (props.node.type) {
    case 'doc':
      return [
        { label: 'Word (.docx)', value: 'word' },
        { label: 'Markdown', value: 'markdown' },
        { label: 'PDF', value: 'pdf' },
        { label: '.lake 系统格式', value: 'lake' },
        // 若后端 JPG 导出文档仍有问题，可暂时注释下一行
        { label: 'JPG 长图', value: 'jpg' },
      ]
    case 'table':
      return [
        { label: 'Excel (.xlsx)', value: 'xlsx' },
      ]
    case 'board':
      return [
        { label: 'JPG 长图', value: 'jpg' },
      ]
    default:
      return []
  }
})

function saveExpandedState() {
  if (expanded.value) {
    expandedSet.add(props.node.id)
  } else {
    expandedSet.delete(props.node.id)
  }
  localStorage.setItem(STORAGE_KEY, JSON.stringify([...expandedSet]))
}

function toggle() {
  expanded.value = !expanded.value
  saveExpandedState()
}

const hasChildren = computed(() => props.node.children && props.node.children.length > 0)

// ---------- 拖拽相关 ----------
const isDragOver = ref(false)
let draggedNoteId = null

function onDragStart(e) {
  draggedNoteId = props.node.id
  e.dataTransfer.effectAllowed = 'move'
  e.dataTransfer.setData('text/plain', props.node.id)
}

function onDragOver(e) {
  e.dataTransfer.dropEffect = 'move'
  isDragOver.value = true
}

function onDragLeave() {
  isDragOver.value = false
}

async function onDrop(e) {
  isDragOver.value = false
  const sourceId = e.dataTransfer.getData('text/plain')
  if (!sourceId || sourceId === props.node.id) return

  try {
    await moveNote(sourceId, {
      targetKbId: props.kbId,
      targetParentDocId: props.node.id
    })
    ElMessage.success('移动成功')
    emit('refresh')
    emit('move-request', { id: sourceId, targetKbId: props.kbId, targetParentDocId: props.node.id })
  } catch (e) {
    ElMessage.error('移动失败')
  }
}


function showExportDialog() {
  exportFormat.value = availableFormats.value[0]?.value || 'word'
  exportDialogVisible.value = true
}

async function handleExport() {
  try {
    console.log('执行 Fetch 导出')  // 用于确认进入此函数
    const token = localStorage.getItem('access_token')
    const url = `/api/note/${props.node.id}/export/${exportFormat.value}`

    const response = await fetch(url, {
      headers: { Authorization: `Bearer ${token}` }
    })

    if (!response.ok) {
      // 尝试读取后端返回的错误信息
      let errorMsg = `导出失败（${response.status}）`
      try {
        const errorBody = await response.json()
        if (errorBody.message || errorBody.msg) {
          errorMsg = errorBody.message || errorBody.msg
        }
      } catch {}
      throw new Error(errorMsg)
    }

    const blob = await response.blob()
    const downloadUrl = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = downloadUrl
    a.download = `${props.node.title || 'note'}.${getExtension(exportFormat.value)}`
    document.body.appendChild(a)
    a.click()
    document.body.removeChild(a)
    window.URL.revokeObjectURL(downloadUrl)

    exportDialogVisible.value = false
    ElMessage.success('导出成功')
  } catch (e) {
    console.error('导出失败', e)
    ElMessage.error(e.message || '导出失败')
  }
}

function getContentType(format) {
  const types = {
    word: 'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
    markdown: 'text/markdown',
    pdf: 'application/pdf',
    lake: 'application/octet-stream',
    jpg: 'image/jpeg',
    xlsx: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'
  }
  return types[format] || 'application/octet-stream'
}

function getExtension(format) {
  const exts = { word: 'docx', markdown: 'md', pdf: 'pdf', lake: 'lake', jpg: 'jpg', xlsx: 'xlsx' }
  return exts[format] || format
}


// ---------- 移动对话框 ----------
const moveDialogVisible = ref(false)
const moving = ref(false)
const moveForm = ref({
  targetKbId: null,
  targetParentDocId: ''
})
const kbList = ref([])

function handleSelect() {
  emit('select', props.node)
}

async function fetchKbListIfNeeded() {
  if (kbList.value.length) return
  try {
    const res = await getMyKbList()
    kbList.value = res.records ?? res
  } catch {}
}

// 三点菜单
async function handleMenu(command) {
  if (command === 'delete') {
    try {
      await ElMessageBox.confirm('确定删除此笔记吗？', '提示', {
        confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning'
      })
      const isCurrentNote = props.activeNoteId === props.node.id
      await deleteNote(props.node.id)
      ElMessage.success('笔记已删除')
      // 删除节点时从存储中移除，避免残留
      expandedSet.delete(props.node.id)
      localStorage.setItem(STORAGE_KEY, JSON.stringify([...expandedSet]))
      // 如果删除的是正在查看的笔记，重定向到知识库首页
      if (isCurrentNote) {
        router.push(`/kb/${props.kbId}`)
      }
      emit('refresh')
    } catch (e) {
      if (e !== 'cancel') ElMessage.error('删除失败')
    }
  } else if (command === 'export') {
    showExportDialog()
  } else if (command === 'move') {
    await fetchKbListIfNeeded()
    moveForm.value.targetKbId = Number(props.kbId)
    moveForm.value.targetParentDocId = ''
    moveDialogVisible.value = true
  }
}

async function confirmMove() {
  moving.value = true
  try {
    await moveNote(props.node.id, {
      targetKbId: moveForm.value.targetKbId,
      targetParentDocId: moveForm.value.targetParentDocId || null
    })
    ElMessage.success('移动成功')
    moveDialogVisible.value = false
    emit('refresh')
    emit('move-request', props.node.id)
  } catch (e) {
    ElMessage.error('移动失败')
  } finally {
    moving.value = false
  }
}

function handleCreate(type) {
  router.push(`/note/new?kbId=${props.kbId}&parentDocId=${props.node.id}&type=${type}`)
}

const iconMap = { doc: 'Document', table: 'Grid', board: 'Picture', mind: 'Share' }
const typeIcon = computed(() => iconMap[props.node.type] || 'Document')
</script>

<style scoped>
.tree-node {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  padding: 10px 12px;
  margin-bottom: 6px;
  border-radius: 14px;
  background: #f8fafc;
  transition: background 0.2s ease, transform 0.2s ease;
}

.tree-node:hover {
  background: #eef2ff;
  transform: translateY(-1px);
}

.tree-node.active {
  background: #dbeafe;
  box-shadow: inset 0 0 0 1px rgba(59,130,246,0.2);
}

.arrow, .arrow-placeholder {
  width: 22px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: #6b7280;
}

.type-icon {
  color: #2563eb;
  min-width: 20px;
}

.node-title {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: #111827;
  font-weight: 500;
}

.node-actions {
  display: flex;
  gap: 6px;
  opacity: 0;
  visibility: hidden;
  transition: opacity 0.2s ease;
}

.tree-node:hover .node-actions {
  opacity: 1;
  visibility: visible;
}

.action-btn {
  padding: 4px;
  border-radius: 8px;
  color: #4b5563;
}

.action-btn:hover {
  background: rgba(29,78,216,0.08);
}

.children-list {
  list-style: none;
  padding: 0 0 0 24px;
  margin: 4px 0 0;
  border-left: 1px dashed #dbeafe;
}
</style>