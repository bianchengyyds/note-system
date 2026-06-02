<template>
  <div class="tiptap-editor-container">
    <!-- 工具栏 -->
    <div class="editor-toolbar" v-if="showToolbar && isEditorReady">
      <!-- 字体大小 -->
      <div class="font-size-dropdown" ref="fontSizeDropdownRef">
        <button 
          class="font-size-btn"
          @click="toggleFontSizeDropdown"
          type="button"
        >
          {{ currentFontSize }}px
          <el-icon><arrow-down /></el-icon>
        </button>
        <div v-if="showFontSizeDropdown" class="font-size-menu">
          <div 
            v-for="size in fontSizes" 
            :key="size" 
            class="font-size-item"
            :class="{ 'is-active': currentFontSize === size }"
            @mousedown.prevent
            @mouseup="applyFontSize(size)"
          >
            {{ size }}px
          </div>
        </div>
      </div>

      <el-divider direction="vertical" />

      <!-- 标题级别 -->
      <el-button-group>
        <el-button 
          size="small" 
          :class="{ 'is-active': isEditorReady && editor?.isActive('heading', { level: 1 }) }"
          @click="toggleHeading(1)"
          title="标题1"
          :disabled="!isEditorReady"
        >
          H1
        </el-button>
        <el-button 
          size="small" 
          :class="{ 'is-active': isEditorReady && editor?.isActive('heading', { level: 2 }) }"
          @click="toggleHeading(2)"
          title="标题2"
          :disabled="!isEditorReady"
        >
          H2
        </el-button>
        <el-button 
          size="small" 
          :class="{ 'is-active': isEditorReady && editor?.isActive('heading', { level: 3 }) }"
          @click="toggleHeading(3)"
          title="标题3"
          :disabled="!isEditorReady"
        >
          H3
        </el-button>
      </el-button-group>

      <el-divider direction="vertical" />

      <!-- 加粗/斜体/删除线/下划线 - Word风格 -->
      <el-button-group>
        <el-button 
          size="small" 
          :class="{ 'is-active': isEditorReady && editor?.isActive('bold') }"
          @click="toggleBold"
          title="加粗 (Ctrl+B)"
          :disabled="!isEditorReady"
        >
          <span class="format-icon format-bold">B</span>
        </el-button>
        <el-button 
          size="small" 
          :class="{ 'is-active': isEditorReady && editor?.isActive('italic') }"
          @click="toggleItalic"
          title="斜体 (Ctrl+I)"
          :disabled="!isEditorReady"
        >
          <span class="format-icon format-italic">I</span>
        </el-button>
        <el-button 
          size="small" 
          :class="{ 'is-active': isEditorReady && editor?.isActive('strike') }"
          @click="toggleStrike"
          title="删除线"
          :disabled="!isEditorReady"
        >
          <span class="format-icon format-strike">S</span>
        </el-button>
        <el-button 
          size="small" 
          :class="{ 'is-active': isEditorReady && editor?.isActive('underline') }"
          @click="toggleUnderline"
          title="下划线"
          :disabled="!isEditorReady"
        >
          <span class="format-icon format-underline">U</span>
        </el-button>
      </el-button-group>

      <el-divider direction="vertical" />

      <el-button-group>
        <el-button 
          size="small" 
          :type="isEditorReady && editor?.isActive('bulletList') ? 'primary' : ''"
          @click="toggleBulletList"
          title="无序列表"
          :disabled="!isEditorReady"
        >
          <el-icon><Menu /></el-icon>
        </el-button>
        <el-button 
          size="small" 
          :type="isEditorReady && editor?.isActive('orderedList') ? 'primary' : ''"
          @click="toggleOrderedList"
          title="有序列表"
          :disabled="!isEditorReady"
        >
          <el-icon><Sort /></el-icon>
        </el-button>
      </el-button-group>

      <el-divider direction="vertical" />

      <el-button-group>
        <el-button 
          size="small" 
          @click="undo"
          :disabled="!isEditorReady || !canUndo"
          title="撤销 (Ctrl+Z)"
        >
          <el-icon><RefreshLeft /></el-icon>
        </el-button>
        <el-button 
          size="small" 
          @click="redo"
          :disabled="!isEditorReady || !canRedo"
          title="重做 (Ctrl+Y)"
        >
          <el-icon><RefreshRight /></el-icon>
        </el-button>
      </el-button-group>

      <el-divider direction="vertical" />

      <!-- 新添加的按钮 -->
      <el-button-group>
        <el-button 
          size="small" 
          :type="props.showOutline ? 'primary' : ''"
          @click="emit('toggle-outline')"
          title="大纲"
          :disabled="!isEditorReady"
        >
          <el-icon><List /></el-icon>
        </el-button>
        <el-button 
          size="small" 
          @click="handleUploadClick"
          title="上传附件"
          :disabled="!isEditorReady"
        >
          <el-icon><Upload /></el-icon>
        </el-button>
      </el-button-group>
    </div>

    <!-- 编辑器主内容区（包含内容和大纲） -->
    <div class="editor-main-wrapper">
      <!-- 编辑器内容区域 -->
      <div 
        ref="editorContentRef"
        class="editor-content"
      >
        <!-- 类似语雀的浮动插入按钮 -->
        <div 
          v-if="editable && isEditorReady && showFloatingButton" 
          class="floating-insert-btn"
          :style="floatingButtonStyle"
          @mousedown.prevent
          @click="handleFloatingClick"
        >
          <el-icon><Plus /></el-icon>
        </div>

        <editor-content v-if="editor && isEditorReady" :editor="editor" />
        <div v-else class="editor-loading">
          <el-icon class="is-loading"><Loading /></el-icon>
          <span>编辑器加载中...</span>
        </div>
      </div>
    </div>

    <!-- 隐藏的文件上传输入框 -->
    <input 
      ref="uploadFileInput"
      type="file" 
      style="display: none;" 
      @change="handleFileUpload"
    />
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount, watch, nextTick } from 'vue'
import { useEditor, EditorContent } from '@tiptap/vue-3'
import StarterKit from '@tiptap/starter-kit'
import Placeholder from '@tiptap/extension-placeholder'
import Image from '@tiptap/extension-image'
import Link from '@tiptap/extension-link'
import Underline from '@tiptap/extension-underline'
import TextAlign from '@tiptap/extension-text-align'
import CodeBlock from '@tiptap/extension-code-block'
import { TextStyle } from '@tiptap/extension-text-style'
import { Extension } from '@tiptap/core'
import { ElMessage } from 'element-plus'

