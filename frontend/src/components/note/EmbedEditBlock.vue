<template>
  <div class="embed-edit-block" :class="[`embed-type-${embedType}`, { 'embed-inline': inline }]">
    <div v-if="loading" class="embed-edit-body loading">加载中...</div>
    <div v-else-if="error" class="embed-edit-body error">{{ error }}</div>

    <div v-else class="embed-edit-body">
      <template v-if="embedType === 'table'">
        <div 
          v-if="isEditable"
          class="table-toolbar"
        >
          <el-button size="small" @click="addRow">+ 行</el-button>
          <el-button size="small" @click="addCol">+ 列</el-button>
          <el-button size="small" type="danger" @click="deleteRow">- 行</el-button>
          <el-button size="small" type="danger" @click="deleteCol">- 列</el-button>
        </div>
        <div 
          class="table-wrapper" 
          ref="tableWrapperRef"
          :class="{ 'is-readonly': !isEditable }"
          @keydown.stop
          @input.stop
          @compositionstart.stop
          @compositionend.stop
        >
          <VueJSpreadsheet
            v-if="tableReady"
            :key="tableKey"
            v-model="tableFlat"
            :config="tableConfig"
            :readonly="!isEditable"
            @ready="handleTableReady"
          />
          <p v-else class="empty-hint">暂无数据行</p>
        </div>
      </template>

      <template v-else-if="embedType === 'board'">
        <div 
          v-if="isEditable"
          class="board-toolbar"
        >
          <el-button size="small" :type="tool === 'select' ? 'primary' : ''" @click="setTool('select')">选择</el-button>
          <el-button size="small" :type="tool === 'pen' ? 'primary' : ''" @click="setTool('pen')">画笔</el-button>
          <el-button size="small" :type="tool === 'rect' ? 'primary' : ''" @click="setTool('rect')">矩形</el-button>
          <el-button size="small" :type="tool === 'circle' ? 'primary' : ''" @click="setTool('circle')">圆形</el-button>
          <el-button size="small" :type="tool === 'line' ? 'primary' : ''" @click="setTool('line')">线条</el-button>
          <el-button size="small" :type="tool === 'text' ? 'primary' : ''" @click="setTool('text')">文本</el-button>
          <el-color-picker v-model="strokeColor" size="small" />
          <el-color-picker v-model="bgColor" size="small" show-alpha @active-change="bgColor = $event" />
          <el-button size="small" type="danger" @click="deleteSelected">删除选中</el-button>
        </div>
        <div 
          class="board-canvas-wrap" 
          ref="boardWrapRef"
          :class="{ 'is-readonly': !isEditable }"
          contenteditable="false"
          tabindex="0"
        >
          <canvas ref="canvasRef" class="embed-board-canvas" tabindex="0"></canvas>
        </div>
      </template>

      <template v-else-if="embedType === 'mind'">
        <div class="mind-editor-wrapper" :class="{ 'is-readonly': !isEditable }">
          <MindNodeEditor
            v-for="(node, idx) in mindNodes"
            :key="node.id || idx"
            :node="node"
            :depth="0"
            :readonly="!isEditable"
            @delete="removeMindNode(idx)"
          />
          <el-button v-if="!mindNodes.length && isEditable" size="small" @click="initMindRoot">添加中心主题</el-button>
        </div>
      </template>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import VueJSpreadsheet from 'vue3_jspreadsheet'
import 'vue3_jspreadsheet/dist/vue3_jspreadsheet.css'
import formula from '@jspreadsheet/formula'
import * as fabric from 'fabric'
import { getNoteDetail, updateNote } from '@/api/note'
import MindNodeEditor from './MindNodeEditor.vue'

const props = defineProps({
  embedType: { type: String, required: true },
  noteId: { type: [String, Number], required: true },
  /** 内嵌于 Vditor 编辑区：无标题栏、无单独保存提示 */
  inline: { type: Boolean, default: false },
  /** 是否可编辑，用于区分查看模式和编辑模式 */
  isEditable: { type: Boolean, default: true }
})

const loading = ref(true)
const error = ref('')
const saving = ref(false)
const localTitle = ref('')

