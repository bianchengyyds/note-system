<template>
  <div class="note-editor-page">
    <!-- 顶栏 -->
    <div class="editor-toolbar">
      <div class="title-section">
        <input
          v-model="noteTitle"
          class="title-input"
          placeholder="无标题"
          @input="onTitleInput"
        />
        <span class="note-type-tag">{{ typeLabel }}</span>
      </div>
      <div class="actions">
        <template v-if="route.query.versionId">
          <el-button size="small" @click="goBackToVersions">
            <el-icon><ArrowLeft /></el-icon> 返回
          </el-button>
        </template>
        <template v-else>
          <el-button v-if="!isEditing" @click="enterEditMode" size="small">
            <el-icon><Edit /></el-icon> 编辑
          </el-button>
          <template v-if="isEditing">
            <el-button size="small" :loading="saving" @click="saveContent">
              <el-icon><Check /></el-icon> 保存
            </el-button>
            <el-button size="small" @click="cancelEdit">取消</el-button>
          </template>
          <el-button v-if="!isNew" size="small" :type="liked ? 'primary' : ''" @click="toggleLike" :loading="likeLoading">
            <el-icon><Star /></el-icon> {{ liked ? '已点赞' : '点赞' }}
          </el-button>
          <el-button v-if="!isNew" size="small" :type="favorited ? 'warning' : ''" @click="toggleFavorite" :loading="favLoading">
            <el-icon><Collection /></el-icon> {{ favorited ? '已收藏' : '收藏' }}
          </el-button>
          <el-button v-if="!isNew" size="small" @click="openShareDialog">
            <el-icon><Share /></el-icon> 分享
          </el-button>
          <el-button v-if="!isNew" size="small" @click="openVersions">
            <el-icon><Clock /></el-icon>
            历史版本
          </el-button>
        </template>
      </div>
    </div>

    <el-dialog v-model="shareDialogVisible" title="分享笔记" width="480px">
      <el-form :model="shareForm" label-position="top">
        <el-form-item label="访问密码（可选）">
          <el-input v-model="shareForm.password" placeholder="留空则无需密码" />
        </el-form-item>
        <el-form-item label="有效期">
          <el-radio-group v-model="shareForm.shareDuration">
            <el-radio label="PERMANENT">永久</el-radio>
            <el-radio label="HALF_YEAR">半年</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="权限">
          <el-radio-group v-model="shareForm.permission">
            <el-radio label="READ">仅查看</el-radio>
            <el-radio label="EDIT">可编辑</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="shareDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="shareLoading" @click="createShare">生成分享链接</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="shareResultVisible" title="分享链接已生成" width="480px">
      <p>链接：<el-input v-model="shareUrl" readonly /></p>
      <p>密码（如有）：{{ sharePassword || '无' }}</p>
      <el-button type="primary" @click="copyShareUrl">复制链接</el-button>
    </el-dialog>

    <div class="editor-body">
      <div class="editor-main-layout">
        <div class="editor-scroll-container">
          <template v-if="noteType === 'doc'">
            <TiptapEditor
              ref="editorRef"
              v-model="docContent"
              :kb-id="kbId"
              :editable="isEditing"
              :show-toolbar="isEditing"
              :show-outline="showOutlinePanel"
              placeholder="开始写作... 鼠标移至空行点击左侧 + 号插入组件"
              @change="onEditorContentChange"
              @headings-change="handleHeadingsChange"
              @ready="onEditorReady"
              @open-embed-dialog="showEmbedDialog = true"
              @toggle-outline="showOutlinePanel = !showOutlinePanel"
            />
          </template>

        <template v-else-if="noteType === 'table'">
          <div class="table-editor-container">
            <div v-if="isEditing" class="table-toolbar">
              <el-button size="small" @click="addTableRow">+ 行</el-button>
              <el-button size="small" @click="addTableColumn">+ 列</el-button>
              <el-button size="small" type="danger" @click="deleteTableRow">- 行</el-button>
              <el-button size="small" type="danger" @click="deleteTableColumn">- 列</el-button>
            </div>
            <div v-if="!isEditing" class="spreadsheet-preview">
              <table v-if="tableDataFlat.length" class="mini-table">
                <thead>
                  <tr>
                    <th v-for="col in spreadsheetData.columns" :key="col.key">{{ col.title }}</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="(row, rowIdx) in tableDataFlat" :key="rowIdx">
                    <td v-for="(cell, colIdx) in row" :key="colIdx">{{ cell }}</td>
                  </tr>
                </tbody>
              </table>
              <p v-else style="color:#999;text-align:center;">暂无数据</p>
            </div>
            <VueJSpreadsheet
              v-else
              ref="spreadsheetRef"
              :key="tableKey"
              v-model="tableDataFlat"
              :config="spreadsheetConfig"
              @change="onSpreadsheetChange"
            />
          </div>
        </template>

        <template v-else-if="noteType === 'board'">
          <div class="board-editor-container">
            <div v-if="isEditing" class="board-toolbar">
              <el-button size="small" :type="currentTool === 'select' ? 'primary' : ''" @click="setTool('select')">选择</el-button>
              <el-button size="small" :type="currentTool === 'pen' ? 'primary' : ''" @click="setTool('pen')">画笔</el-button>
              <el-button size="small" :type="currentTool === 'rect' ? 'primary' : ''" @click="setTool('rect')">矩形</el-button>
              <el-button size="small" :type="currentTool === 'circle' ? 'primary' : ''" @click="setTool('circle')">圆形</el-button>
              <el-button size="small" :type="currentTool === 'line' ? 'primary' : ''" @click="setTool('line')">线条</el-button>
              <el-button size="small" :type="currentTool === 'text' ? 'primary' : ''" @click="setTool('text')">文本</el-button>
              <el-divider direction="vertical" />
              <div class="color-group">
                <span class="color-label">颜色</span>
                <el-color-picker v-model="foregroundColor" size="small" @active-change="foregroundColor = $event" />
              </div>
              <el-select v-model="penWidth" size="small" style="width: 80px">
                <el-option :value="2" label="细" />
                <el-option :value="5" label="中" />
                <el-option :value="10" label="粗" />
              </el-select>
              <el-divider direction="vertical" />
              <div class="color-group">
                <span class="color-label">背景</span>
                <el-color-picker v-model="boardBackground" size="small" show-alpha @active-change="boardBackground = $event" />
              </div>
              <el-button size="small" type="danger" @click="deleteSelected">删除选中</el-button>
              <el-button size="small" @click="clearBoard">清空</el-button>
            </div>
            <div class="board-canvas-wrapper">
              <canvas ref="boardCanvasRef" id="boardCanvas" tabindex="0"></canvas>
            </div>
          </div>
        </template>

        <div v-if="!isNew && !isEditing && !route.query.versionId" class="comment-section">
          <h3>评论</h3>
          <div class="comment-input">
            <el-input v-model="commentText" type="textarea" :rows="3" placeholder="写下你的评论..." />
            <el-button type="primary" size="small" :loading="sendingComment" @click="postComment" class="submit-comment">提交</el-button>
          </div>
          <div v-loading="loadingComments" class="comments-list">
            <CommentNode
              v-for="c in comments" :key="c.id" :comment="c" :currentUserId="currentUserId"
              @toggle-like="toggleCommentLike" @delete-comment="deleteCommentItem" @reply-posted="handleReplyPosted"
            />
            <el-empty v-if="!comments.length && !loadingComments" description="暂无评论" />
          </div>
        </div>
        </div>

        <!-- 右侧大纲面板 -->
        <div v-if="noteType === 'doc' && showOutlinePanel" class="outline-sidebar">
          <div class="outline-sidebar-header">
            <span class="outline-sidebar-title">文档大纲</span>
            <el-button text size="small" @click="showOutlinePanel = false">
              <el-icon><Close /></el-icon>
            </el-button>
          </div>
          <div class="outline-sidebar-content">
            <div
              v-for="(heading, idx) in headings"
              :key="idx"
              class="outline-sidebar-item"
              :class="'outline-level-' + heading.level"
              @click="scrollToHeading(heading)"
            >
              {{ heading.text || '无标题' }}
            </div>
            <el-empty v-if="!headings.length" description="暂无标题" :image-size="40" />
          </div>
        </div>
      </div>

      <el-dialog v-model="showEmbedDialog" title="插入嵌套组件" width="520px" :close-on-click-modal="false">
        <el-form :model="embedForm" label-position="top">
          <el-form-item label="组件类型">
            <el-select v-model="embedForm.embedType" placeholder="请选择类型" style="width: 100%" @change="onEmbedTypeChange">
              <el-option label="📊 表格" value="table" />
              <el-option label="🎨 画板" value="board" />
              <el-option label="🧠 思维导图" value="mind" />
            </el-select>
          </el-form-item>
          <el-form-item label="组件标题">
            <el-input v-model="embedForm.title" placeholder="请输入组件笔记标题" />
          </el-form-item>
          <template v-if="embedForm.embedType === 'table'">
            <el-form-item label="表格列定义">
              <div class="embed-columns-editor">
                <div v-for="(col, idx) in embedForm.tableData.columns" :key="idx" class="embed-column-item">
                  <el-input v-model="col.title" placeholder="列名" size="small" style="width:120px" />
                  <el-select v-model="col.type" size="small" style="width:100px">
                    <el-option label="文本" value="text" />
                    <el-option label="数字" value="number" />
                    <el-option label="日期" value="date" />
                    <el-option label="下拉" value="dropdown" />
                  </el-select>
                  <el-button type="danger" size="small" @click="removeEmbedColumn(idx)">×</el-button>
                </div>
                <el-button size="small" @click="addEmbedColumn">+ 添加列</el-button>
              </div>
            </el-form-item>
            <el-form-item label="初始行数">
              <el-input-number v-model="embedForm.tableData.rowCount" :min="1" :max="20" />
            </el-form-item>
          </template>
          <template v-if="embedForm.embedType === 'board'">
            <el-form-item label="背景颜色">
              <el-color-picker v-model="embedForm.boardData.background" />
            </el-form-item>
            <el-form-item label="初始元素">
              <div style="color:#999;font-size:13px;">保存后可在画板编辑器中添加元素</div>
            </el-form-item>
          </template>
          <template v-if="embedForm.embedType === 'mind'">
            <el-form-item label="中心主题">
              <el-input v-model="embedForm.mindData.centerTopic" placeholder="请输入中心主题" />
            </el-form-item>
          </template>
        </el-form>
        <template #footer>
          <el-button @click="showEmbedDialog = false">取消</el-button>
          <el-button type="primary" :loading="embedLoading" @click="insertEmbedToEditor">创建并插入组件</el-button>
        </template>
      </el-dialog>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, watch, onMounted, onBeforeUnmount, nextTick, provide } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getNoteDetail, updateNote, createNote, getVersionDetail } from '@/api/note'