// 自定义字体大小扩展
const FontSize = Extension.create({
  name: 'fontSize',
  addOptions() {
    return {
      types: ['textStyle'],
    }
  },
  addGlobalAttributes() {
    return [
      {
        types: this.options.types,
        attributes: {
          fontSize: {
            default: null,
            parseHTML: element => element.style.fontSize || null,
            renderHTML: attributes => {
              if (!attributes.fontSize) {
                return {}
              }
              return {
                style: `font-size: ${attributes.fontSize}`,
              }
            },
          },
        },
      },
    ]
  },
  addCommands() {
    return {
      setFontSize: fontSize => ({ chain }) => {
        return chain().setMark('textStyle', { fontSize }).run()
      },
      unsetFontSize: () => ({ chain }) => {
        return chain().setMark('textStyle', { fontSize: null }).removeEmptyTextStyle().run()
      },
    }
  },
})
import { 
  Menu, Sort, Grid, 
  Picture, Share, RefreshLeft, RefreshRight, Loading,
  List, Upload, Close, ArrowDown
} from '@element-plus/icons-vue'
import { createNote } from '@/api/note'
import { uploadFile as doUploadFile } from '@/api/file'
import EmbedNode from './EmbedNode.js'

const props = defineProps({
  modelValue: {
    type: String,
    default: ''
  },
  placeholder: {
    type: String,
    default: '开始写作... 点击上方按钮插入组件'
  },
  showToolbar: {
    type: Boolean,
    default: true
  },
  kbId: {
    type: [Number, String],
    default: null
  },
  editable: {
    type: Boolean,
    default: true
  },
  showOutline: {
    type: Boolean,
    default: true
  }
})

const emit = defineEmits(['update:modelValue', 'change', 'headings-change', 'ready', 'open-embed-dialog', 'toggle-outline'])

