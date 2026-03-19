import { request } from '@/lib/request'
import type { AdminUser, CreateAdminUserRequest, UpdateAdminUserRequest } from '@/types/user'
import type { PageResult } from '@/types/common'

export const adminUserApi = {
  getPage: (params: Record<string, any>): Promise<PageResult<AdminUser>> => request.get('/admin/users', { params }),
  getById: (id: number): Promise<AdminUser> => request.get(`/admin/users/${id}`),
  create: (data: CreateAdminUserRequest): Promise<number> => request.post('/admin/users', data),
  update: (id: number, data: UpdateAdminUserRequest): Promise<void> => request.put(`/admin/users/${id}`, data),
  delete: (id: number): Promise<void> => request.delete(`/admin/users/${id}`),
  toggleStatus: (id: number): Promise<void> => request.put(`/admin/users/${id}/status`),
  resetPassword: (id: number, newPassword: string): Promise<void> => request.put(`/admin/users/${id}/password`, JSON.stringify(newPassword), { headers: { 'Content-Type': 'application/json' } }),
  assignRoles: (id: number, roleIds: number[]): Promise<void> => request.put(`/admin/users/${id}/roles`, roleIds),
}
