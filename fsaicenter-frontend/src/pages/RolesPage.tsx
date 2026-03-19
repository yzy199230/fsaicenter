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
import { adminRoleApi, permissionApi } from '@/api/role'
import type {
  AdminRole,
  CreateAdminRoleRequest,
  UpdateAdminRoleRequest,
  AdminPermission,
} from '@/types/user'
import { toast } from 'sonner'
import {
  Plus,
  Search,
  Shield,
  Pencil,
  Trash2,
  Power,
  Lock,
  Loader2,
  ChevronRight,
  Check,
} from 'lucide-react'

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

function countPermissions(perms: AdminPermission[]): number {
  let count = 0
  for (const p of perms) {
    count += 1
    if (p.children?.length) {
      count += countPermissions(p.children)
    }
  }
  return count
}

function flattenPermissionIds(perms: AdminPermission[]): number[] {
  const ids: number[] = []
  for (const p of perms) {
    ids.push(p.id)
    if (p.children?.length) {
      ids.push(...flattenPermissionIds(p.children))
    }
  }
  return ids
}

function getChildIds(perm: AdminPermission): number[] {
  const ids: number[] = []
  if (perm.children?.length) {
    for (const child of perm.children) {
      ids.push(child.id)
      ids.push(...getChildIds(child))
    }
  }
  return ids
}

// ---------------------------------------------------------------------------
// Role card component
// ---------------------------------------------------------------------------

