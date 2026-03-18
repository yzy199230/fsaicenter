<template>
  <div class="apikey-list">
    <a-card>
      <div class="toolbar">
        <a-space>
          <a-input-search
            v-model="searchKeyword"
            placeholder="搜索 Key 名称"
            style="width: 250px"
            @search="handleSearch"
          />
          <a-select
            v-model="filterStatus"
            placeholder="状态"
            style="width: 120px"
            allow-clear
            @change="handleSearch"
          >
            <a-option :value="1">启用</a-option>
            <a-option :value="0">禁用</a-option>
          </a-select>
        </a-space>
        <a-button type="primary" @click="handleCreate">
          <template #icon><icon-plus /></template>
          创建 API Key
        </a-button>
      </div>

      <a-table
        :columns="columns"
        :data="filteredData"
        :loading="loading"
        :pagination="{ pageSize: 10, showTotal: true, showPageSize: true }"
      >
        <template #status="{ record }">
          <a-tag :color="record.status === 1 ? 'green' : 'red'">
            {{ record.status === 1 ? '启用' : '禁用' }}
          </a-tag>
        </template>

        <template #quota="{ record }">
          <template v-if="record.quotaTotal === -1">
            <a-tag color="blue">无限制</a-tag>
          </template>
          <template v-else>
            <a-progress
              :percent="record.quotaTotal > 0 ? (record.quotaUsed / record.quotaTotal) * 100 : 0"
              :status="record.quotaRemaining <= 0 ? 'danger' : 'normal'"
              size="small"
            />
            <div style="margin-top: 4px; font-size: 12px; color: #86909c;">
              {{ record.quotaUsed }} / {{ record.quotaTotal }}
            </div>
          </template>
        </template>

        <template #actions="{ record }">
          <a-space>
            <a-button type="text" size="small" @click="handleToggleStatus(record)">
              {{ record.status === 1 ? '禁用' : '启用' }}
            </a-button>
            <a-button type="text" size="small" @click="handleResetQuota(record)">
              重置配额
            </a-button>
            <a-button type="text" size="small" @click="handleEdit(record)">
              编辑
            </a-button>
            <a-popconfirm content="确定删除此 API Key 吗？" @ok="handleDelete(record)">
              <a-button type="text" size="small" status="danger">
                删除
              </a-button>
            </a-popconfirm>
          </a-space>
        </template>
      </a-table>
    </a-card>

    <a-modal
      v-model:visible="modalVisible"
      :title="modalTitle"
      @ok="handleSubmit"
      @cancel="modalVisible = false"
      :ok-loading="submitLoading"
    >
      <a-form :model="formData" layout="vertical">
        <a-form-item label="Key 名称" required>
          <a-input v-model="formData.keyName" placeholder="请输入 Key 名称" />
        </a-form-item>

        <a-form-item label="描述">
          <a-textarea v-model="formData.description" placeholder="请输入描述" :max-length="200" />
        </a-form-item>

        <a-form-item label="总配额" required>
          <a-input-number v-model="formData.quotaTotal" :min="-1" style="width: 100%" placeholder="-1 表示不限制" />
          <div style="font-size: 12px; color: #86909c; margin-top: 4px;">-1 表示无限制</div>
        </a-form-item>

        <a-form-item label="每分钟速率限制">
          <a-input-number v-model="formData.rateLimitPerMinute" :min="1" style="width: 100%" />
        </a-form-item>

        <a-form-item label="每天速率限制">
          <a-input-number v-model="formData.rateLimitPerDay" :min="1" style="width: 100%" />
        </a-form-item>

        <a-form-item label="过期时间">
          <a-date-picker v-model="formData.expireTime" show-time style="width: 100%" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { Message } from '@arco-design/web-vue'
import { IconPlus } from '@arco-design/web-vue/es/icon'
import { apiKeyApi } from '@/api/apikey'
import type { ApiKey, CreateApiKeyRequest } from '@/types/apikey'

