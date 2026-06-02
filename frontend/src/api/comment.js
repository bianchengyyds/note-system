import request from '@/utils/request'

// 发表评论
export function createComment(data) {
  return request.post('/comment', data)
}

// 获取评论列表
export function getNoteComments(noteId) {
  return request.get(`/note/${noteId}/comments`)
}

// 删除评论
export function deleteComment(commentId) {
  return request.delete(`/comment/${commentId}`)
}

// 点赞/取消点赞评论
export function likeComment(commentId) {
  return request.post(`/comment/${commentId}/like`)
}
export function unlikeComment(commentId) {
  return request.delete(`/comment/${commentId}/like`)
}