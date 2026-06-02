<template>
  <div class="table-editor-wrapper">
    <v-grid
      :source="tableRows"
      :columns="tableColumns"
      :resize="true"
      :rowHeaders="false"
      theme="material"
      @afteredit="handleCellEdit"
    >
    </v-grid>
  </div>
</template>

<script setup>
import { ref, toRefs, watch } from 'vue'
import VGrid from '@revolist/vue3-datagrid'

const props = defineProps({
  modelValue: {
    type: Object,
    required: true
  }
})

const emit = defineEmits(['update:modelValue'])

// 数据源
const tableRows = ref([])

// 根据数据动态生成列定义
const tableColumns = ref([
  { prop: 'A', name: 'A', size: 120 },
  { prop: 'B', name: 'B', size: 120 },
  { prop: 'C', name: 'C', size: 120 },
])

// 数据变更时更新
const handleCellEdit = (e) => {
  emit('update:modelValue', {
    columns: tableColumns.value.map(col => ({
      key: col.prop,
      title: col.name
    })),
    rows: tableRows.value
  })
}

// 初始化数据
const initData = () => {
  const data = props.modelValue
  if (data?.rows && data?.columns) {
    tableColumns.value = data.columns.map((col, idx) => ({
      prop: col.key || col.title,
      name: col.title,
      size: 120
    }))
    tableRows.value = data.rows.map(row => {
      const newRow = {}
      data.columns.forEach(col => {
        newRow[col.key || col.title] = row[col.key] || ''
      })
      return newRow
    })
  }
}

watch(() => props.modelValue, initData, { immediate: true })
</script>

<style scoped>
.table-editor-wrapper {
  height: 600px;
  border: 1px solid #e5e6eb;
  border-radius: 4px;
}
</style>