<template>
  <div class="note-viewer">
    <!-- doc 类型：使用 Tiptap 渲染 -->
    <div v-if="note.type === 'doc'" class="content-body">
      <TiptapEditor 
        :model-value="tiptapContent" 
        :editable="false" 
        :show-toolbar="false"
      />
    </div>

    <!-- table 类型：HTML 表格 -->
    <div v-else-if="note.type === 'table'" class="table-container">
      <table class="data-table" v-if="tableData.columns.length">
        <thead>
          <tr>
            <th v-for="col in tableData.columns" :key="col.key">{{ col.title }}</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="(row, rowIdx) in tableData.rows" :key="rowIdx">
            <td v-for="col in tableData.columns" :key="col.key">
              {{ (row.cells && row.cells[col.key]) || '' }}
            </td>
          </tr>
        </tbody>
      </table>
      <el-empty v-else description="表格无数据" />
    </div>

    <!-- board 类型：SVG 画板 -->
    <div v-else-if="note.type === 'board'" class="board-container">
      <div v-html="boardSvg"></div>
    </div>

    <!-- mind 类型：思维导图 -->
    <div v-else-if="note.type === 'mind'" class="mind-container">
      <ul class="mind-tree" v-if="mindNodes.length">
        <li v-for="node in mindNodes" :key="node.id" class="mind-node-item">
          <span class="mind-node-content">{{ node.title }}</span>
          <ul v-if="node.children?.length" class="mind-children">
            <li v-for="child in node.children" :key="child.id" class="mind-node-item">
              <span class="mind-node-content">{{ child.title }}</span>
            </li>
          </ul>
        </li>
      </ul>
      <el-empty v-else description="暂无内容" />
    </div>

    <!-- 其他类型暂不支持预览 -->
    <div v-else class="unsupported">该笔记类型暂不支持预览</div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import TiptapEditor from '@/components/note/TiptapEditor.vue'
import { embedMarksToHtml } from '@/utils/vditorEmbed'

const props = defineProps({
  note: {
    type: Object,
    required: true
  }
})

// Tiptap 渲染所需的 HTML (将 {{embed|...}} 转换为插槽)
const tiptapContent = computed(() => {
  if (!props.note || props.note.type !== 'doc') return ''
  return embedMarksToHtml(props.note.content || '')
})

// 表格数据
const tableData = computed(() => {
  const n = props.note
  if (!n || n.type !== 'table') return { columns: [], rows: [] }
  
  if (n.columns && n.rows) {
    return { columns: n.columns, rows: n.rows }
  } else if (n.content) {
    try {
      const data = JSON.parse(n.content)
      return {
        columns: data.columns || [],
        rows: data.rows || []
      }
    } catch (e) {
      return { columns: [], rows: [] }
    }
  }
  return { columns: [], rows: [] }
})

// 画板 SVG 字符串
const boardSvg = computed(() => {
  const n = props.note
  if (!n || n.type !== 'board') return ''
  
  let elements = []
  let background = '#ffffff'
  if (n.elements) {
    elements = n.elements
    background = n.background || '#ffffff'
  } else if (n.content) {
    try {
      const data = JSON.parse(n.content)
      elements = data.elements || []
      background = data.background || '#ffffff'
    } catch {}
  }
  return generateBoardSvg(elements, background)
})

// 思维导图节点
const mindNodes = computed(() => {
  const n = props.note
  if (!n || n.type !== 'mind') return []
  return n.nodes || []
})

// 生成画板 SVG（简化版，仅绘制矩形、圆形、线条、文本）
function generateBoardSvg(elements, bgColor = '#ffffff') {
  const svgParts = []
  svgParts.push(`<svg width="100%" viewBox="0 0 800 600" xmlns="http://www.w3.org/2000/svg" style="max-width:800px;display:block;margin:0 auto;">`)
  svgParts.push(`<rect width="100%" height="100%" fill="${bgColor}" />`)

  for (const el of elements) {
    const x = el.x || 0
    const y = el.y || 0
    const w = el.width || 0
    const h = el.height || 0
    const fill = el.fill || '#cccccc'
    const stroke = el.stroke || '#333333'
    const strokeWidth = el.strokeWidth || 1

    if (el.type === 'rect') {
      svgParts.push(`<rect x="${x}" y="${y}" width="${w}" height="${h}" fill="${fill}" stroke="${stroke}" stroke-width="${strokeWidth}" />`)
      if (el.content) {
        svgParts.push(`<text x="${x + w/2}" y="${y + h/2 + 5}" text-anchor="middle" font-family="Arial" font-size="14" fill="#333">${escapeHtml(el.content)}</text>`)
      }
    } else if (el.type === 'circle') {
      const radius = el.radius || Math.max(w, h) / 2
      svgParts.push(`<circle cx="${x + radius}" cy="${y + radius}" r="${radius}" fill="${fill}" stroke="${stroke}" stroke-width="${strokeWidth}" />`)
    } else if (el.type === 'line') {
      const startX = el.startX || 0
      const startY = el.startY || 0
      const endX = el.endX || 0
      const endY = el.endY || 0
      svgParts.push(`<line x1="${startX}" y1="${startY}" x2="${endX}" y2="${endY}" stroke="${stroke}" stroke-width="${strokeWidth}" />`)
    } else if (el.type === 'i-text') {
      const text = el.text || el.content || ''
      const fontSize = el.fontSize || 20
      const textFill = el.fill || '#000000'
      svgParts.push(`<text x="${x}" y="${y + fontSize}" font-family="Arial" font-size="${fontSize}" fill="${textFill}">${escapeHtml(text)}</text>`)
    } else if (el.type === 'path' && el.content) {
      try {
        const pathArr = JSON.parse(el.content)
        let d = ''
        pathArr.forEach(cmd => {
          d += cmd.join(' ') + ' '
        })
        svgParts.push(`<path d="${d.trim()}" fill="none" stroke="${stroke}" stroke-width="${strokeWidth}" />`)
      } catch {}
    }
  }

  svgParts.push('</svg>')
  return svgParts.join('\n')
}

function escapeHtml(text) {
  return text.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;').replace(/'/g, '&#039;')
}
</script>

<style scoped>
.note-viewer {
  width: 100%;
}

.content-body {
  line-height: 1.8;
}

.table-container {
  margin-top: 20px;
  overflow-x: auto;
}

.data-table {
  width: 100%;
  border-collapse: collapse;
}

.data-table th,
.data-table td {
  border: 1px solid #ddd;
  padding: 8px 12px;
}

.data-table th {
  background: #f5f5f5;
  font-weight: 600;
}

.board-container {
  margin-top: 20px;
  text-align: center;
}

.mind-container {
  max-width: 900px;
  margin: 0 auto;
}

.mind-tree {
  list-style: none;
  padding-left: 0;
}

.mind-node-item {
  margin: 8px 0;
}

.mind-node-content {
  display: inline-block;
  padding: 6px 16px;
  background: #e8f4fd;
  border-radius: 6px;
  font-size: 14px;
  color: #1f2937;
}

.mind-children {
  list-style: none;
  padding-left: 24px;
  border-left: 2px solid #d1d5db;
  margin-left: 12px;
}

.unsupported {
  color: #999;
  text-align: center;
  margin-top: 40px;
}
</style>
