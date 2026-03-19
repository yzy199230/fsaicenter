import { request } from '@/lib/request'
import type { RequestLog, LogDetail } from '@/types/log'
import type { PageResult } from '@/types/common'

export const logApi = {
  getPage: (params: Record<string, any>): Promise<PageResult<RequestLog>> => request.get('/admin/logs', { params }),
  getById: (id: number): Promise<LogDetail> => request.get(`/admin/logs/${id}`),
}
