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
  totalInputTokens: number
  totalOutputTokens: number
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
