# FSAICenter 前端全面补齐实施计划

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 将前端从"骨架+占位"状态补齐为功能完整的管理后台，所有页面与后端 API 完全对接。

**Architecture:** Vue 3 + Arco Design + Pinia + ECharts，通过 Vite proxy 代理到后端 `/api` → `http://localhost:8080`。后端统一返回 `Result<T>` 格式 `{code, message, data, timestamp}`，分页用 `PageResult<T>` `{total, pageNum, pageSize, list}`。所有后端管理接口以 `/admin/` 为前缀。

**Tech Stack:** Vue 3.3, TypeScript 5.3, Arco Design Web Vue 2.55, ECharts 5, Pinia 2, Vite 5, axios

---

## Task 1: 修复构建链 — 重装依赖 + 添加 ECharts

**Files:**
- Modify: `fsaicenter-frontend/package.json`

**Step 1: 清理并重装依赖**

```bash
cd e:/xx/aicy/fsaicenter/fsaicenter-frontend
rm -rf node_modules package-lock.json
npm install
```

Expected: 安装成功，无 ERR

**Step 2: 添加 ECharts 依赖**

```bash
cd e:/xx/aicy/fsaicenter/fsaicenter-frontend
npm install echarts vue-echarts
```

**Step 3: 验证 build 可以运行**

```bash
cd e:/xx/aicy/fsaicenter/fsaicenter-frontend
npx vue-tsc --version
npx vite --version
```

Expected: 都能输出版本号

**Step 4: Commit**

```bash
git add fsaicenter-frontend/package.json fsaicenter-frontend/package-lock.json
git commit -m "fix: reinstall frontend dependencies and add echarts"
```

---

## Task 2: 修复类型系统 — 对齐后端 DTO

后端 Result 格式: `{code: number, message: string, data: T, timestamp: number}`
后端分页: `PageResult<T>` = `{total: number, pageNum: number, pageSize: number, list: T[]}`

**Files:**
- Modify: `src/types/common.ts`
- Modify: `src/types/user.ts`
- Modify: `src/types/apikey.ts`
- Modify: `src/types/model.ts`
- Modify: `src/types/billing.ts`
- Create: `src/types/dashboard.ts`
- Create: `src/types/log.ts`
- Create: `src/types/role.ts`

**Step 1: 重写 `src/types/common.ts`**

```ts
export interface PageRequest {
  pageNum: number
  pageSize: number
  [key: string]: any
}

export interface PageResult<T> {
  total: number
  pageNum: number
  pageSize: number
  list: T[]
  pages: number
}

export interface Result<T = any> {
  code: number
  message: string
  data: T
  timestamp: number
}
```

**Step 2: 重写 `src/types/user.ts`**

```ts
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
```

**Step 3: 重写 `src/types/apikey.ts`**

```ts
export interface ApiKey {
  id: number
  keyValue: string
  keyName: string
  description?: string
  quotaTotal: number
  quotaUsed: number
  quotaRemaining: number
  rateLimitPerMinute?: number
  rateLimitPerDay?: number
  allowedModelTypes?: string[]
  allowedIpWhitelist?: string[]
  expireTime?: string
  status: number
  createdTime: string
  updatedTime: string
}

export interface CreateApiKeyRequest {
  keyName: string
  description?: string
  quotaTotal: number
  rateLimitPerMinute?: number
  rateLimitPerDay?: number
  allowedModelTypes?: string[]
  allowedIpWhitelist?: string[]
  expireTime?: string
}

export interface UpdateApiKeyRequest {
  keyName: string
  description?: string
  quotaTotal?: number
  rateLimitPerMinute?: number
  rateLimitPerDay?: number
  allowedModelTypes?: string[]
  allowedIpWhitelist?: string[]
  expireTime?: string
  status?: number
}

export interface ApiKeyStatistics {
  totalRequests: number
  successRequests: number
  failedRequests: number
  successRate: number
  totalTokens: number
  totalCost: number
  avgResponseTime: number
}
```

