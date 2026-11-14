import { useMemo } from 'react'
import { useAuthStore } from '@/stores/auth'
import { buildMenuTree, collectMenuRoutes } from './menu'

/** 由登录会话派生菜单树与路由表：权限在登录时一次性下发，改权限需重新登录才刷新菜单 */
export function useMenuRoutes() {
  const permissions = useAuthStore((state) => state.user?.permissions)

  return useMemo(() => {
    const tree = buildMenuTree(permissions ?? [])
    const routes = collectMenuRoutes(tree)
    return { tree, routes }
  }, [permissions])
}
