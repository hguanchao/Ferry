import { useEffect, useMemo, useState } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import { Menu, type MenuProps } from 'antd'
import { renderMenuIcon } from '@/components/MenuIcon'
import { isPathActive, normalizePath, type MenuNode } from '@/router/menu'
import { LOCK_ICON, ROADMAP_NAV, WORKBENCH_NAV, plannedLabel } from '@/router/nav'
import { useMenuRoutes } from '@/router/useMenuRoutes'
import { useAuthStore } from '@/stores/auth'
import { useUiStore } from '@/stores/ui'

function BrandMark() {
  return (
    <svg width="19" height="19" viewBox="0 0 24 24" fill="none" aria-hidden>
      <path
        d="M3 15.5c2.2 0 2.2 1.7 4.4 1.7S9.6 15.5 11.8 15.5s2.2 1.7 4.4 1.7 2.2-1.7 4.4-1.7"
        stroke="#ffffff"
        strokeWidth="1.7"
        strokeLinecap="round"
        opacity="0.92"
      />
      <path d="M4.6 12.6 12 5l7.4 7.6" stroke="#ffffff" strokeWidth="1.7" strokeLinecap="round" strokeLinejoin="round" />
      <path d="M9.2 12.6V17m5.6-4.4V17" stroke="#ffffff" strokeWidth="1.7" strokeLinecap="round" />
    </svg>
  )
}

/** 后端菜单树 → antd Menu items，key 用菜单 id，图标名从字典映射 */
function menuToItems(nodes: MenuNode[]): NonNullable<MenuProps['items']> {
  return nodes.map((node) =>
    node.children.length
      ? {
          key: node.id,
          icon: renderMenuIcon(node.icon),
          label: node.name,
          popupClassName: 'ferry-menu-popup',
          children: menuToItems(node.children),
        }
      : {
          key: node.id,
          icon: renderMenuIcon(node.icon),
          label: node.name,
        },
  )
}

export default function SideNav() {
  const navigate = useNavigate()
  const location = useLocation()
  const collapsed = useUiStore((state) => state.collapsed)
  const user = useAuthStore((state) => state.user)
  const { tree, routes } = useMenuRoutes()
  const [openKeys, setOpenKeys] = useState<string[]>([])

  /** 当前命中的路由取最长前缀，避免 /system 与 /system/user 同时匹配 */
  const activeRoute = useMemo(
    () =>
      routes
        .filter((route) => isPathActive(location.pathname, route.path))
        .sort((left, right) => right.path.length - left.path.length)[0],
    [routes, location.pathname],
  )

  useEffect(() => {
    if (activeRoute?.parentIds.length) {
      setOpenKeys((prev) => Array.from(new Set([...prev, ...activeRoute.parentIds])))
      return
    }
    // 首屏（工作台等固定页）没有命中菜单时，默认展开第一个分组，避免导航看起来「空的」
    setOpenKeys((prev) => (prev.length ? prev : tree.length ? [tree[0].id] : []))
  }, [activeRoute, tree])

  const idToPath = useMemo(() => {
    const map = new Map<string, string>()
    const walk = (nodes: MenuNode[]) => {
      nodes.forEach((node) => {
        const path = normalizePath(node.path)
        if (path) map.set(node.id, path)
        walk(node.children)
      })
    }
    walk(tree)
    return map
  }, [tree])

  const items = useMemo<MenuProps['items']>(
    () => [
      {
        key: WORKBENCH_NAV.key,
        icon: WORKBENCH_NAV.icon,
        label: WORKBENCH_NAV.label,
      },
      ...menuToItems(tree),
      {
        key: ROADMAP_NAV.key,
        icon: ROADMAP_NAV.icon,
        label: ROADMAP_NAV.label,
        popupClassName: 'ferry-menu-popup',
        children: (ROADMAP_NAV.children ?? []).map((node) => ({
          key: node.key,
          icon: LOCK_ICON,
          disabled: true,
          label: plannedLabel(node),
        })),
      },
    ],
    [tree],
  )

  const selectedKeys = useMemo(() => {
    if (activeRoute) return [activeRoute.id]
    return location.pathname === '/workbench' ? [WORKBENCH_NAV.key] : []
  }, [activeRoute, location.pathname])

  return (
    <aside className="ferry-sider" data-collapsed={collapsed}>
      <div className="ferry-brand" onClick={() => navigate('/workbench')} role="presentation">
        <span className="ferry-brand__mark">
          <BrandMark />
        </span>
        {!collapsed && (
          <span className="ferry-brand__text">
            <span className="ferry-brand__name">渡口 Ferry</span>
            <span className="ferry-brand__sub">Delivery Platform</span>
          </span>
        )}
      </div>

      <div className="ferry-sider__scroll">
        <Menu
          className="ferry-menu"
          mode="inline"
          inlineCollapsed={collapsed}
          items={items}
          selectedKeys={selectedKeys}
          openKeys={openKeys}
          onOpenChange={(keys) => setOpenKeys(keys.map(String))}
          onClick={({ key }) => {
            if (key === WORKBENCH_NAV.key) {
              navigate('/workbench')
              return
            }
            const path = idToPath.get(String(key))
            if (path) navigate(path)
          }}
        />
      </div>

      <div className="ferry-sider__footer">
        {!collapsed ? (
          <>
            <span className="ferry-env-chip" title="当前接入环境">
              <span className="ferry-env-chip__dot" />
              {user?.tenantId === '000000' ? '内部研发环境' : '试点租户'}
            </span>
            <span className="ferry-sider__version">Ferry v1.4.0 · 菜单动态下发</span>
          </>
        ) : (
          <span className="ferry-env-chip" style={{ padding: 6, justifyContent: 'center' }} title="内部研发环境">
            <span className="ferry-env-chip__dot" />
          </span>
        )}
      </div>
    </aside>
  )
}
