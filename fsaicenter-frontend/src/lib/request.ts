import axios from 'axios'
import type { AxiosInstance, AxiosRequestConfig, AxiosResponse } from 'axios'
import type { Result } from '@/types/common'
import { useAuthStore } from '@/stores/auth'
import { toast } from 'sonner'

const instance: AxiosInstance = axios.create({
  baseURL: '/api',
  timeout: 30000,
  headers: { 'Content-Type': 'application/json' },
})

instance.interceptors.request.use((config) => {
  const { token, tokenName } = useAuthStore.getState()
  if (token) {
    config.headers[tokenName] = token
  }
  return config
})

instance.interceptors.response.use(
  (response: AxiosResponse<Result>) => {
    const { code, message, data } = response.data
    if (code === 200) return data as any
    toast.error(message || '请求失败')
    return Promise.reject(new Error(message || '请求失败'))
  },
  (error) => {
    if (error.response) {
      const { status } = error.response
      if (status === 401) {
        toast.error('未授权，请重新登录')
        useAuthStore.getState().logout()
        window.location.href = '/login'
      } else if (status === 403) {
        toast.error('没有权限访问')
      } else if (status === 500) {
        toast.error('服务器错误')
      } else {
        toast.error(error.response.data?.message || '请求失败')
      }
    } else {
      toast.error('网络错误')
    }
    return Promise.reject(error)
  },
)

export const request = {
  get<T = any>(url: string, config?: AxiosRequestConfig): Promise<T> {
    return instance.get(url, config) as any
  },
  post<T = any>(url: string, data?: any, config?: AxiosRequestConfig): Promise<T> {
    return instance.post(url, data, config) as any
  },
  put<T = any>(url: string, data?: any, config?: AxiosRequestConfig): Promise<T> {
    return instance.put(url, data, config) as any
  },
  delete<T = any>(url: string, config?: AxiosRequestConfig): Promise<T> {
    return instance.delete(url, config) as any
  },
}
