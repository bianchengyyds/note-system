import request from '@/utils/request' 
import axios from 'axios'

// 根据笔记类型创建笔记
export function createNote(data, type) {
  const endpointMap = {
    doc: '/note/doc',
    table: '/note/table',
    board: '/note/board',
    mind: '/note/mind'
  }
  const url = endpointMap[type] || '/note/doc'
  return request.post(url, data)
}

// 获取笔记详情
export function getNoteDetail(noteId) {
  return request.get(`/note/${noteId}`)
}

// 编辑笔记（自动生成新版本）
export function updateNote(noteId, data, type) {
  // 所有类型的编辑都使用 PUT /note/{noteId}，传入完整的类型数据
  return request.put(`/note/${noteId}`, data)
}

// 删除笔记（移入回收站）
export function deleteNote(noteId) {
  return request.delete(`/note/${noteId}`)
}

// 移动笔记
export function moveNote(noteId, data) {
  return request.put(`/note/${noteId}/move`, data)
}


// 获取回收站列表
export function getRecycleNotes(params) {
  return request.get('/note/recycle', { params })
}

// 恢复笔记
export function restoreNote(noteId) {
  return request.put(`/note/${noteId}/restore`)
}

// 彻底删除笔记
export function permanentDeleteNote(noteId) {
  return request.delete(`/note/${noteId}/permanent`)
}


// 导出笔记
export function exportNote(noteId, format) {
  return request.get(`/note/${noteId}/export/${format}`, {
    responseType: 'blob'   // 必须设置，否则会解析失败
  })
}

// 创建分享
export function shareNote(noteId, data) {
  return request.post(`/note/${noteId}/share`, data)
}

// 获取公开笔记（无需登录）
export function getPublicNote(shareKey, password) {
  return axios.get(`/public/note/${shareKey}`, { params: { password } }).then(res => res.data.data)
}

// 获取版本列表
export function getNoteVersions(noteId) {
  return request.get(`/note/${noteId}/versions`)
}

// 获取指定版本笔记详情
export function getVersionDetail(noteId, versionId) {
  return request.get(`/note/${noteId}/version/${versionId}`)
}

// 回滚到指定版本
export function rollbackNote(noteId, versionId) {
  return request.post(`/note/${noteId}/rollback/${versionId}`)
}


// 点赞/取消点赞
export function likeNote(noteId) {
  return request.post(`/note/${noteId}/like`)
}
export function unlikeNote(noteId) {
  return request.delete(`/note/${noteId}/like`)
}

// 收藏/取消收藏
export function favoriteNote(noteId) {
  return request.post(`/note/${noteId}/favorite`)
}
export function unfavoriteNote(noteId) {
  return request.delete(`/note/${noteId}/favorite`)
}

// 记录浏览
export function recordView(noteId) {
  return request.post(`/note/${noteId}/view`)
}