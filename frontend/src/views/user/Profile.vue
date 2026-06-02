d:\JavaWeb\NoteSystem\frontend\vue-note-system\src\views\user\Profile.vue
<template>
  <div class="profile-layout">
    <!-- 左侧导航栏 -->
    <div class="profile-sidebar">
      <!-- 返回按钮 -->
      <el-button text class="back-btn" @click="goBack">
        <el-icon><ArrowLeft /></el-icon> 返回工作台
      </el-button>
      
      <!-- 用户信息 -->
      <div class="user-info">
        <el-avatar :size="64" :src="userInfo?.avatar" class="user-avatar">
          <el-icon><User /></el-icon>
        </el-avatar>
        <span class="user-name">{{ userInfo?.name }}</span>
        <span class="user-email">{{ userInfo?.email }}</span>
      </div>
      
      <!-- 导航菜单 -->
      <div class="nav-menu">
        <div 
          class="nav-item" 
          :class="{ active: activeTab === 'statistics' }"
          @click="activeTab = 'statistics'"
        >
          <el-icon><BarChart /></el-icon> 数据统计
        </div>
        <div 
          class="nav-item" 
          :class="{ active: activeTab === 'profile' }"
          @click="activeTab = 'profile'"
        >
          <el-icon><User /></el-icon> 个人信息
        </div>
        <div 
          class="nav-item" 
          :class="{ active: activeTab === 'password' }"
          @click="activeTab = 'password'"
        >
          <el-icon><Key /></el-icon> 修改密码
        </div>
      </div>
    </div>
    
    <!-- 右侧内容区 -->
    <div class="profile-content">
      <!-- 数据统计 -->
      <div v-if="activeTab === 'statistics'" class="content-panel">
        <h2 class="panel-title">数据统计</h2>
        <div class="stat-cards">
          <div class="card">
            <div class="card-value">{{ stats.createdNotes ?? '-' }}</div>
            <div class="card-label">创作笔记</div>
          </div>
          <div class="card">
            <div class="card-value">{{ stats.createdKbs ?? '-' }}</div>
            <div class="card-label">知识库</div>
          </div>
          <div class="card">
            <div class="card-value">{{ stats.totalViews ?? '-' }}</div>
            <div class="card-label">总浏览量</div>
          </div>
          <div class="card">
            <div class="card-value">{{ stats.totalLikes ?? '-' }}</div>
            <div class="card-label">总点赞数</div>
          </div>
          <div class="card">
            <div class="card-value">{{ stats.totalWords ?? '-' }}</div>
            <div class="card-label">总字数</div>
          </div>
          <div class="card">
            <div class="card-value">{{ stats.activeDays ?? '-' }}</div>
            <div class="card-label">活跃天数</div>
          </div>
        </div>
      </div>
      
      <!-- 个人信息 -->
      <div v-if="activeTab === 'profile'" class="content-panel">
        <h2 class="panel-title">个人信息</h2>
        <el-form :model="profileForm" label-width="100px" class="profile-form">
          <el-form-item label="用户名">
            <el-input v-model="profileForm.name" />
          </el-form-item>
          <el-form-item label="邮箱">
            <el-input v-model="profileForm.email" placeholder="可留空" />
          </el-form-item>
          <el-form-item label="手机号">
            <el-input v-model="profileForm.phone" placeholder="可留空" />
          </el-form-item>
          <el-form-item label="性别">
            <el-radio-group v-model="profileForm.gender">
                <el-radio :label="0">未知</el-radio>
                <el-radio :label="1">男</el-radio>
                <el-radio :label="2">女</el-radio>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="头像URL">
            <el-input v-model="profileForm.avatar" placeholder="可留空" />
          </el-form-item>
          <el-form-item label="简介">
            <el-input v-model="profileForm.bio" type="textarea" :rows="3" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :loading="savingProfile" @click="saveProfile">
              保存资料
            </el-button>
          </el-form-item>
        </el-form>
      </div>
      
      <!-- 修改密码 -->
      <div v-if="activeTab === 'password'" class="content-panel">
        <h2 class="panel-title">修改密码</h2>
        <el-form :model="passwordForm" label-width="120px" class="password-form">
          <el-form-item label="旧密码">
            <el-input v-model="passwordForm.oldPassword" type="password" show-password />
          </el-form-item>
          <el-form-item label="新密码">
            <el-input v-model="passwordForm.newPassword" type="password" show-password />
          </el-form-item>
          <el-form-item label="确认新密码">
            <el-input v-model="passwordForm.confirmPassword" type="password" show-password />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :loading="changingPwd" @click="changePwd">
              修改密码
            </el-button>
          </el-form-item>
        </el-form>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getUserInfo } from '@/utils/auth'
import {
  getUserProfile,
  updateUserProfile,
  changePassword,
  getUserStatistics
} from '@/api/user'

const router = useRouter()
const activeTab = ref('statistics')
const userInfo = ref(getUserInfo())

