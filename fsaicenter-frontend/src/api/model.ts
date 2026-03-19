import { request } from '@/lib/request'
import type {
  AiModel, CreateModelRequest, UpdateModelRequest,
  Provider, CreateProviderRequest, UpdateProviderRequest,
  ModelApiKey, CreateModelApiKeyRequest, UpdateModelApiKeyRequest,
} from '@/types/model'

export const modelApi = {
  getList: (params?: Record<string, any>): Promise<AiModel[]> => request.get('/admin/models', { params }),
  getById: (id: number): Promise<AiModel> => request.get(`/admin/models/${id}`),
  create: (data: CreateModelRequest): Promise<number> => request.post('/admin/models', data),
  update: (id: number, data: UpdateModelRequest): Promise<void> => request.put(`/admin/models/${id}`, data),
  delete: (id: number): Promise<void> => request.delete(`/admin/models/${id}`),
  updateStatus: (id: number): Promise<void> => request.put(`/admin/models/${id}/status`),
}

export const modelApiKeyApi = {
  list: (modelId: number): Promise<ModelApiKey[]> => request.get(`/admin/models/${modelId}/keys`),
  getById: (modelId: number, keyId: number): Promise<ModelApiKey> => request.get(`/admin/models/${modelId}/keys/${keyId}`),
  create: (modelId: number, data: CreateModelApiKeyRequest): Promise<ModelApiKey> => request.post(`/admin/models/${modelId}/keys`, data),
  update: (modelId: number, keyId: number, data: UpdateModelApiKeyRequest): Promise<void> => request.put(`/admin/models/${modelId}/keys/${keyId}`, data),
  delete: (modelId: number, keyId: number): Promise<void> => request.delete(`/admin/models/${modelId}/keys/${keyId}`),
  toggleStatus: (modelId: number, keyId: number): Promise<void> => request.put(`/admin/models/${modelId}/keys/${keyId}/status`),
  resetHealth: (modelId: number, keyId: number): Promise<void> => request.put(`/admin/models/${modelId}/keys/${keyId}/health/reset`),
}

export const providerApi = {
  getList: (): Promise<Provider[]> => request.get('/admin/providers'),
  getAll: (): Promise<Provider[]> => request.get('/admin/providers/all'),
  getById: (id: number): Promise<Provider> => request.get(`/admin/providers/${id}`),
  create: (data: CreateProviderRequest): Promise<number> => request.post('/admin/providers', data),
  update: (id: number, data: UpdateProviderRequest): Promise<void> => request.put(`/admin/providers/${id}`, data),
  delete: (id: number): Promise<void> => request.delete(`/admin/providers/${id}`),
  toggleStatus: (id: number): Promise<void> => request.put(`/admin/providers/${id}/status`),
}
