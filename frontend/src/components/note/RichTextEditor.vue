<template>
  <div class="tiptap-editor-wrapper">
    <div class="tiptap-toolbar">
      <button 
        type="button" 
        class="toolbar-btn" 
        :class="{ active: editor?.isActive('heading', { level: 1 }) }"
        @click="toggleHeading(1)"
        title="标题1"
      >
        H1
      </button>
      <button 
        type="button" 
        class="toolbar-btn" 
        :class="{ active: editor?.isActive('heading', { level: 2 }) }"
        @click="toggleHeading(2)"
        title="标题2"
      >
        H2
      </button>
      <button 
        type="button" 
        class="toolbar-btn" 
        :class="{ active: editor?.isActive('heading', { level: 3 }) }"
        @click="toggleHeading(3)"
        title="标题3"
      >
        H3
      </button>
      <span class="toolbar-divider"></span>
      <button 
        type="button" 
        class="toolbar-btn" 
        :class="{ active: editor?.isActive('bold') }"
        @click="toggleBold"
        title="加粗"
      >
        <strong>B</strong>
      </button>
      <button 
        type="button" 
        class="toolbar-btn" 
        :class="{ active: editor?.isActive('italic') }"
        @click="toggleItalic"
        title="斜体"
      >
        <em>I</em>
      </button>
      <button 
        type="button" 
        class="toolbar-btn" 
        :class="{ active: editor?.isActive('strike') }"
        @click="toggleStrike"
        title="删除线"
      >
        <s>S</s>
      </button>
      <button 
        type="button" 
        class="toolbar-btn" 
        :class="{ active: editor?.isActive('underline') }"
        @click="toggleUnderline"
        title="下划线"
      >
        <u>U</u>
      </button>
      <span class="toolbar-divider"></span>
      <button 
        type="button" 
        class="toolbar-btn" 
        :class="{ active: editor?.isActive('bulletList') }"
        @click="toggleBulletList"
        title="无序列表"
      >
        • 列表
      </button>
      <button 
        type="button" 
        class="toolbar-btn" 
        :class="{ active: editor?.isActive('orderedList') }"
        @click="toggleOrderedList"
        title="有序列表"
      >
        1. 列表
      </button>
      <button 
        type="button" 
        class="toolbar-btn" 
        :class="{ active: editor?.isActive('taskList') }"
        @click="toggleTaskList"
        title="任务列表"
      >
        ☑ 任务
      </button>
      <span class="toolbar-divider"></span>
      <button 
        type="button" 
        class="toolbar-btn" 
        :class="{ active: editor?.isActive('blockquote') }"
        @click="toggleBlockquote"
        title="引用"
      >
        " 引用
      </button>
      <button 
        type="button" 
        class="toolbar-btn" 
        :class="{ active: editor?.isActive('codeBlock') }"
        @click="toggleCodeBlock"
        title="代码块"
      >
        &lt;/&gt; 代码
      </button>
      <button 
        type="button" 
        class="toolbar-btn"
        @click="toggleInlineCode"
        title="行内代码"
      >
        `code`
      </button>
      <span class="toolbar-divider"></span>
      <button 
        type="button" 
        class="toolbar-btn"
        @click="insertLink"
        title="插入链接"
      >
        🔗 链接
      </button>
      <button 
        type="button" 
        class="toolbar-btn"
        @click="insertImage"
        title="插入图片"
      >
        🖼️ 图片
      </button>
      <span class="toolbar-divider"></span>
      <button 
        type="button" 
        class="toolbar-btn"
        @click="insertTable"
        title="插入表格"
      >
        📊 表格
      </button>
      <button 
        type="button" 
        class="toolbar-btn"
        @click="insertHr"
        title="分割线"
      >
        ── 分割线
      </button>
      <span class="toolbar-divider"></span>
      <button 
        type="button" 
        class="toolbar-btn"
        @click="undo"
        :disabled="!editor?.can().undo"
        title="撤销"
      >
        ↩️ 撤销
      </button>
      <button 
        type="button" 
        class="toolbar-btn"
        @click="redo"
        :disabled="!editor?.can().redo"
        title="重做"
      >
        ↪️ 重做
      </button>
    </div>

    <editor-content :editor="editor" class="tiptap-content" />

    <!-- 链接输入弹窗 -->
    <div v-if="showLinkModal" class="modal-overlay" @click.self="showLinkModal = false">
      <div class="modal-content">
        <h3>插入链接</h3>
        <input 
          v-model="linkUrl" 
          type="text" 
          placeholder="请输入链接地址"
          class="link-input"
          @keyup.enter="confirmLink"
        />
        <input 
          v-model="linkText" 
          type="text" 
          placeholder="链接文本（可选）"
          class="link-input"
        />
        <div class="modal-actions">
          <button class="modal-btn cancel" @click="showLinkModal = false">取消</button>
          <button class="modal-btn confirm" @click="confirmLink">确认</button>
        </div>
      </div>
    </div>

    <!-- 图片上传弹窗 -->
    <div v-if="showImageModal" class="modal-overlay" @click.self="showImageModal = false">
      <div class="modal-content">
        <h3>插入图片</h3>
        <input 
          v-model="imageUrl" 
          type="text" 
          placeholder="请输入图片URL"
          class="link-input"
          @keyup.enter="confirmImage"
        />
        <input 
          type="file" 
          accept="image/*" 
          class="image-upload" 
          @change="handleImageUpload"
          ref="imageUploadRef"
        />
        <label class="upload-label" for="image-upload">📁 点击上传图片</label>
        <div class="modal-actions">
          <button class="modal-btn cancel" @click="showImageModal = false">取消</button>
          <button class="modal-btn confirm" @click="confirmImage">确认</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, watch, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { useEditor, EditorContent } from '@tiptap/vue-3'