const editorContentRef = ref(null)
const isEditorReady = ref(false)

// 大纲相关状态
const showOutline = ref(false)
const headings = ref([])
const uploadFileInput = ref(null)

// 浮动按钮相关状态
const showFloatingButton = ref(false)
const floatingButtonStyle = ref({ top: '0px', left: '0px' })

// 字体大小相关
const currentFontSize = ref(15)
const fontSizes = [12, 13, 14, 15, 16, 18, 20, 22, 24, 28, 32, 36, 48, 72]
const showFontSizeDropdown = ref(false)
const fontSizeDropdownRef = ref(null)
const savedSelection = ref(null)

const toggleFontSizeDropdown = () => {
  // 打开下拉框前先保存选区
  if (!showFontSizeDropdown.value && editor.value) {
    const { from, to } = editor.value.state.selection
    if (from !== to) {
      savedSelection.value = { from, to }
    }
  }
  showFontSizeDropdown.value = !showFontSizeDropdown.value
}

// 点击外部关闭下拉框
const handleClickOutside = (e) => {
  if (fontSizeDropdownRef.value && !fontSizeDropdownRef.value.contains(e.target)) {
    showFontSizeDropdown.value = false
  }
}

onMounted(() => {
  document.addEventListener('click', handleClickOutside)
})

onBeforeUnmount(() => {
  document.removeEventListener('click', handleClickOutside)
})

const applyFontSize = (size) => {
  console.log('applyFontSize called, size:', size)
  console.log('editor exists:', !!editor.value)
  console.log('savedSelection:', savedSelection.value)
  
  if (!editor.value) return
  currentFontSize.value = size
  showFontSizeDropdown.value = false
  
  if (savedSelection.value) {
    const { from, to } = savedSelection.value
    console.log('Restoring selection from:', from, 'to:', to)
    savedSelection.value = null
    // 恢复选区并应用字体大小
    const result = editor.value.chain().setTextSelection({ from, to }).setFontSize(size + 'px').run()
    console.log('setFontSize result:', result)
    console.log('HTML after:', editor.value.getHTML())
  } else {
    console.log('No saved selection, applying at cursor')
    const result = editor.value.chain().focus().setFontSize(size + 'px').run()
    console.log('setFontSize result:', result)
    console.log('HTML after:', editor.value.getHTML())
  }
}

// 检查是否显示浮动按钮（当前行为空时显示）
const updateFloatingButton = () => {
  if (!editor.value || !props.editable) {
    showFloatingButton.value = false
    return
  }

  const { selection } = editor.value.state
  const { $from } = selection
  
  // 只有当光标在段落中，且段落为空时显示
  const isParagraph = $from.parent.type.name === 'paragraph'
  const isEmpty = $from.parent.content.size === 0

  if (isParagraph && isEmpty) {
    const view = editor.value.view
    const coords = view.coordsAtPos($from.pos)
    const editorRect = editorContentRef.value?.getBoundingClientRect()

    if (editorRect) {
      floatingButtonStyle.value = {
        top: `${coords.top - editorRect.top + 2}px`, // 微调对齐
        left: `16px`
      }
      showFloatingButton.value = true
    }
  } else {
    showFloatingButton.value = false
  }
}

const handleFloatingClick = () => {
  emit('open-embed-dialog')
}

// 定义扩展
const extensions = [
  StarterKit.configure({
    heading: {
      levels: [1, 2, 3, 4, 5, 6]
    },
    codeBlock: false
  }),
  Placeholder.configure({
    placeholder: props.placeholder
  }),
  Image,
  Link.configure({
    openOnClick: false,
    HTMLAttributes: {
      class: 'tiptap-link'
    }
  }),
  Underline,
  TextStyle,
  FontSize,
  TextAlign.configure({
    types: ['heading', 'paragraph']
  }),
  CodeBlock.configure({
    HTMLAttributes: {
      class: 'hljs'
    }
  }),
  // 自定义嵌入组件节点
  EmbedNode
]

