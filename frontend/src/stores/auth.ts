import { defineStore } from 'pinia'

import { getCurrentUser, login, register } from '@/api/auth'
import type { LoginRequest, RegisterRequest, UserInfo } from '@/types/auth'

const TOKEN_KEY = 'campus_event_token'
const USER_KEY = 'campus_event_user'

function loadUser(): UserInfo | null {
  const raw = localStorage.getItem(USER_KEY)
  if (!raw) {
    return null
  }
  try {
    return JSON.parse(raw) as UserInfo
  } catch {
    localStorage.removeItem(USER_KEY)
    return null
  }

}

export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: localStorage.getItem(TOKEN_KEY) || '',
    user: loadUser(),
  }),
  getters: {
    isLoggedIn: (state) => Boolean(state.token && state.user),
  },
  actions: {
    setSession(token: string, user: UserInfo) {
      this.token = token
      this.user = user
      localStorage.setItem(TOKEN_KEY, token)
      localStorage.setItem(USER_KEY, JSON.stringify(user))
    },
    clearSession() {
      this.token = ''
      this.user = null
      localStorage.removeItem(TOKEN_KEY)
      localStorage.removeItem(USER_KEY)
    },
    async login(payload: LoginRequest) {
      const data = await login(payload)
      this.setSession(data.token, data.user)
      return data
    },
    async register(payload: RegisterRequest) {
      return register(payload)
    },
    async refreshCurrentUser() {
      if (!this.token) {
        return null
      }
      const user = await getCurrentUser()
      this.user = user
      localStorage.setItem(USER_KEY, JSON.stringify(user))
      return user
    },
    logout() {
      this.clearSession()
    },
  },

})
