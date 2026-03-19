import { useState, useEffect } from 'react'
import { motion, AnimatePresence } from 'framer-motion'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Badge } from '@/components/ui/badge'
import { Switch } from '@/components/ui/switch'
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogFooter,
  DialogDescription,
} from '@/components/ui/dialog'
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select'
import { Label } from '@/components/ui/label'
import { Textarea } from '@/components/ui/textarea'
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from '@/components/ui/alert-dialog'
import { providerApi } from '@/api/model'
import type {
  Provider,
  CreateProviderRequest,
  UpdateProviderRequest,
} from '@/types/model'
import { toast } from 'sonner'
import {
  Plus,
  Search,
  Server,
  Globe,
  Pencil,
  Trash2,
  Power,
  Loader2,
  Shield,
  Link,
} from 'lucide-react'

// ---------------------------------------------------------------------------
// Constants & helpers
// ---------------------------------------------------------------------------

const PROVIDER_TYPES = ['Remote', 'Local'] as const
const PROVIDER_TYPE_LABELS: Record<string, string> = { Remote: '远程', Local: '本地' }
const AUTH_TYPES = ['Bearer', 'API-Key', 'Custom'] as const

interface FormData {
  code: string
  name: string
  type: string
  description: string
  baseUrl: string
  protocolType: string
  chatEndpoint: string
  embeddingEndpoint: string
  imageEndpoint: string
  videoEndpoint: string
  authType: string
  authHeader: string
  authPrefix: string
  apiKeyRequired: boolean
  sortOrder: number
}

const EMPTY_FORM: FormData = {
  code: '',
  name: '',
  type: 'Remote',
  description: '',
  baseUrl: '',
  protocolType: 'OpenAI',
  chatEndpoint: '',
  embeddingEndpoint: '',
  imageEndpoint: '',
  videoEndpoint: '',
  authType: 'Bearer',
  authHeader: 'Authorization',
  authPrefix: 'Bearer',
  apiKeyRequired: true,
  sortOrder: 0,
}

/** Count non-empty endpoint fields on a provider. */
function countEndpoints(provider: Provider): number {
  let count = 0
  if (provider.chatEndpoint) count++
  if (provider.embeddingEndpoint) count++
  if (provider.imageEndpoint) count++
  if (provider.videoEndpoint) count++
  return count
}

// ---------------------------------------------------------------------------
// Provider card (horizontal row-style)
// ---------------------------------------------------------------------------

