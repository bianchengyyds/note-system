<template>
  <div class="sidebar-container">
    <!-- 顶部：笔记系统标题 + 用户信息 -->
    <div class="sidebar-top">
      <div class="logo-section">
        <div class="brand-mark">N</div>
        <div class="brand-info">
          <span class="logo-text">笔记系统</span>
          <span class="logo-subtitle">知识库工作台</span>
        </div>
      </div>
      
      <!-- 用户信息区域 -->
      <el-dropdown trigger="hover" class="user-section" @command="handleUserMenuCommand">
        <template #dropdown>
          <div class="user-menu-header">
            <el-avatar :size="48" :src="userInfo?.avatar" class="menu-avatar">
              <el-icon><User /></el-icon>
            </el-avatar>
            <span class="menu-username">{{ userInfo?.name }}</span>
          </div>
          <el-divider class="menu-divider" />
          <el-dropdown-menu>
            <!-- 后台管理（仅 ADMIN 角色可见） -->
            <el-dropdown-item v-if="userInfo?.role === 'ADMIN'" command="admin">
              <el-icon><Setting /></el-icon> 后台管理
            </el-dropdown-item>
            <el-dropdown-item command="profile">
              <el-icon><User /></el-icon> 个人中心
            </el-dropdown-item>
            <el-dropdown-item command="logout">
              <el-icon><component :is="'SignOut'" /></el-icon> 退出登录
            </el-dropdown-item>
          </el-dropdown-menu>
        </template>
        <div class="avatar-wrapper">
          <el-avatar :size="36" :src="userInfo?.avatar">
            <el-icon><User /></el-icon>
          </el-avatar>
        </div>
      </el-dropdown>
    </div>

    
    <!-- 搜索 + 新建 -->
    <div class="search-box">
      <el-input
        v-model="searchKeyword"
        placeholder="搜索笔记..."
        prefix-icon="Search"
        size="small"
        clearable
        @keyup.enter="doGlobalSearch"
      />
      <el-dropdown trigger="hover" @command="handleCommand">
        <el-button class="create-btn" size="small" circle>
          <el-icon :size="18"><Plus /></el-icon>
        </el-button>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item v-if="canEditCurrentKb" command="create-doc">
              <el-icon><Document /></el-icon> 新建文档
            </el-dropdown-item>
            <el-dropdown-item v-if="canEditCurrentKb" command="create-table">
              <el-icon><Grid /></el-icon> 新建表格
            </el-dropdown-item>
            <el-dropdown-item v-if="canEditCurrentKb" command="create-board">
              <el-icon><Picture /></el-icon> 新建画板
            </el-dropdown-item>
            <template v-if="!isInsideKb">
              <el-dropdown-item divided command="create-kb">
                <el-icon><FolderOpened /></el-icon> 新建知识库
              </el-dropdown-item>
            </template>
            <el-dropdown-item divided command="import">
              <el-icon><Upload /></el-icon> 导入
            </el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>

    <!-- 导航菜单（未进入知识库时显示） -->
    <div v-if="!isInsideKb" class="nav-section">
      <div class="section-title">主要功能</div>
      <div class="nav-buttons">
        <div class="nav-item" :class="{ active: currentPath.value === '/dashboard' }" @click="$router.push('/dashboard')">
          <el-icon><CaretRight /></el-icon> 开始
        </div>
        <div class="nav-item" :class="{ active: currentPath.value === '/favorites' }" @click="$router.push('/favorites')">
          <el-icon><Collection /></el-icon> 我的收藏
        </div>
        <div class="nav-item" :class="{ active: currentPath.value === '/browse-history' }" @click="$router.push('/browse-history')">
          <el-icon><Clock /></el-icon> 浏览历史
        </div>
        <div class="nav-item" :class="{ active: currentPath.value === '/trash' }" @click="$router.push('/trash')">
          <el-icon><Delete /></el-icon> 回收站
        </div>
        <div class="nav-item" :class="{ active: currentPath.value === '/invitations' }" @click="$router.push('/invitations')">
          <el-icon><ChatDotRound /></el-icon> 我的邀请
        </div>
      </div>
      
      <!-- 知识库操作区域 -->
      <div class="kb-section">
        <div class="kb-actions">
          <!-- 我的知识库按钮 -->
          <div class="kb-action-btn" @click="goToKbList">
            <el-icon><Folder /></el-icon> 我的知识库
          </div>
          <!-- 下拉列表按钮 -->
          <div class="kb-action-btn dropdown-toggle" @click="toggleKbDropdown">
            <span class="dropdown-arrow">{{ kbDropdownOpen ? '▼' : '▶' }}</span>
          </div>
        </div>
        
        <!-- 完整知识库面板（显示我创建的和协作的） -->
        <div class="kb-list-wrapper">
          <div v-show="showKbPanel" class="kb-panel">
            <!-- 我创建的知识库 -->
            <div class="kb-group">
              <div class="kb-group-title">我创建的</div>
              <div
                v-for="kb in myKbs"
                :key="kb.id"
                class="kb-item"
                :class="{ active: activeKbId === kb.id }"
                @click="selectKb(kb)"
              >
                <el-icon><Folder /></el-icon>
                <span class="item-text">{{ kb.name }}</span>
                <span class="item-count">{{ kb.docCount ?? 0 }}</span>
              </div>
              <el-empty v-if="!myKbs.length" description="暂无知识库" />
            </div>
            
            <!-- 我协作的知识库 -->
            <div class="kb-group">
              <div class="kb-group-title">我协作的</div>
              <div
                v-for="kb in collaborations"
                :key="kb.id"
                class="kb-item"
                :class="{ active: activeKbId === kb.id }"
                @click="selectKb(kb)"
              >
                <el-icon><Folder /></el-icon>
                <span class="item-text">{{ kb.name }}</span>
                <span class="kb-role">{{ getCollabRoleLabel(kb.role) }}</span>
                <span class="item-count">{{ kb.docCount ?? 0 }}</span>
              </div>
              <el-empty v-if="!collaborations.length" description="暂无协作知识库" />
            </div>
          </div>
          
          <!-- 快速下拉列表（仅显示我创建的） -->
          <div v-show="kbDropdownOpen" class="kb-scroll-container">
            <div class="kb-dropdown-content">
              <div
                v-for="kb in kbList"
                :key="kb.id"
                class="kb-item"
                :class="{ active: activeKbId === kb.id }"
                @click="selectKb(kb)"
              >
                <el-icon><Folder /></el-icon>
                <span class="item-text">{{ kb.name }}</span>
                <span class="item-count">{{ kb.docCount ?? 0 }}</span>
              </div>
              <el-empty v-if="!kbList.length" description="暂无知识库" />
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 知识库详情区域（进入知识库时显示笔记树） -->
    <div v-if="isInsideKb" class="sidebar-body">
      <div class="kb-header">
        <el-button text @click="goBack" class="back-btn">
          <el-icon><ArrowLeft /></el-icon>
          返回列表
        </el-button>
        <span class="kb-name">{{ currentKb?.name }}</span>
      </div>
      <NoteTree :kbId="currentKbId" :refreshTrigger="noteTreeRefreshTrigger" :canEdit="canEditCurrentKb" />
    </div>

    <CreateKbDialog v-model:visible="showCreateDialog" @created="fetchKbList" />

    <!-- 导入对话框 -->
    <el-dialog v-model="importDialogVisible" title="导入文件" width="550px">
      <el-form label-position="top">
        <el-form-item label="目标知识库" required>
          <el-select v-model="importKbId" placeholder="请选择目标知识库" style="width:100%" clearable>
            <el-option
              v-for="kb in kbList"
              :key="kb.id"
              :label="kb.name"
              :value="kb.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="选择文件">
          <el-upload
            drag
            multiple
            :auto-upload="false"
            :on-change="handleFilesChange"
            :file-list="importFiles"
            accept=".lakebook,.lake,.lakeboard,.laketable,.doc,.docx,.md,.txt,.ppt,.pptx,.xlsx,.xls,.csv,.pdf,.jpg,.png"
            :limit="10"
          >
            <el-icon class="el-icon--upload"><upload-filled /></el-icon>
            <div class="el-upload__text">
              将文件拖到此处，或<em>点击上传</em>
            </div>
          </el-upload>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="importDialogVisible = false">取消</el-button>
        <el-button
          type="primary"
          :loading="importing"
          @click="executeImport"
          :disabled="!importKbId || !importFiles.length"
        >
          开始导入
        </el-button>
      </template>
      <p v-if="importResult" class="import-result">
        成功 {{ importResult.successCount }} 个，失败 {{ importResult.failCount }} 个
      </p>
    </el-dialog>

  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getUserInfo, clearAuth } from '@/utils/auth'