**Step 4: 重写 `src/types/model.ts`**

```ts
export interface AiModel {
  id: number
  code: string
  name: string
  type: string
  providerId: number
  providerName?: string
  config?: ModelConfig
  supportStream: boolean
  maxTokenLimit?: number
  description?: string
  sortOrder: number
  status: number
  capabilities?: Record<string, any>
  createdTime: string
  updatedTime: string
}

export interface ModelConfig {
  temperature?: number
  maxTokens?: number
  topP?: number
  systemPrompt?: string
}

export interface CreateModelRequest {
  code: string
  name: string
  type: string
  providerId: number
  config?: ModelConfig
  supportStream?: boolean
  maxTokenLimit?: number
  description?: string
  sortOrder?: number
  capabilities?: Record<string, any>
}

export interface UpdateModelRequest {
  name: string
  config?: ModelConfig
  supportStream?: boolean
  maxTokenLimit?: number
  description?: string
  sortOrder?: number
  capabilities?: Record<string, any>
}

export interface Provider {
  id: number
  code: string
  name: string
  type: string
  baseUrl?: string
  protocolType?: string
  chatEndpoint?: string
  embeddingEndpoint?: string
  imageEndpoint?: string
  videoEndpoint?: string
  extraHeaders?: string
  requestTemplate?: string
  responseMapping?: string
  authType?: string
  authHeader?: string
  authPrefix?: string
  apiKeyRequired?: boolean
  description?: string
  sortOrder: number
  status: number
  createdTime: string
  updatedTime: string
}

export interface CreateProviderRequest {
  code: string
  name: string
  type: string
  baseUrl?: string
  protocolType?: string
  chatEndpoint?: string
  embeddingEndpoint?: string
  imageEndpoint?: string
  videoEndpoint?: string
  authType?: string
  authHeader?: string
  authPrefix?: string
  apiKeyRequired?: boolean
  description?: string
  sortOrder?: number
  status?: number
}

export interface UpdateProviderRequest {
  name: string
  baseUrl?: string
  protocolType?: string
  chatEndpoint?: string
  embeddingEndpoint?: string
  imageEndpoint?: string
  videoEndpoint?: string
  authType?: string
  authHeader?: string
  authPrefix?: string
  apiKeyRequired?: boolean
  description?: string
  sortOrder?: number
  status?: number
}
```

**Step 5: 重写 `src/types/billing.ts`**

```ts
export interface BillingRule {
  id: number
  modelId: number
  modelName?: string
  modelType?: string
  billingType: string
  unitPrice?: number
  inputUnitPrice?: number
  outputUnitPrice?: number
  unitAmount: number
  currency: string
  effectiveTime: string
  expireTime?: string
  description?: string
  status: number
  createdTime: string
  updatedTime: string
}

export interface BillingStats {
  totalCost: number
  totalRequests: number
  totalUsage: number
  avgUnitPrice: number
}

export interface BillingTrend {
  date: string
  cost: number
  requests: number
}

export interface ModelCost {
  modelId: number
  modelName: string
  totalCost: number
  requests: number
  usage: number
}
```

**Step 6: 创建 `src/types/dashboard.ts`**

```ts
export interface DashboardStats {
  todayRequests: number
  successRate: number
  totalCost: number
  activeApiKeys: number
}

export interface DashboardTrend {
  xAxis: string[]
  series: { name: string; data: number[] }[]
}

export interface ModelDistribution {
  name: string
  value: number
}

export interface TopModel {
  modelName: string
  callCount: number
}

export interface RecentLog {
  time: string
  model: string
  apiKey: string
  status: number
  duration: string
}
```

**Step 7: 创建 `src/types/log.ts`**

```ts
export interface RequestLog {
  id: number
  requestId: string
  apiKeyId: number
  apiKeyName?: string
  modelId: number
  modelCode: string
  modelType: string
  requestTime: string
  responseTime?: string
  duration?: number
  inputTokens?: number
  outputTokens?: number
  status: string
  errorMessage?: string
  clientIp?: string
  userAgent?: string
}

export interface LogDetail extends RequestLog {
  requestBody?: string
  responseBody?: string
}
```