// 统计数据
const stats = reactive({
  createdNotes: '-',
  createdKbs: '-',
  totalViews: '-',
  totalLikes: '-',
  totalWords: '-',
  activeDays: '-'
})

// 个人资料表单
const profileForm = reactive({
  name: '',
  email: '',
  phone: '',
  gender: 0,
  avatar: '',
  bio: ''
})
const savingProfile = ref(false)

// 密码表单
const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})
const changingPwd = ref(false)

// 返回工作台
function goBack() {
  router.push('/dashboard')
}

// 加载个人资料
async function loadProfile() {
  try {
    const res = await getUserProfile()
    Object.assign(profileForm, res)
  } catch (e) {
    ElMessage.error('获取个人资料失败')
  }
}

// 加载统计数据
async function loadStatistics() {
  try {
    const res = await getUserStatistics()
    Object.assign(stats, res)
  } catch (e) {
    console.error('获取统计数据失败', e)
  }
}

// 保存个人资料
async function saveProfile() {
  savingProfile.value = true
  try {
    await updateUserProfile({
      name: profileForm.name,
      email: profileForm.email,
      phone: profileForm.phone,
      gender: profileForm.gender,
      avatar: profileForm.avatar,
      bio: profileForm.bio
    })
    ElMessage.success('资料更新成功')
    // 更新本地缓存的用户信息
    const info = getUserInfo()
    info.name = profileForm.name
    info.avatar = profileForm.avatar
    localStorage.setItem('user_info', JSON.stringify(info))
    userInfo.value = info
  } catch (e) {
    ElMessage.error('更新失败')
  } finally {
    savingProfile.value = false
  }
}

// 修改密码
async function changePwd() {
  if (!passwordForm.oldPassword) {
    ElMessage.warning('请输入旧密码')
    return
  }
  if (!passwordForm.newPassword) {
    ElMessage.warning('请输入新密码')
    return
  }
  if (passwordForm.newPassword !== passwordForm.confirmPassword) {
    ElMessage.warning('两次输入的新密码不一致')
    return
  }
  changingPwd.value = true
  try {
    await changePassword({
      oldPassword: passwordForm.oldPassword,
      newPassword: passwordForm.newPassword
    })
    ElMessage.success('密码修改成功')
    passwordForm.oldPassword = ''
    passwordForm.newPassword = ''
    passwordForm.confirmPassword = ''
  } catch (e) {
    ElMessage.error(e?.response?.data?.msg || '密码修改失败')
  } finally {
    changingPwd.value = false
  }
}

onMounted(() => {
  loadProfile()
  loadStatistics()
})
</script>

<style scoped>
.profile-layout {
  display: flex;
  min-height: calc(100vh - 60px);
}

/* 左侧导航栏 */
.profile-sidebar {
  width: 280px;
  background: #fafbfc;
  border-right: 1px solid #e5e6eb;
  padding: 20px;
  display: flex;
  flex-direction: column;
  position: sticky;
  top: 60px;
  height: fit-content;
}

.back-btn {
  margin-bottom: 24px;
  padding: 8px 12px;
  font-size: 14px;
  justify-content: flex-start;
}

.user-info {
  text-align: center;
  padding: 20px 0;
  border-bottom: 1px solid #e5e6eb;
  margin-bottom: 16px;
}

.user-avatar {
  margin: 0 auto 12px;
}

.user-name {
  display: block;
  font-size: 18px;
  font-weight: 600;
  color: #1f2937;
  margin-bottom: 4px;
}

.user-email {
  display: block;
  font-size: 13px;
  color: #666;
}

.nav-menu {
  flex: 1;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 16px;
  font-size: 14px;
  color: #666;
  cursor: pointer;
  border-radius: 6px;
  margin-bottom: 4px;
  transition: all 0.2s;
}

.nav-item:hover {
  background: #e8eaed;
}

.nav-item.active {
  background: #e6f0ff;
  color: #1890ff;
  font-weight: 500;
}

/* 右侧内容区 */
.profile-content {
  flex: 1;
  padding: 32px 48px;
  background: #fff;
}

.content-panel {
  max-width: 600px;
}

.panel-title {
  font-size: 20px;
  font-weight: 600;
  margin-bottom: 24px;
  color: #1f2937;
}

/* 统计卡片 */
.stat-cards {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
}

.card {
  background: #f5f7fa;
  border-radius: 8px;
  padding: 24px;
  text-align: center;
}

.card-value {
  font-size: 32px;
  font-weight: 600;
  color: #1f2937;
}

.card-label {
  font-size: 13px;
  color: #666;
  margin-top: 8px;
}

/* 表单样式 */
.profile-form,
.password-form {
  background: #fafbfc;
  padding: 24px;
  border-radius: 8px;
}

.profile-form .el-form-item,
.password-form .el-form-item {
  margin-bottom: 20px;
}
</style>