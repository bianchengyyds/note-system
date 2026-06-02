<template>
  <div class="versions-page">
    <h2>{{ noteTitle }} - 版本历史</h2>
    <el-table :data="versions" v-loading="loading" border>
      <el-table-column prop="version" label="版本号" width="80" />
      <el-table-column prop="title" label="标题" />
      <el-table-column prop="editorName" label="编辑者" width="120" />
      <el-table-column prop="createdAt" label="创建时间" width="180">
        <template #default="{ row }">
          {{ formatDate(row.createdAt) }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="180">
        <template #default="{ row }">
          <el-button size="small" @click="previewVersion(row)">预览</el-button>
          <el-popconfirm title="确定回滚到此版本吗？当前未保存的修改将丢失" @confirm="rollback(row.id)">
            <template #reference>
              <el-button size="small" type="warning">回滚</el-button>
            </template>
          </el-popconfirm>
        </template>
      </el-table-column>
    </el-table>
    <el-empty v-if="!versions.length && !loading" description="暂无版本记录" />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getNoteVersions, rollbackNote } from '@/api/note'

const route = useRoute()
const router = useRouter()
const noteId = route.params.docId
const noteTitle = ref(route.query.title || '笔记')

const versions = ref([])
const loading = ref(false)
const noteType = ref(route.query.type || 'doc')
const kbId = ref(route.query.kbId)
async function fetchVersions() {
  loading.value = true
  try {
    const data = await getNoteVersions(noteId)
    versions.value = Array.isArray(data) ? data : (data.records || [])
  } catch (e) {
    ElMessage.error('获取版本失败')
  } finally {
    loading.value = false
  }
}

function previewVersion(row) {
  router.push(
    `/note/${noteId}?versionId=${row.id}&type=${noteType.value}&kbId=${kbId.value}`
  )
}

async function rollback(versionId) {
  try {
    await rollbackNote(noteId, versionId)
    ElMessage.success('回滚成功')
    // 回滚后跳转到笔记页，并携带 kbId 和 type 保持上下文
    router.replace({
      path: `/note/${noteId}`,
      query: {
        kbId: kbId.value,
        type: noteType.value
      }
    })
  } catch (e) {
    ElMessage.error('回滚失败')
  }
}

function formatDate(dateStr) {
  if (!dateStr) return ''
  return new Date(dateStr).toLocaleString('zh-CN')
}

onMounted(fetchVersions)
</script>

<style scoped>
.versions-page {
  max-width: 900px;
  margin: 0 auto;
  padding: 32px 48px;
}
</style>