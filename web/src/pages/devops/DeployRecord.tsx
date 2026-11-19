import { useState } from 'react'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import { App, Button, Descriptions, Drawer, Form, Input, Select, Space, Table, Tag, Tooltip, type TableColumnsType } from 'antd'
import { Activity, CircleCheck, CircleX, Pause, Play, RotateCw, Search, ShieldAlert } from 'lucide-react'
import PageHeader from '@/components/PageHeader'
import { RoleTag } from '@/components/StatusTag'
import { deployApi, envApi, appApi } from '@/api/devops'
import type { DeployRecordRow } from '@/api/types-devops'
import { useAuthStore } from '@/stores/auth'
import { showError } from '@/lib/notify'
import { COL } from '@/lib/table'

const STATUS: Record<number, { text: string; color: string }> = {
  1: { text: '未开始', color: 'default' }, 2: { text: '执行中', color: 'blue' },
  3: { text: '观察中', color: 'gold' }, 4: { text: '成功', color: 'green' },
  5: { text: '失败', color: 'red' }, 6: { text: '已回滚', color: 'volcano' }, 7: { text: '已取消', color: 'default' },
}
const TYPE: Record<number, string> = { 1: 'SSH', 2: 'Docker', 3: 'Kubernetes' }

type DeployDrawer = { id: string; envId?: string; appId?: string } | null

