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
