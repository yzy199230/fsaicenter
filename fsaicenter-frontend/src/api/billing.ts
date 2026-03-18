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
