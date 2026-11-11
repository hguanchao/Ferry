import { useEffect, useMemo, useRef, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { Search } from 'lucide-react'
import { FIXED_PAGES } from '@/router/nav'
import { useMenuRoutes } from '@/router/useMenuRoutes'
import { useUiStore } from '@/stores/ui'

interface Command {
  key: string
  label: string
  hint: string
  group: string
  run: () => void
}

export default function CommandPalette() {
  const navigate = useNavigate()
  const open = useUiStore((state) => state.paletteOpen)
  const setOpen = useUiStore((state) => state.setPaletteOpen)
  const toggleTheme = useUiStore((state) => state.toggleTheme)
  const { routes } = useMenuRoutes()
  const inputRef = useRef<HTMLInputElement>(null)
  const [keyword, setKeyword] = useState('')
  const [active, setActive] = useState(0)

  /** 页面来源与侧边栏一致：固定入口 + 后端下发的菜单路由，权限已在上游过滤 */
  const commands = useMemo<Command[]>(() => {
    const fixed = FIXED_PAGES.filter((page) => page.path).map((page) => ({
      key: `fixed:${page.key}`,
      label: page.label,
      hint: '固定入口',
      group: '页面导航',
      run: () => navigate(page.path as string),
    }))

    const pages = routes.map((route) => ({
      key: `menu:${route.id}`,
      label: route.name,
      hint: route.trail.slice(0, -1).join(' / ') || '页面',
      group: '页面导航',
      run: () => navigate(route.path),
    }))

    return [
      ...fixed,
      ...pages,
      {
        key: 'action:theme',
        label: '切换深浅色主题',
        hint: '外观',
        group: '快捷操作',
        run: toggleTheme,
      },
      {
        key: 'action:logout',
        label: '退出登录',
        hint: '账号',
        group: '快捷操作',
        run: () => navigate('/profile?tab=security'),
      },
    ]
  }, [navigate, routes, toggleTheme])

  const filtered = useMemo(() => {
    const term = keyword.trim().toLowerCase()
    if (!term) return commands
    return commands.filter((command) => `${command.label}${command.hint}`.toLowerCase().includes(term))
  }, [commands, keyword])

  useEffect(() => {
    const handler = (event: KeyboardEvent) => {
      if ((event.metaKey || event.ctrlKey) && event.key.toLowerCase() === 'k') {
        event.preventDefault()
        setOpen(!useUiStore.getState().paletteOpen)
      }
      if (event.key === 'Escape' && useUiStore.getState().paletteOpen) {
        setOpen(false)
      }
    }
    window.addEventListener('keydown', handler)
    return () => window.removeEventListener('keydown', handler)
  }, [setOpen])

  useEffect(() => {
    if (open) {
      setKeyword('')
      setActive(0)
      window.setTimeout(() => inputRef.current?.focus(), 30)
    }
  }, [open])

  useEffect(() => {
    setActive(0)
  }, [keyword])

  if (!open) return null

  const run = (command: Command) => {
    command.run()
    setOpen(false)
  }

  const grouped = filtered.reduce<Record<string, Command[]>>((acc, command) => {
    acc[command.group] = acc[command.group] ?? []
    acc[command.group].push(command)
    return acc
  }, {})

  let flatIndex = -1

  return (
    <div className="ferry-palette-mask" onMouseDown={() => setOpen(false)}>
      <div className="ferry-palette" onMouseDown={(event) => event.stopPropagation()}>
        <div className="ferry-palette__input">
          <Search size={15} />
          <input
            ref={inputRef}
            value={keyword}
            placeholder="输入页面名称或操作，例如「角色」"
            onChange={(event) => setKeyword(event.target.value)}
            onKeyDown={(event) => {
              if (event.key === 'ArrowDown') {
                event.preventDefault()
                setActive((prev) => (prev + 1) % Math.max(1, filtered.length))
              }
              if (event.key === 'ArrowUp') {
                event.preventDefault()
                setActive((prev) => (prev - 1 + filtered.length) % Math.max(1, filtered.length))
              }
              if (event.key === 'Enter' && filtered[active]) {
                event.preventDefault()
                run(filtered[active])
              }
            }}
          />
          <span className="ferry-kbd">ESC</span>
        </div>

        <div className="ferry-palette__list">
          {filtered.length === 0 && <div className="ferry-palette__empty">没有匹配的页面或操作</div>}
          {Object.entries(grouped).map(([group, items]) => (
            <div key={group}>
              <div className="ferry-palette__group">{group}</div>
              {items.map((command) => {
                flatIndex += 1
                const index = flatIndex
                return (
                  <div
                    key={command.key}
                    className="ferry-palette__item"
                    data-active={index === active}
                    onMouseEnter={() => setActive(index)}
                    onClick={() => run(command)}
                    role="presentation"
                  >
                    <span className="ferry-palette__item-icon">{command.label.slice(0, 1)}</span>
                    <span style={{ flex: 1, minWidth: 0 }}>{command.label}</span>
                    <span style={{ color: 'var(--text-4)', fontSize: 12 }}>{command.hint}</span>
                  </div>
                )
              })}
            </div>
          ))}
        </div>

        <div className="ferry-palette__foot">
          <span>
            <span className="ferry-kbd">↑</span> <span className="ferry-kbd">↓</span> 选择
          </span>
          <span>
            <span className="ferry-kbd">Enter</span> 打开
          </span>
          <span className="ferry-spacer" />
          <span>命令面板</span>
        </div>
      </div>
    </div>
  )
}
