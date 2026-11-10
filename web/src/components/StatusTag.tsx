import { Tag } from 'antd'

export function EnabledTag({ value }: { value?: number | null }) {
  const enabled = value === 1
  return (
    <span className="ferry-status" data-tone={enabled ? 'success' : 'default'}>
      <span className="ferry-status__dot" />
      {enabled ? '正常' : '已停用'}
    </span>
  )
}

export function RoleTag({ children }: { children: React.ReactNode }) {
  return (
    <Tag
      style={{
        background: 'var(--bg-subtle)',
        color: 'var(--text-2)',
        borderColor: 'var(--border-1)',
        fontFamily: 'var(--font-mono)',
        fontSize: 11.5,
      }}
    >
      {children}
    </Tag>
  )
}

export function BuiltinTag() {
  return (
    <Tag color="blue" style={{ fontSize: 11 }}>
      内置
    </Tag>
  )
}
