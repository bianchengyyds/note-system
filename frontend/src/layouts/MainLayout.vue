<template>
  <div class="app-layout">
    <!-- 左侧边栏 -->
    <aside class="app-sidebar">
      <Sidebar />
    </aside>

    <!-- 右侧主内容区 -->
    <main class="app-main">
      <router-view />
    </main>
  </div>
</template>

<script setup>
import Sidebar from '@/components/layout/Sidebar.vue'
</script>

<style scoped>
.app-layout {
  display: flex;
  height: 100vh;
  overflow: hidden;
}

.app-sidebar {
  width: 260px;
  min-width: 260px;
  background: #f7f8fa;
  border-right: 1px solid #e5e6eb;
  display: flex;
  flex-direction: column;
  overflow-y: auto;
}

.app-main {
  flex: 1;
  overflow-y: auto;
  background: #fff;
}
</style>