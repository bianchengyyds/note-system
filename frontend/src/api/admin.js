import request from '@/utils/request'

// 获取用户列表
export function getUsers(params) {
  return request.get('/admin/users', { params })
}

// 获取单个用户详情
export function getUserDetail(userId) {
  return request.get(`/admin/users/${userId}`)
}

// 更新用户
export function updateUser(userId, data) {
  return request.put(`/admin/users/${userId}`, data)
}

// 删除用户
export function deleteUser(userId) {
  return request.delete(`/admin/users/${userId}`)
}

// 重置密码
export function resetUserPassword(userId, data) {
  return request.put(`/admin/users/${userId}/password`, data)
}

// 管理员查询所有笔记（含已删除）
export function getAdminNotes(params) {
  return request.get('/admin/notes', { params })
}

// 管理员查看任意笔记详情
export function getAdminNoteDetail(noteId) {
  return request.get(`/admin/notes/${noteId}`)
}

// 管理员编辑任意笔记
export function updateAdminNote(noteId, data) {
  return request.put(`/admin/notes/${noteId}`, data)
}

// 管理员审核笔记
export function auditAdminNote(noteId, data) {
  return request.put(`/admin/notes/${noteId}/audit`, data)
}

// 管理员查询所有知识库
export function getAdminKbs(params) {
  return request.get('/admin/kbs', { params })
}

// 管理员查询操作日志
export function getAdminLogs(params) {
  return request.get('/admin/logs', { params })
}

// 管理员查看任意知识库详情
export function getAdminKbDetail(kbId) {
  return request.get(`/admin/kbs/${kbId}`)
}

// 管理员删除知识库
export function deleteAdminKb(kbId) {
  return request.delete(`/admin/kbs/${kbId}`)
}