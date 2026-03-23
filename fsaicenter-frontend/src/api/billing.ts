import { request } from '@/lib/request'
import type { BillingRule, BillingStats, BillingTrend, ModelCost } from '@/types/billing'
import type { PageResult } from '@/types/common'

export const billingRuleApi = {
  getPage: (params: Record<string, any>): Promise<PageResult<BillingRule>> => request.get('/admin/billing/rules', { params }),
  getById: (id: number): Promise<BillingRule> => request.get(`/admin/billing/rules/${id}`),
  create: (data: Record<string, any>): Promise<number> => request.post('/admin/billing/rules', data),
  update: (id: number, data: Record<string, any>): Promise<void> => request.put(`/admin/billing/rules/${id}`, data),
  delete: (id: number): Promise<void> => request.delete(`/admin/billing/rules/${id}`),
}

export const billingStatsApi = {
  getStats: (params?: Record<string, any>): Promise<BillingStats> => request.get('/admin/billing/statistics/stats', { params }),
  getTrend: (params?: Record<string, any>): Promise<BillingTrend[]> => request.get('/admin/billing/statistics/trend', { params }),
  getModelCost: (params?: Record<string, any>): Promise<ModelCost[]> => request.get('/admin/billing/statistics/model-cost', { params }),
}