import { likeNote, unlikeNote, favoriteNote, unfavoriteNote, recordView } from '@/api/note'
import { marked } from 'marked'
import VueJSpreadsheet from 'vue3_jspreadsheet'
import 'vue3_jspreadsheet/dist/vue3_jspreadsheet.css'
import formula from '@jspreadsheet/formula'
import * as fabric from 'fabric'
import { getToken, getUserInfo } from '@/utils/auth'
import { shareNote } from '@/api/note'
import { createComment, getNoteComments, deleteComment, likeComment as likeCommentApi, unlikeComment as unlikeCommentApi } from '@/api/comment'
import { createDocEmbed, deleteDocEmbed, getDocEmbeds } from '@/api/doc'
import CommentNode from '@/components/comment/CommentNode.vue'
// 替换 Vditor 为 Tiptap
import TiptapEditor from '@/components/note/TiptapEditor.vue'
// import RichTextEditor from '@/components/note/RichTextEditor.vue' // 已废弃
import {
  mountEmbedSlots,
  insertEmbedSlotAtCursor,
  unmountAllEmbedApps,
  saveAllEmbedsSilent,
  embedApps,
  embedMarksToHtml,
  extractDocContentFromEditor,
  scheduleHydrateEmbeds,
  reloadEditorContent,
  hydrateEmbeds
} from '@/utils/vditorEmbed'
import { normalizeDocContent, extractEmbedRefs, buildEmbedMark } from '@/utils/docEmbed'

const route = useRoute()
const router = useRouter()

const comments = ref([])
const loadingComments = ref(false)
const commentText = ref('')
const sendingComment = ref(false)
const currentUserId = ref(null)

const embedForm = reactive({
  embedType: 'table',
  title: '新建组件',
  tableData: { columns: [{ key: 'col_0', title: '列1', type: 'text' }], rowCount: 3 },
  boardData: { elements: [], background: '#ffffff' },
  mindData: { centerTopic: '中心主题' }
})

function resetEmbedForm() {
  embedForm.embedType = 'table'
  embedForm.title = '新建组件'
  embedForm.tableData.columns = [{ key: 'col_0', title: '列1', type: 'text' }]
  embedForm.tableData.rowCount = 3
  embedForm.boardData.background = '#ffffff'
  embedForm.mindData.centerTopic = '中心主题'
}

function onEmbedTypeChange() {
  if (embedForm.embedType === 'table') embedForm.title = '新建表格'
  else if (embedForm.embedType === 'board') embedForm.title = '新建画板'
  else if (embedForm.embedType === 'mind') embedForm.title = '新建思维导图'
}

function addEmbedColumn() {
  const idx = embedForm.tableData.columns.length
  embedForm.tableData.columns.push({ key: `col_${idx}`, title: `列${idx + 1}`, type: 'text' })
}
function removeEmbedColumn(index) {
  if (embedForm.tableData.columns.length <= 1) return
  embedForm.tableData.columns.splice(index, 1)
}

const embedList = ref([])
const embedLoading = ref(false)
const showEmbedDialog = ref(false)
const showEmbedForm = ref(false)
const removingEmbedId = ref(null)
const showOutlinePanel = ref(true)

const isNew = computed(() => route.params.docId === 'new')
const noteId = computed(() => route.params.docId)
const kbId = computed(() => route.query.kbId)
const noteType = ref(route.query.type || 'doc')

// 防止在插入组件过程中因URL变化触发重复加载
const isInsertingEmbed = ref(false)

// 暂存新建文档时待创建的嵌套组件（保存时一并处理）
const pendingEmbeds = ref([])

watch(() => route.query.type, (newType) => { if (newType) noteType.value = newType })

const noteTitle = ref('')
const isEditing = ref(false)
provide('isNoteEditing', isEditing)
const saving = ref(false)
const originalTitle = ref('')
const originalContent = ref('')
const docContent = ref('')
const editorRef = ref(null)

const tableDataFlat = ref([])
const spreadsheetData = ref({ columns: [], rows: [] })
const spreadsheetConfig = ref({})
const spreadsheetPreviewRef = ref(null)
const tableKey = ref(0)

// 监听 tableKey 变化，在表格渲染后隐藏行号列
watch(tableKey, () => {
  nextTick(() => {
    setTimeout(() => {
      // 查找所有 jspreadsheet 的 table 元素并添加 jss_hidden_index 类
      const tables = document.querySelectorAll('.jss_worksheet')
      tables.forEach(ws => {
        const table = ws.closest('table') || ws.querySelector('table')
        if (table) {
          table.classList.add('jss_hidden_index')
        }
      })
      // 也直接通过 style 隐藏
      const style = document.createElement('style')
      style.textContent = `
        .table-editor-container .jss_worksheet > tbody > tr > td:first-child,
        .table-editor-container .jss_worksheet > thead > tr > td:first-child {
          display: none !important;
        }
      `
      if (!document.querySelector('style[data-hide-index]')) {
        style.setAttribute('data-hide-index', 'true')
        document.head.appendChild(style)
      }
    }, 100)
  })
})

const boardCanvasRef = ref(null)
let boardCanvas = null
const boardElements = ref([])
const boardBgColor = ref('#ffffff')
const boardBackground = ref('#ffffff')
const currentTool = ref('select')
const foregroundColor = ref('#000000')
const penWidth = ref(2)
let startPoint = null

// 画板历史记录栈，用于撤销和重做
const boardHistory = ref([])
const boardHistoryIndex = ref(-1)
const isHandlingBoardHistory = ref(false)

/** 记录当前画板状态到历史记录 */
function saveBoardHistory() {
  if (!boardCanvas || isHandlingBoardHistory.value) return
  
  // 序列化当前画布元素和背景
  const elements = []
  boardCanvas.getObjects().forEach(obj => {
    const el = { id: obj.id || `el_${Date.now()}`, type: obj.type }
    if (obj.type === 'rect') {
      el.x = obj.left ?? 0; el.y = obj.top ?? 0; el.width = obj.width * (obj.scaleX || 1); el.height = obj.height * (obj.scaleY || 1); el.fill = obj.fill; el.stroke = obj.stroke; el.strokeWidth = obj.strokeWidth; el.content = ''
    } else if (obj.type === 'circle') {
      el.x = obj.left ?? 0; el.y = obj.top ?? 0; el.width = (obj.radius || 40) * 2; el.height = (obj.radius || 40) * 2; el.fill = obj.fill; el.stroke = obj.stroke; el.strokeWidth = obj.strokeWidth; el.radius = obj.radius; el.content = ''
    } else if (obj.type === 'line') {
      el.startX = obj.x1 ?? 0; el.startY = obj.y1 ?? 0; el.endX = obj.x2 ?? 0; el.endY = obj.y2 ?? 0; el.stroke = obj.stroke; el.strokeWidth = obj.strokeWidth; el.content = ''
    } else if (obj.type === 'i-text') {
      el.x = obj.left ?? 0; el.y = obj.top ?? 0; el.width = obj.width * (obj.scaleX || 1); el.height = obj.height * (obj.scaleY || 1); el.text = obj.text; el.fontSize = obj.fontSize; el.fill = obj.fill; el.fontFamily = obj.fontFamily; el.content = obj.text || ''
    } else if (obj.type === 'path') {
      el.x = obj.left ?? 0; el.y = obj.top ?? 0; el.width = obj.width * (obj.scaleX || 1); el.height = obj.height * (obj.scaleY || 1); el.stroke = obj.stroke; el.strokeWidth = obj.strokeWidth; el.fill = obj.fill; el.content = obj.path ? JSON.stringify(obj.path) : ''
    }
    elements.push(el)
  })

  const state = {
    elements,
    background: boardCanvas.backgroundColor
  }
  
  const stateStr = JSON.stringify(state)
  
  if (boardHistoryIndex.value >= 0 && boardHistory.value[boardHistoryIndex.value] === stateStr) return

  if (boardHistoryIndex.value < boardHistory.value.length - 1) {
    boardHistory.value = boardHistory.value.slice(0, boardHistoryIndex.value + 1)
  }
  
  boardHistory.value.push(stateStr)
  if (boardHistory.value.length > 50) {
    boardHistory.value.shift()
  } else {
    boardHistoryIndex.value++
  }
}

/** 撤销画板操作 */
function undoBoard() {
  if (boardHistoryIndex.value <= 0) return
  boardHistoryIndex.value--
  applyBoardHistoryState(boardHistory.value[boardHistoryIndex.value])
}

/** 重画板操作 */
function redoBoard() {
  if (boardHistoryIndex.value >= boardHistory.value.length - 1) return
  boardHistoryIndex.value++
  applyBoardHistoryState(boardHistory.value[boardHistoryIndex.value])
}

/** 应用画板历史记录状态 */
async function applyBoardHistoryState(stateStr) {
  if (!boardCanvas) return
  isHandlingBoardHistory.value = true
  try {
    const state = JSON.parse(stateStr)
    boardCanvas.clear()
    boardCanvas.backgroundColor = state.background || '#ffffff'
    
    state.elements.forEach(el => {
      let obj = null
      try {
        if (el.type === 'rect') obj = new fabric.Rect({ left: el.x ?? 0, top: el.y ?? 0, width: el.width ?? 80, height: el.height ?? 60, fill: el.fill ?? '#cccccc', stroke: el.stroke ?? '#333333', strokeWidth: el.strokeWidth ?? 1 })
        else if (el.type === 'circle') { const radius = el.radius ?? (el.width ?? 80) / 2; obj = new fabric.Circle({ left: el.x ?? 0, top: el.y ?? 0, radius: radius, fill: el.fill ?? '#cccccc', stroke: el.stroke ?? '#333333', strokeWidth: el.strokeWidth ?? 1 }) }
        else if (el.type === 'line') obj = new fabric.Line([el.startX ?? 0, el.startY ?? 0, el.endX ?? 100, el.endY ?? 0], { stroke: el.stroke ?? '#333333', strokeWidth: el.strokeWidth ?? 2 })
        else if (el.type === 'i-text') obj = new fabric.IText(el.text ?? el.content ?? '', { left: el.x ?? 0, top: el.y ?? 0, fontSize: el.fontSize ?? 20, fill: el.fill ?? '#000000', fontFamily: el.fontFamily ?? 'Arial' })
        else if (el.type === 'path' && el.content) { const pathData = typeof el.content === 'string' ? JSON.parse(el.content) : el.content; obj = new fabric.Path(pathData, { left: el.x ?? 0, top: el.y ?? 0, stroke: el.stroke ?? '#000000', strokeWidth: el.strokeWidth ?? 2, fill: el.fill ?? null }) }
        if (obj) { obj.id = el.id; boardCanvas.add(obj) }
      } catch (e) { console.warn('历史记录恢复元素失败', e) }
    })
    boardCanvas.renderAll()
  } finally {
    isHandlingBoardHistory.value = false
  }
}