import { getMyKbList, getCollaborations, importToKb, getKbDetail } from '@/api/kb'

import { createNote as createNoteApi } from '@/api/note'
import NoteTree from '@/components/note/NoteTree.vue'
import CreateKbDialog from '@/components/kb/CreateKbDialog.vue'

const route = useRoute()
const router = useRouter()
const currentPath = computed(() => route.path)
const userInfo = ref(getUserInfo())
const showCreateDialog = ref(false)
const searchKeyword = ref('')

const kbList = ref([])
const activeKbId = ref(null)

const importDialogVisible = ref(false)
const importKbId = ref(null)           // 目标知识库ID
const importFiles = ref([])            // 待导入文件列表
const importing = ref(false)
const importResult = ref(null)

const noteTreeRefreshTrigger = ref(0)

// 知识库下拉状态
const kbDropdownOpen = ref(false)

// 知识库面板状态（显示我创建的和协作的）
const showKbPanel = ref(false)
const myKbs = ref([])
const collaborations = ref([])

const collabRoleLabels = {
  OWNER: '拥有者',
  ADMIN: '可编辑',
  EDITOR: '可编辑',
  VIEWER: '可读'
}

function getCollabRoleLabel(role) {
  return collabRoleLabels[role] || '可读'
}

function toggleKbDropdown() {
  kbDropdownOpen.value = !kbDropdownOpen.value
  // 关闭面板
  showKbPanel.value = false
}

