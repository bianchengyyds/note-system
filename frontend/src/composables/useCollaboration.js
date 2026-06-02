import { ref, onMounted, onBeforeUnmount } from 'vue'
import * as Y from 'yjs'
import { WebsocketProvider } from 'y-websocket'
import { TiptapCollaboration } from '@tiptap/extension-collaboration'
import { TiptapCollaborationCursor } from '@tiptap/extension-collaboration-cursor'

/**
 * 协同编辑 Hook
 * 用于实现多人实时协作编辑
 * @param {string} docId - 文档ID
 * @param {object} userInfo - 当前用户信息
 */
export function useCollaboration(docId, userInfo = {}) {
  const ydoc = ref(null)
  const provider = ref(null)
  const isConnected = ref(false)
  const awarenessUsers = ref([])
  
  // 初始化协同编辑
  const initCollaboration = () => {
    // 创建 Yjs 文档
    ydoc.value = new Y.Doc()
    
    // 创建 WebSocket 提供者
    // 注意：需要配置后端的 WebSocket 服务器地址
    const wsUrl = import.meta.env.VITE_COLLAB_WS_URL || 'ws://localhost:1234'
    provider.value = new WebsocketProvider(wsUrl, `doc-${docId}`, ydoc.value, {
      connect: true
    })
    
    // 监听连接状态
    provider.value.on('status', (event) => {
      isConnected.value = event.status === 'connected'
      console.log('协同编辑连接状态:', event.status)
    })
    
    // 监听感知状态（其他用户）
    provider.value.awareness.on('change', () => {
      const states = Array.from(provider.value.awareness.getStates())
      awarenessUsers.value = states.map(([clientId, state]) => ({
        clientId,
        user: state.user,
        cursor: state.cursor
      }))
    })
    
    // 设置当前用户信息
    if (userInfo.name) {
      provider.value.awareness.setLocalStateField('user', {
        name: userInfo.name,
        color: getUserColor(userInfo.id || Date.now()),
        avatar: userInfo.avatar
      })
    }
    
    return {
      ydoc: ydoc.value,
      provider: provider.value
    }
  }
  
  // 销毁协同编辑
  const destroyCollaboration = () => {
    if (provider.value) {
      provider.value.destroy()
      provider.value = null
    }
    if (ydoc.value) {
      ydoc.value.destroy()
      ydoc.value = null
    }
    isConnected.value = false
    awarenessUsers.value = []
  }
  
  // 获取 Tiptap 协同扩展配置
  const getCollaborationExtensions = () => {
    if (!ydoc.value) return []
    
    return [
      TiptapCollaboration.configure({
        document: ydoc.value,
        field: 'content'
      }),
      TiptapCollaborationCursor.configure({
        provider: provider.value,
        user: {
          name: userInfo.name || '匿名用户',
          color: getUserColor(userInfo.id || Date.now())
        }
      })
    ]
  }
  
  // 根据用户ID生成颜色
  const getUserColor = (userId) => {
    const colors = [
      '#FF6B6B', '#4ECDC4', '#45B7D1', '#96CEB4',
      '#FFEAA7', '#DDA0DD', '#98D8C8', '#F7DC6F'
    ]
    return colors[userId % colors.length]
  }
  
  // 生命周期管理
  onMounted(() => {
    if (docId) {
      initCollaboration()
    }
  })
  
  onBeforeUnmount(() => {
    destroyCollaboration()
  })
  
  return {
    ydoc,
    provider,
    isConnected,
    awarenessUsers,
    initCollaboration,
    destroyCollaboration,
    getCollaborationExtensions
  }
}