**Step 8: Commit**

```bash
git add fsaicenter-frontend/src/types/
git commit -m "refactor: align frontend types with backend DTOs"
```

---

## Task 3: 修复 API 层 — 路径对齐 + 响应解包

Vite proxy 把 `/api` 代理到 `http://localhost:8080`，所以前端请求 `/api/admin/xxx` → 后端 `http://localhost:8080/api/admin/xxx`。但后端路径是 `/admin/xxx`，所以需要确认 proxy 配置。

实际上后端 controller 挂的是 `/admin/xxx`，所以前端 baseURL 应该是 `/api`，proxy 应该重写 `/api` → 空（或者让 baseURL = '' 直接请求）。最安全的做法：前端 baseURL 保持 `/api`，proxy 配 rewrite 去掉 `/api` 前缀，这样 `/api/admin/xxx` → `http://localhost:8080/admin/xxx`。

**Files:**
- Modify: `src/utils/request.ts` — 修改响应解包逻辑匹配后端 Result 格式
- Modify: `vite.config.ts` — 确认 proxy 配置加 rewrite
- Modify: `src/api/auth.ts`
- Modify: `src/api/apikey.ts`
- Modify: `src/api/model.ts`
- Modify: `src/api/billing.ts`
- Create: `src/api/dashboard.ts`
- Create: `src/api/user.ts`
- Create: `src/api/role.ts`
- Create: `src/api/log.ts`

**Step 1: 修改 `vite.config.ts`，添加 rewrite**

```ts
server: {
  port: 3000,
  proxy: {
    '/api': {
      target: 'http://localhost:8080',
      changeOrigin: true,
      rewrite: (path: string) => path.replace(/^\/api/, '')
    }
  }
}
```

**Step 2: 修改 `src/utils/request.ts` — 适配后端 Result 格式**

响应拦截器需要检查 `code === 200` 并返回 `data` 字段：

```ts
import axios from 'axios'
import type { AxiosInstance, AxiosRequestConfig, AxiosResponse } from 'axios'
import { Message } from '@arco-design/web-vue'
import { useUserStore } from '@/stores/user'
import type { Result } from '@/types/common'

const instance: AxiosInstance = axios.create({
  baseURL: '/api',
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json'
  }
})

instance.interceptors.request.use(
  (config) => {
    const userStore = useUserStore()
    if (userStore.token) {
      config.headers.Authorization = userStore.token
    }
    return config
  },
  (error) => Promise.reject(error)
)

instance.interceptors.response.use(
  (response: AxiosResponse<Result>) => {
    const { code, message, data } = response.data
    if (code === 200) {
      return data
    }
    Message.error(message || '请求失败')
    return Promise.reject(new Error(message || '请求失败'))
  },
  (error) => {
    if (error.response) {
      const { status } = error.response
      if (status === 401) {
        Message.error('未授权，请重新登录')
        const userStore = useUserStore()
        userStore.logout()
        window.location.href = '/login'
      } else if (status === 403) {
        Message.error('没有权限访问')
      } else if (status === 500) {
        Message.error('服务器错误')
      } else {
        Message.error(error.response.data?.message || '请求失败')
      }
    } else {
      Message.error('网络错误')
    }
    return Promise.reject(error)
  }
)

export default instance

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
  }
}
```

注意：后端 Sa-Token 使用 `tokenName: "satoken"` header，LoginResponse 返回 `tokenName` 和 `tokenValue`。前端在登录后应存储 tokenValue，请求时用后端定义的 header 名带上 token。默认 Sa-Token 接受 `satoken` header 或 `Authorization: Bearer xxx`。为简化，暂时继续用 Bearer token 方式（需要确认后端是否配了 `isReadHeader=true`）。如果后端默认用 satoken header，则改为：`config.headers[tokenName] = tokenValue`。

**Step 3: 修改 `src/stores/user.ts`**

