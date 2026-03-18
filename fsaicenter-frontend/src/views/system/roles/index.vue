<template>
  <div class="role-management">
    <a-card>
      <div class="toolbar">
        <a-input-search v-model="searchKeyword" placeholder="搜索角色" style="width: 250px" @search="handleSearch" />
        <a-button type="primary" @click="handleCreate">
          <template #icon><icon-plus /></template>
          创建角色
        </a-button>
      </div>

      <a-table :columns="columns" :data="filteredData" :loading="loading" :pagination="{ pageSize: 10, showTotal: true }">
        <template #status="{ record }">
          <a-tag :color="record.status === 1 ? 'green' : 'red'">{{ record.status === 1 ? '启用' : '禁用' }}</a-tag>
        </template>
        <template #actions="{ record }">
          <a-space>
            <a-button type="text" size="small" @click="handleToggleStatus(record)">
              {{ record.status === 1 ? '禁用' : '启用' }}
            </a-button>
            <a-button type="text" size="small" @click="handleEdit(record)">编辑</a-button>
            <a-button type="text" size="small" @click="handlePermissions(record)">权限</a-button>
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
        <a-form-item label="角色编码" required v-if="!currentEditId">
          <a-input v-model="formData.roleCode" placeholder="如 admin" />
        </a-form-item>
        <a-form-item label="角色名称" required>
          <a-input v-model="formData.roleName" placeholder="如 管理员" />
        </a-form-item>
        <a-form-item label="描述">
          <a-textarea v-model="formData.description" :max-length="200" />
        </a-form-item>
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="排序">
              <a-input-number v-model="formData.sortOrder" :min="0" style="width: 100%" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="状态">
              <a-select v-model="formData.status">
                <a-option :value="1">启用</a-option>
                <a-option :value="0">禁用</a-option>
              </a-select>
            </a-form-item>
          </a-col>
        </a-row>
      </a-form>
    </a-modal>

    <!-- Permission Assignment Modal -->
    <a-modal v-model:visible="permModalVisible" title="分配权限" @ok="handlePermSubmit" :ok-loading="submitLoading" width="500px">
      <a-tree
        v-model:checked-keys="selectedPermIds"
        :data="permissionTree"
        :field-names="{ key: 'id', title: 'permissionName', children: 'children' }"
        checkable
        :default-expand-all="true"
      />
      <a-empty v-if="permissionTree.length === 0" description="暂无权限数据" />
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { Message } from '@arco-design/web-vue'
import { IconPlus } from '@arco-design/web-vue/es/icon'
import { adminRoleApi, permissionApi } from '@/api/role'
import type { AdminRole, CreateAdminRoleRequest, UpdateAdminRoleRequest, AdminPermission } from '@/types/user'

const columns = [
  { title: 'ID', dataIndex: 'id', width: 60 },
  { title: '角色编码', dataIndex: 'roleCode', width: 130 },
  { title: '角色名称', dataIndex: 'roleName', width: 130 },
  { title: '描述', dataIndex: 'description', ellipsis: true },
  { title: '排序', dataIndex: 'sortOrder', width: 80 },
  { title: '状态', slotName: 'status', width: 80 },
  { title: '操作', slotName: 'actions', width: 230 }
]

const allData = ref<AdminRole[]>([])
const loading = ref(false)
const searchKeyword = ref('')
const modalVisible = ref(false)
const modalTitle = ref('创建角色')
const submitLoading = ref(false)
const currentEditId = ref<number>()
const permModalVisible = ref(false)
const permissionTree = ref<AdminPermission[]>([])
const selectedPermIds = ref<number[]>([])
const permRoleId = ref<number>()

const formData = reactive<CreateAdminRoleRequest>({
  roleCode: '',
  roleName: '',
  description: '',
  sortOrder: 0,
  status: 1
})

const filteredData = computed(() => {
  if (!searchKeyword.value) return allData.value
  const kw = searchKeyword.value.toLowerCase()
  return allData.value.filter(r => r.roleName.toLowerCase().includes(kw) || r.roleCode.toLowerCase().includes(kw))
})

const fetchData = async () => {
  loading.value = true
  try {
    allData.value = await adminRoleApi.getList()
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {}

const handleCreate = () => {
  modalTitle.value = '创建角色'
  currentEditId.value = undefined
  Object.assign(formData, { roleCode: '', roleName: '', description: '', sortOrder: 0, status: 1 })
  modalVisible.value = true
}

const handleEdit = (record: AdminRole) => {
  modalTitle.value = '编辑角色'
  currentEditId.value = record.id
  Object.assign(formData, { roleCode: record.roleCode, roleName: record.roleName, description: record.description || '', sortOrder: record.sortOrder, status: record.status })
  modalVisible.value = true
}

const handleSubmit = async () => {
  if (!formData.roleName) { Message.warning('请输入角色名称'); return }
  submitLoading.value = true
  try {
    if (currentEditId.value) {
      const data: UpdateAdminRoleRequest = { roleName: formData.roleName, description: formData.description, sortOrder: formData.sortOrder, status: formData.status }
      await adminRoleApi.update(currentEditId.value, data)
      Message.success('更新成功')
    } else {
      if (!formData.roleCode) { Message.warning('请输入角色编码'); submitLoading.value = false; return }
      await adminRoleApi.create(formData)
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

const handleToggleStatus = async (record: AdminRole) => {
  try {
    await adminRoleApi.toggleStatus(record.id)
    Message.success('状态更新成功')
    fetchData()
  } catch (e) {
    console.error('操作失败:', e)
  }
}

const handlePermissions = async (record: AdminRole) => {
  permRoleId.value = record.id
  selectedPermIds.value = record.permissionIds || []
  try {
    permissionTree.value = await permissionApi.getTree()
  } catch (e) {
    console.error('获取权限树失败:', e)
  }
  permModalVisible.value = true
}

const handlePermSubmit = async () => {
  submitLoading.value = true
  try {
    await adminRoleApi.assignPermissions(permRoleId.value!, selectedPermIds.value)
    Message.success('权限分配成功')
    permModalVisible.value = false
    fetchData()
  } catch (e) {
    console.error('权限分配失败:', e)
  } finally {
    submitLoading.value = false
  }
}

const handleDelete = async (record: AdminRole) => {
  try {
    await adminRoleApi.delete(record.id)
    Message.success('删除成功')
    fetchData()
  } catch (e) {
    console.error('删除失败:', e)
  }
}

onMounted(fetchData)
</script>

<style scoped>
.role-management { padding: 20px; }
.toolbar { display: flex; justify-content: space-between; margin-bottom: 16px; }
</style>
