import { request } from '@/utils/request'
import type { LoginRequest, LoginResponse, UserInfo } from '@/types/user'

export const authApi = {
  login(data: LoginRequest): Promise<LoginResponse> {
    return request.post('/admin/auth/login', data)
  },
  getUserInfo(): Promise<UserInfo> {
    return request.get('/admin/auth/current')
  },
  logout(): Promise<void> {
    return request.post('/admin/auth/logout')
  }
}
