<template>
  <div class="provider-list">
    <a-card>
      <div class="toolbar">
        <a-input-search v-model="searchKeyword" placeholder="搜索提供商" style="width: 250px" @search="handleSearch" />
        <a-button type="primary" @click="handleCreate">
          <template #icon><icon-plus /></template>
          添加提供商
        </a-button>
      </div>

      <a-table :columns="columns" :data="filteredData" :loading="loading" :pagination="{ pageSize: 10, showTotal: true }">
        <template #type="{ record }">
          <a-tag :color="record.type === 'remote' ? 'blue' : 'green'">{{ record.type === 'remote' ? '远程' : '本地' }}</a-tag>
        </template>
        <template #status="{ record }">
          <a-tag :color="record.status === 1 ? 'green' : 'red'">{{ record.status === 1 ? '启用' : '禁用' }}</a-tag>
        </template>
        <template #actions="{ record }">
          <a-space>
            <a-button type="text" size="small" @click="handleToggleStatus(record)">
              {{ record.status === 1 ? '禁用' : '启用' }}
            </a-button>
            <a-button type="text" size="small" @click="handleEdit(record)">编辑</a-button>
            <a-popconfirm content="确定删除吗？" @ok="handleDelete(record)">
              <a-button type="text" size="small" status="danger">删除</a-button>
            </a-popconfirm>
          </a-space>
        </template>
      </a-table>
    </a-card>

    <a-modal v-model:visible="modalVisible" :title="modalTitle" @ok="handleSubmit" :ok-loading="submitLoading" width="700px">
      <a-form :model="formData" layout="vertical">
        <a-row :gutter="16">
          <a-col :span="8">
            <a-form-item label="编码" required v-if="!currentEditId">
              <a-input v-model="formData.code" placeholder="如 openai" />
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item label="名称" required>
              <a-input v-model="formData.name" placeholder="如 OpenAI" />
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item label="类型" required>
              <a-select v-model="formData.type">
                <a-option value="remote">远程</a-option>
                <a-option value="local">本地</a-option>
              </a-select>
            </a-form-item>
          </a-col>
        </a-row>
        <a-form-item label="Base URL">
          <a-input v-model="formData.baseUrl" placeholder="https://api.openai.com" />
        </a-form-item>
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="Chat 端点">
              <a-input v-model="formData.chatEndpoint" placeholder="/v1/chat/completions" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="Embedding 端点">
              <a-input v-model="formData.embeddingEndpoint" placeholder="/v1/embeddings" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="Image 端点">
              <a-input v-model="formData.imageEndpoint" placeholder="/v1/images/generations" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="协议类型">
              <a-input v-model="formData.protocolType" placeholder="openai" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-divider>认证配置</a-divider>
        <a-row :gutter="16">
          <a-col :span="8">
            <a-form-item label="认证类型">
              <a-select v-model="formData.authType" allow-clear>
                <a-option value="bearer">Bearer</a-option>
                <a-option value="api-key">API Key</a-option>
                <a-option value="custom">Custom</a-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item label="认证头">
              <a-input v-model="formData.authHeader" placeholder="Authorization" />
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item label="认证前缀">
              <a-input v-model="formData.authPrefix" placeholder="Bearer " />
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="需要 API Key">
              <a-switch v-model="formData.apiKeyRequired" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="排序">
              <a-input-number v-model="formData.sortOrder" :min="0" style="width: 100%" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-form-item label="描述">
          <a-textarea v-model="formData.description" :max-length="500" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { Message } from '@arco-design/web-vue'
import { IconPlus } from '@arco-design/web-vue/es/icon'
import { providerApi } from '@/api/model'
import type { Provider, CreateProviderRequest } from '@/types/model'

const columns = [
  { title: 'ID', dataIndex: 'id', width: 60 },
  { title: '编码', dataIndex: 'code', width: 120 },
  { title: '名称', dataIndex: 'name' },
  { title: '类型', slotName: 'type', width: 80 },
  { title: 'Base URL', dataIndex: 'baseUrl', ellipsis: true },
  { title: '状态', slotName: 'status', width: 80 },
  { title: '操作', slotName: 'actions', width: 180 }
]

const allData = ref<Provider[]>([])
const loading = ref(false)
const searchKeyword = ref('')
const modalVisible = ref(false)
const modalTitle = ref('添加提供商')
const submitLoading = ref(false)
const currentEditId = ref<number>()

const formData = reactive<CreateProviderRequest>({
  code: '',
  name: '',
  type: 'remote',
  baseUrl: '',
  protocolType: '',
  chatEndpoint: '',
  embeddingEndpoint: '',
  imageEndpoint: '',
  authType: 'bearer',
  authHeader: 'Authorization',
  authPrefix: 'Bearer ',
  apiKeyRequired: true,
  description: '',
  sortOrder: 0
})

const filteredData = computed(() => {
  if (!searchKeyword.value) return allData.value
  const kw = searchKeyword.value.toLowerCase()
  return allData.value.filter(p => p.name.toLowerCase().includes(kw) || p.code.toLowerCase().includes(kw))
})

const fetchData = async () => {
  loading.value = true
  try {
    allData.value = await providerApi.getList()
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {}

const handleCreate = () => {
  modalTitle.value = '添加提供商'
  currentEditId.value = undefined
  Object.assign(formData, { code: '', name: '', type: 'remote', baseUrl: '', protocolType: '', chatEndpoint: '', embeddingEndpoint: '', imageEndpoint: '', authType: 'bearer', authHeader: 'Authorization', authPrefix: 'Bearer ', apiKeyRequired: true, description: '', sortOrder: 0 })
  modalVisible.value = true
}

const handleEdit = (record: Provider) => {
  modalTitle.value = '编辑提供商'
  currentEditId.value = record.id
  Object.assign(formData, {
    code: record.code, name: record.name, type: record.type,
    baseUrl: record.baseUrl || '', protocolType: record.protocolType || '',
    chatEndpoint: record.chatEndpoint || '', embeddingEndpoint: record.embeddingEndpoint || '',
    imageEndpoint: record.imageEndpoint || '',
    authType: record.authType || '', authHeader: record.authHeader || '',
    authPrefix: record.authPrefix || '', apiKeyRequired: record.apiKeyRequired ?? true,
    description: record.description || '', sortOrder: record.sortOrder
  })
  modalVisible.value = true
}

const handleSubmit = async () => {
  if (!formData.name) { Message.warning('请输入名称'); return }
  submitLoading.value = true
  try {
    if (currentEditId.value) {
      const { code, ...updateData } = formData
      await providerApi.update(currentEditId.value, updateData)
      Message.success('更新成功')
    } else {
      if (!formData.code) { Message.warning('请输入编码'); submitLoading.value = false; return }
      await providerApi.create(formData)
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

const handleToggleStatus = async (record: Provider) => {
  try {
    await providerApi.toggleStatus(record.id)
    Message.success('状态更新成功')
    fetchData()
  } catch (error) {
    console.error('状态更新失败:', error)
  }
}

const handleDelete = async (record: Provider) => {
  try {
    await providerApi.delete(record.id)
    Message.success('删除成功')
    fetchData()
  } catch (error) {
    console.error('删除失败:', error)
  }
}

onMounted(fetchData)
</script>

<style scoped>
.provider-list { padding: 20px; }
.toolbar { display: flex; justify-content: space-between; margin-bottom: 16px; }
</style>
