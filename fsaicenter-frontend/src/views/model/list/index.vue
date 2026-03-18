<template>
  <div class="model-list">
    <a-card>
      <div class="toolbar">
        <a-space>
          <a-input-search v-model="searchKeyword" placeholder="搜索模型" style="width: 200px" @search="fetchData" />
          <a-select v-model="filterType" placeholder="模型类型" style="width: 140px" allow-clear @change="fetchData">
            <a-option value="chat">Chat</a-option>
            <a-option value="embedding">Embedding</a-option>
            <a-option value="image">Image</a-option>
            <a-option value="asr">ASR</a-option>
            <a-option value="tts">TTS</a-option>
            <a-option value="video">Video</a-option>
            <a-option value="image_recognition">Image Recognition</a-option>
          </a-select>
          <a-select v-model="filterProvider" placeholder="提供商" style="width: 140px" allow-clear @change="fetchData">
            <a-option v-for="p in providers" :key="p.id" :value="p.id">{{ p.name }}</a-option>
          </a-select>
        </a-space>
        <a-button type="primary" @click="handleCreate">
          <template #icon><icon-plus /></template>
          添加模型
        </a-button>
      </div>

      <a-table :columns="columns" :data="tableData" :loading="loading" :pagination="{ pageSize: 10, showTotal: true }">
        <template #type="{ record }">
          <a-tag>{{ record.type }}</a-tag>
        </template>
        <template #status="{ record }">
          <a-tag :color="record.status === 1 ? 'green' : 'red'">
            {{ record.status === 1 ? '启用' : '禁用' }}
          </a-tag>
        </template>
        <template #price="{ record }">
          <div v-if="record.config">
            <div>temp: {{ record.config.temperature ?? '-' }}</div>
            <div>maxTokens: {{ record.config.maxTokens ?? '-' }}</div>
          </div>
          <span v-else>-</span>
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

    <a-modal v-model:visible="modalVisible" :title="modalTitle" @ok="handleSubmit" :ok-loading="submitLoading" width="640px">
      <a-form :model="formData" layout="vertical">
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="模型代码" required v-if="!currentEditId">
              <a-input v-model="formData.code" placeholder="如 gpt-4o" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="模型名称" required>
              <a-input v-model="formData.name" placeholder="如 GPT-4o" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="模型类型" required>
              <a-select v-model="formData.type">
                <a-option value="chat">Chat</a-option>
                <a-option value="embedding">Embedding</a-option>
                <a-option value="image">Image</a-option>
                <a-option value="asr">ASR</a-option>
                <a-option value="tts">TTS</a-option>
                <a-option value="video">Video</a-option>
                <a-option value="image_recognition">Image Recognition</a-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="提供商" required>
              <a-select v-model="formData.providerId">
                <a-option v-for="p in providers" :key="p.id" :value="p.id">{{ p.name }}</a-option>
              </a-select>
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="支持流式">
              <a-switch v-model="formData.supportStream" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="最大Token限制">
              <a-input-number v-model="formData.maxTokenLimit" :min="0" style="width: 100%" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-form-item label="描述">
          <a-textarea v-model="formData.description" :max-length="500" />
        </a-form-item>
        <a-divider>模型配置</a-divider>
        <a-row :gutter="16">
          <a-col :span="8">
            <a-form-item label="Temperature">
              <a-input-number v-model="configData.temperature" :min="0" :max="2" :step="0.1" style="width: 100%" />
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item label="Max Tokens">
              <a-input-number v-model="configData.maxTokens" :min="0" style="width: 100%" />
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item label="Top P">
              <a-input-number v-model="configData.topP" :min="0" :max="1" :step="0.1" style="width: 100%" />
            </a-form-item>
          </a-col>
        </a-row>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { Message } from '@arco-design/web-vue'
import { IconPlus } from '@arco-design/web-vue/es/icon'
import { modelApi, providerApi } from '@/api/model'
import type { AiModel, CreateModelRequest, UpdateModelRequest, Provider } from '@/types/model'