const tableColumns = ref([])
const tableFlat = ref([])
const tableConfig = ref({})
const tableKey = ref(0)
let jspreadsheetInstance = null
let tableResizeObserver = null
const tableReady = computed(() => tableColumns.value.length > 0)

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
      let style = document.querySelector('style[data-hide-index-embed]')
      if (!style) {
        style = document.createElement('style')
        style.setAttribute('data-hide-index-embed', 'true')
        style.textContent = `
          .table-wrapper .jss_worksheet > tbody > tr > td:first-child,
          .table-wrapper .jss_worksheet > thead > tr > td:first-child {
            display: none !important;
          }
        `
        document.head.appendChild(style)
      }
    }, 100)
  })
})

function buildTableConfig(cols) {
  const columnTypes = cols.map(col => {
    const typeMap = { text: { type: 'text' }, number: { type: 'numeric', mask: '#,##0', decimal: '.' }, date: { type: 'calendar', options: { format: 'YYYY-MM-DD' } }, dropdown: { type: 'dropdown', source: ['选项1', '选项2'] }, checkbox: { type: 'checkbox' }, color: { type: 'color' }, html: { type: 'html' }, image: { type: 'image' } }
    const mapped = typeMap[col.type] || typeMap.text
    return { ...mapped, title: col.title || col.key, width: col.width || 120, textAlign: 'center' }
  })
  return {
    columns: columnTypes,
    indexColumns: false,
    allowInsertRow: props.isEditable,
    allowInsertColumn: props.isEditable,
    allowDeleteRow: props.isEditable,
    allowDeleteColumn: props.isEditable,
    allowRenameColumn: props.isEditable,
    columnSorting: true,
    columnDrag: props.isEditable,
    columnResize: true,
    rowDrag: props.isEditable,
    rowResize: true,
    tableOverflow: true,
    tableHeight: '600px',
    minSpareRows: 1,
    contextMenu: true,
    selectionCopy: props.isEditable,
    plugins: [formula],
    formula: true,
    onchange: (instance, cell, x, y, value) => {
      tableFlat.value = instance.getData()
    },
    oninsertrow: (instance) => {
      tableFlat.value = instance.getData()
    },
    ondeleterow: (instance) => {
      tableFlat.value = instance.getData()
    },
    oninsertcolumn: (instance) => {
      tableFlat.value = instance.getData()
    },
    ondeletecolumn: (instance) => {
      tableFlat.value = instance.getData()
    },
    onload: (instance) => {
      const table = instance.table
      if (table) {
        table.classList.add('jss_hidden_index')
      }
    }
  }
}

// 监听容器大小变化，重新计算列宽
function adjustColumnWidths() {
  if (!tableWrapperRef.value || !jspreadsheetInstance) return
  const containerWidth = tableWrapperRef.value.clientWidth
  if (containerWidth <= 0) return
  const colCount = tableColumns.value.length
  
  if (colCount === 0) return
  
  const colWidth = Math.max(100, Math.floor(containerWidth / colCount))
  
  tableColumns.value.forEach((_, idx) => {
    jspreadsheetInstance.setWidth(idx, colWidth)
  })
}

function syncTableFromNote(note) {
  let cols = note.columns || []
  let rows = note.rows || []

  // 按照 mind 的方式，如果 columns/rows 为空，尝试从 content 解析
  if ((!cols.length || !rows.length) && note.content) {
    try {
      const parsed = JSON.parse(note.content)
      cols = parsed.columns || cols
      rows = parsed.rows || rows
    } catch (e) {
      console.warn('解析表格 content 失败:', e)
    }
  }

  tableColumns.value = cols.length ? cols : [{ key: 'col_0', title: '列1', type: 'text' }]
  tableFlat.value = rows.length
    ? rows.map((row) => tableColumns.value.map((col) => (row.cells && row.cells[col.key]) || ''))
    : [tableColumns.value.map(() => '')]
  tableConfig.value = buildTableConfig(tableColumns.value)
  tableKey.value++
}

function getTablePayload() {
  // 确保数据是最新的，尝试关闭当前编辑器（如果有）
  if (jspreadsheetInstance) {
    try {
      // JSpreadsheet v4 并没有 closeEditor，但我们可以通过失去焦点或点击外部来确保同步
      // 或者直接调用 getData()，通常 getData() 会包含当前正在编辑的值
      tableFlat.value = jspreadsheetInstance.getData()
    } catch (e) {
      console.warn('获取表格数据失败:', e)
    }
  }

  const cols = tableColumns.value
  const currentData = tableFlat.value
  
  const rows = currentData.map((row, rowIdx) => {
    const cells = {}
    cols.forEach((col, colIdx) => {
      cells[col.key] = row[colIdx] || ''
    })
    return { id: `row_${rowIdx + 1}`, cells }
  })
  
  // 按照 mind 的方式，将完整数据序列化存入 content 字段
  return { 
    title: localTitle.value, 
    columns: cols, 
    rows: rows,
    content: JSON.stringify({ columns: cols, rows: rows })
  }
}

