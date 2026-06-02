<template>
  <div class="kb-detail-page">
    <!-- 顶部信息区 -->
    <div class="kb-header">
      <div class="kb-info">
        <h2>{{ kbInfo?.name }}</h2>
        <p class="description">{{ kbInfo?.description }}</p>
        <div class="meta">
          <span>创建于 {{ formatDate(kbInfo?.createdAt) }}</span>
          <span v-if="kbInfo?.members?.length">
            · {{ kbInfo.members.length }} 位成员
          </span>
        </div>
      </div>
      <div class="header-actions">
        <el-button v-if="isAdmin" @click="router.back()">返回</el-button>
        <el-button v-if="!isAdmin" @click="openSettings">知识库设置</el-button>
      </div>
    </div>

    <!-- 最近笔记列表 -->
    <div class="recent-notes" v-if="recentNotes.length">
      <h3>最近更新的笔记</h3>
      <div
        v-for="note in recentNotes"
        :key="note.id"
        class="note-item"
        @click="openNote(note.id, note.type)"
      >
        <el-icon><Document /></el-icon>
        <span>{{ note.title }}</span>
        <span class="update-time">{{ formatDate(note.updatedAt) }}</span>
      </div>
    </div>

    <!-- ====== 编辑知识库对话框 ====== -->
    <!-- 编辑知识库对话框 -->
    <el-dialog
      v-model="editDialogVisible"
      title="知识库设置"
      width="600px"
      @close="resetEditForm"
    >
      <el-tabs v-model="activeSettingTab" @tab-click="handleTabClick">
        <!-- 基本信息 -->
        <el-tab-pane label="基本信息" name="basic">
          <el-form ref="editFormRef" :model="editForm" :rules="editRules" label-position="top">
            <el-form-item label="名称" prop="name">
              <el-input v-model="editForm.name" maxlength="30" show-word-limit />
            </el-form-item>
            <el-form-item label="描述" prop="description">
              <el-input v-model="editForm.description" type="textarea" maxlength="200" show-word-limit :rows="3" />
            </el-form-item>
            <el-form-item label="封面 URL">
              <el-input v-model="editForm.coverUrl" placeholder="可选" />
            </el-form-item>
            <el-form-item label="权限">
              <el-radio-group v-model="editForm.isPublic">
                <el-radio :label="1">公开</el-radio>
                <el-radio :label="0">私有</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-form>
        </el-tab-pane>

        <!-- 成员管理 -->
        <el-tab-pane label="成员管理" name="members">
          <div class="member-actions">
            <el-button type="primary" size="small" @click="showInvitationDialog">邀请成员</el-button>
          </div>
          <div class="member-list">
            <div class="member-item" v-for="m in kbInfo?.members" :key="m.id || m.userId">
              <span class="member-name">{{ m.name }}</span>
              <!-- OWNER 角色只读展示 -->
              <span v-if="m.role === 'OWNER'" class="owner-role">拥有者</span>
              <!-- 非 OWNER 才显示角色编辑和移除按钮 -->
              <template v-else>
                <el-select
                    :model-value="m.role"
                    size="small"
                    style="width:100px"
                    @change="(role) => changeMemberRole(m, role)"
                >
                  <el-option label="编辑者" value="EDITOR" />
                  <el-option label="查看者" value="VIEWER" />
                </el-select>
                <el-button size="small" type="danger" @click="removeMember(m)">移除</el-button>
              </template>
            </div>
            <el-empty v-if="!kbInfo?.members?.length" description="暂无成员" />
          </div>
          <div class="add-member">
            <el-select
                v-model="selectedUserId"
                filterable
                remote
                reserve-keyword
                placeholder="搜索用户名"
                :remote-method="searchUsersRemote"
                :loading="userSearchLoading"
                style="width: 200px"
                size="small"
            >
                <el-option
                    v-for="user in userOptions"
                    :key="user.id"
                    :label="user.name"
                    :value="user.id"
                />
            </el-select>
            <el-select v-model="newMemberRole" size="small" style="width:100px">
                <el-option label="编辑者" value="EDITOR" />
                <el-option label="查看者" value="VIEWER" />
            </el-select>
            <el-button size="small" type="primary" @click="addMember">邀请</el-button>
        </div>
        </el-tab-pane>

        <!-- 邀请列表 -->
        <el-tab-pane label="邀请列表" name="invitations">
          <div class="invitation-section">
            <h4>知识库邀请</h4>
            <el-table :data="kbInvitations" style="width: 100%">
              <el-table-column label="被邀请人" width="120">
                <template #default="{ row }">
                  {{ getInviteeName(row) }}
                </template>
              </el-table-column>
              <el-table-column label="角色" width="100">
                <template #default="{ row }">
                  <el-tag :type="getRoleTagType(row.role)" size="small">
                    {{ getRoleLabel(row.role) }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column label="状态" width="100">
                <template #default="{ row }">
                  <el-tag :type="getStatusTagType(row.status)" size="small">
                    {{ getStatusLabel(row.status) }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="createdAt" label="发送时间" width="180">
                <template #default="{ row }">
                  {{ formatDate(row.createdAt) }}
                </template>
              </el-table-column>
              <el-table-column prop="expiredAt" label="过期时间" width="180">
                <template #default="{ row }">
                  {{ formatDate(row.expiredAt) }}
                </template>
              </el-table-column>
            </el-table>
            <el-empty v-if="!kbInvitations.length" description="暂无邀请记录" />
          </div>
        </el-tab-pane>

        <!-- 导出 -->
        <el-tab-pane label="导出" name="export">
          <div class="export-section">
            <h4>导出知识库</h4>
            <p class="desc">将知识库和所有笔记导出为 .lakebook 文件</p>
            <el-button type="primary" @click="exportKnowledgeBase">导出</el-button>
          </div>
        </el-tab-pane>

        <!-- 危险操作 -->
        <el-tab-pane label="危险操作" name="danger">
          <el-popconfirm title="确定要删除该知识库吗？" @confirm="deleteKb">
            <template #reference>
              <el-button type="danger" plain>删除知识库</el-button>
            </template>
          </el-popconfirm>
        </el-tab-pane>


      </el-tabs>

      <template #footer>
        <el-button @click="editDialogVisible = false">关闭</el-button>
        <el-button v-if="activeSettingTab === 'basic' && !isAdmin" type="primary" :loading="saving" @click="saveEdit">保存</el-button>
      </template>


    </el-dialog>

    <!-- 邀请成员对话框 -->
    <InvitationDialog
      v-model="invitationDialogVisible"
      :kb-id="kbId"
      @success="onInvitationSuccess"
    />
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getKbDetail, updateKb, deleteKb as deleteKbApi, addKbMember, updateKbMemberRole, removeKbMember, getKbInvitations } from '@/api/kb'
import { searchUsers } from '@/api/user'
import { exportKb, importToKb } from '@/api/kb'
import InvitationDialog from '@/components/kb/InvitationDialog.vue'
import { getUserDetail } from '@/api/admin'
import { getUserInfo } from '@/utils/auth'

const importFiles = ref([])
const importing = ref(false)
const importResult = ref(null)

// 判断是否是管理员
const isAdmin = computed(() => {
  const info = getUserInfo()
  return info?.role === 'ADMIN'
})


const route = useRoute()
const router = useRouter()

const kbId = computed(() => route.params.kbId)

const kbInfo = ref(null)
const recentNotes = ref([])

const selectedUserId = ref(null)
const userOptions = ref([])
const userSearchLoading = ref(false)

// 邀请相关
const invitationDialogVisible = ref(false)
const kbInvitations = ref([])
const inviteeNameCache = ref({})

const roleLabels = {
  OWNER: '拥有者',
  ADMIN: '管理员',
  EDITOR: '编辑者',
  VIEWER: '查看者'
}

const roleTagTypes = {
  OWNER: 'danger',
  ADMIN: 'warning',
  EDITOR: 'primary',
  VIEWER: 'info'
}

const statusLabels = {
  PENDING: '待处理',
  ACCEPTED: '已接受',
  REJECTED: '已拒绝',
  EXPIRED: '已过期'
}

const statusTagTypes = {
  PENDING: 'warning',
  ACCEPTED: 'success',
  REJECTED: 'info',
  EXPIRED: 'danger'
}

function getRoleLabel(role) {
  return roleLabels[role] || role
}

function getRoleTagType(role) {
  return roleTagTypes[role] || 'info'
}

function getStatusLabel(status) {
  return statusLabels[status] || status
}

function getStatusTagType(status) {
  return statusTagTypes[status] || 'info'
}

function showInvitationDialog() {
  invitationDialogVisible.value = true
}

function handleTabClick(tab) {
  if (tab.props?.name === 'invitations' || tab.paneName === 'invitations') {
    loadInvitations()
  }
}

async function loadInvitations() {
  try {
    const data = await getKbInvitations(kbId.value)
    kbInvitations.value = Array.isArray(data) ? data : []
    await loadInviteeNames()
  } catch (e) {
    ElMessage.error('加载邀请列表失败')
  }
}

async function loadInviteeNames() {
  const ids = [...new Set(kbInvitations.value.map(inv => inv.inviteeId).filter(Boolean))]
  for (const id of ids) {
    if (!inviteeNameCache.value[id]) {
      try {
        const user = await getUserDetail(id)
        inviteeNameCache.value[id] = user?.name || `用户${id}`
      } catch {
        inviteeNameCache.value[id] = `用户${id}`
      }
    }
  }
}

function getInviteeName(inv) {
  return inviteeNameCache.value[inv.inviteeId] || inv.inviteeName || `用户${inv.inviteeId}`
}

function onInvitationSuccess() {
  loadInvitations()
  loadKbDetail()
}


// 导出知识库
async function exportKnowledgeBase() {
  try {
    // 1. 获取 token
    const token = localStorage.getItem('access_token') // 根据你的存储键调整
    const url = `/api/kb/${kbId.value}/export`         // 确保路径匹配后端

    // 2. 使用 fetch 获取二进制流
    const response = await fetch(url, {
      headers: { Authorization: `Bearer ${token}` }
    })

    if (!response.ok) {
      throw new Error(`导出失败：${response.status}`)
    }

    // 3. 创建 blob 并下载
    const blob = await response.blob()
    const downloadUrl = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = downloadUrl
    a.download = `${kbInfo.value.name || 'knowledge_base'}.lakebook`
    document.body.appendChild(a)
    a.click()
    document.body.removeChild(a)
    window.URL.revokeObjectURL(downloadUrl)

    ElMessage.success('导出成功')
  } catch (e) {
    console.error('导出失败:', e)
    ElMessage.error('导出失败')
  }
}

// 文件选择变化
function handleFilesChange(file, fileList) {
  importFiles.value = fileList
}

// 执行导入
async function importFilesToKb() {
  if (!importFiles.value.length) return
  importing.value = true
  try {
    const formData = new FormData()
    importFiles.value.forEach(f => {
      formData.append('files', f.raw)
    })
    const res = await importToKb(kbId.value, formData)
    importResult.value = res
    ElMessage.success('导入完成')
    importFiles.value = []
  } catch (e) {
    ElMessage.error('导入失败')
  } finally {
    importing.value = false
  }
}

// 远程搜索用户
async function searchUsersRemote(keyword) {
  if (!keyword) {
    userOptions.value = []
    return
  }
  userSearchLoading.value = true
  try {
    const res = await searchUsers(keyword)
    userOptions.value = Array.isArray(res) ? res : (res.records || [])
  } catch (e) {
    userOptions.value = []
  } finally {
    userSearchLoading.value = false
  }
}

// 编辑知识库相关
const editDialogVisible = ref(false)
const saving = ref(false)
const editFormRef = ref(null)
const editForm = ref({
  name: '',
  description: '',
  coverUrl: '',
  isPublic: 1
})
const editRules = {
  name: [
    { required: true, message: '请输入知识库名称', trigger: 'blur' },
    { max: 30, message: '长度不超过 30 个字符', trigger: 'blur' }
  ]
}

const activeSettingTab = ref('basic')
const newMemberName = ref('')
const newMemberRole = ref('VIEWER')

async function addMember() {
  if (!selectedUserId.value) {
    ElMessage.warning('请选择用户')
    return
  }
  try {
    await addKbMember(kbId.value, {
      userId: selectedUserId.value,
      role: newMemberRole.value
    })
    ElMessage.success('邀请成功')
    selectedUserId.value = null
    loadKbDetail()
  } catch (e) {
    console.error('邀请成员失败:', e)
    ElMessage.error('邀请失败')
  }
}

async function changeMemberRole(member, role) {
  try {
    await updateKbMemberRole(kbId.value, member.userId || member.id, { role })
    ElMessage.success('角色已更新')
    loadKbDetail()
  } catch (e) {
    ElMessage.error('更新失败')
  }
}

async function removeMember(member) {
  try {
    await removeKbMember(kbId.value, member.userId || member.id)
    ElMessage.success('已移除')
    loadKbDetail()
  } catch (e) {
    ElMessage.error('移除失败')
  }
}


// ================== 加载数据 ==================
async function loadKbDetail() {
  try {
    const data = await getKbDetail(kbId.value)
    
    // 私有知识库只有管理员和成员可以查看
    const userInfo = getUserInfo()
    const isMember = data.members?.some(m => m.userId === userInfo?.id || m.id === userInfo?.id)
    if (!data.isPublic && !isAdmin.value && !isMember) {
      ElMessage.error('该知识库为私有知识库，无权查看')
      router.replace('/dashboard')
      return
    }
    
    kbInfo.value = data
    recentNotes.value = (data.noteTree || [])
      .filter(n => n.type !== 'mind')
      .slice(0, 5)
      .sort((a, b) => (b.updatedAt || '').localeCompare(a.updatedAt || ''))
  } catch (error) {
    const status = error.response?.status
    if (status === 403) {
      router.replace('/dashboard')
    } else {
      ElMessage.error('加载知识库失败')
    }
  }
}

function openNote(noteId, noteType) {
  router.push(`/note/${noteId}?kbId=${route.params.kbId}&type=${noteType}`)
}

// ================== 编辑知识库 ==================
function openSettings() {
  if (kbInfo.value) {
    editForm.value = {
      name: kbInfo.value.name || '',
      description: kbInfo.value.description || '',
      coverUrl: kbInfo.value.coverUrl || '',
      isPublic: kbInfo.value.isPublic ? 1 : 0
    }
  }
  editDialogVisible.value = true
}

function resetEditForm() {
  editFormRef.value?.clearValidate()
}

async function saveEdit() {
  if (!editFormRef.value) return
  try {
    await editFormRef.value.validate()
  } catch {
    return
  }
  saving.value = true
  try {
    await updateKb(kbId.value, {
      name: editForm.value.name,
      description: editForm.value.description,
      coverUrl: editForm.value.coverUrl,
      isPublic: editForm.value.isPublic
    })
    ElMessage.success('知识库信息已更新')
    editDialogVisible.value = false
    // 刷新页面数据
    loadKbDetail()
    // 更新侧边栏缓存
    localStorage.setItem('current_kb', JSON.stringify({
      ...kbInfo.value,
      ...editForm.value,
      isPublic: editForm.value.isPublic
    }))
  } catch (error) {
    ElMessage.error('更新失败')
  } finally {
    saving.value = false
  }
}

// ================== 删除知识库 ==================
async function deleteKb() {
  try {
    await deleteKbApi(kbId.value)
    ElMessage.success('知识库已删除')
    // 清除缓存
    localStorage.removeItem('current_kb')
    // 跳转首页，侧边栏会重新拉取列表
    router.push('/dashboard')
  } catch (error) {
    ElMessage.error('删除失败')
  }
}

// ================== 工具函数 ==================
function formatDate(dateStr) {
  if (!dateStr) return ''
  return new Date(dateStr).toLocaleDateString('zh-CN')
}

onMounted(loadKbDetail)
</script>

<style scoped>
.kb-detail-page {
  max-width: 900px;
  margin: 0 auto;
  padding: 32px 48px;
}

.kb-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 40px;
}