```ts
import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { UserInfo, LoginResponse } from '@/types/user'

export const useUserStore = defineStore('user', () => {
  const token = ref<string>(localStorage.getItem('token') || '')
  const tokenName = ref<string>(localStorage.getItem('tokenName') || 'satoken')
  const userInfo = ref<UserInfo | null>(null)

  function setLoginInfo(res: LoginResponse) {
    token.value = res.tokenValue
    tokenName.value = res.tokenName
    localStorage.setItem('token', res.tokenValue)
    localStorage.setItem('tokenName', res.tokenName)
    userInfo.value = {
      userId: res.userId,
      username: res.username,
      realName: res.realName,
      avatar: res.avatar,
      status: 1
    }
  }

  function setUserInfo(info: UserInfo) {
    userInfo.value = info
  }

  function logout() {
    token.value = ''
    userInfo.value = null
    localStorage.removeItem('token')
    localStorage.removeItem('tokenName')
  }

  return { token, tokenName, userInfo, setLoginInfo, setUserInfo, logout }
})
```

**Step 4: 重写 `src/api/auth.ts`**

```ts
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
```

**Step 5: 重写 `src/api/apikey.ts`**

```ts
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
```

**Step 6: 重写 `src/api/model.ts`**

```ts
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
```

**Step 7: 重写 `src/api/billing.ts`**

```ts
import { request } from '@/utils/request'
import type { BillingRule, BillingStats, BillingTrend, ModelCost } from '@/types/billing'
import type { PageResult } from '@/types/common'

export const billingRuleApi = {
  getPage(params: Record<string, any>): Promise<PageResult<BillingRule>> {
    return request.get('/admin/billing/rules', { params })
  },
  getById(id: number): Promise<BillingRule> {
    return request.get(`/admin/billing/rules/${id}`)
  },
  create(data: Record<string, any>): Promise<number> {
    return request.post('/admin/billing/rules', data)
  },
  update(id: number, data: Record<string, any>): Promise<void> {
    return request.put(`/admin/billing/rules/${id}`, data)
  },
  delete(id: number): Promise<void> {
    return request.delete(`/admin/billing/rules/${id}`)
  }
}

export const billingStatsApi = {
  getStats(params?: Record<string, any>): Promise<BillingStats> {
    return request.get('/admin/billing/statistics/stats', { params })
  },
  getTrend(days?: number): Promise<BillingTrend[]> {
    return request.get('/admin/billing/statistics/trend', { params: { days } })
  },
  getModelCost(params?: Record<string, any>): Promise<ModelCost[]> {
    return request.get('/admin/billing/statistics/model-cost', { params })
  }
}
```

**Step 8: 创建 `src/api/dashboard.ts`**

```ts
import { request } from '@/utils/request'
import type { DashboardStats, DashboardTrend, ModelDistribution, TopModel, RecentLog } from '@/types/dashboard'

export const dashboardApi = {
  getStats(): Promise<DashboardStats> {
    return request.get('/admin/dashboard/stats')
  },
  getRequestTrend(days?: number): Promise<DashboardTrend> {
    return request.get('/admin/dashboard/request-trend', { params: { days } })
  },
  getModelDistribution(): Promise<ModelDistribution[]> {
    return request.get('/admin/dashboard/model-distribution')
  },
  getTopModels(limit?: number): Promise<TopModel[]> {
    return request.get('/admin/dashboard/top-models', { params: { limit } })
  },
  getRecentLogs(limit?: number): Promise<RecentLog[]> {
    return request.get('/admin/dashboard/recent-logs', { params: { limit } })
  }
}
```

**Step 9: 创建 `src/api/user.ts`**

