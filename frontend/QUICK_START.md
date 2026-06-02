# Tiptap 编辑器快速开始指南

## 🚀 5分钟快速上手

### 1. 查看演示

启动开发服务器并访问测试页面：

```bash
npm run dev
```

浏览器打开：`http://localhost:5173/test-tiptap`

### 2. 基本使用

在你的 Vue 组件中引入编辑器：

```vue
<template>
  <div>
    <TiptapEditor
      v-model="content"
      :kb-id="1"
      placeholder="开始写作..."
    />
  </div>
</template>

<script setup>
import { ref } from 'vue'
import TiptapEditor from '@/components/note/TiptapEditor.vue'

const content = ref('')
</script>
```

### 3. 插入组件

点击工具栏上的按钮即可插入：
- 📊 **表格** - 创建在线表格
- 🎨 **画板** - 绘制图形
- 🧠 **思维导图** - 构建思维结构

### 4. 获取内容

```javascript
// HTML 格式
const html = editorRef.value.getValue()

// JSON 格式
const json = editorRef.value.getJSON()
```

## 📖 完整示例

```vue
<template>
  <div class="editor-page">
    <!-- 标题 -->
    <input 
      v-model="title" 
      placeholder="文档标题"
      class="title-input"
    />
    
    <!-- 编辑器 -->
    <TiptapEditor
      ref="editorRef"
      v-model="content"
      :kb-id="kbId"
      @change="handleContentChange"
      @headings-change="handleHeadingsChange"
    />
    
    <!-- 操作按钮 -->
    <div class="actions">
      <button @click="saveDocument">保存</button>
      <button @click="clearContent">清空</button>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import TiptapEditor from '@/components/note/TiptapEditor.vue'
import { createDoc, updateDoc } from '@/api/doc'

const title = ref('我的文档')
const content = ref('')
const kbId = ref(1)
const editorRef = ref(null)

// 内容变化
const handleContentChange = (html) => {
  console.log('内容已更新')
}

// 大纲变化
const handleHeadingsChange = (headings) => {
  console.log('大纲:', headings)
}

// 保存文档
const saveDocument = async () => {
  try {
    await createDoc({
      kbId: kbId.value,
      title: title.value,
      content: content.value,
      type: 'doc'
    })
    ElMessage.success('保存成功')
  } catch (error) {
    ElMessage.error('保存失败')
  }
}

// 清空内容
const clearContent = () => {
  content.value = ''
  editorRef.value.setValue('')
}
</script>

<style scoped>
.editor-page {
  max-width: 900px;
  margin: 0 auto;
  padding: 20px;
}

.title-input {
  width: 100%;
  padding: 12px;
  font-size: 24px;
  font-weight: 600;
  border: none;
  outline: none;
  margin-bottom: 20px;
}

.actions {
  margin-top: 20px;
  display: flex;
  gap: 10px;
}
</style>
```

## 🎯 常用场景

### 场景1：新建文档

```javascript
// 创建空文档
const content = ref('')
const kbId = ref(1)

// 用户编辑后保存
await createDoc({
  kbId: kbId.value,
  title: '新文档',
  content: content.value
})
```

### 场景2：编辑现有文档

```javascript
// 加载文档
onMounted(async () => {
  const doc = await getDocDetail(docId)
  title.value = doc.title
  content.value = doc.content
})

// 保存修改
await updateDoc(docId, {
  title: title.value,
  content: content.value
})
```

### 场景3：插入特定组件

```javascript
// 程序化插入组件
const insertTableComponent = async () => {
  // 先创建表格笔记
  const tableNote = await createNote({
    kbId: kbId.value,
    type: 'table',
    title: '数据表格'
  })
  
  // 在编辑器中插入
  editorRef.value.insertEmbed(
    'table',
    tableNote.id,
    tableNote.title
  )
}
```

## 💡 技巧提示

### 1. 键盘快捷键

- `Ctrl/Cmd + B` - 加粗
- `Ctrl/Cmd + I` - 斜体
- `Ctrl/Cmd + U` - 下划线
- `Ctrl/Cmd + Z` - 撤销
- `Ctrl/Cmd + Y` - 重做

### 2. Markdown 快捷输入

直接输入 Markdown 语法会自动转换：
- `# 标题` → H1
- `## 标题` → H2
- `- 列表项` → 无序列表
- `1. 列表项` → 有序列表
- `> 引用` → 引用块
- ```code``` → 代码块

### 3. 拖拽图片

直接将图片拖入编辑器即可上传插入。

### 4. 选中文字格式化

选中文字后点击工具栏按钮或按快捷键进行格式化。

## 🔧 自定义配置

### 隐藏工具栏

```vue
<TiptapEditor
  v-model="content"
  :show-toolbar="false"
/>
```

### 自定义占位符

```vue
<TiptapEditor
  v-model="content"
  placeholder="在这里开始你的创作..."
/>
```

### 只读模式

```vue
<TiptapEditor
  v-model="content"
  :editable="false"
/>
```

## ❓ 常见问题

**Q: 如何获取纯文本？**
```javascript
const text = editorRef.value.editor.getText()
```

**Q: 如何设置初始内容？**
```javascript
content.value = '<p>初始内容</p>'
```

**Q: 如何聚焦编辑器？**
```javascript
editorRef.value.focus()
```

**Q: 支持哪些浏览器？**
现代浏览器（Chrome、Firefox、Safari、Edge）均支持。

## 📚 下一步

- 阅读 [完整使用文档](./TIPTAP_USAGE.md)
- 查看 [实现总结](./IMPLEMENTATION_SUMMARY.md)
- 参考 [Tiptap 官方文档](https://tiptap.dev/)

祝你使用愉快！🎉
