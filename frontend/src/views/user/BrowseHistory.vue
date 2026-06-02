<template>
  <div class="history-page">
    <h2>我的浏览历史</h2>
    <div class="table-header">
      <span class="total-info">共 {{ total }} 条记录</span>
    </div>
    <el-table :data="list" v-loading="loading" border>
      <el-table-column prop="noteId" label="ID" width="60" />
      <el-table-column prop="title" label="标题" />
      <el-table-column prop="type" label="类型" width="80">
        <template #default="{ row }">
          <el-tag size="small">{{ typeLabel(row.type) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="kbName" label="所属知识库" width="150" />
      <el-table-column prop="browseTime" label="浏览时间" width="180">
        <template #default="{ row }">
          {{ formatDate(row.browseTime) }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="100">
        <template #default="{ row }">
          <el-button size="small" @click="openNote(row.noteId, row.type)">查看</el-button>
        </template>
      </el-table-column>
    </el-table>
    <div class="pagination-wrap" v-if="total > 0">
      <el-pagination
        v-model:current-page="page"
        v-model:page-size="size"
        :page-sizes="[10, 20, 50]"
        :total="total"
        layout="total, sizes, prev, pager, next, jumper"
        @current-change="fetchHistory"
        @size-change="fetchHistory"
      />
    </div>
    <el-empty v-if="!list.length && !loading" description="暂无浏览记录" />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getBrowseHistory } from '@/api/user'

const router = useRouter()
const list = ref([])
const loading = ref(false)
const page = ref(1)
const size = ref(10)
const total = ref(0)

async function fetchHistory() {
  loading.value = true
  try {
    const res = await getBrowseHistory({ page: page.value, size: size.value })
    list.value = res.records ?? (Array.isArray(res) ? res : [])
    total.value = res.total ?? 0
  } catch (e) {
    ElMessage.error('获取浏览历史失败')
  } finally {
    loading.value = false
  }
}

function openNote(noteId, type) {
  router.push(`/note/${noteId}?type=${type}`)
}

function typeLabel(type) {
  const map = { doc: '文档', table: '表格', board: '画板', mind: '思维导图' }
  return map[type] || type
}

function formatDate(dateStr) {
  if (!dateStr) return ''
  return new Date(dateStr).toLocaleString('zh-CN')
}

onMounted(fetchHistory)
</script>

<style scoped>
.history-page {
  max-width: 900px;
  margin: 0 auto;
  padding: 32px 48px;
}
.table-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}
.total-info {
  font-size: 14px;
  color: #909399;
}
.pagination-wrap {
  display: flex;
  justify-content: center;
  margin-top: 16px;
}
</style>