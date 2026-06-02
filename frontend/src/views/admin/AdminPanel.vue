<template>
  <div class="admin-layout">
    <!-- 左侧导航栏（笔记查看页面隐藏） -->
    <div v-if="!isNoteView" class="admin-sidebar">
      <div class="user-info">
        <el-avatar :size="64" :src="userInfo?.avatar" class="user-avatar">
          <el-icon><User /></el-icon>
        </el-avatar>
        <span class="user-name">{{ userInfo?.name }}</span>
        <span class="user-role">管理员</span>
      </div>
      
      <div class="nav-menu">
        <div 
          class="nav-item" 
          :class="{ active: currentRoute === 'AdminStatistics' }"
          @click="router.push('/admin/statistics')"
        >
          <el-icon><BarChart /></el-icon> 数据统计
        </div>
        <div 
          class="nav-item" 
          :class="{ active: currentRoute === 'AdminUsers' }"
          @click="router.push('/admin/users')"
        >
          <el-icon><Users /></el-icon> 用户管理
        </div>
        <div 
          class="nav-item" 
          :class="{ active: currentRoute === 'AdminNotes' }"
          @click="router.push('/admin/notes')"
        >
          <el-icon><Document /></el-icon> 笔记管理
        </div>
        <div 
          class="nav-item" 
          :class="{ active: currentRoute === 'AdminKbs' }"
          @click="router.push('/admin/kbs')"
        >
          <el-icon><FolderOpened /></el-icon> 知识库管理
        </div>
        <div 
          class="nav-item" 
          :class="{ active: currentRoute === 'AdminLogs' }"
          @click="router.push('/admin/logs')"
        >
          <el-icon><Clock /></el-icon> 操作日志
        </div>
      </div>

      <div class="sidebar-footer">
        <el-button text class="logout-btn" @click="handleLogout">
          <el-icon><SwitchButton /></el-icon> 退出登录
        </el-button>
      </div>
    </div>
    
    <!-- 右侧内容区 -->
    <div class="admin-content" :class="{ 'full-width': isNoteView }">
      <router-view />
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { getUserInfo, clearAuth } from '@/utils/auth'

const router = useRouter()
const route = useRoute()
const userInfo = getUserInfo()

// 根据当前路由名称高亮导航项
const currentRoute = computed(() => route.name)

// 判断是否是笔记查看页面
const isNoteView = computed(() => route.name === 'AdminNoteView')

function handleLogout() {
  clearAuth()
  router.push('/login')
}
</script>

<style scoped>
/* 保留原有样式不变 */
.admin-layout {
  display: flex;
  min-height: 100vh;
}

.admin-sidebar {
  width: 280px;
  background: #1f2937;
  padding: 20px;
  display: flex;
  flex-direction: column;
  position: sticky;
  top: 0;
  height: 100vh;
}

.user-info {
  text-align: center;
  padding: 20px 0;
  border-bottom: 1px solid #374151;
  margin-bottom: 16px;
}

.user-avatar {
  margin: 0 auto 12px;
  border: 2px solid #4b5563;
}

.user-name {
  display: block;
  font-size: 18px;
  font-weight: 600;
  color: #fff;
  margin-bottom: 4px;
}

.user-role {
  display: inline-block;
  font-size: 13px;
  color: #9ca3af;
  background: #374151;
  padding: 2px 8px;
  border-radius: 10px;
}

.nav-menu {
  flex: 1;
}

.sidebar-footer {
  padding-top: 16px;
  border-top: 1px solid #374151;
  margin-top: 8px;
}

.logout-btn {
  width: 100%;
  padding: 12px 16px;
  font-size: 14px;
  justify-content: center;
  color: #9ca3af;
  background: transparent;
  border: none;
  border-radius: 6px;
  transition: all 0.2s;
}

.logout-btn:hover {
  background: rgba(239, 68, 68, 0.15);
  color: #ef4444;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 16px;
  font-size: 14px;
  color: #9ca3af;
  cursor: pointer;
  border-radius: 6px;
  margin-bottom: 4px;
  transition: all 0.2s;
}

.nav-item:hover {
  background: rgba(255, 255, 255, 0.1);
  color: #fff;
}

.nav-item.active {
  background: #3b82f6;
  color: #fff;
  font-weight: 500;
}

.admin-content {
  flex: 1;
  padding: 32px 48px;
  background: #f9fafb;
  overflow-y: auto;
}

.admin-content.full-width {
  padding: 0;
  background: #fff;
}
</style>