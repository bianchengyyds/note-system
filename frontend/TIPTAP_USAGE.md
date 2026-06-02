# Tiptap 富文本编辑器使用指南

## 概述

本项目使用 **Tiptap 3.x** 实现了类似语雀的所见即所得富文本编辑器，支持在编辑区直接插入 table、board、mind 等组件。

## 核心特性

### 1. 所见即所得编辑
- 实时预览格式化效果
- 支持 Markdown 快捷输入
- 工具栏提供常用格式操作

### 2. 组件插入功能
- **表格组件**：创建可编辑的在线表格
- **画板组件**：绘制图形和原型设计
- **思维导图**：构建层级化的思维结构

### 3. 扩展性
- 基于 ProseMirror 架构
- 支持自定义节点和标记
- 易于添加新的组件类型

## 快速开始

### 基本使用

```vue
<template>
  <TiptapEditor
    v-model="content"
    :kb-id="kbId"
    placeholder="开始写作..."
    @change="handleChange"
    @headings-change="handleHeadingsChange"
  />
</template>

<script setup>
import { ref } from 'vue'
import TiptapEditor from '@/components/note/TiptapEditor.vue'

const content = ref('')
const kbId = ref(1)

const handleChange = (html) => {
  console.log('内容变化:', html)
}

const handleHeadingsChange = (headings) => {
  console.log('大纲更新:', headings)
}
</script>
```

### API 方法

```javascript
// 获取编辑器实例
const editorRef = ref(null)

// 获取 HTML 内容
const html = editorRef.value.getValue()

// 设置内容
editorRef.value.setValue('<p>新内容</p>')

// 聚焦编辑器
editorRef.value.focus()

// 获取 JSON 格式
const json = editorRef.value.getJSON()

// 插入嵌入组件
editorRef.value.insertEmbed('table', noteId, '表格标题')
```

## 组件架构

### 文件结构

```
src/components/note/
├── TiptapEditor.vue      # 主编辑器组件
├── EmbedNode.js          # 嵌入节点扩展
└── EmbedComponent.vue    # 嵌入组件视图
```

### 核心组件说明

#### 1. TiptapEditor.vue
主编辑器组件，提供：
- 工具栏（格式化按钮）
- 编辑区域
- 组件插入功能

#### 2. EmbedNode.js
Tiptap 自定义节点扩展，定义：
- 节点属性（type, noteId, title）
- 解析和渲染逻辑
- 自定义命令（setEmbedComponent）

#### 3. EmbedComponent.vue
嵌入组件的可视化展示：
- 显示组件类型和标题
- 提供编辑和删除操作
- 点击跳转到对应笔记

## 插入组件流程

### 1. 用户点击插入按钮
```
用户点击"表格"按钮 → 调用 insertTable()
```

### 2. 创建笔记
```javascript
const res = await createNote({
  kbId: props.kbId,
  title: '新建表格',
  type: 'table',
  columns: [...],
  rows: [...]
})
```

### 3. 插入嵌入节点
```javascript
editor.value.chain().focus().setEmbedComponent({
  type: 'table',
  noteId: res.id,
  title: res.title
}).run()
```

### 4. 渲染组件卡片
EmbedComponent.vue 根据节点属性渲染可视化卡片

## 数据格式

### 嵌入节点 HTML 结构

```html
<div 
  data-type="embed-component" 
  data-type="table" 
  data-note-id="123" 
  data-title="表格标题"
  contenteditable="false"
>
  <!-- Vue 组件渲染的内容 -->
</div>
```

### 嵌入节点 JSON 结构

```json
{
  "type": "embedComponent",
  "attrs": {
    "type": "table",
    "noteId": 123,
    "title": "表格标题"
  }
}
```

## 快捷键支持

| 快捷键 | 功能 |
|--------|------|
| Ctrl+B | 加粗 |
| Ctrl+I | 斜体 |
| Ctrl+U | 下划线 |
| Ctrl+Z | 撤销 |
| Ctrl+Y | 重做 |
| Tab | 缩进 |

## 样式定制

### 工具栏样式

```css
.editor-toolbar {
  padding: 8px 16px;
  background: #f7f9fa;
  border-bottom: 1px solid #e8e8e8;
}
```

### 编辑器内容样式

```css
.ProseMirror {
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif;
  font-size: 16px;
  line-height: 1.8;
}
```

### 嵌入组件样式

```css
.embed-component {
  padding: 12px 16px;
  border: 2px solid #e8e8e8;
  border-radius: 8px;
  background: #fafafa;
}

.embed-component:hover {
  border-color: #25b864;
  background: #f0f9f4;
}
```

## 扩展新功能

### 添加新的组件类型

1. **在 EmbedComponent.vue 中添加图标和标签**

```javascript
const getTypeLabel = (type) => {
  const labels = {
    table: 'TABLE',
    board: 'BOARD',
    mind: 'MIND',
    chart: 'CHART'  // 新增图表类型
  }
  return labels[type] || type.toUpperCase()
}
```

2. **在 TiptapEditor.vue 中添加插入方法**

```javascript
const insertChart = async () => {
  const res = await createNote({
    kbId: props.kbId,
    title: '新建图表',
    type: 'chart',
    // ... 图表数据
  })
  
  editor.value.chain().focus().setEmbedComponent({
    type: 'chart',
    noteId: res.id,
    title: res.title
  }).run()
}
```

3. **添加工具栏按钮**

```vue
<el-button size="small" @click="insertChart">
  <el-icon><TrendCharts /></el-icon>
  图表
</el-button>
```

## 注意事项

### 1. 知识库 ID
插入组件时必须提供 `kbId`，否则无法创建关联的笔记。

### 2. 权限控制
确保当前用户对目标知识库有编辑权限。

### 3. 数据同步
嵌入组件只是引用，实际数据存储在对应的笔记中。

### 4. 删除处理
删除嵌入组件不会删除关联的笔记，需要单独处理。

## 测试页面

访问 `/test-tiptap` 查看完整的演示：

```bash
npm run dev
# 访问 http://localhost:5173/test-tiptap
```

## 常见问题

### Q: 如何获取纯文本内容？
A: 使用 `editor.value.getText()` 方法。

### Q: 如何插入图片？
A: 使用工具栏的图片按钮或拖拽上传。

### Q: 如何自定义主题？
A: 修改 `.prose` 相关 CSS 类或使用 Tailwind CSS 配置。

### Q: 支持协同编辑吗？
A: 需要集成 Tiptap Collaboration 扩展和 WebSocket 服务。

## 参考资源

- [Tiptap 官方文档](https://tiptap.dev/)
- [ProseMirror 文档](https://prosemirror.net/)
- [Element Plus 文档](https://element-plus.org/)
