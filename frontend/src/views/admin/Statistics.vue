<template>
  <div class="admin-statistics-page">
    <div class="page-header">
      <div>
        <h2>后台统计概览</h2>
        <!-- <p>使用 `/admin/statistics/overview`、`/admin/statistics/yearly` 和 `/admin/statistics/monthly` 接口展示统计数据。</p> -->
      </div>
      <el-button type="primary" @click="refreshAll" :loading="loading">刷新数据</el-button>
    </div>

    <el-row :gutter="20" class="stats-cards">
      <el-col :span="6" v-for="item in statsCards" :key="item.label">
        <el-card shadow="hover" class="stats-card">
          <div class="card-title">{{ item.label }}</div>
          <div class="card-value">{{ item.value }}</div>
          <div class="card-desc">{{ item.desc }}</div>
        </el-card>
      </el-col>
    </el-row>

    <div class="section">
      <div class="section-header">
        <div>
          <h3>年度趋势</h3>
          <span>展示当前年份各月统计汇总数据。</span>
        </div>
        <div class="section-actions">
          <el-select v-model="selectedYear" placeholder="选择年份" size="small" @change="loadYearlyData">
            <el-option v-for="year in availableYears" :key="year" :label="year" :value="year" />
          </el-select>
        </div>
      </div>

      <el-row :gutter="20" class="monthly-cards" v-loading="loadingYearly">
        <el-col :span="4" v-for="item in yearlyCards" :key="item.label">
          <el-card shadow="hover" class="stats-card">
            <div class="card-title">{{ item.label }}</div>
            <div class="card-value">{{ item.value }}</div>
            <div class="card-desc">{{ item.desc }}</div>
          </el-card>
        </el-col>
      </el-row>
      <div v-if="!yearlyData.length && !loadingYearly" class="empty-state">
        <el-empty description="暂无年度统计数据" />
      </div>
    </div>

    <div class="section">
      <div class="section-header">
        <div>
          <h3>月度汇总</h3>
          <span>展示当前月的统计汇总信息。</span>
        </div>
        <div class="section-actions">
          <el-select v-model="selectedMonthYear" placeholder="年份" size="small" @change="loadMonthlyData">
            <el-option v-for="year in availableYears" :key="year" :label="year" :value="year" />
          </el-select>
          <el-select v-model="selectedMonth" placeholder="月份" size="small" @change="loadMonthlyData">
            <el-option v-for="month in months" :key="month.value" :label="month.label" :value="month.value" />
          </el-select>
        </div>
      </div>

      <el-row :gutter="20" class="monthly-cards" v-loading="loadingMonthly">
        <el-col :span="4" v-for="item in monthlyCards" :key="item.label">
          <el-card shadow="hover" class="stats-card">
            <div class="card-title">{{ item.label }}</div>
            <div class="card-value">{{ item.value }}</div>
            <div class="card-desc">{{ item.desc }}</div>
          </el-card>
        </el-col>
      </el-row>
      <div v-if="!monthlyLoaded && !loadingMonthly" class="empty-state">
        <el-empty description="暂无月度统计数据" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import {
  getAdminStatisticsOverview,
  getAdminStatisticsYearly,
  getAdminStatisticsMonthly
} from '@/api/dashboard'

const loading = ref(false)
const loadingYearly = ref(false)
const loadingMonthly = ref(false)
const currentYear = new Date().getFullYear()
const currentMonth = new Date().getMonth() + 1

const stats = reactive({
  totalUsers: 0,
  newUsersToday: 0,
  activeUsersToday: 0,
  totalNotes: 0,
  totalKbs: 0,
  totalComments: 0
})