import StarterKit from '@tiptap/starter-kit'
import Underline from '@tiptap/extension-underline'
import Link from '@tiptap/extension-link'
import Image from '@tiptap/extension-image'
import Placeholder from '@tiptap/extension-placeholder'
import { getToken } from '@/utils/auth'
import { normalizeDocContent, buildEmbedMark, extractEmbedRefs } from '@/utils/docEmbed'

const props = defineProps({
  modelValue: {
    type: String,
    default: ''
  },
  placeholder: {
    type: String,
    default: '开始写作...'
  }
})

const emit = defineEmits(['update:modelValue', 'change', 'headings-change', 'ready'])

// 编辑器实例
const editor = useEditor({
  extensions: [
    StarterKit,
    Underline,
    Link.configure({
      openOnClick: true,
      autolink: true
    }),
    Image.configure({
      inline: true
    }),
    Placeholder.configure({
      placeholder: props.placeholder,
    })
  ],
  content: props.modelValue,
  onUpdate: ({ editor }) => {
    const content = editor.getHTML()
    emit('update:modelValue', content)
    emit('change', content)
    extractHeadings()
  }
})

// 弹窗状态
const showLinkModal = ref(false)
const showImageModal = ref(false)
const linkUrl = ref('')
const linkText = ref('')
const imageUrl = ref('')
const imageUploadRef = ref(null)

// 工具栏操作
const toggleHeading = (level) => {
  editor.value?.chain().focus().toggleHeading({ level }).run()
}

const toggleBold = () => {
  editor.value?.chain().focus().toggleBold().run()
}

const toggleItalic = () => {
  editor.value?.chain().focus().toggleItalic().run()
}

const toggleStrike = () => {
  editor.value?.chain().focus().toggleStrike().run()
}

const toggleUnderline = () => {
  editor.value?.chain().focus().toggleUnderline().run()
}

const toggleBulletList = () => {
  editor.value?.chain().focus().toggleBulletList().run()
}

const toggleOrderedList = () => {
  editor.value?.chain().focus().toggleOrderedList().run()
}

