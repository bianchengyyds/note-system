<!-- 测试专用 -->
<template>
  <div class="test-container">
    <h1>Vditor 编辑器测试</h1>
    
    <!-- 简单的标题输入 -->
    <div class="title-section">
      <input 
        v-model="title" 
        placeholder="请输入标题" 
        class="title-input"
      />
    </div>
    
    <!-- 编辑器区域 -->
    <div class="editor-section">
      <RichTextEditor
        ref="editorRef"
        v-model="content"
        placeholder="开始写作... 支持 Markdown 语法"
        @headings-change="handleHeadingsChange"
      />
    </div>
    
    <!-- 右侧大纲预览 -->
    <div class="outline-preview" v-if="headings.length > 0">
      <h3>目录预览</h3>
      <ul>
        <li 
          v-for="heading in headings" 
          :key="heading.id"
          :style="{ paddingLeft: heading.level * 20 + 'px' }"
        >
          {{ heading.text }} (H{{ heading.level }})
        </li>
      </ul>
    </div>
    
    <!-- 调试信息 -->
    <div class="debug-info">
      <h3>调试信息</h3>
      <p>标题：{{ title }}</p>
      <p>内容长度：{{ content?.length || 0 }} 字符</p>
      <p>大纲数量：{{ headings.length }} 个</p>
      <button @click="showContent">查看完整内容</button>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import RichTextEditor from '@/components/note/RichTextEditor.vue'

const title = ref('')
const content = ref('')
const headings = ref([])
const editorRef = ref(null)

// 处理标题变化
const handleHeadingsChange = (newHeadings) => {
  headings.value = newHeadings
  console.log('大纲更新：', newHeadings)
}

// 查看完整内容
const showContent = () => {
  console.log('=== 当前内容 ===')
  console.log(content.value)
  alert('内容已输出到控制台，请打开浏览器开发者工具查看')
}
</script>

<style scoped>
.test-container {
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
}

.outline-preview {
  margin-top: 20px;
  padding: 16px;
  background: #f7f9fa;
  border: 1px solid #e8e8e8;
  border-radius: 4px;
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

.debug-info button {
  margin-top: 10px;
  padding: 8px 16px;
  background: #25b864;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
}

.debug-info button:hover {
  background: #20a058;
}
</style>