function RoleCard({
  role,
  index,
  onEdit,
  onToggle,
  onDelete,
  onPermissions,
}: {
  role: AdminRole
  index: number
  onEdit: (r: AdminRole) => void
  onToggle: (r: AdminRole) => void
  onDelete: (r: AdminRole) => void
  onPermissions: (r: AdminRole) => void
}) {
  const isActive = role.status === 1

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
        <div className="absolute inset-x-0 top-0 h-[1px] bg-gradient-to-r from-blue-500/20 to-purple-500/20 opacity-60 group-hover:opacity-100 transition-opacity" />

        <CardContent className="p-5 space-y-4">
          {/* ---- Header row ---- */}
          <div className="flex items-start justify-between gap-3">
            <div className="flex items-start gap-3 min-w-0">
              {/* Shield icon */}
              <div className="shrink-0 w-10 h-10 rounded-xl bg-blue-500/15 flex items-center justify-center ring-1 ring-inset ring-white/[0.08]">
                <Shield className="w-5 h-5 text-blue-400" />
              </div>

              <div className="min-w-0">
                <h3 className="text-sm font-semibold text-foreground truncate leading-tight">
                  {role.roleName}
                </h3>
                <p className="text-xs font-mono text-muted-foreground truncate mt-0.5">
                  {role.roleCode}
                </p>
              </div>
            </div>

            {/* Status dot */}
            <div className="flex items-center gap-1.5 shrink-0 pt-1">
              <div className={isActive ? 'dot-online' : 'dot-offline'} />
              <span
                className={`text-[10px] font-medium ${isActive ? 'text-emerald-400' : 'text-zinc-500'}`}
              >
                {isActive ? '正常' : '已禁用'}
              </span>
            </div>
          </div>

          {/* ---- Description ---- */}
          {role.description && (
            <p className="text-xs text-muted-foreground line-clamp-2 leading-relaxed">
              {role.description}
            </p>
          )}

          {/* ---- Badges row ---- */}
          <div className="flex flex-wrap items-center gap-1.5">
            <Badge
              className="text-[10px] px-2 py-0 h-5 bg-purple-500/10 text-purple-400 border border-purple-500/20 font-medium gap-1"
            >
              <Lock className="w-3 h-3" />
              {role.permissionIds?.length ?? 0} 个权限
            </Badge>
            <Badge
              variant="outline"
              className="text-[10px] px-2 py-0 h-5 text-muted-foreground border-white/[0.08] tabular-nums"
            >
              排序：{role.sortOrder}
            </Badge>
          </div>

          {/* ---- Actions ---- */}
          <div className="flex items-center gap-1.5 pt-1">
            <Button
              variant="ghost"
              size="sm"
              className="h-8 px-2.5 text-xs text-muted-foreground hover:text-foreground hover:bg-white/[0.06]"
              onClick={() => onEdit(role)}
            >
              <Pencil className="w-3.5 h-3.5 mr-1.5" />
              编辑
            </Button>
            <Button
              variant="ghost"
              size="sm"
              className="h-8 px-2.5 text-xs text-muted-foreground hover:text-purple-400 hover:bg-purple-500/10"
              onClick={() => onPermissions(role)}
            >
              <Lock className="w-3.5 h-3.5 mr-1.5" />
              权限
            </Button>
            <Button
              variant="ghost"
              size="sm"
              className={`h-8 px-2.5 text-xs hover:bg-white/[0.06] ${
                isActive
                  ? 'text-emerald-400 hover:text-emerald-300'
                  : 'text-muted-foreground hover:text-foreground'
              }`}
              onClick={() => onToggle(role)}
            >
              <Power className="w-3.5 h-3.5 mr-1.5" />
              {isActive ? '禁用' : '启用'}
            </Button>
            <div className="flex-1" />
            <Button
              variant="ghost"
              size="sm"
              className="h-8 px-2.5 text-xs text-muted-foreground hover:text-red-400 hover:bg-red-500/10"
              onClick={() => onDelete(role)}
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
// Permission tree checkbox component
// ---------------------------------------------------------------------------

function PermissionNode({
  permission,
  selectedIds,
  onToggle,
  depth = 0,
}: {
  permission: AdminPermission
  selectedIds: Set<number>
  onToggle: (id: number, childIds: number[]) => void
  depth?: number
}) {
  const [expanded, setExpanded] = useState(true)
  const hasChildren = permission.children && permission.children.length > 0
  const isChecked = selectedIds.has(permission.id)

  // Check if all children are selected
  const allChildIds = getChildIds(permission)
  const allChildrenSelected = allChildIds.length > 0 && allChildIds.every((id) => selectedIds.has(id))
  const someChildrenSelected = allChildIds.length > 0 && allChildIds.some((id) => selectedIds.has(id))
  const isIndeterminate = someChildrenSelected && !allChildrenSelected && !isChecked

  return (
    <div>
      <div
        className={`flex items-center gap-2 rounded-md px-2 py-1.5 hover:bg-white/[0.04] transition-colors cursor-pointer`}
        style={{ paddingLeft: `${depth * 20 + 8}px` }}
      >
        {/* Expand/collapse */}
        {hasChildren ? (
          <button
            type="button"
            className="shrink-0 w-5 h-5 flex items-center justify-center rounded hover:bg-white/[0.06] transition-colors"
            onClick={() => setExpanded(!expanded)}
          >
            <ChevronRight
              className={`w-3.5 h-3.5 text-muted-foreground transition-transform duration-200 ${
                expanded ? 'rotate-90' : ''
              }`}
            />
          </button>
        ) : (
          <div className="shrink-0 w-5 h-5" />
        )}

        {/* Checkbox */}
        <div
          className={`shrink-0 w-4 h-4 rounded border flex items-center justify-center transition-colors cursor-pointer ${
            isChecked
              ? 'bg-primary border-primary'
              : isIndeterminate
                ? 'bg-primary/50 border-primary/70'
                : 'border-white/[0.15] bg-white/[0.03]'
          }`}
          onClick={() => onToggle(permission.id, allChildIds)}
        >
          {isChecked && (
            <Check className="w-3 h-3 text-primary-foreground" />
          )}
          {isIndeterminate && !isChecked && (
            <div className="w-2 h-0.5 bg-primary-foreground rounded-full" />
          )}
        </div>

        {/* Label */}
        <div
          className="flex-1 min-w-0"
          onClick={() => onToggle(permission.id, allChildIds)}
        >
          <span className="text-sm text-foreground">
            {permission.permissionName}
          </span>
          <span className="text-xs text-muted-foreground font-mono ml-2">
            {permission.permissionCode}
          </span>
        </div>

        {/* Type badge */}
        <Badge
          variant="outline"
          className="text-[9px] px-1.5 py-0 h-4 text-muted-foreground border-white/[0.08] shrink-0"
        >
          {permission.permissionType}
        </Badge>
      </div>

      {/* Children */}
      {hasChildren && expanded && (
        <div>
          {permission.children!.map((child) => (
            <PermissionNode
              key={child.id}
              permission={child}
              selectedIds={selectedIds}
              onToggle={onToggle}
              depth={depth + 1}
            />
          ))}
        </div>
      )}
    </div>
  )
}

// ---------------------------------------------------------------------------
// Form types
// ---------------------------------------------------------------------------

interface RoleFormData {
  roleCode: string
  roleName: string
  description: string
  sortOrder: number
}

const EMPTY_FORM: RoleFormData = {
  roleCode: '',
  roleName: '',
  description: '',
  sortOrder: 0,
}

// ---------------------------------------------------------------------------
// Main page component
// ---------------------------------------------------------------------------

export default function RolesPage() {
  // ---- data state ----
  const [roles, setRoles] = useState<AdminRole[]>([])
  const [loading, setLoading] = useState(true)

  // ---- filter state ----
  const [search, setSearch] = useState('')

  // ---- create/edit dialog ----
  const [dialogOpen, setDialogOpen] = useState(false)
  const [editingRole, setEditingRole] = useState<AdminRole | null>(null)
  const [form, setForm] = useState<RoleFormData>({ ...EMPTY_FORM })
  const [saving, setSaving] = useState(false)

  // ---- permission dialog ----
  const [permDialogOpen, setPermDialogOpen] = useState(false)
  const [permRole, setPermRole] = useState<AdminRole | null>(null)
  const [permTree, setPermTree] = useState<AdminPermission[]>([])
  const [selectedPermIds, setSelectedPermIds] = useState<Set<number>>(new Set())
  const [permLoading, setPermLoading] = useState(false)
  const [permSaving, setPermSaving] = useState(false)

  // ---- delete confirm ----
  const [deleteTarget, setDeleteTarget] = useState<AdminRole | null>(null)
  const [deleting, setDeleting] = useState(false)

  // ---- data fetching ----
  const fetchRoles = async () => {
    try {
      const data = await adminRoleApi.getList()
      setRoles(data)
    } catch {
      toast.error('加载角色失败')
    }
  }

  useEffect(() => {
    const init = async () => {
      setLoading(true)
      await fetchRoles()
      setLoading(false)
    }
    init()
  }, [])

  // ---- filtering ----
  const filtered = roles.filter((r) => {
    if (!search) return true
    const s = search.toLowerCase()
    return (
      r.roleName.toLowerCase().includes(s) ||
      r.roleCode.toLowerCase().includes(s) ||
      (r.description?.toLowerCase().includes(s) ?? false)
    )
  })

  // ---- CRUD helpers ----
  const openCreateDialog = () => {
    setEditingRole(null)
    setForm({ ...EMPTY_FORM })
    setDialogOpen(true)
  }

  const openEditDialog = (role: AdminRole) => {
    setEditingRole(role)
    setForm({
      roleCode: role.roleCode,
      roleName: role.roleName,
      description: role.description || '',
      sortOrder: role.sortOrder,
    })
    setDialogOpen(true)
  }

  const handleSave = async () => {
    if (!form.roleCode.trim() || !form.roleName.trim()) {
      toast.error('角色代码和名称为必填项')
      return
    }

    setSaving(true)
    try {
      if (editingRole) {
        const payload: UpdateAdminRoleRequest = {
          roleName: form.roleName,
          description: form.description || undefined,
          sortOrder: form.sortOrder,
        }
        await adminRoleApi.update(editingRole.id, payload)
        toast.success('角色已更新')
      } else {
        const payload: CreateAdminRoleRequest = {
          roleCode: form.roleCode,
          roleName: form.roleName,
          description: form.description || undefined,
          sortOrder: form.sortOrder,
        }
        await adminRoleApi.create(payload)
        toast.success('角色已创建')
      }
      setDialogOpen(false)
      await fetchRoles()
    } catch {
      toast.error(editingRole ? '更新角色失败' : '创建角色失败')
    } finally {
      setSaving(false)
    }
  }

  const handleToggleStatus = async (role: AdminRole) => {
    try {
      await adminRoleApi.toggleStatus(role.id)
      toast.success(role.status === 1 ? '角色已禁用' : '角色已启用')
      await fetchRoles()
    } catch {
      toast.error('更新状态失败')
    }
  }

  const handleDelete = async () => {
    if (!deleteTarget) return
    setDeleting(true)
    try {
      await adminRoleApi.delete(deleteTarget.id)
      toast.success('角色已删除')
      setDeleteTarget(null)
      await fetchRoles()
    } catch {
      toast.error('删除角色失败')
    } finally {
      setDeleting(false)
    }
  }

  // ---- Permission dialog ----
  const openPermDialog = async (role: AdminRole) => {
    setPermRole(role)
    setPermDialogOpen(true)
    setPermLoading(true)
    try {
      const [tree, rolePerms] = await Promise.all([
        permissionApi.getTree(),
        permissionApi.getByRole(role.id),
      ])
      setPermTree(tree)
      setSelectedPermIds(new Set(rolePerms.map((p) => p.id)))
    } catch {
      toast.error('加载权限失败')
    } finally {
      setPermLoading(false)
    }
  }

  const handleTogglePerm = (id: number, childIds: number[]) => {
    setSelectedPermIds((prev) => {
      const next = new Set(prev)
      if (next.has(id)) {
        // Uncheck this + all children
        next.delete(id)
        childIds.forEach((cid) => next.delete(cid))
      } else {
        // Check this + all children
        next.add(id)
        childIds.forEach((cid) => next.add(cid))
      }
      return next
    })
  }

  const handleSelectAllPerms = () => {
    const allIds = flattenPermissionIds(permTree)
    setSelectedPermIds(new Set(allIds))
  }

  const handleDeselectAllPerms = () => {
    setSelectedPermIds(new Set())
  }

  const handleSavePermissions = async () => {
    if (!permRole) return
    setPermSaving(true)
    try {
      await adminRoleApi.assignPermissions(permRole.id, Array.from(selectedPermIds))
      toast.success('权限已更新')
      setPermDialogOpen(false)
      await fetchRoles()
    } catch {
      toast.error('更新权限失败')
    } finally {
      setPermSaving(false)
    }
  }

  const updateForm = <K extends keyof RoleFormData>(key: K, value: RoleFormData[K]) =>
    setForm((prev) => ({ ...prev, [key]: value }))

  const totalPermCount = countPermissions(permTree)

  // ====================================================================
  // Render
  // ====================================================================

  return (
    <div className="space-y-6">
      {/* ---- Page header ---- */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold tracking-tight">角色管理</h1>
          <p className="text-sm text-muted-foreground mt-1">
            管理角色，配置权限和控制访问级别。
          </p>
        </div>
        <Button onClick={openCreateDialog} className="gap-2 shrink-0">
          <Plus className="w-4 h-4" />
          添加角色
        </Button>
      </div>

      {/* ---- Search bar ---- */}
      <div className="flex items-center gap-3">
        <div className="relative flex-1 min-w-0 max-w-md">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-muted-foreground pointer-events-none" />
          <Input
            placeholder="按角色名称或代码搜索..."
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
          <p className="text-sm text-muted-foreground">加载角色中...</p>
        </div>
      ) : filtered.length === 0 ? (
        <motion.div
          initial={{ opacity: 0, y: 12 }}
          animate={{ opacity: 1, y: 0 }}
          className="flex flex-col items-center justify-center py-32 gap-4"
        >
          <div className="w-16 h-16 rounded-2xl bg-white/[0.04] border border-white/[0.06] flex items-center justify-center">
            <Shield className="w-8 h-8 text-muted-foreground" />
          </div>
          <div className="text-center">
            <p className="text-sm font-medium text-foreground">
              {roles.length === 0 ? '暂无角色' : '没有匹配的角色'}
            </p>
            <p className="text-xs text-muted-foreground mt-1">
              {roles.length === 0
                ? '添加您的第一个角色以开始使用。'
                : '请尝试调整搜索条件。'}
            </p>
          </div>
          {roles.length === 0 && (
            <Button onClick={openCreateDialog} className="gap-2 mt-2">
              <Plus className="w-4 h-4" />
              添加角色
            </Button>
          )}
        </motion.div>
      ) : (
        <>
          {/* Result count */}
          <div className="flex items-center gap-2 text-xs text-muted-foreground">
            <span className="tabular-nums">{filtered.length}</span>
            <span>
              个角色
              {filtered.length !== roles.length && `（共 ${roles.length} 个）`}
            </span>
          </div>

          {/* Grid */}
          <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-4">
            {filtered.map((role, i) => (
              <RoleCard
                key={role.id}
                role={role}
                index={i}
                onEdit={openEditDialog}
                onToggle={handleToggleStatus}
                onDelete={setDeleteTarget}
                onPermissions={openPermDialog}
              />
            ))}
          </div>
        </>
      )}

      {/* ================================================================= */}
      {/* Create / Edit Dialog                                               */}
      {/* ================================================================= */}
      <Dialog open={dialogOpen} onOpenChange={setDialogOpen}>
        <DialogContent className="sm:max-w-[480px] bg-[hsl(240_10%_5%)] border-white/[0.08] max-h-[90vh] overflow-y-auto">
          <DialogHeader>
            <DialogTitle>
              {editingRole ? '编辑角色' : '创建角色'}
            </DialogTitle>
            <DialogDescription>
              {editingRole
                ? '更新下方的角色信息。'
                : '向系统添加新角色。'}
            </DialogDescription>
          </DialogHeader>

          <div className="space-y-5 py-2">
            {/* Role code */}
            <div className="space-y-2">
              <Label htmlFor="role-code">
                角色代码 <span className="text-red-400">*</span>
              </Label>
              <Input
                id="role-code"
                placeholder="例如：ROLE_ADMIN"
                value={form.roleCode}
                onChange={(e) => updateForm('roleCode', e.target.value)}
                disabled={!!editingRole}
                className="font-mono text-sm bg-white/[0.03] border-white/[0.06]"
              />
            </div>

            {/* Role name */}
            <div className="space-y-2">
              <Label htmlFor="role-name">
                角色名称 <span className="text-red-400">*</span>
              </Label>
              <Input
                id="role-name"
                placeholder="例如：管理员"
                value={form.roleName}
                onChange={(e) => updateForm('roleName', e.target.value)}
                className="bg-white/[0.03] border-white/[0.06]"
              />
            </div>

            {/* Description */}
            <div className="space-y-2">
              <Label htmlFor="role-desc">描述</Label>
              <Textarea
                id="role-desc"
                placeholder="角色描述（可选）..."
                rows={3}
                value={form.description}
                onChange={(e) => updateForm('description', e.target.value)}
                className="bg-white/[0.03] border-white/[0.06] resize-none"
              />
            </div>

            {/* Sort order */}
            <div className="space-y-2">
              <Label htmlFor="role-sort">排序</Label>
              <Input
                id="role-sort"
                type="number"
                min={0}
                value={form.sortOrder}
                onChange={(e) => updateForm('sortOrder', Number(e.target.value) || 0)}
                className="bg-white/[0.03] border-white/[0.06] w-32"
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
              {editingRole ? '更新' : '创建'}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* ================================================================= */}
      {/* Permission Assignment Dialog                                       */}
      {/* ================================================================= */}
      <Dialog
        open={permDialogOpen}
        onOpenChange={(open) => {
          if (!open) {
            setPermDialogOpen(false)
            setPermRole(null)
          }
        }}
      >
        <DialogContent className="sm:max-w-[580px] bg-[hsl(240_10%_5%)] border-white/[0.08] max-h-[90vh] flex flex-col">
          <DialogHeader>
            <DialogTitle>
              分配权限
            </DialogTitle>
            <DialogDescription>
              为{' '}
              <span className="font-semibold text-foreground">
                {permRole?.roleName}
              </span>
              {' '}配置权限。
            </DialogDescription>
          </DialogHeader>

          {permLoading ? (
            <div className="flex flex-col items-center justify-center py-16 gap-3">
              <Loader2 className="w-6 h-6 text-primary animate-spin" />
              <p className="text-sm text-muted-foreground">加载权限中...</p>
            </div>
          ) : (
            <>
              {/* Select all / Deselect all */}
              <div className="flex items-center justify-between px-1 pb-2 border-b border-white/[0.06]">
                <span className="text-xs text-muted-foreground tabular-nums">
                  {selectedPermIds.size} / {totalPermCount} 已选择
                </span>
                <div className="flex items-center gap-2">
                  <Button
                    variant="ghost"
                    size="sm"
                    className="h-7 px-2.5 text-xs text-muted-foreground hover:text-foreground"
                    onClick={handleSelectAllPerms}
                  >
                    全选
                  </Button>
                  <Button
                    variant="ghost"
                    size="sm"
                    className="h-7 px-2.5 text-xs text-muted-foreground hover:text-foreground"
                    onClick={handleDeselectAllPerms}
                  >
                    取消全选
                  </Button>
                </div>
              </div>

              {/* Permission tree */}
              <div className="flex-1 overflow-y-auto min-h-0 max-h-[400px] rounded-lg border border-white/[0.06] bg-white/[0.02] p-2">
                {permTree.length === 0 ? (
                  <div className="flex flex-col items-center justify-center py-12 gap-2">
                    <Lock className="w-6 h-6 text-muted-foreground" />
                    <p className="text-xs text-muted-foreground">暂无可用权限</p>
                  </div>
                ) : (
                  permTree.map((perm) => (
                    <PermissionNode
                      key={perm.id}
                      permission={perm}
                      selectedIds={selectedPermIds}
                      onToggle={handleTogglePerm}
                    />
                  ))
                )}
              </div>
            </>
          )}

          <DialogFooter>
            <Button
              variant="ghost"
              onClick={() => {
                setPermDialogOpen(false)
                setPermRole(null)
              }}
              disabled={permSaving}
            >
              取消
            </Button>
            <Button
              onClick={handleSavePermissions}
              disabled={permSaving || permLoading}
              className="gap-2"
            >
              {permSaving && <Loader2 className="w-4 h-4 animate-spin" />}
              保存权限
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
            <AlertDialogTitle>删除角色</AlertDialogTitle>
            <AlertDialogDescription>
              确定要删除{' '}
              <span className="font-semibold text-foreground">
                {deleteTarget?.roleName}
              </span>
              {' '}吗？此操作无法撤销。分配了该角色的用户可能会失去相应权限。
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
