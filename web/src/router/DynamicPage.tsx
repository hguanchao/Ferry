import { Suspense } from 'react'
import { Skeleton, Tag } from 'antd'
import { ToolOutlined } from '@ant-design/icons'
import { REGISTERED_PAGES, resolvePage } from './componentMap'
import type { MenuRoute } from './menu'

function PageSkeleton() {
  return (
    <div className="ferry-panel" style={{ padding: 24 }}>
      <Skeleton active title={{ width: 200 }} paragraph={{ rows: 6 }} />
    </div>
  )
}

/**
 * 菜单已存在但前端没有对应组件时的兜底
 *
 * 这种情况通常发生在后端先加了菜单、前端还没发版。页面把差异直接摆出来，
 * 比静默 404 更容易定位——运维和开发看到的都是同一句话。
 */
function UnregisteredMenu({ route }: { route: MenuRoute }) {
  return (
    <div className="ferry-panel">
      <div className="ferry-placeholder" style={{ padding: '48px 24px', maxWidth: 720, margin: '0 auto' }}>
        <span
          style={{
            display: 'grid',
            placeItems: 'center',
            width: 48,
            height: 48,
            borderRadius: 14,
            background: 'var(--color-warning-bg)',
            color: 'var(--color-warning)',
            fontSize: 22,
          }}
        >
          <ToolOutlined />
        </span>
        <span className="ferry-placeholder__title">「{route.name}」尚未注册前端组件</span>
        <span className="ferry-caption" style={{ maxWidth: 520 }}>
          菜单由后端下发，页面组件由前端注册表决定。请在
          <code className="ferry-mono" style={{ margin: '0 4px' }}>
            src/router/componentMap.ts
          </code>
          中登记该组件后重新构建。
        </span>
        <div
          style={{
            marginTop: 8,
            padding: '10px 14px',
            border: '1px solid var(--border-1)',
            borderRadius: 8,
            background: 'var(--bg-subtle)',
            textAlign: 'left',
            width: '100%',
            display: 'grid',
            gap: 6,
            fontSize: 12,
          }}
        >
          <div>
            <span className="ferry-muted">路由地址</span>
            <span className="ferry-mono" style={{ marginLeft: 8 }}>
              {route.path}
            </span>
          </div>
          <div>
            <span className="ferry-muted">component</span>
            <span className="ferry-mono" style={{ marginLeft: 8 }}>
              {route.component || '（空）'}
            </span>
          </div>
          <div>
            <span className="ferry-muted">权限标识</span>
            <span className="ferry-mono" style={{ marginLeft: 8 }}>
              {route.perms || '（空）'}
            </span>
          </div>
        </div>
        <div style={{ display: 'flex', flexWrap: 'wrap', gap: 6, justifyContent: 'center', marginTop: 8 }}>
          {REGISTERED_PAGES.slice(0, 12).map((key) => (
            <Tag key={key} style={{ fontFamily: 'var(--font-mono)', fontSize: 11 }}>
              {key}
            </Tag>
          ))}
        </div>
      </div>
    </div>
  )
}

export default function DynamicPage({ route }: { route: MenuRoute }) {
  const Page = resolvePage(route.component, route.path)
  if (!Page) return <UnregisteredMenu route={route} />
  return (
    <Suspense fallback={<PageSkeleton />}>
      <Page />
    </Suspense>
  )
}