function addRow() {
  tableFlat.value = [...tableFlat.value, tableColumns.value.map(() => '')]
  tableKey.value++
}
function addCol() {
  const idx = tableColumns.value.length
  tableColumns.value = [...tableColumns.value, { key: `col_${Date.now()}`, title: `列${idx + 1}`, type: 'text' }]
  tableFlat.value = tableFlat.value.map((row) => [...row, ''])
  tableConfig.value = buildTableConfig(tableColumns.value)
  tableKey.value++
}
function deleteRow() {
  if (tableFlat.value.length <= 1) return
  tableFlat.value = tableFlat.value.slice(0, -1)
  tableKey.value++
}
function deleteCol() {
  if (tableColumns.value.length <= 1) return
  tableColumns.value = tableColumns.value.slice(0, -1)
  tableFlat.value = tableFlat.value.map((row) => row.slice(0, -1))
  tableConfig.value = buildTableConfig(tableColumns.value)
  tableKey.value++
}

const tool = ref('select')
const strokeColor = ref('#000000')
const bgColor = ref('#ffffff')
const canvasRef = ref(null)
const boardWrapRef = ref(null)
let fabricCanvas = null
let lineStart = null

// 历史记录栈，用于撤销和重做
const history = ref([])
const historyIndex = ref(-1)
const isHandlingHistory = ref(false)

/** 记录当前画布状态到历史记录 */
function saveHistory() {
  if (!fabricCanvas || isHandlingHistory.value) return
  
  // 序列化当前画布元素和背景
  const state = {
    elements: getBoardPayload().elements,
    background: fabricCanvas.backgroundColor
  }
  
  const stateStr = JSON.stringify(state)
  
  // 如果新状态与最后一次记录的状态相同，则不记录
  if (historyIndex.value >= 0 && history.value[historyIndex.value] === stateStr) return

  // 移除当前索引之后的历史（因为产生了新的分支）
  if (historyIndex.value < history.value.length - 1) {
    history.value = history.value.slice(0, historyIndex.value + 1)
  }
  
  history.value.push(stateStr)
  // 限制历史记录数量，防止内存占用过高
  if (history.value.length > 50) {
    history.value.shift()
  } else {
    historyIndex.value++
  }
}

/** 撤销 */
function undo() {
  if (historyIndex.value <= 0) return
  historyIndex.value--
  applyHistoryState(history.value[historyIndex.value])
}

/** 重做 */
function redo() {
  if (historyIndex.value >= history.value.length - 1) return
  historyIndex.value++
  applyHistoryState(history.value[historyIndex.value])
}

/** 应用历史记录中的状态 */
async function applyHistoryState(stateStr) {
  if (!fabricCanvas) return
  isHandlingHistory.value = true
  try {
    const state = JSON.parse(stateStr)
    fabricCanvas.clear()
    fabricCanvas.backgroundColor = state.background || '#ffffff'
    
    // 加载元素
    if (state.elements && state.elements.length) {
      for (const el of state.elements) {
        const obj = createObjectFromData(el)
        if (obj) fabricCanvas.add(obj)
      }
    }
    fabricCanvas.renderAll()
  } finally {
    isHandlingHistory.value = false
  }
}