```ts
import { request } from '@/utils/request'
import type { AdminUser, CreateAdminUserRequest, UpdateAdminUserRequest } from '@/types/user'
import type { PageResult } from '@/types/common'

export const adminUserApi = {
  getPage(params: Record<string, any>): Promise<PageResult<AdminUser>> {
    return request.get('/admin/users', { params })
  },
  getById(id: number): Promise<AdminUser> {
    return request.get(`/admin/users/${id}`)
  },
  create(data: CreateAdminUserRequest): Promise<number> {
    return request.post('/admin/users', data)
  },
  update(id: number, data: UpdateAdminUserRequest): Promise<void> {
    return request.put(`/admin/users/${id}`, data)
  },
  delete(id: number): Promise<void> {
    return request.delete(`/admin/users/${id}`)
  },
  toggleStatus(id: number): Promise<void> {
    return request.put(`/admin/users/${id}/status`)
  },
  resetPassword(id: number, newPassword: string): Promise<void> {
    return request.put(`/admin/users/${id}/password`, newPassword)
  },
  assignRoles(id: number, roleIds: number[]): Promise<void> {
    return request.put(`/admin/users/${id}/roles`, roleIds)
  }
}
```

**Step 10: 创建 `src/api/role.ts`**

```ts
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
```

**Step 11: 创建 `src/api/log.ts`**

```ts
import { request } from '@/utils/request'
import type { RequestLog, LogDetail } from '@/types/log'
import type { PageResult } from '@/types/common'

export const logApi = {
  getPage(params: Record<string, any>): Promise<PageResult<RequestLog>> {
    return request.get('/admin/logs', { params })
  },
  getById(id: number): Promise<LogDetail> {
    return request.get(`/admin/logs/${id}`)
  }
}
```

**Step 12: Commit**

```bash
git add fsaicenter-frontend/src/api/ fsaicenter-frontend/src/utils/ fsaicenter-frontend/src/stores/ fsaicenter-frontend/vite.config.ts
git commit -m "refactor: align all API endpoints and request handling with backend"
```

---

## Task 4: 修复登录页 — 适配新 LoginResponse

**Files:**
- Modify: `src/views/login/index.vue`

**Step 1: 修改登录处理逻辑**

将 `userStore.setToken(res.token)` 改为 `userStore.setLoginInfo(res)`，因为后端返回 `LoginResponse` 格式为 `{userId, username, realName, avatar, tokenName, tokenValue}`。

```ts
const handleSubmit = async ({ errors }: any) => {
  if (errors) return
  loading.value = true
  try {
    const res = await authApi.login(formData)
    userStore.setLoginInfo(res)
    Message.success('登录成功')
    router.push('/')
  } catch (error) {
    console.error('登录失败:', error)
  } finally {
    loading.value = false
  }
}
```

**Step 2: Commit**

```bash
git add fsaicenter-frontend/src/views/login/index.vue
git commit -m "fix: adapt login page to new LoginResponse format"
```

---

## Task 5: 实现仪表盘 — 集成 ECharts + 后端 Dashboard API

**Files:**
- Modify: `src/views/dashboard/index.vue`

**Step 1: 重写 dashboard 页面**

完整实现：4个统计卡片 + 请求趋势折线图 + 模型分布饼图 + 最近日志表格 + Top 模型排行。

数据从 `dashboardApi` 获取：
- `getStats()` → 4个统计卡片
- `getRequestTrend(7)` → 折线图
- `getModelDistribution()` → 饼图
- `getRecentLogs(10)` → 最近日志表格
- `getTopModels(5)` → 排行列表

