import { request } from '@/lib/request'
import type { ApiKey, CreateApiKeyRequest, UpdateApiKeyRequest, ApiKeyStatistics } from '@/types/apikey'

export const apiKeyApi = {
  getList: (): Promise<ApiKey[]> => request.get('/admin/apikeys'),
  getById: (id: number): Promise<ApiKey> => request.get(`/admin/apikeys/${id}`),
  create: (data: CreateApiKeyRequest): Promise<number> => request.post('/admin/apikeys', data),
  update: (id: number, data: UpdateApiKeyRequest): Promise<void> => request.put(`/admin/apikeys/${id}`, data),
  delete: (id: number): Promise<void> => request.delete(`/admin/apikeys/${id}`),
  updateStatus: (id: number, status: number): Promise<void> => request.put(`/admin/apikeys/${id}/status`, { status }),
  resetQuota: (id: number): Promise<void> => request.put(`/admin/apikeys/${id}/quota/reset`),
  getStatistics: (id: number): Promise<ApiKeyStatistics> => request.get(`/admin/apikeys/${id}/statistics`),
}
