import { create } from 'zustand'
import { authApi } from '@/api/auth'
import type { AuthInfo } from '@/api/types'
import { can } from '@/lib/perm'
import { session } from '@/lib/session'

interface LoginInput {
  tenantId: string
  account: string
  password: string
  captchaCode: string
  remember: boolean
}

interface AuthState {
  token: string | null
  user: AuthInfo | null
  ready: boolean
  login: (input: LoginInput) => Promise<AuthInfo>
  logout: (silent?: boolean) => Promise<void>
  /** 令牌失效时的本地清理：localStorage 与 store 必须同时归零，否则各路由守卫读到脏状态会来回弹 */
  clearSession: () => void
  refreshUser: (user: AuthInfo) => void
  can: (perm?: string | null) => boolean
}

export const useAuthStore = create<AuthState>((set, get) => ({
  token: session.readToken(),
  user: session.readUser(),
  ready: true,

  async login(input) {
    const user = await authApi.login(
      { grantType: 'captcha', tenantId: input.tenantId, account: input.account, password: input.password },
      input.captchaCode,
    )
    session.write(user.accessToken, user)
    session.writeRemember(input.remember ? input.account : '')
    session.writeTenant(input.tenantId)
    set({ token: user.accessToken, user })
    return user
  },

  async logout(silent = false) {
    if (!silent) {
      try {
        await authApi.logout()
      } catch {
        /* 令牌可能已失效，登出始终以本地清理为准 */
      }
    }
    get().clearSession()
  },

  refreshUser(user) {
    session.writeUser(user)
    set({ user })
  },

  clearSession() {
    session.clear()
    set({ token: null, user: null })
  },

  can(perm) {
    return can(get().user, perm)
  },
}))

export const useUser = () => useAuthStore((state) => state.user)
export const useCan = () => useAuthStore((state) => state.can)
