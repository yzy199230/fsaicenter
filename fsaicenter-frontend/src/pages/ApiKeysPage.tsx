import { useState, useEffect } from 'react'
import { motion } from 'framer-motion'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Badge } from '@/components/ui/badge'
import { Progress } from '@/components/ui/progress'
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter, DialogDescription } from '@/components/ui/dialog'
import { Label } from '@/components/ui/label'
import { Textarea } from '@/components/ui/textarea'
import { AlertDialog, AlertDialogAction, AlertDialogCancel, AlertDialogContent, AlertDialogDescription, AlertDialogFooter, AlertDialogHeader, AlertDialogTitle } from '@/components/ui/alert-dialog'
import { apiKeyApi } from '@/api/apikey'
import type { ApiKey, CreateApiKeyRequest, UpdateApiKeyRequest } from '@/types/apikey'
import { ModelMultiSelect } from '@/components/ModelMultiSelect'
import { toast } from 'sonner'
import { Plus, Search, Key, Copy, Pencil, Trash2, Power, RotateCcw, Loader2, Infinity, Clock, Gauge, Shield } from 'lucide-react'

// ---------------------------------------------------------------------------
// Constants & helpers
// ---------------------------------------------------------------------------

interface FormData {
  keyName: string
  description: string
  quotaTotal: number
  rateLimitPerMinute?: number
  rateLimitPerDay?: number
  allowedModelIds: number[]
  expireTime: string
}

const EMPTY_FORM: FormData = {
  keyName: '',
  description: '',
  quotaTotal: -1,
  rateLimitPerMinute: undefined,
  rateLimitPerDay: undefined,
  allowedModelIds: [],
  expireTime: '',
}

function maskKey(key: string): string {
  if (!key) return 'sk-****'
  if (key.length <= 8) return 'sk-****'
  return key.slice(0, 5) + '****' + key.slice(-4)
}

function formatQuota(used: number, total: number): string {
  if (total === -1) return `已用 ${used.toLocaleString()}`
  return `${used.toLocaleString()} / ${total.toLocaleString()}`
}

function quotaPercent(used: number, total: number): number {
  if (total <= 0) return 0
  return Math.min(100, Math.round((used / total) * 100))
}

function formatExpireTime(time?: string): string {
  if (!time) return '永不过期'
  const d = new Date(time)
  if (isNaN(d.getTime())) return '永不过期'
  const now = new Date()
  if (d < now) return '已过期'
  return d.toLocaleDateString('zh-CN', {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  })
}

function isExpired(time?: string): boolean {
  if (!time) return false
  const d = new Date(time)
  if (isNaN(d.getTime())) return false
  return d < new Date()
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
  hidden: { opacity: 0, y: 20, scale: 0.97 },
  show: {
    opacity: 1,
    y: 0,
    scale: 1,
    transition: { duration: 0.4, ease: [0.23, 1, 0.32, 1] },
  },
}

// ---------------------------------------------------------------------------
// API Key card
// ---------------------------------------------------------------------------

