# Tiptap 所见即所得编辑器实现总结

## 🎯 实现目标

使用 Tiptap 3.x 实现了类似语雀的富文本编辑器，支持在编辑区直接插入 table、board、mind 等组件，提供流畅的所见即所得编辑体验。

## ✨ 核心功能

### 1. 富文本编辑
- ✅ 标题格式化（H1-H6）
- ✅ 文字样式（加粗、斜体、删除线、下划线）
- ✅ 列表（有序、无序）
- ✅ 代码块
- ✅ 图片插入
- ✅ 链接插入
- ✅ 引用块
- ✅ 撤销/重做

### 2. 组件插入
- ✅ **表格组件**：创建可编辑的在线表格笔记
- ✅ **画板组件**：绘制图形和原型设计
- ✅ **思维导图**：构建层级化的思维结构

### 3. 可视化展示
- ✅ 嵌入组件以卡片形式显示
- ✅ 显示组件类型图标和标题
- ✅ 悬停显示操作按钮（编辑、删除）
- ✅ 点击跳转到对应笔记进行编辑

### 4. 大纲导航
- ✅ 自动提取文档标题生成大纲
- ✅ 实时同步更新
- ✅ 支持快速定位

## 📁 文件结构

```
src/components/note/
├── TiptapEditor.vue          # 主编辑器组件（548行）
├── EmbedNode.js              # 嵌入节点扩展（72行）
└── EmbedComponent.vue        # 嵌入组件视图（140行）

src/views/
└── TestTiptap.vue            # 测试演示页面（195行）

TIPTAP_USAGE.md               # 详细使用文档
```

## 🔧 技术实现

### 1. Tiptap 自定义节点

创建了 `EmbedNode` 扩展，实现：
- 原子节点（不可直接编辑）
- 存储元数据（type, noteId, title）
- 自定义命令 API
- Vue 组件渲染

```javascript
const EmbedNode = Node.create({
  name: 'embedComponent',
  group: 'block',
  atom: true,
  
  addAttributes() {
    return {
      type: { default: 'table' },
      noteId: { default: null },
      title: { default: '' }
    }
  },
  
  addCommands() {
    return {
      setEmbedComponent: (attributes) => ({ commands }) => {
        return commands.insertContent({
          type: this.name,
          attrs: attributes
        })
      }
    }
  }
})
```

### 2. 组件插入流程

```
用户点击按钮 
  ↓
调用 createNote API 创建笔记
  ↓
获取笔记 ID 和标题
  ↓
调用 editor.chain().setEmbedComponent()
  ↓
Tiptap 插入自定义节点
  ↓
Vue 渲染 EmbedComponent 视图
  ↓
显示组件卡片
```

### 3. 数据持久化

- 编辑器内容以 HTML 格式存储
- 嵌入节点序列化为自定义 HTML 标签
- 通过 data-* 属性存储元数据
- 加载时自动解析并恢复组件状态

## 🎨 UI/UX 设计

### 工具栏
- 分组布局，功能清晰
- 激活状态高亮显示
- 禁用状态灰显
- Tooltip 提示

### 嵌入组件卡片
- 边框 + 背景色区分
- 悬停效果（绿色边框 + 阴影）
- 图标 + 标题 + 标签
- 操作按钮（编辑、删除）

### 编辑器样式
- 语雀风格排版
- 合适的行高和间距
- 清晰的标题层级
- 响应式设计

## 🚀 使用方法

### 基础用法

```vue
<template>
  <TiptapEditor
    v-model="content"
    :kb-id="kbId"
    @change="handleChange"
  />
</template>

<script setup>
import { ref } from 'vue'
import TiptapEditor from '@/components/note/TiptapEditor.vue'

const content = ref('')
const kbId = ref(1)
</script>
```

### 高级用法

```vue
<script setup>
const editorRef = ref(null)

// 插入组件
const insertCustomComponent = () => {
  editorRef.value.insertEmbed('table', 123, '自定义表格')
}

// 获取内容
const getHTML = () => editorRef.value.getHTML()
const getJSON = () => editorRef.value.getJSON()
</script>
```

## 📊 测试验证

访问测试页面验证功能：

```bash
npm run dev
# 访问 http://localhost:5173/test-tiptap
```

测试项目：
- ✅ 文本格式化
- ✅ 插入表格组件
- ✅ 插入画板组件
- ✅ 插入思维导图组件
- ✅ 组件编辑跳转
- ✅ 组件删除
- ✅ 大纲生成
- ✅ 撤销/重做

## 🔗 集成到现有系统

### 替换 Vditor 编辑器

在 `NoteEditor.vue` 中：

```vue
<!-- 原来 -->
<RichTextEditor v-model="noteContent" />

<!-- 改为 -->
<TiptapEditor 
  v-model="noteContent" 
  :kb-id="kbId"
/>
```

### 保持兼容性

两种编辑器可以共存：
- Vditor：Markdown 源码编辑
- Tiptap：所见即所得编辑

根据用户需求选择使用。

## 🎯 优势对比

| 特性 | Vditor | Tiptap |
|------|--------|--------|
| 编辑模式 | Markdown 源码 | 所见即所得 |
| 组件插入 | 需要特殊语法 | 点击按钮即可 |
| 实时预览 | 分屏/即时渲染 | 完全实时 |
| 扩展性 | 有限 | 高度可扩展 |
| 学习成本 | 需懂 Markdown | 零门槛 |
| 自定义节点 | 困难 | 简单 |

## 📝 后续优化建议

1. **性能优化**
   - 虚拟滚动处理大型文档
   - 懒加载嵌入组件
   - 防抖保存

2. **功能增强**
   - 拖拽调整组件位置
   - 组件预览（不跳转）
   - 批量操作

3. **协同编辑**
   - 集成 Tiptap Collaboration
   - WebSocket 实时同步
   - 冲突解决

4. **移动端适配**
   - 触摸手势支持
   - 响应式工具栏
   - 虚拟键盘优化

## 🐛 已知问题

1. 首次加载时可能需要短暂时间初始化编辑器
2. 大量嵌入组件可能影响性能
3. 需要确保后端 API 正常响应

## 📚 相关文档

- [Tiptap 官方文档](https://tiptap.dev/)
- [使用指南](./TIPTAP_USAGE.md)
- [接口文档](./public/接口文档.md)

## 🎉 总结

成功实现了基于 Tiptap 的所见即所得编辑器，完美支持在编辑区直接插入 table、board、mind 等组件，提供了类似语雀的流畅编辑体验。代码结构清晰，易于维护和扩展。