// 使用 useEditor 初始化编辑器 (Top-level call)
const editor = useEditor({
  content: props.modelValue || '',
  extensions,
  editable: props.editable,
  autofocus: 'end',
  onTransaction: ({ transaction }) => {
    if (!isEditorReady.value || !editor.value) return
    
    const html = editor.value.getHTML()
    emit('update:modelValue', html)
    emit('change', html)
    
    // 更新浮动按钮位置
    updateFloatingButton()
    
    // 只在文档内容变化时提取标题
    if (transaction.docChanged) {
      setTimeout(() => {
        if (editor.value) {
          const headings = extractHeadings(editor.value)
          emit('headings-change', headings)
        }
      }, 100)
    }
  },
  onSelectionUpdate: () => {
    updateFloatingButton()
  },
  onCreate: ({ editor: editorInstance }) => {
    console.log('Tiptap onCreate 回调被调用')
    isEditorReady.value = true
    console.log('Tiptap 编辑器已就绪')
    // 发射 ready 事件
    setTimeout(() => {
      console.log('发射 ready 事件')
      emit('ready', editorInstance)
    }, 50)
  },
  onDestroy: () => {
    isEditorReady.value = false
    console.log('Tiptap 编辑器已销毁')
  },
  editorProps: {
    attributes: {
      class: 'prose prose-sm sm:prose lg:prose-lg xl:prose-2xl mx-auto focus:outline-none'
    },
    handleDOMEvents: {
      keydown: (view, event) => {
        if ((event.ctrlKey || event.metaKey) && event.key === 's') {
          event.preventDefault()
          return true
        }
        return false
      }
    }
  }
})

// 监听编辑状态变化
watch(() => props.editable, (newVal) => {
  if (editor.value) {
    editor.value.setEditable(newVal)
  }
})

// 监听外部值变化
watch(() => props.modelValue, (newVal) => {
  if (editor.value && isEditorReady.value) {
    const currentHTML = editor.value.getHTML()
    // 只有当新值与当前值不同时才更新
    if (newVal !== undefined && newVal !== null && newVal !== currentHTML) {
      editor.value.commands.setContent(newVal, false)
    }
  }
}, { immediate: false })

// 监听 editable prop 变化，动态更新编辑器可编辑状态
watch(() => props.editable, (newEditable) => {
  if (editor.value) {
    editor.value.setEditable(newEditable)
  }
})

// 监听编辑器就绪状态，确保初始内容被正确设置
watch(() => isEditorReady.value, (ready) => {
  if (ready && props.modelValue && editor.value) {
    const currentHTML = editor.value.getHTML()
    if (props.modelValue !== currentHTML) {
      editor.value.commands.setContent(props.modelValue, false)
    }
  }
})

// ==================== 格式化操作 ====================

const toggleHeading = (level) => {
  editor.value.chain().focus().toggleHeading({ level }).run()
}

const toggleBold = () => {
  editor.value.chain().focus().toggleBold().run()
}

const toggleItalic = () => {
  editor.value.chain().focus().toggleItalic().run()
}

const toggleStrike = () => {
  editor.value.chain().focus().toggleStrike().run()
}

const toggleUnderline = () => {
  editor.value.chain().focus().toggleUnderline().run()
}

const toggleBulletList = () => {
  editor.value.chain().focus().toggleBulletList().run()
}

const toggleOrderedList = () => {
  editor.value.chain().focus().toggleOrderedList().run()
}

const undo = () => {
  editor.value.chain().focus().undo().run()
}

const redo = () => {
  editor.value.chain().focus().redo().run()
}

// ==================== 辅助函数 ====================

const extractHeadings = (editorInstance) => {
  if (!editorInstance) return []
  
  const extractedHeadings = []
  const doc = editorInstance.state.doc
  
  doc.forEach((node, pos) => {
    if (node.type.name === 'heading') {
      extractedHeadings.push({
        level: node.attrs.level,
        text: node.textContent,
        pos: pos
      })
    }
  })
  
  // 更新本地的 headings 状态
  headings.value = extractedHeadings
  
  return extractedHeadings
}

// 大纲功能相关函数
const toggleOutline = () => {
  showOutline.value = !showOutline.value
}

const scrollToHeading = (heading) => {
  if (!editor.value) return
  const view = editor.value.view
  const dom = view.nodeDOM(heading.pos)
  if (dom) {
    dom.scrollIntoView({ behavior: 'smooth', block: 'start' })
  }
}

