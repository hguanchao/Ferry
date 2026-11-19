import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { App, Button, Form, Input, Modal, Select, Space, Table, Tag, Tooltip, type TableColumnsType } from 'antd'
import { RotateCw, Search, UserPlus, UserRoundCheck } from 'lucide-react'
import PageHeader from '@/components/PageHeader'
import { RoleTag } from '@/components/StatusTag'
import { gateApi, appApi } from '@/api/devops'
import { post } from '@/api/client'
import type { TechDebtRow } from '@/api/types-devops'
import { useAuthStore } from '@/stores/auth'
import { showError } from '@/lib/notify'
import { COL } from '@/lib/table'

const SEVERITY: Record<number, { label: string; color: string }> = {
  1: { label: '阻断', color: 'red' }, 2: { label: '高危', color: 'orange' },
  3: { label: '中危', color: 'gold' }, 4: { label: '低危', color: 'blue' },
}
const STATUS: Record<number, { label: string; color: string }> = {
  1: { label: '新增', color: 'red' }, 2: { label: '遗留', color: 'orange' },
  3: { label: '已修复', color: 'green' }, 4: { label: '误报', color: 'default' },
}

export default function TechDebt() {
  const { message } = App.useApp()
  const queryClient = useQueryClient()
  const can = useAuthStore((state) => state.can)
  const [form] = Form.useForm()
  const [filters, setFilters] = useState<Record<string, unknown>>({})
  const [draft, setDraft] = useState<Record<string, unknown>>({})
  const [page, setPage] = useState({ current: 1, size: 30 })
  const [assignRow, setAssignRow] = useState<TechDebtRow | null>(null)
  const [fpRow, setFpRow] = useState<TechDebtRow | null>(null)

  const query = useQuery({
    queryKey: ['tech-debt', filters, page],
    queryFn: () => gateApi.techDebtPage({ ...filters, ...page }),
  })
  const appsQuery = useQuery({ queryKey: ['app-options'], queryFn: () => appApi.options() })

  const assignMutation = useMutation({
    mutationFn: (values: Record<string, unknown>) => post<string>('/ferry-devops/techDebt/assign', values),
    onSuccess: (text) => { message.success(text || '指派成功'); setAssignRow(null); void queryClient.invalidateQueries({ queryKey: ['tech-debt'] }) },
    onError: showError,
  })
  const fpMutation = useMutation({
    mutationFn: (values: Record<string, unknown>) => post<string>('/ferry-devops/techDebt/falsePositive/apply', values),
    onSuccess: (text) => { message.success(text || '已提交'); setFpRow(null); void queryClient.invalidateQueries({ queryKey: ['tech-debt'] }) },
    onError: showError,
  })

  const columns: TableColumnsType<TechDebtRow> = [
    { title: '问题ID', dataIndex: 'id', width: 130, render: (v: string) => <span className="ferry-mono" style={{ fontSize: 12 }}>{v}</span> },
    { title: '指纹', dataIndex: 'fingerprint', width: 160, render: (v: string) => <Tooltip title={v}><span className="ferry-mono" style={{ fontSize: 11 }}>{v?.slice(0, 16)}…</span></Tooltip> },
    { title: '级别', dataIndex: 'severity', width: 90,
      render: (v: number) => <Tag color={SEVERITY[v]?.color}>{SEVERITY[v]?.label ?? v}</Tag>,
    },
    { title: '状态', dataIndex: 'status', width: 90,
      render: (v: number) => <Tag color={STATUS[v]?.color}>{STATUS[v]?.label ?? v}</Tag>,
    },
    { title: '文件路径', dataIndex: 'filePath', render: (v: string | undefined, r) => <Tooltip title={v ?? r.file_path}><span className="ferry-mono" style={{ fontSize: 11 }}>{(v ?? r.file_path)?.slice(-40) || '—'}</span></Tooltip> },
    { title: '责任人', dataIndex: 'assignee', width: 110, render: (v: string) => v || <span style={{ color: 'var(--text-3)' }}>未指派</span> },
    { title: '首次出现', dataIndex: 'firstSeen', width: COL.time },
    { title: '操作', key: 'action', width: COL.action, fixed: 'right', render: (_, r) => (
      <Space size={0}>
        {can('devops:techDebt:assign') && (
          <Tooltip title="指派">
            <Button type="link" size="small" icon={<UserPlus size={14} />} onClick={() => { form.resetFields(); setAssignRow(r) }} />
          </Tooltip>
        )}
        {can('devops:techDebt:falsePositive') && r.status !== 4 && (
          <Tooltip title="误报">
            <Button type="link" size="small" icon={<UserRoundCheck size={14} />} onClick={() => { form.resetFields(); setFpRow(r) }} />
          </Tooltip>
        )}
      </Space>
    ) },
  ]

  return (
    <>
      <PageHeader title="技术债看板"
        description="存量问题清单不参与门禁判定。支持按级别、规则类型、应用与时间筛选，指派责任人与误报申请（需审批）。"
        tag={<RoleTag>共 {query.data?.total ?? 0} 项</RoleTag>}
      />
      <section className="ferry-panel">
        <div className="ferry-toolbar">
          <Select allowClear placeholder="应用" style={{ width: 150 }} value={draft.appId as string | undefined}
            onChange={(v) => setDraft((p) => ({ ...p, appId: v }))}
            options={(appsQuery.data ?? []).map((a) => ({ value: a.value, label: a.label }))} />
          <Select allowClear placeholder="级别" style={{ width: 100 }} value={draft.severity as number | undefined}
            onChange={(v) => setDraft((p) => ({ ...p, severity: v }))}
            options={Object.entries(SEVERITY).map(([k, v]) => ({ value: Number(k), label: v.label }))} />
          <Select allowClear placeholder="状态" style={{ width: 100 }} value={draft.status as number | undefined}
            onChange={(v) => setDraft((p) => ({ ...p, status: v }))}
            options={Object.entries(STATUS).map(([k, v]) => ({ value: Number(k), label: v.label }))} />
          <Input allowClear placeholder="指纹" style={{ width: 160 }}
            onChange={(e) => setDraft((p) => ({ ...p, fingerprint: e.target.value }))} />
          <Button type="primary" icon={<Search size={15} />}
            onClick={() => { setFilters(draft); setPage((p) => ({ ...p, current: 1 })) }}>查询</Button>
          <Button icon={<RotateCw size={14} />}
            onClick={() => { setDraft({}); setFilters({}); setPage((p) => ({ ...p, current: 1 })) }}>重置</Button>
          <Button icon={<RotateCw size={14} />}
            onClick={async () => {
              const csv = await post<string>('/ferry-devops/techDebt/export', filters)
              const blob = new Blob([csv], { type: 'text/csv' })
              const url = URL.createObjectURL(blob); const a = document.createElement('a')
              a.href = url; a.download = 'tech-debt.csv'; a.click(); URL.revokeObjectURL(url)
            }}>导出</Button>
        </div>
        <div className="ferry-table-wrap">
          <Table<TechDebtRow> rowKey="id" columns={columns} dataSource={query.data?.records ?? []}
            loading={query.isFetching} scroll={{ x: 'max-content' }}
            pagination={{ current: page.current, pageSize: page.size, total: query.data?.total ?? 0,
              showSizeChanger: true, pageSizeOptions: ['30', '100', '200'],
              showTotal: (t) => `共 ${t} 条`, onChange: (c, s) => setPage({ current: c, size: s }) }}
          />
        </div>
      </section>

      <Modal open={!!assignRow} title="指派修复责任人" okText="确定" cancelText="取消" width={420}
        confirmLoading={assignMutation.isPending} onCancel={() => setAssignRow(null)}
        onOk={async () => {
          const values = await form.validateFields()
          assignMutation.mutate({ id: assignRow?.id, assigneeId: values.assigneeId })
        }} destroyOnHidden>
        <Form form={form} layout="vertical">
          <Form.Item name="assigneeId" label="责任人用户id" rules={[{ required: true }]}><Input placeholder="用户id" /></Form.Item>
        </Form>
      </Modal>

      <Modal open={!!fpRow} title="误报申请" okText="提交" cancelText="取消" width={460}
        confirmLoading={fpMutation.isPending} onCancel={() => setFpRow(null)}
        onOk={async () => {
          const values = await form.validateFields()
          fpMutation.mutate({ id: fpRow?.id, reason: values.reason })
        }} destroyOnHidden>
        <Form form={form} layout="vertical">
          <Form.Item label="问题"><span className="ferry-mono">{fpRow?.fingerprint}</span></Form.Item>
          <Form.Item name="reason" label="误报理由" rules={[{ required: true, message: '请填写理由' }]}>
            <Input.TextArea rows={4} placeholder="说明为何判定为误报，审批通过后写入基线排除集" />
          </Form.Item>
        </Form>
      </Modal>
    </>
  )
}
