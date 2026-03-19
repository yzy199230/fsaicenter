import { request } from '@/lib/request'
import type { DashboardStats, DashboardTrend, ModelDistribution, TopModel, RecentLog } from '@/types/dashboard'

export const dashboardApi = {
  getStats: (): Promise<DashboardStats> => request.get('/admin/dashboard/stats'),
  getRequestTrend: (days?: number): Promise<DashboardTrend> => request.get('/admin/dashboard/request-trend', { params: { days } }),
  getModelDistribution: (): Promise<ModelDistribution[]> => request.get('/admin/dashboard/model-distribution'),
  getTopModels: (limit?: number): Promise<TopModel[]> => request.get('/admin/dashboard/top-models', { params: { limit } }),
  getRecentLogs: (limit?: number): Promise<RecentLog[]> => request.get('/admin/dashboard/recent-logs', { params: { limit } }),
}