```vue
<template>
  <div class="dashboard">
    <a-row :gutter="16">
      <a-col :span="6">
        <a-card>
          <a-statistic title="今日请求数" :value="stats.todayRequests" />
        </a-card>
      </a-col>
      <a-col :span="6">
        <a-card>
          <a-statistic title="成功率" :value="stats.successRate" :precision="2" suffix="%" />
        </a-card>
      </a-col>
      <a-col :span="6">
        <a-card>
          <a-statistic title="总消费" :value="stats.totalCost" :precision="2" prefix="¥" />
        </a-card>
      </a-col>
      <a-col :span="6">
        <a-card>
          <a-statistic title="活跃 API Key" :value="stats.activeApiKeys" />
        </a-card>
      </a-col>
    </a-row>

    <a-row :gutter="16" style="margin-top: 16px">
      <a-col :span="16">
        <a-card title="请求趋势（近7天）">
          <v-chart :option="trendOption" style="height: 320px" autoresize />
        </a-card>
      </a-col>
      <a-col :span="8">
        <a-card title="模型调用分布">
          <v-chart :option="pieOption" style="height: 320px" autoresize />
        </a-card>
      </a-col>
    </a-row>

    <a-row :gutter="16" style="margin-top: 16px">
      <a-col :span="16">
        <a-card title="最近请求日志">
          <a-table :columns="logColumns" :data="recentLogs" :pagination="false" size="small" />
        </a-card>
      </a-col>
      <a-col :span="8">
        <a-card title="热门模型 Top 5">
          <div v-for="(item, index) in topModels" :key="index" class="top-model-item">
            <span class="rank">{{ index + 1 }}</span>
            <span class="name">{{ item.modelName }}</span>
            <span class="count">{{ item.callCount }} 次</span>
          </div>
        </a-card>
      </a-col>
    </a-row>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import VChart from 'vue-echarts'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { LineChart, PieChart } from 'echarts/charts'
import { GridComponent, TooltipComponent, LegendComponent, TitleComponent } from 'echarts/components'
import { dashboardApi } from '@/api/dashboard'
import type { DashboardStats, DashboardTrend, ModelDistribution, TopModel, RecentLog } from '@/types/dashboard'

use([CanvasRenderer, LineChart, PieChart, GridComponent, TooltipComponent, LegendComponent, TitleComponent])

const stats = reactive<DashboardStats>({
  todayRequests: 0,
  successRate: 0,
  totalCost: 0,
  activeApiKeys: 0
})

const trendData = ref<DashboardTrend>({ xAxis: [], series: [] })
const distributionData = ref<ModelDistribution[]>([])
const topModels = ref<TopModel[]>([])
const recentLogs = ref<RecentLog[]>([])

const trendOption = computed(() => ({
  tooltip: { trigger: 'axis' },
  legend: {},
  grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
  xAxis: { type: 'category', data: trendData.value.xAxis },
  yAxis: { type: 'value' },
  series: trendData.value.series.map(s => ({
    name: s.name,
    type: 'line',
    smooth: true,
    data: s.data
  }))
}))

const pieOption = computed(() => ({
  tooltip: { trigger: 'item' },
  series: [{
    type: 'pie',
    radius: ['40%', '70%'],
    data: distributionData.value.map(d => ({ name: d.name, value: d.value })),
    emphasis: { itemStyle: { shadowBlur: 10 } }
  }]
}))

const logColumns = [
  { title: '时间', dataIndex: 'time', width: 160 },
  { title: '模型', dataIndex: 'model' },
  { title: 'API Key', dataIndex: 'apiKey' },
  { title: '状态', dataIndex: 'status', render: ({ record }: any) => record.status === 1 ? '成功' : '失败' },
  { title: '耗时', dataIndex: 'duration', width: 100 }
]

const fetchData = async () => {
  try {
    const [statsRes, trendRes, distRes, topRes, logsRes] = await Promise.all([
      dashboardApi.getStats(),
      dashboardApi.getRequestTrend(7),
      dashboardApi.getModelDistribution(),
      dashboardApi.getTopModels(5),
      dashboardApi.getRecentLogs(10)
    ])
    Object.assign(stats, statsRes)
    trendData.value = trendRes
    distributionData.value = distRes
    topModels.value = topRes
    recentLogs.value = logsRes
  } catch (error) {
    console.error('加载仪表盘数据失败:', error)
  }
}

onMounted(fetchData)
</script>

<style scoped>
.dashboard {
  padding: 20px;
}
.top-model-item {
  display: flex;
  align-items: center;
  padding: 8px 0;
  border-bottom: 1px solid #f0f0f0;
}
.top-model-item:last-child {
  border-bottom: none;
}
.rank {
  width: 24px;
  height: 24px;
  line-height: 24px;
  text-align: center;
  border-radius: 50%;
  background: #e8f3ff;
  color: #165dff;
  font-size: 12px;
  margin-right: 12px;
}
.top-model-item:nth-child(1) .rank { background: #ff7d00; color: #fff; }
.top-model-item:nth-child(2) .rank { background: #86909c; color: #fff; }
.top-model-item:nth-child(3) .rank { background: #c9cdd4; color: #fff; }
.name {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.count {
  color: #86909c;
  font-size: 13px;
}
</style>
```