const selectedYear = ref(currentYear)
const selectedMonthYear = ref(currentYear)
const selectedMonth = ref(currentMonth)
const availableYears = ref([currentYear - 2, currentYear - 1, currentYear])
const months = [
  { label: '1月', value: 1 },
  { label: '2月', value: 2 },
  { label: '3月', value: 3 },
  { label: '4月', value: 4 },
  { label: '5月', value: 5 },
  { label: '6月', value: 6 },
  { label: '7月', value: 7 },
  { label: '8月', value: 8 },
  { label: '9月', value: 9 },
  { label: '10月', value: 10 },
  { label: '11月', value: 11 },
  { label: '12月', value: 12 }
]

const yearlyData = ref([])
const monthlyData = reactive({
  statMonth: '',
  totalUsers: 0,
  newUsers: 0,
  activeUsers: 0,
  totalNotes: 0,
  newNotes: 0,
  totalKbs: 0,
  totalComments: 0
})
const monthlyLoaded = ref(false)

const statsCards = computed(() => [
  { label: '总用户数', value: stats.totalUsers, desc: '系统注册用户总量' },
  { label: '今日新增用户', value: stats.newUsersToday, desc: '当天注册用户数量' },
  { label: '今日活跃用户', value: stats.activeUsersToday, desc: '当天有操作的用户数' },
  { label: '总笔记数', value: stats.totalNotes, desc: '系统中所有笔记总数' },
  { label: '知识库数', value: stats.totalKbs, desc: '当前启用的知识库数量' },
  { label: '总评论数', value: stats.totalComments, desc: '系统中累计评论数量' }
])

const monthlyCards = computed(() => [
  { label: '统计区间', value: monthlyData.statMonth || `${selectedMonthYear.value}年${selectedMonth.value}月`, desc: '当前月度统计范围' },
  { label: '累计用户数', value: monthlyData.totalUsers, desc: '本月累计用户数' },
  { label: '新增用户数', value: monthlyData.newUsers, desc: '本月新增用户数' },
  { label: '活跃用户数', value: monthlyData.activeUsers, desc: '本月活跃用户数' },
  { label: '累计笔记数', value: monthlyData.totalNotes, desc: '本月累计笔记数' },
  { label: '新增笔记数', value: monthlyData.newNotes, desc: '本月新增笔记数' },
  { label: '累计知识库数', value: monthlyData.totalKbs, desc: '本月累计知识库数' },
  { label: '累计评论数', value: monthlyData.totalComments, desc: '本月累计评论数' }
])

const yearlyTotals = computed(() => {
  const totals = {
    totalUsers: 0,
    newUsers: 0,
    activeUsers: 0,
    totalNotes: 0,
    newNotes: 0,
    totalKbs: 0,
    totalComments: 0
  }

  yearlyData.value.forEach(item => {
    totals.totalUsers = item.totalUsers ?? totals.totalUsers
    totals.totalNotes = item.totalNotes ?? totals.totalNotes
    totals.totalKbs = item.totalKbs ?? totals.totalKbs
    totals.newUsers += item.newUsers ?? 0
    totals.activeUsers += item.activeUsers ?? 0
    totals.newNotes += item.newNotes ?? 0
    totals.totalComments += item.totalComments ?? 0
  })

  return totals
})

const yearlyCards = computed(() => [
  { label: '统计年份', value: `${selectedYear.value}年`, desc: '当前年度统计范围' },
  { label: '累计用户数', value: yearlyTotals.value.totalUsers, desc: '年度累计用户数' },
  { label: '新增用户数', value: yearlyTotals.value.newUsers, desc: '年度新增用户数' },
  { label: '活跃用户数', value: yearlyTotals.value.activeUsers, desc: '年度活跃用户数' },
  { label: '累计笔记数', value: yearlyTotals.value.totalNotes, desc: '年度累计笔记数' },
  { label: '新增笔记数', value: yearlyTotals.value.newNotes, desc: '年度新增笔记数' },
  { label: '累计知识库数', value: yearlyTotals.value.totalKbs, desc: '年度累计知识库数' },
  { label: '累计评论数', value: yearlyTotals.value.totalComments, desc: '年度累计评论数' }
])