const liked = ref(false)
const favorited = ref(false)
const likeLoading = ref(false)
const favLoading = ref(false)
const LIKE_STORE_KEY = 'note_liked_map'
const FAV_STORE_KEY = 'note_favorited_map'

function getCurrentUserId() {
  const userInfo = JSON.parse(localStorage.getItem('user_info') || '{}')
  return userInfo.id
}

async function handleReplyPosted({ parentCommentId, content }) {
  try {
    await createComment({ noteId: noteId.value, parentCommentId, content })
    ElMessage.success('回复成功')
    loadComments()
  } catch (e) { ElMessage.error('回复失败') }
}

async function loadComments() {
  if (isNew.value) return
  loadingComments.value = true
  try {
    const data = await getNoteComments(noteId.value)
    const flatList = data.records ?? (Array.isArray(data) ? data : [])
    comments.value = buildCommentTree(flatList)
  } catch (e) { console.error('加载评论失败', e) } finally { loadingComments.value = false }
}

function buildCommentTree(list) {
  const map = {}
  const roots = []
  list.forEach(item => { map[item.id] = { ...item, replies: [] } })
  list.forEach(item => {
    const node = map[item.id]
    if (item.parentCommentId && map[item.parentCommentId]) map[item.parentCommentId].replies.push(node)
    else roots.push(node)
  })
  return roots
}
function formatDate(dateStr) { if (!dateStr) return ''; return new Date(dateStr).toLocaleString('zh-CN') }

async function postComment() {
  const text = commentText.value.trim()
  if (!text) return
  sendingComment.value = true
  try {
    await createComment({ noteId: noteId.value, parentCommentId: null, content: text })
    commentText.value = ''
    ElMessage.success('评论成功')
    loadComments()
  } catch (e) { ElMessage.error('评论失败') } finally { sendingComment.value = false }
}

async function deleteCommentItem(commentId) {
  try { await deleteComment(commentId); ElMessage.success('评论已删除'); loadComments() }
  catch (e) { ElMessage.error('删除失败') }
}

async function toggleCommentLike(comment) {
  try {
    if (comment.hasLiked) { await unlikeCommentApi(comment.id); comment.hasLiked = false; comment.likeCount = Math.max(0, (comment.likeCount || 0) - 1) }
    else { await likeCommentApi(comment.id); comment.hasLiked = true; comment.likeCount = (comment.likeCount || 0) + 1 }
  } catch (e) { ElMessage.error('操作失败') }
}

onMounted(() => { currentUserId.value = getCurrentUserId() })

function getStoredMap(key) { try { const json = localStorage.getItem(key); return json ? JSON.parse(json) : {} } catch { return {} } }
function setStoredMap(key, map) { localStorage.setItem(key, JSON.stringify(map)) }
const likedMap = reactive(getStoredMap(LIKE_STORE_KEY))
const favoritedMap = reactive(getStoredMap(FAV_STORE_KEY))

function initLikeFavFromStorage(backendHasLiked, backendHasFavorited) {
  const id = noteId.value
  liked.value = backendHasLiked !== undefined ? backendHasLiked : !!likedMap[id]
  favorited.value = backendHasFavorited !== undefined ? backendHasFavorited : !!favoritedMap[id]
}

const tiptapEditor = ref(null)
const editorContent = ref('')
let titleSaveTimer = null
const headings = ref([])

const handleHeadingsChange = (h) => {
  headings.value = h
}

const scrollToHeading = (heading) => {
  if (!editorRef.value?.editor) return
  const { editor } = editorRef.value
  editor.commands.setTextSelection(heading.pos)
  editor.commands.focus()
  
  const scrollContainer = document.querySelector('.editor-scroll-container')
  if (scrollContainer) {
    const dom = editor.view.nodeDOM(heading.pos)
    if (dom && dom instanceof HTMLElement) {
      const top = dom.offsetTop - 80
      scrollContainer.scrollTo({ top: Math.max(0, top), behavior: 'smooth' })
    }
  }
}

async function hydrateEditorEmbeds() {
  if (!tiptapEditor.value) return
  const editor = tiptapEditor.value.editor
  if (!editor) return
  await loadEmbedContent()
  await hydrateEmbeds(editor)
}

function destroyEditor() {
  unmountAllEmbedApps()
}

const typeLabel = computed(() => {
  const m = { doc: '文档', table: '表格', board: '画板', mind: '思维导图' }
  return m[noteType.value] || '笔记'
})

// 嵌套组件缓存：直接通过 getNoteDetail 加载每个嵌套组件的完整笔记内容
const embedCache = reactive({})
const embedLoadingMap = reactive({})

// 渲染思维导图节点的递归函数
function renderMindNodesHtml(nodes, level = 0) {
  if (!nodes || !nodes.length) return ''
  return `<ul class="embed-mind-list" style="padding-left:${level * 20 + 20}px;list-style:none;margin:0;">
    ${nodes.map(n => `<li class="embed-mind-item" style="margin:4px 0;">
      <span class="embed-mind-node" style="display:inline-flex;align-items:center;gap:6px;padding:4px 12px;background:#e8f4fd;border-radius:6px;font-size:14px;color:#1f2937;">
        <span style="font-size:12px;">●</span>${n.title}
      </span>
      ${renderMindNodesHtml(n.children, level + 1)}
    </li>`).join('')}
  </ul>`
}

// 替换 {{embed|type|noteId}} 为组件真实内容的 HTML
function replaceEmbedWithPlaceholder(htmlContent, match, type, noteId) {
  const embedKey = `${type}_${noteId}`
  const cached = embedCache[embedKey]

  if (cached && cached.error) {
    return htmlContent.replace(match, `<div class="embed-placeholder error">❌ ${cached.title || '加载失败'}</div>`)
  }

  if (!cached || !cached.data) {
    return htmlContent.replace(match, `<div class="embed-placeholder loading" data-embed-key="${embedKey}">⏳ 加载中...</div>`)
  }

  const note = cached.data

  let contentHtml = ''
  if (type === 'table') {
    let cols = note.columns || []
    let rows = note.rows || []
    
    // 按照 mind 的方式，如果 columns/rows 为空，尝试从 content 解析
    if ((!cols.length || !rows.length) && note.content) {
      try {
        const parsed = JSON.parse(note.content)
        cols = parsed.columns || cols
        rows = parsed.rows || rows
      } catch (e) {
        console.warn('预览解析表格 content 失败:', e)
      }
    }

    contentHtml = `<div class="embed-render-wrapper" style="margin:16px 0;border:1px solid #e5e6eb;border-radius:10px;overflow:hidden;background:#fff;">
      <div class="embed-render-header" style="display:flex;align-items:center;gap:8px;padding:8px 16px;background:#f0f7ff;border-bottom:1px solid #d0e3ff;">
        <span style="font-size:16px;">📊</span>
        <span style="display:inline-block;padding:1px 8px;background:#e8f4fd;border-radius:4px;font-size:12px;color:#666;">表格</span>
      </div>
      <div class="embed-render-body" style="padding:12px 16px;overflow-x:auto;">
        <table class="embed-mini-table" style="width:100%;border-collapse:collapse;font-size:13px;">
          <thead><tr>${cols.map(c => `<th style="border:1px solid #e5e6eb;padding:8px 10px;background:#f9fafb;font-weight:600;text-align:left;white-space:nowrap;">${(c.title || c.key || '').replace(/</g, '<')}</th>`).join('')}</tr></thead>
          <tbody>${rows.map(r => `<tr>${cols.map(c => `<td style="border:1px solid #e5e6eb;padding:6px 10px;color:#374151;">${((r.cells && r.cells[c.key]) || '').replace(/</g, '<')}</td>`).join('')}</tr>`).join('')}</tbody>
        </table>
        ${!rows.length ? '<p style="color:#999;text-align:center;padding:16px;">暂无数据行</p>' : ''}
      </div>
    </div>`
  } else if (type === 'board') {
    let elements = note.elements || []
    let bg = note.background || '#ffffff'
    
    // 按照 mind 的方式，如果 elements 为空，尝试从 content 解析
    if (!elements.length && note.content) {
      try {
        const parsed = JSON.parse(note.content)
        elements = parsed.elements || []
        bg = parsed.background || bg
      } catch (e) {
        console.warn('预览解析画板 content 失败:', e)
      }
    }

    contentHtml = `<div class="embed-render-wrapper" style="margin:16px 0;border:1px solid #e5e6eb;border-radius:10px;overflow:hidden;background:#fff;">
      <div class="embed-render-header" style="display:flex;align-items:center;gap:8px;padding:8px 16px;background:#fff7e6;border-bottom:1px solid #ffe4ba;">
        <span style="font-size:16px;">🎨</span>
        <span style="display:inline-block;padding:1px 8px;background:#fef3e2;border-radius:4px;font-size:12px;color:#b45309;">画板</span>
      </div>
      <div class="embed-render-body" style="padding:16px;">
        <div class="embed-board-preview" style="background:${bg};min-height:200px;border:1px solid #e5e6eb;border-radius:8px;display:flex;flex-direction:column;align-items:center;justify-content:center;color:#666;gap:8px;">
          <span style="font-size:32px;">🎨</span>
          <span style="font-size:14px;">${elements.length} 个元素</span>
          <span style="font-size:12px;color:#999;">背景色: ${bg}</span>
        </div>
      </div>
    </div>`
  } else if (type === 'mind') {
    let mindData = note.mindData || note.nodes || []
    
    // 按照 mind 的方式，如果 mindData 为空，尝试从 content 解析
    if ((!mindData || !mindData.length) && note.content) {
      try {
        const parsed = JSON.parse(note.content)
        mindData = parsed.mindData || parsed.nodes || parsed.elements || []
      } catch (e) {
        console.warn('预览解析思维导图 content 失败:', e)
      }
    }

    contentHtml = `<div class="embed-render-wrapper" style="margin:16px 0;border:1px solid #e5e6eb;border-radius:10px;overflow:hidden;background:#fff;">
      <div class="embed-render-header" style="display:flex;align-items:center;gap:8px;padding:8px 16px;background:#f0fff4;border-bottom:1px solid #c6f6d5;">
        <span style="font-size:16px;">🧠</span>
        <span style="display:inline-block;padding:1px 8px;background:#e6ffed;border-radius:4px;font-size:12px;color:#166534;">思维导图</span>
      </div>
      <div class="embed-render-body" style="padding:12px 16px;max-height:400px;overflow-y:auto;">
        ${renderMindNodesHtml(mindData)}
      </div>
    </div>`
  } else {
    contentHtml = `<div class="embed-placeholder" style="border:2px solid #409eff;background:#f0f7ff;margin:16px 0;padding:16px;border-radius:8px;text-align:center;">
      <span style="font-size:24px;">📎</span>
    </div>`
  }

  return htmlContent.replace(match, contentHtml)
}