const columns = [
  { title: 'ID', dataIndex: 'id', width: 80 },
  { title: 'Key 名称', dataIndex: 'keyName' },
  { title: 'Key 值', dataIndex: 'keyValue', ellipsis: true, tooltip: true },
  { title: '状态', slotName: 'status', width: 80 },
  { title: '配额使用', slotName: 'quota', width: 180 },
  { title: '速率(次/分)', dataIndex: 'rateLimitPerMinute', width: 110 },
  { title: '创建时间', dataIndex: 'createdTime', width: 180 },
  { title: '操作', slotName: 'actions', width: 260 }
]

const allData = ref<ApiKey[]>([])
const loading = ref(false)
const searchKeyword = ref('')
const filterStatus = ref<number | undefined>(undefined)
const modalVisible = ref(false)
const modalTitle = ref('创建 API Key')
const submitLoading = ref(false)
const currentEditId = ref<number>()

const formData = reactive<CreateApiKeyRequest>({
  keyName: '',
  description: '',
  quotaTotal: 1000000,
  rateLimitPerMinute: 60,
  rateLimitPerDay: undefined,
  expireTime: undefined
})

const filteredData = computed(() => {
  return allData.value.filter(item => {
    const matchKeyword = !searchKeyword.value || item.keyName.toLowerCase().includes(searchKeyword.value.toLowerCase())
    const matchStatus = filterStatus.value === undefined || item.status === filterStatus.value
    return matchKeyword && matchStatus
  })
})

const fetchData = async () => {
  loading.value = true
  try {
    allData.value = await apiKeyApi.getList()
  } catch (error) {
    console.error('获取数据失败:', error)
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  // filteredData is computed, no action needed
}

const handleCreate = () => {
  modalTitle.value = '创建 API Key'
  currentEditId.value = undefined
  Object.assign(formData, {
    keyName: '',
    description: '',
    quotaTotal: 1000000,
    rateLimitPerMinute: 60,
    rateLimitPerDay: undefined,
    expireTime: undefined
  })
  modalVisible.value = true
}

const handleEdit = (record: ApiKey) => {
  modalTitle.value = '编辑 API Key'
  currentEditId.value = record.id
  Object.assign(formData, {
    keyName: record.keyName,
    description: record.description || '',
    quotaTotal: record.quotaTotal,
    rateLimitPerMinute: record.rateLimitPerMinute,
    rateLimitPerDay: record.rateLimitPerDay,
    expireTime: record.expireTime
  })
  modalVisible.value = true
}

const handleSubmit = async () => {
  if (!formData.keyName) {
    Message.warning('请输入 Key 名称')
    return
  }
  submitLoading.value = true
  try {
    if (currentEditId.value) {
      await apiKeyApi.update(currentEditId.value, formData)
      Message.success('更新成功')
    } else {
      await apiKeyApi.create(formData)
      Message.success('创建成功')
    }
    modalVisible.value = false
    fetchData()
  } catch (error) {
    console.error('操作失败:', error)
  } finally {
    submitLoading.value = false
  }
}

const handleToggleStatus = async (record: ApiKey) => {
  try {
    const newStatus = record.status === 1 ? 0 : 1
    await apiKeyApi.updateStatus(record.id, newStatus)
    Message.success('状态更新成功')
    fetchData()
  } catch (error) {
    console.error('状态更新失败:', error)
  }
}

const handleResetQuota = async (record: ApiKey) => {
  try {
    await apiKeyApi.resetQuota(record.id)
    Message.success('配额重置成功')
    fetchData()
  } catch (error) {
    console.error('配额重置失败:', error)
  }
}

const handleDelete = async (record: ApiKey) => {
  try {
    await apiKeyApi.delete(record.id)
    Message.success('删除成功')
    fetchData()
  } catch (error) {
    console.error('删除失败:', error)
  }
}

onMounted(fetchData)
</script>

<style scoped>
.apikey-list {
  padding: 20px;
}
.toolbar {
  display: flex;
  justify-content: space-between;
  margin-bottom: 16px;
}
</style>
