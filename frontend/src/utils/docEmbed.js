export const EMBED_REGEX = /\{\{embed\|(\w+)\|(\d+)\}\}/g

const EMBED_SLOT_DIV_RE = /<div[^>]*\bvditor-embed-slot\b[^>]*>\s*<\/div>/gi

/** 从 HTML 占位 div 中解析 type / id（属性顺序不固定） */
function parseEmbedSlotHtml(html) {
  const typeMatch = html.match(/data-embed-type="(\w+)"/i)
  const idMatch = html.match(/data-note-id="(\d+)"/i)
  if (typeMatch && idMatch) {
    return { embedType: typeMatch[1], noteId: idMatch[1] }
  }
  return null
}

export function buildEmbedMark(embedType, noteId) {
  return `{{embed|${embedType}|${noteId}}}`
}

/**
 * 统一文档内容为 {{embed|type|id}} 格式
 * 兼容历史误存的 vditor-embed-slot HTML
 */
export function normalizeDocContent(content) {
  if (!content) return ''
  return content.replace(EMBED_SLOT_DIV_RE, (slotHtml) => {
    const parsed = parseEmbedSlotHtml(slotHtml)
    return parsed ? buildEmbedMark(parsed.embedType, parsed.noteId) : slotHtml
  })
}

export function hasEmbedsInContent(content) {
  return extractEmbedRefs(content).length > 0
}

/** 从文档内容中提取所有嵌套组件引用 */
export function extractEmbedRefs(content) {
  const refs = []
  const seen = new Set()
  const normalized = normalizeDocContent(content || '')
  const re = new RegExp(EMBED_REGEX.source, 'g')
  let match
  while ((match = re.exec(normalized)) !== null) {
    const key = `${match[1]}_${match[2]}`
    if (!seen.has(key)) {
      seen.add(key)
      refs.push({ embedType: match[1], noteId: match[2] })
    }
  }
  return refs
}

let blockSeq = 0
function nextBlockId(prefix) {
  blockSeq += 1
  return `${prefix}_${blockSeq}`
}

export function parseContentToBlocks(content) {
  blockSeq = 0
  const normalized = normalizeDocContent(content)
  const blocks = []
  const regex = new RegExp(EMBED_REGEX.source, 'g')
  let lastIndex = 0
  let match

  while ((match = regex.exec(normalized)) !== null) {
    if (match.index > lastIndex) {
      const text = normalized.slice(lastIndex, match.index)
      if (text.trim() || blocks.length === 0) {
        blocks.push({ id: nextBlockId('text'), type: 'text', content: text })
      }
    }
    blocks.push({
      id: nextBlockId('embed'),
      type: 'embed',
      embedType: match[1],
      noteId: String(match[2])
    })
    lastIndex = regex.lastIndex
  }

  if (lastIndex < normalized.length) {
    blocks.push({ id: nextBlockId('text'), type: 'text', content: normalized.slice(lastIndex) })
  }

  if (!blocks.length) {
    blocks.push({ id: nextBlockId('text'), type: 'text', content: normalized || '' })
  }

  return blocks
}

export function blocksToContent(blocks) {
  return blocks
    .map((block) => {
      if (block.type === 'text') return block.content || ''
      return buildEmbedMark(block.embedType, block.noteId)
    })
    .join('')
}