function ProviderCard({
  provider,
  index,
  onEdit,
  onToggle,
  onDelete,
}: {
  provider: Provider
  index: number
  onEdit: (p: Provider) => void
  onToggle: (p: Provider) => void
  onDelete: (p: Provider) => void
}) {
  const isActive = provider.status === 1
  const isRemote = provider.type === 'Remote'
  const endpoints = countEndpoints(provider)

  return (
    <motion.div
      layout
      initial={{ opacity: 0, y: 24, scale: 0.97 }}
      animate={{ opacity: 1, y: 0, scale: 1 }}
      exit={{ opacity: 0, y: -16, scale: 0.97 }}
      transition={{
        duration: 0.35,
        delay: index * 0.04,
        ease: [0.23, 1, 0.32, 1],
      }}
    >
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
            isRemote
              ? 'from-blue-500/20 to-cyan-500/10'
              : 'from-emerald-500/20 to-teal-500/10'
          } opacity-60 group-hover:opacity-100 transition-opacity`}
        />

        <CardContent className="p-5">
          <div className="flex items-center gap-4">
            {/* Icon */}
            <div
              className={`shrink-0 w-10 h-10 rounded-xl flex items-center justify-center ring-1 ring-inset ring-white/[0.08] ${
                isRemote ? 'bg-blue-500/15' : 'bg-emerald-500/15'
              }`}
            >
              {isRemote ? (
                <Globe className="w-5 h-5 text-blue-400" />
              ) : (
                <Server className="w-5 h-5 text-emerald-400" />
              )}
            </div>

            {/* Name + code */}
            <div className="min-w-0 flex-1">
              <div className="flex items-center gap-2">
                <h3 className="text-sm font-semibold text-foreground truncate leading-tight">
                  {provider.name}
                </h3>
                <span className="text-xs font-mono text-muted-foreground bg-white/[0.04] px-1.5 py-0.5 rounded shrink-0">
                  {provider.code}
                </span>
              </div>

              {/* Base URL */}
              {provider.baseUrl && (
                <p className="text-xs text-muted-foreground truncate mt-1 flex items-center gap-1">
                  <Link className="w-3 h-3 shrink-0" />
                  {provider.baseUrl}
                </p>
              )}
            </div>

            {/* Badges cluster */}
            <div className="hidden sm:flex items-center gap-1.5 shrink-0">
              {/* Type badge */}
              <Badge
                className={`text-[10px] px-2 py-0 h-5 border font-medium ${
                  isRemote
                    ? 'text-blue-400 bg-blue-500/15 border-blue-500/30'
                    : 'text-emerald-400 bg-emerald-500/15 border-emerald-500/30'
                }`}
              >
                {PROVIDER_TYPE_LABELS[provider.type] ?? provider.type}
              </Badge>

              {/* Auth type badge */}
              {provider.authType && (
                <Badge
                  variant="outline"
                  className="text-[10px] px-2 py-0 h-5 text-muted-foreground border-white/[0.08] gap-1"
                >
                  <Shield className="w-3 h-3" />
                  {provider.authType}
                </Badge>
              )}

              {/* Endpoints count */}
              {endpoints > 0 && (
                <Badge
                  variant="outline"
                  className="text-[10px] px-2 py-0 h-5 text-muted-foreground border-white/[0.08] tabular-nums"
                >
                  {endpoints} 个端点
                </Badge>
              )}
            </div>

            {/* Status dot */}
            <div className="flex items-center gap-1.5 shrink-0">
              <div className={isActive ? 'dot-online' : 'dot-offline'} />
              <span
                className={`text-[10px] font-medium hidden lg:inline ${
                  isActive ? 'text-emerald-400' : 'text-zinc-500'
                }`}
              >
                {isActive ? '已启用' : '已禁用'}
              </span>
            </div>

            {/* Actions */}
            <div className="flex items-center gap-1 shrink-0">
              <Button
                variant="ghost"
                size="sm"
                className="h-8 px-2.5 text-xs text-muted-foreground hover:text-foreground hover:bg-white/[0.06]"
                onClick={() => onEdit(provider)}
              >
                <Pencil className="w-3.5 h-3.5 mr-1.5" />
                <span className="hidden md:inline">编辑</span>
              </Button>
              <Button
                variant="ghost"
                size="sm"
                className={`h-8 px-2.5 text-xs hover:bg-white/[0.06] ${
                  isActive
                    ? 'text-emerald-400 hover:text-emerald-300'
                    : 'text-muted-foreground hover:text-foreground'
                }`}
                onClick={() => onToggle(provider)}
              >
                <Power className="w-3.5 h-3.5 mr-1.5" />
                <span className="hidden md:inline">
                  {isActive ? '禁用' : '启用'}
                </span>
              </Button>
              <Button
                variant="ghost"
                size="sm"
                className="h-8 px-2.5 text-xs text-muted-foreground hover:text-red-400 hover:bg-red-500/10"
                onClick={() => onDelete(provider)}
              >
                <Trash2 className="w-3.5 h-3.5" />
              </Button>
            </div>
          </div>
        </CardContent>
      </Card>
    </motion.div>
  )
}

// ---------------------------------------------------------------------------
// Main page component
// ---------------------------------------------------------------------------

export default function ProvidersPage() {
  // ---- data state ----
  const [providers, setProviders] = useState<Provider[]>([])
  const [loading, setLoading] = useState(true)

  // ---- filter state ----
  const [search, setSearch] = useState('')

  // ---- dialog state ----
  const [dialogOpen, setDialogOpen] = useState(false)
  const [editingProvider, setEditingProvider] = useState<Provider | null>(null)
  const [form, setForm] = useState<FormData>({ ...EMPTY_FORM })
  const [saving, setSaving] = useState(false)

  // ---- delete confirm ----
  const [deleteTarget, setDeleteTarget] = useState<Provider | null>(null)
  const [deleting, setDeleting] = useState(false)

  // ---- data fetching ----
  const fetchProviders = async () => {
    try {
      const data = await providerApi.getList()
      setProviders(data)
    } catch {
      toast.error('加载提供商失败')
    }
  }

  useEffect(() => {
    const init = async () => {
      setLoading(true)
      await fetchProviders()
      setLoading(false)
    }
    init()
  }, [])

  // ---- filtering ----
  const filtered = providers.filter((p) => {
    if (!search) return true
    const q = search.toLowerCase()
    return (
      p.name.toLowerCase().includes(q) ||
      p.code.toLowerCase().includes(q) ||
      (p.baseUrl && p.baseUrl.toLowerCase().includes(q))
    )
  })

  // ---- CRUD helpers ----
  const openCreateDialog = () => {
    setEditingProvider(null)
    setForm({ ...EMPTY_FORM })
    setDialogOpen(true)
  }

  const openEditDialog = (provider: Provider) => {
    setEditingProvider(provider)
    setForm({
      code: provider.code,
      name: provider.name,
      type: provider.type,
      description: provider.description ?? '',
      baseUrl: provider.baseUrl ?? '',
      protocolType: provider.protocolType ?? 'OpenAI',
      chatEndpoint: provider.chatEndpoint ?? '',
      embeddingEndpoint: provider.embeddingEndpoint ?? '',
      imageEndpoint: provider.imageEndpoint ?? '',
      videoEndpoint: provider.videoEndpoint ?? '',
      authType: provider.authType ?? 'Bearer',
      authHeader: provider.authHeader ?? 'Authorization',
      authPrefix: provider.authPrefix ?? 'Bearer',
      apiKeyRequired: provider.apiKeyRequired ?? true,
      sortOrder: provider.sortOrder ?? 0,
    })
    setDialogOpen(true)
  }

  const handleSave = async () => {
    if (!form.code.trim() || !form.name.trim()) {
      toast.error('提供商代码和名称为必填项')
      return
    }

    setSaving(true)
    try {
      if (editingProvider) {
        const payload: UpdateProviderRequest = {
          name: form.name,
          baseUrl: form.baseUrl || undefined,
          protocolType: form.protocolType || undefined,
          chatEndpoint: form.chatEndpoint || undefined,
          embeddingEndpoint: form.embeddingEndpoint || undefined,
          imageEndpoint: form.imageEndpoint || undefined,
          videoEndpoint: form.videoEndpoint || undefined,
          authType: form.authType || undefined,
          authHeader: form.authHeader || undefined,
          authPrefix: form.authPrefix || undefined,
          apiKeyRequired: form.apiKeyRequired,
          description: form.description || undefined,
          sortOrder: form.sortOrder,
        }
        await providerApi.update(editingProvider.id, payload)
        toast.success('提供商已更新')
      } else {
        const payload: CreateProviderRequest = {
          code: form.code,
          name: form.name,
          type: form.type,
          baseUrl: form.baseUrl || undefined,
          protocolType: form.protocolType || undefined,
          chatEndpoint: form.chatEndpoint || undefined,
          embeddingEndpoint: form.embeddingEndpoint || undefined,
          imageEndpoint: form.imageEndpoint || undefined,
          videoEndpoint: form.videoEndpoint || undefined,
          authType: form.authType || undefined,
          authHeader: form.authHeader || undefined,
          authPrefix: form.authPrefix || undefined,
          apiKeyRequired: form.apiKeyRequired,
          description: form.description || undefined,
          sortOrder: form.sortOrder,
        }
        await providerApi.create(payload)
        toast.success('提供商已创建')
      }
      setDialogOpen(false)
      await fetchProviders()
    } catch {
      toast.error(
        editingProvider
          ? '更新提供商失败'
          : '创建提供商失败',
      )
    } finally {
      setSaving(false)
    }
  }

  const handleToggleStatus = async (provider: Provider) => {
    try {
      await providerApi.toggleStatus(provider.id)
      toast.success(
        provider.status === 1 ? '提供商已禁用' : '提供商已启用',
      )
      await fetchProviders()
    } catch {
      toast.error('更新提供商失败')
    }
  }

  const handleDelete = async () => {
    if (!deleteTarget) return
    setDeleting(true)
    try {
      await providerApi.delete(deleteTarget.id)
      toast.success('提供商已删除')
      setDeleteTarget(null)
      await fetchProviders()
    } catch {
      toast.error('删除提供商失败')
    } finally {
      setDeleting(false)
    }
  }

  // ---- form updater ----
  const updateForm = <K extends keyof FormData>(key: K, value: FormData[K]) =>
    setForm((prev) => ({ ...prev, [key]: value }))

  // ====================================================================
  // Render
  // ====================================================================

  return (
    <div className="space-y-6">
      {/* ---- Page header ---- */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold tracking-tight">提供商管理</h1>
          <p className="text-sm text-muted-foreground mt-1">
            管理 AI 模型提供商，配置端点和认证。
          </p>
        </div>
        <Button onClick={openCreateDialog} className="gap-2 shrink-0">
          <Plus className="w-4 h-4" />
          添加提供商
        </Button>
      </div>

      {/* ---- Search bar ---- */}
      <div className="flex items-center gap-3">
        <div className="relative flex-1 min-w-0">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-muted-foreground pointer-events-none" />
          <Input
            placeholder="按名称、代码或 URL 搜索..."
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
          <p className="text-sm text-muted-foreground">
            加载提供商中...
          </p>
        </div>
      ) : filtered.length === 0 ? (
        <motion.div
          initial={{ opacity: 0, y: 12 }}
          animate={{ opacity: 1, y: 0 }}
          className="flex flex-col items-center justify-center py-32 gap-4"
        >
          <div className="w-16 h-16 rounded-2xl bg-white/[0.04] border border-white/[0.06] flex items-center justify-center">
            <Server className="w-8 h-8 text-muted-foreground" />
          </div>
          <div className="text-center">
            <p className="text-sm font-medium text-foreground">
              {providers.length === 0
                ? '暂无提供商'
                : '没有匹配的提供商'}
            </p>
            <p className="text-xs text-muted-foreground mt-1">
              {providers.length === 0
                ? '添加您的第一个 AI 提供商以开始使用。'
                : '请尝试调整搜索条件。'}
            </p>
          </div>
          {providers.length === 0 && (
            <Button onClick={openCreateDialog} className="gap-2 mt-2">
              <Plus className="w-4 h-4" />
              添加提供商
            </Button>
          )}
        </motion.div>
      ) : (
        <>
          {/* Result count */}
          <div className="flex items-center gap-2 text-xs text-muted-foreground">
            <span className="tabular-nums">{filtered.length}</span>
            <span>
              个提供商
              {filtered.length !== providers.length &&
                `（共 ${providers.length} 个）`}
            </span>
          </div>

          {/* Provider list (vertical stack of horizontal cards) */}
          <motion.div layout className="flex flex-col gap-3">
            <AnimatePresence mode="popLayout">
              {filtered.map((provider, i) => (
                <ProviderCard
                  key={provider.id}
                  provider={provider}
                  index={i}
                  onEdit={openEditDialog}
                  onToggle={handleToggleStatus}
                  onDelete={setDeleteTarget}
                />
              ))}
            </AnimatePresence>
          </motion.div>
        </>
      )}

      {/* ================================================================= */}
      {/* Create / Edit Dialog                                               */}
      {/* ================================================================= */}
      <Dialog open={dialogOpen} onOpenChange={setDialogOpen}>
        <DialogContent className="sm:max-w-[640px] bg-[hsl(240_10%_5%)] border-white/[0.08] max-h-[90vh] overflow-y-auto">
          <DialogHeader>
            <DialogTitle>
              {editingProvider ? '编辑提供商' : '创建提供商'}
            </DialogTitle>
            <DialogDescription>
              {editingProvider
                ? '更新下方的提供商配置。'
                : '向平台添加新的 AI 模型提供商。'}
            </DialogDescription>
          </DialogHeader>

          <div className="space-y-5 py-2">
            {/* ---- Section: Basic Info ---- */}
            <div className="space-y-3 rounded-lg border border-white/[0.06] bg-white/[0.02] p-4">
              <div className="flex items-center gap-2 text-sm font-medium">
                <Server className="w-4 h-4 text-muted-foreground" />
                基本信息
              </div>

              <div className="grid grid-cols-2 gap-4">
                <div className="space-y-2">
                  <Label htmlFor="provider-code">
                    代码 <span className="text-red-400">*</span>
                  </Label>
                  <Input
                    id="provider-code"
                    placeholder="openai"
                    value={form.code}
                    onChange={(e) => updateForm('code', e.target.value)}
                    disabled={!!editingProvider}
                    className="font-mono text-sm bg-white/[0.03] border-white/[0.06]"
                  />
                </div>
                <div className="space-y-2">
                  <Label htmlFor="provider-name">
                    名称 <span className="text-red-400">*</span>
                  </Label>
                  <Input
                    id="provider-name"
                    placeholder="OpenAI"
                    value={form.name}
                    onChange={(e) => updateForm('name', e.target.value)}
                    className="bg-white/[0.03] border-white/[0.06]"
                  />
                </div>
              </div>

              <div className="grid grid-cols-2 gap-4">
                <div className="space-y-2">
                  <Label>类型</Label>
                  <Select
                    value={form.type}
                    onValueChange={(v) => updateForm('type', v)}
                  >
                    <SelectTrigger className="bg-white/[0.03] border-white/[0.06]">
                      <SelectValue />
                    </SelectTrigger>
                    <SelectContent>
                      {PROVIDER_TYPES.map((t) => (
                        <SelectItem key={t} value={t}>
                          <span className="flex items-center gap-2">
                            {t === 'Remote' ? (
                              <Globe className="w-3.5 h-3.5 text-blue-400" />
                            ) : (
                              <Server className="w-3.5 h-3.5 text-emerald-400" />
                            )}
                            {PROVIDER_TYPE_LABELS[t] ?? t}
                          </span>
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                </div>
                <div className="space-y-2">
                  <Label htmlFor="provider-sort">排序</Label>
                  <Input
                    id="provider-sort"
                    type="number"
                    placeholder="0"
                    value={form.sortOrder}
                    onChange={(e) =>
                      updateForm('sortOrder', Number(e.target.value) || 0)
                    }
                    className="bg-white/[0.03] border-white/[0.06]"
                  />
                </div>
              </div>

              <div className="space-y-2">
                <Label htmlFor="provider-desc">描述</Label>
                <Textarea
                  id="provider-desc"
                  placeholder="提供商描述（可选）..."
                  rows={2}
                  value={form.description}
                  onChange={(e) => updateForm('description', e.target.value)}
                  className="bg-white/[0.03] border-white/[0.06] resize-none"
                />
              </div>
            </div>

            {/* ---- Section: Connection ---- */}
            <div className="space-y-3 rounded-lg border border-white/[0.06] bg-white/[0.02] p-4">
              <div className="flex items-center gap-2 text-sm font-medium">
                <Link className="w-4 h-4 text-muted-foreground" />
                连接配置
              </div>

              <div className="grid grid-cols-2 gap-4">
                <div className="space-y-2 col-span-2">
                  <Label htmlFor="provider-baseurl">基础 URL</Label>
                  <Input
                    id="provider-baseurl"
                    placeholder="https://api.openai.com"
                    value={form.baseUrl}
                    onChange={(e) => updateForm('baseUrl', e.target.value)}
                    className="font-mono text-sm bg-white/[0.03] border-white/[0.06]"
                  />
                </div>
              </div>

              <div className="space-y-2">
                <Label htmlFor="provider-protocol">协议类型</Label>
                <Input
                  id="provider-protocol"
                  placeholder="OpenAI"
                  value={form.protocolType}
                  onChange={(e) => updateForm('protocolType', e.target.value)}
                  className="bg-white/[0.03] border-white/[0.06]"
                />
              </div>

              <div className="grid grid-cols-2 gap-4">
                <div className="space-y-2">
                  <Label htmlFor="provider-chat-ep">对话端点</Label>
                  <Input
                    id="provider-chat-ep"
                    placeholder="/v1/chat/completions"
                    value={form.chatEndpoint}
                    onChange={(e) =>
                      updateForm('chatEndpoint', e.target.value)
                    }
                    className="font-mono text-xs bg-white/[0.03] border-white/[0.06]"
                  />
                </div>
                <div className="space-y-2">
                  <Label htmlFor="provider-embed-ep">嵌入端点</Label>
                  <Input
                    id="provider-embed-ep"
                    placeholder="/v1/embeddings"
                    value={form.embeddingEndpoint}
                    onChange={(e) =>
                      updateForm('embeddingEndpoint', e.target.value)
                    }
                    className="font-mono text-xs bg-white/[0.03] border-white/[0.06]"
                  />
                </div>
              </div>

              <div className="grid grid-cols-2 gap-4">
                <div className="space-y-2">
                  <Label htmlFor="provider-img-ep">图像端点</Label>
                  <Input
                    id="provider-img-ep"
                    placeholder="/v1/images/generations"
                    value={form.imageEndpoint}
                    onChange={(e) =>
                      updateForm('imageEndpoint', e.target.value)
                    }
                    className="font-mono text-xs bg-white/[0.03] border-white/[0.06]"
                  />
                </div>
                <div className="space-y-2">
                  <Label htmlFor="provider-video-ep">视频端点</Label>
                  <Input
                    id="provider-video-ep"
                    placeholder="/v1/video/generations"
                    value={form.videoEndpoint}
                    onChange={(e) =>
                      updateForm('videoEndpoint', e.target.value)
                    }
                    className="font-mono text-xs bg-white/[0.03] border-white/[0.06]"
                  />
                </div>
              </div>
            </div>

            {/* ---- Section: Authentication ---- */}
            <div className="space-y-3 rounded-lg border border-white/[0.06] bg-white/[0.02] p-4">
              <div className="flex items-center gap-2 text-sm font-medium">
                <Shield className="w-4 h-4 text-muted-foreground" />
                认证配置
              </div>

              <div className="grid grid-cols-3 gap-4">
                <div className="space-y-2">
                  <Label>认证类型</Label>
                  <Select
                    value={form.authType}
                    onValueChange={(v) => updateForm('authType', v)}
                  >
                    <SelectTrigger className="bg-white/[0.03] border-white/[0.06]">
                      <SelectValue />
                    </SelectTrigger>
                    <SelectContent>
                      {AUTH_TYPES.map((t) => (
                        <SelectItem key={t} value={t}>
                          {t}
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                </div>
                <div className="space-y-2">
                  <Label htmlFor="provider-auth-header">认证头</Label>
                  <Input
                    id="provider-auth-header"
                    placeholder="Authorization"
                    value={form.authHeader}
                    onChange={(e) => updateForm('authHeader', e.target.value)}
                    className="font-mono text-sm bg-white/[0.03] border-white/[0.06]"
                  />
                </div>
                <div className="space-y-2">
                  <Label htmlFor="provider-auth-prefix">认证前缀</Label>
                  <Input
                    id="provider-auth-prefix"
                    placeholder="Bearer"
                    value={form.authPrefix}
                    onChange={(e) => updateForm('authPrefix', e.target.value)}
                    className="font-mono text-sm bg-white/[0.03] border-white/[0.06]"
                  />
                </div>
              </div>

              <div className="flex items-center justify-between rounded-lg border border-white/[0.06] bg-white/[0.02] px-4 py-3">
                <div className="space-y-0.5">
                  <Label className="text-sm">需要 API Key</Label>
                  <p className="text-[11px] text-muted-foreground">
                    认证需要 API Key
                  </p>
                </div>
                <Switch
                  checked={form.apiKeyRequired}
                  onCheckedChange={(v) => updateForm('apiKeyRequired', v)}
                />
              </div>
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
              {editingProvider ? '更新' : '创建'}
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
            <AlertDialogTitle>删除提供商</AlertDialogTitle>
            <AlertDialogDescription>
              确定要删除{' '}
              <span className="font-semibold text-foreground">
                {deleteTarget?.name}
              </span>
              ？与此提供商关联的所有模型可能会受到影响。此操作不可撤销。
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
    </div>
  )
}
