import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { App, Button, Form, Input, Modal, Select, Tag, Tooltip, type TableColumnsType } from 'antd'
import FerryTable from '@/components/FerryTable'
import { CheckCircle2, CornerUpLeft, Play, Plus, RotateCw, Search } from 'lucide-react'
import PageHeader from '@/components/PageHeader'
import { RoleTag } from '@/components/StatusTag'
import { aiApi } from '@/api/devops'
import type { ActionCatalogRow, ActionRecordRow } from '@/api/types-devops'
import { useAuthStore } from '@/stores/auth'
import { showError } from '@/lib/notify'
const LEVEL_LABEL: Record<number, string> = { 0: 'L0', 1: 'L1 · 自动', 2: 'L2 · 待确认', 3: 'L3 · 禁止' }
const LEVEL_COLOR: Record<number, string> = { 0: 'default', 1: 'blue', 2: 'orange', 3: 'red' }
const STATUS_LABEL: Record<number, string> = {
  1: '已提议', 2: '待确认', 3: '已授权', 4: '执行中', 5: '执行成功', 6: '执行失败', 7: '已撤销', 8: '已拒绝',
}
const STATUS_COLOR: Record<number, string> = {
  1: 'default', 2: 'orange', 3: 'blue', 4: 'processing', 5: 'green', 6: 'red', 7: 'default', 8: 'red',
}
const SOURCE_LABEL: Record<number, string> = { 1: '归因', 2: '预案', 3: '异常' }

/** 后端 ActionRecordVO 比共享类型多返回 source/reason/undoDeadline 等字段 */
type ActionRecordExt = ActionRecordRow & {
  source?: number
  reason?: string
  undoDeadline?: string
  result?: string
}

