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

// ---------------------------------------------------------------------------
// Model API Key
// ---------------------------------------------------------------------------

export interface ModelApiKey {
  id: number
  modelId: number
  keyName: string
  apiKey: string
  apiKeyMasked: string
  weight: number
  totalRequests: number
  successRequests: number
  failedRequests: number
  lastUsedTime?: string
  lastSuccessTime?: string
  lastFailTime?: string
  healthStatus: number // 1=HEALTHY, 0=UNHEALTHY, -1=DISABLED
  failCount: number
  rateLimitPerMinute?: number
  rateLimitPerDay?: number
  quotaTotal?: number
  quotaUsed?: number
  expireTime?: string
  status: number
  sortOrder: number
  description?: string
  createdTime: string
  updatedTime: string
}

export interface CreateModelApiKeyRequest {
  keyName: string
  apiKey: string
  weight?: number
  rateLimitPerMinute?: number
  rateLimitPerDay?: number
  quotaTotal?: number
  expireTime?: string
  sortOrder?: number
  description?: string
}

export interface UpdateModelApiKeyRequest {
  keyName?: string
  apiKey?: string
  weight?: number
  rateLimitPerMinute?: number
  rateLimitPerDay?: number
  quotaTotal?: number
  expireTime?: string
  sortOrder?: number
  description?: string
}