/** 根据数据创建 Fabric 对象 */
function createObjectFromData(el) {
  let obj = null
  if (el.type === 'rect') {
    obj = new fabric.Rect({ left: el.x ?? 0, top: el.y ?? 0, width: el.width ?? 80, height: el.height ?? 60, fill: el.fill ?? '#ccc', stroke: el.stroke ?? '#333', strokeWidth: el.strokeWidth ?? 1 })
  } else if (el.type === 'circle') {
    obj = new fabric.Circle({ left: el.x ?? 0, top: el.y ?? 0, radius: el.radius ?? 40, fill: el.fill ?? '#ccc', stroke: el.stroke ?? '#333', strokeWidth: el.strokeWidth ?? 1 })
  } else if (el.type === 'line') {
    obj = new fabric.Line([el.startX ?? 0, el.startY ?? 0, el.endX ?? 100, el.endY ?? 0], { stroke: el.stroke ?? '#333', strokeWidth: el.strokeWidth ?? 2 })
  } else if (el.type === 'i-text') {
    obj = new fabric.IText(el.text ?? el.content ?? '文本', { left: el.x ?? 0, top: el.y ?? 0, fontSize: el.fontSize ?? 18, fill: el.fill ?? '#000' })
  } else if (el.type === 'path' && el.content) {
    const pathData = typeof el.content === 'string' ? JSON.parse(el.content) : el.content
    obj = new fabric.Path(pathData, { left: el.x ?? 0, top: el.y ?? 0, stroke: el.stroke ?? '#000', strokeWidth: el.strokeWidth ?? 2, fill: el.fill ?? null })
  }
  if (obj) obj.id = el.id
  return obj
}

function initBoardCanvas(elements, background) {
  if (!canvasRef.value) return
  
  // 销毁旧实例
  if (fabricCanvas) {
    try {
      fabricCanvas.dispose()
    } catch (e) {
      console.error('销毁旧画板失败:', e)
    }
    fabricCanvas = null
  }
  
  // 延迟一帧确保 DOM 布局完成
  setTimeout(() => {
    if (!canvasRef.value) return
    
    const container = boardWrapRef.value
    const width = container?.clientWidth || 700
    const height = 400
    
    try {
      // Fabric v7 核心初始化
      fabricCanvas = new fabric.Canvas(canvasRef.value, {
        backgroundColor: background || '#ffffff',
        width: width - 16,
        height: height,
        selection: props.isEditable,
        preserveObjectStacking: true,
        stopContextMenu: !props.isEditable,
        fireRightClick: props.isEditable,
        enablePointerEvents: true,
        allowTouchScrolling: true
      })
      
      // 非编辑模式下，禁用所有交互
      if (!props.isEditable) {
        fabricCanvas.selection = false
        fabricCanvas.skipTargetFind = true
        fabricCanvas.defaultCursor = 'default'
      }
      
      // 允许点击穿透 Tiptap 的阻止逻辑
      fabricCanvas.allowTouchScrolling = true
      
      // 绑定事件（只在编辑模式下绑定）
      if (props.isEditable) {
        fabricCanvas.on('mouse:down', (opt) => {
          // 如果点击了对象且当前不是选择工具，自动切换到选择工具
          if (opt.target && tool.value !== 'select') {
            setTool('select')
          }
          onBoardMouseDown(opt)
        })
        fabricCanvas.on('mouse:up', onBoardMouseUp)
        
        // 监听对象变化以记录历史
        fabricCanvas.on('object:added', saveHistory)
        fabricCanvas.on('object:modified', saveHistory)
        fabricCanvas.on('object:removed', saveHistory)
        
        // 键盘快捷键监听
        window.addEventListener('keydown', handleKeyDown)
      }
      
      // 监听窗口缩放调整画板大小
      const resizeObserver = new ResizeObserver(() => {
        if (fabricCanvas && container) {
          const newWidth = container.clientWidth - 16
          if (newWidth > 0) {
            fabricCanvas.setDimensions({ width: newWidth, height: 400 })
            fabricCanvas.renderAll()
          }
        }
      })
      if (container) resizeObserver.observe(container)

      // 加载现有元素
      if (elements && elements.length) {
        elements.forEach((el) => {
          const obj = createObjectFromData(el)
          if (obj) fabricCanvas.add(obj)
        })
      }
      
      fabricCanvas.renderAll()
      // 初始化历史记录
      history.value = []
      historyIndex.value = -1
      saveHistory()
      
      // 强制设置初始工具
      setTool(tool.value || 'select')
      
    } catch (err) {
      console.error('Fabric 初始化严重错误:', err)
    }
  }, 100)
}

function handleKeyDown(e) {
  // 如果不是编辑模式或当前焦点不在画板容器内，不处理
  if (!props.isEditable || !boardWrapRef.value?.contains(document.activeElement)) return

  // Delete / Backspace 删除选中
  if (e.key === 'Delete' || e.key === 'Backspace') {
    deleteSelected()
    e.preventDefault()
  }
  
  // Ctrl + Z 撤销
  if ((e.ctrlKey || e.metaKey) && e.key.toLowerCase() === 'z') {
    if (e.shiftKey) redo() // Ctrl + Shift + Z
    else undo()
    e.preventDefault()
  }
  
  // Ctrl + Y 重做
  if ((e.ctrlKey || e.metaKey) && e.key.toLowerCase() === 'y') {
    redo()
    e.preventDefault()
  }
}

