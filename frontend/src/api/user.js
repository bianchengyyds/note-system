import request from '@/utils/request'

// --- 个人资料 ---
export function getUserProfile() {
  return request.get('/user/profile')
}

export function updateUserProfile(data) {
  return request.put('/user/profile', data)
}

// --- 修改密码 ---
export function changePassword(data) {
  return request.put('/user/password', data)
}

// --- 个人统计 ---
export function getUserStatistics() {
  return request.get('/user/statistics')
}


/**
 * 获取浏览历史
 */
export function getBrowseHistory(params) {
  return request.get('/user/browse-history', { params })
}


export function searchUsers(keyword) {
  return request.get('/user/search', { params: { keyword } })
}

export function getUserById(userId) {
  return request.get(`/user/${userId}`)
}

export function getUserFavorites(params) {
  return request.get('/user/favorites', { params })
}

