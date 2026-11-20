import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { App, Button, Input, Select, Tag, Tooltip, type TableColumnsType } from 'antd'
import FerryTable from '@/components/FerryTable'
import { RotateCw, Search, Undo2 } from 'lucide-react'
import PageHeader from '@/components/PageHeader'
import { RoleTag } from '@/components/StatusTag'
import { gateApi } from '@/api/devops'
import { post } from '@/api/client'
import type { GateExemptionRow } from '@/api/types-devops'
import { useAuthStore } from '@/stores/auth'
import { showError } from '@/lib/notify'
const STATUS: Record<number, { label: string; color: string }> = {
  1: { label: '生效', color: 'green' }, 2: { label: '已到期', color: 'default' }, 3: { label: '已撤销', color: 'red' },
}

export default function GateExemption() {
  const { message } = App.useApp()
  const queryClient = useQueryClient()
  const can = useAuthStore((state) => state.can)
  const [filters, setFilters] = useState<Record<string, unknown>>({})
  const [draft, setDraft] = useState<Record<string, unknown>>({})
  const [page, setPage] = useState({ current: 1, size: 30 })

  const query = useQuery({
    queryKey: ['gate-exemptions', filters, page],
    queryFn: () => gateApi.exemptionList({ ...filters, ...page }),
  })

  const revokeMutation = useMutation({
    mutationFn: (id: string) => post<string>(`/ferry-devops/gateExemption/revoke/${id}`),
    onSuccess: (text) => { message.success(text || '已撤销'); void queryClient.invalidateQueries({ queryKey: ['gate-exemptions'] }) },
    onError: showError,
  })

  const columns: TableColumnsType<GateExemptionRow> = [
    { title: '豁免ID', dataIndex: 'id',  render: (v: string) => <span className="ferry-mono" style={{ fontSize: 12 }}>{v}</span> },
    { title: '结果ID', dataIndex: 'gateResultId',  render: (v: string) => <span className="ferry-mono" style={{ fontSize: 12 }}>{v}</span> },
    { title: '运行ID', dataIndex: 'runId',  render: (v: string) => <span className="ferry-mono" style={{ fontSize: 12 }}>{v}</span> },
    { title: '理由', dataIndex: 'reason', render: (v: string) => <span title={v}>{v?.slice(0, 40)}{v?.length > 40 ? '…' : ''}</span> },
    { title: '到期时间', dataIndex: 'expireAt', },
    { title: '关联问题单', dataIndex: 'issueNo',  render: (v: string) => v || '—' },
    { title: '状态', dataIndex: 'status',
      render: (v: number) => <Tag color={STATUS[v]?.color}>{STATUS[v]?.label ?? v}</Tag>,
    },
    { title: '申请时间', dataIndex: 'createTime', },
    { title: '操作', key: 'action',   render: (_, r) => (
      can('devops:gateExemption:apply') && r.status === 1 && (
        <Tooltip title="撤销">
          <Button type="link" size="small" danger icon={<Undo2 size={14} />} onClick={() => revokeMutation.mutate(r.id)} />
        </Tooltip>
      )
    ) },
  ]

  return (
    <>
      <PageHeader title="门禁豁免"
        description="豁免粒度为单条不通过规则，有效期不超过 72 小时，理由不少于 20 字；数据获取失败不可豁免。"
        tag={<RoleTag>共 {query.data?.total ?? 0} 条</RoleTag>}
      />
      <section className="ferry-panel">
        <div className="ferry-toolbar">
          <Input allowClear placeholder="运行ID" style={{ width: 160 }}
            onChange={(e) => setDraft((p) => ({ ...p, runId: e.target.value }))} />
          <Select allowClear placeholder="状态" style={{ width: 110 }} value={draft.status as number | undefined}
            onChange={(v) => setDraft((p) => ({ ...p, status: v }))}
            options={Object.entries(STATUS).map(([k, v]) => ({ value: Number(k), label: v.label }))} />
          <Button type="primary" icon={<Search size={15} />}
            onClick={() => { setFilters(draft); setPage((p) => ({ ...p, current: 1 })) }}>查询</Button>
          <Button icon={<RotateCw size={14} />}
            onClick={() => { setDraft({}); setFilters({}); setPage((p) => ({ ...p, current: 1 })) }}>重置</Button>
        </div>
        <div className="ferry-table-wrap">
          <FerryTable<GateExemptionRow> rowKey="id" columns={columns} dataSource={query.data?.records ?? []}
            loading={query.isFetching} scroll={{ x: 'max-content' }}
            pagination={{ current: page.current, pageSize: page.size, total: query.data?.total ?? 0,
              showSizeChanger: true, pageSizeOptions: ['30', '100', '200'],
              showTotal: (t) => `共 ${t} 条`, onChange: (c, s) => setPage({ current: c, size: s }) }}
          />
        </div>
      </section>
    </>
  )
}