function toggleKbPanel() {
  showKbPanel.value = !showKbPanel.value
  // 如果打开面板，加载数据
  if (showKbPanel.value && (!myKbs.value.length || !collaborations.value.length)) {
    fetchKbLists()
  }
  // 关闭下拉
  kbDropdownOpen.value = false
}

async function fetchKbLists() {
  try {
    const [myRes, collabRes] = await Promise.all([
      getMyKbList(),
      getCollaborations()
    ])
    myKbs.value = myRes?.data?.records || myRes?.records || (Array.isArray(myRes) ? myRes : [])
    collaborations.value = collabRes?.data?.records || collabRes?.records || []
  } catch (e) {
    console.error('获取知识库列表失败', e)
  }
}


async function goToKbList() {
  // 跳转到知识库列表页面
  router.push('/kb')
}

// 用户菜单命令处理
function handleUserMenuCommand(command) {
  switch (command) {
    case 'admin':
      router.push('/admin/statistics')
      break
    case 'profile': 
      router.push('/profile')
      break
    case 'logout':
      handleLogout()
      break
  }
}


// ✅ 核心修复：同时支持 params.kbId 和 query.kbId
const currentKbId = computed(() => {
  const id = route.params.kbId || route.query.kbId
  return id ? Number(id) : null   // 强制转为数字
})
const isInsideKb = computed(() => !!currentKbId.value)

