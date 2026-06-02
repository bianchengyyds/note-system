<template>
  <div class="pending-invitations">
    <div class="header">
      <h3>待处理的邀请</h3>
      <el-button size="small" @click="loadInvitations">刷新</el-button>
    </div>

    <el-empty v-if="!invitations.length" description="暂无待处理的邀请" />

    <div v-else class="invitation-list">
      <div
        v-for="inv in invitations"
        :key="inv.id"
        class="invitation-item"
      >
        <div class="inv-info">
          <span class="kb-name">{{ getKbName(inv) }}</span>
          <span class="role-tag">
            <el-tag :type="getRoleTagType(inv.role)" size="small">
              {{ getRoleLabel(inv.role) }}
            </el-tag>
          </span>
          <span class="inviter">邀请人: {{ getInviterName(inv) }}</span>
          <span class="time">{{ formatDate(inv.createdAt) }}</span>
        </div>
        <div class="inv-actions">
          <el-button
            size="small"
            type="primary"
            :loading="acceptingId === inv.id"
            @click="handleAccept(inv)"
          >
            接受
          </el-button>
          <el-button
            size="small"
            type="info"
            :loading="rejectingId === inv.id"
            @click="handleReject(inv)"
          >
            拒绝
          </el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getPendingInvitations, acceptKbInvitation, rejectKbInvitation, getKbDetail } from '@/api/kb'
import { getUserDetail } from '@/api/admin'

const router = useRouter()

const invitations = ref([])
const acceptingId = ref(null)
const rejectingId = ref(null)
const userCache = ref({})
const kbCache = ref({})

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

function getRoleLabel(role) {
  return roleLabels[role] || role
}

function getRoleTagType(role) {
  return roleTagTypes[role] || 'info'
}

async function loadInvitations() {
  try {
    const data = await getPendingInvitations()
    invitations.value = Array.isArray(data) ? data : []
    await Promise.all([loadInviterNames(), loadKbNames()])
  } catch (e) {
    ElMessage.error('加载邀请列表失败')
  }
}

async function loadInviterNames() {
  const ids = [...new Set(invitations.value.map(inv => inv.inviterId).filter(Boolean))]
  for (const id of ids) {
    if (!userCache.value[id]) {
      try {
        const user = await getUserDetail(id)
        userCache.value[id] = user?.name || `用户${id}`
      } catch {
        userCache.value[id] = `用户${id}`
      }
    }
  }
}

async function loadKbNames() {
  const ids = [...new Set(invitations.value.map(inv => inv.kbId).filter(Boolean))]
  for (const id of ids) {
    if (!kbCache.value[id]) {
      try {
        const kb = await getKbDetail(id)
        kbCache.value[id] = kb?.name || `知识库${id}`
      } catch {
        kbCache.value[id] = `知识库${id}`
      }
    }
  }
}

function getInviterName(inv) {
  return userCache.value[inv.inviterId] || inv.inviterName || `用户${inv.inviterId}`
}

function getKbName(inv) {
  return kbCache.value[inv.kbId] || inv.kbName || `知识库${inv.kbId}`
}

async function handleAccept(inv) {
  acceptingId.value = inv.id
  try {
    await acceptKbInvitation(inv.id)
    ElMessage.success('已接受邀请')
    await loadInvitations()
  } catch (e) {
    ElMessage.error('接受邀请失败')
  } finally {
    acceptingId.value = null
  }
}

async function handleReject(inv) {
  rejectingId.value = inv.id
  try {
    await rejectKbInvitation(inv.id)
    ElMessage.success('已拒绝邀请')
    await loadInvitations()
  } catch (e) {
    ElMessage.error('拒绝邀请失败')
  } finally {
    rejectingId.value = null
  }
}

function formatDate(dateStr) {
  if (!dateStr) return ''
  return new Date(dateStr).toLocaleString('zh-CN')
}

onMounted(loadInvitations)
</script>

<style scoped>
.pending-invitations {
  padding: 16px;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.header h3 {
  margin: 0;
  font-size: 16px;
}

.invitation-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.invitation-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  background: #f5f7fa;
  border-radius: 8px;
}

.inv-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.kb-name {
  font-weight: 500;
}

.inviter {
  color: #909399;
  font-size: 13px;
}

.time {
  color: #c0c4cc;
  font-size: 12px;
}

.inv-actions {
  display: flex;
  gap: 8px;
}
</style>
