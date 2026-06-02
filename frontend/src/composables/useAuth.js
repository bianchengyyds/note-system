// src/composables/useAuth.js
import { ref, computed } from 'vue'
import { login as loginApi, register as registerApi } from '@/api/auth'
import { setToken, getToken, removeToken, setUserInfo, getUserInfo } from '@/utils/auth'

export function useAuth() {
  const user = ref(getUserInfo() || null)
  const token = ref(getToken() || null)
  
  const isLoggedIn = computed(() => !!token.value)
  const isAdmin = computed(() => user.value?.role === 'ADMIN')
  
  async function login(loginForm) {
    const data = await loginApi(loginForm)
    token.value = data.token
    user.value = data.userInfo
    setToken(data.token)
    setUserInfo(data.userInfo)
    return data
  }
  
  async function register(registerForm) {
    return await registerApi(registerForm)
  }
  
  function logout() {
    token.value = null
    user.value = null
    removeToken()
    localStorage.removeItem('userInfo')
  }
  
  return {
    user,
    token,
    isLoggedIn,
    isAdmin,
    login,
    register,
    logout
  }
}
