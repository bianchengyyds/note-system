<template>
  <div class="kb-list-page">
    <div class="page-header">
      <h1 class="page-title">我的知识库</h1>
      <el-button type="primary" @click="showCreateDialog = true">
        <el-icon><Plus /></el-icon> 新建知识库
      </el-button>
    </div>

    <div class="kb-tabs">
      <el-tabs v-model="activeTab" type="card">
        <el-tab-pane label="我创建的" name="personal">
          <div class="kb-grid">
            <div
              v-for="kb in personalKbs"
              :key="kb.id"
              class="kb-card"
              @click="openKb(kb.id)"
            >
              <div class="card-header">
                <el-icon class="kb-icon"><Folder /></el-icon>
                <span class="kb-name">{{ kb.name }}</span>
              </div>
              <div class="card-body">
                <p class="kb-desc">{{ kb.description || '暂无描述' }}</p>
              </div>
              <div class="card-footer">
                <span class="meta-item">
                  <el-icon><Document /></el-icon> {{ kb.docCount || 0 }} 篇笔记
                </span>
                <span class="meta-item">
                  <el-icon><User /></el-icon> {{ kb.creatorName || '我' }}
                </span>
                <span class="meta-item">
                  <el-icon><Clock /></el-icon> {{ formatDate(kb.createdAt) }}
                </span>
              </div>
              <div class="card-actions">
                <el-button text size="small" @click.stop="deleteKb(kb)">删除</el-button>
              </div>
            </div>
            <el-empty v-if="!personalKbs.length" description="暂无个人知识库" />
          </div>
        </el-tab-pane>

        <el-tab-pane label="我协作的" name="collaboration">
          <div class="kb-grid">
            <div
              v-for="kb in collaborationKbs"
              :key="kb.id"
              class="kb-card collaboration"
              @click="openKb(kb.id)"
            >
              <div class="card-header">
                <el-icon class="kb-icon"><Users /></el-icon>
                <span class="kb-name">{{ kb.name }}</span>
                <span class="collab-tag">{{ getCollabRoleLabel(kb.role) }}</span>
              </div>
              <div class="card-body">
                <p class="kb-desc">{{ kb.description || '暂无描述' }}</p>
              </div>
              <div class="card-footer">
                <span class="meta-item">
                  <el-icon><Document /></el-icon> {{ kb.docCount || 0 }} 篇笔记
                </span>
                <span class="meta-item">
                  <el-icon><User /></el-icon> {{ kb.creatorName }}
                </span>
                <span class="meta-item">
                  <el-icon><Clock /></el-icon> {{ formatDate(kb.joinedAt || kb.createdAt) }}
                </span>
              </div>

            </div>
            <el-empty v-if="!collaborationKbs.length" description="暂无协作知识库" />
          </div>
        </el-tab-pane>
      </el-tabs>
    </div>

    <CreateKbDialog 
      v-model:visible="showCreateDialog" 
      @created="fetchKbLists"
    />
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getMyKbList, getCollaborations, deleteKb as deleteKbApi } from '@/api/kb'
import CreateKbDialog from '@/components/kb/CreateKbDialog.vue'

const router = useRouter()
const activeTab = ref('personal')
const personalKbs = ref([])
const collaborationKbs = ref([])
const showCreateDialog = ref(false)

const collabRoleLabels = {
  OWNER: '拥有者',
  ADMIN: '可编辑',
  EDITOR: '可编辑',
  VIEWER: '可读'
}

function getCollabRoleLabel(role) {
  return collabRoleLabels[role] || '可读'
}

function handleKbListUpdated() {
  fetchKbLists()
}

async function fetchKbLists() {
  try {
    const [myKbsRes, collaborationsRes] = await Promise.all([
      getMyKbList(),
      getCollaborations()
    ])
    personalKbs.value = myKbsRes?.data?.records || myKbsRes?.records || (Array.isArray(myKbsRes) ? myKbsRes : [])
    collaborationKbs.value = collaborationsRes?.data?.records || collaborationsRes?.records || []
  } catch (e) {
    console.error('获取知识库列表失败', e)
    ElMessage.error('获取知识库列表失败')
  }
}

function openKb(kbId) {
  router.push(`/kb/${kbId}`)
}

async function deleteKb(kb) {
  if (!confirm(`确定删除知识库 "${kb.name}" 吗？`)) return
  
  try {
    await deleteKbApi(kb.id)
    ElMessage.success('删除成功')
    fetchKbLists()
    window.dispatchEvent(new CustomEvent('kb-list-updated'))
  } catch (e) {
    ElMessage.error('删除失败')
  }
}



function formatDate(dateStr) {
  if (!dateStr) return ''
  return new Date(dateStr).toLocaleDateString('zh-CN')
}

onMounted(() => {
  fetchKbLists()
  window.addEventListener('kb-list-updated', handleKbListUpdated)
})

onUnmounted(() => {
  window.removeEventListener('kb-list-updated', handleKbListUpdated)
})
</script>

<style scoped>
.kb-list-page {
  padding: 24px;
  max-width: 1200px;
  margin: 0 auto;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.page-title {
  font-size: 24px;
  font-weight: 600;
}

.kb-tabs {
  margin-top: 16px;
}

.kb-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 20px;
  padding: 20px 0;
}

.kb-card {
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  overflow: hidden;
  cursor: pointer;
  transition: all 0.2s;
  position: relative;
}

.kb-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.1);
}

.kb-card.collaboration {
  border-left: 3px solid #409eff;
}

.card-header {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 16px;
  border-bottom: 1px solid #f0f0f0;
}

.kb-icon {
  font-size: 20px;
  color: #409eff;
}

.kb-card.collaboration .kb-icon {
  color: #67c23a;
}

.kb-name {
  flex: 1;
  font-weight: 600;
  font-size: 15px;
}

.collab-tag {
  background: #e8f5e9;
  color: #2e7d32;
  font-size: 12px;
  padding: 2px 8px;
  border-radius: 4px;
}

.card-body {
  padding: 16px;
}

.kb-desc {
  font-size: 14px;
  color: #666;
  margin: 0;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.card-footer {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  padding: 12px 16px;
  background: #fafafa;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #999;
}

.card-actions {
  position: absolute;
  top: 16px;
  right: 16px;
  opacity: 0;
  transition: opacity 0.2s;
  display: flex;
  gap: 8px;
}

.kb-card:hover .card-actions {
  opacity: 1;
}

.card-actions .el-button {
  padding: 4px 8px;
  font-size: 12px;
}
</style>