const columns = [
  { title: 'ID', dataIndex: 'id', width: 60 },
  { title: '模型名称', dataIndex: 'name' },
  { title: '模型代码', dataIndex: 'code' },
  { title: '提供商', dataIndex: 'providerName' },
  { title: '类型', slotName: 'type', width: 100 },
  { title: '配置', slotName: 'price', width: 150 },
  { title: '状态', slotName: 'status', width: 80 },
  { title: '操作', slotName: 'actions', width: 180 }
]

const tableData = ref<AiModel[]>([])
const providers = ref<Provider[]>([])
const loading = ref(false)
const searchKeyword = ref('')
const filterType = ref<string | undefined>(undefined)
const filterProvider = ref<number | undefined>(undefined)
const modalVisible = ref(false)
const modalTitle = ref('添加模型')
const submitLoading = ref(false)
const currentEditId = ref<number>()

const formData = reactive<CreateModelRequest>({
  code: '',
  name: '',
  type: 'chat',
  providerId: 0,
  supportStream: true,
  maxTokenLimit: undefined,
  description: ''
})

const configData = reactive({
  temperature: 0.7,
  maxTokens: undefined as number | undefined,
  topP: 1.0
})

const fetchData = async () => {
  loading.value = true
  try {
    const params: Record<string, any> = {}
    if (searchKeyword.value) params.keyword = searchKeyword.value
    if (filterType.value) params.modelType = filterType.value
    if (filterProvider.value) params.providerId = filterProvider.value
    tableData.value = await modelApi.getList(params)
  } finally {
    loading.value = false
  }
}

const fetchProviders = async () => {
  try {
    providers.value = await providerApi.getAll()
  } catch (error) {
    console.error('获取提供商失败:', error)
  }
}

const handleCreate = () => {
  modalTitle.value = '添加模型'
  currentEditId.value = undefined
  Object.assign(formData, { code: '', name: '', type: 'chat', providerId: providers.value[0]?.id || 0, supportStream: true, maxTokenLimit: undefined, description: '' })
  Object.assign(configData, { temperature: 0.7, maxTokens: undefined, topP: 1.0 })
  modalVisible.value = true
}

const handleEdit = (record: AiModel) => {
  modalTitle.value = '编辑模型'
  currentEditId.value = record.id
  Object.assign(formData, { code: record.code, name: record.name, type: record.type, providerId: record.providerId, supportStream: record.supportStream, maxTokenLimit: record.maxTokenLimit, description: record.description || '' })
  Object.assign(configData, { temperature: record.config?.temperature ?? 0.7, maxTokens: record.config?.maxTokens, topP: record.config?.topP ?? 1.0 })
  modalVisible.value = true
}

const handleSubmit = async () => {
  if (!formData.name) { Message.warning('请输入模型名称'); return }
  if (!currentEditId.value && !formData.code) { Message.warning('请输入模型代码'); return }
  submitLoading.value = true
  try {
    const config = { temperature: configData.temperature, maxTokens: configData.maxTokens, topP: configData.topP }
    if (currentEditId.value) {
      const data: UpdateModelRequest = { name: formData.name, config, supportStream: formData.supportStream, maxTokenLimit: formData.maxTokenLimit, description: formData.description }
      await modelApi.update(currentEditId.value, data)
      Message.success('更新成功')
    } else {
      await modelApi.create({ ...formData, config })
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

const handleToggleStatus = async (record: AiModel) => {
  try {
    await modelApi.updateStatus(record.id)
    Message.success('状态更新成功')
    fetchData()
  } catch (error) {
    console.error('状态更新失败:', error)
  }
}

const handleDelete = async (record: AiModel) => {
  try {
    await modelApi.delete(record.id)
    Message.success('删除成功')
    fetchData()
  } catch (error) {
    console.error('删除失败:', error)
  }
}

onMounted(() => {
  fetchProviders()
  fetchData()
})
</script>

<style scoped>
.model-list { padding: 20px; }
.toolbar { display: flex; justify-content: space-between; margin-bottom: 16px; }
</style>
