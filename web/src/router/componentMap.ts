import { lazy, type ComponentType, type LazyExoticComponent } from 'react'

/**
 * 页面组件解析：约定优于配置
 *
 * 参考 hnc-ops 的动态路由做法——后端菜单只给 component（如 `/system/User`），
 * 前端用 import.meta.glob 建立懒加载映射，按「目录小写、文件名首字母大写」的约定
 * 直接定位到 `src/pages/system/User.tsx`。新增页面只需建文件 + 配菜单，不再改注册表。
 *
 * 这里必须使用 glob 而不是变量拼 import 路径：Vite 需要在构建期分析出分包边界，
 * 动态字符串会让整站落进同一个 chunk。
 */
type PageComponent = LazyExoticComponent<ComponentType>
type PageLoader = () => Promise<{ default: ComponentType }>

const MODULES = import.meta.glob<{ default: ComponentType }>('../pages/system/*.tsx')

const lazyCache = new Map<string, PageComponent>()

/** `/system/User`、`/system/user`、`@/pages/system/User` 统一成 glob 的 key */
export function toModuleKey(value?: string | null): string | null {
  if (!value) return null
  const cleaned = value
    .trim()
    .replace(/^@\//, '')
    .replace(/^\/+/, '')
    .replace(/\.(tsx|ts|jsx|js)$/i, '')
  const segments = cleaned.split('/').filter(Boolean)
  if (segments.length < 2) return null

  const file = segments[segments.length - 1]
  const dirs = segments.slice(0, -1).join('/')
  return `../pages/${dirs}/${file.charAt(0).toUpperCase()}${file.slice(1)}.tsx`
}

function toLazy(key: string, loader: PageLoader): PageComponent {
  const cached = lazyCache.get(key)
  if (cached) return cached
  const component = lazy(async () => loader())
  lazyCache.set(key, component)
  return component
}

export function resolvePage(component?: string | null, path?: string | null): PageComponent | null {
  const keys = [toModuleKey(component), toModuleKey(path)]
  for (const key of keys) {
    if (!key) continue
    const loader = MODULES[key]
    if (loader) return toLazy(key, loader)
  }
  return null
}

export function isRegistered(component?: string | null, path?: string | null): boolean {
  return resolvePage(component, path) !== null
}

/** 已注册的页面清单，用于「未注册组件」页面的提示 */
export const REGISTERED_PAGES = Object.keys(MODULES)
  .map((key) => key.replace('../pages/', '').replace('.tsx', ''))
  .sort()