/** 前端 CSV 下载：避免引入后端导出接口 */
function downloadCsv(rows: ActionRecordExt[], fileName: string) {
  const header = ['动作ID', '级别', '来源', '应用ID', '状态', '执行人', '理由', '执行时间']
  const lines = rows.map((r) => [
    r.actionId, (r.level !== undefined ? LEVEL_LABEL[r.level] ?? r.level : ''), (r.source !== undefined ? SOURCE_LABEL[r.source] ?? r.source : ''),
    r.appId ?? '', STATUS_LABEL[r.status] ?? r.status, r.operator, r.reason ?? '', r.executeTime ?? '',
  ].map((v) => `"${String(v).replace(/"/g, '""')}"`).join(','))
  const blob = new Blob(['\ufeff' + [header.join(','), ...lines].join('\n')], { type: 'text/csv;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = fileName
  document.body.appendChild(a)
  a.click()
  document.body.removeChild(a)
  URL.revokeObjectURL(url)
}

export default function AiAction() {
  const { message, modal } = App.useApp()
  const queryClient = useQueryClient()
  const can = useAuthStore((state) => state.can)
  const [form] = Form.useForm()
  const [open, setOpen] = useState(false)
  const [filters, setFilters] = useState<Record<string, unknown>>({})
  const [draft, setDraft] = useState<Record<string, unknown>>({})
  const [page, setPage] = useState({ current: 1, size: 30 })

  const catalogQuery = useQuery({ queryKey: ['ai-action-catalog'], queryFn: () => aiApi.catalog() })
  const recordsQuery = useQuery({
    queryKey: ['ai-action-records', filters, page],
    queryFn: () => aiApi.actionRecords({ ...filters, ...page }),
  })

  const invalidate = () => {
    void queryClient.invalidateQueries({ queryKey: ['ai-action-records'] })
    void queryClient.invalidateQueries({ queryKey: ['ai-action-catalog'] })
  }

  const proposeMutation = useMutation({
    mutationFn: (values: Record<string, unknown>) => aiApi.propose(values),
    onSuccess: () => { message.success('动作已提议'); setOpen(false); invalidate() },
    onError: showError,
  })

  const executeMutation = useMutation({
    mutationFn: (recordId: string) => aiApi.execute({ recordId }),
    onSuccess: (result) => {
      message.success(result.status === 2 ? '该动作需确认影响面后执行' : '动作已执行')
      invalidate()
    },
    onError: showError,
  })

  const confirmMutation = useMutation({
    mutationFn: (recordId: string) => aiApi.confirm(recordId),
    onSuccess: (text) => { message.success(text || '已确认执行'); invalidate() },
    onError: showError,
  })

  const undoMutation = useMutation({
    mutationFn: (recordId: string) => aiApi.undo(recordId),
    onSuccess: (text) => { message.success(text || '撤销成功'); invalidate() },
    onError: showError,
  })

  const catalogColumns: TableColumnsType<ActionCatalogRow> = [
    { title: '动作ID', dataIndex: 'actionId',
      render: (v: string) => <span className="ferry-mono" style={{ fontSize: 12 }}>{v}</span> },
    { title: '名称', dataIndex: 'name' },
    { title: '级别', dataIndex: 'level',
      render: (v: number) => <Tag color={LEVEL_COLOR[v]}>{LEVEL_LABEL[v] ?? v}</Tag> },
    { title: '可撤销', dataIndex: 'isUndoable',
      render: (v: number) => <Tag color={v === 1 ? 'green' : 'default'}>{v === 1 ? '是' : '否'}</Tag> },
    { title: '前置条件', dataIndex: 'precondition',
      render: (v?: string) => <Tooltip title={v}><span>{v || '—'}</span></Tooltip> },
  ]

  const recordColumns: TableColumnsType<ActionRecordExt> = [
    { title: '动作ID', dataIndex: 'actionId',
      render: (v: string) => <span className="ferry-mono" style={{ fontSize: 12 }}>{v}</span> },
    { title: '级别', dataIndex: 'level',
      render: (v: number) => <Tag color={LEVEL_COLOR[v]}>{LEVEL_LABEL[v] ?? v}</Tag> },
    { title: '来源', dataIndex: 'source',
      render: (v: number) => SOURCE_LABEL[v] ?? v },
    { title: '状态', dataIndex: 'status',
      render: (v: number) => <Tag color={STATUS_COLOR[v]}>{STATUS_LABEL[v] ?? v}</Tag> },
    { title: '理由', dataIndex: 'reason', render: (v?: string) => <Tooltip title={v}><span>{v || '—'}</span></Tooltip> },
    { title: '执行时间', dataIndex: 'executeTime',
      render: (v?: string) => <span className="ferry-mono" style={{ fontSize: 12, color: 'var(--text-3)' }}>{v || '—'}</span> },
    { title: '操作', key: 'action',
      render: (_, record) => (
        <div style={{ display: 'flex', alignItems: 'center', gap: 2 }}>
          {record.status === 1 && can('devops:action:execute') && (
            <Tooltip title="执行">
              <Button type="link" size="small" icon={<Play size={14} />}
                onClick={() => executeMutation.mutate(record.id)} />
            </Tooltip>
          )}
          {record.status === 2 && can('devops:action:execute') && (
            <Tooltip title="确认">
              <Button type="link" size="small" icon={<CheckCircle2 size={14} />}
                onClick={() => modal.confirm({
                  title: '确认执行该动作？',
                  content: '请确认已核对动作的影响面（服务、环境、实例、流量占比）。',
                  okText: '确认执行', cancelText: '取消',
                  onOk: () => confirmMutation.mutateAsync(record.id),
                })} />
            </Tooltip>
          )}
          {record.status === 5 && record.undoDeadline && can('devops:action:undo') && (
            <Tooltip title="撤销">
              <Button type="link" size="small" icon={<CornerUpLeft size={14} />}
                onClick={() => undoMutation.mutate(record.id)} />
            </Tooltip>
          )}
        </div>
      ),
    },
  ]

  return (
    <>
      <PageHeader title="AI 动作引擎" description="动作目录为白名单，提案经六项校验后按级别执行：L1 直接执行、L2 需确认、L3 禁止。全程写动作记录。"
        tag={<RoleTag>目录 {catalogQuery.data?.length ?? 0} 项</RoleTag>}
        extra={
          <>
            <Button icon={<RotateCw size={14} />} onClick={() => {
              downloadCsv(recordsQuery.data?.records ?? [], '动作记录.csv')
            }}>导出 CSV</Button>
            {can('devops:action:propose') && (
              <Button type="primary" icon={<Plus size={14} />} onClick={() => { form.resetFields(); setOpen(true) }}>提议动作</Button>
            )}
          </>
        }
      />

      <section className="ferry-panel">
        <div className="ferry-panel__head"><span className="ferry-panel__title">动作目录</span></div>
        <div className="ferry-table-wrap">
          <FerryTable<ActionCatalogRow> rowKey="id" columns={catalogColumns} dataSource={catalogQuery.data ?? []}
            loading={catalogQuery.isLoading} scroll={{ x: 'max-content' }} pagination={false} size="small" />
        </div>
      </section>

      <section className="ferry-panel" style={{ marginTop: 16 }}>
        <div className="ferry-toolbar">
          <Input allowClear prefix={<Search size={15} />} placeholder="动作ID" style={{ width: 180 }}
            value={draft.actionId as string}
            onChange={(e) => setDraft((prev) => ({ ...prev, actionId: e.target.value }))}
            onPressEnter={() => { setFilters(draft); setPage((p) => ({ ...p, current: 1 })) }} />
          <Select allowClear placeholder="状态" style={{ width: 130 }} value={draft.status as number}
            onChange={(value) => setDraft((prev) => ({ ...prev, status: value }))}
            options={Object.entries(STATUS_LABEL).map(([k, v]) => ({ value: Number(k), label: v }))} />
          <Button type="primary" icon={<Search size={15} />}
            onClick={() => { setFilters(draft); setPage((p) => ({ ...p, current: 1 })) }}>查询</Button>
          <Button icon={<RotateCw size={15} />}
            onClick={() => { setDraft({}); setFilters({}); setPage((p) => ({ ...p, current: 1 })) }}>重置</Button>
        </div>
        <div className="ferry-table-wrap">
          <FerryTable<ActionRecordExt> rowKey="id" columns={recordColumns} dataSource={(recordsQuery.data?.records ?? []) as ActionRecordExt[]}
            loading={recordsQuery.isLoading || recordsQuery.isFetching} scroll={{ x: 'max-content' }}
            pagination={{
              current: page.current, pageSize: page.size, total: recordsQuery.data?.total ?? 0,
              showSizeChanger: true, pageSizeOptions: ['30', '100', '200'],
              showTotal: (total) => `共 ${total} 条`, onChange: (current, size) => setPage({ current, size }),
            }} />
        </div>
      </section>

      <Modal open={open} title="提议 AI 动作" okText="提交提议" cancelText="取消" width={560}
        confirmLoading={proposeMutation.isPending} onCancel={() => setOpen(false)}
        onOk={async () => {
          const values = await form.validateFields()
          proposeMutation.mutate({ ...values, evidence: values.evidence ? String(values.evidence).split(/[\n,，]/).filter(Boolean) : [] })
        }} destroyOnHidden>
        <Form form={form} layout="vertical" requiredMark="optional" initialValues={{ source: 1 }}>
          <Form.Item name="actionId" label="动作ID" rules={[{ required: true, message: '请选择动作' }]}>
            <Select showSearch placeholder="从动作目录选择"
              options={(catalogQuery.data ?? []).map((c) => ({ value: c.actionId, label: `${c.actionId} · ${c.name}` }))} />
          </Form.Item>
          <Form.Item name="appId" label="应用ID" rules={[{ required: true, message: '请输入应用ID' }]}>
            <Input placeholder="应用id" />
          </Form.Item>
          <Form.Item name="source" label="触发来源" rules={[{ required: true }]}>
            <Select options={[{ value: 1, label: '归因' }, { value: 2, label: '预案' }, { value: 3, label: '异常' }]} />
          </Form.Item>
          <Form.Item name="reason" label="理由">
            <Input.TextArea rows={2} placeholder="说明提议该动作的原因" />
          </Form.Item>
          <Form.Item name="evidence" label="证据定位" extra="每行或逗号分隔一条，空定位会被服务端剔除">
            <Input.TextArea rows={3} placeholder={'log:run.log:120\nchange:OrderService.java:45'} />
          </Form.Item>
          <Form.Item name="params" label="参数JSON">
            <Input.TextArea rows={2} placeholder='{"runId": 123}' />
          </Form.Item>
        </Form>
      </Modal>
    </>
  )
}