function getBoardPointer(opt) {
  if (!fabricCanvas) return null
  // Fabric v6+ 推荐使用 getScenePoint
  try {
    return fabricCanvas.getScenePoint(opt.e)
  } catch (e) {
    // 兼容性回退
    const rect = canvasRef.value.getBoundingClientRect()
    return {
      x: opt.e.clientX - rect.left,
      y: opt.e.clientY - rect.top
    }
  }
}

function onBoardMouseDown(opt) {
  const pointer = getBoardPointer(opt)
  if (!pointer) return
  if (opt.target) return
  if (tool.value === 'line') { lineStart = pointer; return }
  if (tool.value === 'select' || tool.value === 'pen') return
  createBoardShape(tool.value, pointer)
}

function onBoardMouseUp(opt) {
  const pointer = getBoardPointer(opt)
  if (!pointer || tool.value !== 'line' || !lineStart) return
  const line = new fabric.Line([lineStart.x, lineStart.y, pointer.x, pointer.y], {
    stroke: strokeColor.value, strokeWidth: 2, id: `line_${Date.now()}`
  })
  fabricCanvas.add(line)
  fabricCanvas.renderAll()
  lineStart = null
}

function createBoardShape(shapeTool, pointer) {
  let shape = null
  const id = `${shapeTool}_${Date.now()}`
  if (shapeTool === 'rect') {
    shape = new fabric.Rect({ left: pointer.x - 40, top: pointer.y - 30, width: 80, height: 60, fill: '#ccc', stroke: strokeColor.value, strokeWidth: 2, id })
  } else if (shapeTool === 'circle') {
    shape = new fabric.Circle({ left: pointer.x - 40, top: pointer.y - 40, radius: 40, fill: '#ccc', stroke: strokeColor.value, strokeWidth: 2, id })
  } else if (shapeTool === 'text') {
    shape = new fabric.IText('文本', { left: pointer.x - 20, top: pointer.y - 10, fontSize: 18, fill: strokeColor.value, id })
  }
  if (shape) { fabricCanvas.add(shape); fabricCanvas.renderAll() }
}

function setTool(t) {
  tool.value = t
  if (!fabricCanvas) return
  
  // 确保画布获得焦点
  if (canvasRef.value) {
    canvasRef.value.focus()
  }

  if (t === 'pen') {
    fabricCanvas.isDrawingMode = true
    // Fabric v7 实例化 PencilBrush
    fabricCanvas.freeDrawingBrush = new fabric.PencilBrush(fabricCanvas)
    fabricCanvas.freeDrawingBrush.color = strokeColor.value
    fabricCanvas.freeDrawingBrush.width = 2
    fabricCanvas.defaultCursor = 'crosshair'
  } else {
    fabricCanvas.isDrawingMode = false
    fabricCanvas.defaultCursor = t === 'select' ? 'default' : 'crosshair'
  }
  fabricCanvas.renderAll()
}

function deleteSelected() {
  if (!fabricCanvas) return
  const active = fabricCanvas.getActiveObject()
  if (!active) return
  if (active.type === 'activeselection') {
    active.getObjects().forEach((obj) => fabricCanvas.remove(obj))
    fabricCanvas.discardActiveObject()
  } else {
    fabricCanvas.remove(active)
    fabricCanvas.discardActiveObject()
  }
  fabricCanvas.renderAll()
}

watch(strokeColor, (c) => {
  if (fabricCanvas?.freeDrawingBrush) fabricCanvas.freeDrawingBrush.color = c
})
watch(bgColor, (c) => {
  if (fabricCanvas) { fabricCanvas.backgroundColor = c; fabricCanvas.renderAll() }
})