const currentKb = computed(() => {
  if (!currentKbId.value) {
    return null
  }
  // 先从我创建的知识库列表中查找
  if (kbList.value.length) {
    const found = kbList.value.find(kb => kb.id === currentKbId.value)
    if (found) {
      return found
    }
  }
  // 再从协作的知识库列表中查找
  if (collaborations.value.length) {
    const found = collaborations.value.find(kb => kb.id === currentKbId.value)
    if (found) {
      return found
    }
  }
  // 从缓存中查找
  if (kbNameCache.value[currentKbId.value]) {
    return { id: currentKbId.value, name: kbNameCache.value[currentKbId.value] }
  }
  // 如果都没有，返回一个临时对象
  return { id: currentKbId.value, name: `知识库 ${currentKbId.value}` }
})

// 判断当前用户是否有编辑权限（只读用户不能创建笔记）
const canEditCurrentKb = computed(() => {
  if (!currentKbId.value) return false
  // 我创建的知识库 → 可以编辑
  if (kbList.value.find(kb => kb.id === currentKbId.value)) return true
  // 协作的知识库 → 根据角色判断
  const collab = collaborations.value.find(kb => kb.id === currentKbId.value)
  if (collab) {
    return collab.role === 'EDITOR' || collab.role === 'OWNER'
  }
  // 默认不可编辑（安全起见）
  return false
})

// 知识库名称缓存（用于从热门等入口进入时自动获取名称）
const kbNameCache = ref({})

async function resolveKbName(kbId) {
  if (!kbId) return null
  // 先查缓存
  if (kbNameCache.value[kbId]) {
    return kbNameCache.value[kbId]
  }
  // 查列表
  const fromList = kbList.value.find(kb => kb.id === kbId) ||
                   collaborations.value.find(kb => kb.id === kbId)
  if (fromList) {
    kbNameCache.value[kbId] = fromList.name
    return fromList.name
  }
  // 请求详情
  try {
    const detail = await getKbDetail(kbId)
    if (detail?.name) {
      kbNameCache.value[kbId] = detail.name
      return detail.name
    }
  } catch {
    // 忽略
  }
  return null
}

const handleCommand = async (command) => {
  switch (command) {
    case 'create-doc':
      await createNoteInCurrentKb('doc')
      break
    case 'create-table':
      router.push(`/note/new?kbId=${currentKbId.value}&type=table`)
      break
    case 'create-board':
      router.push(`/note/new?kbId=${currentKbId.value}&type=board`)
      break
    case 'create-kb':
      showCreateDialog.value = true
      break
    case 'import':
      importKbId.value = currentKbId.value || null   // 如果当前在知识库内，默认选中该知识库，否则为空
      importFiles.value = []
      importResult.value = null
      importDialogVisible.value = true
      break
  }
}

function handleFilesChange(file, fileList) {
  importFiles.value = fileList
}

async function executeImport() {
  if (!importKbId.value || !importFiles.value.length) return
  importing.value = true
  try {
    const formData = new FormData()
    importFiles.value.forEach(f => {
      formData.append('files', f.raw)
    })
    const res = await importToKb(importKbId.value, formData)
    importResult.value = res
    ElMessage.success('导入完成')
    importFiles.value = []
    
    // 如果当前就在目标知识库内，刷新笔记树
    if (isInsideKb.value && currentKbId.value == importKbId.value) {
      noteTreeRefreshTrigger.value++
    } else {
      // 否则跳转到目标知识库，刷新整个页面
      router.push(`/kb/${importKbId.value}`)
    }
    importDialogVisible.value = false
  } catch (e) {
    ElMessage.error('导入失败')
  } finally {
    importing.value = false
  }
}

function doGlobalSearch() {
  if (!searchKeyword.value.trim()) return
  router.push(`/search?keyword=${encodeURIComponent(searchKeyword.value.trim())}`)
}

async function createNoteInCurrentKb(type) {
  if (!isInsideKb.value) {
    ElMessage.warning('请先进入一个知识库')
    return
  }
  router.push(`/note/new?kbId=${currentKbId.value}&type=${type}`)
}

