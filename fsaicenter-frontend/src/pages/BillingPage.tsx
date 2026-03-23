import { useState, useEffect } from 'react'
import { motion } from 'framer-motion'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Textarea } from '@/components/ui/textarea'
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs'
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter, DialogDescription } from '@/components/ui/dialog'
import { AlertDialog, AlertDialogAction, AlertDialogCancel, AlertDialogContent, AlertDialogDescription, AlertDialogFooter, AlertDialogHeader, AlertDialogTitle } from '@/components/ui/alert-dialog'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import { billingStatsApi, billingRuleApi } from '@/api/billing'
import { modelApi } from '@/api/model'
import type { BillingStats, BillingTrend, ModelCost, BillingRule } from '@/types/billing'
import type { AiModel } from '@/types/model'
import type { PageResult } from '@/types/common'
import { toast } from 'sonner'
import { Coins, TrendingUp, Activity, Calculator, BarChart3, Plus, Pencil, Trash2, Loader2, CalendarDays, Zap, ArrowDownToLine, ArrowUpFromLine, Search } from 'lucide-react'
import { BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, LineChart, Line, ComposedChart } from 'recharts'

// ---------------------------------------------------------------------------
// Animation variants
// ---------------------------------------------------------------------------
const containerVariants = {
  hidden: { opacity: 0 },
  show: {
    opacity: 1,
    transition: { staggerChildren: 0.08 },
  },
}

const itemVariants = {
  hidden: { opacity: 0, y: 20 },
  show: { opacity: 1, y: 0, transition: { duration: 0.45, ease: 'easeOut' } },
}

// ---------------------------------------------------------------------------
// Stat card config
// ---------------------------------------------------------------------------
interface StatCardConfig {
  id: string
  label: string
  icon: React.ElementType
  iconBg: string
  iconText: string
  getValue: (s: BillingStats) => string
  subValue?: (s: BillingStats) => string
  borderGradient: string
}

const statCards: StatCardConfig[] = [
  {
    id: 'totalCost',
    label: '总费用',
    icon: Coins,
    iconBg: 'bg-amber-500/15',
    iconText: 'text-amber-400',
    getValue: (s) => `¥${s.totalCost.toFixed(4)}`,
    borderGradient: 'from-amber-500/40 via-yellow-400/40 to-amber-500/0',
  },
  {
    id: 'totalRequests',
    label: '总请求数',
    icon: Activity,
    iconBg: 'bg-blue-500/15',
    iconText: 'text-blue-400',
    getValue: (s) => s.totalRequests.toLocaleString(),
    borderGradient: 'from-blue-500/40 via-cyan-500/40 to-blue-500/0',
  },
  {
    id: 'totalTokens',
    label: '消耗 Token',
    icon: Zap,
    iconBg: 'bg-violet-500/15',
    iconText: 'text-violet-400',
    getValue: (s) => s.totalUsage.toLocaleString(),
    subValue: (s) => `输入 ${s.totalInputTokens.toLocaleString()} / 输出 ${s.totalOutputTokens.toLocaleString()}`,
    borderGradient: 'from-violet-500/40 via-purple-400/40 to-violet-500/0',
  },
  {
    id: 'avgUnitPrice',
    label: '平均单价',
    icon: Calculator,
    iconBg: 'bg-emerald-500/15',
    iconText: 'text-emerald-400',
    getValue: (s) => `¥${s.avgUnitPrice.toFixed(6)}`,
    borderGradient: 'from-emerald-500/40 via-green-400/40 to-emerald-500/0',
  },
]

// ---------------------------------------------------------------------------
// Billing type options (match backend BillingType enum)
// ---------------------------------------------------------------------------
const BILLING_TYPE_OPTIONS = [
  { value: 'TOKEN', label: '按 Token' },
  { value: 'IMAGE', label: '按图片数' },
  { value: 'AUDIO_DURATION', label: '按音频时长' },
  { value: 'VIDEO_DURATION', label: '按视频时长' },
]

// ---------------------------------------------------------------------------
// Form data
// ---------------------------------------------------------------------------
interface RuleFormData {
  modelId: string
  billingType: string
  unitPrice: string
  inputUnitPrice: string
  outputUnitPrice: string
  unitAmount: string
  currency: string
  effectiveTime: string
  expireTime: string
  description: string
}

const EMPTY_FORM: RuleFormData = {
  modelId: '',
  billingType: 'TOKEN',
  unitPrice: '',
  inputUnitPrice: '',
  outputUnitPrice: '',
  unitAmount: '1000',
  currency: 'CNY',
  effectiveTime: '',
  expireTime: '',
  description: '',
}

