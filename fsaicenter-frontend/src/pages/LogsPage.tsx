import { useState, useEffect } from 'react'
import { motion } from 'framer-motion'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Badge } from '@/components/ui/badge'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogDescription } from '@/components/ui/dialog'
import { Separator } from '@/components/ui/separator'
import { logApi } from '@/api/log'
import type { RequestLog, LogDetail } from '@/types/log'
import type { PageResult } from '@/types/common'
import { toast } from 'sonner'
import { Search, ScrollText, Clock, Cpu, Key, Globe, ChevronLeft, ChevronRight, Eye, Loader2, Filter } from 'lucide-react'

// ---------------------------------------------------------------------------
// Constants
// ---------------------------------------------------------------------------

const MODEL_TYPES = [
  'Chat',
  'Embedding',
  'Image',
  'ASR',
  'TTS',
  'Video',
  'ImageRecognition',
] as const

const PAGE_SIZE = 20

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

function formatDateTime(iso: string): string {
  try {
    const d = new Date(iso)
    return d.toLocaleString('en-US', {
      month: 'short',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit',
      hour12: false,
    })
  } catch {
    return iso
  }
}

function truncate(str: string, len: number): string {
  if (!str) return ''
  return str.length > len ? str.slice(0, len) + '...' : str
}

function formatJson(raw?: string): string {
  if (!raw) return ''
  try {
    return JSON.stringify(JSON.parse(raw), null, 2)
  } catch {
    return raw
  }
}

// ---------------------------------------------------------------------------
// Animation variants
// ---------------------------------------------------------------------------

const containerVariants = {
  hidden: { opacity: 0 },
  show: {
    opacity: 1,
    transition: { staggerChildren: 0.06 },
  },
}

const itemVariants = {
  hidden: { opacity: 0, y: 16 },
  show: { opacity: 1, y: 0, transition: { duration: 0.4, ease: 'easeOut' } },
}

// ---------------------------------------------------------------------------
// Detail field component
// ---------------------------------------------------------------------------

function DetailField({ label, value, mono }: { label: string; value?: string | number | null; mono?: boolean }) {
  return (
    <div className="space-y-1">
      <p className="text-[10px] uppercase tracking-wider text-zinc-500 font-medium">{label}</p>
      <p className={`text-sm text-zinc-200 break-all ${mono ? 'font-mono text-xs' : ''}`}>
        {value != null && value !== '' ? String(value) : '-'}
      </p>
    </div>
  )
}

// ---------------------------------------------------------------------------
// JSON code block component
// ---------------------------------------------------------------------------

function JsonBlock({ title, content }: { title: string; content?: string }) {
  return (
    <div className="space-y-2">
      <p className="text-xs font-medium text-zinc-400">{title}</p>
      <div className="relative rounded-lg bg-[hsl(240_10%_4%)] border border-white/[0.06] overflow-hidden">
        <pre className="p-4 overflow-x-auto text-xs text-zinc-300 font-mono leading-relaxed max-h-[400px] overflow-y-auto whitespace-pre-wrap break-words">
          {content ? formatJson(content) : <span className="text-zinc-600 italic">无数据</span>}
        </pre>
      </div>
    </div>
  )
}

// ---------------------------------------------------------------------------
// Main page
// ---------------------------------------------------------------------------