// 渲染带嵌套组件的内容
// 将 markdown 转换为可用于 Tiptap 显示的 HTML（含 embed 标记替换为 slot）
function markdownToTiptapHtml(content) {
  if (!content) return ''
  let html = ''
  try { html = marked(content) }
  catch (e) { html = content.replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/\n/g, '<br>') }
  return embedMarksToHtml(html)
}

// 策略：先用唯一占位符替换 embed 标记 → marked() 转换 → 再把占位符换回真实 HTML
// 这样可以避免 marked() 砊坏 embed 的 HTML 结构（产生多余的 </div> / </li> 等）
let embedSeq = 0
const EMBED_PH_PREFIX = 'EMBED_PLACEHOLDER_'

function buildEmbedPlaceholderMap(content) {
  const map = {}
  embedSeq = 0
  const normalized = normalizeDocContent(content)
  const embedRegex = /\{\{embed\|(\w+)\|(\d+)\}\}/g
  let match
  while ((match = embedRegex.exec(normalized)) !== null) {
    const [fullMatch, type, noteId] = match
    const key = `${EMBED_PH_PREFIX}${embedSeq}`
    map[key] = { type, noteId, fullMatch }
    embedSeq++
  }
  return map
}

const renderedContent = computed(() => {
  if (noteType.value !== 'doc') return ''
  let content = normalizeDocContent(originalContent.value || '')
  if (!content) return '<p style="color:#999">暂无内容</p>'

  // 步骤1：收集并替换 embed 标记为纯文本占位符
  const placeholderMap = buildEmbedPlaceholderMap(content)
  for (const [key, info] of Object.entries(placeholderMap)) {
    content = content.replace(info.fullMatch, key)
  }

  // 步骤2：Markdown 转换（此时 embed 占位符是纯文本，不会被 marked 破坏）
  let html
  try { html = marked(content) }
  catch (e) { html = content.replace(/</g, '<').replace(/>/g, '>').replace(/\n/g, '<br>') }

  // 步骤3：将占位符替换为真实的 embed HTML（不再经过 marked，保持结构完整）
  for (const [key, info] of Object.entries(placeholderMap)) {
    const embedHtml = replaceEmbedWithPlaceholder(key, key, info.type, info.noteId)
    html = html.replace(key, embedHtml)
  }

  return html
})

// 主方法：通过 getNoteDetail 加载每个嵌套组件的完整笔记内容
async function loadEmbedContent() {
  if (noteType.value !== 'doc') return
  if (isNew.value) return // 新建文档未保存，跳过加载嵌套组件
  const refs = extractEmbedRefs(originalContent.value || '')

  for (const { embedType: type, noteId } of refs) {
    const embedKey = `${type}_${noteId}`
    if (embedCache[embedKey]?.data) continue // 已有数据则跳过

    embedLoadingMap[embedKey] = true
    try {
      const res = await getNoteDetail(noteId)
      embedCache[embedKey] = { type, noteId, title: res.title || '笔记', data: res }
    } catch (e) {
      console.error(`加载嵌套组件 ${noteId} 失败:`, e)
      embedCache[embedKey] = { type, noteId, title: '加载失败', error: true }
    } finally {
      embedLoadingMap[embedKey] = false
    }
  }
}

async function toggleLike() {
  likeLoading.value = true
  try {
    if (liked.value) { await unlikeNote(noteId.value); liked.value = false; ElMessage.success('已取消点赞') }
    else { await likeNote(noteId.value); liked.value = true; ElMessage.success('点赞成功') }
  } catch (e) {
    if (e.response && e.response.status === 400) { liked.value = true; ElMessage.info('笔记已点赞') }
    else ElMessage.error('操作失败')
  } finally {
    likedMap[noteId.value] = liked.value
    setStoredMap(LIKE_STORE_KEY, { ...likedMap })
    likeLoading.value = false
  }
}

async function toggleFavorite() {
  favLoading.value = true
  try {
    if (favorited.value) { await unfavoriteNote(noteId.value); favorited.value = false; ElMessage.success('已取消收藏') }
    else { await favoriteNote(noteId.value); favorited.value = true; ElMessage.success('收藏成功') }
  } catch (e) {
    if (e.response && e.response.status === 400) { favorited.value = true; ElMessage.info('笔记已收藏') }
    else ElMessage.error('操作失败')
  } finally {
    favoritedMap[noteId.value] = favorited.value
    setStoredMap(FAV_STORE_KEY, { ...favoritedMap })
    favLoading.value = false
  }
}

function openVersions() { router.push(`/note/${noteId.value}/versions?title=${encodeURIComponent(noteTitle.value)}&kbId=${kbId.value}&type=${noteType.value}`) }
function goBackToVersions() { router.push(`/note/${noteId.value}/versions?title=${encodeURIComponent(noteTitle.value)}&kbId=${kbId.value}&type=${noteType.value}`) }

function buildConfig(columns, isReadOnly) {
  const columnTypes = columns.map(col => {
    const typeMap = { text: { type: 'text' }, number: { type: 'numeric', mask: '#,##0', decimal: '.' }, date: { type: 'calendar', options: { format: 'YYYY-MM-DD' } }, dropdown: { type: 'dropdown', source: ['选项1', '选项2'] }, checkbox: { type: 'checkbox' }, color: { type: 'color' }, html: { type: 'html' }, image: { type: 'image' } }
    const mapped = typeMap[col.type] || typeMap.text
    return { ...mapped, title: col.title || col.key, width: col.width || 120, textAlign: 'center' }
  })
  return {
    columns: columnTypes,
    indexColumns: false,
    allowInsertRow: !isReadOnly,
    allowInsertColumn: !isReadOnly,
    allowDeleteRow: !isReadOnly,
    allowDeleteColumn: !isReadOnly,
    allowRenameColumn: !isReadOnly,
    columnSorting: true,
    columnDrag: !isReadOnly,
    columnResize: true,
    rowDrag: !isReadOnly,
    rowResize: true,
    tableOverflow: true,
    tableHeight: '600px',
    minSpareRows: 1,
    contextMenu: true,
    selectionCopy: !isReadOnly,
    plugins: [formula],
    formula: true,
    onload: (instance) => {
      const table = instance.table
      if (table) {
        table.classList.add('jss_hidden_index')
      }
    },
  }
}

function rebuildConfig(isReadOnly) {
  const cols = spreadsheetData.value.columns.length ? spreadsheetData.value.columns : tableDataFlat.value[0] ? tableDataFlat.value[0].map((_, i) => ({ key: `col_${i}`, title: String.fromCharCode(65 + i), type: 'text' })) : [{ title: 'A' }, { title: 'B' }, { title: 'C' }]
  spreadsheetConfig.value = buildConfig(cols, isReadOnly)
}

function addTableRow() {
  const cols = spreadsheetData.value.columns.length ? spreadsheetData.value.columns : tableDataFlat.value[0] ? tableDataFlat.value[0].map((_, i) => ({ key: `col_${i}`, title: String.fromCharCode(65 + i), type: 'text' })) : [{ key: 'col_0', title: 'A', type: 'text' }]
  const newRow = cols.map(() => '')
  tableDataFlat.value = [...tableDataFlat.value, newRow]
  rebuildConfig(false)
  tableKey.value++
}

function addTableColumn() {
  const idx = spreadsheetData.value.columns.length
  const newCol = { key: `col_${Date.now()}`, title: String.fromCharCode(65 + (idx % 26)), type: 'text' }
  spreadsheetData.value.columns = [...spreadsheetData.value.columns, newCol]
  tableDataFlat.value = tableDataFlat.value.map(row => [...row, ''])
  rebuildConfig(false)
  tableKey.value++
}

function deleteTableRow() { if (tableDataFlat.value.length <= 1) { ElMessage.warning('至少保留一行'); return }; tableDataFlat.value = tableDataFlat.value.slice(0, -1); rebuildConfig(false); tableKey.value++ }
function deleteTableColumn() { if (spreadsheetData.value.columns.length <= 1) { ElMessage.warning('至少保留一列'); return }; spreadsheetData.value.columns = spreadsheetData.value.columns.slice(0, -1); tableDataFlat.value = tableDataFlat.value.map(row => row.slice(0, -1)); rebuildConfig(false); tableKey.value++ }

function onTableReady(instance) {
  if (instance && typeof instance.hideIndex === 'function') {
    instance.hideIndex()
  }
}