// ---------------------------------------------------------------------------
// Custom Recharts tooltips
// ---------------------------------------------------------------------------
function TrendTooltip({ active, payload, label }: any) {
  if (!active || !payload?.length) return null
  return (
    <div className="rounded-lg border border-white/10 bg-[hsl(240_10%_6%)] px-3.5 py-2.5 shadow-xl">
      <p className="mb-1.5 text-xs text-zinc-400">{label}</p>
      {payload.map((entry: any) => (
        <p key={entry.dataKey} className="text-sm">
          <span
            className="inline-block w-2 h-2 rounded-full mr-2"
            style={{ backgroundColor: entry.color }}
          />
          <span className="font-semibold text-white">
            {entry.dataKey === 'cost' ? `¥${entry.value.toFixed(4)}` : entry.value.toLocaleString()}
          </span>
          <span className="text-zinc-500 font-normal ml-1.5">
            {entry.dataKey === 'cost' ? '费用' : '请求数'}
          </span>
        </p>
      ))}
    </div>
  )
}

function ModelCostTooltip({ active, payload, label }: any) {
  if (!active || !payload?.length) return null
  return (
    <div className="rounded-lg border border-white/10 bg-[hsl(240_10%_6%)] px-3.5 py-2 shadow-xl">
      <p className="mb-1 text-xs text-zinc-400">{label}</p>
      <p className="text-sm font-semibold text-white">
        ¥{payload[0].value.toFixed(4)} <span className="text-zinc-500 font-normal">费用</span>
      </p>
    </div>
  )
}

// ---------------------------------------------------------------------------
// Skeleton pulse
// ---------------------------------------------------------------------------
function Skeleton({ className = '' }: { className?: string }) {
  return <div className={`animate-pulse rounded-md bg-white/[0.06] ${className}`} />
}

// ---------------------------------------------------------------------------
// Billing type label helper
// ---------------------------------------------------------------------------
function getBillingTypeLabel(type: string): string {
  const opt = BILLING_TYPE_OPTIONS.find((o) => o.value === type)
  return opt ? opt.label : type
}

function currencySymbol(currency?: string): string {
  return currency === 'USD' ? '$' : '¥'
}

function getBillingTypeBadge(type: string) {
  switch (type) {
    case 'TOKEN':
      return { bg: 'bg-blue-500/15', text: 'text-blue-400', border: 'border-blue-500/20' }
    case 'IMAGE':
      return { bg: 'bg-emerald-500/15', text: 'text-emerald-400', border: 'border-emerald-500/20' }
    case 'AUDIO_DURATION':
      return { bg: 'bg-orange-500/15', text: 'text-orange-400', border: 'border-orange-500/20' }
    case 'VIDEO_DURATION':
      return { bg: 'bg-violet-500/15', text: 'text-violet-400', border: 'border-violet-500/20' }
    default:
      return { bg: 'bg-zinc-500/15', text: 'text-zinc-400', border: 'border-zinc-500/20' }
  }
}

