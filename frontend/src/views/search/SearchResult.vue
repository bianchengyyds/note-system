<template>
  <div class="search-page">
    <div class="search-header">
      <h2>搜索结果：{{ keyword }}</h2>
      <el-radio-group v-model="searchType" @change="onTypeChange">
        <el-radio-button label="all">全部</el-radio-button>
        <el-radio-button label="doc">文档</el-radio-button>
        <el-radio-button label="table">表格</el-radio-button>
        <el-radio-button label="board">画板</el-radio-button>
        <el-radio-button label="KNOWLEDGE_BASE">知识库</el-radio-button>
      </el-radio-group>
      <div class="result-count">共 {{ total }} 条结果</div>
    </div>

    <div class="search-results" v-loading="loading">
      <div
        v-for="item in results"
        :key="item.id + item.type"
        class="result-item"
        @click="openItem(item)"
      >
        <el-icon :size="20">
          <component :is="typeIcon(item.type)" />
        </el-icon>
        <div class="result-info">
          <span class="result-title">{{ item.title || item.name }}</span>
          <span class="result-meta">
            {{ typeLabel(item.type) }} · {{ item.kbName || item.creatorName }}
          </span>
        </div>
      </div>
      <el-empty v-if="!results.length && !loading" description="未找到相关内容" />
    </div>

    <div class="pagination-wrap" v-if="total > 0">
      <el-pagination
        v-model:current-page="page"
        v-model:page-size="size"
        :page-sizes="[10, 20, 50]"
        :total="total"
        layout="total, sizes, prev, pager, next, jumper"
        @current-change="doSearch"
        @size-change="doSearch"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, watch, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { search } from '@/api/search'

const route = useRoute()
const router = useRouter()
const keyword = ref(route.query.keyword || '')
const searchType = ref('all')
const results = ref([])
const loading = ref(false)
const page = ref(1)
const size = ref(10)
const total = ref(0)

function onTypeChange() {
  page.value = 1
  doSearch()
}

async function doSearch() {
  if (!keyword.value.trim()) return
  loading.value = true
  try {
    const res = await search({
      keyword: keyword.value,
      type: searchType.value === 'all' ? null : searchType.value,
      page: page.value,
      size: size.value
    })
    // 兼容两种数据格式
    results.value = res.records ?? (Array.isArray(res) ? res : [])
    total.value = res.total ?? 0
  } catch (e) {
    results.value = []
  } finally {
    loading.value = false
  }
}

function openItem(item) {
  if (item.type === 'KNOWLEDGE_BASE') {
    router.push(`/kb/${item.id}`)
  } else {
    router.push(`/note/${item.id}?type=${item.type}`)
  }
}

function typeIcon(type) {
  const map = {
    doc: 'Document',
    table: 'Grid',
    board: 'Picture',
    KNOWLEDGE_BASE: 'Folder'
  }
  return map[type] || 'Document'
}

function typeLabel(type) {
  const map = {
    doc: '文档',
    table: '表格',
    board: '画板',
    KNOWLEDGE_BASE: '知识库'
  }
  return map[type] || type
}

// 监听路由 keyword 变化，自动重新搜索
watch(
  () => route.query.keyword,
  (newKeyword) => {
    if (newKeyword) {
      keyword.value = newKeyword
      page.value = 1
      doSearch()
    }
  }
)

onMounted(() => {
  if (keyword.value) doSearch()
})
</script>

<style scoped>
.search-page {
  max-width: 900px;
  margin: 0 auto;
  padding: 32px 48px;
}

.search-header {
  margin-bottom: 24px;
}
.search-header h2 {
  margin-bottom: 12px;
}

.result-count {
  font-size: 14px;
  color: #8c8c8c;
  margin-top: 8px;
}

.search-results {
  margin-top: 16px;
}

.result-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  border-radius: 6px;
  cursor: pointer;
  transition: background 0.15s;
}
.result-item:hover {
  background: #f5f6f8;
}

.result-info {
  display: flex;
  flex-direction: column;
}

.result-title {
  font-weight: 500;
  color: #1f2937;
}

.result-meta {
  font-size: 12px;
  color: #999;
  margin-top: 4px;
}

.pagination-wrap {
  display: flex;
  justify-content: center;
  margin-top: 16px;
}
</style>