**Step 2: Commit**

```bash
git add fsaicenter-frontend/src/views/dashboard/
git commit -m "feat: implement dashboard with ECharts trends and real API data"
```

---

## Task 6: 完善 API Key 管理页 — 适配新类型

**Files:**
- Modify: `src/views/apikey/list/index.vue`

**Step 1: 修改 apikey 列表页**

主要变更：
- 后端返回列表（非分页），所以去掉分页或改为前端分页
- 字段名对齐：`keyName`, `keyValue`, `quotaTotal/quotaUsed/quotaRemaining`, `rateLimitPerMinute`, `status`, `createdTime`
- 表单字段对齐：`quotaTotal` (用 -1 表示不限), `rateLimitPerMinute`, `rateLimitPerDay`, `description`
- 创建/编辑弹窗完整实现

后端 `GET /admin/apikeys` 返回 `List<ApiKeyResponse>`（不是分页），所以前端做本地过滤和分页。

将整个文件重写，核心变化：去掉后端分页调用，改为本地过滤 + arco table 自带分页。表单增加 `description`, `rateLimitPerDay` 字段，`quotaLimit` 改为 `quotaTotal`。

**Step 2: Commit**

```bash
git add fsaicenter-frontend/src/views/apikey/
git commit -m "feat: update apikey management with correct API integration"
```

---

## Task 7: 完善模型管理页 — 实现创建/编辑弹窗

**Files:**
- Modify: `src/views/model/list/index.vue`

**Step 1: 实现模型创建/编辑弹窗**

添加 Modal，包含字段：
- `code` (创建时必填)
- `name` (必填)
- `type` 下拉 (chat/embedding/image/asr/tts/video/image_recognition)
- `providerId` 下拉 (从 providerApi.getAll() 获取)
- `supportStream` 开关
- `maxTokenLimit` 数字
- `description` 文本域
- `config.temperature`, `config.maxTokens`, `config.topP` 配置项

后端 `GET /admin/models` 支持 `ModelQueryRequest` 查询参数: `pageNum, pageSize, keyword, modelType, providerId, status`。

**Step 2: Commit**

```bash
git add fsaicenter-frontend/src/views/model/list/
git commit -m "feat: implement model create/edit dialog with provider selection"
```

---

## Task 8: 实现提供商管理页

**Files:**
- Rewrite: `src/views/model/providers/index.vue`

**Step 1: 完整实现提供商管理页**

功能：
- 表格展示所有提供商（列：ID、编码、名称、类型、baseUrl、状态、操作）
- 搜索过滤
- 创建/编辑弹窗（字段：code, name, type, baseUrl, protocolType, chatEndpoint, embeddingEndpoint, imageEndpoint, authType, authHeader, authPrefix, apiKeyRequired, description）
- 启用/禁用切换
- 删除确认

**Step 2: Commit**

```bash
git add fsaicenter-frontend/src/views/model/providers/
git commit -m "feat: implement provider management page"
```

---

## Task 9: 实现用户管理页

**Files:**
- Rewrite: `src/views/system/users/index.vue`

**Step 1: 完整实现用户管理页**

功能：
- 分页表格（后端 `GET /admin/users` 返回 `PageResult<AdminUserResponse>`）
- 列：ID、用户名、真实姓名、邮箱、手机、状态、角色、最后登录时间、操作
- 搜索（keyword, status）
- 创建用户弹窗（username, password, realName, email, phone, status, roleIds 多选）
- 编辑用户弹窗（realName, email, phone, status, roleIds）
- 重置密码
- 启用/禁用切换
- 删除确认

