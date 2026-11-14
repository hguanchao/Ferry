import type { AuthInfo } from '@/api/types'

const TOKEN_KEY = 'ferry.token'
const USER_KEY = 'ferry.user'
const REMEMBER_KEY = 'ferry.remember'
const TENANT_KEY = 'ferry.tenant'
export const UNAUTHORIZED_EVENT = 'ferry:unauthorized'

/** 会话读写集中在此，避免 axios 客户端与 store 互相引用形成循环依赖 */
export const session = {
  readToken(): string | null {
    try {
      return localStorage.getItem(TOKEN_KEY)
    } catch {
      return null
    }
  },
  readUser(): AuthInfo | null {
    try {
      const raw = localStorage.getItem(USER_KEY)
      return raw ? (JSON.parse(raw) as AuthInfo) : null
    } catch {
      return null
    }
  },
  write(token: string, user: AuthInfo) {
    try {
      localStorage.setItem(TOKEN_KEY, token)
      localStorage.setItem(USER_KEY, JSON.stringify(user))
    } catch {
      /* 隐私模式下写入失败时降级为内存会话 */
    }
  },
  writeUser(user: AuthInfo) {
    try {
      localStorage.setItem(USER_KEY, JSON.stringify(user))
    } catch {
      /* ignore */
    }
  },
  clear() {
    try {
      localStorage.removeItem(TOKEN_KEY)
      localStorage.removeItem(USER_KEY)
    } catch {
      /* ignore */
    }
  },
  readRemember(): string {
    try {
      return localStorage.getItem(REMEMBER_KEY) ?? ''
    } catch {
      return ''
    }
  },
  writeRemember(account: string) {
    try {
      if (account) localStorage.setItem(REMEMBER_KEY, account)
      else localStorage.removeItem(REMEMBER_KEY)
    } catch {
      /* ignore */
    }
  },
  /** 租户编码与账号分开存：换账号登录时仍沿用上次的租户，减少重复输入 */
  readTenant(): string {
    try {
      return localStorage.getItem(TENANT_KEY) ?? ''
    } catch {
      return ''
    }
  },
  writeTenant(tenant: string) {
    try {
      if (tenant) localStorage.setItem(TENANT_KEY, tenant)
      else localStorage.removeItem(TENANT_KEY)
    } catch {
      /* ignore */
    }
  },
  notifyUnauthorized() {
    window.dispatchEvent(new CustomEvent(UNAUTHORIZED_EVENT))
  },
}
