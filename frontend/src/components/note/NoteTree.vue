<template>
  <div class="note-tree">
    <div v-if="loading" class="loading">加载中...</div>
    <div v-else-if="!filteredTreeData.length" class="empty">暂无笔记</div>
    <ul v-else class="tree-root">
      <TreeNodeItem
        v-for="node in filteredTreeData"
        :key="node.id"
        :node="node"
        :kbId="kbId"
        :activeNoteId="activeNoteId"
        :canEdit="canEdit"
        @select="selectNote"
        @refresh="loadTree"
        @move-request="loadTree"
      />
    </ul>
  </div>
</template>

<script setup>
import { ref, watch, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getKbDetail } from '@/api/kb'
import TreeNodeItem from './NoteTreeItem.vue'

const props = defineProps({
  kbId: { type: [String, Number], required: true },
  refreshTrigger: { type: Number, default: 0 },
  canEdit: { type: Boolean, default: true }
})

const route = useRoute()
const router = useRouter()

const treeData = ref([])
const loading = ref(false)

// 过滤掉嵌套组件笔记（可选，如果用户希望所有笔记都显示在树中，可以简化此逻辑）
const filteredTreeData = computed(() => {
  // 目前用户要求创建的表格、画板都是独立的笔记，应当在树中显示
  // 因此我们不再过滤带有 parentDocId 的组件
  return treeData.value
})

const activeNoteId = computed(() => Number(route.params.docId) || null)

async function loadTree() {
  loading.value = true
  try {
    const data = await getKbDetail(props.kbId)
    treeData.value = data.docTree ?? data.noteTree ?? [] // 兼容两种字段
  } catch (e) {
    const status = e.response?.status
    if (status === 403) {
      router.replace('/dashboard')
    } else {
      console.error('加载笔记树失败', e)
    }
  } finally {
    loading.value = false
  }
}

function selectNote(node) {
  router.push(`/note/${node.id}?kbId=${props.kbId}&type=${node.type}`)
}

watch(() => props.kbId, loadTree, { immediate: true })
watch(
  () => props.refreshTrigger,
  () => {
    if (props.refreshTrigger > 0) {
      loadTree()
    }
  }
)
</script>

<style scoped>
.note-tree {
  padding: 0 8px;
}
.loading, .empty {
  padding: 20px;
  text-align: center;
  color: #999;
  font-size: 13px;
}
.tree-root {
  list-style: none;
  padding: 0;
  margin: 0;
}
</style>