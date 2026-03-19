import { request } from '@/lib/request'
import type { AdminRole, CreateAdminRoleRequest, UpdateAdminRoleRequest, AdminPermission } from '@/types/user'

export const adminRoleApi = {
  getList: (params?: Record<string, any>): Promise<AdminRole[]> => request.get('/admin/roles', { params }),
  getAll: (): Promise<AdminRole[]> => request.get('/admin/roles/all'),
  getById: (id: number): Promise<AdminRole> => request.get(`/admin/roles/${id}`),
  create: (data: CreateAdminRoleRequest): Promise<number> => request.post('/admin/roles', data),
  update: (id: number, data: UpdateAdminRoleRequest): Promise<void> => request.put(`/admin/roles/${id}`, data),
  delete: (id: number): Promise<void> => request.delete(`/admin/roles/${id}`),
  toggleStatus: (id: number): Promise<void> => request.put(`/admin/roles/${id}/status`),
  assignPermissions: (id: number, permissionIds: number[]): Promise<void> => request.put(`/admin/roles/${id}/permissions`, permissionIds),
}

export const permissionApi = {
  getTree: (): Promise<AdminPermission[]> => request.get('/admin/permissions/tree'),
  getAll: (): Promise<AdminPermission[]> => request.get('/admin/permissions'),
  getByRole: (roleId: number): Promise<AdminPermission[]> => request.get(`/admin/permissions/role/${roleId}`),
}
