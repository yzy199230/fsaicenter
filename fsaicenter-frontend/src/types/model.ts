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