function ApiKeyCard({
  apiKey,
  onEdit,
  onToggle,
  onDelete,
  onResetQuota,
}: {
  apiKey: ApiKey
  onEdit: (k: ApiKey) => void
  onToggle: (k: ApiKey) => void
  onDelete: (k: ApiKey) => void
  onResetQuota: (k: ApiKey) => void
}) {
  const [revealed, setRevealed] = useState(false)
  const [copying, setCopying] = useState(false)
  const isActive = apiKey.status === 1
  const unlimited = apiKey.quotaTotal === -1
  const expired = isExpired(apiKey.expireTime)

  const handleCopy = async (e: React.MouseEvent) => {
    e.stopPropagation()
    try {
      await navigator.clipboard.writeText(apiKey.keyValue)
      setCopying(true)
      toast.success('API Key 已复制到剪贴板')
      setTimeout(() => setCopying(false), 1500)
    } catch {
      toast.error('复制失败')
    }
  }

  const handleReveal = () => {
    setRevealed(true)
    setTimeout(() => setRevealed(false), 3000)
  }

  return (
    <Card
      className={`
        group relative card-shine
        bg-white/[0.03] border-white/[0.06]
        hover:bg-white/[0.06] hover:border-white/[0.1]
        hover:shadow-[0_0_30px_-5px_rgba(100,140,255,0.08)]
        transition-all duration-300
      `}
    >
      {/* Colored top accent line */}
      <div
        className={`absolute inset-x-0 top-0 h-[1px] bg-gradient-to-r ${
          isActive && !expired
            ? 'from-blue-500/40 via-cyan-500/40 to-blue-500/0'
            : 'from-zinc-500/30 via-zinc-400/20 to-zinc-500/0'
        } opacity-60 group-hover:opacity-100 transition-opacity`}
      />

      <CardContent className="p-5 space-y-4">
        {/* ---- Header row ---- */}
        <div className="flex items-start justify-between gap-3">
          <div className="flex items-start gap-3 min-w-0">
            {/* Key icon circle */}
            <div
              className={`shrink-0 w-10 h-10 rounded-xl flex items-center justify-center ring-1 ring-inset ring-white/[0.08] ${
                isActive && !expired ? 'bg-blue-500/15' : 'bg-zinc-500/15'
              }`}
            >
              <Key
                className={`w-5 h-5 ${isActive && !expired ? 'text-blue-400' : 'text-zinc-500'}`}
              />
            </div>

            <div className="min-w-0">
              <h3 className="text-sm font-semibold text-foreground truncate leading-tight">
                {apiKey.keyName}
              </h3>
              {apiKey.description && (
                <p className="text-xs text-muted-foreground truncate mt-0.5 max-w-[260px]">
                  {apiKey.description}
                </p>
              )}
            </div>
          </div>

          {/* Status dot */}
          <div className="flex items-center gap-1.5 shrink-0 pt-1">
            <div className={isActive && !expired ? 'dot-online' : 'dot-offline'} />
            <span
              className={`text-[10px] font-medium ${
                expired
                  ? 'text-red-400'
                  : isActive
                    ? 'text-emerald-400'
                    : 'text-zinc-500'
              }`}
            >
              {expired ? '已过期' : isActive ? '活跃' : '已禁用'}
            </span>
          </div>
        </div>

        {/* ---- Key value row ---- */}
        <div className="flex items-center gap-2 rounded-lg bg-white/[0.02] border border-white/[0.04] px-3 py-2">
          <code
            className="flex-1 text-xs font-mono text-muted-foreground truncate cursor-pointer select-none"
            onClick={handleReveal}
            title="点击显示"
          >
            {revealed ? apiKey.keyValue : maskKey(apiKey.keyValue)}
          </code>
          <Button
            variant="ghost"
            size="sm"
            className="h-6 w-6 p-0 text-muted-foreground hover:text-foreground hover:bg-white/[0.06] shrink-0"
            onClick={handleCopy}
          >
            {copying ? (
              <span className="text-emerald-400 text-[10px] font-medium">OK</span>
            ) : (
              <Copy className="w-3.5 h-3.5" />
            )}
          </Button>
        </div>

        {/* ---- Quota section ---- */}
        <div className="space-y-2">
          <div className="flex items-center justify-between text-xs">
            <span className="text-muted-foreground flex items-center gap-1.5">
              <Gauge className="w-3 h-3" />
              配额
            </span>
            {unlimited ? (
              <Badge className="text-[10px] px-2 py-0 h-5 bg-violet-500/10 text-violet-400 border border-violet-500/20 font-medium gap-1">
                <Infinity className="w-3 h-3" />
                无限制
              </Badge>
            ) : (
              <span className="text-muted-foreground tabular-nums">
                {formatQuota(apiKey.quotaUsed, apiKey.quotaTotal)}
              </span>
            )}
          </div>
          {!unlimited && (
            <div className="relative">
              <Progress
                value={quotaPercent(apiKey.quotaUsed, apiKey.quotaTotal)}
                className="h-1.5 bg-white/[0.06]"
              />
              {quotaPercent(apiKey.quotaUsed, apiKey.quotaTotal) > 80 && (
                <div className="absolute right-0 -top-0.5 w-1.5 h-1.5 rounded-full bg-amber-400 animate-pulse" />
              )}
            </div>
          )}
        </div>

        {/* ---- Rate limits & Expire time ---- */}
        <div className="flex flex-wrap items-center gap-1.5">
          {apiKey.rateLimitPerMinute != null && apiKey.rateLimitPerMinute > 0 && (
            <Badge
              variant="outline"
              className="text-[10px] px-2 py-0 h-5 text-muted-foreground border-white/[0.08] gap-1 tabular-nums"
            >
              <Gauge className="w-3 h-3" />
              {apiKey.rateLimitPerMinute}/分钟
            </Badge>
          )}
          {apiKey.rateLimitPerDay != null && apiKey.rateLimitPerDay > 0 && (
            <Badge
              variant="outline"
              className="text-[10px] px-2 py-0 h-5 text-muted-foreground border-white/[0.08] gap-1 tabular-nums"
            >
              <Gauge className="w-3 h-3" />
              {apiKey.rateLimitPerDay}/天
            </Badge>
          )}
          {apiKey.allowedModelIds && apiKey.allowedModelIds.length > 0 && (
            <Badge
              variant="outline"
              className="text-[10px] px-2 py-0 h-5 text-blue-400 border-blue-500/20 gap-1 tabular-nums"
            >
              <Shield className="w-3 h-3" />
              {apiKey.allowedModelIds.length} 个模型
            </Badge>
          )}
          <Badge
            variant="outline"
            className={`text-[10px] px-2 py-0 h-5 border-white/[0.08] gap-1 ${
              expired ? 'text-red-400 border-red-500/20' : 'text-muted-foreground'
            }`}
          >
            <Clock className="w-3 h-3" />
            {formatExpireTime(apiKey.expireTime)}
          </Badge>
        </div>

        {/* ---- Created time ---- */}
        <p className="text-[10px] text-zinc-600">
          创建于 {new Date(apiKey.createdTime).toLocaleDateString('zh-CN', {
            year: 'numeric',
            month: 'short',
            day: 'numeric',
          })}
        </p>

        {/* ---- Actions ---- */}
        <div className="flex items-center gap-1.5 pt-1">
          <Button
            variant="ghost"
            size="sm"
            className="h-8 px-2.5 text-xs text-muted-foreground hover:text-foreground hover:bg-white/[0.06]"
            onClick={() => onEdit(apiKey)}
          >
            <Pencil className="w-3.5 h-3.5 mr-1.5" />
            编辑
          </Button>
          <Button
            variant="ghost"
            size="sm"
            className={`h-8 px-2.5 text-xs hover:bg-white/[0.06] ${
              isActive
                ? 'text-emerald-400 hover:text-emerald-300'
                : 'text-muted-foreground hover:text-foreground'
            }`}
            onClick={() => onToggle(apiKey)}
          >
            <Power className="w-3.5 h-3.5 mr-1.5" />
            {isActive ? '禁用' : '启用'}
          </Button>
          <Button
            variant="ghost"
            size="sm"
            className="h-8 px-2.5 text-xs text-muted-foreground hover:text-amber-400 hover:bg-amber-500/10"
            onClick={() => onResetQuota(apiKey)}
            title="重置配额"
          >
            <RotateCcw className="w-3.5 h-3.5" />
          </Button>
          <div className="flex-1" />
          <Button
            variant="ghost"
            size="sm"
            className="h-8 px-2.5 text-xs text-muted-foreground hover:text-red-400 hover:bg-red-500/10"
            onClick={() => onDelete(apiKey)}
          >
            <Trash2 className="w-3.5 h-3.5" />
          </Button>
        </div>
      </CardContent>
    </Card>
  )
}

