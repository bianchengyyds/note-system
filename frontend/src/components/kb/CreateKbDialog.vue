<template>
  <el-dialog
    v-model="visible"
    title="新建知识库"
    width="480px"
    :close-on-click-modal="false"
    @close="resetForm"
  >
    <el-form
      ref="formRef"
      :model="form"
      :rules="rules"
      label-position="top"
    >
      <el-form-item label="名称" prop="name">
        <el-input
          v-model="form.name"
          placeholder="知识库名称"
          maxlength="30"
          show-word-limit
        />
      </el-form-item>

      <el-form-item label="描述" prop="description">
        <el-input
          v-model="form.description"
          type="textarea"
          placeholder="简单描述知识库内容（可选）"
          maxlength="200"
          show-word-limit
          :rows="3"
        />
      </el-form-item>

      <el-form-item label="封面" prop="coverUrl">
        <el-input
          v-model="form.coverUrl"
          placeholder="封面图片 URL（可选）"
        />
      </el-form-item>

      <el-form-item label="权限" prop="isPublic">
        <el-radio-group v-model="form.isPublic">
          <el-radio :label="1">公开</el-radio>
          <el-radio :label="0">私有</el-radio>
        </el-radio-group>
        <div class="form-tip">公开后其他人可以搜索和查看</div>
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" :loading="loading" @click="submit">
        创建
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, reactive, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { createKb } from '@/api/kb'

const props = defineProps({
  visible: {
    type: Boolean,
    required: true
  }
})
const emit = defineEmits(['update:visible', 'created'])

const formRef = ref(null)
const loading = ref(false)

const form = reactive({
  name: '',
  description: '',
  coverUrl: '',
  isPublic: 1
})

const rules = {
  name: [
    { required: true, message: '请输入知识库名称', trigger: 'blur' },
    { min: 1, max: 30, message: '长度在 1 到 30 个字符', trigger: 'blur' }
  ]
}

// 同步 visible 到内部（也可直接用 computed + emit，这里用 watch 保持清晰）
const visible = ref(props.visible)
watch(() => props.visible, (val) => { visible.value = val })
watch(visible, (val) => { emit('update:visible', val) })

function resetForm() {
  form.name = ''
  form.description = ''
  form.coverUrl = ''
  form.isPublic = 1
  formRef.value?.clearValidate()
}

async function submit() {
  if (!formRef.value) return
  try {
    await formRef.value.validate()
  } catch {
    return
  }

  loading.value = true
  try {
    await createKb({
      name: form.name,
      description: form.description,
      coverUrl: form.coverUrl,
      isPublic: form.isPublic
    })
    ElMessage.success('知识库创建成功')
    visible.value = false
    resetForm()
    emit('created')  // 通知父组件刷新列表
    window.dispatchEvent(new CustomEvent('kb-list-updated'))
  } catch (error) {
    ElMessage.error(error.message || '创建失败')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.form-tip {
  font-size: 12px;
  color: #999;
  margin-top: 4px;
}
</style>