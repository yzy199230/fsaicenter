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
  allowedModelIds?: number[]
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
  allowedModelIds?: number[]
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
  allowedModelIds?: number[]
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
