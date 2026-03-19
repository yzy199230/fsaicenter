import { useState, useEffect } from 'react'
import { motion, AnimatePresence } from 'framer-motion'
import { Card, CardContent } from '@/components/ui/card'
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
import { Tooltip, TooltipContent, TooltipProvider, TooltipTrigger } from '@/components/ui/tooltip'
import { Progress } from '@/components/ui/progress'
import { modelApi, providerApi, modelApiKeyApi } from '@/api/model'
import type {
  AiModel,
  CreateModelRequest,
  UpdateModelRequest,
  Provider,
  ModelConfig,
  ModelApiKey,
  CreateModelApiKeyRequest,
  UpdateModelApiKeyRequest,
} from '@/types/model'
import { toast } from 'sonner'
import {
  Plus,
  Search,
  Bot,
  MessageSquare,
  Image,
  Mic,
  Video,
  Eye,
  FileText,
  Zap,
  Settings2,
  Trash2,
  Pencil,
  Power,
  Thermometer,
  Hash,
  Layers,
  Loader2,
  Filter,
  Key,
  Shield,
  ShieldAlert,
  ShieldOff,
  Copy,
  Check,
  RotateCcw,
  Activity,
  Clock,
  AlertTriangle,
} from 'lucide-react'

// ---------------------------------------------------------------------------
// Constants & helpers
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

type ModelType = (typeof MODEL_TYPES)[number]

interface TypeMeta {
  icon: React.ElementType
  color: string      // tailwind text-* color
  bg: string         // tailwind bg-* for the icon circle
  border: string     // border color for badges
  gradient: string   // gradient for the badge bg
}

const TYPE_META: Record<ModelType, TypeMeta> = {
  Chat: {
    icon: MessageSquare,
    color: 'text-blue-400',
    bg: 'bg-blue-500/15',
    border: 'border-blue-500/30',
    gradient: 'from-blue-500/20 to-blue-600/10',
  },
  Embedding: {
    icon: FileText,
    color: 'text-purple-400',
    bg: 'bg-purple-500/15',
    border: 'border-purple-500/30',
    gradient: 'from-purple-500/20 to-purple-600/10',
  },
  Image: {
    icon: Image,
    color: 'text-pink-400',
    bg: 'bg-pink-500/15',
    border: 'border-pink-500/30',
    gradient: 'from-pink-500/20 to-pink-600/10',
  },
  ASR: {
    icon: Mic,
    color: 'text-emerald-400',
    bg: 'bg-emerald-500/15',
    border: 'border-emerald-500/30',
    gradient: 'from-emerald-500/20 to-emerald-600/10',
  },
  TTS: {
    icon: Mic,
    color: 'text-amber-400',
    bg: 'bg-amber-500/15',
    border: 'border-amber-500/30',
    gradient: 'from-amber-500/20 to-amber-600/10',
  },
  Video: {
    icon: Video,
    color: 'text-red-400',
    bg: 'bg-red-500/15',
    border: 'border-red-500/30',
    gradient: 'from-red-500/20 to-red-600/10',
  },
  ImageRecognition: {
    icon: Eye,
    color: 'text-cyan-400',
    bg: 'bg-cyan-500/15',
    border: 'border-cyan-500/30',
    gradient: 'from-cyan-500/20 to-cyan-600/10',
  },
}

function getTypeMeta(type: string): TypeMeta {
  return (
    TYPE_META[type as ModelType] ?? {
      icon: Bot,
      color: 'text-zinc-400',
      bg: 'bg-zinc-500/15',
      border: 'border-zinc-500/30',
      gradient: 'from-zinc-500/20 to-zinc-600/10',
    }
  )
}

const EMPTY_FORM: FormData = {
  code: '',
  name: '',
  type: 'Chat',
  providerId: 0,
  supportStream: true,
  maxTokenLimit: undefined,
  description: '',
  config: { temperature: 0.7, maxTokens: 4096, topP: 1 },
}

interface FormData {
  code: string
  name: string
  type: string
  providerId: number
  supportStream: boolean
  maxTokenLimit?: number
  description: string
  config: ModelConfig
}

// ---------------------------------------------------------------------------
// Gradient bar component used for temperature & topP visualisation
// ---------------------------------------------------------------------------

function GradientBar({
  value,
  max,
  fromColor,
  toColor,
  label,
}: {
  value: number
  max: number
  fromColor: string
  toColor: string
  label: string
}) {
  const pct = Math.min(100, Math.max(0, (value / max) * 100))
  return (
    <div className="flex items-center gap-2 min-w-0">
      <span className="text-[10px] text-muted-foreground w-16 shrink-0 truncate">
        {label}
      </span>
      <div className="flex-1 h-1.5 rounded-full bg-white/[0.06] overflow-hidden">
        <motion.div
          className={`h-full rounded-full bg-gradient-to-r ${fromColor} ${toColor}`}
          initial={{ width: 0 }}
          animate={{ width: `${pct}%` }}
          transition={{ duration: 0.6, ease: 'easeOut' }}
        />
      </div>
      <span className="text-[10px] text-muted-foreground tabular-nums w-8 text-right shrink-0">
        {value}
      </span>
    </div>
  )
}

// ---------------------------------------------------------------------------
// Model card
// ---------------------------------------------------------------------------

