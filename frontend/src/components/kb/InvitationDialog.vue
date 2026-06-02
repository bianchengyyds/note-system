<template>
  <el-dialog
    v-model="visible"
    title="邀请成员"
    width="500px"
    @close="handleClose"
  >
    <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
      <el-form-item label="用户" prop="userId">
        <el-select
          v-model="form.userId"
          filterable
          remote
          reserve-keyword
          placeholder="搜索用户名"
          :remote-method="searchUsersRemote"
          :loading="userSearchLoading"
          style="width: 100%"
        >
          <el-option
            v-for="user in userOptions"
            :key="user.id"
            :label="user.name"
            :value="user.id"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="角色" prop="role">
        <el-select v-model="form.role" style="width: 100%">
          <el-option label="管理员" value="ADMIN" />
          <el-option label="编辑者" value="EDITOR" />
          <el-option label="查看者" value="VIEWER" />
        </el-select>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" :loading="sending" @click="handleSend">发送邀请</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { sendKbInvitation } from '@/api/kb'
import { searchUsers } from '@/api/user'

const props = defineProps({
  kbId: {
    type: [String, Number],
    required: true
  },
  modelValue: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['update:modelValue', 'success'])

const visible = ref(props.modelValue)

watch(
  () => props.modelValue,
  (val) => { visible.value = val }
)

watch(visible, (val) => {
  emit('update:modelValue', val)
})

const formRef = ref(null)
const form = ref({
  userId: null,
  role: 'EDITOR'
})

const rules = {
  userId: [{ required: true, message: '请选择用户', trigger: 'change' }],
  role: [{ required: true, message: '请选择角色', trigger: 'change' }]
}

const userOptions = ref([])
const userSearchLoading = ref(false)
const sending = ref(false)

async function searchUsersRemote(keyword) {
  if (!keyword) {
    userOptions.value = []
    return
  }
  userSearchLoading.value = true
  try {
    const res = await searchUsers(keyword)
    userOptions.value = Array.isArray(res) ? res : (res.records || [])
  } catch (e) {
    userOptions.value = []
  } finally {
    userSearchLoading.value = false
  }
}

async function handleSend() {
  if (!formRef.value) return
  try {
    await formRef.value.validate()
  } catch {
    return
  }

  sending.value = true
  try {
    await sendKbInvitation(props.kbId, {
      userId: form.value.userId,
      role: form.value.role
    })
    ElMessage.success('邀请已发送')
    visible.value = false
    emit('success')
  } catch (e) {
    ElMessage.error('发送邀请失败')
  } finally {
    sending.value = false
  }
}

function handleClose() {
  formRef.value?.resetFields()
  userOptions.value = []
}
</script>
