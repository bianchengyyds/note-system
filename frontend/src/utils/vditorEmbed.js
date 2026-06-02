import { createApp } from 'vue'
import ElementPlus from 'element-plus'
import EmbedEditBlock from '@/components/note/EmbedEditBlock.vue'
import { buildEmbedMark, normalizeDocContent } from './docEmbed'

const EMBED_MARK_RE = /\{\{embed\|(\w+)\|(\d+)\}\}/g

export const embedApps = new Map()

/** 将 embed 标记转为 HTML 块 */
export function embedMarksToHtml(content) {
  const normalized = normalizeDocContent(content)
  return normalized.replace(EMBED_MARK_RE, (_, embedType, noteId) => {
    return `<div class="tiptap-embed-slot" contenteditable="false" data-embed-type="${embedType}" data-note-id="${noteId}"></div>`
  })
}

/** 创建嵌入插槽元素 */
export function createEmbedSlotElement(embedType, noteId, title = '') {
  const div = document.createElement('div')
  div.className = 'tiptap-embed-slot'
  div.contentEditable = 'false'
  div.dataset.embedType = embedType
  div.dataset.noteId = String(noteId)
  if (title) div.dataset.title = title
  return div
}

/** 在编辑器内容中替换 embed 标记为插槽元素 */
export function replaceEmbedMarksInEditor(root) {
  if (!root) return
  const walker = document.createTreeWalker(root, NodeFilter.SHOW_TEXT)
  const textNodes = []
  while (walker.nextNode()) textNodes.push(walker.currentNode)

  for (const textNode of textNodes) {
    if (textNode.parentElement?.closest('.tiptap-embed-slot')) continue
    const text = textNode.textContent || ''
    if (!text.includes('{{embed|')) continue

    const re = new RegExp(EMBED_MARK_RE.source, 'g')
    const parts = []
    let lastIndex = 0
    let match
    while ((match = re.exec(text)) !== null) {
      if (match.index > lastIndex) {
        parts.push({ type: 'text', value: text.slice(lastIndex, match.index) })
      }
      parts.push({ type: 'embed', embedType: match[1], noteId: match[2] })
      lastIndex = re.lastIndex
    }
    if (lastIndex < text.length) {
      parts.push({ type: 'text', value: text.slice(lastIndex) })
    }
    if (!parts.some((p) => p.type === 'embed')) continue

    const frag = document.createDocumentFragment()
    parts.forEach((p) => {
      if (p.type === 'text' && p.value) {
        frag.appendChild(document.createTextNode(p.value))
      } else if (p.type === 'embed') {
        frag.appendChild(createEmbedSlotElement(p.embedType, p.noteId))
      }
    })
    textNode.parentNode?.replaceChild(frag, textNode)
  }
}

/** 挂载嵌入组件到插槽 */
export function mountEmbedSlots(root) {
  if (!root) return
  root.querySelectorAll('.tiptap-embed-slot').forEach((slot) => {
    const embedType = slot.dataset.embedType
    const noteId = String(slot.dataset.noteId)
    if (!embedType || !noteId) return
    if (slot.dataset.mounted === '1' && slot.querySelector('.embed-edit-block')) return

    const existing = embedApps.get(noteId)
    if (existing) {
      try { existing.app.unmount() } catch (_) { /* ignore */ }
      embedApps.delete(noteId)
      slot.innerHTML = ''
      slot.dataset.mounted = '0'
    }

    const app = createApp(EmbedEditBlock, { embedType, noteId, inline: true })
    app.use(ElementPlus)
    const vm = app.mount(slot)
    slot.dataset.mounted = '1'
    embedApps.set(noteId, { app, vm })
  })
}

/** 从Tiptap编辑器提取文档内容 */
export function extractDocContentFromEditor(editor) {
  if (!editor) return ''

  const content = editor.getHTML()
  
  // 将嵌入插槽转换为标记
  const tempDiv = document.createElement('div')
  tempDiv.innerHTML = content
  
  const slots = tempDiv.querySelectorAll('.tiptap-embed-slot')
  slots.forEach((slot) => {
    const embedType = slot.dataset.embedType
    const noteId = slot.dataset.noteId
    if (embedType && noteId) {
      const textNode = document.createTextNode(buildEmbedMark(embedType, noteId))
      slot.replaceWith(textNode)
    }
  })

  return normalizeDocContent(tempDiv.innerHTML)
}

/** 在光标位置插入嵌入插槽 */
export function insertEmbedSlotAtCursor(editor, embedType, noteId, title = '') {
  if (!editor) return null

  const slot = createEmbedSlotElement(embedType, noteId, title)
  const slotHtml = slot.outerHTML
  
  editor.chain().focus().insertContent(slotHtml).run()
  
  // 延迟挂载组件
  setTimeout(() => {
    const root = editor.view.dom
    mountEmbedSlots(root)
  }, 100)
  
  return slot
}

/** 卸载所有嵌入应用 */
export function unmountAllEmbedApps() {
  embedApps.forEach(({ app }) => {
    try { app.unmount() } catch (_) { /* ignore */ }
  })
  embedApps.clear()
}

/** 静默保存所有嵌入组件 */
export async function saveAllEmbedsSilent() {
  for (const { vm } of embedApps.values()) {
    if (vm?.saveEmbed) await vm.saveEmbed({ silent: true })
  }
}

let hydrateTimer = null

/** 延迟执行嵌入组件的hydrate */
export function scheduleHydrateEmbeds(hydrateFn) {
  clearTimeout(hydrateTimer)
  hydrateTimer = setTimeout(() => hydrateFn(), 150)
}

/** 重新加载编辑器内容 */
export function reloadEditorContent(editor, content) {
  if (!editor) return
  unmountAllEmbedApps()
  editor.commands.setContent(embedMarksToHtml(normalizeDocContent(content)))
}

/** Hydrate嵌入组件 */
export async function hydrateEmbeds(editor) {
  if (!editor) return
  const root = editor.view.dom
  replaceEmbedMarksInEditor(root)
  await new Promise(resolve => setTimeout(resolve, 100))
  mountEmbedSlots(root)
}