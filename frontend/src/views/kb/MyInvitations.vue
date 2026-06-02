<template>
  <div class="invitations-page">
    <div class="page-header">
      <el-button type="primary" @click="$router.push('/dashboard')">
        <el-icon><HomeFilled /></el-icon> 返回首页
      </el-button>
    </div>
    <PendingInvitations />
  </div>
</template>

<script setup>
import PendingInvitations from '@/components/kb/PendingInvitations.vue'
</script>

<style scoped>
.invitations-page {
  max-width: 900px;
  margin: 0 auto;
  padding: 32px 48px;
}

.page-header {
  margin-bottom: 20px;
}
</style>