function initBoardCanvas(readonly = false) {
  if (!boardCanvasRef.value) return
  if (boardCanvas) boardCanvas.dispose()
  boardCanvas = new fabric.Canvas('boardCanvas', { backgroundColor: boardBgColor.value, width: 800, height: 600, selection: !readonly, selectionColor: readonly ? 'transparent' : undefined, selectionBorderColor: readonly ? 'transparent' : undefined })
  if (!readonly) {
    boardCanvas.on('mouse:down', (opt) => {
      // 如果点击了对象且当前不是选择工具，自动切换到选择工具
      if (opt.target && currentTool.value !== 'select') {
        setTool('select')
      }
      onCanvasMouseDown(opt)
    })
    boardCanvas.on('mouse:up', onCanvasMouseUp)
    boardCanvas.on('selection:created', syncForegroundColor)
    boardCanvas.on('selection:updated', syncForegroundColor)
    
    // 监听对象变化以记录历史
    boardCanvas.on('object:added', saveBoardHistory)
    boardCanvas.on('object:modified', saveBoardHistory)
    boardCanvas.on('object:removed', saveBoardHistory)
    
    window.addEventListener('keydown', onKeyDown)
  }
  boardElements.value.forEach(el => {
    let obj = null
    try {
      if (el.type === 'rect') obj = new fabric.Rect({ left: el.x ?? 0, top: el.y ?? 0, width: el.width ?? 80, height: el.height ?? 60, fill: el.fill ?? '#cccccc', stroke: el.stroke ?? '#333333', strokeWidth: el.strokeWidth ?? 1 })
      else if (el.type === 'circle') { const radius = el.radius ?? (el.width ?? 80) / 2; obj = new fabric.Circle({ left: el.x ?? 0, top: el.y ?? 0, radius: radius, fill: el.fill ?? '#cccccc', stroke: el.stroke ?? '#333333', strokeWidth: el.strokeWidth ?? 1 }) }
      else if (el.type === 'line') obj = new fabric.Line([el.startX ?? 0, el.startY ?? 0, el.endX ?? 100, el.endY ?? 0], { stroke: el.stroke ?? '#333333', strokeWidth: el.strokeWidth ?? 2 })
      else if (el.type === 'i-text') obj = new fabric.IText(el.text ?? el.content ?? '', { left: el.x ?? 0, top: el.y ?? 0, fontSize: el.fontSize ?? 20, fill: el.fill ?? '#000000', fontFamily: el.fontFamily ?? 'Arial' })
      else if (el.type === 'path' && el.content) { const pathData = typeof el.content === 'string' ? JSON.parse(el.content) : el.content; obj = new fabric.Path(pathData, { left: el.x ?? 0, top: el.y ?? 0, stroke: el.stroke ?? '#000000', strokeWidth: el.strokeWidth ?? 2, fill: el.fill ?? null }) }
      if (obj) { obj.id = el.id; obj.selectable = !readonly; obj.evented = !readonly; boardCanvas.add(obj) }
    } catch (e) { console.warn('恢复元素失败', e) }
  })
  boardCanvas.renderAll()
  
  if (!readonly) {
    // 初始化历史记录
    boardHistory.value = []
    boardHistoryIndex.value = -1
    saveBoardHistory()
    setTool('select')
  }
}

function syncForegroundColor(e) { const obj = e.selected?.[0]; if (obj && obj.type === 'i-text') foregroundColor.value = obj.fill || '#000000' }

watch(foregroundColor, (newColor) => {
  if (!boardCanvas) return
  updateBrush()
  const active = boardCanvas.getActiveObject()
  if (active && active.type === 'i-text') { active.set('fill', newColor); boardCanvas.renderAll() }
})

function getCanvasPointer(opt) {
  if (!boardCanvas) return null
  if (typeof boardCanvas.getScenePoint === 'function') return boardCanvas.getScenePoint(opt.e)
  else if (typeof boardCanvas.getPointer === 'function') return boardCanvas.getPointer(opt.e)
  else { const rect = boardCanvasRef.value?.getBoundingClientRect(); return { x: (opt.e.clientX - rect.left) * (boardCanvas.width / rect.width), y: (opt.e.clientY - rect.top) * (boardCanvas.height / rect.height) } }
}

function onKeyDown(e) {
  if (!isEditing.value || noteType.value !== 'board') return
  const target = e.target
  const isInputFocused = target.tagName === 'INPUT' || target.tagName === 'TEXTAREA' || target.isContentEditable
  if (isInputFocused) return

  // Delete / Backspace 删除选中
  if (e.key === 'Delete' || e.key === 'Backspace') {
    e.preventDefault()
    deleteSelected()
  }
  
  // Ctrl + Z 撤销
  if ((e.ctrlKey || e.metaKey) && e.key.toLowerCase() === 'z') {
    e.preventDefault()
    if (e.shiftKey) redoBoard() // Ctrl + Shift + Z
    else undoBoard()
  }
  
  // Ctrl + Y 重做
  if ((e.ctrlKey || e.metaKey) && e.key.toLowerCase() === 'y') {
    e.preventDefault()
    redoBoard()
  }
}

function onCanvasMouseDown(opt) {
  const pointer = getCanvasPointer(opt)
  if (!pointer) return
  if (opt.target) { if (currentTool.value !== 'select') setTool('select'); return }
  if (currentTool.value === 'select' || currentTool.value === 'pen') return
  if (currentTool.value === 'line') { startPoint = pointer; return }
  createShape(currentTool.value, pointer)
}

function onCanvasMouseUp(opt) { const pointer = getCanvasPointer(opt); if (!pointer) return; if (currentTool.value === 'line' && startPoint) { const line = new fabric.Line([startPoint.x, startPoint.y, pointer.x, pointer.y], { stroke: foregroundColor.value, strokeWidth: penWidth.value, id: `line_${Date.now()}` }); boardCanvas.add(line); boardCanvas.renderAll(); startPoint = null } }

function createShape(tool, pointer) {
  let shape = null
  const id = `${tool}_${Date.now()}`
  switch (tool) {
    case 'rect': shape = new fabric.Rect({ left: pointer.x - 40, top: pointer.y - 30, width: 80, height: 60, fill: '#cccccc', stroke: foregroundColor.value, strokeWidth: penWidth.value, id }); break
    case 'circle': shape = new fabric.Circle({ left: pointer.x - 40, top: pointer.y - 40, radius: 40, fill: '#cccccc', stroke: foregroundColor.value, strokeWidth: penWidth.value, id }); break
    case 'text': shape = new fabric.IText('文本', { left: pointer.x - 20, top: pointer.y - 10, fontSize: 20, fill: foregroundColor.value, fontFamily: 'Arial', id }); break
  }
  if (shape) { boardCanvas.add(shape); boardCanvas.renderAll() }
}

function setTool(tool) { currentTool.value = tool; if (!boardCanvas) return; if (tool === 'pen') { boardCanvas.isDrawingMode = true; boardCanvas.freeDrawingBrush = new fabric.PencilBrush(boardCanvas); updateBrush() } else { boardCanvas.isDrawingMode = false } }
function updateBrush() { if (boardCanvas?.freeDrawingBrush) { boardCanvas.freeDrawingBrush.color = foregroundColor.value; boardCanvas.freeDrawingBrush.width = penWidth.value } }
function deleteSelected() { if (!boardCanvas) return; const active = boardCanvas.getActiveObject(); if (!active) return; if (active.type === 'activeselection') { active.getObjects().forEach(obj => boardCanvas.remove(obj)); boardCanvas.discardActiveObject() } else { boardCanvas.remove(active); boardCanvas.discardActiveObject() }; boardCanvas.renderAll() }
function clearBoard() { if (!boardCanvas) return; boardCanvas.clear(); boardCanvas.backgroundColor = boardBgColor.value || '#ffffff'; boardCanvas.renderAll() }
watch(boardBgColor, (newColor) => { if (boardCanvas) { boardCanvas.backgroundColor = newColor || '#ffffff'; boardCanvas.renderAll() } })
function disposeBoardCanvas() { if (boardCanvas) { boardCanvas.dispose(); boardCanvas = null }; window.removeEventListener('keydown', onKeyDown) }

async function loadNote() {
  const versionId = route.query.versionId
  if (versionId) {
    try {
      const res = await getVersionDetail(noteId.value, versionId)
      noteTitle.value = res.title; noteType.value = res.type
      if (res.type === 'doc') {
        originalContent.value = normalizeDocContent(res.content || '')
        docContent.value = markdownToTiptapHtml(res.content || '')
      } else if (res.type === 'table') {
        let cols = res.columns || [], rows = res.rows || []
        if (!cols.length && res.content) { try { const parsed = JSON.parse(res.content); cols = parsed.columns || []; rows = parsed.rows || [] } catch {} }
        spreadsheetData.value = { columns: cols, rows: rows }
        tableDataFlat.value = rows.map(row => cols.map(col => (row.cells && row.cells[col.key]) || ''))
        if (!tableDataFlat.value.length) tableDataFlat.value = [Array(cols.length || 3).fill('')]
      } else if (res.type === 'board') {
        let elements = [], background = '#ffffff'
        if (res.elements) { elements = res.elements; background = res.background || '#ffffff' }
        else if (res.content) { try { const parsed = JSON.parse(res.content); elements = parsed.elements || []; background = parsed.background || '#ffffff' } catch {} }
        boardElements.value = elements; boardBgColor.value = background; boardBackground.value = background; await nextTick(); initBoardCanvas(true)
      } else originalContent.value = res.content || ''
      isEditing.value = false; return
    } catch (e) { ElMessage.error('加载历史版本失败'); return }
  }

  if (isNew.value) {
    noteTitle.value = '无标题'; originalTitle.value = ''; originalContent.value = ''; docContent.value = ''
    pendingEmbeds.value = []  // 清空暂存组件
    if (noteType.value === 'table') { spreadsheetData.value = { columns: [{ key: 'col_0', title: 'A', type: 'text' }, { key: 'col_1', title: 'B', type: 'text' }, { key: 'col_2', title: 'C', type: 'text' }], rows: [] }; tableDataFlat.value = [['', '', '']]; spreadsheetConfig.value = buildConfig(spreadsheetData.value.columns, false) }
    else if (noteType.value === 'board') { boardElements.value = []; boardBackground.value = '#ffffff'; boardBgColor.value = '#ffffff' }
    isEditing.value = false; await nextTick()
    if (noteType.value === 'doc') enterEditMode()
    else if (noteType.value === 'table') isEditing.value = true
    else if (noteType.value === 'board') { isEditing.value = true; initBoardCanvas() }
    return
  }

  try {
    const res = await getNoteDetail(noteId.value)

    // 审核状态检查：未通过的笔记只有作者本人能查看
    const userInfo = getUserInfo()
    const isAuthor = userInfo && res.creatorId && userInfo.id === res.creatorId
    if (res.auditStatus !== 1 && !isAuthor) {
      ElMessage.error('该笔记尚未通过审核，暂无法查看')
      router.replace('/dashboard')
      return
    }

    noteTitle.value = res.title; originalTitle.value = res.title; noteType.value = res.type
    if (res.type === 'doc') {
      originalContent.value = normalizeDocContent(res.content || '')
      docContent.value = markdownToTiptapHtml(res.content || '')
    } else if (res.type === 'table') {
      let cols = res.columns || [], rows = res.rows || []
      if (!cols.length && res.content) { try { const parsed = JSON.parse(res.content); cols = parsed.columns || []; rows = parsed.rows || [] } catch {} }
      spreadsheetData.value = { columns: cols, rows: rows }
      tableDataFlat.value = rows.map(row => cols.map(col => (row.cells && row.cells[col.key]) || ''))
      if (!tableDataFlat.value.length) tableDataFlat.value = [Array(cols.length || 3).fill('')]
      spreadsheetConfig.value = buildConfig(cols.length ? cols : [{ title: 'A' }, { title: 'B' }, { title: 'C' }], true); await nextTick()
    } else if (res.type === 'board') {
      let elements = [], background = '#ffffff'
      if (res.elements) { elements = res.elements; background = res.background || '#ffffff' }
      else if (res.content) { try { const parsed = JSON.parse(res.content); elements = parsed.elements || []; background = parsed.background || '#ffffff' } catch {} }
      boardElements.value = elements; boardBgColor.value = background; boardBackground.value = background; await nextTick(); initBoardCanvas(true)
    } else originalContent.value = res.content || ''

    if (!isNew.value) initLikeFavFromStorage(res.hasLiked, res.hasFavorited)
    
    // 如果 URL 中携带 mode=edit 参数，则自动进入编辑模式
    if (route.query.mode === 'edit') {
      enterEditMode()
    } else {
      isEditing.value = false
    }

    if (!isNew.value) {
      loadComments()
      if (noteType.value === 'doc') {
        loadEmbedList()
        await nextTick()
        loadEmbedContent()  // 加载嵌套组件内容
      }
    }

    if (!isNew.value && !route.query.versionId) recordView(noteId.value).catch(() => {})
  } catch (e) { ElMessage.error('加载笔记失败'); router.replace('/dashboard') }
}

