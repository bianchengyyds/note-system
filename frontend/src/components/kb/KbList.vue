<template>
  <div class="kb-list">
    <div class="list-title">我的知识库</div>
    <div
      v-for="kb in kbList"
      :key="kb.id"
      class="kb-item"
      :class="{ active: activeKbId === kb.id }"
      @click="selectKb(kb)"
    >
      <el-icon><Folder /></el-icon>
      <span class="item-text">{{ kb.name }}</span>
      <span class="item-count">{{ kb.docCount ?? 0 }}</span>
    </div>
    <el-empty v-if="!kbList.length" description="暂无知识库" />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { getMyKbList } from '@/api/kb'

const router = useRouter()
const route = useRoute()
const kbList = ref([])
const activeKbId = ref(Number(route.params.kbId) || null)

async function fetchList() {
  const res = await getMyKbList()
  kbList.value = res.records ?? res ?? []
}

function selectKb(kb) {
  localStorage.setItem('current_kb', JSON.stringify(kb))
  router.push(`/kb/${kb.id}`)
}

onMounted(fetchList)
</script>

<style scoped>
.kb-list { padding: 0 12px; }
.list-title { font-size: 12px; color: #999; padding: 8px 4px; }
.kb-item {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 10px;
  border-radius: 6px;
  cursor: pointer;
  font-size: 14px;
}
.kb-item:hover { background: #e8eaed; }
.kb-item.active { background: #d9e2f3; }
.item-text { flex: 1; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.item-count { font-size: 12px; color: #999; }
</style>