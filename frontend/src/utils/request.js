import axios from 'axios'
import { ElMessage } from 'element-plus'
import { getToken, clearAuth } from '@/utils/auth'

// 错误消息去重
const errorMessages = new Set()
function showError(msg) {
  if (errorMessages.has(msg)) return
  errorMessages.add(msg)
  ElMessage.error(msg)
  setTimeout(() => errorMessages.delete(msg), 3000)
}

// 创建 axios 实例
const service = axios.create({
  baseURL: '/api',
  timeout: 15000,
  headers: {
    'Content-Type': 'application/json'
  }
})

// 请求拦截器
service.interceptors.request.use(
  config => {
    const token = getToken()
    
    // ✅ 添加详细日志
    console.log('=== 请求拦截器 ===')
    console.log('URL:', config.url)
    console.log('Token:', token ? token.substring(0, 20) + '...' : 'null')
    
    if (token) {
      config.headers['Authorization'] = `Bearer ${token}`
      console.log('已设置 Authorization header')
    } else {
      console.warn('⚠️ Token 为空，未设置 Authorization')
    }
    
    return config
  },
  error => {
    console.error('Request error:', error)
    return Promise.reject(error)
  }
)

// 响应拦截器
service.interceptors.response.use(
  response => {
    const res = response.data
    
    if (res.code !== 1) {
      showError(res.msg || '请求失败')
      return Promise.reject(new Error(res.msg || 'Error'))
    }
    
    return res.data
  },
  error => {
    console.error('Response error:', error)
    
    if (error.response) {
      switch (error.response.status) {
        case 401:
          showError('未授权，请重新登录')
          clearAuth()
          window.location.href = '/login'
          break
        case 403:
          showError(error.response.data?.msg || '权限不足')
          break
        case 404:
          showError('请求资源不存在')
          break
        case 500:
          showError('服务器错误')
          break
        default:
          showError(error.response.data?.msg || '网络错误')
      }
    } else {
      showError('网络连接失败')
    }
    
    return Promise.reject(error)
  }
)

export default service
