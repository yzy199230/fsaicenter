export interface PageRequest {
  pageNum: number
  pageSize: number
  [key: string]: any
}

export interface PageResult<T> {
  total: number
  pageNum: number
  pageSize: number
  list: T[]
  pages: number
}

export interface Result<T = any> {
  code: number
  message: string
  data: T
  timestamp: number
}
