import { useState, useEffect } from 'react'
import { motion } from 'framer-motion'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Badge } from '@/components/ui/badge'
import { dashboardApi } from '@/api/dashboard'
import type { DashboardStats, DashboardTrend, TopModel, RecentLog } from '@/types/dashboard'
import { Activity, CheckCircle, DollarSign, Key, TrendingUp, ArrowUpRight, ArrowDownRight } from 'lucide-react'
import { AreaChart, Area, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer } from 'recharts'

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
  key: keyof DashboardStats
  label: string
  icon: React.ElementType
  color: string        // tailwind ring / bg accent color token
  iconBg: string       // bg- class for the icon circle
  iconText: string     // text- class for the icon itself
  format: (v: number) => string
  borderGradient: string
}

const statCards: StatCardConfig[] = [
  {
    key: 'todayRequests',
    label: '今日请求',
    icon: Activity,
    color: 'blue',
    iconBg: 'bg-blue-500/15',
    iconText: 'text-blue-400',
    format: (v) => v.toLocaleString(),
    borderGradient: 'from-blue-500/40 via-cyan-500/40 to-blue-500/0',
  },
  {
    key: 'successRate',
    label: '成功率',
    icon: CheckCircle,
    color: 'emerald',
    iconBg: 'bg-emerald-500/15',
    iconText: 'text-emerald-400',
    format: (v) => `${v.toFixed(1)}%`,
    borderGradient: 'from-emerald-500/40 via-green-400/40 to-emerald-500/0',
  },
  {
    key: 'totalCost',
    label: '总费用',
    icon: DollarSign,
    color: 'amber',
    iconBg: 'bg-amber-500/15',
    iconText: 'text-amber-400',
    format: (v) => `$${v.toFixed(2)}`,
    borderGradient: 'from-amber-500/40 via-yellow-400/40 to-amber-500/0',
  },
  {
    key: 'activeApiKeys',
    label: '活跃 API Key',
    icon: Key,
    color: 'purple',
    iconBg: 'bg-purple-500/15',
    iconText: 'text-purple-400',
    format: (v) => v.toLocaleString(),
    borderGradient: 'from-purple-500/40 via-violet-400/40 to-purple-500/0',
  },
]