function selectKb(kb) {
  localStorage.setItem('current_kb', JSON.stringify(kb))
  router.push(`/kb/${kb.id}`)
}

function goBack() {
  if (userInfo.value?.role === 'ADMIN') {
    router.push('/admin/kbs')
  } else {
    router.push('/dashboard')
  }
}

function handleLogout() {
  clearAuth()
  router.push('/login')
}

async function fetchKbList() {
  try {
    const [myRes, collabRes] = await Promise.all([
      getMyKbList(),
      getCollaborations()
    ])
    kbList.value = myRes?.data?.records || myRes?.records || (Array.isArray(myRes) ? myRes : [])
    collaborations.value = collabRes?.data?.records || collabRes?.records || []
    
    // 同步当前知识库名称到 localStorage
    if (isInsideKb.value && currentKbId.value) {
      const current = kbList.value.find(kb => kb.id == currentKbId.value) ||
                      collaborations.value.find(kb => kb.id == currentKbId.value)
      if (current) {
        localStorage.setItem('current_kb', JSON.stringify(current))
      }
    }
  } catch (e) {
    console.error('获取知识库列表失败', e)
  }
}

watch(
  () => route.path,
  async (newPath, oldPath) => {
    // 当从 /note/new 跳转到具体笔记页，且仍在知识库内时，触发笔记树刷新
    if (
      oldPath === '/note/new' &&
      newPath.startsWith('/note/') &&
      isInsideKb.value
    ) {
      noteTreeRefreshTrigger.value++
    }
    // 原本的：离开知识库时刷新列表（排除进入笔记页的情况）
    if (
      oldPath &&
      oldPath.startsWith('/kb/') &&
      !newPath.startsWith('/kb/') &&
      !newPath.startsWith('/note/')
    ) {
      fetchKbList()
    }
    // 进入知识库时，尝试解析名称（处理从热门、后台管理等入口进入的情况）
    if (newPath.startsWith('/kb/') && isInsideKb.value) {
      const name = await resolveKbName(currentKbId.value)
      if (name) {
        kbNameCache.value = { ...kbNameCache.value }
      }
    }
  }
)

function handleKbListUpdated() {
  fetchKbList()
}

onMounted(async () => {
  fetchKbList()
  window.addEventListener('kb-list-updated', handleKbListUpdated)
  // 页面加载时如果已经在知识库内，解析名称
  if (isInsideKb.value && currentKbId.value) {
    const name = await resolveKbName(currentKbId.value)
    if (name) {
      kbNameCache.value = { ...kbNameCache.value }
    }
  }
})

onUnmounted(() => {
  window.removeEventListener('kb-list-updated', handleKbListUpdated)
})
</script>


<style scoped>
.sidebar-container {
  display: flex;
  flex-direction: column;
  height: 100vh;
  width: 280px;
  background: #0f172a;
  color: #d1d5db;
  border-right: 1px solid rgba(148,163,184,0.12);
  overflow: hidden;
}

/* 顶部区域 */
.sidebar-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 18px 20px;
  border-bottom: 1px solid rgba(148,163,184,0.12);
}

.logo-section {
  display: flex;
  align-items: center;
  gap: 12px;
}

.brand-mark {
  width: 42px;
  height: 42px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #2563eb, #3b82f6);
  color: #ffffff;
  font-size: 18px;
  font-weight: 700;
}

.brand-info {
  display: flex;
  flex-direction: column;
}

.logo-text {
  font-size: 18px;
  font-weight: 700;
  color: #f8fafc;
}

.logo-subtitle {
  font-size: 12px;
  color: #94a3b8;
}

.user-section {
  display: flex;
  align-items: center;
  cursor: pointer;
}

.avatar-wrapper {
  transition: transform 0.2s ease;
}

.user-section:hover .avatar-wrapper {
  transform: scale(1.05);
}


