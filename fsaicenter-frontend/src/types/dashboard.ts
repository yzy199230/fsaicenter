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
