<template>
  <div class="mind-tree-node" :class="{ 'is-root': depth === 0 }">
    <div class="node-main">
      <!-- 节点内容区域 -->
      <div class="node-box" :class="{ 'has-children': node.children?.length > 0 }">
        <template v-if="!readonly">
          <el-input
            v-model="node.title"
            size="small"
            class="mind-input"
            :placeholder="depth === 0 ? '思维导图' : '新建节点'"
            @change="$emit('update')"
          />
        </template>
        <div v-else class="mind-text-view">{{ node.title || '无标题' }}</div>
        
        <div class="node-actions" v-if="!readonly">
          <el-button size="small" circle @click="addChild">
            <el-icon><Plus /></el-icon>
          </el-button>
          <el-button v-if="depth > 0" size="small" circle @click="$emit('delete')">
            <el-icon><Delete /></el-icon>
          </el-button>
        </div>
      </div>
      
      <!-- 右侧连接线 (父节点发出) -->
      <div class="connector-right" v-if="node.children?.length > 0"></div>
    </div>

    <!-- 子节点容器 -->
    <div class="children-list" v-if="node.children?.length > 0">
      <div 
        v-for="(child, idx) in node.children" 
        :key="child.id || idx"
        class="child-item-wrapper"
      >
        <!-- 每一项左侧的弯曲连线 -->
        <div class="curve-connector" :class="getCurveClass(idx, node.children.length)"></div>
        
        <MindNodeEditor
          :node="child"
          :depth="depth + 1"
          :readonly="readonly"
          @update="$emit('update')"
          @delete="removeChild(idx)"
        />
      </div>
    </div>
  </div>
</template>

<script setup>
import { Plus, Delete } from '@element-plus/icons-vue'

const props = defineProps({
  node: { type: Object, required: true },
  depth: { type: Number, default: 0 },
  readonly: { type: Boolean, default: false }
})

const emit = defineEmits(['update', 'delete'])

function addChild() {
  if (!props.node.children) props.node.children = []
  props.node.children.push({
    id: `node_${Date.now()}`,
    title: '新建节点',
    children: []
  })
  emit('update')
}

function removeChild(idx) {
  props.node.children.splice(idx, 1)
  emit('update')
}

// 根据索引判断连线弯曲方向
function getCurveClass(index, total) {
  if (total === 1) return 'curve-middle'
  if (index === 0) return 'curve-top'
  if (index === total - 1) return 'curve-bottom'
  return 'curve-middle'
}
</script>

<style scoped>
.mind-tree-node {
  display: flex;
  align-items: center;
  position: relative;
}

.node-main {
  display: flex;
  align-items: center;
  position: relative;
  z-index: 10;
}

.node-box {
  display: flex;
  align-items: center;
  padding: 4px 12px;
  border-radius: 4px;
  transition: background 0.2s;
}

.node-box:hover {
  background: rgba(0, 0, 0, 0.04);
}

/* 根节点样式 - 灰色圆角矩形 */
.is-root > .node-main .node-box {
  background: #eeeeee;
  padding: 12px 24px;
  border-radius: 6px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.05);
}

.is-root > .node-main .node-box :deep(.el-input__inner) {
  font-size: 18px;
  font-weight: 500;
  color: #333;
}

/* 子节点样式 - 仅文字 */
.mind-input {
  width: auto;
  min-width: 60px;
}

.mind-text-view {
  font-size: 14px;
  color: #333;
  line-height: 1.5;
  padding: 2px 0;
}

.is-root > .node-main .mind-text-view {
  font-size: 18px;
  font-weight: 500;
}

:deep(.el-input__inner) {
  border: none !important;
  background: transparent !important;
  padding: 0 !important;
  height: auto !important;
  line-height: 1.5 !important;
  box-shadow: none !important;
  color: #333;
  font-size: 14px;
}

.node-actions {
  display: flex;
  gap: 4px;
  margin-left: 10px;
  opacity: 0;
  transition: opacity 0.2s;
}

.node-box:hover .node-actions {
  opacity: 1;
}

.node-actions .el-button--small {
  padding: 4px;
  height: 20px;
  width: 20px;
}

/* 布局控制 */
.children-list {
  display: flex;
  flex-direction: column;
  padding-left: 50px;
  position: relative;
}

.child-item-wrapper {
  display: flex;
  align-items: center;
  position: relative;
  padding: 8px 0;
}

/* 连线系统 - 使用颜色循环 */
.child-item-wrapper:nth-child(3n+1) { --line-color: #a38cf4; }
.child-item-wrapper:nth-child(3n+2) { --line-color: #7687f1; }
.child-item-wrapper:nth-child(3n+3) { --line-color: #6dc8d3; }

/* 根节点右侧发出的中心线 */
.connector-right {
  position: absolute;
  right: -50px;
  top: 50%;
  width: 50px;
  height: 2px;
  background: var(--line-color, #b4bccc);
  z-index: 1;
}

/* 弯曲连线核心逻辑 */
.curve-connector {
  position: absolute;
  left: -50px;
  width: 50px;
  border-color: var(--line-color, #b4bccc);
  border-style: solid;
  border-width: 0;
  pointer-events: none;
}

/* 顶部子节点：向上弯曲 */
.curve-top {
  top: 50%;
  height: 100%;
  border-left-width: 2px;
  border-top-width: 2px;
  border-top-left-radius: 12px;
  transform: translateY(-2px);
}

/* 底部子节点：向下弯曲 */
.curve-bottom {
  bottom: 50%;
  height: 100%;
  border-left-width: 2px;
  border-bottom-width: 2px;
  border-bottom-left-radius: 12px;
  transform: translateY(2px);
}

/* 中间子节点：水平直连 */
.curve-middle {
  top: 50%;
  width: 50px;
  height: 2px;
  background: var(--line-color, #b4bccc);
}

/* 修正：只有一个子节点时不需要竖线 */
.child-item-wrapper:only-child .curve-connector {
  height: 2px;
  border: none;
  background: var(--line-color, #b4bccc);
  top: 50%;
}

</style>
