import { request } from '@/utils/request'
import type { AdminRole, CreateAdminRoleRequest, UpdateAdminRoleRequest, AdminPermission } from '@/types/user'

export const adminRoleApi = {
  getList(params?: Record<string, any>): Promise<AdminRole[]> {
    return request.get('/admin/roles', { params })
  },
  getAll(): Promise<AdminRole[]> {
    return request.get('/admin/roles/all')
  },
  getById(id: number): Promise<AdminRole> {
    return request.get(`/admin/roles/${id}`)
  },
  create(data: CreateAdminRoleRequest): Promise<number> {
    return request.post('/admin/roles', data)
  },
  update(id: number, data: UpdateAdminRoleRequest): Promise<void> {
    return request.put(`/admin/roles/${id}`, data)
  },
  delete(id: number): Promise<void> {
    return request.delete(`/admin/roles/${id}`)
  },
  toggleStatus(id: number): Promise<void> {
    return request.put(`/admin/roles/${id}/status`)
  },
  assignPermissions(id: number, permissionIds: number[]): Promise<void> {
    return request.put(`/admin/roles/${id}/permissions`, permissionIds)
  }
}

export const permissionApi = {
  getTree(): Promise<AdminPermission[]> {
    return request.get('/admin/permissions/tree')
  },
  getAll(): Promise<AdminPermission[]> {
    return request.get('/admin/permissions')
  },
  getByRole(roleId: number): Promise<AdminPermission[]> {
    return request.get(`/admin/permissions/role/${roleId}`)
  }
}
