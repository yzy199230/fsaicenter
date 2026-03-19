import { useState } from 'react'
import { NavLink, useLocation } from 'react-router-dom'
import { cn } from '@/lib/utils'
import { ScrollArea } from '@/components/ui/scroll-area'
import {
  LayoutDashboard,
  Bot,
  Server,
  Key,
  Receipt,
  ScrollText,
  Users,
  Shield,
  ChevronLeft,
  Sparkles,
} from 'lucide-react'

const navItems = [
  { path: '/dashboard', label: '仪表盘', icon: LayoutDashboard },
  { path: '/models', label: '模型管理', icon: Bot },
  { path: '/providers', label: '提供商', icon: Server },
  { path: '/apikeys', label: 'API Key', icon: Key },
  { path: '/billing', label: '计费记录', icon: Receipt },
  { path: '/logs', label: '请求日志', icon: ScrollText },
  { path: '/users', label: '用户管理', icon: Users },
  { path: '/roles', label: '角色管理', icon: Shield },
]

export function Sidebar() {
  const [collapsed, setCollapsed] = useState(false)
  const location = useLocation()

  return (
    <aside
      className={cn(
        'relative flex flex-col border-r border-white/[0.06] bg-[hsl(240,10%,5%)] transition-all duration-300',
        collapsed ? 'w-[68px]' : 'w-[240px]',
      )}
    >
      {/* Logo */}
      <div className="flex h-16 items-center gap-3 border-b border-white/[0.06] px-4">
        <div className="flex h-9 w-9 shrink-0 items-center justify-center rounded-lg bg-gradient-to-br from-blue-500 to-cyan-400">
          <Sparkles className="h-5 w-5 text-white" />
        </div>
        {!collapsed && (
          <div className="flex flex-col">
            <span className="text-sm font-semibold text-white tracking-tight">FSAICenter</span>
            <span className="text-[10px] text-zinc-500 tracking-wider uppercase">AI 模型网关</span>
          </div>
        )}
      </div>

      {/* Navigation */}
      <ScrollArea className="flex-1 py-4">
        <nav className="flex flex-col gap-1 px-3">
          {navItems.map((item) => {
            const isActive =
              location.pathname === item.path ||
              location.pathname.startsWith(item.path + '/')

            return (
              <NavLink
                key={item.path}
                to={item.path}
                className={cn(
                  'group flex items-center gap-3 rounded-lg px-3 py-2.5 text-sm font-medium transition-all duration-200',
                  isActive
                    ? 'bg-white/[0.08] text-white shadow-sm'
                    : 'text-zinc-400 hover:bg-white/[0.04] hover:text-zinc-200',
                  collapsed && 'justify-center px-0',
                )}
              >
                <item.icon
                  className={cn(
                    'h-[18px] w-[18px] shrink-0 transition-colors',
                    isActive ? 'text-blue-400' : 'text-zinc-500 group-hover:text-zinc-400',
                  )}
                />
                {!collapsed && <span>{item.label}</span>}
                {isActive && !collapsed && (
                  <div className="ml-auto h-1.5 w-1.5 rounded-full bg-blue-400 shadow-[0_0_6px_rgba(59,130,246,0.5)]" />
                )}
              </NavLink>
            )
          })}
        </nav>
      </ScrollArea>

      {/* Collapse Toggle */}
      <button
        onClick={() => setCollapsed(!collapsed)}
        className="flex h-12 items-center justify-center border-t border-white/[0.06] text-zinc-500 hover:text-zinc-300 transition-colors"
      >
        <ChevronLeft
          className={cn('h-4 w-4 transition-transform duration-300', collapsed && 'rotate-180')}
        />
      </button>
    </aside>
  )
}