// loadEmbedList 只获取嵌套组件列表，不阻塞内容加载
async function loadEmbedList() {
  if (!noteId.value || isNew.value) return // 新建文档未保存，跳过加载嵌套组件列表
  embedLoading.value = true
  try {
    const res = await getDocEmbeds(noteId.value, false)
    embedList.value = res?.data ?? res?.records ?? res ?? []
  } catch (e) {
    console.error('加载嵌套组件列表失败', e)
    embedList.value = []
  } finally {
    embedLoading.value = false
  }
}

async function insertEmbedToEditor() {
  if (!embedForm.embedType) { ElMessage.warning('请选择组件类型'); return }
  
  embedLoading.value = true
  isInsertingEmbed.value = true
  
  const resultType = embedForm.embedType
  
  // 构建嵌套组件创建参数
  const payload = { embedType: resultType, position: 'cursor' }
  if (resultType === 'table') {
    const cols = embedForm.tableData.columns; const rows = []
    for (let i = 0; i < embedForm.tableData.rowCount; i++) { const cells = {}; cols.forEach(col => { cells[col.key] = '' }); rows.push({ id: `row_${i + 1}`, cells }) }
    payload.newTable = { title: embedForm.title || '新建表格', columns: cols, rows }
  } else if (resultType === 'board') {
    payload.newBoard = { title: embedForm.title || '新建画板', elements: [], background: embedForm.boardData.background }
  } else if (resultType === 'mind') {
    payload.newMind = { title: embedForm.title || '新建思维导图', mindData: [{ id: 'node_1', title: embedForm.mindData.centerTopic || '中心主题', children: [] }] }
  }
  
  // 新建文档：暂存嵌套组件，待保存时一并创建
  if (isNew.value) {
    const tempId = `temp_${pendingEmbeds.value.length}`
    pendingEmbeds.value.push({ type: resultType, tempId, payload, title: embedForm.title || '新建组件' })
    
    // 构建临时预览数据
    const previewData = { id: tempId, title: embedForm.title || '新建组件', type: resultType }
    if (resultType === 'table') {
      previewData.columns = embedForm.tableData.columns
      previewData.rows = Array.from({ length: embedForm.tableData.rowCount }, (_, i) => {
        const cells = {}; embedForm.tableData.columns.forEach(col => { cells[col.key] = '' }); return { id: `row_${i + 1}`, cells }
      })
    } else if (resultType === 'board') {
      previewData.elements = []; previewData.background = embedForm.boardData.background || '#ffffff'
    } else if (resultType === 'mind') {
      previewData.mindData = [{ id: 'node_1', title: embedForm.mindData.centerTopic || '中心主题', children: [] }]
    }
    
    embedCache[`${resultType}_${tempId}`] = {
      type: resultType,
      noteId: tempId,
      title: embedForm.title || '新建组件',
      data: previewData
    }
    
    const mark = buildEmbedMark(resultType, tempId)
    const base = normalizeDocContent(originalContent.value || '')
    const content = base.includes(mark) ? base : (base ? `${base}\n${mark}` : mark)
    originalContent.value = content
    if (editorRef.value?.editor) {
      insertEmbedSlotAtCursor(editorRef.value.editor, resultType, tempId, embedForm.title || '新建组件')
    }
    await loadEmbedContent()
    ElMessage.success('组件已添加，保存笔记时一并创建')
    showEmbedDialog.value = false
    resetEmbedForm()
    await loadEmbedList()
    embedLoading.value = false
    isInsertingEmbed.value = false
    return
  }
  
  // 已有文档：直接调用API创建嵌套组件
  try {
    const res = await createDocEmbed(noteId.value, payload)
    const resultNoteId = res?.note?.id ?? res?.noteId
    if (res?.note && resultNoteId) {
      embedCache[`${resultType}_${resultNoteId}`] = {
        type: resultType,
        noteId: resultNoteId,
        title: res.note.title || embedForm.title,
        data: res.note
      }
    }
    const mark = buildEmbedMark(resultType, resultNoteId)
    const base = normalizeDocContent(originalContent.value || '')
    const content = base.includes(mark) ? base : (base ? `${base}\n${mark}` : mark)
    originalContent.value = content
    if (editorRef.value?.editor) {
      insertEmbedSlotAtCursor(editorRef.value.editor, resultType, resultNoteId, res.note.title || embedForm.title)
    }
    await loadEmbedContent()
    ElMessage.success('新组件已插入')
    showEmbedDialog.value = false; resetEmbedForm(); await loadEmbedList()
  } catch (e) { console.error('插入嵌套组件失败', e); ElMessage.error('插入失败') } finally { embedLoading.value = false; isInsertingEmbed.value = false }
}

async function insertEmbed() {
  if (!embedForm.embedType || !embedForm.noteId) { ElMessage.warning('请先选择嵌入类型并填写笔记ID'); return }
  embedLoading.value = true
  try { await createDocEmbed(noteId.value, { embedType: embedForm.embedType, noteId: embedForm.noteId, position: embedForm.position || 'end' }); ElMessage.success('嵌套组件已插入'); embedForm.noteId = null; showEmbedForm.value = false; await loadEmbedList() }
  catch (e) { console.error('插入嵌套组件失败', e); ElMessage.error('插入失败') } finally { embedLoading.value = false }
}

async function removeEmbed(embedId) {
  if (!embedId) return; removingEmbedId.value = embedId
  try { await deleteDocEmbed(noteId.value, embedId); ElMessage.success('嵌套组件已移除'); await loadEmbedList() }
  catch (e) { console.error('删除嵌套组件失败', e); ElMessage.error('删除失败') } finally { removingEmbedId.value = null }
}

function getEmbedTypeLabel(type) { const map = { table: '表格', board: '画板', mind: '思维导图' }; return map[type] || type }
function getEmbedTypeTagType(type) { const map = { table: '', board: 'warning', mind: 'success' }; return map[type] || 'info' }

// 添加编辑器就绪回调
const onEditorReady = (editor) => {
  console.log('Tiptap 编辑器已就绪')
}

// 轮询等待编辑器就绪
async function waitForEditorReady() {
  const maxAttempts = 100 // 最多尝试 100 次
  const delay = 50 // 每次等待 50ms
  
  for (let i = 0; i < maxAttempts; i++) {
    if (editorRef.value) {
      // 检查方式1：通过 isReady 方法检查
      if (typeof editorRef.value.isReady === 'function' && editorRef.value.isReady()) {
        const editorInstance = editorRef.value.editor
        if (editorInstance && typeof editorInstance.commands?.setContent === 'function') {
          return editorInstance
        }
      }
      
      // 检查方式2：直接检查 editor 实例 (Vue 3 会自动拆包 ref)
      if (editorRef.value.editor && typeof editorRef.value.editor.commands?.setContent === 'function') {
        return editorRef.value.editor
      }
    }
    
    await new Promise(resolve => setTimeout(resolve, delay))
  }
  
  throw new Error('Tiptap 编辑器初始化超时')
}

async function enterEditMode() {
  if (noteType.value === 'doc') {
    const content = normalizeDocContent(originalContent.value || '')
    originalContent.value = content
    
    let htmlContent = ''
    try {
      htmlContent = marked(content)
    } catch (e) {
      htmlContent = content.replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/\n/g, '<br>')
    }
    
    docContent.value = embedMarksToHtml(htmlContent)
    
    // 设置 isEditing，触发组件挂载
    isEditing.value = true
    
    // 等待 DOM 更新，让 TiptapEditor 组件挂载
    await nextTick()
    
    console.log('开始等待编辑器就绪...')
    
    // 使用轮询方式等待编辑器就绪
    const editor = await waitForEditorReady()
    
    try {
      // 设置编辑器内容
      if (editor && typeof editor.commands.setContent === 'function') {
        console.log('设置编辑器内容:', docContent.value.length, '字符')
        editor.commands.setContent(docContent.value, false)
        
        /* 
        // 使用 Tiptap 原生 NodeView，不再需要手动 hydrate
        setTimeout(() => {
          if (editorRef.value?.editor) {
            hydrateEmbeds(editorRef.value.editor)
          }
        }, 100)
        */
      } else {
        console.error('编辑器实例无效或没有 setContent 方法')
        ElMessage.error('编辑器初始化失败，请刷新页面重试')
      }
    } catch (error) {
      console.error('Tiptap 编辑器初始化失败:', error.message)
      ElMessage.error('编辑器初始化失败，请刷新页面重试')
    }
  } else if (noteType.value === 'table') { 
    isEditing.value = true
    await nextTick()
    spreadsheetConfig.value = buildConfig(spreadsheetData.value.columns.length ? spreadsheetData.value.columns : [{ title: 'A' }, { title: 'B' }, { title: 'C' }], false) 
  } else if (noteType.value === 'board') { 
    isEditing.value = true
    await nextTick()
    initBoardCanvas() 
  }
}