function getBoardPayload() {
  const elements = []
  if (!fabricCanvas) return { title: localTitle.value, elements: [], background: bgColor.value }
  fabricCanvas.getObjects().forEach((obj) => {
    const el = { id: obj.id || `el_${Date.now()}`, type: obj.type }
    if (obj.type === 'rect') {
      Object.assign(el, { x: obj.left ?? 0, y: obj.top ?? 0, width: obj.width * (obj.scaleX || 1), height: obj.height * (obj.scaleY || 1), fill: obj.fill, stroke: obj.stroke, strokeWidth: obj.strokeWidth, content: '' })
    } else if (obj.type === 'circle') {
      Object.assign(el, { x: obj.left ?? 0, y: obj.top ?? 0, radius: obj.radius, fill: obj.fill, stroke: obj.stroke, strokeWidth: obj.strokeWidth, content: '' })
    } else if (obj.type === 'line') {
      Object.assign(el, { startX: obj.x1 ?? 0, startY: obj.y1 ?? 0, endX: obj.x2 ?? 0, endY: obj.y2 ?? 0, stroke: obj.stroke, strokeWidth: obj.strokeWidth, content: '' })
    } else if (obj.type === 'i-text') {
      Object.assign(el, { x: obj.left ?? 0, y: obj.top ?? 0, text: obj.text, fontSize: obj.fontSize, fill: obj.fill, content: obj.text || '' })
    } else if (obj.type === 'path') {
      Object.assign(el, { x: obj.left ?? 0, y: obj.top ?? 0, stroke: obj.stroke, strokeWidth: obj.strokeWidth, content: obj.path ? JSON.stringify(obj.path) : '' })
    }
    elements.push(el)
  })
  
  const background = fabricCanvas.backgroundColor || bgColor.value
  return { 
    title: localTitle.value, 
    elements, 
    background,
    content: JSON.stringify({ elements, background })
  }
}

const mindNodes = ref([])

function initMindRoot() {
  mindNodes.value = [{ id: 'node_1', title: '中心主题', children: [] }]
}

function removeMindNode(idx) {
  mindNodes.value.splice(idx, 1)
}

async function loadNote() {
  loading.value = true
  error.value = ''
  let boardDataToInit = null
  
  // 临时ID（新建文档中暂存的组件），不调用后端API
  if (typeof props.noteId === 'string' && props.noteId.startsWith('temp_')) {
    localTitle.value = '新建组件'
    if (props.embedType === 'table') {
      tableFlat.value = [['', '', '']]
      tableColumns.value = [
        { key: 'col_0', title: '列1', type: 'text', editable: props.isEditable, readOnly: !props.isEditable },
        { key: 'col_1', title: '列2', type: 'text', editable: props.isEditable, readOnly: !props.isEditable },
        { key: 'col_2', title: '列3', type: 'text', editable: props.isEditable, readOnly: !props.isEditable }
      ]
    } else if (props.embedType === 'board') {
      boardDataToInit = { elements: [], background: '#ffffff' }
    } else if (props.embedType === 'mind') {
      mindNodes.value = [{ id: 'node_1', title: '中心主题', children: [] }]
    }
    loading.value = false
    if (boardDataToInit) {
      nextTick(() => initBoardCanvas(boardDataToInit.elements, boardDataToInit.background))
    }
    return
  }
  
  try {
    const note = await getNoteDetail(props.noteId)
    localTitle.value = note.title || ''
    
    if (props.embedType === 'table') {
      syncTableFromNote(note)
    } else if (props.embedType === 'board') {
      let elements = note.elements || []
      let background = note.background || '#ffffff'
      if (!elements.length && note.content) {
        try {
          const parsed = JSON.parse(note.content)
          elements = parsed.elements || []
          background = parsed.background || background
        } catch (_) { /* empty */ }
      }
      bgColor.value = background
      // 记录数据，等 loading 结束后再初始化
      boardDataToInit = { elements, background }
    } else if (props.embedType === 'mind') {
      let nodes = note.mindData || note.nodes || []
      if ((!nodes || nodes.length === 0) && note.content) {
        try {
          const parsed = JSON.parse(note.content)
          nodes = parsed.mindData || parsed.nodes || parsed.elements || []
        } catch (_) {}
      }
      mindNodes.value = JSON.parse(JSON.stringify(nodes))
      if (!mindNodes.value.length) {
        mindNodes.value = [{ id: 'node_1', title: '中心主题', children: [] }]
      }
    }
  } catch (e) {
    error.value = '加载失败'
    console.error(e)
  } finally {
    loading.value = false
    
    // 关键修复：确保 loading 变为 false 且 DOM 更新后再初始化画板
    if (boardDataToInit) {
      nextTick(() => {
        initBoardCanvas(boardDataToInit.elements, boardDataToInit.background)
      })
    }
  }
}

