import { request } from '@/lib/request'
import type { LoginRequest, LoginResponse, UserInfo } from '@/types/user'

export const authApi = {
  login: (data: LoginRequest): Promise<LoginResponse> => request.post('/admin/auth/login', data),
  getUserInfo: (): Promise<UserInfo> => request.get('/admin/auth/current'),
  logout: (): Promise<void> => request.post('/admin/auth/logout'),
}
