import type { ReactNode } from 'react'
import { Navigate } from 'react-router-dom'
import { useAuthStore } from '@/stores/auth'

/** 功能权限不足时跳 403；接口层仍会二次校验，这里只负责交互体验 */
export default function RequirePerm({ perm, children }: { perm: string; children: ReactNode }) {
  const can = useAuthStore((state) => state.can)
  if (!can(perm)) {
    return <Navigate to="/403" replace />
  }
  return <>{children}</>
}
