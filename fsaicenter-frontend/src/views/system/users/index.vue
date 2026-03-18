<template>
  <div class="user-management">
    <a-card>
      <div class="toolbar">
        <a-space>
          <a-input-search v-model="queryParams.keyword" placeholder="搜索用户名/姓名" style="width: 250px" @search="handleSearch" />
          <a-select v-model="queryParams.status" placeholder="状态" style="width: 120px" allow-clear @change="handleSearch">
            <a-option :value="1">启用</a-option>
            <a-option :value="0">禁用</a-option>
          </a-select>
        </a-space>
        <a-button type="primary" @click="handleCreate">
          <template #icon><icon-plus /></template>
          创建用户
        </a-button>
      </div>

      <a-table :columns="columns" :data="tableData" :loading="loading" :pagination="pagination" @page-change="handlePageChange" @page-size-change="handlePageSizeChange">
        <template #status="{ record }">
          <a-tag :color="record.status === 1 ? 'green' : 'red'">{{ record.status === 1 ? '启用' : '禁用' }}</a-tag>
        </template>
        <template #roles="{ record }">
          <a-space wrap>
            <a-tag v-for="role in record.roles" :key="role.id" color="arcoblue">{{ role.roleName }}</a-tag>
            <span v-if="!record.roles?.length" style="color: #86909c">无角色</span>
          </a-space>
        </template>
        <template #actions="{ record }">
          <a-space>
            <a-button type="text" size="small" @click="handleToggleStatus(record)">
              {{ record.status === 1 ? '禁用' : '启用' }}
            </a-button>
            <a-button type="text" size="small" @click="handleEdit(record)">编辑</a-button>
            <a-button type="text" size="small" @click="handleResetPassword(record)">重置密码</a-button>
            <a-popconfirm content="确定删除吗？" @ok="handleDelete(record)">
              <a-button type="text" size="small" status="danger">删除</a-button>
            </a-popconfirm>
          </a-space>
        </template>
      </a-table>
    </a-card>

    <!-- Create/Edit Modal -->
    <a-modal v-model:visible="modalVisible" :title="modalTitle" @ok="handleSubmit" :ok-loading="submitLoading">
      <a-form :model="formData" layout="vertical">
        <a-form-item label="用户名" required v-if="!currentEditId">
          <a-input v-model="formData.username" placeholder="请输入用户名" />
        </a-form-item>
        <a-form-item label="密码" required v-if="!currentEditId">
          <a-input-password v-model="formData.password" placeholder="请输入密码" />
        </a-form-item>
        <a-form-item label="真实姓名">
          <a-input v-model="formData.realName" placeholder="请输入真实姓名" />
        </a-form-item>
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="邮箱">
              <a-input v-model="formData.email" placeholder="请输入邮箱" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="手机">
              <a-input v-model="formData.phone" placeholder="请输入手机号" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-form-item label="角色">
          <a-select v-model="formData.roleIds" multiple placeholder="请选择角色">
            <a-option v-for="role in allRoles" :key="role.id" :value="role.id">{{ role.roleName }}</a-option>
          </a-select>
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- Reset Password Modal -->
    <a-modal v-model:visible="passwordModalVisible" title="重置密码" @ok="handlePasswordSubmit" :ok-loading="submitLoading">
      <a-form layout="vertical">
        <a-form-item label="新密码" required>
          <a-input-password v-model="newPassword" placeholder="请输入新密码（6-20位）" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { Message } from '@arco-design/web-vue'
import { IconPlus } from '@arco-design/web-vue/es/icon'
import { adminUserApi } from '@/api/user'
import { adminRoleApi } from '@/api/role'
import type { AdminUser, AdminRole, CreateAdminUserRequest, UpdateAdminUserRequest } from '@/types/user'

const columns = [
  { title: 'ID', dataIndex: 'id', width: 60 },
  { title: '用户名', dataIndex: 'username', width: 120 },
  { title: '真实姓名', dataIndex: 'realName', width: 120 },
  { title: '邮箱', dataIndex: 'email', ellipsis: true },
  { title: '手机', dataIndex: 'phone', width: 130 },
  { title: '角色', slotName: 'roles', width: 200 },
  { title: '状态', slotName: 'status', width: 80 },
  { title: '最后登录', dataIndex: 'lastLoginTime', width: 170 },
  { title: '操作', slotName: 'actions', width: 260 }
]

