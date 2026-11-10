import type { ReactNode } from 'react'
import { Navigate, useLocation } from 'react-router-dom'
import { useAuthStore } from '@/stores/auth'

/**
 * 未登录一律回登录页，并记录来源路径用于登录后跳回。
 *
 * 令牌存在但用户信息缺失（localStorage 被部分清理、跨标签页登出）时同样回登录：
 * 动态路由依赖用户权限渲染，放行会得到一片空白或 404。
 */
export default function RequireAuth({ children }: { children: ReactNode }) {
  const token = useAuthStore((state) => state.token)
  const user = useAuthStore((state) => state.user)
  const location = useLocation()

  if (!token || !user) {
    return <Navigate to="/login" replace state={{ from: location.pathname }} />
  }
  return <>{children}</>
}
