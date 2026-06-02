import { Node, VueNodeViewRenderer } from '@tiptap/vue-3'
import EmbedComponent from './EmbedComponent.vue'

export default Node.create({
  name: 'embed',

  group: 'block',

  addAttributes() {
    return {
      type: {
        default: 'table'
      },
      noteId: {
        default: null
      },
      title: {
        default: ''
      }
    }
  },

  parseHTML() {
    return [
      {
        tag: 'div[data-type="embed"]'
      }
    ]
  },

  renderHTML({ HTMLAttributes }) {
    return ['div', { 'data-type': 'embed', ...HTMLAttributes }, 0]
  },

  addNodeView() {
    return VueNodeViewRenderer(EmbedComponent, {
      // 传递编辑器实例给组件，用于判断是否可编辑
      props: (nodeView) => ({
        isEditable: nodeView.editor?.isEditable ?? true
      }),
      // 选择性阻止事件，确保嵌入组件的交互正常工作
      stopEvent: (event) => {
        // 检查事件目标是否在编辑区域内
        const target = event.target
        const editContainer = target.closest('.embed-edit-container, .embed-edit-block')
        
        // 如果正在编辑模式，允许所有事件传递到内部组件
        if (editContainer) {
          return false
        }
        
        // 在非编辑模式下，只阻止可能影响编辑器选择的事件
        const stopTypes = ['mousedown', 'mouseup', 'click']
        if (stopTypes.includes(event.type)) {
          return true
        }
        
        // 其他情况允许冒泡
        return false
      },
      // 忽略组件内部的 DOM 变动，防止 ProseMirror 尝试同步
      ignoreMutation: (mutation) => {
        // 如果是属性变化（如 contenteditable），始终忽略
        if (mutation.type === 'attributes') {
          return true
        }
        
        // 如果是子节点变化，检查是否在编辑容器内
        if (mutation.type === 'childList' || mutation.type === 'characterData') {
          const target = mutation.target
          const editContainer = target.closest?.('.embed-edit-container, .embed-edit-block')
          // 如果在编辑容器内，忽略该变动
          if (editContainer) {
            return true
          }
        }
        
        // 其他情况不忽略
        return false
      }
    })
  }
})