// ---------------------------------------------------------------------------
// Main page component
// ---------------------------------------------------------------------------

export default function ApiKeysPage() {
  // ---- data state ----
  const [apiKeys, setApiKeys] = useState<ApiKey[]>([])
  const [loading, setLoading] = useState(true)

  // ---- filter state ----
  const [search, setSearch] = useState('')

  // ---- dialog state ----
  const [dialogOpen, setDialogOpen] = useState(false)
  const [editingKey, setEditingKey] = useState<ApiKey | null>(null)
  const [form, setForm] = useState<FormData>({ ...EMPTY_FORM })
  const [saving, setSaving] = useState(false)

  // ---- delete confirm ----
  const [deleteTarget, setDeleteTarget] = useState<ApiKey | null>(null)
  const [deleting, setDeleting] = useState(false)

  // ---- reset quota confirm ----
  const [resetTarget, setResetTarget] = useState<ApiKey | null>(null)
  const [resetting, setResetting] = useState(false)

  // ---- data fetching ----
  const fetchKeys = async () => {
    try {
      const data = await apiKeyApi.getList()
      setApiKeys(data)
    } catch {
      toast.error('加载 API Key 失败')
    }
  }

  useEffect(() => {
    const init = async () => {
      setLoading(true)
      await fetchKeys()
      setLoading(false)
    }
    init()
  }, [])

  // ---- filtering ----
  const filtered = apiKeys.filter((k) => {
    if (!search) return true
    const q = search.toLowerCase()
    return (
      k.keyName.toLowerCase().includes(q) ||
      k.keyValue.toLowerCase().includes(q) ||
      (k.description ?? '').toLowerCase().includes(q)
    )
  })

  // ---- CRUD helpers ----
  const openCreateDialog = () => {
    setEditingKey(null)
    setForm({ ...EMPTY_FORM })
    setDialogOpen(true)
  }

  const openEditDialog = (apiKey: ApiKey) => {
    setEditingKey(apiKey)
    setForm({
      keyName: apiKey.keyName,
      description: apiKey.description ?? '',
      quotaTotal: apiKey.quotaTotal,
      rateLimitPerMinute: apiKey.rateLimitPerMinute,
      rateLimitPerDay: apiKey.rateLimitPerDay,
      allowedModelIds: apiKey.allowedModelIds ?? [],
      expireTime: apiKey.expireTime
        ? apiKey.expireTime.slice(0, 16) // format for datetime-local input
        : '',
    })
    setDialogOpen(true)
  }

  const handleSave = async () => {
    if (!form.keyName.trim()) {
      toast.error('密钥名称为必填项')
      return
    }

    setSaving(true)
    try {
      if (editingKey) {
        const payload: UpdateApiKeyRequest = {
          keyName: form.keyName,
          description: form.description || undefined,
          quotaTotal: form.quotaTotal,
          rateLimitPerMinute: form.rateLimitPerMinute,
          rateLimitPerDay: form.rateLimitPerDay,
          allowedModelIds: form.allowedModelIds,
          expireTime: form.expireTime || undefined,
        }
        await apiKeyApi.update(editingKey.id, payload)
        toast.success('API Key 已更新')
      } else {
        const payload: CreateApiKeyRequest = {
          keyName: form.keyName,
          description: form.description || undefined,
          quotaTotal: form.quotaTotal,
          rateLimitPerMinute: form.rateLimitPerMinute,
          rateLimitPerDay: form.rateLimitPerDay,
          allowedModelIds: form.allowedModelIds,
          expireTime: form.expireTime || undefined,
        }
        await apiKeyApi.create(payload)
        toast.success('API Key 已创建')
      }
      setDialogOpen(false)
      await fetchKeys()
    } catch {
      toast.error(editingKey ? '更新 API Key 失败' : '创建 API Key 失败')
    } finally {
      setSaving(false)
    }
  }

  const handleToggleStatus = async (apiKey: ApiKey) => {
    try {
      const newStatus = apiKey.status === 1 ? 0 : 1
      await apiKeyApi.updateStatus(apiKey.id, newStatus)
      toast.success(newStatus === 1 ? 'API Key 已启用' : 'API Key 已禁用')
      await fetchKeys()
    } catch {
      toast.error('更新状态失败')
    }
  }

  const handleDelete = async () => {
    if (!deleteTarget) return
    setDeleting(true)
    try {
      await apiKeyApi.delete(deleteTarget.id)
      toast.success('API Key 已删除')
      setDeleteTarget(null)
      await fetchKeys()
    } catch {
      toast.error('删除 API Key 失败')
    } finally {
      setDeleting(false)
    }
  }

  const handleResetQuota = async () => {
    if (!resetTarget) return
    setResetting(true)
    try {
      await apiKeyApi.resetQuota(resetTarget.id)
      toast.success('配额已重置')
      setResetTarget(null)
      await fetchKeys()
    } catch {
      toast.error('重置配额失败')
    } finally {
      setResetting(false)
    }
  }

  // ---- form updater ----
  const updateForm = <K extends keyof FormData>(key: K, value: FormData[K]) =>
    setForm((prev) => ({ ...prev, [key]: value }))

  // ---- summary stats ----
  const totalKeys = apiKeys.length
  const activeKeys = apiKeys.filter((k) => k.status === 1).length
  const totalQuotaUsed = apiKeys.reduce((sum, k) => sum + k.quotaUsed, 0)

  // ====================================================================
  // Render
  // ====================================================================

  return (
    <div className="space-y-6">
      {/* ---- Page header ---- */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold tracking-tight">API Key 管理</h1>
          <p className="text-sm text-muted-foreground mt-1">
            创建和管理 API Key，配置配额和速率限制。
          </p>
        </div>
        <Button onClick={openCreateDialog} className="gap-2 shrink-0">
          <Plus className="w-4 h-4" />
          创建密钥
        </Button>
      </div>

      {/* ---- Summary stats ---- */}
      {!loading && apiKeys.length > 0 && (
        <motion.div
          initial={{ opacity: 0, y: 12 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.4 }}
          className="grid grid-cols-1 sm:grid-cols-3 gap-4"
        >
          {[
            {
              label: '密钥总数',
              value: totalKeys.toLocaleString(),
              icon: Key,
              color: 'text-blue-400',
              bg: 'bg-blue-500/15',
            },
            {
              label: '活跃密钥',
              value: activeKeys.toLocaleString(),
              icon: Power,
              color: 'text-emerald-400',
              bg: 'bg-emerald-500/15',
            },
            {
              label: '已用配额',
              value: totalQuotaUsed.toLocaleString(),
              icon: Gauge,
              color: 'text-amber-400',
              bg: 'bg-amber-500/15',
            },
          ].map((stat) => {
            const Icon = stat.icon
            return (
              <Card key={stat.label} className="glass border-0">
                <CardContent className="p-4 flex items-center gap-3">
                  <div
                    className={`shrink-0 w-9 h-9 rounded-lg ${stat.bg} flex items-center justify-center`}
                  >
                    <Icon className={`w-4 h-4 ${stat.color}`} />
                  </div>
                  <div>
                    <p className="text-[10px] font-medium tracking-wide text-zinc-400 uppercase">
                      {stat.label}
                    </p>
                    <p className="text-lg font-bold tracking-tight text-white">
                      {stat.value}
                    </p>
                  </div>
                </CardContent>
              </Card>
            )
          })}
        </motion.div>
      )}

      {/* ---- Search bar ---- */}
      <div className="flex items-center gap-3">
        <div className="relative flex-1 min-w-0">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-muted-foreground pointer-events-none" />
          <Input
            placeholder="按名称、密钥或描述搜索..."
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            className="pl-9 bg-white/[0.03] border-white/[0.06] focus:border-white/[0.12] h-10"
          />
        </div>
      </div>

      {/* ---- Content area ---- */}
      {loading ? (
        <div className="flex flex-col items-center justify-center py-32 gap-4">
          <Loader2 className="w-8 h-8 text-primary animate-spin" />
          <p className="text-sm text-muted-foreground">加载 API Key 中...</p>
        </div>
      ) : filtered.length === 0 ? (
        <motion.div
          initial={{ opacity: 0, y: 12 }}
          animate={{ opacity: 1, y: 0 }}
          className="flex flex-col items-center justify-center py-32 gap-4"
        >
          <div className="w-16 h-16 rounded-2xl bg-white/[0.04] border border-white/[0.06] flex items-center justify-center">
            <Key className="w-8 h-8 text-muted-foreground" />
          </div>
          <div className="text-center">
            <p className="text-sm font-medium text-foreground">
              {apiKeys.length === 0 ? '暂无 API Key' : '没有匹配的密钥'}
            </p>
            <p className="text-xs text-muted-foreground mt-1">
              {apiKeys.length === 0
                ? '创建您的第一个 API Key 以开始使用。'
                : '请尝试调整搜索条件。'}
            </p>
          </div>
          {apiKeys.length === 0 && (
            <Button onClick={openCreateDialog} className="gap-2 mt-2">
              <Plus className="w-4 h-4" />
              创建密钥
            </Button>
          )}
        </motion.div>
      ) : (
        <>
          {/* Result count */}
          <div className="flex items-center gap-2 text-xs text-muted-foreground">
            <span className="tabular-nums">{filtered.length}</span>
            <span>
              个密钥
              {filtered.length !== apiKeys.length &&
                `（共 ${apiKeys.length} 个）`}
            </span>
          </div>

          {/* Grid */}
          <motion.div
            className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-4"
            variants={containerVariants}
            initial="hidden"
            animate="show"
          >
            {filtered.map((apiKey) => (
              <motion.div key={apiKey.id} variants={itemVariants}>
                <ApiKeyCard
                  apiKey={apiKey}
                  onEdit={openEditDialog}
                  onToggle={handleToggleStatus}
                  onDelete={setDeleteTarget}
                  onResetQuota={setResetTarget}
                />
              </motion.div>
            ))}
          </motion.div>
        </>
      )}

      {/* ================================================================= */}
      {/* Create / Edit Dialog                                               */}
      {/* ================================================================= */}
      <Dialog open={dialogOpen} onOpenChange={setDialogOpen}>
        <DialogContent className="sm:max-w-[520px] bg-[hsl(240_10%_5%)] border-white/[0.08] max-h-[90vh] overflow-y-auto">
          <DialogHeader>
            <DialogTitle>
              {editingKey ? '编辑 API Key' : '创建 API Key'}
            </DialogTitle>
            <DialogDescription>
              {editingKey
                ? '更新下方的 API Key 配置。'
                : '创建新的 API Key 并配置配额和速率限制。'}
            </DialogDescription>
          </DialogHeader>

          <div className="space-y-5 py-2">
            {/* ---- Key name ---- */}
            <div className="space-y-2">
              <Label htmlFor="key-name">
                密钥名称 <span className="text-red-400">*</span>
              </Label>
              <Input
                id="key-name"
                placeholder="例如：生产环境 API Key"
                value={form.keyName}
                onChange={(e) => updateForm('keyName', e.target.value)}
                className="bg-white/[0.03] border-white/[0.06]"
              />
            </div>

            {/* ---- Description ---- */}
            <div className="space-y-2">
              <Label htmlFor="key-desc">描述</Label>
              <Textarea
                id="key-desc"
                placeholder="API Key 描述（可选）..."
                rows={3}
                value={form.description}
                onChange={(e) => updateForm('description', e.target.value)}
                className="bg-white/[0.03] border-white/[0.06] resize-none"
              />
            </div>

            {/* ---- Quota ---- */}
            <div className="space-y-3 rounded-lg border border-white/[0.06] bg-white/[0.02] p-4">
              <div className="flex items-center gap-2 text-sm font-medium">
                <Gauge className="w-4 h-4 text-muted-foreground" />
                配额与限制
              </div>

              <div className="space-y-2">
                <Label htmlFor="quota-total" className="text-xs text-muted-foreground">
                  总配额（-1 为无限制）
                </Label>
                <Input
                  id="quota-total"
                  type="number"
                  placeholder="-1"
                  value={form.quotaTotal}
                  onChange={(e) =>
                    updateForm('quotaTotal', e.target.value ? Number(e.target.value) : -1)
                  }
                  className="bg-white/[0.03] border-white/[0.06] text-sm"
                />
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div className="space-y-2">
                  <Label
                    htmlFor="rate-per-min"
                    className="text-xs text-muted-foreground flex items-center gap-1.5"
                  >
                    <Gauge className="w-3 h-3" />
                    每分钟限制
                  </Label>
                  <Input
                    id="rate-per-min"
                    type="number"
                    placeholder="60"
                    value={form.rateLimitPerMinute ?? ''}
                    onChange={(e) =>
                      updateForm(
                        'rateLimitPerMinute',
                        e.target.value ? Number(e.target.value) : undefined,
                      )
                    }
                    className="bg-white/[0.03] border-white/[0.06] text-sm"
                  />
                </div>
                <div className="space-y-2">
                  <Label
                    htmlFor="rate-per-day"
                    className="text-xs text-muted-foreground flex items-center gap-1.5"
                  >
                    <Gauge className="w-3 h-3" />
                    每日限制
                  </Label>
                  <Input
                    id="rate-per-day"
                    type="number"
                    placeholder="10000"
                    value={form.rateLimitPerDay ?? ''}
                    onChange={(e) =>
                      updateForm(
                        'rateLimitPerDay',
                        e.target.value ? Number(e.target.value) : undefined,
                      )
                    }
                    className="bg-white/[0.03] border-white/[0.06] text-sm"
                  />
                </div>
              </div>
            </div>

            {/* ---- Model Access ---- */}
            <div className="space-y-2">
              <Label className="flex items-center gap-1.5">
                <Shield className="w-3.5 h-3.5 text-muted-foreground" />
                模型访问权限
              </Label>
              <ModelMultiSelect
                value={form.allowedModelIds}
                onChange={(ids) => updateForm('allowedModelIds', ids)}
              />
            </div>

            {/* ---- Expire time ---- */}
            <div className="space-y-2">
              <Label htmlFor="expire-time" className="flex items-center gap-1.5">
                <Clock className="w-3.5 h-3.5 text-muted-foreground" />
                过期时间
              </Label>
              <Input
                id="expire-time"
                type="datetime-local"
                value={form.expireTime}
                onChange={(e) => updateForm('expireTime', e.target.value)}
                className="bg-white/[0.03] border-white/[0.06]"
              />
              <p className="text-[10px] text-zinc-600">
                留空则永不过期。
              </p>
            </div>
          </div>

          <DialogFooter>
            <Button
              variant="ghost"
              onClick={() => setDialogOpen(false)}
              disabled={saving}
            >
              取消
            </Button>
            <Button onClick={handleSave} disabled={saving} className="gap-2">
              {saving && <Loader2 className="w-4 h-4 animate-spin" />}
              {editingKey ? '更新' : '创建'}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* ================================================================= */}
      {/* Delete confirmation                                                */}
      {/* ================================================================= */}
      <AlertDialog
        open={!!deleteTarget}
        onOpenChange={(open) => !open && setDeleteTarget(null)}
      >
        <AlertDialogContent className="bg-[hsl(240_10%_5%)] border-white/[0.08]">
          <AlertDialogHeader>
            <AlertDialogTitle>删除 API Key</AlertDialogTitle>
            <AlertDialogDescription>
              确定要删除{' '}
              <span className="font-semibold text-foreground">
                {deleteTarget?.keyName}
              </span>
              ？此操作无法撤销。使用此密钥的所有服务将立即失去访问权限。
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel disabled={deleting}>取消</AlertDialogCancel>
            <AlertDialogAction
              onClick={handleDelete}
              disabled={deleting}
              className="bg-red-600 hover:bg-red-700 text-white gap-2"
            >
              {deleting && <Loader2 className="w-4 h-4 animate-spin" />}
              删除
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>

      {/* ================================================================= */}
      {/* Reset quota confirmation                                           */}
      {/* ================================================================= */}
      <AlertDialog
        open={!!resetTarget}
        onOpenChange={(open) => !open && setResetTarget(null)}
      >
        <AlertDialogContent className="bg-[hsl(240_10%_5%)] border-white/[0.08]">
          <AlertDialogHeader>
            <AlertDialogTitle>重置配额</AlertDialogTitle>
            <AlertDialogDescription>
              确定要重置{' '}
              <span className="font-semibold text-foreground">
                {resetTarget?.keyName}
              </span>
              {' '}的使用配额？已用配额将归零。
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel disabled={resetting}>取消</AlertDialogCancel>
            <AlertDialogAction
              onClick={handleResetQuota}
              disabled={resetting}
              className="bg-amber-600 hover:bg-amber-700 text-white gap-2"
            >
              {resetting && <Loader2 className="w-4 h-4 animate-spin" />}
              重置
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </div>
  )
}
