<template>
  <div class="admin-page">
    <h2>笔记管理</h2>

    <el-row :gutter="16" class="search-bar">
      <el-col :span="6">
        <el-input
          v-model="searchParams.keyword"
          placeholder="标题关键字"
          clearable
          @keyup.enter="fetchData"
        />
      </el-col>
      <el-col :span="4">
        <el-select v-model="searchParams.type" placeholder="笔记类型"  >
          <el-option label="全部" value="all" />
          <el-option label="文档" value="doc" />
          <el-option label="表格" value="table" />
          <el-option label="画板" value="board" />
          <el-option label="思维导图" value="mind" />
        </el-select>
      </el-col>
      <el-col :span="4">
        <el-select v-model="searchParams.auditStatus" placeholder="审核状态">
          <el-option label="全部" value="all" />
          <el-option label="待审核" value="0" />
          <el-option label="通过" value="1" />
          <el-option label="驳回" value="2" />
        </el-select>
      </el-col>
      <el-col :span="4">
        <el-button type="primary" @click="fetchData">搜索</el-button>
      </el-col>
    </el-row>

    <el-table :data="noteList" v-loading="loading" border class="mt-16">
      <el-table-column label="序号" width="60">
        <template #default="{ $index }">
          {{ ($index + 1) + (searchParams.page - 1) * searchParams.size }}
        </template>
      </el-table-column>
      <el-table-column prop="title" label="标题" show-overflow-tooltip />
      <el-table-column prop="type" label="类型" width="100">
        <template #default="{ row }">
          <el-tag type="primary">{{ typeLabel(row.type) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="kbName" label="知识库" width="100" show-overflow-tooltip />
      <el-table-column label="作者" width="100">
        <template #default="{ row }">
          {{ authorLabel(row) }}
        </template>
      </el-table-column>
      <el-table-column prop="auditStatus" label="审核" width="100">
        <template #default="{ row }">
          <el-tag :type="statusTagType(row.auditStatus)">
            {{ auditStatusLabel(row.auditStatus) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" label="创建时间" width="180" />
      <el-table-column prop="updatedAt" label="更新时间" width="180" />
      <el-table-column label="操作" width="150">
        <template #default="{ row }">
          <el-button size="small" @click="openDetail(row)">查看</el-button>
          <el-button
            size="small"
            type="warning"
            @click="openAudit(row)"
            :disabled="row.auditStatus === 1"
          >
            审核
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="pagination-wrapper">
      <span class="total-count">共 {{ total }} 条笔记</span>
      <el-pagination
        v-model:current-page="searchParams.page"
        :page-size="searchParams.size"
        :total="total"
        layout="prev, pager, next"
        @current-change="fetchData"
      />
    </div>

    <el-dialog v-model="auditDialogVisible" title="审核笔记" width="520px">
      <el-form :model="auditForm" label-width="100px">
        <el-form-item label="审核结果">
          <el-radio-group v-model="auditForm.approved">
            <el-radio :label="true">通过</el-radio>
            <el-radio :label="false">驳回</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="说明理由">
          <el-input
            type="textarea"
            v-model="auditForm.reason"
            placeholder="可选：填写审核说明"
            :rows="4"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="auditDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="auditLoading" @click="submitAudit">提交审核</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getAdminNotes, auditAdminNote, getUserDetail } from '@/api/admin'

const router = useRouter()
const loading = ref(false)
const noteList = ref([])
const total = ref(0)
const creatorNames = reactive({})

const searchParams = reactive({
  keyword: '',
  type: 'all',
  auditStatus: 'all',
  page: 1,
  size: 10
})

const auditDialogVisible = ref(false)
const auditLoading = ref(false)
const currentAuditNoteId = ref(null)
const auditForm = reactive({ approved: true, reason: '' })

function typeLabel(type) {
  const map = { doc: '文档', table: '表格', board: '画板', mind: '思维导图' }
  return map[type] || type || '-'
}

function auditStatusLabel(status) {
  if (status === 0 || status === '0') return '待审核'
  if (status === 1 || status === '1') return '通过'
  if (status === 2 || status === '2') return '驳回'
  return '未知'
}

function statusTagType(status) {
  if (status === 0 || status === '0') return 'warning'
  if (status === 1 || status === '1') return 'success'
  if (status === 2 || status === '2') return 'danger'
  return 'info'
}

async function fetchData() {
  loading.value = true
  try {
    const res = await getAdminNotes({ ...searchParams })
    noteList.value = res.records ?? []
    total.value = res.total ?? 0
    await loadCreatorNames(noteList.value)
  } catch (e) {
    console.error('获取笔记列表失败', e)
    ElMessage.error('获取笔记列表失败')
  } finally {
    loading.value = false
  }
}

async function loadCreatorNames(rows) {
  const ids = [...new Set(rows
    .map(item => item.creatorId)
    .filter(id => id != null && !creatorNames[id]))]

  await Promise.all(ids.map(id => fetchCreatorName(id)))
}

async function fetchCreatorName(creatorId) {
  if (!creatorId || creatorNames[creatorId]) return
  try {
    const user = await getUserDetail(creatorId)
    creatorNames[creatorId] = user.name || user.username || String(creatorId)
  } catch (e) {
    creatorNames[creatorId] = String(creatorId)
  }
}

function openDetail(row) {
  router.push({ path: `/admin/notes/view/${row.id}`, query: { type: row.type } })
}

function authorLabel(row) {
  if (!row) return '-'
  if (row.creatorName) return row.creatorName
  if (row.creatorId != null) {
    return creatorNames[row.creatorId] || String(row.creatorId)
  }
  return '-'
}

function openAudit(row) {
  currentAuditNoteId.value = row.id
  auditForm.approved = row.auditStatus === 1
  auditForm.reason = ''
  auditDialogVisible.value = true
}

async function submitAudit() {
  auditLoading.value = true
  try {
    await auditAdminNote(currentAuditNoteId.value, {
      approved: auditForm.approved,
      reason: auditForm.reason
    })
    ElMessage.success('审核提交成功')
    auditDialogVisible.value = false
    fetchData()
  } catch (e) {
    console.error('审核失败', e)
    ElMessage.error('审核失败')
  } finally {
    auditLoading.value = false
  }
}

onMounted(fetchData)
</script>

<style scoped>
.admin-page {
  max-width: 1200px;
  margin: 0 auto;
  padding: 32px 48px;
}
.search-bar {
  margin-bottom: 16px;
}
.mt-16 {
  margin-top: 16px;
}
.pagination-wrapper {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 16px;
}
.total-count {
  color: #666;
  font-size: 14px;
}
.detail-content {
  white-space: pre-wrap;
  word-break: break-word;
  min-height: 180px;
  background: #f7f7f7;
}
.dialog-loading {
  padding: 24px 0;
  text-align: center;
  color: #999;
}

.type-text {
  color: #409eff;
  font-weight: 600;
}
</style>
