import { useState, useEffect } from 'react'
import { motion } from 'framer-motion'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Badge } from '@/components/ui/badge'
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
import { adminUserApi } from '@/api/user'
import { adminRoleApi } from '@/api/role'
import type {
  AdminUser,
  CreateAdminUserRequest,
  UpdateAdminUserRequest,
  AdminRole,
} from '@/types/user'
import type { PageResult } from '@/types/common'
import { toast } from 'sonner'
import {
  Plus,
  Search,
  Users,
  Pencil,
  Trash2,
  Power,
  KeyRound,
  Loader2,
  Mail,
  Phone,
  ChevronLeft,
  ChevronRight,
} from 'lucide-react'

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

function formatTime(time?: string) {
  if (!time) return '-'
  return new Date(time).toLocaleString()
}

function getInitials(name: string) {
  return name
    .split(/\s+/)
    .map((w) => w[0])
    .join('')
    .toUpperCase()
    .slice(0, 2)
}

const STATUS_OPTIONS = [
  { value: 'all', label: '所有状态' },
  { value: '1', label: '正常' },
  { value: '0', label: '已禁用' },
]

// ---------------------------------------------------------------------------
// User row component
// ---------------------------------------------------------------------------

function UserRow({
  user,
  index,
  onEdit,
  onToggle,
  onDelete,
  onResetPassword,
}: {
  user: AdminUser
  index: number
  onEdit: (u: AdminUser) => void
  onToggle: (u: AdminUser) => void
  onDelete: (u: AdminUser) => void
  onResetPassword: (u: AdminUser) => void
}) {
  const isActive = user.status === 1

  return (
    <motion.div
      initial={{ opacity: 0, y: 16 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{
        duration: 0.3,
        delay: index * 0.03,
        ease: [0.23, 1, 0.32, 1],
      }}
    >
      <Card
        className={`
          group relative
          bg-white/[0.03] border-white/[0.06]
          hover:bg-white/[0.06] hover:border-white/[0.1]
          hover:shadow-[0_0_30px_-5px_rgba(100,140,255,0.08)]
          transition-all duration-300
        `}
      >
        <CardContent className="p-4">
          <div className="flex items-center gap-4">
            {/* Avatar */}
            <div className="shrink-0 w-10 h-10 rounded-full bg-gradient-to-br from-blue-500/20 to-purple-500/20 border border-white/[0.08] flex items-center justify-center">
              <span className="text-xs font-semibold text-blue-300">
                {getInitials(user.realName || user.username)}
              </span>
            </div>

            {/* User info */}
            <div className="flex-1 min-w-0 grid grid-cols-1 sm:grid-cols-[1fr_1fr_1fr_auto] gap-2 sm:gap-4 items-center">
              {/* Name + username */}
              <div className="min-w-0">
                <h3 className="text-sm font-semibold text-foreground truncate leading-tight">
                  {user.realName || user.username}
                </h3>
                <p className="text-xs font-mono text-muted-foreground truncate mt-0.5">
                  @{user.username}
                </p>
              </div>

              {/* Contact */}
              <div className="min-w-0 hidden sm:block">
                {user.email && (
                  <div className="flex items-center gap-1.5 text-xs text-muted-foreground truncate">
                    <Mail className="w-3 h-3 shrink-0" />
                    <span className="truncate">{user.email}</span>
                  </div>
                )}
                {user.phone && (
                  <div className="flex items-center gap-1.5 text-xs text-muted-foreground truncate mt-0.5">
                    <Phone className="w-3 h-3 shrink-0" />
                    <span className="truncate">{user.phone}</span>
                  </div>
                )}
              </div>

              {/* Roles + status */}
              <div className="flex flex-wrap items-center gap-1.5">
                {user.roles?.map((role) => (
                  <Badge
                    key={role.id}
                    className="text-[10px] px-2 py-0 h-5 bg-purple-500/10 text-purple-400 border border-purple-500/20 font-medium"
                  >
                    {role.roleName}
                  </Badge>
                ))}
                <div className="flex items-center gap-1.5 ml-1">
                  <div className={isActive ? 'dot-online' : 'dot-offline'} />
                  <span
                    className={`text-[10px] font-medium ${isActive ? 'text-emerald-400' : 'text-zinc-500'}`}
                  >
                    {isActive ? '正常' : '已禁用'}
                  </span>
                </div>
              </div>

              {/* Last login */}
              <div className="hidden sm:block text-right">
                <p className="text-[10px] text-muted-foreground">最后登录</p>
                <p className="text-xs text-muted-foreground tabular-nums">
                  {formatTime(user.lastLoginTime)}
                </p>
              </div>
            </div>

            {/* Actions */}
            <div className="flex items-center gap-1 shrink-0">
              <Button
                variant="ghost"
                size="sm"
                className="h-8 w-8 p-0 text-muted-foreground hover:text-foreground hover:bg-white/[0.06]"
                onClick={() => onEdit(user)}
                title="编辑"
              >
                <Pencil className="w-3.5 h-3.5" />
              </Button>
              <Button
                variant="ghost"
                size="sm"
                className={`h-8 w-8 p-0 hover:bg-white/[0.06] ${
                  isActive
                    ? 'text-emerald-400 hover:text-emerald-300'
                    : 'text-muted-foreground hover:text-foreground'
                }`}
                onClick={() => onToggle(user)}
                title={isActive ? '禁用' : '启用'}
              >
                <Power className="w-3.5 h-3.5" />
              </Button>
              <Button
                variant="ghost"
                size="sm"
                className="h-8 w-8 p-0 text-muted-foreground hover:text-amber-400 hover:bg-amber-500/10"
                onClick={() => onResetPassword(user)}
                title="重置密码"
              >
                <KeyRound className="w-3.5 h-3.5" />
              </Button>
              <Button
                variant="ghost"
                size="sm"
                className="h-8 w-8 p-0 text-muted-foreground hover:text-red-400 hover:bg-red-500/10"
                onClick={() => onDelete(user)}
                title="删除"
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
// Create / Edit form types
// ---------------------------------------------------------------------------

interface UserFormData {
  username: string
  password: string
  realName: string
  email: string
  phone: string
  roleIds: number[]
}

const EMPTY_FORM: UserFormData = {
  username: '',
  password: '',
  realName: '',
  email: '',
  phone: '',
  roleIds: [],
}

// ---------------------------------------------------------------------------
// Main page component
// ---------------------------------------------------------------------------

export default function UsersPage() {
  // ---- data state ----
  const [pageData, setPageData] = useState<PageResult<AdminUser>>({
    total: 0,
    pageNum: 1,
    pageSize: 10,
    list: [],
    pages: 0,
  })
  const [roles, setRoles] = useState<AdminRole[]>([])
  const [loading, setLoading] = useState(true)

  // ---- filter state ----
  const [search, setSearch] = useState('')
  const [filterStatus, setFilterStatus] = useState('all')
  const [pageNum, setPageNum] = useState(1)
  const pageSize = 10

  // ---- create/edit dialog ----
  const [dialogOpen, setDialogOpen] = useState(false)
  const [editingUser, setEditingUser] = useState<AdminUser | null>(null)
  const [form, setForm] = useState<UserFormData>({ ...EMPTY_FORM })
  const [saving, setSaving] = useState(false)

  // ---- reset password dialog ----
  const [resetTarget, setResetTarget] = useState<AdminUser | null>(null)
  const [newPassword, setNewPassword] = useState('')
  const [resetting, setResetting] = useState(false)

  // ---- delete confirm ----
  const [deleteTarget, setDeleteTarget] = useState<AdminUser | null>(null)
  const [deleting, setDeleting] = useState(false)

  // ---- data fetching ----
  const fetchUsers = async (page?: number) => {
    try {
      const params: Record<string, any> = {
        pageNum: page ?? pageNum,
        pageSize,
      }
      if (search.trim()) params.keyword = search.trim()
      if (filterStatus !== 'all') params.status = Number(filterStatus)
      const data = await adminUserApi.getPage(params)
      setPageData(data)
    } catch {
      toast.error('加载用户失败')
    }
  }

  const fetchRoles = async () => {
    try {
      const data = await adminRoleApi.getAll()
      setRoles(data)
    } catch {
      toast.error('加载角色失败')
    }
  }

  useEffect(() => {
    const init = async () => {
      setLoading(true)
      await Promise.all([fetchUsers(1), fetchRoles()])
      setLoading(false)
    }
    init()
  }, [])

  // Re-fetch when filters or page change
  useEffect(() => {
    fetchUsers()
  }, [pageNum, filterStatus])

  const handleSearch = () => {
    setPageNum(1)
    fetchUsers(1)
  }

  // ---- CRUD helpers ----
  const openCreateDialog = () => {
    setEditingUser(null)
    setForm({ ...EMPTY_FORM })
    setDialogOpen(true)
  }

  const openEditDialog = (user: AdminUser) => {
    setEditingUser(user)
    setForm({
      username: user.username,
      password: '',
      realName: user.realName || '',
      email: user.email || '',
      phone: user.phone || '',
      roleIds: user.roleIds || [],
    })
    setDialogOpen(true)
  }

  const handleSave = async () => {
    if (!editingUser && !form.username.trim()) {
      toast.error('用户名为必填项')
      return
    }
    if (!editingUser && !form.password.trim()) {
      toast.error('密码为必填项')
      return
    }

    setSaving(true)
    try {
      if (editingUser) {
        const payload: UpdateAdminUserRequest = {
          realName: form.realName || undefined,
          email: form.email || undefined,
          phone: form.phone || undefined,
          roleIds: form.roleIds,
        }
        await adminUserApi.update(editingUser.id, payload)
        toast.success('用户已更新')
      } else {
        const payload: CreateAdminUserRequest = {
          username: form.username,
          password: form.password,
          realName: form.realName || undefined,
          email: form.email || undefined,
          phone: form.phone || undefined,
          roleIds: form.roleIds,
        }
        await adminUserApi.create(payload)
        toast.success('用户已创建')
      }
      setDialogOpen(false)
      await fetchUsers()
    } catch {
      toast.error(editingUser ? '更新用户失败' : '创建用户失败')
    } finally {
      setSaving(false)
    }
  }

  const handleToggleStatus = async (user: AdminUser) => {
    try {
      await adminUserApi.toggleStatus(user.id)
      toast.success(user.status === 1 ? '用户已禁用' : '用户已启用')
      await fetchUsers()
    } catch {
      toast.error('更新状态失败')
    }
  }

  const handleDelete = async () => {
    if (!deleteTarget) return
    setDeleting(true)
    try {
      await adminUserApi.delete(deleteTarget.id)
      toast.success('用户已删除')
      setDeleteTarget(null)
      await fetchUsers()
    } catch {
      toast.error('删除用户失败')
    } finally {
      setDeleting(false)
    }
  }

  const handleResetPassword = async () => {
    if (!resetTarget) return
    if (!newPassword.trim()) {
      toast.error('请输入新密码')
      return
    }
    setResetting(true)
    try {
      await adminUserApi.resetPassword(resetTarget.id, newPassword)
      toast.success('密码已重置')
      setResetTarget(null)
      setNewPassword('')
    } catch {
      toast.error('重置密码失败')
    } finally {
      setResetting(false)
    }
  }

  const toggleRole = (roleId: number) => {
    setForm((prev) => ({
      ...prev,
      roleIds: prev.roleIds.includes(roleId)
        ? prev.roleIds.filter((id) => id !== roleId)
        : [...prev.roleIds, roleId],
    }))
  }

  const updateForm = <K extends keyof UserFormData>(key: K, value: UserFormData[K]) =>
    setForm((prev) => ({ ...prev, [key]: value }))

  // ---- pagination helpers ----
  const totalPages = pageData.pages
  const canPrev = pageNum > 1
  const canNext = pageNum < totalPages

  // ====================================================================
  // Render
  // ====================================================================

  return (
    <div className="space-y-6">
      {/* ---- Page header ---- */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold tracking-tight">用户管理</h1>
          <p className="text-sm text-muted-foreground mt-1">
            管理管理员用户，分配角色和控制访问权限。
          </p>
        </div>
        <Button onClick={openCreateDialog} className="gap-2 shrink-0">
          <Plus className="w-4 h-4" />
          添加用户
        </Button>
      </div>

      {/* ---- Filter bar ---- */}
      <div className="flex flex-col sm:flex-row items-stretch sm:items-center gap-3">
        {/* Search */}
        <div className="relative flex-1 min-w-0">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-muted-foreground pointer-events-none" />
          <Input
            placeholder="按用户名、姓名、邮箱搜索..."
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            onKeyDown={(e) => e.key === 'Enter' && handleSearch()}
            className="pl-9 bg-white/[0.03] border-white/[0.06] focus:border-white/[0.12] h-10"
          />
        </div>

        {/* Status filter */}
        <Select value={filterStatus} onValueChange={(v) => { setFilterStatus(v); setPageNum(1) }}>
          <SelectTrigger className="w-full sm:w-[150px] bg-white/[0.03] border-white/[0.06] h-10">
            <SelectValue placeholder="所有状态" />
          </SelectTrigger>
          <SelectContent>
            {STATUS_OPTIONS.map((opt) => (
              <SelectItem key={opt.value} value={opt.value}>
                {opt.label}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>

        {/* Search button */}
        <Button
          variant="outline"
          className="h-10 px-4 border-white/[0.06] bg-white/[0.03] hover:bg-white/[0.06]"
          onClick={handleSearch}
        >
          <Search className="w-4 h-4 mr-2" />
          搜索
        </Button>
      </div>

      {/* ---- Content area ---- */}
      {loading ? (
        <div className="flex flex-col items-center justify-center py-32 gap-4">
          <Loader2 className="w-8 h-8 text-primary animate-spin" />
          <p className="text-sm text-muted-foreground">加载用户中...</p>
        </div>
      ) : pageData.list.length === 0 ? (
        <motion.div
          initial={{ opacity: 0, y: 12 }}
          animate={{ opacity: 1, y: 0 }}
          className="flex flex-col items-center justify-center py-32 gap-4"
        >
          <div className="w-16 h-16 rounded-2xl bg-white/[0.04] border border-white/[0.06] flex items-center justify-center">
            <Users className="w-8 h-8 text-muted-foreground" />
          </div>
          <div className="text-center">
            <p className="text-sm font-medium text-foreground">
              {pageData.total === 0 && !search ? '暂无用户' : '没有匹配的用户'}
            </p>
            <p className="text-xs text-muted-foreground mt-1">
              {pageData.total === 0 && !search
                ? '添加您的第一个管理员用户。'
                : '请尝试调整搜索条件。'}
            </p>
          </div>
          {pageData.total === 0 && !search && (
            <Button onClick={openCreateDialog} className="gap-2 mt-2">
              <Plus className="w-4 h-4" />
              添加用户
            </Button>
          )}
        </motion.div>
      ) : (
        <>
          {/* Result count */}
          <div className="flex items-center gap-2 text-xs text-muted-foreground">
            <span className="tabular-nums">{pageData.total}</span>
            <span>个用户</span>
          </div>

          {/* User list */}
          <div className="space-y-2">
            {pageData.list.map((user, i) => (
              <UserRow
                key={user.id}
                user={user}
                index={i}
                onEdit={openEditDialog}
                onToggle={handleToggleStatus}
                onDelete={setDeleteTarget}
                onResetPassword={(u) => {
                  setResetTarget(u)
                  setNewPassword('')
                }}
              />
            ))}
          </div>

          {/* Pagination */}
          {totalPages > 1 && (
            <div className="flex items-center justify-between pt-2">
              <p className="text-xs text-muted-foreground tabular-nums">
                第 {pageNum} 页 / 共 {totalPages} 页
              </p>
              <div className="flex items-center gap-2">
                <Button
                  variant="outline"
                  size="sm"
                  className="h-8 px-3 border-white/[0.06] bg-white/[0.03] hover:bg-white/[0.06]"
                  disabled={!canPrev}
                  onClick={() => setPageNum((p) => p - 1)}
                >
                  <ChevronLeft className="w-4 h-4 mr-1" />
                  上一页
                </Button>
                <Button
                  variant="outline"
                  size="sm"
                  className="h-8 px-3 border-white/[0.06] bg-white/[0.03] hover:bg-white/[0.06]"
                  disabled={!canNext}
                  onClick={() => setPageNum((p) => p + 1)}
                >
                  下一页
                  <ChevronRight className="w-4 h-4 ml-1" />
                </Button>
              </div>
            </div>
          )}
        </>
      )}

      {/* ================================================================= */}
      {/* Create / Edit Dialog                                               */}
      {/* ================================================================= */}
      <Dialog open={dialogOpen} onOpenChange={setDialogOpen}>
        <DialogContent className="sm:max-w-[520px] bg-[hsl(240_10%_5%)] border-white/[0.08] max-h-[90vh] overflow-y-auto">
          <DialogHeader>
            <DialogTitle>
              {editingUser ? '编辑用户' : '创建用户'}
            </DialogTitle>
            <DialogDescription>
              {editingUser
                ? '更新下方的用户信息。'
                : '向系统添加新的管理员用户。'}
            </DialogDescription>
          </DialogHeader>

          <div className="space-y-5 py-2">
            {/* Username (only for create) */}
            {!editingUser && (
              <div className="space-y-2">
                <Label htmlFor="user-username">
                  用户名 <span className="text-red-400">*</span>
                </Label>
                <Input
                  id="user-username"
                  placeholder="请输入用户名"
                  value={form.username}
                  onChange={(e) => updateForm('username', e.target.value)}
                  className="font-mono text-sm bg-white/[0.03] border-white/[0.06]"
                />
              </div>
            )}

            {/* Password (only for create) */}
            {!editingUser && (
              <div className="space-y-2">
                <Label htmlFor="user-password">
                  密码 <span className="text-red-400">*</span>
                </Label>
                <Input
                  id="user-password"
                  type="password"
                  placeholder="请输入密码"
                  value={form.password}
                  onChange={(e) => updateForm('password', e.target.value)}
                  className="bg-white/[0.03] border-white/[0.06]"
                />
              </div>
            )}

            {/* Real name */}
            <div className="space-y-2">
              <Label htmlFor="user-realname">真实姓名</Label>
              <Input
                id="user-realname"
                placeholder="请输入真实姓名"
                value={form.realName}
                onChange={(e) => updateForm('realName', e.target.value)}
                className="bg-white/[0.03] border-white/[0.06]"
              />
            </div>

            {/* Email + Phone */}
            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-2">
                <Label htmlFor="user-email">邮箱</Label>
                <Input
                  id="user-email"
                  type="email"
                  placeholder="user@example.com"
                  value={form.email}
                  onChange={(e) => updateForm('email', e.target.value)}
                  className="bg-white/[0.03] border-white/[0.06]"
                />
              </div>
              <div className="space-y-2">
                <Label htmlFor="user-phone">手机号</Label>
                <Input
                  id="user-phone"
                  placeholder="请输入手机号"
                  value={form.phone}
                  onChange={(e) => updateForm('phone', e.target.value)}
                  className="bg-white/[0.03] border-white/[0.06]"
                />
              </div>
            </div>

            {/* Role selection (multi-checkbox) */}
            <div className="space-y-3">
              <Label>角色</Label>
              <div className="rounded-lg border border-white/[0.06] bg-white/[0.02] p-3 space-y-2 max-h-[200px] overflow-y-auto">
                {roles.length === 0 ? (
                  <p className="text-xs text-muted-foreground text-center py-2">
                    暂无可用角色
                  </p>
                ) : (
                  roles.map((role) => {
                    const checked = form.roleIds.includes(role.id)
                    return (
                      <label
                        key={role.id}
                        className="flex items-center gap-3 rounded-md px-3 py-2 cursor-pointer hover:bg-white/[0.04] transition-colors"
                      >
                        <div
                          className={`shrink-0 w-4 h-4 rounded border flex items-center justify-center transition-colors ${
                            checked
                              ? 'bg-primary border-primary'
                              : 'border-white/[0.15] bg-white/[0.03]'
                          }`}
                          onClick={(e) => {
                            e.preventDefault()
                            toggleRole(role.id)
                          }}
                        >
                          {checked && (
                            <svg
                              className="w-3 h-3 text-primary-foreground"
                              fill="none"
                              viewBox="0 0 24 24"
                              stroke="currentColor"
                              strokeWidth={3}
                            >
                              <path
                                strokeLinecap="round"
                                strokeLinejoin="round"
                                d="M5 13l4 4L19 7"
                              />
                            </svg>
                          )}
                        </div>
                        <input
                          type="checkbox"
                          className="sr-only"
                          checked={checked}
                          onChange={() => toggleRole(role.id)}
                        />
                        <div className="min-w-0">
                          <p className="text-sm font-medium text-foreground">
                            {role.roleName}
                          </p>
                          <p className="text-xs text-muted-foreground font-mono">
                            {role.roleCode}
                          </p>
                        </div>
                        {role.status !== 1 && (
                          <Badge
                            variant="outline"
                            className="text-[10px] ml-auto border-zinc-500/30 text-zinc-500"
                          >
                            已禁用
                          </Badge>
                        )}
                      </label>
                    )
                  })
                )}
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
              {editingUser ? '更新' : '创建'}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* ================================================================= */}
      {/* Reset Password Dialog                                              */}
      {/* ================================================================= */}
      <Dialog
        open={!!resetTarget}
        onOpenChange={(open) => {
          if (!open) {
            setResetTarget(null)
            setNewPassword('')
          }
        }}
      >
        <DialogContent className="sm:max-w-[400px] bg-[hsl(240_10%_5%)] border-white/[0.08]">
          <DialogHeader>
            <DialogTitle>重置密码</DialogTitle>
            <DialogDescription>
              重置{' '}
              <span className="font-semibold text-foreground">
                {resetTarget?.realName || resetTarget?.username}
              </span>
              {' '}的密码。
            </DialogDescription>
          </DialogHeader>

          <div className="space-y-4 py-2">
            <div className="space-y-2">
              <Label htmlFor="new-password">
                新密码 <span className="text-red-400">*</span>
              </Label>
              <Input
                id="new-password"
                type="password"
                placeholder="请输入新密码"
                value={newPassword}
                onChange={(e) => setNewPassword(e.target.value)}
                className="bg-white/[0.03] border-white/[0.06]"
              />
            </div>
          </div>

          <DialogFooter>
            <Button
              variant="ghost"
              onClick={() => {
                setResetTarget(null)
                setNewPassword('')
              }}
              disabled={resetting}
            >
              取消
            </Button>
            <Button
              onClick={handleResetPassword}
              disabled={resetting}
              className="gap-2"
            >
              {resetting && <Loader2 className="w-4 h-4 animate-spin" />}
              重置密码
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
            <AlertDialogTitle>删除用户</AlertDialogTitle>
            <AlertDialogDescription>
              确定要删除{' '}
              <span className="font-semibold text-foreground">
                {deleteTarget?.realName || deleteTarget?.username}
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
    </div>
  )
}
