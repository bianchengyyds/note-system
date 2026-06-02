<template>
  <div class="login-container" :style="{ backgroundImage: `linear-gradient(rgba(15,23,42,0.55), rgba(15,23,42,0.55)), url(${bgImg})` }">
    <div class="login-box">
      <div class="login-header">
        <h1>笔记系统</h1>
        <p>欢迎登录</p>
      </div>
      
      <el-form
        ref="loginFormRef"
        :model="loginForm"
        :rules="loginRules"
        class="login-form"
        size="large"
      >
        <el-form-item prop="name">
          <el-input
            v-model="loginForm.name"
            placeholder="请输入用户名"
            prefix-icon="User"
            clearable
          />
        </el-form-item>
        
        <el-form-item prop="password">
          <el-input
            v-model="loginForm.password"
            type="password"
            placeholder="请输入密码"
            prefix-icon="Lock"
            show-password
            @keyup.enter="handleLogin"
          />
        </el-form-item>
        
        <el-form-item>
          <el-button
            type="primary"
            class="login-button"
            :loading="loading"
            @click="handleLogin"
          >
            {{ loading ? '登录中...' : '登录' }}
          </el-button>
        </el-form-item>
        
        <div class="login-footer">
          <span>还没有账号？</span>
          <router-link to="/register" class="register-link">
            立即注册
          </router-link>
        </div>
      </el-form>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { login } from '@/api/auth'
import { setToken, setUserInfo } from '@/utils/auth'
import bgImg from '@/assets/images/bg1.jpg'

const router = useRouter()
const loginFormRef = ref(null)
const loading = ref(false)

const loginForm = reactive({
  name: '',
  password: ''
})

const loginRules = {
  name: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 2, max: 20, message: '用户名长度在 2 到 20 个字符', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度在 6 到 20 个字符', trigger: 'blur' }
  ]
}

const handleLogin = async () => {
  if (!loginFormRef.value) return

  // 使用 validate() 的 Promise 形式，避免回调陷阱
  try {
    await loginFormRef.value.validate()
  } catch {
    return // 验证不通过，直接返回
  }

  loading.value = true
  try {
    const data = await login(loginForm)

    // 确保 token 存在
    if (!data.token) {
      throw new Error('登录响应中没有 Token')
    }

    // 保存 token 和用户信息
    setToken(data.token)
    setUserInfo(data.userInfo)

    ElMessage.success('登录成功')

    // 根据角色确定跳转目标
    const redirect = router.currentRoute.value.query.redirect
    let targetPath = redirect || '/dashboard'
    if (data.userInfo?.role === 'ADMIN') {
      targetPath = redirect || '/admin/statistics'
    }

    // 跳转，并捕获可能的异常
    await router.push(targetPath)
    console.log('✅ 导航成功')
  } catch (error) {
    console.error('登录失败：', error)
    ElMessage.error(error.message || '登录失败')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
/* 保持你原来的样式不变 */
.login-container {
  height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  background-size: cover;
  background-position: center;
  background-repeat: no-repeat;
}

.login-box {
  width: 420px;
  padding: 40px;
  background: white;
  border-radius: 12px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1);
}

.login-header {
  text-align: center;
  margin-bottom: 40px;
}

.login-header h1 {
  font-size: 32px;
  color: #262626;
  margin-bottom: 8px;
  font-weight: 600;
}

.login-header p {
  font-size: 14px;
  color: #8c8c8c;
}

.login-form {
  margin-top: 20px;
}

.login-button {
  width: 100%;
  height: 44px;
  font-size: 16px;
}

.login-footer {
  text-align: center;
  margin-top: 16px;
  font-size: 14px;
  color: #595959;
}

.register-link {
  color: #25b864;
  margin-left: 8px;
  text-decoration: none;
}

.register-link:hover {
  color: #20a058;
}
</style>