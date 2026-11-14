import type { AuthPerm } from '@/api/types'

/** 菜单与动态路由：数据源是登录返回的 permissions，只把 type=1 且带 path 的菜单注册成扁平路由 */

export interface MenuNode {
  id: string
  parentId: string
  name: string
  type: number
  icon?: string | null
  path?: string | null
  component?: string | null
  perms?: string | null
  sort: number
  children: MenuNode[]
}

export interface MenuRoute {
  id: string
  path: string
  name: string
  component?: string | null
  perms?: string | null
  icon?: string | null
  /** 从根目录到当前菜单的名称链，用于面包屑与命令面板 */
  trail: string[]
  /** 祖先目录 id，用于展开侧边栏 */
  parentIds: string[]
}

/** 归一化路由地址；外链与空值不注册为内部路由 */
export function normalizePath(path?: string | null): string | null {
  if (!path) return null
  const trimmed = path.trim()
  if (!trimmed) return null
  if (/^(https?:)?\/\//i.test(trimmed)) return null
  return trimmed.startsWith('/') ? trimmed : `/${trimmed}`
}

export function isPathActive(pathname: string, routePath: string): boolean {
  return pathname === routePath || pathname.startsWith(`${routePath}/`)
}

/** 由扁平权限列表构建可见菜单树：隐藏停用与隐藏项、剔除空目录、按 sort 排序 */
export function buildMenuTree(perms: AuthPerm[]): MenuNode[] {
  const visible = perms.filter((perm) => perm.type !== 2 && perm.isShow !== 0)
  const nodes = new Map<string, MenuNode>()

  visible.forEach((perm) => {
    const id = String(perm.id)
    nodes.set(id, {
      id,
      parentId: String(perm.parentId ?? '0'),
      name: perm.name,
      type: perm.type,
      icon: perm.icon,
      path: perm.path,
      component: perm.component,
      perms: perm.perms,
      sort: perm.sort ?? 0,
      children: [],
    })
  })

  const roots: MenuNode[] = []
  nodes.forEach((node) => {
    const parent = nodes.get(node.parentId)
    if (parent && parent.id !== node.id) parent.children.push(node)
    else roots.push(node)
  })

  const sortRec = (list: MenuNode[]) => {
    list.sort((left, right) => left.sort - right.sort)
    list.forEach((node) => sortRec(node.children))
  }
  sortRec(roots)

  // 角色只授了子菜单、没授目录时，目录仍要保留；反过来空目录不该出现在导航里
  const prune = (list: MenuNode[]): MenuNode[] =>
    list.filter((node) => {
      node.children = prune(node.children)
      return node.type !== 0 || node.children.length > 0
    })

  return prune(roots)
}

/** 拍平为可注册的页面路由（含祖先链），顺序即菜单顺序 */
export function collectMenuRoutes(
  nodes: MenuNode[],
  parents: Array<{ id: string; name: string }> = [],
): MenuRoute[] {
  return nodes.flatMap((node) => {
    const trail = [...parents.map((item) => item.name), node.name]
    const parentIds = parents.map((item) => item.id)
    const children = node.children.length
      ? collectMenuRoutes(node.children, [...parents, { id: node.id, name: node.name }])
      : []

    if (node.type !== 1) return children
    const path = normalizePath(node.path)
    if (!path) return children

    return [
      {
        id: node.id,
        path,
        name: node.name,
        component: node.component,
        perms: node.perms,
        icon: node.icon,
        trail,
        parentIds,
      },
      ...children,
    ]
  })
}

/** 在菜单树里找当前路径对应的菜单链 */
export function findMenuTrail(nodes: MenuNode[], pathname: string): Array<{ label: string; path?: string }> {
  const walk = (list: MenuNode[], trail: string[]): { labels: string[]; path: string } | null => {
    for (const node of list) {
      const next = [...trail, node.name]
      const path = normalizePath(node.path)
      if (node.type === 1 && path && isPathActive(pathname, path)) {
        return { labels: next, path }
      }
      if (node.children.length) {
        const found = walk(node.children, next)
        if (found) return found
      }
    }
    return null
  }

  const result = walk(nodes, [])
  if (!result) return []
  return result.labels.map((label, index) => ({
    label,
    path: index === result.labels.length - 1 ? result.path : undefined,
  }))
}
