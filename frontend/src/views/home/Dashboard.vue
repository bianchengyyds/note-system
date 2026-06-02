<template>
  <div class="dashboard-page">
    <h1 class="welcome">欢迎回来，{{ userName }}</h1>

    <!-- 统计卡片 -->
    <div class="stat-cards">
      <div class="card">
        <div class="card-value">{{ stats.totalNotes ?? '-' }}</div>
        <div class="card-label">笔记总数</div>
      </div>
      <div class="card">
        <div class="card-value">{{ stats.totalKbs ?? '-' }}</div>
        <div class="card-label">知识库</div>
      </div>
      <div class="card">
        <div class="card-value">{{ stats.activeDays ?? '-' }}</div>
        <div class="card-label">活跃天数</div>
      </div>
    </div>

    <!-- 最近更新的笔记 -->
    <div class="recent-section" v-if="recentNotes.length">
      <h3>最近更新</h3>
      <div
        v-for="note in recentNotes"
        :key="note.id"
        class="recent-item"
        @click="openNote(note.id)"
      >
        <el-icon><Document /></el-icon>
        <span>{{ note.title }}</span>
        <span class="time">{{ formatDate(note.updatedAt) }}</span>
      </div>
    </div>

    <!-- 热门推荐区域 -->
    <div class="recommend-section" v-if="hotNotes.length || hotKbs.length">
      <el-divider />
      <h3>热门推荐</h3>
      <el-row :gutter="24">
        <!-- 热门笔记 -->
        <el-col :span="12" v-if="hotNotes.length">
          <div class="recommend-panel">
            <h4 class="panel-title">🔥 热门笔记</h4>
            <div
              v-for="note in hotNotes"
              :key="note.id"
              class="recommend-item"
              @click="openNote(note.id, note.type)"
            >
              <span class="item-title">{{ note.title }}</span>
              <span class="item-meta">
                {{ note.viewCount }} 浏览 · {{ note.likeCount }} 赞 · {{ note.kbName }}
              </span>
            </div>
          </div>
        </el-col>
        <!-- 热门知识库 -->
        <el-col :span="12" v-if="hotKbs.length">
          <div class="recommend-panel">
            <h4 class="panel-title">📚 热门知识库</h4>
            <div
              v-for="kb in hotKbs"
              :key="kb.id"
              class="recommend-item"
              @click="openKb(kb.id)"
            >
              <span class="item-title">{{ kb.name }}</span>
              <span class="item-meta">{{ kb.docCount }} 篇笔记 · {{ kb.totalViews || 0 }} 浏览 · {{ kb.creatorName }}</span>
            </div>
          </div>
        </el-col>
      </el-row>
    </div>

  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getUserInfo } from '@/utils/auth'
import { getMyKbList } from '@/api/kb'
import { getNoteDetail } from '@/api/note' // 临时用于获取统计无直接接口，改用客户统计接口
import request from '@/utils/request'

import { getRecommend } from '@/api/recommend'

const hotNotes = ref([])
const hotKbs = ref([])

// 加载热门推荐
async function loadRecommend() {
  try {
    const [notes, kbs] = await Promise.all([
      getRecommend('note', 5),
      getRecommend('kb', 5)
    ])
    hotNotes.value = Array.isArray(notes) ? notes : []
    hotKbs.value = Array.isArray(kbs) ? kbs : []
  } catch (e) {
    // 静默失败
  }
}

// 打开知识库
function openKb(kbId) {
  router.push(`/kb/${kbId}`)
}

// 在 onMounted 中调用 loadRecommend
onMounted(() => {
  // ... 原有的加载统计和最近笔记
  loadRecommend()
})

const router = useRouter()
const userName = ref('')

const stats = ref({
  totalNotes: 0,
  totalKbs: 0,
  activeDays: 0
})

const recentNotes = ref([])

async function loadDashboard() {
  const userInfo = getUserInfo()
  if (userInfo) userName.value = userInfo.name

  try {
    // 获取知识库列表
    const kbListRes = await getMyKbList()
    const kbList = kbListRes.records ?? (Array.isArray(kbListRes) ? kbListRes : [])
    stats.value.totalKbs = kbList.length

    // 尝试获取个人统计（如果有接口）
    try {
      const statRes = await request.get('/user/statistics')
      stats.value.totalNotes = statRes.createdNotes ?? 0
      stats.value.activeDays = statRes.activeDays ?? 0
    } catch {
      stats.value.totalNotes = '-'
      stats.value.activeDays = '-'
    }

    // 构建最近笔记（从知识库导出）
    const notes = []
    for (const kb of kbList.slice(0, 3)) {
      try {
        const detail = await request.get(`/kb/${kb.id}`)
        if (detail.noteTree) {
          notes.push(...detail.noteTree)
        }
      } catch {}
    }
    recentNotes.value = notes
      .filter(n => !['table', 'board', 'mind'].includes(n.type))
      .sort((a, b) => (b.updatedAt || '').localeCompare(a.updatedAt || ''))
      .slice(0, 10)
  } catch (e) {
    console.error('加载工作台失败', e)
  }
}

function openNote(noteId) {
  router.push(`/note/${noteId}`)
}

function formatDate(dateStr) {
  if (!dateStr) return ''
  return new Date(dateStr).toLocaleDateString('zh-CN')
}

onMounted(loadDashboard)
</script>

<style scoped>
.dashboard-page {
  max-width: 960px;
  margin: 0 auto;
  padding: 48px 48px;
}

.welcome {
  font-size: 28px;
  margin-bottom: 40px;
}

.stat-cards {
  display: flex;
  gap: 24px;
  margin-bottom: 48px;
}

.card {
  flex: 1;
  background: #f5f7fa;
  border-radius: 8px;
  padding: 24px;
  text-align: center;
}

.card-value {
  font-size: 36px;
  font-weight: 600;
  color: #1f2937;
}

.card-label {
  font-size: 14px;
  color: #666;
  margin-top: 8px;
}

.recent-section {
  margin-top: 24px;
}

.recent-section h3 {
  font-size: 18px;
  margin-bottom: 16px;
}

.recent-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 12px;
  border-radius: 6px;
  cursor: pointer;
  font-size: 14px;
}
.recent-item:hover {
  background: #f0f2f5;
}
.time {
  margin-left: auto;
  font-size: 12px;
  color: #999;
}

.recommend-section {
  margin-top: 32px;
}
.recommend-panel {
  background: #f9fafb;
  border-radius: 8px;
  padding: 16px;
}
.panel-title {
  margin-bottom: 12px;
  font-size: 16px;
  font-weight: 600;
}
.recommend-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 0;
  border-bottom: 1px solid #f0f0f0;
  cursor: pointer;
  transition: background 0.2s;
}
.recommend-item:hover {
  background: #f3f5f7;
}
.item-title {
  font-weight: 500;
  max-width: 60%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.item-meta {
  font-size: 12px;
  color: #999;
  white-space: nowrap;
}
</style>