async function loadOverview() {
  loading.value = true
  try {
    const res = await getAdminStatisticsOverview()
    Object.assign(stats, {
      totalUsers: res.totalUsers ?? 0,
      newUsersToday: res.newUsersToday ?? 0,
      activeUsersToday: res.activeUsersToday ?? 0,
      totalNotes: res.totalNotes ?? 0,
      totalKbs: res.totalKbs ?? 0,
      totalComments: res.totalComments ?? 0
    })
  } catch (e) {
    console.error('获取后台统计概览失败', e.response?.data || e)
    ElMessage.error(`获取后台统计概览失败：${e.response?.data?.msg || e.message || '服务器错误'}`)
  } finally {
    loading.value = false
  }
}

async function loadYearlyData() {
  loadingYearly.value = true
  yearlyData.value = []
  try {
    const res = await getAdminStatisticsYearly(selectedYear.value)
    if (Array.isArray(res)) {
      yearlyData.value = res.map(item => ({
        statMonth: item.statMonth,
        totalUsers: item.totalUsers ?? 0,
        newUsers: item.newUsers ?? 0,
        activeUsers: item.activeUsers ?? 0,
        totalNotes: item.totalNotes ?? 0,
        newNotes: item.newNotes ?? 0,
        totalKbs: item.totalKbs ?? 0,
        totalComments: item.totalComments ?? 0
      }))
    }
  } catch (e) {
    console.error('获取年度统计数据失败', e.response?.data || e)
    ElMessage.error(`获取年度统计数据失败：${e.response?.data?.msg || e.message || '服务器错误'}`)
  } finally {
    loadingYearly.value = false
  }
}

async function loadMonthlyData() {
  loadingMonthly.value = true
  monthlyLoaded.value = false
  try {
    const res = await getAdminStatisticsMonthly(selectedMonthYear.value, selectedMonth.value)
    Object.assign(monthlyData, {
      statMonth: res.statMonth ?? `${selectedMonthYear.value}-${String(selectedMonth.value).padStart(2, '0')}`,
      totalUsers: res.totalUsers ?? 0,
      newUsers: res.newUsers ?? 0,
      activeUsers: res.activeUsers ?? 0,
      totalNotes: res.totalNotes ?? 0,
      newNotes: res.newNotes ?? 0,
      totalKbs: res.totalKbs ?? 0,
      totalComments: res.totalComments ?? 0
    })
    monthlyLoaded.value = true
  } catch (e) {
    console.error('获取月度统计数据失败', e.response?.data || e)
    ElMessage.error(`获取月度统计数据失败：${e.response?.data?.msg || e.message || '服务器错误'}`)
  } finally {
    loadingMonthly.value = false
  }
}

async function refreshAll() {
  await Promise.all([loadOverview(), loadYearlyData(), loadMonthlyData()])
}

onMounted(() => {
  refreshAll()
})
</script>

<style scoped>
.admin-statistics-page {
  width: 100%;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.page-header h2 {
  margin: 0;
  font-size: 24px;
}

.page-header p {
  margin: 8px 0 0;
  color: #6b7280;
}

.stats-cards {
  margin-bottom: 24px;
}

.stats-card {
  min-height: 120px;
}

.card-title {
  color: #6b7280;
  margin-bottom: 12px;
}

.card-value {
  font-size: 28px;
  font-weight: 700;
  color: #111827;
  margin-bottom: 8px;
}

.card-desc {
  color: #6b7280;
  font-size: 12px;
}

.section {
  margin-bottom: 24px;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  margin-bottom: 16px;
}

.section-header h3 {
  margin: 0;
}

.section-header span {
  color: #6b7280;
  font-size: 14px;
}

.section-actions {
  display: flex;
  gap: 12px;
  align-items: center;
}

.monthly-cards {
  margin-top: 16px;
}

.empty-state {
  padding: 40px 0;
  display: flex;
  justify-content: center;
}
</style>