function ModelCard({
  model,
  index,
  onEdit,
  onToggle,
  onDelete,
  onManageKeys,
}: {
  model: AiModel
  index: number
  onEdit: (m: AiModel) => void
  onToggle: (m: AiModel) => void
  onDelete: (m: AiModel) => void
  onManageKeys: (m: AiModel) => void
}) {
  const meta = getTypeMeta(model.type)
  const Icon = meta.icon
  const isActive = model.status === 1

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
          className={`absolute inset-x-0 top-0 h-[1px] bg-gradient-to-r ${meta.gradient} opacity-60 group-hover:opacity-100 transition-opacity`}
        />

        <CardContent className="p-5 space-y-4">
          {/* ---- Header row ---- */}
          <div className="flex items-start justify-between gap-3">
            <div className="flex items-start gap-3 min-w-0">
              {/* Type icon circle */}
              <div
                className={`shrink-0 w-10 h-10 rounded-xl ${meta.bg} flex items-center justify-center ring-1 ring-inset ring-white/[0.08]`}
              >
                <Icon className={`w-5 h-5 ${meta.color}`} />
              </div>

              <div className="min-w-0">
                <h3 className="text-sm font-semibold text-foreground truncate leading-tight">
                  {model.name}
                </h3>
                <p className="text-xs font-mono text-muted-foreground truncate mt-0.5">
                  {model.code}
                </p>
              </div>
            </div>

            {/* Status dot */}
            <div className="flex items-center gap-1.5 shrink-0 pt-1">
              <div className={isActive ? 'dot-online' : 'dot-offline'} />
              <span
                className={`text-[10px] font-medium ${isActive ? 'text-emerald-400' : 'text-zinc-500'}`}
              >
                {isActive ? '运行中' : '已停用'}
              </span>
            </div>
          </div>

          {/* ---- Badges row ---- */}
          <div className="flex flex-wrap items-center gap-1.5">
            <Badge
              className={`text-[10px] px-2 py-0 h-5 ${meta.color} ${meta.bg} ${meta.border} border font-medium`}
            >
              {model.type}
            </Badge>
            {model.providerName && (
              <Badge
                variant="outline"
                className="text-[10px] px-2 py-0 h-5 text-muted-foreground border-white/[0.08]"
              >
                {model.providerName}
              </Badge>
            )}
            {model.supportStream && (
              <Badge className="text-[10px] px-2 py-0 h-5 bg-blue-500/10 text-blue-400 border border-blue-500/20 font-medium gap-1">
                <Zap className="w-3 h-3" />
                Stream
              </Badge>
            )}
            {model.maxTokenLimit && (
              <Badge
                variant="outline"
                className="text-[10px] px-2 py-0 h-5 text-muted-foreground border-white/[0.08] gap-1 tabular-nums"
              >
                <Hash className="w-3 h-3" />
                {model.maxTokenLimit.toLocaleString()}
              </Badge>
            )}
          </div>

          {/* ---- Config visualisation ---- */}
          {model.config && (
            <div className="space-y-1.5 rounded-lg bg-white/[0.02] border border-white/[0.04] p-3">
              <div className="flex items-center gap-1.5 mb-2">
                <Settings2 className="w-3 h-3 text-muted-foreground" />
                <span className="text-[10px] font-medium text-muted-foreground uppercase tracking-wider">
                  配置
                </span>
              </div>

              {model.config.temperature != null && (
                <GradientBar
                  label="温度"
                  value={model.config.temperature}
                  max={2}
                  fromColor="from-amber-500"
                  toColor="to-orange-500"
                />
              )}
              {model.config.topP != null && (
                <GradientBar
                  label="Top P"
                  value={model.config.topP}
                  max={1}
                  fromColor="from-cyan-500"
                  toColor="to-blue-500"
                />
              )}
              {model.config.maxTokens != null && (
                <div className="flex items-center gap-2">
                  <span className="text-[10px] text-muted-foreground w-16 shrink-0">
                    Token
                  </span>
                  <div className="flex items-center gap-1.5">
                    <Layers className="w-3 h-3 text-muted-foreground" />
                    <span className="text-[10px] text-foreground tabular-nums font-medium">
                      {model.config.maxTokens.toLocaleString()}
                    </span>
                  </div>
                </div>
              )}
            </div>
          )}

          {/* ---- Description (if present, one line) ---- */}
          {model.description && (
            <p className="text-xs text-muted-foreground line-clamp-2 leading-relaxed">
              {model.description}
            </p>
          )}

          {/* ---- Actions ---- */}
          <div className="flex items-center gap-1.5 pt-1">
            <Button
              variant="ghost"
              size="sm"
              className="h-8 px-2.5 text-xs text-muted-foreground hover:text-foreground hover:bg-white/[0.06]"
              onClick={() => onEdit(model)}
            >
              <Pencil className="w-3.5 h-3.5 mr-1.5" />
              编辑
            </Button>
            <Button
              variant="ghost"
              size="sm"
              className="h-8 px-2.5 text-xs text-amber-400 hover:text-amber-300 hover:bg-amber-500/10"
              onClick={() => onManageKeys(model)}
            >
              <Key className="w-3.5 h-3.5 mr-1.5" />
              Key
            </Button>
            <Button
              variant="ghost"
              size="sm"
              className={`h-8 px-2.5 text-xs hover:bg-white/[0.06] ${
                isActive
                  ? 'text-emerald-400 hover:text-emerald-300'
                  : 'text-muted-foreground hover:text-foreground'
              }`}
              onClick={() => onToggle(model)}
            >
              <Power className="w-3.5 h-3.5 mr-1.5" />
              {isActive ? '禁用' : '启用'}
            </Button>
            <div className="flex-1" />
            <Button
              variant="ghost"
              size="sm"
              className="h-8 px-2.5 text-xs text-muted-foreground hover:text-red-400 hover:bg-red-500/10"
              onClick={() => onDelete(model)}
            >
              <Trash2 className="w-3.5 h-3.5" />
            </Button>
          </div>
        </CardContent>
      </Card>
    </motion.div>
  )
}

// ---------------------------------------------------------------------------
// Main page component
// ---------------------------------------------------------------------------

