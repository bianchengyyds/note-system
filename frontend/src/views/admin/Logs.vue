<template>
  <!-- 局部配置中文：确保日期面板、按钮等显示中文 -->
  <el-config-provider :locale="zhCn">
    <div class="admin-page">
      <h2>操作日志</h2>

      <el-row :gutter="16" class="search-bar">
        <el-col :span="5">
          <el-input
            v-model="searchParams.userName"
            placeholder="用户名（模糊查询）"
            clearable
            @keyup.enter="fetchData"
          />
        </el-col>
        <el-col :span="5">
          <el-input
            v-model="searchParams.operation"
            placeholder="操作类型（模糊查询）"
            clearable
            @keyup.enter="fetchData"
          />
        </el-col>
        <!-- 改为两个独立的日期选择器 -->
        <el-col :span="4">
          <el-date-picker
            v-model="searchParams.startDate"
            type="date"
            placeholder="开始日期"
            format="YYYY-MM-DD"
            value-format="YYYY-MM-DD"
            style="width: 100%"
          />
        </el-col>
        <el-col :span="4">
          <el-date-picker
            v-model="searchParams.endDate"
            type="date"
            placeholder="结束日期"
            format="YYYY-MM-DD"
            value-format="YYYY-MM-DD"
            style="width: 100%"
          />
        </el-col>
        <el-col :span="6">
          <el-button type="primary" @click="fetchData">搜索</el-button>
          <el-button @click="resetSearch">重置</el-button>
        </el-col>
      </el-row>

      <el-table
        :data="logList"
        v-loading="loading"
        border
        style="width: 100%"
        class="mt-16"
      >
        <!-- 表格列保持不变 -->
        <el-table-column label="序号" width="60">
          <template #default="{ $index }">
            {{ ($index + 1) + (searchParams.page - 1) * searchParams.size }}
          </template>
        </el-table-column>
        <el-table-column prop="userName" label="用户" width="140" />
        <el-table-column prop="operation" label="操作类型" width="200" />
        <el-table-column label="目标资源" width="200">
          <template #default="{ row }">
            {{ row.targetType ? `${row.targetType}:${row.targetId}` : '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="ip" label="IP" width="140" />
        <el-table-column prop="createdAt" label="时间" width="180" />
      </el-table>

      <div class="pagination-wrapper">
        <span class="total-count">共 {{ total }} 条日志</span>
        <el-pagination
          v-model:current-page="searchParams.page"
          :page-size="searchParams.size"
          :total="total"
          layout="prev, pager, next"
          @current-change="handlePageChange"
        />
      </div>
    </div>
  </el-config-provider>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getAdminLogs, getUserDetail } from '@/api/admin'
// 引入 Element Plus 中文语言包
import zhCn from 'element-plus/dist/locale/zh-cn.mjs'

const loading = ref(false)
const logList = ref([])
const total = ref(0)

const searchParams = reactive({
  userName: '',
  operation: '',
  // 改为两个独立字段
  startDate: '',
  endDate: '',
  page: 1,
  size: 10
})

// 格式化函数保留，但可能不再需要，下面仍可用于兼容
function formatLocalDate(value) {
  if (!value) return ''
  const date = value instanceof Date ? value : new Date(value)
  if (Number.isNaN(date.getTime())) return ''
  const pad = (n) => String(n).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`
}

// 补全缺失用户名的函数：批量按 userId 请求用户详情并更新表格
async function fillMissingUserNames() {
  try {
    const missingIds = [...new Set(logList.value
      .filter(i => (!i.userName || i.userName === '') && i.userId != null)
      .map(i => String(i.userId)))]

    if (!missingIds.length) return

    const promises = missingIds.map(idStr =>
      getUserDetail(idStr)
        .then(user => ({ id: idStr, name: (user?.name || user?.username || user?.nickName || user?.nick_name || String(idStr)).toString() }))
        .catch(() => ({ id: idStr, name: String(idStr) }))
    )

    const results = await Promise.all(promises)
    const nameMap = new Map(results.map(r => [String(r.id), r.name]))

    logList.value = logList.value.map(item => {
      const key = item.userId != null ? String(item.userId) : null
      if ((!item.userName || item.userName === '') && key && nameMap.has(key)) {
        return { ...item, userName: nameMap.get(key) }
      }
      return item
    })
  } catch (e) {
    console.error('填充缺失用户名失败', e)
  }
}

async function fetchData() {
  loading.value = true
  try {
    const params = {
      page: searchParams.page,
      size: searchParams.size
    }

    // --- 模糊查询修正：直接传 userName / operation，让后端自己处理 like ---
    if (searchParams.userName) {
      params.userName = searchParams.userName.trim()
    }
    if (searchParams.operation) {
      params.operation = searchParams.operation.trim()
    }

    // --- 日期处理：使用两个独立字段 ---
    if (searchParams.startDate) {
      // 传后端时加上时间部分，方便后端处理一整天
      params.startTime = `${searchParams.startDate}T00:00:00`
    }
    if (searchParams.endDate) {
      params.endTime = `${searchParams.endDate}T23:59:59`
    }

    const res = await getAdminLogs(params)

    const records = (res.records || res.list || []).map((item) => ({
      ...item,
      userName:
        item.userName?.trim() ||
        item.username?.trim() ||
        item.user?.name?.trim() ||
        item.user?.username?.trim() ||
        item.user?.nickName?.trim() ||
        item.user?.nick_name?.trim() ||
        '',
      operation: item.operation || item.action || item.type || '未知',
      targetType: item.targetType || item.resourceType || '',
      targetId: item.targetId || item.resourceId || '',
      ip: item.ip || item.clientIp || item.ipAddress || '',
      createdAt: item.createdAt || item.timestamp || item.operateTime || ''
    }))

    logList.value = records
    total.value = res.total ?? res.count ?? logList.value.length

    // 补全缺失用户名（若后端未返回用户名但返回了 userId）
    await fillMissingUserNames()
  } catch (e) {
    console.error('获取操作日志失败', e)
    ElMessage.error('获取操作日志失败')
  } finally {
    loading.value = false
  }
}

function resetSearch() {
  searchParams.userName = ''
  searchParams.operation = ''
  searchParams.startDate = ''
  searchParams.endDate = ''
  searchParams.page = 1
  fetchData()
}

function handlePageChange(page) {
  searchParams.page = page
  fetchData()
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
  padding: 16px 0;
}
.total-count {
  color: #606266;
}
</style>