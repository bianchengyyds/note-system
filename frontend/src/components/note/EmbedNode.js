import { Node, mergeAttributes } from '@tiptap/core'
import { VueNodeViewRenderer } from '@tiptap/vue-3'
import EmbedComponent from './EmbedComponent.vue'

/**
 * 嵌入组件节点扩展
 * 用于在 Tiptap 编辑器中插入 table、board、mind 等组件
 */
const EmbedNode = Node.create({
  name: 'embedComponent',
  
  group: 'block',
  
  atom: true, // 原子节点，不可编辑
  
  addAttributes() {
    return {
      type: {
        default: 'table', // table | board | mind
        parseHTML: element => element.getAttribute('data-embed-type') || element.getAttribute('data-type'),
        renderHTML: attributes => ({
          'data-embed-type': attributes.type
        })
      },
      noteId: {
        default: null,
        parseHTML: element => element.getAttribute('data-note-id'),
        renderHTML: attributes => ({
          'data-note-id': attributes.noteId
        })
      },
      title: {
        default: '',
        parseHTML: element => element.getAttribute('data-title'),
        renderHTML: attributes => ({
          'data-title': attributes.title
        })
      }
    }
  },
  
  parseHTML() {
    return [
      {
        tag: 'div[data-embed-type]'
      },
      {
        tag: 'div[data-type="embed-component"]'
      },
      {
        tag: 'div.tiptap-embed-slot'
      }
    ]
  },
  
  renderHTML({ HTMLAttributes }) {
    return ['div', mergeAttributes(HTMLAttributes, { 
      'class': 'tiptap-embed-slot',
      'contenteditable': 'false'
    })]
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
  },
  
  addNodeView() {
    return VueNodeViewRenderer(EmbedComponent, {
      // 停止事件冒泡到 ProseMirror，确保组件内部的交互（点击单元格、绘图等）由组件自身处理
      // 仅在编辑器可编辑时允许组件内部交互
      stopEvent: (params) => {
        const { editor, event } = params
        const target = event?.target

        if (!editor) return true
        if (editor.isEditable === false) return false
        
        // 彻底放行画板、表格、思维导图的所有内部交互
        return true
      },
      // 忽略组件内部的 DOM 变动，防止 ProseMirror 尝试同步
      ignoreMutation: () => true
    })
  }
})

export default EmbedNode
