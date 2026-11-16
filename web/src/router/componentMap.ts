import { lazy, type ComponentType, type LazyExoticComponent } from 'react'

/** 页面组件解析：按约定用 import.meta.glob 建立懒加载映射；必须用 glob 才能让 Vite 在构建期分包 */
type PageComponent = LazyExoticComponent<ComponentType>
type PageLoader = () => Promise<{ default: ComponentType }>

const MODULES = import.meta.glob<{ default: ComponentType }>([
  '../pages/system/*.tsx',
])

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
