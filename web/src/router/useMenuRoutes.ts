import { useMemo } from 'react'
import { useAuthStore } from '@/stores/auth'
import { buildMenuTree, collectMenuRoutes } from './menu'

/**
 * 由登录会话派生菜单树与路由表
 *
 * 权限在登录时一次性下发（AuthInfo.permissions），改权限后需重新登录才会刷新菜单；
 * 接口层每次请求仍会校验，因此「菜单没刷新」不会造成越权。
 */
export function useMenuRoutes() {
  const permissions = useAuthStore((state) => state.user?.permissions)

  return useMemo(() => {
    const tree = buildMenuTree(permissions ?? [])
    const routes = collectMenuRoutes(tree)
    return { tree, routes }
  }, [permissions])
}