角色下拉从 `adminRoleApi.getAll()` 获取。

**Step 2: Commit**

```bash
git add fsaicenter-frontend/src/views/system/users/
git commit -m "feat: implement user management page with CRUD"
```

---

## Task 10: 实现角色管理页

**Files:**
- Rewrite: `src/views/system/roles/index.vue`

**Step 1: 完整实现角色管理页**

功能：
- 表格（后端 `GET /admin/roles` 返回 `List<AdminRoleResponse>`）
- 列：ID、角色编码、角色名称、描述、排序、状态、操作
- 创建/编辑弹窗（roleCode, roleName, description, sortOrder, status）
- 权限分配弹窗（tree 选择，从 `permissionApi.getTree()` 获取树形数据）
- 启用/禁用切换
- 删除确认

**Step 2: Commit**

```bash
git add fsaicenter-frontend/src/views/system/roles/
git commit -m "feat: implement role management page with permission assignment"
```

---

## Task 11: 实现计费记录页

**Files:**
- Rewrite: `src/views/billing/records/index.vue`

**Step 1: 完整实现计费记录页**

展示两部分：
1. 统计卡片（从 `billingStatsApi.getStats()` 获取）: 总消费、总请求、总用量、平均单价
2. 计费规则表格（从 `billingRuleApi.getPage()` 获取，支持分页）
   - 列：ID、模型名称、模型类型、计费类型、单价、输入单价、输出单价、单位数量、生效时间、状态、操作
3. 消费趋势图（从 `billingStatsApi.getTrend()` 获取）
4. 模型成本排行（从 `billingStatsApi.getModelCost()` 获取）

**Step 2: Commit**

```bash
git add fsaicenter-frontend/src/views/billing/
git commit -m "feat: implement billing records page with statistics and rules"
```

---

## Task 12: 实现请求日志页

**Files:**
- Rewrite: `src/views/logs/requests/index.vue`

**Step 1: 完整实现请求日志页**

功能：
- 分页表格（后端 `GET /admin/logs` 返回 `PageResult<LogListResponse>`）
- 查询参数：keyword, modelType, status, startTime, endTime, pageNum, pageSize
- 列：请求ID、API Key名称、模型代码、模型类型、请求时间、耗时、输入Token、输出Token、状态、操作
- 筛选栏：关键词搜索、模型类型下拉、状态下拉、时间范围选择
- 详情弹窗（点击查看，从 `logApi.getById()` 获取，展示 requestBody 和 responseBody）

**Step 2: Commit**

```bash
git add fsaicenter-frontend/src/views/logs/
git commit -m "feat: implement request logs page with filtering and detail view"
```

---

## Task 13: 修复 MainLayout — 适配请求头

**Files:**
- Modify: `src/layouts/MainLayout.vue`

**Step 1: 退出登录时调用后端 logout API**

```ts
import { authApi } from '@/api/auth'

const handleLogout = async () => {
  try {
    await authApi.logout()
  } catch (e) {
    // ignore
  }
  userStore.logout()
  router.push('/login')
}
```

**Step 2: 显示用户真实姓名**

在 header-right 区域显示 `userStore.userInfo?.realName || userStore.userInfo?.username`。

**Step 3: Commit**

```bash
git add fsaicenter-frontend/src/layouts/
git commit -m "fix: call backend logout API and display user name in header"
```

---

## Task 14: 验证构建

**Step 1: 运行 dev 服务器确认无编译错误**

```bash
cd e:/xx/aicy/fsaicenter/fsaicenter-frontend
npx vite --force 2>&1 | head -20
```

Expected: Vite dev server 启动成功，无 TypeScript 编译错误

**Step 2: 运行 type check**

```bash
cd e:/xx/aicy/fsaicenter/fsaicenter-frontend
npx vue-tsc --noEmit
```

Expected: 无类型错误

**Step 3: 最终 Commit**

```bash
git add -A fsaicenter-frontend/
git commit -m "chore: final frontend build verification"
```
