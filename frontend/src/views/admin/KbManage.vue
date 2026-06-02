<template>
  <div class="admin-page">
    <h2>知识库管理</h2>

    <el-row :gutter="16" class="search-bar">
      <el-col :span="6">
        <el-input
          v-model="searchParams.keyword"
          placeholder="知识库名称关键字"
          clearable
          @keyup.enter="fetchData"
        />
      </el-col>
      <el-col :span="4">
        <el-select v-model="searchParams.isPublic" placeholder="公开状态">
          <el-option label="全部" value="all" />
          <el-option label="公开" value="1" />
          <el-option label="私有" value="0" />
        </el-select>
      </el-col>
      <el-col :span="4">
        <el-button type="primary" @click="fetchData">搜索</el-button>
      </el-col>
    </el-row>

    <el-table :data="kbList" v-loading="loading" border class="mt-16">
      <el-table-column label="序号" width="60">
        <template #default="{ $index }">
          {{ ($index + 1) + (searchParams.page - 1) * searchParams.size }}
        </template>
      </el-table-column>
      <el-table-column prop="name" label="名称" width="130" />
      <el-table-column prop="description" label="描述" width="180" />
      <el-table-column label="创建者" width="100">
        <template #default="{ row }">
          {{ creatorLabel(row) }}
        </template>
      </el-table-column>
      <el-table-column prop="isPublic" label="公开" width="80">
        <template #default="{ row }">
          <el-tag :type="row.isPublic ? 'success' : 'info'">
            {{ row.isPublic ? '公开' : '私有' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="docCount" label="文档数" width="80" />
      <el-table-column prop="createdAt" label="创建时间" width="180" />
      <el-table-column label="操作" width="150">
        <template #default="{ row }">
          <el-button size="small" @click="openDetail(row)">查看</el-button>
          <el-button
            size="small"
            type="danger"
            @click="handleDelete(row)"
          >
            删除
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="pagination-wrapper">
      <span class="total-count">共 {{ total }} 个知识库</span>
      <el-pagination
        v-model:current-page="searchParams.page"
        :page-size="searchParams.size"
        :total="total"
        layout="prev, pager, next"
        @current-change="fetchData"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getAdminKbs, deleteAdminKb, getUserDetail } from '@/api/admin'

const router = useRouter()

const loading = ref(false)
const kbList = ref([])
const total = ref(0)
const creatorNames = reactive({})

const searchParams = reactive({
  keyword: '',
  isPublic: 'all',
  page: 1,
  size: 10
})

function creatorLabel(row) {
  if (!row) return '-'
  if (row.creatorName) return row.creatorName
  if (row.creatorId != null) {
    return creatorNames[row.creatorId] || String(row.creatorId)
  }
  return '-'
}

async function fetchData() {
  loading.value = true
  try {
    const params = { ...searchParams }
    if (params.isPublic === 'all') delete params.isPublic
    const res = await getAdminKbs(params)
    kbList.value = res.records ?? []
    total.value = res.total ?? 0
    await loadCreatorNames(kbList.value)
  } catch (e) {
    console.error('获取知识库列表失败', e)
    ElMessage.error('获取知识库列表失败')
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

async function openDetail(row) {
  router.push({ path: `/kb/${row.id}` })
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(
      `确定要删除知识库 "${row.name}" 吗？此操作不可恢复！`,
      '确认删除',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
      }
    )

    await deleteAdminKb(row.id)
    ElMessage.success('删除成功')
    fetchData()
  } catch (e) {
    if (e !== 'cancel') {
      console.error('删除知识库失败', e)
      ElMessage.error('删除知识库失败')
    }
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
  font-size: 14px;
  color: #666;
}
.dialog-loading {
  text-align: center;
  padding: 20px;
}
</style>