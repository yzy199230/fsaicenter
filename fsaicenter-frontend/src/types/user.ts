export interface LoginRequest {
  username: string
  password: string
}

export interface LoginResponse {
  userId: number
  username: string
  realName: string
  avatar: string
  tokenName: string
  tokenValue: string
}

export interface UserInfo {
  userId: number
  username: string
  realName: string
  email?: string
  phone?: string
  avatar?: string
  status: number
  lastLoginTime?: string
  lastLoginIp?: string
}

export interface AdminUser {
  id: number
  username: string
  realName: string
  email?: string
  phone?: string
  avatar?: string
  status: number
  lastLoginTime?: string
  lastLoginIp?: string
  createdTime: string
  updatedTime: string
  roles: AdminRole[]
  roleIds: number[]
}

export interface AdminRole {
  id: number
  roleCode: string
  roleName: string
  description?: string
  sortOrder: number
  status: number
  createdTime: string
  updatedTime: string
  permissionIds: number[]
}

export interface CreateAdminUserRequest {
  username: string
  password: string
  realName?: string
  email?: string
  phone?: string
  status?: number
  roleIds?: number[]
}

export interface UpdateAdminUserRequest {
  realName?: string
  email?: string
  phone?: string
  avatar?: string
  status?: number
  roleIds?: number[]
}

export interface CreateAdminRoleRequest {
  roleCode: string
  roleName: string
  description?: string
  sortOrder?: number
  status?: number
  permissionIds?: number[]
}

export interface UpdateAdminRoleRequest {
  roleName?: string
  description?: string
  sortOrder?: number
  status?: number
  permissionIds?: number[]
}

export interface AdminPermission {
  id: number
  parentId: number
  permissionCode: string
  permissionName: string
  permissionType: string
  permissionPath?: string
  icon?: string
  sortOrder: number
  status: number
  createdTime: string
  children?: AdminPermission[]
}