// ---------------------------------------------------------------------------
// Format datetime for input[type=datetime-local]
// ---------------------------------------------------------------------------
function toDatetimeLocal(s?: string): string {
  if (!s) return ''
  // backend returns "2025-01-01 00:00:00" or ISO format
  const d = new Date(s.replace(' ', 'T'))
  if (isNaN(d.getTime())) return ''
  const pad = (n: number) => n.toString().padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`
}

// ---------------------------------------------------------------------------
// Main page
// ---------------------------------------------------------------------------
// ---------------------------------------------------------------------------
// Date range helpers
// ---------------------------------------------------------------------------
function formatDateParam(date: string, end?: boolean): string {
  return date + (end ? ' 23:59:59' : ' 00:00:00')
}

function getDefaultDateRange(): { startDate: string; endDate: string } {
  const today = new Date()
  const sevenDaysAgo = new Date(today)
  sevenDaysAgo.setDate(today.getDate() - 6)
  const fmt = (d: Date) => d.toISOString().slice(0, 10)
  return { startDate: fmt(sevenDaysAgo), endDate: fmt(today) }
}

export default function BillingPage() {
  // Date range state
  const defaultRange = getDefaultDateRange()
  const [startDate, setStartDate] = useState(defaultRange.startDate)
  const [endDate, setEndDate] = useState(defaultRange.endDate)

  // Overview state
  const [stats, setStats] = useState<BillingStats | null>(null)
  const [trend, setTrend] = useState<BillingTrend[]>([])
  const [modelCosts, setModelCosts] = useState<ModelCost[]>([])
  const [loading, setLoading] = useState(true)

  // Rules state
  const [billingRules, setBillingRules] = useState<BillingRule[]>([])
  const [rulesLoading, setRulesLoading] = useState(true)

  // Models list for selector
  const [models, setModels] = useState<AiModel[]>([])

  // Dialog state
  const [dialogOpen, setDialogOpen] = useState(false)
  const [editingRule, setEditingRule] = useState<BillingRule | null>(null)
  const [form, setForm] = useState<RuleFormData>(EMPTY_FORM)
  const [saving, setSaving] = useState(false)

  // Delete state
  const [deleteTarget, setDeleteTarget] = useState<BillingRule | null>(null)
  const [deleting, setDeleting] = useState(false)

  // -------------------------------------------------------------------------
  // Fetch data
  // -------------------------------------------------------------------------
  useEffect(() => {
    fetchOverview()
    fetchRules()
    fetchModels()
  }, [])

  async function fetchOverview(start?: string, end?: string) {
    setLoading(true)
    const s = start || startDate
    const e = end || endDate
    const dateParams = {
      startTime: formatDateParam(s),
      endTime: formatDateParam(e, true),
    }
    try {
      const [statsData, trendData, modelCostData] = await Promise.all([
        billingStatsApi.getStats(dateParams),
        billingStatsApi.getTrend(dateParams),
        billingStatsApi.getModelCost(dateParams),
      ])
      setStats(statsData)
      setTrend(trendData)
      setModelCosts(modelCostData)
    } catch (err) {
      console.error('Billing overview fetch failed', err)
    } finally {
      setLoading(false)
    }
  }

  function handleDateSearch() {
    if (!startDate || !endDate) {
      toast.error('请选择完整的日期区间')
      return
    }
    if (startDate > endDate) {
      toast.error('开始日期不能晚于结束日期')
      return
    }
    fetchOverview(startDate, endDate)
  }

  async function fetchRules() {
    setRulesLoading(true)
    try {
      const data: PageResult<BillingRule> = await billingRuleApi.getPage({
        pageNum: 1,
        pageSize: 100,
      })
      setBillingRules(data.list)
    } catch (err) {
      console.error('Billing rules fetch failed', err)
    } finally {
      setRulesLoading(false)
    }
  }

  async function fetchModels() {
    try {
      const data = await modelApi.getList()
      setModels(data)
    } catch (err) {
      console.error('Models fetch failed', err)
    }
  }

  // -------------------------------------------------------------------------
  // Dialog handlers
  // -------------------------------------------------------------------------
  function openCreateDialog() {
    setEditingRule(null)
    setForm(EMPTY_FORM)
    setDialogOpen(true)
  }

  function openEditDialog(rule: BillingRule) {
    setEditingRule(rule)
    setForm({
      modelId: String(rule.modelId),
      billingType: rule.billingType,
      unitPrice: rule.unitPrice != null ? String(rule.unitPrice) : '',
      inputUnitPrice: rule.inputUnitPrice != null ? String(rule.inputUnitPrice) : '',
      outputUnitPrice: rule.outputUnitPrice != null ? String(rule.outputUnitPrice) : '',
      unitAmount: String(rule.unitAmount),
      currency: rule.currency || 'CNY',
      effectiveTime: toDatetimeLocal(rule.effectiveTime),
      expireTime: toDatetimeLocal(rule.expireTime),
      description: rule.description || '',
    })
    setDialogOpen(true)
  }

  function updateForm<K extends keyof RuleFormData>(key: K, value: RuleFormData[K]) {
    setForm((prev) => ({ ...prev, [key]: value }))
  }

  async function handleSave() {
    // Validate
    if (!editingRule && !form.modelId) {
      toast.error('请选择模型')
      return
    }
    if (!form.effectiveTime) {
      toast.error('请设置生效时间')
      return
    }
    if (form.billingType === 'TOKEN') {
      if (!form.inputUnitPrice && !form.outputUnitPrice) {
        toast.error('请填写输入单价或输出单价')
        return
      }
    } else {
      if (!form.unitPrice) {
        toast.error('请填写单位价格')
        return
      }
    }

    setSaving(true)
    try {
      const payload: Record<string, any> = {
        billingType: form.billingType,
        unitAmount: parseInt(form.unitAmount) || 1000,
        currency: form.currency || 'CNY',
        effectiveTime: form.effectiveTime ? form.effectiveTime.replace('T', ' ') + ':00' : null,
        expireTime: form.expireTime ? form.expireTime.replace('T', ' ') + ':00' : null,
        description: form.description || null,
      }

      if (form.billingType === 'TOKEN') {
        payload.inputUnitPrice = form.inputUnitPrice ? parseFloat(form.inputUnitPrice) : null
        payload.outputUnitPrice = form.outputUnitPrice ? parseFloat(form.outputUnitPrice) : null
      } else {
        payload.unitPrice = form.unitPrice ? parseFloat(form.unitPrice) : null
      }

      if (editingRule) {
        await billingRuleApi.update(editingRule.id, payload)
        toast.success('计费规则已更新')
      } else {
        payload.modelId = parseInt(form.modelId)
        await billingRuleApi.create(payload)
        toast.success('计费规则已创建')
      }

      setDialogOpen(false)
      fetchRules()
    } catch (err) {
      console.error('Save billing rule failed', err)
    } finally {
      setSaving(false)
    }
  }

  // -------------------------------------------------------------------------
  // Delete handler
  // -------------------------------------------------------------------------
  async function handleDelete() {
    if (!deleteTarget) return
    setDeleting(true)
    try {
      await billingRuleApi.delete(deleteTarget.id)
      toast.success('计费规则已删除')
      setDeleteTarget(null)
      fetchRules()
    } catch (err) {
      console.error('Delete billing rule failed', err)
    } finally {
      setDeleting(false)
    }
  }

  const maxModelCost = modelCosts.length ? Math.max(...modelCosts.map((m) => m.totalCost)) : 1
  const isTokenType = form.billingType === 'TOKEN'

  return (
    <motion.div
      className="space-y-6"
      variants={containerVariants}
      initial="hidden"
      animate="show"
    >
      {/* ----------------------------------------------------------------- */}
      {/* Date range filter                                                 */}
      {/* ----------------------------------------------------------------- */}
      <motion.div variants={itemVariants}>
        <Card className="glass border-0">
          <CardContent className="p-4">
            <div className="flex flex-wrap items-center gap-3">
              <CalendarDays className="h-4 w-4 text-zinc-400 shrink-0" />
              <span className="text-xs text-zinc-400 shrink-0">日期区间</span>
              <Input
                type="date"
                value={startDate}
                onChange={(e) => setStartDate(e.target.value)}
                className="h-8 w-[140px] bg-white/[0.04] border-white/[0.08] text-zinc-200 text-xs"
              />
              <span className="text-zinc-500 text-xs">至</span>
              <Input
                type="date"
                value={endDate}
                onChange={(e) => setEndDate(e.target.value)}
                className="h-8 w-[140px] bg-white/[0.04] border-white/[0.08] text-zinc-200 text-xs"
              />
              <Button
                size="sm"
                className="h-8 gap-1.5 bg-blue-600 hover:bg-blue-500 text-white"
                onClick={handleDateSearch}
                disabled={loading}
              >
                {loading ? <Loader2 className="h-3.5 w-3.5 animate-spin" /> : <Search className="h-3.5 w-3.5" />}
                查询
              </Button>
              <div className="flex gap-1.5 ml-auto">
                {[
                  { label: '今日', days: 0 },
                  { label: '近7天', days: 6 },
                  { label: '近30天', days: 29 },
                  { label: '近90天', days: 89 },
                ].map((p) => (
                  <Button
                    key={p.label}
                    variant="ghost"
                    size="sm"
                    className="h-7 px-2.5 text-xs text-zinc-400 hover:text-white hover:bg-white/[0.06]"
                    onClick={() => {
                      const today = new Date()
                      const from = new Date(today)
                      from.setDate(today.getDate() - p.days)
                      const fmt = (d: Date) => d.toISOString().slice(0, 10)
                      const s = fmt(from)
                      const e = fmt(today)
                      setStartDate(s)
                      setEndDate(e)
                      fetchOverview(s, e)
                    }}
                  >
                    {p.label}
                  </Button>
                ))}
              </div>
            </div>
          </CardContent>
        </Card>
      </motion.div>

      {/* ----------------------------------------------------------------- */}
      {/* Stat cards                                                        */}
      {/* ----------------------------------------------------------------- */}
      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 xl:grid-cols-4">
        {statCards.map((cfg) => {
          const Icon = cfg.icon
          return (
            <motion.div key={cfg.id} variants={itemVariants}>
              <div className="group relative rounded-xl">
                <div
                  className={`pointer-events-none absolute -inset-px rounded-xl bg-gradient-to-b ${cfg.borderGradient} opacity-0 transition-opacity duration-500 group-hover:opacity-100`}
                />
                <Card className="relative glass card-shine glass-hover border-0">
                  <CardContent className="p-5">
                    <div className="flex items-center justify-between">
                      <div className="space-y-1">
                        <p className="text-xs font-medium tracking-wide text-zinc-400 uppercase">
                          {cfg.label}
                        </p>
                        {loading || !stats ? (
                          <Skeleton className="h-8 w-24" />
                        ) : (
                          <>
                            <p className="text-2xl font-bold tracking-tight text-white">
                              {cfg.getValue(stats)}
                            </p>
                            {cfg.subValue && (
                              <p className="text-[11px] text-zinc-500 mt-0.5">
                                {cfg.subValue(stats)}
                              </p>
                            )}
                          </>
                        )}
                      </div>
                      <div className={`flex h-11 w-11 shrink-0 items-center justify-center rounded-lg ${cfg.iconBg}`}>
                        <Icon className={`h-5 w-5 ${cfg.iconText}`} />
                      </div>
                    </div>
                  </CardContent>
                </Card>
              </div>
            </motion.div>
          )
        })}
      </div>

      {/* ----------------------------------------------------------------- */}
      {/* Tabs: Overview / Billing Rules                                    */}
      {/* ----------------------------------------------------------------- */}
      <motion.div variants={itemVariants}>
        <Tabs defaultValue="overview" className="space-y-4">
          <TabsList className="bg-white/[0.04] border border-white/[0.06]">
            <TabsTrigger
              value="overview"
              className="data-[state=active]:bg-white/[0.08] data-[state=active]:text-white text-zinc-400 gap-1.5"
            >
              <TrendingUp className="h-3.5 w-3.5" />
              概览
            </TabsTrigger>
            <TabsTrigger
              value="rules"
              className="data-[state=active]:bg-white/[0.08] data-[state=active]:text-white text-zinc-400 gap-1.5"
            >
              <Calculator className="h-3.5 w-3.5" />
              计费规则
            </TabsTrigger>
          </TabsList>

          {/* ============================================================= */}
          {/* Overview Tab                                                   */}
          {/* ============================================================= */}
          <TabsContent value="overview" className="space-y-4">
            <div className="grid grid-cols-1 gap-4 lg:grid-cols-3">
              {/* Cost trend chart (2/3 width) */}
              <Card className="glass border-0 lg:col-span-2">
                <CardHeader className="pb-2">
                  <div className="flex items-center gap-2">
                    <TrendingUp className="h-4 w-4 text-cyan-400" />
                    <CardTitle className="text-sm font-medium text-zinc-300">
                      费用趋势
                    </CardTitle>
                    <span className="text-xs text-zinc-500">{startDate} ~ {endDate}</span>
                  </div>
                </CardHeader>
                <CardContent className="pt-2 pb-4">
                  {loading ? (
                    <Skeleton className="h-[280px] w-full" />
                  ) : trend.length === 0 ? (
                    <div className="flex items-center justify-center h-[280px]">
                      <p className="text-sm text-zinc-500">暂无趋势数据</p>
                    </div>
                  ) : (
                    <ResponsiveContainer width="100%" height={280}>
                      <ComposedChart data={trend} margin={{ top: 5, right: 10, left: -10, bottom: 0 }}>
                        <defs>
                          <linearGradient id="costBarGradient" x1="0" y1="0" x2="0" y2="1">
                            <stop offset="0%" stopColor="#f59e0b" stopOpacity={0.8} />
                            <stop offset="95%" stopColor="#f59e0b" stopOpacity={0.2} />
                          </linearGradient>
                        </defs>
                        <CartesianGrid
                          strokeDasharray="3 3"
                          stroke="rgba(255,255,255,0.04)"
                          vertical={false}
                        />
                        <XAxis
                          dataKey="date"
                          tick={{ fill: '#71717a', fontSize: 11 }}
                          axisLine={false}
                          tickLine={false}
                        />
                        <YAxis
                          yAxisId="cost"
                          tick={{ fill: '#71717a', fontSize: 11 }}
                          axisLine={false}
                          tickLine={false}
                          tickFormatter={(v: number) => `¥${v}`}
                        />
                        <YAxis
                          yAxisId="requests"
                          orientation="right"
                          tick={{ fill: '#71717a', fontSize: 11 }}
                          axisLine={false}
                          tickLine={false}
                          allowDecimals={false}
                        />
                        <Tooltip content={<TrendTooltip />} cursor={{ fill: 'rgba(255,255,255,0.03)' }} />
                        <Bar
                          yAxisId="cost"
                          dataKey="cost"
                          fill="url(#costBarGradient)"
                          radius={[4, 4, 0, 0]}
                          maxBarSize={40}
                        />
                        <Line
                          yAxisId="requests"
                          type="monotone"
                          dataKey="requests"
                          stroke="#22d3ee"
                          strokeWidth={2}
                          dot={false}
                          activeDot={{ r: 4, fill: '#22d3ee', stroke: '#0e1117', strokeWidth: 2 }}
                        />
                      </ComposedChart>
                    </ResponsiveContainer>
                  )}
                </CardContent>
              </Card>

              {/* Model cost ranking (1/3 width) */}
              <Card className="glass border-0 h-full">
                <CardHeader className="pb-2">
                  <div className="flex items-center gap-2">
                    <BarChart3 className="h-4 w-4 text-violet-400" />
                    <CardTitle className="text-sm font-medium text-zinc-300">
                      模型费用排名
                    </CardTitle>
                  </div>
                </CardHeader>
                <CardContent className="pt-2 space-y-3">
                  {loading ? (
                    Array.from({ length: 5 }).map((_, i) => (
                      <Skeleton key={i} className="h-10 w-full" />
                    ))
                  ) : modelCosts.length === 0 ? (
                    <p className="text-sm text-zinc-500 py-4 text-center">暂无数据</p>
                  ) : (
                    modelCosts.map((model, index) => {
                      const pct = maxModelCost > 0 ? (model.totalCost / maxModelCost) * 100 : 0
                      return (
                        <div key={model.modelId} className="space-y-1.5">
                          <div className="flex items-center justify-between text-xs">
                            <span className="flex items-center gap-2 text-zinc-300">
                              <span className="flex h-5 w-5 items-center justify-center rounded-md bg-white/[0.06] text-[10px] font-bold text-zinc-400">
                                {index + 1}
                              </span>
                              <span className="truncate max-w-[120px]">{model.modelName}</span>
                            </span>
                            <span className="text-zinc-500 tabular-nums">¥{model.totalCost.toFixed(4)}</span>
                          </div>
                          <div className="h-2 w-full overflow-hidden rounded-full bg-white/[0.04]">
                            <motion.div
                              className="h-full rounded-full bg-gradient-to-r from-amber-500 to-orange-400"
                              initial={{ width: 0 }}
                              animate={{ width: `${pct}%` }}
                              transition={{ duration: 0.7, delay: 0.1 * index, ease: 'easeOut' }}
                            />
                          </div>
                          <div className="flex items-center gap-3 text-[10px] text-zinc-500">
                            <span>{model.requests.toLocaleString()} 请求数</span>
                            <span>{model.usage.toLocaleString()} 用量</span>
                          </div>
                        </div>
                      )
                    })
                  )}
                </CardContent>
              </Card>
            </div>
          </TabsContent>

          {/* ============================================================= */}
          {/* Billing Rules Tab                                              */}
          {/* ============================================================= */}
          <TabsContent value="rules">
            <Card className="glass border-0">
              <CardHeader className="pb-2">
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-2">
                    <Calculator className="h-4 w-4 text-emerald-400" />
                    <CardTitle className="text-sm font-medium text-zinc-300">
                      计费规则
                    </CardTitle>
                    {!rulesLoading && (
                      <span className="text-xs text-zinc-500">{billingRules.length} 条规则</span>
                    )}
                  </div>
                  <Button
                    size="sm"
                    className="h-8 gap-1.5 bg-emerald-600 hover:bg-emerald-500 text-white"
                    onClick={openCreateDialog}
                  >
                    <Plus className="h-3.5 w-3.5" />
                    新增规则
                  </Button>
                </div>
              </CardHeader>
              <CardContent className="pt-0">
                {rulesLoading ? (
                  <div className="space-y-2 pt-2">
                    {Array.from({ length: 5 }).map((_, i) => (
                      <Skeleton key={i} className="h-12 w-full" />
                    ))}
                  </div>
                ) : billingRules.length === 0 ? (
                  <div className="flex flex-col items-center justify-center py-16 gap-3">
                    <div className="w-14 h-14 rounded-2xl bg-white/[0.04] border border-white/[0.06] flex items-center justify-center">
                      <Calculator className="w-7 h-7 text-muted-foreground" />
                    </div>
                    <p className="text-sm text-zinc-500">暂无计费规则</p>
                    <Button
                      size="sm"
                      variant="outline"
                      className="mt-2 gap-1.5 border-white/[0.08] text-zinc-300 hover:bg-white/[0.06]"
                      onClick={openCreateDialog}
                    >
                      <Plus className="h-3.5 w-3.5" />
                      创建第一条规则
                    </Button>
                  </div>
                ) : (
                  <div className="overflow-x-auto">
                    <table className="w-full text-xs">
                      <thead>
                        <tr className="border-b border-white/[0.06] text-left text-zinc-500">
                          <th className="pb-2 pr-4 font-medium">模型</th>
                          <th className="pb-2 pr-4 font-medium">类型</th>
                          <th className="pb-2 pr-4 font-medium">单价</th>
                          <th className="pb-2 pr-4 font-medium">计费单位</th>
                          <th className="pb-2 pr-4 font-medium">生效时间</th>
                          <th className="pb-2 pr-4 font-medium">状态</th>
                          <th className="pb-2 font-medium text-right">操作</th>
                        </tr>
                      </thead>
                      <tbody>
                        {billingRules.map((rule, i) => {
                          const typeBadge = getBillingTypeBadge(rule.billingType)
                          const isActive = rule.status === 1
                          return (
                            <motion.tr
                              key={rule.id}
                              initial={{ opacity: 0, y: 8 }}
                              animate={{ opacity: 1, y: 0 }}
                              transition={{ duration: 0.3, delay: i * 0.03 }}
                              className="border-b border-white/[0.03] transition-colors hover:bg-white/[0.02]"
                            >
                              <td className="py-3 pr-4">
                                <div className="flex flex-col">
                                  <span className="text-zinc-300 font-medium">
                                    {rule.modelName || `Model #${rule.modelId}`}
                                  </span>
                                  {rule.modelType && (
                                    <span className="text-[10px] text-zinc-500 mt-0.5">{rule.modelType}</span>
                                  )}
                                </div>
                              </td>
                              <td className="py-3 pr-4">
                                <Badge className={`text-[10px] px-2 py-0 h-5 ${typeBadge.text} ${typeBadge.bg} ${typeBadge.border} border font-medium`}>
                                  {getBillingTypeLabel(rule.billingType)}
                                </Badge>
                              </td>
                              <td className="py-3 pr-4">
                                <div className="flex flex-col gap-0.5 tabular-nums">
                                  {rule.billingType === 'TOKEN' ? (
                                    <>
                                      {rule.inputUnitPrice != null && (
                                        <span className="text-zinc-400">
                                          输入：<span className="text-zinc-200">{currencySymbol(rule.currency)}{rule.inputUnitPrice.toFixed(6)}</span>
                                        </span>
                                      )}
                                      {rule.outputUnitPrice != null && (
                                        <span className="text-zinc-400">
                                          输出：<span className="text-zinc-200">{currencySymbol(rule.currency)}{rule.outputUnitPrice.toFixed(6)}</span>
                                        </span>
                                      )}
                                    </>
                                  ) : (
                                    rule.unitPrice != null && (
                                      <span className="text-zinc-200">{currencySymbol(rule.currency)}{rule.unitPrice.toFixed(6)}</span>
                                    )
                                  )}
                                </div>
                              </td>
                              <td className="py-3 pr-4 text-zinc-400 tabular-nums">
                                {rule.unitAmount.toLocaleString()}
                              </td>
                              <td className="py-3 pr-4 whitespace-nowrap text-zinc-400">
                                {rule.effectiveTime}
                              </td>
                              <td className="py-3 pr-4">
                                {isActive ? (
                                  <Badge className="bg-emerald-500/15 text-emerald-400 border-emerald-500/20 border text-[10px] px-1.5 py-0">
                                    生效中
                                  </Badge>
                                ) : (
                                  <Badge className="bg-zinc-500/15 text-zinc-400 border-zinc-500/20 border text-[10px] px-1.5 py-0">
                                    已停用
                                  </Badge>
                                )}
                              </td>
                              <td className="py-3 text-right">
                                <div className="flex items-center justify-end gap-1">
                                  <Button
                                    variant="ghost"
                                    size="sm"
                                    className="h-7 w-7 p-0 text-zinc-400 hover:text-blue-400 hover:bg-blue-500/10"
                                    onClick={() => openEditDialog(rule)}
                                  >
                                    <Pencil className="h-3.5 w-3.5" />
                                  </Button>
                                  <Button
                                    variant="ghost"
                                    size="sm"
                                    className="h-7 w-7 p-0 text-zinc-400 hover:text-red-400 hover:bg-red-500/10"
                                    onClick={() => setDeleteTarget(rule)}
                                  >
                                    <Trash2 className="h-3.5 w-3.5" />
                                  </Button>
                                </div>
                              </td>
                            </motion.tr>
                          )
                        })}
                      </tbody>
                    </table>
                  </div>
                )}
              </CardContent>
            </Card>
          </TabsContent>
        </Tabs>
      </motion.div>

      {/* ================================================================= */}
      {/* Create / Edit Dialog                                               */}
      {/* ================================================================= */}
      <Dialog open={dialogOpen} onOpenChange={setDialogOpen}>
        <DialogContent className="sm:max-w-[520px] bg-[hsl(240_10%_6%)] border-white/[0.08]">
          <DialogHeader>
            <DialogTitle className="text-zinc-100">
              {editingRule ? '编辑计费规则' : '新增计费规则'}
            </DialogTitle>
            <DialogDescription className="text-zinc-500">
              {editingRule ? '修改计费规则的价格和有效期配置' : '为模型设置计费规则，定义价格和有效期'}
            </DialogDescription>
          </DialogHeader>

          <div className="space-y-4 py-2">
            {/* Model selector (only for create) */}
            {!editingRule && (
              <div className="space-y-2">
                <Label className="text-zinc-300">模型 <span className="text-red-400">*</span></Label>
                <Select value={form.modelId} onValueChange={(v) => updateForm('modelId', v)}>
                  <SelectTrigger className="bg-white/[0.04] border-white/[0.08] text-zinc-200">
                    <SelectValue placeholder="选择模型" />
                  </SelectTrigger>
                  <SelectContent className="bg-[hsl(240_10%_8%)] border-white/[0.08]">
                    {models.map((m) => (
                      <SelectItem key={m.id} value={String(m.id)} className="text-zinc-200 focus:bg-white/[0.06]">
                        {m.name} <span className="text-zinc-500 ml-1">({m.code})</span>
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>
            )}

            {/* Billing type */}
            <div className="space-y-2">
              <Label className="text-zinc-300">计费类型 <span className="text-red-400">*</span></Label>
              <Select
                value={form.billingType}
                onValueChange={(v) => updateForm('billingType', v)}
                disabled={!!editingRule}
              >
                <SelectTrigger className="bg-white/[0.04] border-white/[0.08] text-zinc-200">
                  <SelectValue />
                </SelectTrigger>
                <SelectContent className="bg-[hsl(240_10%_8%)] border-white/[0.08]">
                  {BILLING_TYPE_OPTIONS.map((opt) => (
                    <SelectItem key={opt.value} value={opt.value} className="text-zinc-200 focus:bg-white/[0.06]">
                      {opt.label}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>

            {/* Prices */}
            {isTokenType ? (
              <div className="grid grid-cols-2 gap-3">
                <div className="space-y-2">
                  <Label className="text-zinc-300">输入单价</Label>
                  <Input
                    type="number"
                    step="0.000001"
                    min="0"
                    placeholder="0.000000"
                    value={form.inputUnitPrice}
                    onChange={(e) => updateForm('inputUnitPrice', e.target.value)}
                    className="bg-white/[0.04] border-white/[0.08] text-zinc-200 tabular-nums"
                  />
                  <p className="text-[10px] text-zinc-500">每 {form.unitAmount || 1000} Token</p>
                </div>
                <div className="space-y-2">
                  <Label className="text-zinc-300">输出单价</Label>
                  <Input
                    type="number"
                    step="0.000001"
                    min="0"
                    placeholder="0.000000"
                    value={form.outputUnitPrice}
                    onChange={(e) => updateForm('outputUnitPrice', e.target.value)}
                    className="bg-white/[0.04] border-white/[0.08] text-zinc-200 tabular-nums"
                  />
                  <p className="text-[10px] text-zinc-500">每 {form.unitAmount || 1000} Token</p>
                </div>
              </div>
            ) : (
              <div className="space-y-2">
                <Label className="text-zinc-300">单位价格 <span className="text-red-400">*</span></Label>
                <Input
                  type="number"
                  step="0.000001"
                  min="0"
                  placeholder="0.000000"
                  value={form.unitPrice}
                  onChange={(e) => updateForm('unitPrice', e.target.value)}
                  className="bg-white/[0.04] border-white/[0.08] text-zinc-200 tabular-nums"
                />
              </div>
            )}

            {/* Unit amount & Currency */}
            <div className="grid grid-cols-2 gap-3">
              <div className="space-y-2">
                <Label className="text-zinc-300">计费单位数量</Label>
                <Input
                  type="number"
                  min="1"
                  placeholder="1000"
                  value={form.unitAmount}
                  onChange={(e) => updateForm('unitAmount', e.target.value)}
                  className="bg-white/[0.04] border-white/[0.08] text-zinc-200"
                />
                <p className="text-[10px] text-zinc-500">
                  {isTokenType ? '每多少 Token 为一个计价单位' : '每多少单位为一个计价单位'}
                </p>
              </div>
              <div className="space-y-2">
                <Label className="text-zinc-300">货币</Label>
                <Select value={form.currency} onValueChange={(v) => updateForm('currency', v)}>
                  <SelectTrigger className="bg-white/[0.04] border-white/[0.08] text-zinc-200">
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent className="bg-[hsl(240_10%_8%)] border-white/[0.08]">
                    <SelectItem value="CNY" className="text-zinc-200 focus:bg-white/[0.06]">CNY (人民币)</SelectItem>
                    <SelectItem value="USD" className="text-zinc-200 focus:bg-white/[0.06]">USD (美元)</SelectItem>
                  </SelectContent>
                </Select>
              </div>
            </div>

            {/* Effective time & Expire time */}
            <div className="grid grid-cols-2 gap-3">
              <div className="space-y-2">
                <Label className="text-zinc-300">生效时间 <span className="text-red-400">*</span></Label>
                <Input
                  type="datetime-local"
                  value={form.effectiveTime}
                  onChange={(e) => updateForm('effectiveTime', e.target.value)}
                  className="bg-white/[0.04] border-white/[0.08] text-zinc-200"
                />
              </div>
              <div className="space-y-2">
                <Label className="text-zinc-300">过期时间</Label>
                <Input
                  type="datetime-local"
                  value={form.expireTime}
                  onChange={(e) => updateForm('expireTime', e.target.value)}
                  className="bg-white/[0.04] border-white/[0.08] text-zinc-200"
                />
                <p className="text-[10px] text-zinc-500">不填则永不过期</p>
              </div>
            </div>

            {/* Description */}
            <div className="space-y-2">
              <Label className="text-zinc-300">描述</Label>
              <Textarea
                placeholder="可选：描述此计费规则的用途"
                value={form.description}
                onChange={(e) => updateForm('description', e.target.value)}
                rows={2}
                className="bg-white/[0.04] border-white/[0.08] text-zinc-200 resize-none"
              />
            </div>
          </div>

          <DialogFooter>
            <Button
              variant="outline"
              onClick={() => setDialogOpen(false)}
              className="border-white/[0.08] text-zinc-300 hover:bg-white/[0.06]"
            >
              取消
            </Button>
            <Button
              onClick={handleSave}
              disabled={saving}
              className="bg-emerald-600 hover:bg-emerald-500 text-white"
            >
              {saving && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
              {editingRule ? '保存修改' : '创建规则'}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* ================================================================= */}
      {/* Delete Confirmation                                                */}
      {/* ================================================================= */}
      <AlertDialog open={!!deleteTarget} onOpenChange={(open) => !open && setDeleteTarget(null)}>
        <AlertDialogContent className="bg-[hsl(240_10%_6%)] border-white/[0.08]">
          <AlertDialogHeader>
            <AlertDialogTitle className="text-zinc-100">确认删除</AlertDialogTitle>
            <AlertDialogDescription className="text-zinc-400">
              确定要删除模型「{deleteTarget?.modelName || `Model #${deleteTarget?.modelId}`}」的
              {deleteTarget ? getBillingTypeLabel(deleteTarget.billingType) : ''}计费规则吗？此操作不可撤销。
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel className="border-white/[0.08] text-zinc-300 hover:bg-white/[0.06]">
              取消
            </AlertDialogCancel>
            <AlertDialogAction
              onClick={handleDelete}
              disabled={deleting}
              className="bg-red-600 hover:bg-red-500 text-white"
            >
              {deleting && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
              删除
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </motion.div>
  )
}
