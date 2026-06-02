<template>
  <div class="register-container" :style="{ backgroundImage: `linear-gradient(rgba(15,23,42,0.5), rgba(15,23,42,0.5)), url(${bgImg})` }">
    <div class="register-box">
      <div class="register-header">
        <h1>笔记系统</h1>
        <p>创建新账号</p>
      </div>
      
      <el-form
        ref="registerFormRef"
        :model="registerForm"
        :rules="registerRules"
        class="register-form"
        size="large"
        label-width="80px"
      >
        <el-form-item label="用户名" prop="name">
          <el-input
            v-model="registerForm.name"
            placeholder="请输入用户名"
            prefix-icon="User"
            clearable
          />
        </el-form-item>
        
        <el-form-item label="密码" prop="password">
          <el-input
            v-model="registerForm.password"
            type="password"
            placeholder="请输入密码"
            prefix-icon="Lock"
            show-password
          />
        </el-form-item>
        
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input
            v-model="registerForm.confirmPassword"
            type="password"
            placeholder="请再次输入密码"
            prefix-icon="Lock"
            show-password
          />
        </el-form-item>
        
        <el-form-item label="邮箱" prop="email">
          <el-input
            v-model="registerForm.email"
            placeholder="请输入邮箱（可选）"
            prefix-icon="Message"
            clearable
          />
        </el-form-item>
        
        <el-form-item label="手机号" prop="phone">
          <el-input
            v-model="registerForm.phone"
            placeholder="请输入手机号（可选）"
            prefix-icon="Phone"
            clearable
          />
        </el-form-item>
        
        <!-- ✅ 使用 :label 而不是 :value -->
        <el-form-item label="性别">
          <el-radio-group v-model="registerForm.gender">
            <el-radio :label="0">保密</el-radio>
            <el-radio :label="1">男</el-radio>
            <el-radio :label="2">女</el-radio>
          </el-radio-group>
        </el-form-item>
        
        <el-form-item>
          <el-button
            type="primary"
            class="register-button"
            :loading="loading"
            @click="handleRegister"
          >
            {{ loading ? '注册中...' : '注册' }}
          </el-button>
        </el-form-item>
        
        <div class="register-footer">
          <span>已有账号？</span>
          <router-link to="/login" class="login-link">
            立即登录
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
import { register } from '@/api/auth'
import bgImg from '@/assets/images/bg1.jpg'

const router = useRouter()
const registerFormRef = ref(null)
const loading = ref(false)

// 注册表单数据
const registerForm = reactive({
  name: '',
  password: '',
  confirmPassword: '',
  email: '',
  phone: '',
  gender: 0  // 默认值为 0
})

// 自定义验证：确认密码
const validateConfirmPassword = (rule, value, callback) => {
  if (value !== registerForm.password) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

// 表单验证规则
const registerRules = {
  name: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 2, max: 20, message: '用户名长度在 2 到 20 个字符', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度在 6 到 20 个字符', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入密码', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' }
  ],
  email: [
    { type: 'email', message: '请输入正确的邮箱地址', trigger: 'blur' }
  ],
  phone: [
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' }
  ]
}

// 处理注册
const handleRegister = async () => {
  if (!registerFormRef.value) return
  
  await registerFormRef.value.validate(async (valid, fields) => {
    if (valid) {
      console.log('✅ 表单验证通过')
      
      loading.value = true
      try {
        const { confirmPassword, ...registerData } = registerForm
        
        // 确保 gender 是数字类型
        registerData.gender = Number(registerData.gender)
        
        console.log('注册数据：', registerData)
        
        await register(registerData)
        
        ElMessage.success('注册成功')
        
        setTimeout(() => {
          router.push('/login')
        }, 1500)
      } catch (error) {
        console.error('注册失败：', error)
      } finally {
        loading.value = false
      }
    } else {
      console.log('❌ 表单验证失败')
      console.log('失败的字段：', fields)
      
      if (fields) {
        const firstField = Object.keys(fields)[0]
        const firstError = fields[firstField][0].message
        ElMessage.error(firstError)
      }
    }
  })
}
</script>

<style scoped>
.register-container {
  min-height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 20px;
  background-size: cover;
  background-position: center;
  background-repeat: no-repeat;
}

.register-box {
  width: 500px;
  padding: 40px;
  background: white;
  border-radius: 12px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1);
}

.register-header {
  text-align: center;
  margin-bottom: 30px;
}

.register-header h1 {
  font-size: 32px;
  color: #262626;
  margin-bottom: 8px;
  font-weight: 600;
}

.register-header p {
  font-size: 14px;
  color: #8c8c8c;
}

.register-form {
  margin-top: 20px;
}

.register-button {
  width: 100%;
  height: 44px;
  font-size: 16px;
}

.register-footer {
  text-align: center;
  margin-top: 16px;
  font-size: 14px;
  color: #595959;
}

.login-link {
  color: #25b864;
  margin-left: 8px;
  text-decoration: none;
}

.login-link:hover {
  color: #20a058;
}
</style>
