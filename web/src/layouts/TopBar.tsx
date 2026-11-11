import { useMemo } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import { App, Avatar, Badge, Dropdown, Popover, Tooltip, type MenuProps } from 'antd'
import {
  Bell,
  ChevronDown,
  CircleHelp,
  LogOut,
  Moon,
  PanelLeftClose,
  PanelLeftOpen,
  Search,
  Sun,
  User,
} from 'lucide-react'
import { findMenuTrail } from '@/router/menu'
import { FIXED_TRAILS } from '@/router/nav'
import { useMenuRoutes } from '@/router/useMenuRoutes'
import { useAuthStore } from '@/stores/auth'
import { useUiStore } from '@/stores/ui'

function initials(name?: string) {
  if (!name) return 'U'
  return name.slice(0, 1).toUpperCase()
}

const ANNOUNCEMENTS = [
  {
    date: '11-09',
    title: 'V1.4.0 制品存储定稿：控制面与数据面职责分离',
    desc: '渡口不再代理镜像 blob 流量，节点按摘要直连 OCI Registry。',
  },
  {
    date: '11-08',
    title: '审计日志留存口径调整为 365 天',
    desc: '审计表仅插入、不可修改，超期归档不物理删除。',
  },
  {
    date: '11-06',
    title: '系统管理模块上线：角色支持「应用-环境」数据权限',
    desc: '生产环境操作权限需单独授予并填写授权理由。',
  },
]

export default function TopBar() {
  const navigate = useNavigate()
  const location = useLocation()
  const { modal, message } = App.useApp()
  const user = useAuthStore((state) => state.user)
  const logout = useAuthStore((state) => state.logout)
  const collapsed = useUiStore((state) => state.collapsed)
  const toggleCollapsed = useUiStore((state) => state.toggleCollapsed)
  const theme = useUiStore((state) => state.theme)
  const toggleTheme = useUiStore((state) => state.toggleTheme)
  const setPaletteOpen = useUiStore((state) => state.setPaletteOpen)
  const { tree } = useMenuRoutes()

  const trail = useMemo(() => {
    const dynamic = findMenuTrail(tree, location.pathname)
    return dynamic.length ? dynamic : (FIXED_TRAILS[location.pathname] ?? [])
  }, [tree, location.pathname])

  const userMenu: MenuProps['items'] = [
    { key: 'profile', icon: <User size={15} />, label: '个人中心' },
    { key: 'security', icon: <CircleHelp size={17} />, label: '安全设置' },
    { type: 'divider' },
    { key: 'logout', icon: <LogOut size={16} />, label: '退出登录', danger: true },
  ]

  const handleUserMenu: MenuProps['onClick'] = ({ key }) => {
    if (key === 'logout') {
      modal.confirm({
        title: '退出登录',
        content: '退出后需重新验证身份，未保存的表单内容将丢失。',
        okText: '退出登录',
        cancelText: '取消',
        okButtonProps: { danger: true },
        onOk: async () => {
          await logout()
          message.success('已安全退出')
          navigate('/login', { replace: true })
        },
      })
      return
    }
    navigate(key === 'security' ? '/profile?tab=security' : '/profile')
  }

  const shortcutKey = navigator.platform.toLowerCase().includes('mac') ? '⌘' : 'Ctrl'

  return (
    <header className="ferry-header">
      <button
        type="button"
        className="ferry-header__collapse"
        onClick={toggleCollapsed}
        aria-label={collapsed ? '展开导航' : '收起导航'}
      >
        {collapsed ? <PanelLeftOpen size={18} /> : <PanelLeftClose size={18} />}
      </button>

      <nav className="ferry-crumbs" aria-label="面包屑">
        {trail.map((item, index) => (
          <span key={`${item.label}-${index}`} className="ferry-hstack" style={{ gap: 6 }}>
            {index > 0 && <span className="ferry-crumbs__sep">/</span>}
            {index === trail.length - 1 ? (
              <span className="ferry-crumbs__current">{item.label}</span>
            ) : (
              <span className="ferry-crumbs__item">{item.label}</span>
            )}
          </span>
        ))}
      </nav>

      <div className="ferry-spacer" />

      <button type="button" className="ferry-search-trigger" onClick={() => setPaletteOpen(true)}>
        <Search size={15} />
        <span>搜索菜单或操作</span>
        <span className="ferry-spacer" />
        <span className="ferry-kbd">{shortcutKey}</span>
        <span className="ferry-kbd">K</span>
      </button>

      <Tooltip title={theme === 'dark' ? '切换到浅色' : '切换到深色'}>
        <button type="button" className="ferry-icon-btn" onClick={toggleTheme} aria-label="切换主题">
          {theme === 'dark' ? <Sun size={17} /> : <Moon size={17} />}
        </button>
      </Tooltip>

      <Popover
        placement="bottomRight"
        trigger="click"
        arrow={false}
        title="系统公告"
        content={
          <div style={{ width: 320, padding: '4px 0' }}>
            {ANNOUNCEMENTS.map((item) => (
              <div key={item.title} style={{ padding: '8px 0', borderBottom: '1px dashed var(--border-1)' }}>
                <div style={{ fontSize: 13, fontWeight: 550, color: 'var(--text-1)', lineHeight: 1.5 }}>
                  {item.title}
                </div>
                <div style={{ fontSize: 12, color: 'var(--text-3)', marginTop: 2 }}>
                  {item.date} · {item.desc}
                </div>
              </div>
            ))}
            <div style={{ paddingTop: 8, fontSize: 12, color: 'var(--text-4)' }}>仅展示最近 3 条变更记录</div>
          </div>
        }
      >
        <button type="button" className="ferry-icon-btn" aria-label="通知">
          <Badge dot color="#1677ff" offset={[0, 2]}>
            <Bell size={18} />
          </Badge>
        </button>
      </Popover>

      <Tooltip title="接口文档">
        <a className="ferry-icon-btn" href="/api/swagger-ui.html" target="_blank" rel="noreferrer" aria-label="接口文档">
          <CircleHelp size={17} />
        </a>
      </Tooltip>

      <Dropdown menu={{ items: userMenu, onClick: handleUserMenu }} placement="bottomRight" trigger={['click']}>
        <button type="button" className="ferry-user-chip">
          <Avatar
            size={28}
            style={{
              background: 'linear-gradient(145deg, #1c4fd6, #35b6ff)',
              fontSize: 13,
              fontWeight: 600,
              flex: '0 0 auto',
            }}
          >
            {initials(user?.nickname)}
          </Avatar>
          <span style={{ display: 'flex', flexDirection: 'column', alignItems: 'flex-start', minWidth: 0 }}>
            <span className="ferry-user-chip__name">{user?.nickname ?? '未登录'}</span>
            <span className="ferry-user-chip__role">{user?.isSuper === 1 ? '超级管理员' : '平台用户'}</span>
          </span>
          <ChevronDown size={10} style={{ fontSize: 10, color: 'var(--text-3)' }} />
        </button>
      </Dropdown>
    </header>
  )
}
