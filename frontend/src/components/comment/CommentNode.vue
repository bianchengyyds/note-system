<template>
  <div class="comment-node">
    <!-- 评论头部 -->
    <div class="comment-header">
      <span class="comment-user">{{ comment.userName }}</span>
      <span class="comment-time">{{ formatDate(comment.createdAt) }}</span>
    </div>

    <!-- 评论内容 -->
    <div class="comment-content">{{ comment.content }}</div>

    <!-- 操作按钮 -->
    <div class="comment-actions">
      <el-button
        text
        size="small"
        :icon="comment.hasLiked ? 'StarFilled' : 'Star'"
        @click="$emit('toggle-like', comment)"
      >
        {{ comment.likeCount || 0 }}
      </el-button>
      <el-button text size="small" @click="startReply">回复</el-button>
      <el-button
        v-if="comment.userId === currentUserId"
        text
        size="small"
        type="danger"
        @click="$emit('delete-comment', comment.id)"
      >删除</el-button>
    </div>

    <!-- 回复输入框 -->
    <div v-if="showReplyInput" class="reply-input">
      <p class="reply-hint">回复 <strong>{{ comment.userName }}</strong>：</p>
      <el-input v-model="replyContent" placeholder="输入回复内容..." size="small" />
      <el-button size="small" type="primary" @click="submitReply">回复</el-button>
      <el-button size="small" @click="cancelReply">取消</el-button>
    </div>

    <!-- 递归子评论 -->
    <div v-if="comment.replies && comment.replies.length" class="sub-comments">
      <CommentNode
        v-for="reply in comment.replies"
        :key="reply.id"
        :comment="reply"
        :currentUserId="currentUserId"
        @toggle-like="$emit('toggle-like', $event)"
        @delete-comment="$emit('delete-comment', $event)"
        @reply-posted="$emit('reply-posted', $event)"
      />
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { ElMessage } from 'element-plus'

// ✅ 关键：手动导入自身，确保递归解析
import CommentNode from './CommentNode.vue'

const props = defineProps({
  comment: { type: Object, required: true },
  currentUserId: { type: [Number, String], required: true }
})

const emit = defineEmits(['toggle-like', 'delete-comment', 'reply-posted'])

const showReplyInput = ref(false)
const replyContent = ref('')

function startReply() {
  showReplyInput.value = true
  replyContent.value = ''
}
function cancelReply() {
  showReplyInput.value = false
}
function submitReply() {
  if (!replyContent.value.trim()) {
    ElMessage.warning('请输入回复内容')
    return
  }
  emit('reply-posted', {
    parentCommentId: props.comment.id,
    content: replyContent.value
  })
  showReplyInput.value = false
  replyContent.value = ''
}
function formatDate(dateStr) {
  if (!dateStr) return ''
  return new Date(dateStr).toLocaleString('zh-CN')
}
</script>

<style scoped>
.comment-node {
  padding: 12px 0;
  border-bottom: 1px solid #f0f0f0;
}
.comment-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 6px;
}
.comment-user {
  font-weight: 500;
  color: #1f2937;
}
.comment-time {
  font-size: 12px;
  color: #999;
}
.comment-content {
  margin-left: 4px;
  line-height: 1.6;
}
.comment-actions {
  display: flex;
  gap: 8px;
  margin-top: 6px;
}
.reply-input {
  margin-top: 8px;
  margin-left: 20px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.reply-input .el-input,
.reply-input .el-button {
  max-width: 300px;
}
.reply-hint {
  margin: 0;
  font-size: 13px;
  color: #666;
}
.sub-comments {
  margin-left: 24px;
  border-left: 2px solid #e5e6eb;
  padding-left: 16px;
  margin-top: 8px;
}
</style>