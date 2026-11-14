import {
  Book,
  Boxes,
  Building2,
  FileSearch,
  IdCard,
  Layers,
  LayoutDashboard,
  LayoutGrid,
  MapPin,
  Menu as MenuIconGlyph,
  Network,
  Rocket,
  Settings,
  ShieldCheck,
  SlidersHorizontal,
  Users,
  type LucideIcon,
} from 'lucide-react'
import type { ReactNode } from 'react'

/** 菜单图标登记表：键名与 sys_menu.icon 一致，未登记的键回落到通用图标 */
const ICONS: Record<string, LucideIcon> = {
  /* 目录 */
  Settings,
  Layers,
  LayoutGrid,
  /* 系统管理 */
  Users,
  Network,
  MenuIconGlyph,
  ShieldCheck,
  IdCard,
  Book,
  SlidersHorizontal,
  MapPin,
  FileSearch,
  /* 其它 */
  LayoutDashboard,
  Rocket,
  Boxes,
  Building2,
}

/** 未登记键的兜底图标 */
const FALLBACK = MenuIconGlyph

export function MenuIcon({ name }: { name?: string | null }) {
  if (!name) return <span style={{ color: 'var(--text-4)' }}>—</span>
  const Icon = ICONS[name] ?? FALLBACK
  return (
    <span style={{ color: 'var(--text-2)', display: 'inline-flex', alignItems: 'center' }} title={name}>
      <Icon size={15} strokeWidth={1.9} />
    </span>
  )
}

/** 供菜单/导航渲染：名称未登记时回落到通用图标，不阻断渲染 */
export function renderMenuIcon(name?: string | null): ReactNode {
  if (!name) return undefined
  const Icon = ICONS[name] ?? FALLBACK
  return <Icon size={15} strokeWidth={1.9} />
}

export function iconByName(name?: string | null): LucideIcon | undefined {
  return name ? ICONS[name] : undefined
}

/** 菜单编辑弹窗的图标候选，按用途排序便于挑选 */
export const ICON_OPTIONS = Object.keys(ICONS).map((key) => ({ value: key, label: key }))
