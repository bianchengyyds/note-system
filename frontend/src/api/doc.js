import request from '@/utils/request'

/**
 * 创建文档
 * @param {Object} data - 文档数据
 * @param {number} data.kbId - 知识库ID
 * @param {string} data.title - 标题
 * @param {string} data.content - 内容
 * @param {string} data.type - 类型（doc/table/board/mind）
 * @param {number} data.parentId - 父节点ID
 */
export function createDoc(data) {
  return request({
    url: '/note/doc',  // ✅ 修正为 /note/doc
    method: 'post',
    data
  })
}

/**
 * 获取文档详情
 * @param {number} docId - 文档ID
 */
export function getDocDetail(docId) {
  return request({
    url: `/note/${docId}`,  // ✅ 修正为 /note/{docId}
    method: 'get'
  })
}

/**
 * 更新文档
 * @param {number} docId - 文档ID
 * @param {Object} data - 更新数据
 */
export function updateDoc(docId, data) {
  return request({
    url: `/note/${docId}`,  // ✅ 修正为 /note/{docId}
    method: 'put',
    data
  })
}

/**
 * 删除文档（软删除）
 * @param {number} docId - 文档ID
 */
export function deleteDoc(docId) {
  return request({
    url: `/note/${docId}`,  // ✅ 修正为 /note/{docId}
    method: 'delete'
  })
}

/**
 * 移动文档
 * @param {number} docId - 文档ID
 * @param {Object} data - 移动数据
 */
export function moveDoc(docId, data) {
  return request({
    url: `/note/${docId}/move`,
    method: 'put',
    data
  })
}

/**
 * 获取文档版本列表
 * @param {number} docId - 文档ID
 */
export function getDocVersions(docId) {
  return request({
    url: `/note/${docId}/versions`,
    method: 'get'
  })
}

/**
 * 在文档中插入嵌套组件（支持两种模式）
 * 模式1：嵌入已有组件 - 传入 noteId
 * 模式2：一步创建并嵌入 - 传入 newTable/newBoard/newMind
 * @param {number} docId - 文档ID
 * @param {Object} data - 嵌套组件数据
 * @param {string} data.embedType - 组件类型（table/board/mind）
 * @param {number} [data.noteId] - 已有笔记ID（模式1：嵌入已有组件时使用）
 * @param {Object} [data.newTable] - 新建表格数据（模式2：一步创建并嵌入时使用）
 * @param {Object} [data.newBoard] - 新建画板数据（模式2）
 * @param {Object} [data.newMind] - 新建思维导图数据（模式2）
 * @param {string} data.position - 插入位置
 */
export function createDocEmbed(docId, data) {
  return request({
    url: `/doc/${docId}/embed`,
    method: 'post',
    data
  })
}

/**
 * 一步创建并嵌入嵌套组件
 * 简化接口，直接传入新组件的必要数据
 * @param {number} docId - 文档ID
 * @param {Object} data - 创建并嵌入数据
 * @param {string} data.embedType - 组件类型（table/board/mind）
 * @param {string} data.title - 新组件笔记标题
 * @param {Object} [data.tableData] - 表格数据（embedType=table时必填）
 * @param {Array} [data.tableData.columns] - 表格列定义
 * @param {Array} [data.tableData.rows] - 表格行数据
 * @param {Object} [data.boardData] - 画板数据（embedType=board时必填）
 * @param {Array} [data.boardData.elements] - 画板元素
 * @param {string} [data.boardData.background] - 画板背景色
 * @param {Object} [data.mindData] - 思维导图数据（embedType=mind时必填）
 * @param {Array} [data.mindData.nodes] - 思维导图节点
 */
export function createAndEmbedDocEmbed(docId, data) {
  const payload = {
    embedType: data.embedType,
    position: 'cursor'
  }
  
  if (data.embedType === 'table') {
    payload.newTable = {
      title: data.title || '新建表格',
      columns: data.tableData?.columns || [],
      rows: data.tableData?.rows || []
    }
  } else if (data.embedType === 'board') {
    payload.newBoard = {
      title: data.title || '新建画板',
      elements: data.boardData?.elements || [],
      background: data.boardData?.background || '#ffffff'
    }
  } else if (data.embedType === 'mind') {
    payload.newMind = {
      title: data.title || '新建思维导图',
      mindData: data.mindData?.nodes || []
    }
  }
  
  return request({
    url: `/doc/${docId}/embed`,
    method: 'post',
    data: payload
  })
}

/**
 * 删除文档中的嵌套组件
 * @param {number} docId - 文档ID
 * @param {string|number} embedId - 嵌套组件ID
 */
export function deleteDocEmbed(docId, embedId) {
  return request({
    url: `/doc/${docId}/embed/${embedId}`,
    method: 'delete'
  })
}

/**
 * 获取文档中的嵌套组件列表（支持 includeData 参数）
 * @param {number} docId - 文档ID
 * @param {boolean} [includeData=false] - 是否同时返回嵌套组件的完整内容数据
 */
export function getDocEmbeds(docId, includeData = false) {
  return request({
    url: `/doc/${docId}/embeds`,
    method: 'get',
    params: { includeData }
  })
}

/** * 恢复文档版本
 * @param {number} docId - 文档ID
 * @param {number} versionId - 版本ID
 */
export function restoreDocVersion(docId, versionId) {
  return request({
    url: `/note/${docId}/versions/${versionId}/restore`,
    method: 'post'
  })
}

/**
 * 获取回收站文档列表
 */
export function getRecycleDocs() {
  return request({
    url: '/note/recycle',
    method: 'get'
  })
}

/**
 * 恢复回收站文档
 * @param {number} docId - 文档ID
 */
export function restoreDoc(docId) {
  return request({
    url: `/note/${docId}/restore`,
    method: 'put'
  })
}