function cancelEdit() {
  if (isNew.value) {
    // 如果是新建笔记，点击取消应该回到父笔记、知识库或上一页
    if (noteType.value === 'doc') unmountAllEmbedApps()
    if (noteType.value === 'board') disposeBoardCanvas()
    
    const parentId = route.query.parentDocId
    if (parentId) {
      router.push(`/note/${parentId}?kbId=${kbId.value}`)
    } else if (kbId.value) {
      router.push(`/kb/${kbId.value}`)
    } else {
      router.back()
    }
    return
  }

  if (noteType.value === 'doc') unmountAllEmbedApps()
  if (noteType.value === 'table') spreadsheetConfig.value = buildConfig(spreadsheetData.value.columns || [], true)
  if (noteType.value === 'board') { disposeBoardCanvas(); nextTick(() => initBoardCanvas(true)) }
  isEditing.value = false
}

function onEditorContentChange(content) {
  docContent.value = content
}

function onTitleInput() {
  if (isNew.value) return
  clearTimeout(titleSaveTimer)
  titleSaveTimer = setTimeout(async () => {
    try {
      await updateNote(noteId.value, { title: noteTitle.value }, noteType.value)
      originalTitle.value = noteTitle.value
    } catch (e) { ElMessage.error('标题更新失败') }
  }, 800)
}
/** 从编辑器提取文档内容（保留正文 + 嵌套组件标记） */
function collectDocContent() {
  if (!editorRef.value?.editor) return normalizeDocContent(originalContent.value || '')
  const content = extractDocContentFromEditor(editorRef.value.editor)
  return normalizeDocContent(content)
}

function getCurrentContent() {
  if (noteType.value === 'doc') return collectDocContent()
  return originalContent.value || ''
}


async function saveContent() {
  if (!noteTitle.value.trim()) { ElMessage.warning('标题不能为空'); return }
  saving.value = true
  try {
    const payload = { title: noteTitle.value, type: noteType.value }
    if (noteType.value === 'table') {
      const cols = spreadsheetData.value.columns.length ? spreadsheetData.value.columns : tableDataFlat.value[0] ? tableDataFlat.value[0].map((_, i) => ({ key: `col_${i}`, title: String.fromCharCode(65 + i), type: 'text' })) : []
      const rows = tableDataFlat.value.map((row, rowIdx) => { const cells = {}; cols.forEach((col, colIdx) => { cells[col.key] = row[colIdx] || '' }); return { id: `row_${rowIdx + 1}`, cells } })
      payload.columns = cols; payload.rows = rows
      // 按照 mind 的方式，同步存入 content 字段
      payload.content = JSON.stringify({ columns: cols, rows: rows })
    } else if (noteType.value === 'board') {
      const elements = []
      if (boardCanvas) { 
        boardCanvas.getObjects().forEach(obj => { 
          const el = { id: obj.id || `el_${Date.now()}`, type: obj.type }
          if (obj.type === 'rect') { 
            el.x = obj.left ?? 0; el.y = obj.top ?? 0; el.width = obj.width * (obj.scaleX || 1); el.height = obj.height * (obj.scaleY || 1); el.fill = obj.fill; el.stroke = obj.stroke; el.strokeWidth = obj.strokeWidth; el.content = '' 
          } else if (obj.type === 'circle') { 
            el.x = obj.left ?? 0; el.y = obj.top ?? 0; el.width = (obj.radius || 40) * 2; el.height = (obj.radius || 40) * 2; el.fill = obj.fill; el.stroke = obj.stroke; el.strokeWidth = obj.strokeWidth; el.radius = obj.radius; el.content = '' 
          } else if (obj.type === 'line') { 
            el.startX = obj.x1 ?? 0; el.startY = obj.y1 ?? 0; el.endX = obj.x2 ?? 0; el.endY = obj.y2 ?? 0; el.stroke = obj.stroke; el.strokeWidth = obj.strokeWidth; el.content = '' 
          } else if (obj.type === 'i-text') { 
            el.x = obj.left ?? 0; el.y = obj.top ?? 0; el.width = obj.width * (obj.scaleX || 1); el.height = obj.height * (obj.scaleY || 1); el.text = obj.text; el.fontSize = obj.fontSize; el.fill = obj.fill; el.fontFamily = obj.fontFamily; el.content = obj.text || '' 
          } else if (obj.type === 'path') { 
            el.x = obj.left ?? 0; el.y = obj.top ?? 0; el.width = obj.width * (obj.scaleX || 1); el.height = obj.height * (obj.scaleY || 1); el.stroke = obj.stroke; el.strokeWidth = obj.strokeWidth; el.fill = obj.fill; el.content = obj.path ? JSON.stringify(obj.path) : '' 
          }
          if (el.type !== 'line') { delete el.startX; delete el.startY; delete el.endX; delete el.endY }
          if (el.type !== 'rect' && el.type !== 'circle' && el.type !== 'i-text') { delete el.width; delete el.height }
          elements.push(el) 
        }) 
      } else {
        // 如果 boardCanvas 不存在（可能是在非编辑状态下点击保存），则使用现有数据
        elements.push(...boardElements.value)
      }
      payload.elements = elements
      payload.background = boardCanvas ? boardCanvas.backgroundColor : (boardBgColor.value || '#ffffff')
      // 按照 mind 的方式，同步存入 content 字段
      payload.content = JSON.stringify({ elements, background: payload.background })
    } else if (noteType.value === 'doc') {
      payload.content = collectDocContent()
    }
    if (isNew.value) {
      payload.kbId = kbId.value
      payload.parentDocId = route.query.parentDocId || null
      const created = await createNote(payload, noteType.value)
      
      // 处理暂存的嵌套组件：用真实ID替换临时ID，并同步用户编辑数据
      if (pendingEmbeds.value.length > 0 && noteType.value === 'doc') {
        let needUpdate = false
        for (const pending of pendingEmbeds.value) {
          try {
            const embedRes = await createDocEmbed(created.id, pending.payload)
            const realNoteId = embedRes?.note?.id ?? embedRes?.noteId
            if (realNoteId) {
              // 收集用户在编辑器中输入的实际数据并保存到嵌套文档
              const embedAppEntry = embedApps.get(pending.tempId)
              if (embedAppEntry?.vm?.getPayload) {
                try {
                  const userPayload = embedAppEntry.vm.getPayload()
                  await updateNote(realNoteId, userPayload, pending.type)
                } catch (e) { console.error('保存嵌套组件用户数据失败', e) }
              }
              
              const tempMark = buildEmbedMark(pending.type, pending.tempId)
              const realMark = buildEmbedMark(pending.type, realNoteId)
              payload.content = payload.content.replace(tempMark, realMark)
              delete embedCache[`${pending.type}_${pending.tempId}`]
              embedCache[`${pending.type}_${realNoteId}`] = {
                type: pending.type,
                noteId: realNoteId,
                title: embedRes.note.title || pending.title,
                data: embedRes.note
              }
              needUpdate = true
            }
          } catch (e) { console.error('创建嵌套组件失败', e) }
        }
        pendingEmbeds.value = []
        if (needUpdate) {
          await updateNote(created.id, payload, noteType.value)
        }
      }
      
      ElMessage.success('笔记创建成功')
      router.replace(`/note/${created.id}?kbId=${kbId.value}&type=${noteType.value}`)
      originalTitle.value = noteTitle.value
      if (noteType.value === 'doc') originalContent.value = payload.content
      unmountAllEmbedApps()
      if (boardCanvas) disposeBoardCanvas()
      isEditing.value = false
    }
    else {
      if (noteType.value === 'doc') {
        await saveAllEmbedsSilent()
        payload.content = collectDocContent()
      }
      await updateNote(noteId.value, payload, noteType.value)
      ElMessage.success('保存成功')
      originalTitle.value = noteTitle.value
      if (noteType.value === 'doc') {
        originalContent.value = payload.content
        await loadEmbedContent()
        if (editorRef.value?.editor) {
          // reloadEditorContent(editorRef.value.editor, payload.content)
          // await nextTick()
          // setTimeout(() => hydrateEmbeds(editorRef.value.editor), 200)
          editorRef.value.editor.commands.setContent(embedMarksToHtml(payload.content), false)
        }
      }
      if (noteType.value === 'table') {
        spreadsheetData.value.columns = payload.columns
        spreadsheetData.value.rows = payload.rows
      }
      if (noteType.value === 'board') {
        boardElements.value = payload.elements
        boardBgColor.value = payload.background
        boardBackground.value = payload.background
      }
      if (noteType.value !== 'doc') {
        unmountAllEmbedApps()
        isEditing.value = false
      } else {
        // 富文本文档保存后也退出编辑模式
        isEditing.value = false
      }
      if (boardCanvas) disposeBoardCanvas()
      
      // 关键修复：画板保存后，如果是独立笔记模式，需要重新初始化为只读模式
      if (noteType.value === 'board') {
        nextTick(() => {
          initBoardCanvas(true)
        })
      }
    }
  } catch (e) { ElMessage.error(e.message || '保存失败') } finally { saving.value = false }
}

const shareDialogVisible = ref(false)
const shareResultVisible = ref(false)
const shareLoading = ref(false)
const shareUrl = ref('')
const sharePassword = ref('')
const shareForm = ref({ password: '', shareDuration: 'PERMANENT', permission: 'READ' })

