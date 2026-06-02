const TOKEN_KEY = 'access_token'
const USER_INFO_KEY = 'user_info'

export function getToken() {
  const token = localStorage.getItem(TOKEN_KEY)
  console.log('getToken 被调用，返回：', token)  // ✅ 调试日志
  return token
}

export function setToken(token) {
  console.log('setToken 被调用，设置：', token)  // ✅ 调试日志
  localStorage.setItem(TOKEN_KEY, token)
  
  // ✅ 验证是否保存成功
  const saved = localStorage.getItem(TOKEN_KEY)
  console.log('保存后验证：', saved)
}

export function removeToken() {
  localStorage.removeItem(TOKEN_KEY)
}

export function getUserInfo() {
  const info = localStorage.getItem(USER_INFO_KEY)
  return info ? JSON.parse(info) : null
}

export function setUserInfo(userInfo) {
  localStorage.setItem(USER_INFO_KEY, JSON.stringify(userInfo))
}

export function removeUserInfo() {
  localStorage.removeItem(USER_INFO_KEY)
}

export function clearAuth() {
  removeToken()
  removeUserInfo()
}