// 文件上传相关函数
const handleUploadClick = () => {
  uploadFileInput.value?.click()
}

const handleFileUpload = async (event) => {
  const file = event.target.files?.[0]
  if (!file) return
  
  try {
    const result = await doUploadFile(file)
    if (result.code === 1 && result.data?.url) {
      const url = result.data.url
      const linkMarkdown = `[${file.name}](${url})`
      
      if (editor.value) {
        editor.value.chain().focus().insertContent(linkMarkdown).run()
      }
      
      ElMessage.success('附件上传成功，链接已插入')
    } else {
      ElMessage.error(result.msg || '上传失败')
    }
  } catch (error) {
    ElMessage.error('上传失败')
  }
  
  // 清空 input
  if (uploadFileInput.value) {
    uploadFileInput.value.value = ''
  }
}

// 提供一些方法给父组件使用
defineExpose({
  editor,
  isReady: () => isEditorReady.value, // 暴露就绪状态检查方法
  getHTML: () => editor.value?.getHTML(),
  getJSON: () => editor.value?.getJSON(),
  insertEmbed: (type, noteId, title) => {
    if (editor.value) {
      editor.value.chain().focus().setEmbedComponent({
        type,
        noteId,
        title
      }).run()
    }
  }
})
</script>

<style scoped>
.tiptap-editor-container {
  width: 100%;
  display: flex;
  flex-direction: column;
  background: #fff;
  border-radius: 8px;
  min-height: 100%;
}

/* 确保 textStyle 的 fontSize 能正确应用 */
:deep(.ProseMirror span[style]) {
  /* 保留内联样式，不被覆盖 */
}

/* 字体大小下拉框样式 */
.font-size-dropdown {
  position: relative;
  display: inline-block;
}

.font-size-btn {
  height: 32px;
  padding: 0 12px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  background: #fff;
  font-size: 14px;
  color: #606266;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 4px;
  min-width: 80px;
  justify-content: space-between;
}

.font-size-btn:hover {
  border-color: #c0c4cc;
}

.font-size-menu {
  position: absolute;
  top: 100%;
  left: 0;
  margin-top: 4px;
  background: #fff;
  border: 1px solid #e4e7ed;
  border-radius: 4px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
  max-height: 200px;
  overflow-y: auto;
  z-index: 1000;
  min-width: 80px;
}

.font-size-item {
  padding: 6px 12px;
  cursor: pointer;
  font-size: 14px;
  color: #606266;
  white-space: nowrap;
}

.font-size-item:hover {
  background: #f5f7fa;
  color: #409eff;
}

.font-size-item.is-active {
  color: #409eff;
  font-weight: 600;
}

.editor-toolbar {
  padding: 8px 16px;
  border-bottom: 1px solid #e8e8e8;
  background: #f7f9fa;
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  border-radius: 8px 8px 0 0;
  position: sticky;
  top: 0;
  z-index: 100;
  flex-shrink: 0;
}

/* Word风格格式图标 */
.format-icon {
  font-family: 'Times New Roman', serif;
  font-size: 15px;
  line-height: 1;
  display: inline-block;
  width: 18px;
  text-align: center;
}

.format-bold {
  font-weight: 700;
}

.format-italic {
  font-style: italic;
  font-weight: 400;
}

.format-strike {
  text-decoration: line-through;
  font-weight: 400;
}

.format-underline {
  text-decoration: underline;
  font-weight: 400;
}

/* 按钮激活状态 */
.el-button-group .el-button.is-active {
  color: #409eff;
  background-color: #ecf5ff;
  border-color: #c6e2ff;
}

/* 编辑器主内容区 */
.editor-main-wrapper {
  width: 100%;
}

.editor-content {
  padding: 32px 48px;
  position: relative;
}

/* 浮动插入按钮样式 */
.floating-insert-btn {
  position: absolute;
  width: 24px;
  height: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #fff;
  border: 1px solid #e8e8e8;
  border-radius: 4px;
  color: #999;
  cursor: pointer;
  transition: all 0.2s ease;
  z-index: 10;
  box-shadow: 0 2px 8px rgba(0,0,0,0.05);
}

.floating-insert-btn:hover {
  color: #25b864;
  border-color: #25b864;
  background: #f6ffed;
  transform: scale(1.1);
}

