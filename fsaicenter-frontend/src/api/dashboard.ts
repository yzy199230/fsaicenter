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