const tableData = ref<AdminUser[]>([])
const allRoles = ref<AdminRole[]>([])
const loading = ref(false)
const queryParams = reactive({ keyword: '', status: undefined as number | undefined, pageNum: 1, pageSize: 10 })
const pagination = reactive({ current: 1, pageSize: 10, total: 0, showTotal: true, showPageSize: true })
const modalVisible = ref(false)
const modalTitle = ref('创建用户')
const submitLoading = ref(false)
const currentEditId = ref<number>()
const passwordModalVisible = ref(false)
const passwordUserId = ref<number>()
const newPassword = ref('')

const formData = reactive<CreateAdminUserRequest & UpdateAdminUserRequest>({
  username: '',
  password: '',
  realName: '',
  email: '',
  phone: '',
  roleIds: []
})

const fetchData = async () => {
  loading.value = true
  try {
    const res = await adminUserApi.getPage({ ...queryParams })
    tableData.value = res.list
    pagination.total = res.total
    pagination.current = res.pageNum
  } finally {
    loading.value = false
  }
}

const fetchRoles = async () => {
  try {
    allRoles.value = await adminRoleApi.getAll()
  } catch (e) {
    console.error('获取角色失败:', e)
  }
}

const handleSearch = () => { queryParams.pageNum = 1; pagination.current = 1; fetchData() }
const handlePageChange = (page: number) => { queryParams.pageNum = page; pagination.current = page; fetchData() }
const handlePageSizeChange = (size: number) => { queryParams.pageSize = size; queryParams.pageNum = 1; pagination.pageSize = size; pagination.current = 1; fetchData() }

const handleCreate = () => {
  modalTitle.value = '创建用户'
  currentEditId.value = undefined
  Object.assign(formData, { username: '', password: '', realName: '', email: '', phone: '', roleIds: [] })
  modalVisible.value = true
}

const handleEdit = (record: AdminUser) => {
  modalTitle.value = '编辑用户'
  currentEditId.value = record.id
  Object.assign(formData, { username: record.username, password: '', realName: record.realName || '', email: record.email || '', phone: record.phone || '', roleIds: record.roleIds || [] })
  modalVisible.value = true
}

const handleSubmit = async () => {
  submitLoading.value = true
  try {
    if (currentEditId.value) {
      const data: UpdateAdminUserRequest = { realName: formData.realName, email: formData.email, phone: formData.phone, roleIds: formData.roleIds }
      await adminUserApi.update(currentEditId.value, data)
      Message.success('更新成功')
    } else {
      if (!formData.username || !formData.password) { Message.warning('请填写用户名和密码'); submitLoading.value = false; return }
      await adminUserApi.create(formData as CreateAdminUserRequest)
      Message.success('创建成功')
    }
    modalVisible.value = false
    fetchData()
  } catch (e) {
    console.error('操作失败:', e)
  } finally {
    submitLoading.value = false
  }
}

const handleToggleStatus = async (record: AdminUser) => {
  try {
    await adminUserApi.toggleStatus(record.id)
    Message.success('状态更新成功')
    fetchData()
  } catch (e) {
    console.error('操作失败:', e)
  }
}

const handleResetPassword = (record: AdminUser) => {
  passwordUserId.value = record.id
  newPassword.value = ''
  passwordModalVisible.value = true
}

const handlePasswordSubmit = async () => {
  if (!newPassword.value || newPassword.value.length < 6) { Message.warning('密码至少6位'); return }
  submitLoading.value = true
  try {
    await adminUserApi.resetPassword(passwordUserId.value!, newPassword.value)
    Message.success('密码重置成功')
    passwordModalVisible.value = false
  } catch (e) {
    console.error('重置密码失败:', e)
  } finally {
    submitLoading.value = false
  }
}

const handleDelete = async (record: AdminUser) => {
  try {
    await adminUserApi.delete(record.id)
    Message.success('删除成功')
    fetchData()
  } catch (e) {
    console.error('删除失败:', e)
  }
}

onMounted(() => { fetchRoles(); fetchData() })
</script>

<style scoped>
.user-management { padding: 20px; }
.toolbar { display: flex; justify-content: space-between; margin-bottom: 16px; }
</style>
