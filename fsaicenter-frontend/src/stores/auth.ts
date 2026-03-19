import { create } from 'zustand'
import { persist } from 'zustand/middleware'
import type { UserInfo, LoginResponse } from '@/types/user'

interface AuthState {
  token: string
  tokenName: string
  userInfo: UserInfo | null
  setLoginInfo: (res: LoginResponse) => void
  setUserInfo: (info: UserInfo) => void
  logout: () => void
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set) => ({
      token: '',
      tokenName: 'satoken',
      userInfo: null,
      setLoginInfo: (res) =>
        set({
          token: res.tokenValue,
          tokenName: res.tokenName,
          userInfo: {
            userId: res.userId,
            username: res.username,
            realName: res.realName,
            avatar: res.avatar,
            status: 1,
          },
        }),
      setUserInfo: (info) => set({ userInfo: info }),
      logout: () => set({ token: '', userInfo: null }),
    }),
    { name: 'auth-storage' },
  ),
)
