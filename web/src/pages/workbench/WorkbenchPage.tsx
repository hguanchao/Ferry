import { useMemo } from 'react'
import { useNavigate } from 'react-router-dom'
import { App, Button, Tooltip } from 'antd'
import { ArrowRight, Plus, RotateCw, Settings, ShieldCheck, User } from 'lucide-react'
import PageHeader from '@/components/PageHeader'
import { useAuthStore } from '@/stores/auth'
import type { WorkbenchOverview } from '@/api/types'

const SHORTCUT_ICONS: Record<string, React.ReactNode> = {
  user: <User size={15} />,
  role: <ShieldCheck size={15} />,
  config: <Settings size={15} />,
}

/** 工作台接口（/ferry-system/workbench/overview）尚未提供，先用静态骨架 + 0 占位 */
const PLACEHOLDER_STATS: WorkbenchOverview['stats'] = [
  { key: 'accounts', label: '平台账号', value: 0, unit: '人', hint: '待接入', tone: 'primary' },
  { key: 'roles', label: '角色与权限', value: 0, unit: '个', hint: '待接入', tone: 'default' },
  { key: 'todayOps', label: '今日写操作', value: 0, unit: '次', hint: '待接入', tone: 'success' },
  { key: 'risk', label: '待治理项', value: 0, unit: '项', hint: '待接入', tone: 'warning' },
]

const PLACEHOLDER_SHORTCUTS: WorkbenchOverview['shortcuts'] = [
  { key: 'user', label: '新增用户', desc: '创建账号并分配角色', path: '/system/user?action=create', perm: 'system:user:save' },
  { key: 'role', label: '配置角色权限', desc: '功能权限与数据权限', path: '/system/role', perm: 'system:role:pageList' },
  { key: 'config', label: '调整参数', desc: '登录策略与留存口径', path: '/system/config', perm: 'system:config:pageList' },
]

function StatCard({ stat }: { stat: WorkbenchOverview['stats'][number] }) {
  return (
    <div className="ferry-stat" data-tone={stat.tone ?? 'default'}>
      <span className="ferry-stat__stripe" />
      <span className="ferry-stat__label">{stat.label}</span>
      <span className="ferry-stat__value">
        {stat.value}
        {stat.unit && <span className="ferry-stat__unit">{stat.unit}</span>}
      </span>
      {stat.hint && <span className="ferry-stat__hint">{stat.hint}</span>}
    </div>
  )
}

export default function WorkbenchPage() {
  const navigate = useNavigate()
  const { message } = App.useApp()
  const user = useAuthStore((state) => state.user)
  const can = useAuthStore((state) => state.can)

  const greeting = useMemo(() => {
    const hour = new Date().getHours()
    const prefix = hour < 6 ? '夜深了' : hour < 12 ? '上午好' : hour < 14 ? '中午好' : hour < 18 ? '下午好' : '晚上好'
    return `${prefix}，${user?.nickname ?? '同事'}`
  }, [user?.nickname])

  return (
    <>
      <PageHeader
        title={greeting}
        description="工作台汇总平台账号与权限状态。统计数据待工作台接口接入，当前以 0 与空态占位。"
        extra={
          <>
            <span className="ferry-env-chip">
              <span className="ferry-env-chip__dot" />
              系统运行正常
            </span>
            <Tooltip title="工作台接口待接入，暂不刷新数据">
              <Button icon={<RotateCw size={15} />} onClick={() => message.info('工作台接口尚未接入，当前为空数据占位')}>
                刷新
              </Button>
            </Tooltip>
          </>
        }
      />

      <div className="ferry-stat-grid">
        {PLACEHOLDER_STATS.map((stat) => (
          <StatCard key={stat.key} stat={stat} />
        ))}
      </div>

      <div className="ferry-workbench">
        <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
          <section className="ferry-panel">
            <div className="ferry-panel__head">
              <span className="ferry-panel__title">快捷入口</span>
              <span className="ferry-panel__extra">
                <span className="ferry-caption">按当前账号权限过滤</span>
              </span>
            </div>
            <div style={{ padding: 16 }}>
              <div className="ferry-quick-grid">
                {PLACEHOLDER_SHORTCUTS.filter((item) => !item.perm || can(item.perm)).map((item) => (
                  <button type="button" className="ferry-quick" key={item.key} onClick={() => navigate(item.path)}>
                    <span className="ferry-quick__icon">{SHORTCUT_ICONS[item.key] ?? <Plus size={14} />}</span>
                    <span className="ferry-quick__text">
                      <span className="ferry-quick__label">{item.label}</span>
                      <span className="ferry-quick__desc">{item.desc}</span>
                    </span>
                    <ArrowRight size={14} style={{ marginLeft: 'auto', color: 'var(--text-4)', fontSize: 11 }} />
                  </button>
                ))}
              </div>
            </div>
          </section>

          <section className="ferry-panel">
            <div className="ferry-panel__head">
              <span className="ferry-panel__title">系统概览</span>
              <span className="ferry-panel__extra">
                <span className="ferry-caption">只读</span>
              </span>
            </div>
            <div style={{ padding: '4px 18px 14px' }}>
              <dl className="ferry-kv">
                <div>
                  <dt>产品版本</dt>
                  <dd>
                    —
                    <span className="ferry-caption" style={{ marginLeft: 8 }}>
                      编排层
                    </span>
                  </dd>
                </div>
                <div>
                  <dt>接入环境</dt>
                  <dd>{user?.tenantId === '000000' ? '内部研发环境' : '试点租户'}</dd>
                </div>
                <div>
                  <dt>平台账号 / 角色</dt>
                  <dd>0 / 0</dd>
                </div>
                <div>
                  <dt>菜单与权限点</dt>
                  <dd>0 项</dd>
                </div>
                <div>
                  <dt>日志留存</dt>
                  <dd>365 天</dd>
                </div>
                <div>
                  <dt>最近备份</dt>
                  <dd className="ferry-mono" style={{ fontSize: 12.5 }}>
                    —
                  </dd>
                </div>
              </dl>
            </div>
          </section>
        </div>

      </div>
    </>
  )
}
