import { useNavigate } from 'react-router-dom'
import { useAuthStore } from '@/stores/auth'
import { authApi } from '@/api/auth'
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu'
import { LogOut, User, ChevronDown } from 'lucide-react'
import { toast } from 'sonner'

export function Header() {
  const navigate = useNavigate()
  const { userInfo, logout } = useAuthStore()

  const handleLogout = async () => {
    try {
      await authApi.logout()
    } catch {
      // ignore
    }
    logout()
    navigate('/login')
    toast.success('已退出登录')
  }

  return (
    <header className="flex h-16 items-center justify-between border-b border-white/[0.06] bg-[hsl(240,10%,5%)]/80 backdrop-blur-xl px-6">
      <div />

      <DropdownMenu>
        <DropdownMenuTrigger className="flex items-center gap-2 rounded-lg px-3 py-2 text-sm text-zinc-400 hover:bg-white/[0.04] hover:text-zinc-200 transition-colors outline-none">
          <div className="flex h-7 w-7 items-center justify-center rounded-full bg-gradient-to-br from-blue-500/20 to-cyan-500/20 border border-blue-500/20">
            <User className="h-3.5 w-3.5 text-blue-400" />
          </div>
          <span>{userInfo?.realName || userInfo?.username || '管理员'}</span>
          <ChevronDown className="h-3.5 w-3.5 text-zinc-500" />
        </DropdownMenuTrigger>
        <DropdownMenuContent align="end" className="w-48">
          <DropdownMenuLabel className="text-xs text-zinc-500">
            {userInfo?.username}
          </DropdownMenuLabel>
          <DropdownMenuSeparator />
          <DropdownMenuItem onClick={handleLogout} className="text-red-400 focus:text-red-300 cursor-pointer">
            <LogOut className="mr-2 h-4 w-4" />
            退出登录
          </DropdownMenuItem>
        </DropdownMenuContent>
      </DropdownMenu>
    </header>
  )
}
