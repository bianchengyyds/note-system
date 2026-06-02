<template>
  <div class="embed-edit-block" :class="[`embed-type-${embedType}`, { 'embed-inline': inline }]">
    <div v-if="loading" class="embed-edit-body loading">加载中...</div>
    <div v-else-if="error" class="embed-edit-body error">{{ error }}</div>

    <div v-else class="embed-edit-body">
      <template v-if="embedType === 'table'">
        <div 
          class="table-toolbar"
          @mousedown.stop
          @click.stop
        >
          <el-button size="small" @click="addRow">+ 行</el-button>
          <el-button size="small" @click="addCol">+ 列</el-button>
          <el-button size="small" type="danger" @click="deleteRow">- 行</el-button>
          <el-button size="small" type="danger" @click="deleteCol">- 列</el-button>
        </div>
        <div 
          class="table-wrapper" 
          @mousedown.stop 
          @mouseup.stop 
          @click.stop
          contenteditable="false"
        >
          <VueJSpreadsheet
            v-if="tableReady"
            :key="tableKey"
            v-model="tableFlat"
            :config="tableConfig"
          />
          <p v-else class="empty-hint">暂无数据行</p>
        </div>
      </template>

      <template v-else-if="embedType === 'board'">
        <div 
          class="board-toolbar"
          @mousedown.stop
          @click.stop
        >
          <el-button size="small" :type="tool === 'select' ? 'primary' : ''" @click="setTool('select')">选择</el-button>
          <el-button size="small" :type="tool === 'pen' ? 'primary' : ''" @click="setTool('pen')">画笔</el-button>
          <el-button size="small" :type="tool === 'rect' ? 'primary' : ''" @click="setTool('rect')">矩形</el-button>
          <el-button size="small" :type="tool === 'circle' ? 'primary' : ''" @click="setTool('circle')">圆形</el-button>
          <el-button size="small" :type="tool === 'line' ? 'primary' : ''" @click="setTool('line')">线条</el-button>
          <el-button size="small" :type="tool === 'text' ? 'primary' : ''" @click="setTool('text')">文本</el-button>
          <el-color-picker v-model="strokeColor" size="small" />
          <el-color-picker v-model="bgColor" size="small" show-alpha @active-change="bgColor = $event" />
          <el-button size="small" type="danger" @click="deleteSelected">删除选中</el-button>
        </div>
        <div 
          class="board-canvas-wrap" 
          ref="boardWrapRef"
          contenteditable="false"
          @mousedown.stop
          @mouseup.stop
          @click.stop
          @contextmenu.stop
        >
          <canvas ref="canvasRef" class="embed-board-canvas" tabindex="0"></canvas>
        </div>
      </template>

      <template v-else-if="embedType === 'mind'">
        <MindNodeEditor
          v-for="(node, idx) in mindNodes"
          :key="node.id || idx"
          :node="node"
          :depth="0"
        />
        <el-button v-if="!mindNodes.length" size="small" @click="initMindRoot">添加中心主题</el-button>
      </template>
    </div>
  </div>
</template>