/* 编辑器加载状态 */
.editor-loading {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  min-height: 400px;
  color: #999;
  font-size: 14px;
}

.editor-loading .el-icon {
  font-size: 24px;
}

/* Tiptap 编辑器样式 */
:deep(.ProseMirror) {
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Helvetica, Arial, sans-serif;
  font-size: 16px;
  line-height: 1.8;
  color: #262626;
  outline: none;
}

/* 确保内联 font-size 样式不被覆盖 */
:deep(.ProseMirror span[style*="font-size"]) {
  /* inline style has higher priority by default */
}

:deep(.ProseMirror p.is-editor-empty:first-child::before) {
  color: #adb5bd;
  content: attr(data-placeholder);
  float: left;
  height: 0;
  pointer-events: none;
}

:deep(.ProseMirror h1),
:deep(.ProseMirror h2),
:deep(.ProseMirror h3),
:deep(.ProseMirror h4),
:deep(.ProseMirror h5),
:deep(.ProseMirror h6) {
  margin-top: 24px;
  margin-bottom: 16px;
  font-weight: 600;
  line-height: 1.25;
  color: #000;
}

:deep(.ProseMirror h1) {
  font-size: 2em;
  border-bottom: 1px solid #eaecef;
  padding-bottom: 0.3em;
}

:deep(.ProseMirror h2) {
  font-size: 1.5em;
  border-bottom: 1px solid #eaecef;
  padding-bottom: 0.3em;
}

:deep(.ProseMirror h3) {
  font-size: 1.25em;
}

:deep(.ProseMirror p) {
  margin-bottom: 16px;
}

:deep(.ProseMirror code) {
  background-color: #f0f0f0;
  padding: 2px 6px;
  border-radius: 3px;
  font-family: "SFMono-Regular", Consolas, "Liberation Mono", Menlo, monospace;
  font-size: 85%;
}

:deep(.ProseMirror pre) {
  background-color: #f6f8fa;
  border-radius: 6px;
  padding: 16px;
  overflow: auto;
}

:deep(.ProseMirror blockquote) {
  border-left: 4px solid #dfe2e5;
  color: #6a737d;
  padding: 0 1em;
  margin: 0 0 16px 0;
}

:deep(.ProseMirror ul),
:deep(.ProseMirror ol) {
  padding-left: 2em;
  margin-bottom: 16px;
}

:deep(.ProseMirror li) {
  margin-bottom: 4px;
}

:deep(.ProseMirror a) {
  color: #25b864;
  text-decoration: none;
}

:deep(.ProseMirror a:hover) {
  text-decoration: underline;
}

:deep(.ProseMirror img) {
  max-width: 100%;
  height: auto;
  border-radius: 4px;
}

/* 嵌入组件样式 */
:deep(.embed-component-wrapper) {
  margin: 16px 0;
}

/* 嵌入组件中表格隐藏行号列 */
:deep(.embed-component-wrapper .table-wrapper .jexcel colgroup col:first-child),
:deep(.embed-component-wrapper .table-wrapper .jspreadsheet colgroup col:first-child) {
  width: 0 !important;
  min-width: 0 !important;
}

:deep(.embed-component-wrapper .table-wrapper .jexcel .jexcel_row_header),
:deep(.embed-component-wrapper .table-wrapper .jexcel_headers .jexcel_headers_row_header),
:deep(.embed-component-wrapper .table-wrapper .jspreadsheet .jspreadsheet_row_header),
:deep(.embed-component-wrapper .table-wrapper .jspreadsheet_headers .jspreadsheet_headers_row_header) {
  display: none !important;
  width: 0 !important;
}

/* 隐藏表格第一列（行号列） */
:deep(.embed-component-wrapper .table-wrapper .jexcel table tbody tr td:first-child),
:deep(.embed-component-wrapper .table-wrapper .jexcel table thead tr th:first-child),
:deep(.embed-component-wrapper .table-wrapper .jspreadsheet table tbody tr td:first-child),
:deep(.embed-component-wrapper .table-wrapper .jspreadsheet table thead tr th:first-child) {
  display: none !important;
  width: 0 !important;
  padding: 0 !important;
  border: none !important;
}
</style>