function openShareDialog() { if (isNew.value) return; shareForm.value = { password: '', shareDuration: 'PERMANENT', permission: 'READ' }; shareDialogVisible.value = true }
async function createShare() { shareLoading.value = true; try { const res = await shareNote(noteId.value, { password: shareForm.value.password || null, shareDuration: shareForm.value.shareDuration, permission: shareForm.value.permission }); const shareKey = res.shareUrl.split('/').pop(); shareUrl.value = `${window.location.origin}/share/${shareKey}`; sharePassword.value = shareForm.value.password || ''; shareDialogVisible.value = false; shareResultVisible.value = true } catch (e) { ElMessage.error('创建分享失败') } finally { shareLoading.value = false } }
function copyShareUrl() { navigator.clipboard.writeText(shareUrl.value); ElMessage.success('已复制到剪贴板') }
function getContentType(format) { const types = { word: 'application/vnd.openxmlformats-officedocument.wordprocessingml.document', markdown: 'text/markdown', pdf: 'application/pdf', lake: 'application/octet-stream', jpg: 'image/jpeg', xlsx: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' }; return types[format] || 'application/octet-stream' }
function getExtension(format) { const exts = { word: 'docx', markdown: 'md', pdf: 'pdf', lake: 'lake', jpg: 'jpg', xlsx: 'xlsx' }; return exts[format] || format }

// embedCache 变化时 renderedContent 会自动重新计算（因为它依赖 embedCache），无需额外手动刷新

watch(
  [() => route.params.docId, () => route.query.kbId, () => route.query.type],
  ([newDocId, newKbId, newType], [oldDocId, oldKbId, oldType]) => {
    // 防止在插入组件过程中因URL变化触发重复加载
    if (isInsertingEmbed.value) return
    if (newDocId === oldDocId && newKbId === oldKbId && newType === oldType) return
    unmountAllEmbedApps()
    if (boardCanvas) disposeBoardCanvas()
    Object.keys(embedCache).forEach(key => delete embedCache[key])
    loadNote()
  }
)

onMounted(loadNote)
onBeforeUnmount(() => { unmountAllEmbedApps(); if (boardCanvas) disposeBoardCanvas() })
</script>

<style scoped>
.note-editor-page { display: flex; flex-direction: column; height: 100vh; overflow: hidden; }
.editor-toolbar { display: flex; justify-content: space-between; align-items: center; padding: 12px 32px; border-bottom: 1px solid #e5e6eb; background: #fff; flex-shrink: 0; }
.title-section { display: flex; align-items: center; gap: 12px; flex: 1; margin-right: 24px; }
.title-input { border: none; outline: none; font-size: 24px; font-weight: 600; width: 100%; color: #1f2937; background: transparent; }
.title-input::placeholder { color: #d0d5dd; }
.note-type-tag { background: #f0f2f5; padding: 2px 10px; border-radius: 4px; font-size: 12px; color: #666; white-space: nowrap; }
.editor-body { flex: 1; display: flex; flex-direction: column; overflow: hidden; }
.editor-main-layout { flex: 1; display: flex; overflow: hidden; }
.editor-scroll-container { flex: 1; overflow-y: auto; }

.content-preview { max-width: 900px; margin: 0 auto; padding: 32px 48px; }
.tiptap-wrap { padding: 16px 48px; margin-top: 16px; }
:deep(.ProseMirror .tiptap-embed-slot) {
  margin: 12px 0;
  user-select: none;
}

.table-editor-container { min-height: 100%; display: flex; flex-direction: column; }
.table-toolbar { display: flex; gap: 8px; padding: 8px 16px; background: #f5f6f8; border-bottom: 1px solid #e5e6eb; }
.spreadsheet-preview { padding: 16px; }
.mini-table { width: 100%; border-collapse: collapse; font-size: 14px; }
.mini-table th, .mini-table td { border: 1px solid #e5e6eb; padding: 8px 12px; text-align: left; }
.mini-table th { background: #f5f6f8; font-weight: 600; }

/* 语雀风格表格样式 */
:deep(.table-editor-container .jexcel),
:deep(.table-editor-container .jspreadsheet) {
  width: 100% !important;
  border: none !important;
}

:deep(.table-editor-container .jexcel table),
:deep(.table-editor-container .jspreadsheet table) {
  width: 100% !important;
  table-layout: fixed;
  border-collapse: collapse;
}

:deep(.table-editor-container .jexcel td),
:deep(.table-editor-container .jspreadsheet td) {
  min-width: 100px;
  height: 36px;
  min-height: 36px;
  max-height: 36px;
  border: 1px solid #e8e8e8 !important;
  padding: 8px 12px !important;
  font-size: 14px;
  line-height: 1.6;
  color: #262626;
  background: #fff !important;
  text-align: center;
}

:deep(.table-editor-container .jexcel td:hover),
:deep(.table-editor-container .jspreadsheet td:hover) {
  background: #f5f5f5 !important;
}

:deep(.table-editor-container .jexcel td.selected),
:deep(.table-editor-container .jspreadsheet td.selected) {
  background: #e6f7ff !important;
  border-color: #1890ff !important;
}

/* 编辑器输入框居中对齐 */
:deep(.table-editor-container .jss_worksheet .editor),
:deep(.table-editor-container .jss_worksheet .editor > input) {
  text-align: center !important;
}

:deep(.table-editor-container .jexcel table th),
:deep(.table-editor-container .jspreadsheet table th) {
  background: #fafafa !important;
  border: 1px solid #e8e8e8 !important;
  padding: 8px 12px !important;
  font-size: 14px;
  font-weight: 500;
  color: #262626;
  text-align: left;
}

/* 隐藏行号列 */
:deep(.table-editor-container .jexcel colgroup col:first-child),
:deep(.table-editor-container .jspreadsheet colgroup col:first-child) {
  width: 0 !important;
  min-width: 0 !important;
}

:deep(.table-editor-container .jexcel .jexcel_row_header),
:deep(.table-editor-container .jexcel_headers .jexcel_headers_row_header),
:deep(.table-editor-container .jspreadsheet .jspreadsheet_row_header),
:deep(.table-editor-container .jspreadsheet_headers .jspreadsheet_headers_row_header) {
  display: none !important;
  width: 0 !important;
}

/* 隐藏表格第一列（行号列） */
:deep(.table-editor-container .jexcel table tbody tr td:first-child),
:deep(.table-editor-container .jexcel table thead tr th:first-child),
:deep(.table-editor-container .jspreadsheet table tbody tr td:first-child),
:deep(.table-editor-container .jspreadsheet table thead tr th:first-child) {
  display: none !important;
  width: 0 !important;
  padding: 0 !important;
  border: none !important;
}
.board-editor-container { display: flex; flex-direction: column; min-height: 100%; }
.board-toolbar { display: flex; gap: 8px; padding: 8px 16px; background: #f5f6f8; border-bottom: 1px solid #e5e6eb; }
.board-canvas-wrapper { flex: 1; overflow: auto; background: #f0f0f0; display: flex; justify-content: center; padding: 20px; }
.board-canvas-wrapper canvas { border: 1px solid #ccc; box-shadow: 0 2px 8px rgba(0,0,0,0.1); outline: none; }
.board-canvas-wrapper canvas:focus { border-color: #409eff; box-shadow: 0 0 0 2px rgba(64, 158, 255, 0.2); }
.color-group { display: flex; align-items: center; gap: 4px; }
.color-label { font-size: 12px; color: #666; }
.comment-section { padding: 32px 48px; max-width: 900px; margin: 0 auto; border-top: 1px solid #e5e6eb; margin-top: 24px; }
.outline-sidebar { width: 260px; flex-shrink: 0; border-left: 1px solid #e5e6eb; background: #fff; display: flex; flex-direction: column; overflow: hidden; }
.outline-sidebar-header { display: flex; justify-content: space-between; align-items: center; padding: 12px 16px; background: #f7f9fa; border-bottom: 1px solid #e8e8e8; }
.outline-sidebar-title { font-size: 14px; font-weight: 600; color: #1f2937; }
.outline-sidebar-content { flex: 1; overflow-y: auto; padding: 8px 0; }
.outline-sidebar-item { padding: 8px 16px; cursor: pointer; color: #606266; font-size: 14px; line-height: 1.5; }
.outline-sidebar-item:hover { background: #f5f7fa; color: #409eff; }
.outline-level-1 { padding-left: 16px; font-weight: 600; }
.outline-level-2 { padding-left: 32px; font-weight: 500; }
.outline-level-3 { padding-left: 48px; }
.outline-level-4 { padding-left: 64px; font-size: 13px; }
.outline-level-5 { padding-left: 80px; font-size: 13px; }
.outline-level-6 { padding-left: 96px; font-size: 12px; }
.comment-input { margin-bottom: 20px; }
.submit-comment { margin-top: 8px; }
.comments-list { margin-top: 20px; }
.comment-item { padding: 12px 0; border-bottom: 1px solid #f0f0f0; }
.comment-header { display: flex; align-items: center; gap: 12px; margin-bottom: 6px; }
.comment-user { font-weight: 500; color: #1f2937; }
.comment-time { font-size: 12px; color: #999; }
.comment-content { margin-left: 4px; line-height: 1.6; }
.comment-actions { display: flex; gap: 8px; margin-top: 6px; }
.sub-comments { margin-left: 20px; border-left: 2px solid #e5e6eb; padding-left: 16px; margin-top: 8px; }
.sub-comment-item { padding: 8px 0; }
.sub-comment-content { margin-bottom: 4px; }
.sub-comment-meta { display: flex; align-items: center; gap: 12px; font-size: 12px; color: #999; }
.reply-input { margin-top: 8px; margin-left: 20px; display: flex; gap: 8px; }
.reply-hint { margin: 0 0 4px 0; font-size: 13px; color: #666; }
.reply-hint strong { color: #1f2937; }
.embed-toolbar { max-width: 900px; margin: 0 auto; padding: 12px 48px; display: flex; justify-content: flex-end; }
.embed-section { max-width: 900px; margin: 0 auto; padding: 24px 48px; border-top: 1px solid #e5e6eb; margin-top: 24px; }
.embed-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.embed-header h3 { margin: 0; font-size: 16px; font-weight: 600; color: #1f2937; }
.embed-panel { background: #f9fafb; border-radius: 8px; padding: 16px; border: 1px solid #e5e6eb; }
.embed-form-container { background: #fff; border-radius: 6px; padding: 16px; margin-bottom: 16px; border: 1px solid #e5e6eb; }
.embed-form-actions { display: flex; gap: 8px; justify-content: flex-end; margin-top: 8px; }
.embed-list { margin-top: 0; }
.embed-list-header { margin-bottom: 12px; }
.embed-list-header h4 { margin: 0; font-size: 14px; font-weight: 500; color: #4b5563; }
.embed-columns-editor { display: flex; flex-direction: column; gap: 8px; }
.embed-column-item { display: flex; gap: 8px; align-items: center; }
.embed-column-item .el-button { flex-shrink: 0; }
.embed-placeholder { margin: 16px 0; padding: 16px; background: #f8f9fa; border: 1px dashed #dee2e6; border-radius: 8px; text-align: center; color: #6c757d; font-size: 14px; min-height: 60px; display: flex; align-items: center; justify-content: center; }
.embed-placeholder .embed-icon { font-size: 24px; margin-right: 8px; }
.embed-placeholder .embed-title { font-weight: 500; color: #495057; }
.embed-placeholder .embed-type { display: inline-block; padding: 2px 8px; background: #e9ecef; border-radius: 4px; font-size: 12px; margin-left: 8px; }
.embed-placeholder.error { background: #fff5f5; border-color: #ffc9c9; color: #e03131; }
.embed-placeholder.loading { background: #f8f9fa; border-color: #dee2e6; color: #6c757d; }
</style>