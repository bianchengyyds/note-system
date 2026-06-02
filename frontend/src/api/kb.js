import request from '@/utils/request' 

// 获取我的知识库列表
export function getMyKbList() {
  return request.get('/kb/my')
}

// 获取我协作的知识库列表
export function getCollaborations(params) {
  return request.get('/kb/collaborations', { params })
}

// 获取知识库详情（含笔记树）
export function getKbDetail(kbId) {
  return request.get(`/kb/${kbId}`)
}

// 创建知识库
export function createKb(data) {
  return request.post('/kb', data)
}

// 更新知识库
export function updateKb(kbId, data) {
  return request.put(`/kb/${kbId}`, data)
}

// 删除知识库（软删除）
export function deleteKb(kbId) {
  return request.delete(`/kb/${kbId}`)
}

// 获取回收站中的知识库
export function getRecycleKbs(params) {
  return request.get('/kb/recycle', { params })   // 假设后端提供
}

// 恢复知识库
export function restoreKb(kbId) {
  return request.put(`/kb/${kbId}/restore`)
}

// 彻底删除知识库
export function permanentDeleteKb(kbId) {
  return request.delete(`/kb/${kbId}/permanent`)
}


// 添加成员
export function addKbMember(kbId, data) {
  return request.post(`/kb/${kbId}/members`, data)
}

// 修改成员角色
export function updateKbMemberRole(kbId, userId, data) {
  return request.put(`/kb/${kbId}/members/${userId}`, data)
}

// 移除成员
export function removeKbMember(kbId, userId) {
  return request.delete(`/kb/${kbId}/members/${userId}`)
}

// 导出知识库
export function exportKb(kbId) {
  return request.get(`/kb/${kbId}/export`, { responseType: 'blob' })
}

// 导入文件到知识库
export function importToKb(kbId, formData) {
  return request.post(`/kb/${kbId}/import`, formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

// 2.6 发送知识库邀请
export function sendKbInvitation(kbId, data) {
  return request.post(`/kb/${kbId}/invitations`, data)
}

// 2.7 接受知识库邀请
export function acceptKbInvitation(invitationId) {
  return request.put(`/kb/invitations/${invitationId}/accept`)
}

// 2.8 拒绝知识库邀请
export function rejectKbInvitation(invitationId) {
  return request.put(`/kb/invitations/${invitationId}/reject`)
}

// 2.9 获取待处理邀请列表
export function getPendingInvitations() {
  return request.get('/kb/invitations/pending')
}

// 2.10 获取知识库邀请列表
export function getKbInvitations(kbId) {
  return request.get(`/kb/${kbId}/invitations`)
}