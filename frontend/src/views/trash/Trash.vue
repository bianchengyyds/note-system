<template>
  <div class="trash-page">
    <h2>回收站</h2>
    <el-tabs v-model="activeTab" @tab-click="fetchData">
      <el-tab-pane label="笔记" name="notes">
        <div class="table-header">
          <span class="total-info">共 {{ notesTotal }} 条</span>
        </div>
        <el-table :data="notes" v-loading="loadingNotes" border>
          <el-table-column prop="title" label="标题" />
          <el-table-column prop="type" label="类型" width="100">
            <template #default="{ row }">
              <el-tag size="small">{{ typeLabel(row.type) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="deletedAt" label="删除时间" width="200">
            <template #default="{ row }">
              {{ formatDate(row.deletedAt) }}
            </template>
          </el-table-column>
          <el-table-column label="操作" width="200">
            <template #default="{ row }">
              <el-button size="small" type="primary" @click="restoreNote(row.id)">恢复</el-button>
              <el-popconfirm title="永久删除？不可恢复" @confirm="permanentDeleteNote(row.id)">
                <template #reference>
                  <el-button size="small" type="danger">彻底删除</el-button>
                </template>
              </el-popconfirm>
            </template>
          </el-table-column>
        </el-table>
        <div class="pagination-wrap" v-if="notesTotal > 0">
          <el-pagination
            v-model:current-page="notesPage"
            v-model:page-size="notesSize"
            :page-sizes="[10, 20, 50]"
            :total="notesTotal"
            layout="total, sizes, prev, pager, next, jumper"
            @current-change="fetchNotes"
            @size-change="fetchNotes"
          />
        </div>
        <el-empty v-if="!notes.length && !loadingNotes" description="回收站无笔记" />
      </el-tab-pane>

      <el-tab-pane label="知识库" name="kbs">
        <div class="table-header">
          <span class="total-info">共 {{ kbsTotal }} 条</span>
        </div>
        <el-table :data="kbs" v-loading="loadingKbs" border>
          <el-table-column prop="name" label="名称" />
          <el-table-column prop="description" label="描述" />
          <el-table-column label="操作" width="200">
            <template #default="{ row }">
              <el-button size="small" type="primary" @click="restoreKb(row.id)">恢复</el-button>
              <el-popconfirm title="永久删除？不可恢复" @confirm="permanentDeleteKb(row.id)">
                <template #reference>
                  <el-button size="small" type="danger">彻底删除</el-button>
                </template>
              </el-popconfirm>
            </template>
          </el-table-column>
        </el-table>
        <div class="pagination-wrap" v-if="kbsTotal > 0">
          <el-pagination
            v-model:current-page="kbsPage"
            v-model:page-size="kbsSize"
            :page-sizes="[10, 20, 50]"
            :total="kbsTotal"
            layout="total, sizes, prev, pager, next, jumper"
            @current-change="fetchKbs"
            @size-change="fetchKbs"
          />
        </div>
        <el-empty v-if="!kbs.length && !loadingKbs" description="回收站无知识库" />
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getRecycleNotes, restoreNote as restoreNoteApi, permanentDeleteNote as permanentDeleteNoteApi } from '@/api/note'
import { getRecycleKbs, restoreKb as restoreKbApi, permanentDeleteKb as permanentDeleteKbApi } from '@/api/kb'

const activeTab = ref('notes')

const notes = ref([])
const loadingNotes = ref(false)
const notesPage = ref(1)
const notesSize = ref(10)
const notesTotal = ref(0)

const kbs = ref([])
const loadingKbs = ref(false)
const kbsPage = ref(1)
const kbsSize = ref(10)
const kbsTotal = ref(0)

async function fetchData() {
  if (activeTab.value === 'notes') {
    await fetchNotes()
  } else {
    await fetchKbs()
  }
}

async function fetchNotes() {
  loadingNotes.value = true
  try {
    const data = await getRecycleNotes({ page: notesPage.value, size: notesSize.value })
    notes.value = data.records ?? (Array.isArray(data) ? data : [])
    notesTotal.value = data.total ?? 0
  } catch (e) {
    ElMessage.error('获取笔记回收站失败')
  } finally {
    loadingNotes.value = false
  }
}

async function fetchKbs() {
  loadingKbs.value = true
  try {
    const data = await getRecycleKbs({ page: kbsPage.value, size: kbsSize.value })
    kbs.value = data.records ?? (Array.isArray(data) ? data : [])
    kbsTotal.value = data.total ?? 0
  } catch (e) {
    ElMessage.error('获取知识库回收站失败')
  } finally {
    loadingKbs.value = false
  }
}

async function restoreNote(id) {
  try {
    await restoreNoteApi(id)
    ElMessage.success('笔记已恢复')
    fetchNotes()
  } catch (e) {
    ElMessage.error('恢复失败')
  }
}

async function permanentDeleteNote(id) {
  try {
    await permanentDeleteNoteApi(id)
    ElMessage.success('已彻底删除')
    fetchNotes()
  } catch (e) {
    ElMessage.error('删除失败')
  }
}

async function restoreKb(id) {
  try {
    await restoreKbApi(id)
    ElMessage.success('知识库已恢复')
    fetchKbs()
    // 可选：刷新侧边栏知识库列表
  } catch (e) {
    ElMessage.error('恢复失败')
  }
}

async function permanentDeleteKb(id) {
  try {
    await permanentDeleteKbApi(id)
    ElMessage.success('已彻底删除')
    fetchKbs()
  } catch (e) {
    ElMessage.error('删除失败')
  }
}

function typeLabel(type) {
  const m = { doc: '文档', table: '表格', board: '画板', mind: '思维导图' }
  return m[type] || type
}

function formatDate(dateStr) {
  if (!dateStr) return ''
  return new Date(dateStr).toLocaleString('zh-CN')
}

onMounted(() => {
  fetchNotes()
  fetchKbs()
})

</script>

<style scoped>
.trash-page {
  max-width: 900px;
  margin: 0 auto;
  padding: 32px 48px;
}
.subtitle {
  color: #999;
  font-size: 14px;
  margin-bottom: 16px;
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