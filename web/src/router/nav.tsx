import type { ReactNode } from 'react'
import { LayoutDashboard, Lock, Rocket } from 'lucide-react'

/** 固定导航项：不进权限表的工作台入口与交付域占位，业务菜单由 useMenuRoutes 动态生成 */
export interface NavNode {
  key: string
  label: string
  icon?: ReactNode
  path?: string
  children?: NavNode[]
  disabled?: boolean
  tag?: string
}

export const WORKBENCH_NAV: NavNode = {
  key: 'workbench',
  label: '工作台',
  icon: <LayoutDashboard size={15} />,
  path: '/workbench',
}

export const PROFILE_NAV: NavNode = {
  key: 'profile',
  label: '个人中心',
  path: '/profile',
}

/** 交付域尚未建设的模块：不注册路由，仅作路线图占位 */
export const ROADMAP_NAV: NavNode = {
  key: 'roadmap',
  label: '交付域（规划中）',
  icon: <Rocket size={15} />,
  children: [
    { key: 'roadmap:app', label: '应用管理', path: '/roadmap/app', disabled: true, tag: '规划中' },
    { key: 'roadmap:artifact', label: '制品管理', path: '/roadmap/artifact', disabled: true, tag: '规划中' },
    { key: 'roadmap:release', label: '发布准入', path: '/roadmap/release', disabled: true, tag: '规划中' },
  ],
}

export const FIXED_PAGES: NavNode[] = [WORKBENCH_NAV, PROFILE_NAV]

/** 非菜单页面的面包屑兜底 */
export const FIXED_TRAILS: Record<string, Array<{ label: string; path?: string }>> = {
  '/workbench': [{ label: '工作台' }],
  '/profile': [{ label: '账号' }, { label: '个人中心' }],
  '/403': [{ label: '无访问权限' }],
}

export function plannedLabel(node: NavNode) {
  return (
    <span className="ferry-menu__planned">
      {node.label}
      <span className="ferry-menu__planned-tag">{node.tag ?? '规划中'}</span>
    </span>
  )
}

export const LOCK_ICON = <Lock size={15} />