export default function ModelsPage() {
  // ---- data state ----
  const [models, setModels] = useState<AiModel[]>([])
  const [providers, setProviders] = useState<Provider[]>([])
  const [loading, setLoading] = useState(true)

  // ---- filter state ----
  const [search, setSearch] = useState('')
  const [filterType, setFilterType] = useState<string>('all')
  const [filterProvider, setFilterProvider] = useState<string>('all')

  // ---- dialog state ----
  const [dialogOpen, setDialogOpen] = useState(false)
  const [editingModel, setEditingModel] = useState<AiModel | null>(null)
  const [form, setForm] = useState<FormData>({ ...EMPTY_FORM })
  const [saving, setSaving] = useState(false)

  // ---- delete confirm ----
  const [deleteTarget, setDeleteTarget] = useState<AiModel | null>(null)
  const [deleting, setDeleting] = useState(false)

  // ---- key management ----
  const [keyDialogOpen, setKeyDialogOpen] = useState(false)
  const [keyModel, setKeyModel] = useState<AiModel | null>(null)
  const [keys, setKeys] = useState<ModelApiKey[]>([])
  const [keysLoading, setKeysLoading] = useState(false)
  const [keyFormOpen, setKeyFormOpen] = useState(false)
  const [editingKey, setEditingKey] = useState<ModelApiKey | null>(null)
  const [keyForm, setKeyForm] = useState<CreateModelApiKeyRequest>({
    keyName: '',
    apiKey: '',
    weight: 1,
  })
  const [keySaving, setKeySaving] = useState(false)
  const [deleteKeyTarget, setDeleteKeyTarget] = useState<ModelApiKey | null>(null)
  const [deletingKey, setDeletingKey] = useState(false)
  const [copiedKeyId, setCopiedKeyId] = useState<number | null>(null)

  // ---- data fetching ----
  const fetchModels = async () => {
    try {
      const data = await modelApi.getList()
      setModels(data)
    } catch {
      toast.error('加载模型失败')
    }
  }

  const fetchProviders = async () => {
    try {
      const data = await providerApi.getAll()
      setProviders(data)
    } catch {
      toast.error('加载提供商失败')
    }
  }

  useEffect(() => {
    const init = async () => {
      setLoading(true)
      await Promise.all([fetchModels(), fetchProviders()])
      setLoading(false)
    }
    init()
  }, [])

  // ---- filtering ----
  const filtered = models.filter((m) => {
    const matchSearch =
      !search ||
      m.name.toLowerCase().includes(search.toLowerCase()) ||
      m.code.toLowerCase().includes(search.toLowerCase())
    const matchType = filterType === 'all' || m.type === filterType
    const matchProvider =
      filterProvider === 'all' || m.providerId === Number(filterProvider)
    return matchSearch && matchType && matchProvider
  })

  // ---- CRUD helpers ----
  const openCreateDialog = () => {
    setEditingModel(null)
    setForm({ ...EMPTY_FORM })
    setDialogOpen(true)
  }

  const openEditDialog = (model: AiModel) => {
    setEditingModel(model)
    setForm({
      code: model.code,
      name: model.name,
      type: model.type,
      providerId: model.providerId,
      supportStream: model.supportStream,
      maxTokenLimit: model.maxTokenLimit,
      description: model.description ?? '',
      config: {
        temperature: model.config?.temperature ?? 0.7,
        maxTokens: model.config?.maxTokens ?? 4096,
        topP: model.config?.topP ?? 1,
      },
    })
    setDialogOpen(true)
  }

  const handleSave = async () => {
    if (!form.code.trim() || !form.name.trim()) {
      toast.error('模型代码和名称为必填项')
      return
    }
    if (!form.providerId) {
      toast.error('请选择提供商')
      return
    }

    setSaving(true)
    try {
      if (editingModel) {
        const payload: UpdateModelRequest = {
          name: form.name,
          supportStream: form.supportStream,
          maxTokenLimit: form.maxTokenLimit,
          description: form.description || undefined,
          config: form.config,
        }
        await modelApi.update(editingModel.id, payload)
        toast.success('模型已更新')
      } else {
        const payload: CreateModelRequest = {
          code: form.code,
          name: form.name,
          type: form.type,
          providerId: form.providerId,
          supportStream: form.supportStream,
          maxTokenLimit: form.maxTokenLimit,
          description: form.description || undefined,
          config: form.config,
        }
        await modelApi.create(payload)
        toast.success('模型已创建')
      }
      setDialogOpen(false)
      await fetchModels()
    } catch {
      toast.error(editingModel ? '更新模型失败' : '创建模型失败')
    } finally {
      setSaving(false)
    }
  }

  const handleToggleStatus = async (model: AiModel) => {
    try {
      await modelApi.updateStatus(model.id)
      toast.success(
        model.status === 1 ? '模型已禁用' : '模型已启用',
      )
      await fetchModels()
    } catch {
      toast.error('更新状态失败')
    }
  }

  const handleDelete = async () => {
    if (!deleteTarget) return
    setDeleting(true)
    try {
      await modelApi.delete(deleteTarget.id)
      toast.success('模型已删除')
      setDeleteTarget(null)
      await fetchModels()
    } catch {
      toast.error('删除模型失败')
    } finally {
      setDeleting(false)
    }
  }

  // ---- key management helpers ----
  const openKeyDialog = async (model: AiModel) => {
    setKeyModel(model)
    setKeyDialogOpen(true)
    setKeysLoading(true)
    try {
      const data = await modelApiKeyApi.list(model.id)
      setKeys(data)
    } catch {
      toast.error('加载 Key 列表失败')
    } finally {
      setKeysLoading(false)
    }
  }

  const refreshKeys = async () => {
    if (!keyModel) return
    try {
      const data = await modelApiKeyApi.list(keyModel.id)
      setKeys(data)
    } catch {
      toast.error('刷新 Key 列表失败')
    }
  }

  const openCreateKey = () => {
    setEditingKey(null)
    setKeyForm({ keyName: '', apiKey: '', weight: 1 })
    setKeyFormOpen(true)
  }

  const openEditKey = (k: ModelApiKey) => {
    setEditingKey(k)
    setKeyForm({
      keyName: k.keyName,
      apiKey: '',
      weight: k.weight,
      rateLimitPerMinute: k.rateLimitPerMinute,
      rateLimitPerDay: k.rateLimitPerDay,
      quotaTotal: k.quotaTotal,
      expireTime: k.expireTime,
      sortOrder: k.sortOrder,
      description: k.description,
    })
    setKeyFormOpen(true)
  }

  const handleSaveKey = async () => {
    if (!keyModel) return
    if (!keyForm.keyName.trim()) { toast.error('Key 名称为必填项'); return }
    if (!editingKey && !keyForm.apiKey.trim()) { toast.error('API Key 为必填项'); return }

    setKeySaving(true)
    try {
      if (editingKey) {
        const payload: UpdateModelApiKeyRequest = { ...keyForm }
        if (!payload.apiKey) delete payload.apiKey
        await modelApiKeyApi.update(keyModel.id, editingKey.id, payload)
        toast.success('Key 已更新')
      } else {
        await modelApiKeyApi.create(keyModel.id, keyForm)
        toast.success('Key 已添加')
      }
      setKeyFormOpen(false)
      await refreshKeys()
    } catch {
      toast.error(editingKey ? '更新 Key 失败' : '添加 Key 失败')
    } finally {
      setKeySaving(false)
    }
  }

  const handleToggleKeyStatus = async (k: ModelApiKey) => {
    if (!keyModel) return
    try {
      await modelApiKeyApi.toggleStatus(keyModel.id, k.id)
      toast.success(k.status === 1 ? 'Key 已禁用' : 'Key 已启用')
      await refreshKeys()
    } catch {
      toast.error('切换状态失败')
    }
  }

  const handleResetKeyHealth = async (k: ModelApiKey) => {
    if (!keyModel) return
    try {
      await modelApiKeyApi.resetHealth(keyModel.id, k.id)
      toast.success('健康状态已重置')
      await refreshKeys()
    } catch {
      toast.error('重置健康状态失败')
    }
  }

  const handleDeleteKey = async () => {
    if (!keyModel || !deleteKeyTarget) return
    setDeletingKey(true)
    try {
      await modelApiKeyApi.delete(keyModel.id, deleteKeyTarget.id)
      toast.success('Key 已删除')
      setDeleteKeyTarget(null)
      await refreshKeys()
    } catch {
      toast.error('删除 Key 失败')
    } finally {
      setDeletingKey(false)
    }
  }

  const handleCopyKey = async (k: ModelApiKey) => {
    try {
      await navigator.clipboard.writeText(k.apiKey)
      setCopiedKeyId(k.id)
      setTimeout(() => setCopiedKeyId(null), 2000)
    } catch {
      toast.error('复制失败')
    }
  }

  const updateKeyForm = <K extends keyof CreateModelApiKeyRequest>(key: K, value: CreateModelApiKeyRequest[K]) =>
    setKeyForm((prev) => ({ ...prev, [key]: value }))

  // ---- form updaters ----
  const updateForm = <K extends keyof FormData>(key: K, value: FormData[K]) =>
    setForm((prev) => ({ ...prev, [key]: value }))

  const updateConfig = <K extends keyof ModelConfig>(
    key: K,
    value: ModelConfig[K],
  ) =>
    setForm((prev) => ({
      ...prev,
      config: { ...prev.config, [key]: value },
    }))

  // ---- active filter count (for the filter icon badge) ----
  const activeFilters =
    (filterType !== 'all' ? 1 : 0) + (filterProvider !== 'all' ? 1 : 0)

  // ====================================================================
  // Render
  // ====================================================================

  return (
    <div className="space-y-6">
      {/* ---- Page header ---- */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold tracking-tight">模型管理</h1>
          <p className="text-sm text-muted-foreground mt-1">
            管理 AI 模型，配置参数并监控可用性。
          </p>
        </div>
        <Button onClick={openCreateDialog} className="gap-2 shrink-0">
          <Plus className="w-4 h-4" />
          添加模型
        </Button>
      </div>

      {/* ---- Filter bar ---- */}
      <div className="flex flex-col sm:flex-row items-stretch sm:items-center gap-3">
        {/* Search */}
        <div className="relative flex-1 min-w-0">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-muted-foreground pointer-events-none" />
          <Input
            placeholder="按名称或代码搜索模型..."
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            className="pl-9 bg-white/[0.03] border-white/[0.06] focus:border-white/[0.12] h-10"
          />
        </div>

        {/* Type filter */}
        <Select value={filterType} onValueChange={setFilterType}>
          <SelectTrigger className="w-full sm:w-[160px] bg-white/[0.03] border-white/[0.06] h-10">
            <div className="flex items-center gap-2">
              <Filter className="w-3.5 h-3.5 text-muted-foreground" />
              <SelectValue placeholder="所有类型" />
            </div>
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="all">所有类型</SelectItem>
            {MODEL_TYPES.map((t) => {
              const m = getTypeMeta(t)
              const Ic = m.icon
              return (
                <SelectItem key={t} value={t}>
                  <span className="flex items-center gap-2">
                    <Ic className={`w-3.5 h-3.5 ${m.color}`} />
                    {t}
                  </span>
                </SelectItem>
              )
            })}
          </SelectContent>
        </Select>

        {/* Provider filter */}
        <Select value={filterProvider} onValueChange={setFilterProvider}>
          <SelectTrigger className="w-full sm:w-[180px] bg-white/[0.03] border-white/[0.06] h-10">
            <SelectValue placeholder="所有提供商" />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="all">所有提供商</SelectItem>
            {providers.map((p) => (
              <SelectItem key={p.id} value={String(p.id)}>
                {p.name}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>

        {/* Active filter indicator */}
        {activeFilters > 0 && (
          <Button
            variant="ghost"
            size="sm"
            className="text-xs text-muted-foreground hover:text-foreground h-10 px-3"
            onClick={() => {
              setFilterType('all')
              setFilterProvider('all')
              setSearch('')
            }}
          >
            清除筛选
            <Badge className="ml-1.5 px-1.5 h-4 text-[10px] bg-primary/20 text-primary border-0">
              {activeFilters}
            </Badge>
          </Button>
        )}
      </div>

      {/* ---- Content area ---- */}
      {loading ? (
        <div className="flex flex-col items-center justify-center py-32 gap-4">
          <Loader2 className="w-8 h-8 text-primary animate-spin" />
          <p className="text-sm text-muted-foreground">加载模型中...</p>
        </div>
      ) : filtered.length === 0 ? (
        <motion.div
          initial={{ opacity: 0, y: 12 }}
          animate={{ opacity: 1, y: 0 }}
          className="flex flex-col items-center justify-center py-32 gap-4"
        >
          <div className="w-16 h-16 rounded-2xl bg-white/[0.04] border border-white/[0.06] flex items-center justify-center">
            <Bot className="w-8 h-8 text-muted-foreground" />
          </div>
          <div className="text-center">
            <p className="text-sm font-medium text-foreground">
              {models.length === 0 ? '暂无模型' : '没有匹配的模型'}
            </p>
            <p className="text-xs text-muted-foreground mt-1">
              {models.length === 0
                ? '添加您的第一个 AI 模型以开始使用。'
                : '请尝试调整搜索条件或筛选项。'}
            </p>
          </div>
          {models.length === 0 && (
            <Button onClick={openCreateDialog} className="gap-2 mt-2">
              <Plus className="w-4 h-4" />
              添加模型
            </Button>
          )}
        </motion.div>
      ) : (
        <>
          {/* Result count */}
          <div className="flex items-center gap-2 text-xs text-muted-foreground">
            <span className="tabular-nums">{filtered.length}</span>
            <span>
              个模型
              {filtered.length !== models.length &&
                `（共 ${models.length} 个）`}
            </span>
          </div>

          {/* Grid */}
          <motion.div
            layout
            className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-4"
          >
            <AnimatePresence mode="popLayout">
              {filtered.map((model, i) => (
                <ModelCard
                  key={model.id}
                  model={model}
                  index={i}
                  onEdit={openEditDialog}
                  onToggle={handleToggleStatus}
                  onDelete={setDeleteTarget}
                  onManageKeys={openKeyDialog}
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
        <DialogContent className="sm:max-w-[560px] bg-[hsl(240_10%_5%)] border-white/[0.08] max-h-[90vh] overflow-y-auto">
          <DialogHeader>
            <DialogTitle>
              {editingModel ? '编辑模型' : '创建模型'}
            </DialogTitle>
            <DialogDescription>
              {editingModel
                ? '更新下方的模型配置。'
                : '向平台添加新的 AI 模型。'}
            </DialogDescription>
          </DialogHeader>

          <div className="space-y-5 py-2">
            {/* ---- Basic fields ---- */}
            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-2">
                <Label htmlFor="model-code">
                  代码 <span className="text-red-400">*</span>
                </Label>
                <Input
                  id="model-code"
                  placeholder="gpt-4o"
                  value={form.code}
                  onChange={(e) => updateForm('code', e.target.value)}
                  disabled={!!editingModel}
                  className="font-mono text-sm bg-white/[0.03] border-white/[0.06]"
                />
              </div>
              <div className="space-y-2">
                <Label htmlFor="model-name">
                  名称 <span className="text-red-400">*</span>
                </Label>
                <Input
                  id="model-name"
                  placeholder="GPT-4o"
                  value={form.name}
                  onChange={(e) => updateForm('name', e.target.value)}
                  className="bg-white/[0.03] border-white/[0.06]"
                />
              </div>
            </div>

            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-2">
                <Label>
                  类型 <span className="text-red-400">*</span>
                </Label>
                <Select
                  value={form.type}
                  onValueChange={(v) => updateForm('type', v)}
                >
                  <SelectTrigger className="bg-white/[0.03] border-white/[0.06]">
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    {MODEL_TYPES.map((t) => {
                      const m = getTypeMeta(t)
                      const Ic = m.icon
                      return (
                        <SelectItem key={t} value={t}>
                          <span className="flex items-center gap-2">
                            <Ic className={`w-3.5 h-3.5 ${m.color}`} />
                            {t}
                          </span>
                        </SelectItem>
                      )
                    })}
                  </SelectContent>
                </Select>
              </div>
              <div className="space-y-2">
                <Label>
                  提供商 <span className="text-red-400">*</span>
                </Label>
                <Select
                  value={form.providerId ? String(form.providerId) : ''}
                  onValueChange={(v) => updateForm('providerId', Number(v))}
                >
                  <SelectTrigger className="bg-white/[0.03] border-white/[0.06]">
                    <SelectValue placeholder="请选择提供商" />
                  </SelectTrigger>
                  <SelectContent>
                    {providers.map((p) => (
                      <SelectItem key={p.id} value={String(p.id)}>
                        {p.name}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>
            </div>

            {/* ---- Stream + Token limit ---- */}
            <div className="grid grid-cols-2 gap-4">
              <div className="flex items-center justify-between rounded-lg border border-white/[0.06] bg-white/[0.02] px-4 py-3">
                <div className="space-y-0.5">
                  <Label className="text-sm">流式支持</Label>
                  <p className="text-[11px] text-muted-foreground">
                    启用流式响应
                  </p>
                </div>
                <Switch
                  checked={form.supportStream}
                  onCheckedChange={(v) => updateForm('supportStream', v)}
                />
              </div>
              <div className="space-y-2">
                <Label htmlFor="max-token-limit">最大 Token 限制</Label>
                <Input
                  id="max-token-limit"
                  type="number"
                  placeholder="128000"
                  value={form.maxTokenLimit ?? ''}
                  onChange={(e) =>
                    updateForm(
                      'maxTokenLimit',
                      e.target.value ? Number(e.target.value) : undefined,
                    )
                  }
                  className="bg-white/[0.03] border-white/[0.06]"
                />
              </div>
            </div>

            {/* ---- Config section ---- */}
            <div className="space-y-3 rounded-lg border border-white/[0.06] bg-white/[0.02] p-4">
              <div className="flex items-center gap-2 text-sm font-medium">
                <Settings2 className="w-4 h-4 text-muted-foreground" />
                模型配置
              </div>

              <div className="grid grid-cols-3 gap-3">
                <div className="space-y-2">
                  <Label
                    htmlFor="cfg-temperature"
                    className="text-xs text-muted-foreground flex items-center gap-1.5"
                  >
                    <Thermometer className="w-3 h-3" />
                    温度
                  </Label>
                  <Input
                    id="cfg-temperature"
                    type="number"
                    step="0.1"
                    min="0"
                    max="2"
                    value={form.config.temperature ?? ''}
                    onChange={(e) =>
                      updateConfig(
                        'temperature',
                        e.target.value ? Number(e.target.value) : undefined,
                      )
                    }
                    className="bg-white/[0.03] border-white/[0.06] text-sm"
                  />
                </div>
                <div className="space-y-2">
                  <Label
                    htmlFor="cfg-maxtokens"
                    className="text-xs text-muted-foreground flex items-center gap-1.5"
                  >
                    <Hash className="w-3 h-3" />
                    最大 Token
                  </Label>
                  <Input
                    id="cfg-maxtokens"
                    type="number"
                    step="1"
                    min="1"
                    value={form.config.maxTokens ?? ''}
                    onChange={(e) =>
                      updateConfig(
                        'maxTokens',
                        e.target.value ? Number(e.target.value) : undefined,
                      )
                    }
                    className="bg-white/[0.03] border-white/[0.06] text-sm"
                  />
                </div>
                <div className="space-y-2">
                  <Label
                    htmlFor="cfg-topp"
                    className="text-xs text-muted-foreground flex items-center gap-1.5"
                  >
                    <Layers className="w-3 h-3" />
                    Top P
                  </Label>
                  <Input
                    id="cfg-topp"
                    type="number"
                    step="0.05"
                    min="0"
                    max="1"
                    value={form.config.topP ?? ''}
                    onChange={(e) =>
                      updateConfig(
                        'topP',
                        e.target.value ? Number(e.target.value) : undefined,
                      )
                    }
                    className="bg-white/[0.03] border-white/[0.06] text-sm"
                  />
                </div>
              </div>
            </div>

            {/* ---- Description ---- */}
            <div className="space-y-2">
              <Label htmlFor="model-desc">描述</Label>
              <Textarea
                id="model-desc"
                placeholder="模型描述（可选）..."
                rows={3}
                value={form.description}
                onChange={(e) => updateForm('description', e.target.value)}
                className="bg-white/[0.03] border-white/[0.06] resize-none"
              />
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
              {editingModel ? '更新' : '创建'}
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
            <AlertDialogTitle>删除模型</AlertDialogTitle>
            <AlertDialogDescription>
              确定要删除{' '}
              <span className="font-semibold text-foreground">
                {deleteTarget?.name}
              </span>
              {' '}吗？此操作无法撤销。
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
      {/* Key Management Dialog                                              */}
      {/* ================================================================= */}
      <Dialog open={keyDialogOpen} onOpenChange={(open) => { if (!open) { setKeyDialogOpen(false); setKeyModel(null); setKeys([]) } }}>
        <DialogContent className="sm:max-w-[780px] bg-[hsl(240_10%_5%)] border-white/[0.08] max-h-[90vh] overflow-y-auto">
          <DialogHeader>
            <DialogTitle className="flex items-center gap-2">
              <Key className="w-5 h-5 text-amber-400" />
              Key 管理 — {keyModel?.name}
            </DialogTitle>
            <DialogDescription>
              管理该模型的上游 API Key 池，支持负载均衡和故障切换。
            </DialogDescription>
          </DialogHeader>

          <div className="space-y-4 py-2">
            {/* Add key button */}
            <div className="flex justify-end">
              <Button size="sm" className="gap-2" onClick={openCreateKey}>
                <Plus className="w-4 h-4" />
                添加 Key
              </Button>
            </div>

            {/* Keys list */}
            {keysLoading ? (
              <div className="flex items-center justify-center py-16">
                <Loader2 className="w-6 h-6 text-primary animate-spin" />
              </div>
            ) : keys.length === 0 ? (
              <div className="flex flex-col items-center justify-center py-16 gap-3">
                <div className="w-12 h-12 rounded-xl bg-white/[0.04] border border-white/[0.06] flex items-center justify-center">
                  <Key className="w-6 h-6 text-muted-foreground" />
                </div>
                <p className="text-sm text-muted-foreground">暂无 API Key</p>
                <Button size="sm" variant="outline" className="gap-2" onClick={openCreateKey}>
                  <Plus className="w-4 h-4" />
                  添加第一个 Key
                </Button>
              </div>
            ) : (
              <div className="space-y-3">
                {keys.map((k) => {
                  const isHealthy = k.healthStatus === 1
                  const isUnhealthy = k.healthStatus === 0
                  const isEnabled = k.status === 1
                  const successRate = k.totalRequests > 0
                    ? Math.round((k.successRequests / k.totalRequests) * 100)
                    : null
                  const quotaPct = k.quotaTotal && k.quotaTotal > 0
                    ? Math.round(((k.quotaUsed ?? 0) / k.quotaTotal) * 100)
                    : null

                  return (
                    <motion.div
                      key={k.id}
                      initial={{ opacity: 0, y: 8 }}
                      animate={{ opacity: 1, y: 0 }}
                      className={`rounded-lg border bg-white/[0.02] p-4 space-y-3 ${
                        !isEnabled
                          ? 'border-white/[0.04] opacity-60'
                          : isUnhealthy
                            ? 'border-red-500/20'
                            : 'border-white/[0.06]'
                      }`}
                    >
                      {/* Row 1: Name + status + actions */}
                      <div className="flex items-center justify-between gap-3">
                        <div className="flex items-center gap-3 min-w-0">
                          <div className={`shrink-0 w-8 h-8 rounded-lg flex items-center justify-center ${
                            !isEnabled ? 'bg-zinc-500/10' : isHealthy ? 'bg-emerald-500/10' : 'bg-red-500/10'
                          }`}>
                            {!isEnabled
                              ? <ShieldOff className="w-4 h-4 text-zinc-500" />
                              : isHealthy
                                ? <Shield className="w-4 h-4 text-emerald-400" />
                                : <ShieldAlert className="w-4 h-4 text-red-400" />
                            }
                          </div>
                          <div className="min-w-0">
                            <h4 className="text-sm font-medium truncate">{k.keyName}</h4>
                            <div className="flex items-center gap-2 mt-0.5">
                              <code className="text-[11px] text-muted-foreground font-mono">{k.apiKeyMasked}</code>
                              <TooltipProvider delayDuration={0}>
                                <Tooltip>
                                  <TooltipTrigger asChild>
                                    <button
                                      className="text-muted-foreground hover:text-foreground transition-colors"
                                      onClick={() => handleCopyKey(k)}
                                    >
                                      {copiedKeyId === k.id
                                        ? <Check className="w-3 h-3 text-emerald-400" />
                                        : <Copy className="w-3 h-3" />
                                      }
                                    </button>
                                  </TooltipTrigger>
                                  <TooltipContent side="top"><p>复制 Key</p></TooltipContent>
                                </Tooltip>
                              </TooltipProvider>
                            </div>
                          </div>
                        </div>

                        <div className="flex items-center gap-1.5 shrink-0">
                          <Badge className={`text-[10px] px-2 h-5 ${
                            !isEnabled
                              ? 'bg-zinc-500/10 text-zinc-500 border-zinc-500/20'
                              : isHealthy
                                ? 'bg-emerald-500/10 text-emerald-400 border-emerald-500/20'
                                : 'bg-red-500/10 text-red-400 border-red-500/20'
                          } border`}>
                            {!isEnabled ? '已禁用' : isHealthy ? '健康' : '异常'}
                          </Badge>
                          <Badge variant="outline" className="text-[10px] px-2 h-5 border-white/[0.08] text-muted-foreground">
                            权重 {k.weight}
                          </Badge>
                        </div>
                      </div>

                      {/* Row 2: Stats */}
                      <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
                        <div className="space-y-1">
                          <span className="text-[10px] text-muted-foreground uppercase tracking-wider">请求统计</span>
                          <div className="flex items-baseline gap-1.5">
                            <Activity className="w-3 h-3 text-blue-400 shrink-0 self-center" />
                            <span className="text-xs tabular-nums font-medium">{k.totalRequests.toLocaleString()}</span>
                            {successRate !== null && (
                              <span className={`text-[10px] tabular-nums ${successRate >= 95 ? 'text-emerald-400' : successRate >= 80 ? 'text-amber-400' : 'text-red-400'}`}>
                                {successRate}%
                              </span>
                            )}
                          </div>
                        </div>

                        {k.failCount > 0 && (
                          <div className="space-y-1">
                            <span className="text-[10px] text-muted-foreground uppercase tracking-wider">连续失败</span>
                            <div className="flex items-baseline gap-1.5">
                              <AlertTriangle className="w-3 h-3 text-red-400 shrink-0 self-center" />
                              <span className="text-xs tabular-nums font-medium text-red-400">{k.failCount}</span>
                            </div>
                          </div>
                        )}

                        {quotaPct !== null && (
                          <div className="space-y-1">
                            <span className="text-[10px] text-muted-foreground uppercase tracking-wider">配额</span>
                            <div className="space-y-1">
                              <Progress value={quotaPct} className="h-1.5" />
                              <span className="text-[10px] tabular-nums text-muted-foreground">
                                {(k.quotaUsed ?? 0).toLocaleString()} / {k.quotaTotal!.toLocaleString()}
                              </span>
                            </div>
                          </div>
                        )}

                        {k.lastUsedTime && (
                          <div className="space-y-1">
                            <span className="text-[10px] text-muted-foreground uppercase tracking-wider">最后使用</span>
                            <div className="flex items-baseline gap-1.5">
                              <Clock className="w-3 h-3 text-muted-foreground shrink-0 self-center" />
                              <span className="text-[10px] tabular-nums text-muted-foreground">{k.lastUsedTime}</span>
                            </div>
                          </div>
                        )}
                      </div>

                      {/* Row 3: Rate limits + description */}
                      {(k.rateLimitPerMinute || k.rateLimitPerDay || k.expireTime || k.description) && (
                        <div className="flex flex-wrap items-center gap-2">
                          {k.rateLimitPerMinute && (
                            <Badge variant="outline" className="text-[10px] px-2 h-5 border-white/[0.06] text-muted-foreground">
                              {k.rateLimitPerMinute}/min
                            </Badge>
                          )}
                          {k.rateLimitPerDay && (
                            <Badge variant="outline" className="text-[10px] px-2 h-5 border-white/[0.06] text-muted-foreground">
                              {k.rateLimitPerDay.toLocaleString()}/day
                            </Badge>
                          )}
                          {k.expireTime && (
                            <Badge variant="outline" className="text-[10px] px-2 h-5 border-white/[0.06] text-muted-foreground gap-1">
                              <Clock className="w-3 h-3" />
                              {k.expireTime}
                            </Badge>
                          )}
                          {k.description && (
                            <span className="text-[10px] text-muted-foreground truncate">{k.description}</span>
                          )}
                        </div>
                      )}

                      {/* Row 4: Actions */}
                      <div className="flex items-center gap-1.5 border-t border-white/[0.04] pt-3">
                        <Button variant="ghost" size="sm" className="h-7 px-2 text-xs text-muted-foreground hover:text-foreground" onClick={() => openEditKey(k)}>
                          <Pencil className="w-3 h-3 mr-1" />编辑
                        </Button>
                        <Button
                          variant="ghost" size="sm"
                          className={`h-7 px-2 text-xs ${isEnabled ? 'text-emerald-400 hover:text-emerald-300' : 'text-muted-foreground hover:text-foreground'}`}
                          onClick={() => handleToggleKeyStatus(k)}
                        >
                          <Power className="w-3 h-3 mr-1" />{isEnabled ? '禁用' : '启用'}
                        </Button>
                        {isUnhealthy && (
                          <Button variant="ghost" size="sm" className="h-7 px-2 text-xs text-amber-400 hover:text-amber-300" onClick={() => handleResetKeyHealth(k)}>
                            <RotateCcw className="w-3 h-3 mr-1" />重置健康
                          </Button>
                        )}
                        <div className="flex-1" />
                        <Button variant="ghost" size="sm" className="h-7 px-2 text-xs text-muted-foreground hover:text-red-400" onClick={() => setDeleteKeyTarget(k)}>
                          <Trash2 className="w-3 h-3" />
                        </Button>
                      </div>
                    </motion.div>
                  )
                })}
              </div>
            )}
          </div>
        </DialogContent>
      </Dialog>

      {/* ================================================================= */}
      {/* Key Create / Edit Dialog                                           */}
      {/* ================================================================= */}
      <Dialog open={keyFormOpen} onOpenChange={setKeyFormOpen}>
        <DialogContent className="sm:max-w-[480px] bg-[hsl(240_10%_6%)] border-white/[0.08] max-h-[85vh] overflow-y-auto">
          <DialogHeader>
            <DialogTitle>{editingKey ? '编辑 Key' : '添加 Key'}</DialogTitle>
            <DialogDescription>
              {editingKey ? '更新 API Key 配置。留空 API Key 字段则不修改密钥。' : '为该模型添加一个新的上游 API Key。'}
            </DialogDescription>
          </DialogHeader>

          <div className="space-y-4 py-2">
            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-2">
                <Label>名称 <span className="text-red-400">*</span></Label>
                <Input
                  placeholder="主 Key"
                  value={keyForm.keyName}
                  onChange={(e) => updateKeyForm('keyName', e.target.value)}
                  className="bg-white/[0.03] border-white/[0.06]"
                />
              </div>
              <div className="space-y-2">
                <Label>权重</Label>
                <Input
                  type="number" min="1" placeholder="1"
                  value={keyForm.weight ?? ''}
                  onChange={(e) => updateKeyForm('weight', e.target.value ? Number(e.target.value) : undefined)}
                  className="bg-white/[0.03] border-white/[0.06]"
                />
              </div>
            </div>

            <div className="space-y-2">
              <Label>API Key {!editingKey && <span className="text-red-400">*</span>}</Label>
              <Input
                placeholder={editingKey ? '留空则不修改' : 'sk-xxxxxxxxxxxx'}
                value={keyForm.apiKey}
                onChange={(e) => updateKeyForm('apiKey', e.target.value)}
                className="bg-white/[0.03] border-white/[0.06] font-mono text-sm"
              />
            </div>

            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-2">
                <Label>每分钟限制</Label>
                <Input
                  type="number" placeholder="60"
                  value={keyForm.rateLimitPerMinute ?? ''}
                  onChange={(e) => updateKeyForm('rateLimitPerMinute', e.target.value ? Number(e.target.value) : undefined)}
                  className="bg-white/[0.03] border-white/[0.06]"
                />
              </div>
              <div className="space-y-2">
                <Label>每天限制</Label>
                <Input
                  type="number" placeholder="10000"
                  value={keyForm.rateLimitPerDay ?? ''}
                  onChange={(e) => updateKeyForm('rateLimitPerDay', e.target.value ? Number(e.target.value) : undefined)}
                  className="bg-white/[0.03] border-white/[0.06]"
                />
              </div>
            </div>

            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-2">
                <Label>总配额</Label>
                <Input
                  type="number" placeholder="-1 表示无限"
                  value={keyForm.quotaTotal ?? ''}
                  onChange={(e) => updateKeyForm('quotaTotal', e.target.value ? Number(e.target.value) : undefined)}
                  className="bg-white/[0.03] border-white/[0.06]"
                />
              </div>
              <div className="space-y-2">
                <Label>排序</Label>
                <Input
                  type="number" placeholder="0"
                  value={keyForm.sortOrder ?? ''}
                  onChange={(e) => updateKeyForm('sortOrder', e.target.value ? Number(e.target.value) : undefined)}
                  className="bg-white/[0.03] border-white/[0.06]"
                />
              </div>
            </div>

            <div className="space-y-2">
              <Label>过期时间</Label>
              <Input
                type="datetime-local"
                value={keyForm.expireTime ? keyForm.expireTime.replace(' ', 'T').slice(0, 16) : ''}
                onChange={(e) => updateKeyForm('expireTime', e.target.value ? e.target.value.replace('T', ' ') + ':00' : undefined)}
                className="bg-white/[0.03] border-white/[0.06]"
              />
            </div>

            <div className="space-y-2">
              <Label>描述</Label>
              <Textarea
                placeholder="可选描述..."
                rows={2}
                value={keyForm.description ?? ''}
                onChange={(e) => updateKeyForm('description', e.target.value || undefined)}
                className="bg-white/[0.03] border-white/[0.06] resize-none"
              />
            </div>
          </div>

          <DialogFooter>
            <Button variant="ghost" onClick={() => setKeyFormOpen(false)} disabled={keySaving}>取消</Button>
            <Button onClick={handleSaveKey} disabled={keySaving} className="gap-2">
              {keySaving && <Loader2 className="w-4 h-4 animate-spin" />}
              {editingKey ? '更新' : '添加'}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* ================================================================= */}
      {/* Delete Key confirmation                                            */}
      {/* ================================================================= */}
      <AlertDialog open={!!deleteKeyTarget} onOpenChange={(open) => !open && setDeleteKeyTarget(null)}>
        <AlertDialogContent className="bg-[hsl(240_10%_5%)] border-white/[0.08]">
          <AlertDialogHeader>
            <AlertDialogTitle>删除 Key</AlertDialogTitle>
            <AlertDialogDescription>
              确定要删除 <span className="font-semibold text-foreground">{deleteKeyTarget?.keyName}</span> 吗？此操作无法撤销。
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel disabled={deletingKey}>取消</AlertDialogCancel>
            <AlertDialogAction onClick={handleDeleteKey} disabled={deletingKey} className="bg-red-600 hover:bg-red-700 text-white gap-2">
              {deletingKey && <Loader2 className="w-4 h-4 animate-spin" />}
              删除
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </div>
  )
}