async function saveEmbed(options = {}) {
  const silent = options.silent ?? props.inline
  saving.value = true
  try {
    let payload = {}
    if (props.embedType === 'table') payload = getTablePayload()
    else if (props.embedType === 'board') payload = getBoardPayload()
    else if (props.embedType === 'mind') {
      payload = { 
        title: localTitle.value, 
        mindData: mindNodes.value, // 后端 kb_mind 表对应字段
        nodes: mindNodes.value,    // 兼容性字段
        content: JSON.stringify({ mindData: mindNodes.value }) // 备选存储方案
      }
    }
    await updateNote(props.noteId, payload, props.embedType)
    if (!silent) ElMessage.success('组件已保存')
  } catch (e) {
    if (!silent) ElMessage.error('保存组件失败')
    throw e
  } finally {
    saving.value = false
  }
}

defineExpose({
  saveEmbed,
  getPayload() {
    if (props.embedType === 'table') return getTablePayload()
    if (props.embedType === 'board') return getBoardPayload()
    if (props.embedType === 'mind') return { title: localTitle.value, nodes: mindNodes.value }
    return { title: localTitle.value }
  }
})

const tableWrapperRef = ref(null)

function handleTableReady(instance) {
  jspreadsheetInstance = instance
  
  // 使用 CSS 隐藏行号列
  nextTick(() => {
    setTimeout(() => {
      const table = tableWrapperRef.value?.querySelector('table')
      if (table) {
        table.classList.add('jss_hidden_index')
        
        const style = document.createElement('style')
        style.setAttribute('data-hide-embed-index', 'true')
        style.textContent = `
          .table-wrapper table tr > td:first-child,
          .table-wrapper table tr > th:first-child,
          .table-wrapper table colgroup col:first-child {
            display: none !important;
            width: 0 !important;
            min-width: 0 !important;
          }
        `
        if (!document.querySelector('style[data-hide-embed-index]')) {
          document.head.appendChild(style)
        }
      }
    }, 200)
  })
  
  // 表格组件就绪后，调整列宽以适应容器
  nextTick(() => {
    adjustColumnWidths()
  })
  
  // 表格组件就绪后，确保获得焦点
  if (tableWrapperRef.value) {
    tableWrapperRef.value.click()
    
    setTimeout(() => {
      const spreadsheet = tableWrapperRef.value.querySelector('.jspreadsheet')
      if (spreadsheet) {
        spreadsheet.focus()
      }
    }, 100)
  }
}

// 监听 isEditable 变化，重新初始化组件状态
watch(() => props.isEditable, async () => {
  if (props.embedType === 'board') {
    // 重新初始化画板以切换编辑状态（选择、事件绑定等）
    await loadNote()
  } else if (props.embedType === 'table') {
    // 重新构建表格配置以切换只读状态
    tableConfig.value = buildTableConfig(tableColumns.value)
    tableKey.value++
  }
})

// 监听容器大小变化，重新调整列宽
watch(() => props.noteId, () => {
  if (tableResizeObserver) {
    tableResizeObserver.disconnect()
    tableResizeObserver = null
  }
})

// 在组件挂载后尝试激活表格
onMounted(async () => {
  await loadNote()
  
  // 如果表格已准备好，立即激活
  if (props.embedType === 'table' && tableReady.value) {
    handleTableReady()
  }
  
  // 监听表格容器大小变化
  if (props.embedType === 'table' && tableWrapperRef.value) {
    tableResizeObserver = new ResizeObserver(() => {
      adjustColumnWidths()
    })
    tableResizeObserver.observe(tableWrapperRef.value)
  }
})
onBeforeUnmount(() => { 
  if (fabricCanvas) { 
    fabricCanvas.dispose()
    fabricCanvas = null 
  } 
  window.removeEventListener('keydown', handleKeyDown)
  if (tableResizeObserver) {
    tableResizeObserver.disconnect()
    tableResizeObserver = null
  }
})
watch(() => props.noteId, loadNote)
</script>

