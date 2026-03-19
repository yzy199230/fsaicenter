import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { motion } from 'framer-motion'
import { authApi } from '@/api/auth'
import { useAuthStore } from '@/stores/auth'
import { Input } from '@/components/ui/input'
import { Button } from '@/components/ui/button'
import { Label } from '@/components/ui/label'
import { Sparkles, Eye, EyeOff, Loader2 } from 'lucide-react'
import { toast } from 'sonner'

export default function LoginPage() {
  const navigate = useNavigate()
  const setLoginInfo = useAuthStore((s) => s.setLoginInfo)

  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [showPassword, setShowPassword] = useState(false)
  const [loading, setLoading] = useState(false)

  const handleLogin = async (e: React.FormEvent) => {
    e.preventDefault()

    if (!username.trim()) {
      toast.error('请输入用户名')
      return
    }
    if (!password.trim()) {
      toast.error('请输入密码')
      return
    }

    setLoading(true)
    try {
      const res = await authApi.login({ username, password })
      setLoginInfo(res)
      toast.success('登录成功')
      navigate('/dashboard')
    } catch (err: any) {
      toast.error(err?.response?.data?.message || '登录失败，请重试')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="relative flex min-h-screen items-center justify-center overflow-hidden bg-[#060a13]">
      {/* ── Mesh gradient background ── */}
      <div className="pointer-events-none absolute inset-0">
        {/* Large diffused orb - top-left blue */}
        <motion.div
          className="absolute -left-40 -top-40 h-[600px] w-[600px] rounded-full bg-blue-600/15 blur-[160px]"
          animate={{
            x: [0, 30, -20, 0],
            y: [0, -25, 15, 0],
            scale: [1, 1.08, 0.95, 1],
          }}
          transition={{ duration: 20, repeat: Infinity, ease: 'easeInOut' }}
        />
        {/* Cyan orb - right side */}
        <motion.div
          className="absolute -right-32 top-1/4 h-[500px] w-[500px] rounded-full bg-cyan-500/10 blur-[140px]"
          animate={{
            x: [0, -40, 20, 0],
            y: [0, 30, -20, 0],
            scale: [1, 0.92, 1.06, 1],
          }}
          transition={{ duration: 25, repeat: Infinity, ease: 'easeInOut' }}
        />
        {/* Deep blue orb - bottom */}
        <motion.div
          className="absolute -bottom-20 left-1/3 h-[450px] w-[450px] rounded-full bg-blue-800/20 blur-[130px]"
          animate={{
            x: [0, 25, -30, 0],
            y: [0, -20, 10, 0],
            scale: [1, 1.1, 0.94, 1],
          }}
          transition={{ duration: 22, repeat: Infinity, ease: 'easeInOut' }}
        />
        {/* Accent orb - small, bright */}
        <motion.div
          className="absolute left-1/2 top-1/3 h-[200px] w-[200px] -translate-x-1/2 rounded-full bg-indigo-500/10 blur-[80px]"
          animate={{
            scale: [1, 1.3, 0.9, 1],
            opacity: [0.5, 0.8, 0.4, 0.5],
          }}
          transition={{ duration: 15, repeat: Infinity, ease: 'easeInOut' }}
        />
      </div>

      {/* ── Subtle grid overlay ── */}
      <div
        className="pointer-events-none absolute inset-0 opacity-[0.015]"
        style={{
          backgroundImage:
            'linear-gradient(rgba(255,255,255,0.1) 1px, transparent 1px), linear-gradient(90deg, rgba(255,255,255,0.1) 1px, transparent 1px)',
          backgroundSize: '60px 60px',
        }}
      />

      {/* ── Login card ── */}
      <motion.div
        initial={{ opacity: 0, y: 32, scale: 0.96 }}
        animate={{ opacity: 1, y: 0, scale: 1 }}
        transition={{ duration: 0.7, ease: [0.22, 1, 0.36, 1] }}
        className="relative z-10 w-full max-w-[420px] px-4"
      >
        {/* Card glow effect */}
        <div className="absolute -inset-[1px] rounded-2xl bg-gradient-to-b from-white/[0.08] via-white/[0.03] to-transparent opacity-0 transition-opacity duration-500 group-hover:opacity-100" />

        <div
          className="relative overflow-hidden rounded-2xl border border-white/[0.06] bg-white/[0.03] p-8 shadow-2xl backdrop-blur-xl"
          style={{
            boxShadow:
              '0 0 40px -10px rgba(59, 130, 246, 0.12), 0 25px 50px -12px rgba(0, 0, 0, 0.5)',
          }}
        >
          {/* Card inner shimmer line at top */}
          <div className="absolute inset-x-0 top-0 h-px bg-gradient-to-r from-transparent via-blue-400/20 to-transparent" />

          {/* ── Logo & branding ── */}
          <motion.div
            initial={{ opacity: 0, y: 16 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: 0.15, duration: 0.5 }}
            className="mb-8 flex flex-col items-center"
          >
            <div className="relative mb-4 flex h-14 w-14 items-center justify-center rounded-xl bg-gradient-to-br from-blue-500 to-cyan-400 shadow-lg shadow-blue-500/25">
              <Sparkles className="h-7 w-7 text-white" />
              {/* Pulsing ring */}
              <motion.div
                className="absolute inset-0 rounded-xl border border-blue-400/40"
                animate={{ scale: [1, 1.15, 1], opacity: [0.6, 0, 0.6] }}
                transition={{ duration: 3, repeat: Infinity, ease: 'easeInOut' }}
              />
            </div>
            <h1 className="text-2xl font-bold tracking-tight">
              <span className="gradient-text">FSAICenter</span>
            </h1>
            <p className="mt-1.5 text-sm text-muted-foreground">
              登录您的账户
            </p>
          </motion.div>

          {/* ── Form ── */}
          <motion.form
            initial={{ opacity: 0, y: 16 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: 0.3, duration: 0.5 }}
            onSubmit={handleLogin}
            className="space-y-5"
          >
            {/* Username */}
            <div className="space-y-2">
              <Label htmlFor="username" className="text-zinc-300">
                用户名
              </Label>
              <Input
                id="username"
                type="text"
                placeholder="请输入用户名"
                value={username}
                onChange={(e) => setUsername(e.target.value)}
                autoComplete="username"
                className="h-11 rounded-lg border-white/[0.06] bg-white/[0.04] text-zinc-100 placeholder:text-zinc-500 focus-visible:border-blue-500/40 focus-visible:ring-blue-500/20"
              />
            </div>

            {/* Password */}
            <div className="space-y-2">
              <Label htmlFor="password" className="text-zinc-300">
                密码
              </Label>
              <div className="relative">
                <Input
                  id="password"
                  type={showPassword ? 'text' : 'password'}
                  placeholder="请输入密码"
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  autoComplete="current-password"
                  className="h-11 rounded-lg border-white/[0.06] bg-white/[0.04] pr-11 text-zinc-100 placeholder:text-zinc-500 focus-visible:border-blue-500/40 focus-visible:ring-blue-500/20"
                />
                <button
                  type="button"
                  onClick={() => setShowPassword(!showPassword)}
                  className="absolute right-3 top-1/2 -translate-y-1/2 text-zinc-500 transition-colors hover:text-zinc-300"
                  tabIndex={-1}
                >
                  {showPassword ? (
                    <EyeOff className="h-4.5 w-4.5" />
                  ) : (
                    <Eye className="h-4.5 w-4.5" />
                  )}
                </button>
              </div>
            </div>

            {/* Login button */}
            <motion.div
              initial={{ opacity: 0, y: 8 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: 0.45, duration: 0.4 }}
            >
              <Button
                type="submit"
                disabled={loading}
                className="group relative h-11 w-full overflow-hidden rounded-lg bg-gradient-to-r from-blue-600 to-blue-500 font-medium text-white shadow-lg shadow-blue-500/20 transition-all duration-300 hover:from-blue-500 hover:to-blue-400 hover:shadow-blue-500/30 disabled:from-blue-600/60 disabled:to-blue-500/60"
              >
                {/* Shimmer sweep effect */}
                <span className="absolute inset-0 -translate-x-full bg-gradient-to-r from-transparent via-white/10 to-transparent transition-transform duration-700 ease-out group-hover:translate-x-full" />

                <span className="relative flex items-center justify-center gap-2">
                  {loading ? (
                    <>
                      <Loader2 className="h-4 w-4 animate-spin" />
                      登录中...
                    </>
                  ) : (
                    '登 录'
                  )}
                </span>
              </Button>
            </motion.div>
          </motion.form>

          {/* ── Footer ── */}
          <motion.p
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            transition={{ delay: 0.6, duration: 0.5 }}
            className="mt-6 text-center text-xs text-zinc-600"
          >
            FSAICenter 驱动
          </motion.p>
        </div>
      </motion.div>
    </div>
  )
}
