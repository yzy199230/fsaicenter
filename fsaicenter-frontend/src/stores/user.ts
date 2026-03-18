import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { UserInfo, LoginResponse } from '@/types/user'

export const useUserStore = defineStore('user', () => {
  const token = ref<string>(localStorage.getItem('token') || '')
  const tokenName = ref<string>(localStorage.getItem('tokenName') || 'satoken')
  const userInfo = ref<UserInfo | null>(null)

  function setLoginInfo(res: LoginResponse) {
    token.value = res.tokenValue
    tokenName.value = res.tokenName
    localStorage.setItem('token', res.tokenValue)
    localStorage.setItem('tokenName', res.tokenName)
    userInfo.value = {
      userId: res.userId,
      username: res.username,
      realName: res.realName,
      avatar: res.avatar,
      status: 1
    }
  }

  function setUserInfo(info: UserInfo) {
    userInfo.value = info
  }

  function logout() {
    token.value = ''
    userInfo.value = null
    localStorage.removeItem('token')
    localStorage.removeItem('tokenName')
  }

  return { token, tokenName, userInfo, setLoginInfo, setUserInfo, logout }
})