// ---------------------------------------------------------------------------
// Custom Recharts tooltip
// ---------------------------------------------------------------------------
function ChartTooltip({ active, payload, label }: any) {
  if (!active || !payload?.length) return null
  return (
    <div className="rounded-lg border border-white/10 bg-[hsl(240_10%_6%)] px-3.5 py-2 shadow-xl">
      <p className="mb-1 text-xs text-zinc-400">{label}</p>
      <p className="text-sm font-semibold text-white">
        {payload[0].value.toLocaleString()} <span className="text-zinc-500 font-normal">次请求</span>
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
// Main page
// ---------------------------------------------------------------------------
export default function DashboardPage() {
  const [stats, setStats] = useState<DashboardStats | null>(null)
  const [trend, setTrend] = useState<{ date: string; value: number }[]>([])
  const [topModels, setTopModels] = useState<TopModel[]>([])
  const [recentLogs, setRecentLogs] = useState<RecentLog[]>([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    async function fetchAll() {
      try {
        const [statsData, trendData, modelsData, logsData] = await Promise.all([
          dashboardApi.getStats(),
          dashboardApi.getRequestTrend(7),
          dashboardApi.getTopModels(5),
          dashboardApi.getRecentLogs(10),
        ])

        setStats(statsData)

        // Transform trend data from {xAxis, series} into recharts format
        const chartData = trendData.xAxis.map((date, i) => ({
          date,
          value: trendData.series[0]?.data[i] ?? 0,
        }))
        setTrend(chartData)

        setTopModels(modelsData)
        setRecentLogs(logsData)
      } catch (err) {
        console.error('Dashboard fetch failed', err)
      } finally {
        setLoading(false)
      }
    }

    fetchAll()
  }, [])

  const maxModelCalls = topModels.length ? Math.max(...topModels.map((m) => m.callCount)) : 1

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
                {/* gradient border – visible on hover */}
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
      {/* Charts row                                                        */}
      {/* ----------------------------------------------------------------- */}
      <div className="grid grid-cols-1 gap-4 lg:grid-cols-3">
        {/* Request trend chart – takes 2/3 width on lg */}
        <motion.div variants={itemVariants} className="lg:col-span-2">
          <Card className="glass border-0">
            <CardHeader className="pb-2">
              <div className="flex items-center gap-2">
                <TrendingUp className="h-4 w-4 text-cyan-400" />
                <CardTitle className="text-sm font-medium text-zinc-300">
                  请求趋势
                </CardTitle>
                <span className="text-xs text-zinc-500">近 7 天</span>
              </div>
            </CardHeader>
            <CardContent className="pt-2 pb-4">
              {loading ? (
                <Skeleton className="h-[260px] w-full" />
              ) : (
                <ResponsiveContainer width="100%" height={260}>
                  <AreaChart data={trend} margin={{ top: 5, right: 10, left: -20, bottom: 0 }}>
                    <defs>
                      <linearGradient id="areaGradient" x1="0" y1="0" x2="0" y2="1">
                        <stop offset="0%" stopColor="#22d3ee" stopOpacity={0.3} />
                        <stop offset="95%" stopColor="#3b82f6" stopOpacity={0} />
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
                      tick={{ fill: '#71717a', fontSize: 11 }}
                      axisLine={false}
                      tickLine={false}
                      allowDecimals={false}
                    />
                    <Tooltip content={<ChartTooltip />} cursor={{ stroke: 'rgba(255,255,255,0.08)' }} />
                    <Area
                      type="monotone"
                      dataKey="value"
                      stroke="#22d3ee"
                      strokeWidth={2}
                      fill="url(#areaGradient)"
                      dot={false}
                      activeDot={{ r: 4, fill: '#22d3ee', stroke: '#0e1117', strokeWidth: 2 }}
                    />
                  </AreaChart>
                </ResponsiveContainer>
              )}
            </CardContent>
          </Card>
        </motion.div>

        {/* Top models – takes 1/3 width on lg */}
        <motion.div variants={itemVariants}>
          <Card className="glass border-0 h-full">
            <CardHeader className="pb-2">
              <div className="flex items-center gap-2">
                <ArrowUpRight className="h-4 w-4 text-violet-400" />
                <CardTitle className="text-sm font-medium text-zinc-300">
                  热门模型
                </CardTitle>
              </div>
            </CardHeader>
            <CardContent className="pt-2 space-y-3">
              {loading ? (
                Array.from({ length: 5 }).map((_, i) => (
                  <Skeleton key={i} className="h-8 w-full" />
                ))
              ) : topModels.length === 0 ? (
                <p className="text-sm text-zinc-500 py-4 text-center">暂无数据</p>
              ) : (
                topModels.map((model, index) => {
                  const pct = (model.callCount / maxModelCalls) * 100
                  return (
                    <div key={model.modelName} className="space-y-1.5">
                      <div className="flex items-center justify-between text-xs">
                        <span className="flex items-center gap-2 text-zinc-300">
                          <span className="flex h-5 w-5 items-center justify-center rounded-md bg-white/[0.06] text-[10px] font-bold text-zinc-400">
                            {index + 1}
                          </span>
                          <span className="truncate max-w-[140px]">{model.modelName}</span>
                        </span>
                        <span className="text-zinc-500 tabular-nums">{model.callCount.toLocaleString()}</span>
                      </div>
                      <div className="h-2 w-full overflow-hidden rounded-full bg-white/[0.04]">
                        <motion.div
                          className="h-full rounded-full bg-gradient-to-r from-violet-500 to-cyan-400"
                          initial={{ width: 0 }}
                          animate={{ width: `${pct}%` }}
                          transition={{ duration: 0.7, delay: 0.15 * index, ease: 'easeOut' }}
                        />
                      </div>
                    </div>
                  )
                })
              )}
            </CardContent>
          </Card>
        </motion.div>
      </div>

      {/* ----------------------------------------------------------------- */}
      {/* Recent logs table                                                 */}
      {/* ----------------------------------------------------------------- */}
      <motion.div variants={itemVariants}>
        <Card className="glass border-0">
          <CardHeader className="pb-2">
            <div className="flex items-center gap-2">
              <ArrowDownRight className="h-4 w-4 text-blue-400" />
              <CardTitle className="text-sm font-medium text-zinc-300">
                最近请求
              </CardTitle>
            </div>
          </CardHeader>
          <CardContent className="pt-0">
            {loading ? (
              <div className="space-y-2 pt-2">
                {Array.from({ length: 5 }).map((_, i) => (
                  <Skeleton key={i} className="h-9 w-full" />
                ))}
              </div>
            ) : (
              <div className="overflow-x-auto">
                <table className="w-full text-xs">
                  <thead>
                    <tr className="border-b border-white/[0.06] text-left text-zinc-500">
                      <th className="pb-2 pr-4 font-medium">时间</th>
                      <th className="pb-2 pr-4 font-medium">模型</th>
                      <th className="pb-2 pr-4 font-medium">API Key</th>
                      <th className="pb-2 pr-4 font-medium">状态</th>
                      <th className="pb-2 font-medium text-right">耗时</th>
                    </tr>
                  </thead>
                  <tbody>
                    {recentLogs.length === 0 ? (
                      <tr>
                        <td colSpan={5} className="py-8 text-center text-zinc-500">
                          暂无请求记录
                        </td>
                      </tr>
                    ) : (
                      recentLogs.map((log, i) => (
                        <tr
                          key={i}
                          className="border-b border-white/[0.03] transition-colors hover:bg-white/[0.02]"
                        >
                          <td className="py-2.5 pr-4 whitespace-nowrap text-zinc-400">{log.time}</td>
                          <td className="py-2.5 pr-4 whitespace-nowrap text-zinc-300 font-medium">{log.model}</td>
                          <td className="py-2.5 pr-4 whitespace-nowrap text-zinc-500 font-mono">{log.apiKey}</td>
                          <td className="py-2.5 pr-4">
                            {log.status === 200 ? (
                              <Badge className="bg-emerald-500/15 text-emerald-400 border-emerald-500/20 text-[10px] px-1.5 py-0">
                                成功
                              </Badge>
                            ) : (
                              <Badge className="bg-red-500/15 text-red-400 border-red-500/20 text-[10px] px-1.5 py-0">
                                失败
                              </Badge>
                            )}
                          </td>
                          <td className="py-2.5 text-right whitespace-nowrap text-zinc-400 tabular-nums">
                            {log.duration}
                          </td>
                        </tr>
                      ))
                    )}
                  </tbody>
                </table>
              </div>
            )}
          </CardContent>
        </Card>
      </motion.div>
    </motion.div>
  )
}