<style scoped>
.embed-edit-block {
  margin: 12px 0;
  border-radius: 8px;
  overflow: hidden;
  background: #fff;
}
.embed-inline {
  margin: 8px 0;
}
.embed-edit-body { padding: 12px; }
.embed-edit-body.loading,
.embed-edit-body.error { text-align: center; color: #999; padding: 20px; }
.embed-edit-body.error { color: #e03131; }
.table-toolbar,
.board-toolbar {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-bottom: 8px;
}
.table-wrapper {
  margin-top: 8px;
  border: 1px solid #e8e8e8;
  border-radius: 4px;
  overflow: visible;
  pointer-events: all !important;
  user-select: text !important;
  -webkit-user-select: text !important;
  position: relative;
  z-index: 50;
  width: 100%;
  background: #fff;
}

:deep(.table-wrapper .jexcel),
:deep(.table-wrapper .jspreadsheet) {
  width: 100% !important;
  border: none !important;
}

:deep(.table-wrapper .jexcel table),
:deep(.table-wrapper .jspreadsheet table) {
  width: 100% !important;
  table-layout: fixed;
  border-collapse: collapse;
}

:deep(.table-wrapper .jexcel td),
:deep(.table-wrapper .jspreadsheet td) {
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

:deep(.table-wrapper .jexcel td:hover),
:deep(.table-wrapper .jspreadsheet td:hover) {
  background: #f5f5f5 !important;
}

:deep(.table-wrapper .jexcel td.selected),
:deep(.table-wrapper .jspreadsheet td.selected) {
  background: #e6f7ff !important;
  border-color: #1890ff !important;
}

/* 编辑器输入框居中对齐 */
:deep(.table-wrapper .jss_worksheet .editor),
:deep(.table-wrapper .jss_worksheet .editor > input) {
  text-align: center !important;
}

:deep(.table-wrapper .jexcel table th),
:deep(.table-wrapper .jspreadsheet table th) {
  background: #fafafa !important;
  border: 1px solid #e8e8e8 !important;
  padding: 8px 12px !important;
  font-size: 14px;
  font-weight: 500;
  color: #262626;
  text-align: left;
}

/* 隐藏表格的行号列（兼容 jexcel / jspreadsheet） */
:deep(.table-wrapper .jexcel colgroup col:first-child),
:deep(.table-wrapper .jspreadsheet colgroup col:first-child) {
  width: 0 !important;
  min-width: 0 !important;
}

:deep(.table-wrapper .jexcel .jexcel_row_header),
:deep(.table-wrapper .jexcel_headers .jexcel_headers_row_header),
:deep(.table-wrapper .jspreadsheet .jspreadsheet_row_header),
:deep(.table-wrapper .jspreadsheet_headers .jspreadsheet_headers_row_header) {
  display: none !important;
  width: 0 !important;
}

/* 隐藏表格第一列（行号列） */
:deep(.table-wrapper .jexcel table tbody tr td:first-child),
:deep(.table-wrapper .jexcel table thead tr th:first-child),
:deep(.table-wrapper .jspreadsheet table tbody tr td:first-child),
:deep(.table-wrapper .jspreadsheet table thead tr th:first-child) {
  display: none !important;
  width: 0 !important;
  padding: 0 !important;
  border: none !important;
}

.board-canvas-wrap {
  display: flex;
  justify-content: center;
  background: #f0f2f5;
  padding: 8px;
  border-radius: 6px;
  min-height: 400px;
  position: relative;
  outline: none;
  /* 强制开启指针事件，不再受 is-readonly 类的影响 */
  pointer-events: all !important;
}

.board-canvas-wrap.is-readonly {
  /* 只读模式下依然允许点击，但通过 Fabric 内部控制不可编辑 */
  cursor: default;
}

.table-wrapper.is-readonly,
.board-canvas-wrap.is-readonly,
.mind-editor-wrapper.is-readonly {
  pointer-events: none !important;
  user-select: none !important;
}

.embed-board-canvas {
  border: 1px solid #dcdfe6;
  background-color: #ffffff;
  box-shadow: 0 2px 12px 0 rgba(0,0,0,0.1);
}

.empty-hint { color: #999; text-align: center; padding: 12px; }

/* 确保嵌入组件在Tiptap编辑器中正确显示 */
:deep(.tiptap-embed-slot) {
  display: block;
}

/* Fabric v7 容器和上层画布样式，确保交互正常 */
:deep(.canvas-container) {
  margin: 0 auto;
  pointer-events: all !important;
  user-select: none;
  position: relative;
  z-index: 50;
}

:deep(.upper-canvas) {
  pointer-events: all !important;
}

:deep(.lower-canvas) {
  pointer-events: none;
}
</style>