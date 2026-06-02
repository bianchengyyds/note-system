<template>
  <div class="test-tiptap-container">
    <h1>Tiptap 富文本编辑器测试</h1>
    
    <!-- 标题输入 -->
    <div class="title-section">
      <input 
        v-model="title" 
        placeholder="请输入标题" 
        class="title-input"
      />
    </div>
    
    <!-- Tiptap 编辑器 -->
    <div class="editor-section">
      <TiptapEditor
        ref="editorRef"
        v-model="content"
        :kb-id="kbId"
        placeholder="开始写作... 点击上方按钮插入表格、画板、思维导图等组件"
        @headings-change="handleHeadingsChange"
      />
    </div>
    
    <!-- 右侧大纲预览 -->
    <div class="outline-preview" v-if="headings.length > 0">
      <h3>目录预览</h3>
      <ul>
        <li 
          v-for="(heading, index) in headings" 
          :key="index"
          :style="{ paddingLeft: heading.level * 20 + 'px' }"
        >
          {{ heading.text }} (H{{ heading.level }})
        </li>
      </ul>
    </div>
    
    <!-- 操作按钮 -->
    <div class="actions">
      <el-button type="primary" @click="saveContent">保存内容</el-button>
      <el-button @click="clearContent">清空内容</el-button>
      <el-button @click="showHTML">查看 HTML</el-button>
      <el-button @click="showJSON">查看 JSON</el-button>
    </div>
    
    <!-- 调试信息 -->
    <div class="debug-info">
      <h3>调试信息</h3>
      <p>标题：{{ title }}</p>
      <p>内容长度：{{ content?.length || 0 }} 字符</p>
      <p>大纲数量：{{ headings.length }} 个</p>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import TiptapEditor from '@/components/note/TiptapEditor.vue'

const title = ref('测试文档')
const content = ref('')
const headings = ref([])
const kbId = ref(1) // 示例知识库ID
const editorRef = ref(null)

// 处理标题变化
const handleHeadingsChange = (newHeadings) => {
  headings.value = newHeadings
  console.log('大纲更新：', newHeadings)
}

// 保存内容
const saveContent = () => {
  console.log('=== 保存内容 ===')
  console.log('标题:', title.value)
  console.log('HTML:', content.value)
  ElMessage.success('内容已保存到控制台')
}

// 清空内容
const clearContent = () => {
  content.value = ''
  ElMessage.success('内容已清空')
}

// 查看 HTML
const showHTML = () => {
  console.log('=== HTML 内容 ===')
  console.log(content.value)
  ElMessage.success('HTML 已输出到控制台')
}

// 查看 JSON
const showJSON = () => {
  const json = editorRef.value?.getJSON()
  console.log('=== JSON 内容 ===')
  console.log(JSON.stringify(json, null, 2))
  ElMessage.success('JSON 已输出到控制台')
}
</script>

<style scoped>
.test-tiptap-container {
  padding: 20px;
  max-width: 1400px;
  margin: 0 auto;
}

h1 {
  margin-bottom: 20px;
  color: #262626;
}

.title-section {
  margin-bottom: 20px;
}

.title-input {
  width: 100%;
  padding: 12px 16px;
  font-size: 18px;
  border: 1px solid #d9d9d9;
  border-radius: 4px;
  outline: none;
}

.title-input:focus {
  border-color: #25b864;
}

.editor-section {
  height: 600px;
  border: 1px solid #e8e8e8;
  border-radius: 4px;
  overflow: hidden;
  margin-bottom: 20px;
}

.outline-preview {
  position: fixed;
  right: 20px;
  top: 100px;
  width: 250px;
  padding: 16px;
  background: #f7f9fa;
  border: 1px solid #e8e8e8;
  border-radius: 4px;
  max-height: 400px;
  overflow-y: auto;
}

.outline-preview h3 {
  margin-bottom: 12px;
  font-size: 14px;
  color: #262626;
}

.outline-preview ul {
  list-style: none;
  padding: 0;
  margin: 0;
}

.outline-preview li {
  padding: 4px 0;
  font-size: 13px;
  color: #595959;
  cursor: pointer;
}

.outline-preview li:hover {
  color: #25b864;
}

.actions {
  margin-top: 20px;
  display: flex;
  gap: 10px;
}

.debug-info {
  margin-top: 20px;
  padding: 16px;
  background: #fffbe6;
  border: 1px solid #ffe58f;
  border-radius: 4px;
}

.debug-info h3 {
  margin-bottom: 12px;
  font-size: 14px;
}
</style>
