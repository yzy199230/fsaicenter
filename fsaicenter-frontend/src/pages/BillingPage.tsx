import { useState, useEffect } from 'react'
import { motion } from 'framer-motion'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Badge } from '@/components/ui/badge'
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs'
import { billingStatsApi, billingRuleApi } from '@/api/billing'
import type { BillingStats, BillingTrend, ModelCost, BillingRule } from '@/types/billing'
import type { PageResult } from '@/types/common'
import { DollarSign, TrendingUp, Activity, Calculator, BarChart3 } from 'lucide-react'
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
  key: keyof BillingStats
  label: string
  icon: React.ElementType
  iconBg: string
  iconText: string
  format: (v: number) => string
  borderGradient: string
}

const statCards: StatCardConfig[] = [
  {
    key: 'totalCost',
    label: '总费用',
    icon: DollarSign,
    iconBg: 'bg-amber-500/15',
    iconText: 'text-amber-400',
    format: (v) => `$${v.toFixed(4)}`,
    borderGradient: 'from-amber-500/40 via-yellow-400/40 to-amber-500/0',
  },
  {
    key: 'totalRequests',
    label: '总请求数',
    icon: Activity,
    iconBg: 'bg-blue-500/15',
    iconText: 'text-blue-400',
    format: (v) => v.toLocaleString(),
    borderGradient: 'from-blue-500/40 via-cyan-500/40 to-blue-500/0',
  },
  {
    key: 'totalUsage',
    label: '总用量',
    icon: BarChart3,
    iconBg: 'bg-violet-500/15',
    iconText: 'text-violet-400',
    format: (v) => v.toLocaleString(),
    borderGradient: 'from-violet-500/40 via-purple-400/40 to-violet-500/0',
  },
  {
    key: 'avgUnitPrice',
    label: '平均单价',
    icon: Calculator,
    iconBg: 'bg-emerald-500/15',
    iconText: 'text-emerald-400',
    format: (v) => `$${v.toFixed(6)}`,
    borderGradient: 'from-emerald-500/40 via-green-400/40 to-emerald-500/0',
  },
]

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
            {entry.dataKey === 'cost' ? `$${entry.value.toFixed(4)}` : entry.value.toLocaleString()}
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
        ${payload[0].value.toFixed(4)} <span className="text-zinc-500 font-normal">费用</span>
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
  switch (type) {
    case 'TOKEN':
      return '按 Token'
    case 'REQUEST':
      return '按请求'
    case 'DURATION':
      return '按时长'
    default:
      return type
  }
}

function getBillingTypeBadge(type: string) {
  switch (type) {
    case 'TOKEN':
      return { bg: 'bg-blue-500/15', text: 'text-blue-400', border: 'border-blue-500/20' }
    case 'REQUEST':
      return { bg: 'bg-emerald-500/15', text: 'text-emerald-400', border: 'border-emerald-500/20' }
    case 'DURATION':
      return { bg: 'bg-violet-500/15', text: 'text-violet-400', border: 'border-violet-500/20' }
    default:
      return { bg: 'bg-zinc-500/15', text: 'text-zinc-400', border: 'border-zinc-500/20' }
  }
}

// ---------------------------------------------------------------------------
// Main page
// ---------------------------------------------------------------------------
export default function BillingPage() {
  const [stats, setStats] = useState<BillingStats | null>(null)
  const [trend, setTrend] = useState<BillingTrend[]>([])
  const [modelCosts, setModelCosts] = useState<ModelCost[]>([])
  const [billingRules, setBillingRules] = useState<BillingRule[]>([])
  const [loading, setLoading] = useState(true)
  const [rulesLoading, setRulesLoading] = useState(true)

  useEffect(() => {
    async function fetchOverview() {
      try {
        const [statsData, trendData, modelCostData] = await Promise.all([
          billingStatsApi.getStats(),
          billingStatsApi.getTrend(7),
          billingStatsApi.getModelCost(),
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

    async function fetchRules() {
      try {
        const data: PageResult<BillingRule> = await billingRuleApi.getPage({
          pageNum: 1,
          pageSize: 20,
        })
        setBillingRules(data.list)
      } catch (err) {
        console.error('Billing rules fetch failed', err)
      } finally {
        setRulesLoading(false)
      }
    }

    fetchOverview()
    fetchRules()
  }, [])

  const maxModelCost = modelCosts.length ? Math.max(...modelCosts.map((m) => m.totalCost)) : 1

  return (
    <motion.div
      className="space-y-6"
      variants={containerVariants}
      initial="hidden"
      animate="show"
    >
      {/* ----------------------------------------------------------------- */}
      {/* Stat cards                                                        */}
      {/* ----------------------------------------------------------------- */}
      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 xl:grid-cols-4">
        {statCards.map((cfg) => {
          const Icon = cfg.icon
          return (
            <motion.div key={cfg.key} variants={itemVariants}>
              <div className="group relative rounded-xl">
                {/* gradient border on hover */}
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
                          <p className="text-2xl font-bold tracking-tight text-white">
                            {cfg.format(stats[cfg.key])}
                          </p>
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
                    <span className="text-xs text-zinc-500">近 7 天</span>
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
                          tickFormatter={(v: number) => `$${v}`}
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
                            <span className="text-zinc-500 tabular-nums">${model.totalCost.toFixed(4)}</span>
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
                <div className="flex items-center gap-2">
                  <Calculator className="h-4 w-4 text-emerald-400" />
                  <CardTitle className="text-sm font-medium text-zinc-300">
                    计费规则
                  </CardTitle>
                  {!rulesLoading && (
                    <span className="text-xs text-zinc-500">{billingRules.length} 条规则</span>
                  )}
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
                          <th className="pb-2 font-medium text-right">状态</th>
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
                                          输入：<span className="text-zinc-200">${rule.inputUnitPrice.toFixed(6)}</span>
                                        </span>
                                      )}
                                      {rule.outputUnitPrice != null && (
                                        <span className="text-zinc-400">
                                          输出：<span className="text-zinc-200">${rule.outputUnitPrice.toFixed(6)}</span>
                                        </span>
                                      )}
                                    </>
                                  ) : (
                                    rule.unitPrice != null && (
                                      <span className="text-zinc-200">${rule.unitPrice.toFixed(6)}</span>
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
                              <td className="py-3 text-right">
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
    </motion.div>
  )
}