export default function DeployRecord() {
  const { message, modal } = App.useApp()
  const queryClient = useQueryClient()
  const can = useAuthStore((s) => s.can)
  const [filters, setFilters] = useState<Record<string, unknown>>({})
  const [draft, setDraft] = useState<Record<string, unknown>>({})
  const [page, setPage] = useState({ current: 1, size: 30 })
  const [drawer, setDrawer] = useState<DeployDrawer>(null)
  const [reason, setReason] = useState('')

  const list = useQuery({
    queryKey: ['deploy', filters, page],
    queryFn: () => deployApi.pageList({ ...filters, ...page }),
  })
  const apps = useQuery({ queryKey: ['app-options'], queryFn: () => appApi.options() })
  const envs = useQuery({ queryKey: ['env-options'], queryFn: () => envApi.options() })

  const detail = useQuery({
    queryKey: ['deploy-detail', drawer?.id],
    queryFn: () => deployApi.detail(drawer!.id),
    enabled: !!drawer?.id,
  })
  const batches = useQuery({
    queryKey: ['deploy-batches', drawer?.id],
    queryFn: () => deployApi.batches(drawer!.id),
    enabled: !!drawer?.id,
  })
  const health = useQuery({
    queryKey: ['deploy-health', drawer?.id],
    queryFn: () => deployApi.health(drawer!.id),
    enabled: !!drawer?.id,
  })

  const invalidate = () => void queryClient.invalidateQueries({ queryKey: ['deploy'] })
  const simple = (fn: () => Promise<string>, ok: string) => {
    fn().then((t) => { message.success(t || ok); invalidate() }).catch(showError)
  }

  const columns: TableColumnsType<DeployRecordRow> = [
    { title: '部署编号', dataIndex: 'deployNo', width: 170, render: (v: string) => <span className="ferry-mono" style={{ fontSize: 12 }}>{v}</span> },
    { title: '应用', dataIndex: 'appId', width: 130, render: (v: string) => apps.data?.find((a) => a.value === v)?.label ?? v },
    { title: '环境', dataIndex: 'envId', width: 130, render: (v: string) => envs.data?.find((e) => e.value === v)?.label ?? v },
    { title: '形态', dataIndex: 'deployType', width: 110, render: (v: number) => TYPE[v] ?? '—' },
    { title: '状态', dataIndex: 'status', width: 100, render: (v: number) => <Tag color={STATUS[v]?.color}>{STATUS[v]?.text ?? '—'}</Tag> },
    { title: '操作人', dataIndex: 'operator', width: 100 },
    { title: '开始时间', dataIndex: 'startTime', width: COL.time, render: (v?: string) => <span className="ferry-mono" style={{ fontSize: 12, color: 'var(--text-3)' }}>{v ?? '—'}</span> },
    { title: '操作', key: 'action', width: 220, fixed: 'right', render: (_, r) => (
      <Space size={2}>
        <Tooltip title="详情">
          <Button type="link" size="small" icon={<Activity size={14} />} onClick={() => setDrawer({ id: r.id, envId: r.envId, appId: r.appId })} />
        </Tooltip>
        {can('devops:deploy:pause') && [2, 3].includes(r.status) && (
          <Tooltip title="暂停">
            <Button type="link" size="small" icon={<Pause size={14} />} onClick={() => simple(() => deployApi.pause(r.id), '已暂停')} />
          </Tooltip>
        )}
        {can('devops:deploy:resume') && r.status === 3 && (
          <Tooltip title="恢复">
            <Button type="link" size="small" icon={<Play size={14} />} onClick={() => simple(() => deployApi.resume(r.id), '已恢复')} />
          </Tooltip>
        )}
      </Space>
    ) },
  ]

  const d = (detail.data ?? {}) as Record<string, unknown>
  const batchRows = batches.data ?? []
  const healthRows = health.data ?? []

  return (
    <>
      <PageHeader title="部署记录" description="灰度批次、健康检查与熔断决策。生产部署需独立的生产权限与不同确认人。"
        tag={<RoleTag>共 {list.data?.total ?? 0} 项</RoleTag>} />
      <section className="ferry-panel">
        <div className="ferry-toolbar">
          <Input allowClear prefix={<Search size={15} />} placeholder="部署编号" style={{ width: 180 }} value={draft.deployNo as string}
            onChange={(e) => setDraft({ ...draft, deployNo: e.target.value })} />
          <Select allowClear placeholder="应用" style={{ width: 150 }} value={draft.appId as number}
            onChange={(v) => setDraft({ ...draft, appId: v })} options={apps.data?.map((a) => ({ value: a.value, label: a.label }))} />
          <Select allowClear placeholder="状态" style={{ width: 120 }} value={draft.status as number}
            onChange={(v) => setDraft({ ...draft, status: v })} options={Object.entries(STATUS).map(([k, v]) => ({ value: Number(k), label: v.text }))} />
          <Button type="primary" icon={<Search size={15} />} onClick={() => { setFilters(draft); setPage((p) => ({ ...p, current: 1 })) }}>查询</Button>
          <Button icon={<RotateCw size={15} />} onClick={() => { setDraft({}); setFilters({}); setPage((p) => ({ ...p, current: 1 })) }}>重置</Button>
        </div>
        <div className="ferry-table-wrap">
          <Table<DeployRecordRow> rowKey="id" columns={columns} dataSource={list.data?.records ?? []}
            loading={list.isLoading || list.isFetching} scroll={{ x: 'max-content' }}
            pagination={{ current: page.current, pageSize: page.size, total: list.data?.total ?? 0, showSizeChanger: true,
              pageSizeOptions: ['30', '100', '200'], showTotal: (t) => `共 ${t} 条`, onChange: (c, s) => setPage({ current: c, size: s }) }} />
        </div>
      </section>

      <Drawer open={!!drawer} width={720} title={`部署详情 · ${d.deployNo ?? ''}`} onClose={() => setDrawer(null)} destroyOnHidden>
        <Descriptions bordered size="small" column={2}>
          <Descriptions.Item label="应用">{String(d.appName ?? '—')}</Descriptions.Item>
          <Descriptions.Item label="环境">{String(d.envName ?? '—')}</Descriptions.Item>
          <Descriptions.Item label="形态">{TYPE[d.deployType as number] ?? '—'}</Descriptions.Item>
          <Descriptions.Item label="状态"><Tag color={STATUS[d.status as number]?.color}>{STATUS[d.status as number]?.text ?? '—'}</Tag></Descriptions.Item>
          <Descriptions.Item label="操作人">{String(d.operator ?? '—')}</Descriptions.Item>
          <Descriptions.Item label="确认人">{String(d.confirmer ?? '—')}</Descriptions.Item>
        </Descriptions>
        <h4 style={{ marginTop: 20 }}>灰度批次</h4>
        <Table size="small" rowKey="id" pagination={false} dataSource={batchRows as unknown as Record<string, unknown>[]}
          columns={[
            { title: '批次', dataIndex: 'batchNo', width: 70 },
            { title: '实例数', dataIndex: 'targetCount', width: 90 },
            { title: '状态', dataIndex: 'status', render: (v: number) => <Tag color={STATUS[v]?.color}>{STATUS[v]?.text}</Tag> },
            { title: '健康', dataIndex: 'healthResult', render: (v: number) => v === 2 ? <Tag color="red" icon={<CircleX size={12} />}>不通过</Tag> : v === 1 ? <Tag color="green" icon={<CircleCheck size={12} />}>通过</Tag> : '—' },
          ]} />
        <h4 style={{ marginTop: 20 }}>健康检查采样</h4>
        <Table size="small" rowKey="id" pagination={false} dataSource={healthRows as Record<string, unknown>[]}
          columns={[
            { title: '检查项', dataIndex: 'checkType', width: 90 },
            { title: '采样值', dataIndex: 'value', width: 120 },
            { title: '阈值', dataIndex: 'threshold', width: 120 },
            { title: '结论', dataIndex: 'conclusion', render: (v: number) => v === 2 ? <Tag color="red">不通过</Tag> : v === 1 ? <Tag color="green">通过</Tag> : '—' },
          ]} />
        {can('devops:deploy:circuitContinue') && [5, 6].includes(d.status as number) && (
          <Form layout="vertical" style={{ marginTop: 20 }}>
            <Form.Item label="熔断后强制继续理由（需生产权限，写审计）">
              <Input.TextArea rows={2} value={reason} onChange={(e) => setReason(e.target.value)} placeholder="填写强制继续理由" />
            </Form.Item>
            <Button danger icon={<ShieldAlert size={14} />} onClick={() => modal.confirm({
              title: '熔断后强制继续？', content: '将强制继续后续批次并写入审计。',
              onOk: () => simple(() => deployApi.circuitContinue({ deployId: drawer?.id, reason }), '已强制继续'),
            })}>强制继续</Button>
          </Form>
        )}
      </Drawer>
    </>
  )
}