const toggleTaskList = () => {
  editor.value?.chain().focus().toggleTaskList().run()
}

const toggleBlockquote = () => {
  editor.value?.chain().focus().toggleBlockquote().run()
}

const toggleCodeBlock = () => {
  editor.value?.chain().focus().toggleCodeBlock().run()
}

const toggleInlineCode = () => {
  editor.value?.chain().focus().toggleCode().run()
}

const insertLink = () => {
  showLinkModal.value = true
  const selection = editor.value?.getText()
  if (selection) {
    linkText.value = selection
  }
}

const confirmLink = () => {
  if (!linkUrl.value) return
  editor.value?.chain().focus().extendMarkRange('link').unsetLink().setLink({ href: linkUrl.value }).run()
  if (linkText.value) {
    editor.value?.chain().focus().insertContent(linkText.value).run()
  }
  showLinkModal.value = false
  linkUrl.value = ''
  linkText.value = ''
}

const insertImage = () => {
  showImageModal.value = true
}

const handleImageUpload = async (event) => {
  const file = event.target.files?.[0]
  if (!file) return

  const formData = new FormData()
  formData.append('file', file)

  try {
    const response = await fetch('/api/file/upload', {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${getToken()}`
      },
      body: formData
    })
    const res = await response.json()
    if (res.code === 1 && res.data?.url) {
      imageUrl.value = res.data.url
    }
  } catch (error) {
    console.error('上传失败:', error)
  }
}

const confirmImage = () => {
  if (!imageUrl.value) return
  editor.value?.chain().focus().setImage({ src: imageUrl.value }).run()
  showImageModal.value = false
  imageUrl.value = ''
}

const insertTable = () => {
  editor.value?.chain().focus().insertTable({ rows: 3, cols: 3 }).run()
}

const insertHr = () => {
  editor.value?.chain().focus().setHorizontalRule().run()
}

const undo = () => {
  editor.value?.chain().focus().undo().run()
}

const redo = () => {
  editor.value?.chain().focus().redo().run()
}

// 提取标题
const extractHeadings = () => {
  if (!editor.value) return
  
  const headings = []
  editor.value.doc.content.forEach((node, index) => {
    if (node.type.name === 'heading') {
      headings.push({
        id: `heading-${index}`,
        level: node.attrs.level,
        text: node.textContent.trim()
      })
    }
  })
  
  emit('headings-change', headings)
}

// 组件挂载时等待编辑器就绪并触发事件
onMounted(async () => {
  try {
    const instance = await editor.value
    if (instance) {
      emit('ready', instance)
    }
  } catch (error) {
    console.error('编辑器初始化失败:', error)
  }
})

// 监听外部值变化
watch(() => props.modelValue, (newVal) => {
  if (editor.value && newVal !== editor.value.getHTML()) {
    editor.value.commands.setContent(newVal)
  }
})

// 组件卸载时销毁
onBeforeUnmount(() => {
  editor.value?.destroy()
})

// 暴露方法给父组件
defineExpose({
  getValue: () => editor.value?.getHTML(),
  setValue: (value) => editor.value?.commands.setContent(value),
  focus: () => editor.value?.commands.focus(),
  getHTML: () => editor.value?.getHTML(),
  getMarkdown: () => editor.value?.getMarkdown?.() || '',
  insertEmbed: (embedType, noteId) => {
    const mark = buildEmbedMark(embedType, noteId)
    editor.value?.chain().focus().insertContent(`<p>${mark}</p>`).run()
  },
  editor: editor
})
</script>

<style scoped>
.tiptap-editor-wrapper {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  border: 1px solid #e8e8e8;
  border-radius: 4px;
  overflow: hidden;
  background: #fff;
}

.tiptap-toolbar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 4px;
  padding: 8px 12px;
  background-color: #f7f9fa;
  border-bottom: 1px solid #e8e8e8;
  flex-shrink: 0;
}

.toolbar-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 6px 12px;
  border: none;
  background: transparent;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
  color: #595959;
  transition: all 0.2s;
}

.toolbar-btn:hover {
  background: #e8f4fd;
  color: #1890ff;
}

.toolbar-btn.active {
  background: #1890ff;
  color: #fff;
}

.toolbar-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.toolbar-divider {
  width: 1px;
  height: 20px;
  background: #e8e8e8;
  margin: 0 4px;
}

.tiptap-content {
  flex: 1;
  overflow-y: auto;
  min-height: 300px;
}

/* 编辑器内容样式（语雀风格） */
:deep(.ProseMirror) {
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Helvetica, Arial, sans-serif;
  font-size: 16px;
  line-height: 1.8;
  color: #262626;
  padding: 40px;
  max-width: 900px;
  margin: 0 auto;
  outline: none;
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
  margin: 16px 0;
}

:deep(.ProseMirror pre code) {
  background: none;
  padding: 0;
}

:deep(.ProseMirror blockquote) {
  border-left: 4px solid #dfe2e5;
  color: #6a737d;
  padding: 0 1em;
  margin: 16px 0;
  background: #f7f7f7;
  padding: 8px 16px;
  border-radius: 0 4px 4px 0;
}

:deep(.ProseMirror table) {
  border-collapse: collapse;
  width: 100%;
  margin-bottom: 16px;
}

:deep(.ProseMirror table th),
:deep(.ProseMirror table td) {
  border: 1px solid #dfe2e5;
  padding: 6px 13px;
}

:deep(.ProseMirror table tr:nth-child(2n)) {
  background-color: #f6f8fa;
}

:deep(.ProseMirror ul),
:deep(.ProseMirror ol) {
  padding-left: 2em;
  margin-bottom: 16px;
}

:deep(.ProseMirror li) {
  margin-bottom: 4px;
}

:deep(.ProseMirror ul[data-type="taskList"] li) {
  list-style: none;
  display: flex;
  align-items: flex-start;
  gap: 8px;
}

:deep(.ProseMirror ul[data-type="taskList"] li > label) {
  margin-top: 4px;
}

:deep(.ProseMirror ul[data-type="taskList"] li > div) {
  flex: 1;
}

:deep(.ProseMirror hr) {
  border: none;
  border-top: 1px solid #eaecef;
  margin: 24px 0;
}

:deep(.ProseMirror a) {
  color: #1890ff;
  text-decoration: none;
}

:deep(.ProseMirror a:hover) {
  text-decoration: underline;
}

:deep(.ProseMirror img) {
  max-width: 100%;
  border-radius: 4px;
}

/* 弹窗样式 */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.modal-content {
  background: #fff;
  padding: 24px;
  border-radius: 8px;
  width: 400px;
}

.modal-content h3 {
  margin: 0 0 16px 0;
  font-size: 16px;
  font-weight: 600;
}

.link-input {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid #d9d9d9;
  border-radius: 4px;
  margin-bottom: 12px;
  font-size: 14px;
  box-sizing: border-box;
}

.link-input:focus {
  outline: none;
  border-color: #1890ff;
}

.image-upload {
  display: none;
}

.upload-label {
  display: inline-block;
  padding: 10px 20px;
  background: #f7f9fa;
  border: 1px dashed #d9d9d9;
  border-radius: 4px;
  cursor: pointer;
  margin-bottom: 12px;
  font-size: 14px;
}

.upload-label:hover {
  border-color: #1890ff;
  background: #e8f4fd;
}

.modal-actions {
  display: flex;
  gap: 12px;
  justify-content: flex-end;
}

.modal-btn {
  padding: 8px 16px;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
}

.modal-btn.cancel {
  background: #f5f5f5;
  color: #595959;
}

.modal-btn.cancel:hover {
  background: #e8e8e8;
}

.modal-btn.confirm {
  background: #1890ff;
  color: #fff;
}

.modal-btn.confirm:hover {
  background: #40a9ff;
}
</style>