.kb-info h2 {
  font-size: 24px;
  margin-bottom: 12px;
}

.description {
  color: #666;
  margin-bottom: 12px;
}

.meta {
  font-size: 13px;
  color: #999;
}

.header-actions {
  display: flex;
  gap: 8px;
}

.recent-notes {
  margin-top: 20px;
}

.recent-notes h3 {
  font-size: 16px;
  margin-bottom: 12px;
}

.note-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 12px;
  border-radius: 6px;
  cursor: pointer;
  font-size: 14px;
}

.note-item:hover {
  background: #f5f6f8;
}

.update-time {
  margin-left: auto;
  font-size: 12px;
  color: #999;
}

/* 危险操作区 */
.danger-zone {
  margin-top: 8px;
}
.danger-title {
  font-weight: 500;
  margin-bottom: 12px;
  color: #e6a23c;
}


.owner-role {
  display: inline-block;
  padding: 2px 10px;
  background: #ffe6cc;
  color: #e67e00;
  border-radius: 4px;
  font-size: 13px;
  font-weight: 500;
}


.export-section, .import-section { margin-bottom: 16px; }
.desc { color: #999; font-size: 13px; margin-bottom: 8px; }

.member-actions {
  margin-bottom: 16px;
  display: flex;
  gap: 8px;
}

.invitation-section {
  margin-bottom: 16px;
}

.invitation-section h4 {
  margin-bottom: 12px;
  font-size: 14px;
}

.import-result { margin-top: 8px; color: #67c23a; font-weight: 500; }
</style>