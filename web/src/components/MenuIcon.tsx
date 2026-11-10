import {
  ApartmentOutlined,
  BankOutlined,
  BookOutlined,
  ControlOutlined,
  DashboardOutlined,
  EnvironmentOutlined,
  FileSearchOutlined,
  AppstoreOutlined,
  IdcardOutlined,
  BlockOutlined,
  MenuOutlined,
  RocketOutlined,
  SafetyCertificateOutlined,
  SettingOutlined,
  TeamOutlined,
  UserOutlined,
} from '@ant-design/icons'
import type { ReactNode } from 'react'

const ICONS: Record<string, ReactNode> = {
  SettingOutlined: <SettingOutlined />,
  UserOutlined: <UserOutlined />,
  TeamOutlined: <TeamOutlined />,
  SafetyCertificateOutlined: <SafetyCertificateOutlined />,
  MenuOutlined: <MenuOutlined />,
  ApartmentOutlined: <ApartmentOutlined />,
  IdcardOutlined: <IdcardOutlined />,
  BookOutlined: <BookOutlined />,
  ControlOutlined: <ControlOutlined />,
  EnvironmentOutlined: <EnvironmentOutlined />,
  BankOutlined: <BankOutlined />,
  FileSearchOutlined: <FileSearchOutlined />,
  DashboardOutlined: <DashboardOutlined />,
  RocketOutlined: <RocketOutlined />,
  BlockOutlined: <BlockOutlined />,
  AppstoreOutlined: <AppstoreOutlined />,
}

export function MenuIcon({ name }: { name?: string | null }) {
  if (!name) return <span style={{ color: 'var(--text-4)' }}>—</span>
  return (
    <span style={{ color: 'var(--text-2)', fontSize: 14 }} title={name}>
      {ICONS[name] ?? <MenuOutlined />}
    </span>
  )
}

/** 供菜单/导航渲染：名称未登记时回落到通用图标，不阻断渲染 */
export function renderMenuIcon(name?: string | null): ReactNode {
  if (!name) return undefined
  return ICONS[name] ?? <MenuOutlined />
}

export const ICON_OPTIONS = Object.keys(ICONS).map((key) => ({ value: key, label: key }))