export default function LogsPage() {
  // ---- data state ----
  const [logs, setLogs] = useState<RequestLog[]>([])
  const [total, setTotal] = useState(0)
  const [pages, setPages] = useState(0)
  const [pageNum, setPageNum] = useState(1)
  const [loading, setLoading] = useState(true)

  // ---- filter state ----
  const [keyword, setKeyword] = useState('')
  const [filterType, setFilterType] = useState<string>('all')
  const [filterStatus, setFilterStatus] = useState<string>('all')

  // ---- detail dialog state ----
  const [detailOpen, setDetailOpen] = useState(false)
  const [detail, setDetail] = useState<LogDetail | null>(null)
  const [detailLoading, setDetailLoading] = useState(false)

  // ---- data fetching ----
  const fetchLogs = async (page: number = pageNum) => {
    setLoading(true)
    try {
      const params: Record<string, any> = {
        pageNum: page,
        pageSize: PAGE_SIZE,
      }
      if (keyword.trim()) params.keyword = keyword.trim()
      if (filterType !== 'all') params.modelType = filterType
      if (filterStatus !== 'all') params.status = filterStatus

      const data: PageResult<RequestLog> = await logApi.getPage(params)
      setLogs(data.list)
      setTotal(data.total)
      setPages(data.pages)
      setPageNum(data.pageNum)
    } catch {
      toast.error('加载请求日志失败')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    fetchLogs(1)
  }, [filterType, filterStatus])

  const handleSearch = () => {
    fetchLogs(1)
  }

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter') handleSearch()
  }

  // ---- pagination ----
  const goToPrev = () => {
    if (pageNum > 1) fetchLogs(pageNum - 1)
  }

  const goToNext = () => {
    if (pageNum < pages) fetchLogs(pageNum + 1)
  }

  // ---- detail dialog ----
  const openDetail = async (log: RequestLog) => {
    setDetailOpen(true)
    setDetail(null)
    setDetailLoading(true)
    try {
      const data = await logApi.getById(log.id)
      setDetail(data)
    } catch {
      toast.error('加载日志详情失败')
      setDetailOpen(false)
    } finally {
      setDetailLoading(false)
    }
  }

  // ---- active filter count ----
  const activeFilters =
    (filterType !== 'all' ? 1 : 0) + (filterStatus !== 'all' ? 1 : 0)

  // ====================================================================
  // Render
  // ====================================================================

  return (
    <motion.div
      className="space-y-6"
      variants={containerVariants}
      initial="hidden"
      animate="show"
    >
      {/* ---- Page header ---- */}
      <motion.div variants={itemVariants}>
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
          <div>
            <h1 className="text-2xl font-bold tracking-tight flex items-center gap-2.5">
              <ScrollText className="w-6 h-6 text-blue-400" />
              请求日志
            </h1>
            <p className="text-sm text-muted-foreground mt-1">
              浏览和检查 API 请求历史、响应详情和性能指标。
            </p>
          </div>
          <div className="flex items-center gap-2 text-xs text-muted-foreground">
            <span className="tabular-nums font-medium text-foreground">{total.toLocaleString()}</span>
            <span>条记录</span>
          </div>
        </div>
      </motion.div>

      {/* ---- Filter bar ---- */}
      <motion.div variants={itemVariants}>
        <div className="flex flex-col sm:flex-row items-stretch sm:items-center gap-3">
          {/* Keyword search */}
          <div className="relative flex-1 min-w-0">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-muted-foreground pointer-events-none" />
            <Input
              placeholder="按请求 ID、API Key、模型搜索..."
              value={keyword}
              onChange={(e) => setKeyword(e.target.value)}
              onKeyDown={handleKeyDown}
              className="pl-9 bg-white/[0.03] border-white/[0.06] focus:border-white/[0.12] h-10"
            />
          </div>

          {/* Model type filter */}
          <Select value={filterType} onValueChange={setFilterType}>
            <SelectTrigger className="w-full sm:w-[160px] bg-white/[0.03] border-white/[0.06] h-10">
              <div className="flex items-center gap-2">
                <Cpu className="w-3.5 h-3.5 text-muted-foreground" />
                <SelectValue placeholder="所有类型" />
              </div>
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="all">所有类型</SelectItem>
              {MODEL_TYPES.map((t) => (
                <SelectItem key={t} value={t}>{t}</SelectItem>
              ))}
            </SelectContent>
          </Select>

          {/* Status filter */}
          <Select value={filterStatus} onValueChange={setFilterStatus}>
            <SelectTrigger className="w-full sm:w-[150px] bg-white/[0.03] border-white/[0.06] h-10">
              <div className="flex items-center gap-2">
                <Filter className="w-3.5 h-3.5 text-muted-foreground" />
                <SelectValue placeholder="所有状态" />
              </div>
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="all">所有状态</SelectItem>
              <SelectItem value="SUCCESS">
                <span className="flex items-center gap-2">
                  <span className="w-1.5 h-1.5 rounded-full bg-emerald-400" />
                  成功
                </span>
              </SelectItem>
              <SelectItem value="FAILED">
                <span className="flex items-center gap-2">
                  <span className="w-1.5 h-1.5 rounded-full bg-red-400" />
                  失败
                </span>
              </SelectItem>
            </SelectContent>
          </Select>

          {/* Search button */}
          <Button onClick={handleSearch} variant="default" className="h-10 px-4 gap-2 shrink-0">
            <Search className="w-4 h-4" />
            搜索
          </Button>

          {/* Clear filters */}
          {activeFilters > 0 && (
            <Button
              variant="ghost"
              size="sm"
              className="text-xs text-muted-foreground hover:text-foreground h-10 px-3"
              onClick={() => {
                setFilterType('all')
                setFilterStatus('all')
                setKeyword('')
              }}
            >
              清除
              <Badge className="ml-1.5 px-1.5 h-4 text-[10px] bg-primary/20 text-primary border-0">
                {activeFilters}
              </Badge>
            </Button>
          )}
        </div>
      </motion.div>

      {/* ---- Table ---- */}
      <motion.div variants={itemVariants}>
        <Card className="bg-white/[0.02] border-white/[0.06] backdrop-blur-sm">
          <CardContent className="p-0">
            {loading ? (
              <div className="flex flex-col items-center justify-center py-32 gap-4">
                <Loader2 className="w-8 h-8 text-primary animate-spin" />
                <p className="text-sm text-muted-foreground">加载日志中...</p>
              </div>
            ) : logs.length === 0 ? (
              <div className="flex flex-col items-center justify-center py-32 gap-4">
                <div className="w-16 h-16 rounded-2xl bg-white/[0.04] border border-white/[0.06] flex items-center justify-center">
                  <ScrollText className="w-8 h-8 text-muted-foreground" />
                </div>
                <div className="text-center">
                  <p className="text-sm font-medium text-foreground">暂无日志</p>
                  <p className="text-xs text-muted-foreground mt-1">
                    请尝试调整搜索条件或筛选项。
                  </p>
                </div>
              </div>
            ) : (
              <div className="overflow-x-auto">
                <table className="w-full text-xs">
                  <thead>
                    <tr className="border-b border-white/[0.06] text-left text-zinc-500">
                      <th className="px-4 py-3 font-medium whitespace-nowrap">请求 ID</th>
                      <th className="px-4 py-3 font-medium whitespace-nowrap">API Key</th>
                      <th className="px-4 py-3 font-medium whitespace-nowrap">模型</th>
                      <th className="px-4 py-3 font-medium whitespace-nowrap">类型</th>
                      <th className="px-4 py-3 font-medium whitespace-nowrap">时间</th>
                      <th className="px-4 py-3 font-medium whitespace-nowrap text-right">耗时</th>
                      <th className="px-4 py-3 font-medium whitespace-nowrap text-right">Token (输入/输出)</th>
                      <th className="px-4 py-3 font-medium whitespace-nowrap">状态</th>
                      <th className="px-4 py-3 font-medium whitespace-nowrap text-center">操作</th>
                    </tr>
                  </thead>
                  <tbody>
                    {logs.map((log, index) => (
                      <tr
                        key={log.id}
                        className={`
                          border-b border-white/[0.03] transition-colors cursor-pointer
                          hover:bg-white/[0.04]
                          ${index % 2 === 1 ? 'bg-white/[0.015]' : ''}
                        `}
                        onClick={() => openDetail(log)}
                      >
                        <td className="px-4 py-3 whitespace-nowrap">
                          <span className="font-mono text-zinc-400" title={log.requestId}>
                            {truncate(log.requestId, 16)}
                          </span>
                        </td>
                        <td className="px-4 py-3 whitespace-nowrap">
                          <span className="flex items-center gap-1.5 text-zinc-300">
                            <Key className="w-3 h-3 text-zinc-600" />
                            {log.apiKeyName || '-'}
                          </span>
                        </td>
                        <td className="px-4 py-3 whitespace-nowrap">
                          <span className="font-mono text-zinc-300">{log.modelCode}</span>
                        </td>
                        <td className="px-4 py-3 whitespace-nowrap">
                          <Badge
                            variant="outline"
                            className="text-[10px] px-2 py-0 h-5 border-white/[0.08] text-zinc-400 font-medium"
                          >
                            {log.modelType}
                          </Badge>
                        </td>
                        <td className="px-4 py-3 whitespace-nowrap">
                          <span className="flex items-center gap-1.5 text-zinc-400">
                            <Clock className="w-3 h-3 text-zinc-600" />
                            {formatDateTime(log.requestTime)}
                          </span>
                        </td>
                        <td className="px-4 py-3 whitespace-nowrap text-right">
                          <span className="text-zinc-400 tabular-nums">
                            {log.duration != null ? `${log.duration}ms` : '-'}
                          </span>
                        </td>
                        <td className="px-4 py-3 whitespace-nowrap text-right">
                          <span className="text-zinc-400 tabular-nums">
                            {log.inputTokens ?? 0} / {log.outputTokens ?? 0}
                          </span>
                        </td>
                        <td className="px-4 py-3 whitespace-nowrap">
                          {log.status === 'SUCCESS' ? (
                            <Badge className="bg-emerald-500/15 text-emerald-400 border border-emerald-500/20 text-[10px] px-2 py-0 h-5 font-medium">
                              成功
                            </Badge>
                          ) : (
                            <Badge className="bg-red-500/15 text-red-400 border border-red-500/20 text-[10px] px-2 py-0 h-5 font-medium">
                              失败
                            </Badge>
                          )}
                        </td>
                        <td className="px-4 py-3 whitespace-nowrap text-center">
                          <Button
                            variant="ghost"
                            size="sm"
                            className="h-7 w-7 p-0 text-zinc-500 hover:text-zinc-200 hover:bg-white/[0.06]"
                            onClick={(e) => {
                              e.stopPropagation()
                              openDetail(log)
                            }}
                          >
                            <Eye className="w-3.5 h-3.5" />
                          </Button>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </CardContent>
        </Card>
      </motion.div>

      {/* ---- Pagination ---- */}
      {!loading && logs.length > 0 && (
        <motion.div variants={itemVariants}>
          <div className="flex items-center justify-between">
            <p className="text-xs text-muted-foreground tabular-nums">
              显示 {(pageNum - 1) * PAGE_SIZE + 1} - {Math.min(pageNum * PAGE_SIZE, total)} 共 {total.toLocaleString()} 条
            </p>
            <div className="flex items-center gap-2">
              <Button
                variant="outline"
                size="sm"
                className="h-8 px-3 text-xs bg-white/[0.03] border-white/[0.06] hover:bg-white/[0.06] disabled:opacity-40"
                disabled={pageNum <= 1}
                onClick={goToPrev}
              >
                <ChevronLeft className="w-3.5 h-3.5 mr-1" />
                上一页
              </Button>
              <span className="text-xs text-muted-foreground tabular-nums px-2">
                {pageNum} / {pages}
              </span>
              <Button
                variant="outline"
                size="sm"
                className="h-8 px-3 text-xs bg-white/[0.03] border-white/[0.06] hover:bg-white/[0.06] disabled:opacity-40"
                disabled={pageNum >= pages}
                onClick={goToNext}
              >
                下一页
                <ChevronRight className="w-3.5 h-3.5 ml-1" />
              </Button>
            </div>
          </div>
        </motion.div>
      )}

      {/* ================================================================= */}
      {/* Detail Dialog                                                      */}
      {/* ================================================================= */}
      <Dialog open={detailOpen} onOpenChange={setDetailOpen}>
        <DialogContent className="sm:max-w-[720px] bg-[hsl(240_10%_5%)] border-white/[0.08] max-h-[90vh] overflow-y-auto">
          <DialogHeader>
            <DialogTitle className="flex items-center gap-2">
              <Eye className="w-5 h-5 text-blue-400" />
              请求详情
            </DialogTitle>
            <DialogDescription>
              完整的请求和响应信息。
            </DialogDescription>
          </DialogHeader>

          {detailLoading ? (
            <div className="flex flex-col items-center justify-center py-16 gap-4">
              <Loader2 className="w-6 h-6 text-primary animate-spin" />
              <p className="text-sm text-muted-foreground">加载详情中...</p>
            </div>
          ) : detail ? (
            <div className="space-y-5 py-2">
              {/* ---- Status header ---- */}
              <div className="flex items-center gap-3">
                {detail.status === 'SUCCESS' ? (
                  <Badge className="bg-emerald-500/15 text-emerald-400 border border-emerald-500/20 text-xs px-3 py-0.5 font-medium">
                    成功
                  </Badge>
                ) : (
                  <Badge className="bg-red-500/15 text-red-400 border border-red-500/20 text-xs px-3 py-0.5 font-medium">
                    失败
                  </Badge>
                )}
                {detail.errorMessage && (
                  <span className="text-xs text-red-400/80 truncate">{detail.errorMessage}</span>
                )}
              </div>

              {/* ---- Detail grid ---- */}
              <div className="grid grid-cols-2 sm:grid-cols-3 gap-4 rounded-lg bg-white/[0.02] border border-white/[0.06] p-4">
                <DetailField label="请求 ID" value={detail.requestId} mono />
                <DetailField label="API Key" value={detail.apiKeyName} />
                <DetailField label="模型" value={detail.modelCode} mono />
                <DetailField label="模型类型" value={detail.modelType} />
                <DetailField label="请求时间" value={detail.requestTime ? formatDateTime(detail.requestTime) : undefined} />
                <DetailField label="耗时" value={detail.duration != null ? `${detail.duration}ms` : undefined} />
                <DetailField label="输入 Token" value={detail.inputTokens} />
                <DetailField label="输出 Token" value={detail.outputTokens} />
                <DetailField label="客户端 IP" value={detail.clientIp} mono />
              </div>

              <Separator className="bg-white/[0.06]" />

              {/* ---- Request body ---- */}
              <JsonBlock title="请求体" content={detail.requestBody} />

              {/* ---- Response body ---- */}
              <JsonBlock title="响应体" content={detail.responseBody} />
            </div>
          ) : null}
        </DialogContent>
      </Dialog>
    </motion.div>
  )
}