/* 下拉用户菜单 */
.user-menu-header {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 16px 12px 8px;
}

.menu-avatar {
  margin-bottom: 10px;
}

.menu-username {
  font-size: 15px;
  font-weight: 600;
  color: #111827;
}

.menu-divider {
  margin: 10px 0;
}

.user-section .username {
  font-size: 14px;
  color: #cbd5e1;
}

/* 搜索框 */
.search-box {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 14px 18px;
  border-bottom: 1px solid rgba(148,163,184,0.12);
  background: rgba(255,255,255,0.04);
}

.search-box .el-input {
  flex: 1;
}

.create-btn {
  width: 36px;
  height: 36px;
  padding: 0;
  border-radius: 12px;
  border: 1px solid rgba(255,255,255,0.1);
  background: rgba(255,255,255,0.08);
  color: #ffffff;
}

.create-btn:hover {
  background: rgba(255,255,255,0.16);
}

/* 导航菜单 */
.nav-section {
  display: flex;
  flex-direction: column;
  padding: 14px 0 12px;
}

.section-title {
  padding: 0 20px 10px;
  font-size: 12px;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: #94a3b8;
}

.nav-buttons {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 0 16px 16px;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 16px;
  border-radius: 14px;
  font-size: 14px;
  color: #e5e7eb;
  cursor: pointer;
  transition: background 0.2s ease, color 0.2s ease;
}

.nav-item:hover {
  background: rgba(255,255,255,0.08);
  color: #ffffff;
}

.nav-item.active {
  background: rgba(59,130,246,0.24);
  color: #ffffff;
}

/* 知识库区域 */
.kb-section {
  margin-top: 10px;
  padding: 0 0 16px;
  border-top: 1px solid rgba(148,163,184,0.12);
  display: flex;
  flex-direction: column;
}

.kb-actions {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
  padding: 16px;
}

.kb-action-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 12px 14px;
  border-radius: 14px;
  font-size: 13px;
  color: #e5e7eb;
  background: rgba(255,255,255,0.05);
  cursor: pointer;
  transition: background 0.2s ease;
}

.kb-action-btn:hover {
  background: rgba(255,255,255,0.12);
}

.kb-list-wrapper {
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.kb-panel,
.kb-scroll-container {
  padding: 0 16px 12px;
  overflow-x: hidden;
}

.kb-panel {
  display: flex;
  flex-direction: column;
  gap: 12px;
  max-height: 260px;
  overflow-y: auto;
}

.kb-scroll-container {
  max-height: 260px;
  overflow-y: auto;
}

.kb-group {
  padding: 10px 0;
}

.kb-group-title {
  font-size: 12px;
  color: #94a3b8;
  padding-bottom: 6px;
}

.kb-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 14px;
  cursor: pointer;
  font-size: 14px;
  color: #e5e7eb;
  border-radius: 14px;
  transition: background 0.2s ease;
}

.kb-item:hover {
  background: rgba(255,255,255,0.08);
}

.kb-item.active {
  background: rgba(59,130,246,0.24);
  color: #ffffff;
}

.kb-role {
  font-size: 11px;
  color: #94a3b8;
  background: rgba(148,163,184,0.16);
  padding: 2px 8px;
  border-radius: 999px;
  margin-left: auto;
}

.item-text {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.item-count {
  font-size: 12px;
  color: #94a3b8;
}

/* 知识库详情区域 */
.sidebar-body {
  flex: 1;
  overflow-y: auto;
  border-top: 1px solid rgba(148,163,184,0.12);
}

.kb-header {
  padding: 16px 18px;
  border-bottom: 1px solid rgba(148,163,184,0.12);
}

.back-btn {
  margin-bottom: 8px;
  color: #e5e7eb;
}

.kb-name {
  font-weight: 700;
  font-size: 15px;
  display: block;
  padding: 4px 0;
  color: #f8fafc;
}
</style>