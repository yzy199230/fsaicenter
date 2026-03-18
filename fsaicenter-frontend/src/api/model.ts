import { request } from '@/utils/request'
import type { AiModel, CreateModelRequest, UpdateModelRequest, Provider, CreateProviderRequest, UpdateProviderRequest } from '@/types/model'

export const modelApi = {
  getList(params?: Record<string, any>): Promise<AiModel[]> {
    return request.get('/admin/models', { params })
  },
  getById(id: number): Promise<AiModel> {
    return request.get(`/admin/models/${id}`)
  },
  create(data: CreateModelRequest): Promise<number> {
    return request.post('/admin/models', data)
  },
  update(id: number, data: UpdateModelRequest): Promise<void> {
    return request.put(`/admin/models/${id}`, data)
  },
  delete(id: number): Promise<void> {
    return request.delete(`/admin/models/${id}`)
  },
  updateStatus(id: number): Promise<void> {
    return request.put(`/admin/models/${id}/status`)
  }
}

export const providerApi = {
  getList(): Promise<Provider[]> {
    return request.get('/admin/providers')
  },
  getAll(): Promise<Provider[]> {
    return request.get('/admin/providers/all')
  },
  getById(id: number): Promise<Provider> {
    return request.get(`/admin/providers/${id}`)
  },
  create(data: CreateProviderRequest): Promise<number> {
    return request.post('/admin/providers', data)
  },
  update(id: number, data: UpdateProviderRequest): Promise<void> {
    return request.put(`/admin/providers/${id}`, data)
  },
  delete(id: number): Promise<void> {
    return request.delete(`/admin/providers/${id}`)
  },
  toggleStatus(id: number): Promise<void> {
    return request.put(`/admin/providers/${id}/status`)
  }
}
