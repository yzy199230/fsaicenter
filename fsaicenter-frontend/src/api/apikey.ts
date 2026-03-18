import { request } from '@/utils/request'
import type { ApiKey, CreateApiKeyRequest, UpdateApiKeyRequest, ApiKeyStatistics } from '@/types/apikey'

export const apiKeyApi = {
  getList(): Promise<ApiKey[]> {
    return request.get('/admin/apikeys')
  },
  getById(id: number): Promise<ApiKey> {
    return request.get(`/admin/apikeys/${id}`)
  },
  create(data: CreateApiKeyRequest): Promise<number> {
    return request.post('/admin/apikeys', data)
  },
  update(id: number, data: UpdateApiKeyRequest): Promise<void> {
    return request.put(`/admin/apikeys/${id}`, data)
  },
  delete(id: number): Promise<void> {
    return request.delete(`/admin/apikeys/${id}`)
  },
  updateStatus(id: number, status: number): Promise<void> {
    return request.put(`/admin/apikeys/${id}/status`, { status })
  },
  resetQuota(id: number): Promise<void> {
    return request.put(`/admin/apikeys/${id}/quota/reset`)
  },
  getStatistics(id: number): Promise<ApiKeyStatistics> {
    return request.get(`/admin/apikeys/${id}/statistics`)
  }
}
