<template>
  <div class="admin-page">
    <h2>用户管理</h2>

    <!-- 搜索栏 -->
    <el-row :gutter="16" class="search-bar">
      <el-col :span="6">
        <el-input v-model="searchParams.keyword" placeholder="用户名/邮箱/手机" @keyup.enter="fetchData" clearable />
      </el-col>
      <el-col :span="4">
        <el-select v-model="searchParams.status" placeholder="状态">
          <el-option label="全部" value="all" />
          <el-option label="正常" value="1" />
          <el-option label="禁用" value="0" />
        </el-select>
      </el-col>
      <el-col :span="4">
        <el-select v-model="searchParams.role" placeholder="角色">
          <el-option label="全部" value="all" />
          <el-option label="用户" value="USER" />
          <el-option label="管理员" value="ADMIN" />
        </el-select>
      </el-col>
      <el-col :span="4">
        <el-button type="primary" @click="fetchData">搜索</el-button>
      </el-col>
    </el-row>

    <!-- 用户表格 -->
    <el-table :data="userList" v-loading="loading" border class="mt-16">
      <el-table-column label="序号" width="60">
        <template #default="{ $index }">
          {{ ($index + 1) + (searchParams.page - 1) * searchParams.size }}
        </template>
      </el-table-column>
      <el-table-column prop="name" label="用户名" />
      <el-table-column prop="email" label="邮箱" show-overflow-tooltip />
      <el-table-column prop="phone" label="手机号" show-overflow-tooltip />
      <el-table-column prop="gender" label="性别" width="80">
        <template #default="{ row }">
          {{ row.gender === 1 ? '男' : row.gender === 2 ? '女' : '未知' }}
        </template>
      </el-table-column>
      <el-table-column prop="role" label="角色" width="80" />
      <el-table-column prop="status" label="状态" width="80">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'danger'">
            {{ row.status === 1 ? '正常' : '禁用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" label="注册时间" width="160" />
      <el-table-column label="操作" width="220">
        <template #default="{ row }">
          <el-button size="small" @click="openEdit(row)">编辑</el-button>
          <el-button size="small" type="danger" @click="handleDelete(row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 分页 -->
    <div class="pagination-wrapper">
      <span class="total-count">共 {{ total }} 个用户</span>
      <el-pagination
        v-model:current-page="searchParams.page"
        :page-size="searchParams.size"
        :total="total"
        layout="prev, pager, next"
        @current-change="fetchData"
      />
    </div>

    <!-- 编辑用户对话框 -->
    <el-dialog v-model="editDialogVisible" title="编辑用户" width="500px">
      <el-form :model="editForm" label-width="80px">
        <el-form-item label="用户名">
          <el-input v-model="editForm.name" />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="editForm.email" />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="editForm.phone" />
        </el-form-item>
        <el-form-item label="性别">
          <el-radio-group v-model="editForm.gender">
            <el-radio :label="0">未知</el-radio>
            <el-radio :label="1">男</el-radio>
            <el-radio :label="2">女</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="editForm.role">
            <el-option label="用户" value="USER" />
            <el-option label="管理员" value="ADMIN" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="editForm.status" :active-value="1" :inactive-value="0" active-text="正常" inactive-text="禁用" />
        </el-form-item>
        <el-form-item label="头像URL">
          <el-input v-model="editForm.avatar" />
        </el-form-item>
        <el-form-item label="简介">
          <el-input v-model="editForm.bio" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveEdit">保存</el-button>
        <el-button type="warning" @click="resetPassword">重置密码</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getUsers, getUserDetail, updateUser, deleteUser, resetUserPassword } from '@/api/admin'

const loading = ref(false)
const userList = ref([])
const total = ref(0)

const searchParams = reactive({
  keyword: '',
  status: 'all',
  role: 'all',
  page: 1,
  size: 10,
  sortBy: 'created_at',
  sortOrder: 'desc'
})

const editDialogVisible = ref(false)
const saving = ref(false)
const currentEditId = ref(null)
const editForm = reactive({
  name: '',
  email: '',
  phone: '',
  gender: 0,
  role: 'USER',
  status: 1,
  avatar: '',
  bio: ''
})

async function fetchData() {
  loading.value = true
  try {
    const res = await getUsers(searchParams)
    userList.value = res.records
    total.value = res.total
  } catch (e) {
    ElMessage.error('获取用户列表失败')
  } finally {
    loading.value = false
  }
}

async function openEdit(row) {
  currentEditId.value = row.id
  // ✅ 先重置表单，避免Object.assign不覆盖null值导致显示上一个用户的数据
  Object.assign(editForm, {
    name: '',
    email: '',
    phone: '',
    gender: 0,
    role: 'USER',
    status: 1,
    avatar: '',
    bio: ''
  })
  try {
    const detail = await getUserDetail(row.id)
    Object.assign(editForm, detail)
    editDialogVisible.value = true
  } catch (e) {
    ElMessage.error('获取用户详情失败')
  }
}

async function saveEdit() {
  saving.value = true
  try {
    await updateUser(currentEditId.value, { ...editForm })
    ElMessage.success('更新成功')
    editDialogVisible.value = false
    fetchData()
  } catch (e) {
    ElMessage.error('更新失败')
  } finally {
    saving.value = false
  }
}

async function handleDelete(userId) {
  try {
    await ElMessageBox.confirm('确定删除该用户吗？此操作不可恢复。', '警告', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await deleteUser(userId)
    ElMessage.success('删除成功')
    fetchData()
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

async function resetPassword() {
  try {
    const { value } = await ElMessageBox.prompt('请输入新密码', '重置密码', {
      confirmButtonText: '确定',
      cancelButtonText: '取消'
    })
    await resetUserPassword(currentEditId.value, { newPassword: value })
    ElMessage.success('密码已重置')
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error('重置失败')
    }
  }
}

onMounted(fetchData)
</script>

<style scoped>
.admin-page {
  max-width: 1200px;
  margin: 0 auto;
  padding: 32px 48px;
}
.search-bar { margin-bottom: 16px; }
.mt-16 { margin-top: 16px; }
.pagination-wrapper {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 16px;
}
.total-count {
  color: #666;
  font-size: 14px;
}
</style>