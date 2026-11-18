import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { App, Button, Form, Input, Modal, Select, Table, Tag, type TableColumnsType } from 'antd'
import {
  CircleCheck, CirclePause, ExternalLink, OctagonAlert, Pencil, Play, Plus, RotateCw,
  Search, Trash2, XCircle,
} from 'lucide-react'
import PageHeader from '@/components/PageHeader'
import { pipelineApi } from '@/api/devops'
import type { PrecheckResult, TriggerResult } from '@/api/types-devops'
import { useAuthStore } from '@/stores/auth'
import { showError } from '@/lib/notify'
import { COL } from '@/lib/table'

/** 后端 PipelineVO 比前端共享类型多几个冗余展示字段 */
interface PipelineRowExt {
  id: string
  appId: string
  name: string
  isEnabled: number
  currentVersionId?: string
  currentVersionNo?: number
  ownerId?: string
  lastRunId?: string
  lastRunStatus?: number
  lastBranch?: string
  lastRunTime?: string
  appName?: string
  createTime: string
}

const STATUS_LABEL: Record<number, string> = {
  1: '排队中', 2: '运行中', 3: '阻塞', 4: '成功', 5: '失败', 6: '超时', 7: '已取消', 8: '结果待确认',
}
const CONCLUSION_LABEL: Record<number, string> = { 1: '通过', 2: '不通过', 3: '合并', 4: '复用' }
const METRICS = [
  { key: 'metricRunning', label: '运行中', icon: <RotateCw size={16} /> },
  { key: 'metricFailedToday', label: '今日失败', icon: <XCircle size={16} /> },
  { key: 'metricGateBlocked', label: '门禁阻塞', icon: <OctagonAlert size={16} /> },
  { key: 'metricPendingAdmission', label: '待准入', icon: <CirclePause size={16} /> },
  { key: 'metricObserving', label: '发布观察中', icon: <CircleCheck size={16} /> },
] as const

function newRequestId() {
  return `${Date.now()}-${Math.random().toString(16).slice(2)}`
}

export default function Pipeline() {
  const { message, modal } = App.useApp()
  const queryClient = useQueryClient()
  const can = useAuthStore((state) => state.can)
  const [form] = Form.useForm()
  const [triggerForm] = Form.useForm()
  const [open, setOpen] = useState(false)
  const [triggerOpen, setTriggerOpen] = useState(false)
  const [editing, setEditing] = useState<PipelineRowExt | null>(null)
  const [triggerRow, setTriggerRow] = useState<PipelineRowExt | null>(null)
  const [precheck, setPrecheck] = useState<PrecheckResult | null>(null)
  const [filters, setFilters] = useState<Record<string, unknown>>({ metricRunning: 1 })
  const [page, setPage] = useState({ current: 1, size: 30 })

  const query = useQuery({
    queryKey: ['pipelines', filters, page],
    queryFn: () => pipelineApi.pageList({ ...filters, ...page }),
  })
  const rows = (query.data?.records ?? []) as PipelineRowExt[]

  const invalidate = () => void queryClient.invalidateQueries({ queryKey: ['pipelines'] })

  const saveMutation = useMutation({
    mutationFn: (values: Record<string, unknown>) => pipelineApi.save(values),
    onSuccess: (text) => { message.success(text || '保存成功'); setOpen(false); invalidate() },
    onError: showError,
  })
  const deleteMutation = useMutation({
    mutationFn: (ids: string[]) => pipelineApi.deleted(ids),
    onSuccess: (text) => { message.success(text || '删除成功'); invalidate() },
    onError: showError,
  })
  const enableMutation = useMutation({
    mutationFn: (body: Record<string, unknown>) => pipelineApi.enable(body),
    onSuccess: (text) => { message.success(text || '操作成功'); invalidate() },
    onError: showError,
  })
  const disableMutation = useMutation({
    mutationFn: (body: Record<string, unknown>) => pipelineApi.disable(body),
    onSuccess: (text) => { message.success(text || '操作成功'); invalidate() },
    onError: showError,
  })
  const precheckMutation = useMutation({
    mutationFn: (body: Record<string, unknown>) => pipelineApi.triggerPrecheck(body),
    onSuccess: (result) => { setPrecheck(result); if (!result.canTrigger) message.warning('预校验未全部通过') },
    onError: showError,
  })
  const triggerMutation = useMutation({
    mutationFn: (body: Record<string, unknown>) => pipelineApi.trigger(body, newRequestId()),
    onSuccess: (result: TriggerResult) => {
      message.success(`触发成功：${result.result}`)
      setTriggerOpen(false)
      setPrecheck(null)
      invalidate()
    },
    onError: showError,
  })

  const openEditor = (record: PipelineRowExt | null) => {
    setEditing(record)
    form.resetFields()
    if (record) {
      form.setFieldsValue({ id: record.id, appId: record.appId, name: record.name, ownerId: record.ownerId })
    }
    setOpen(true)
  }

  const openTrigger = (record: PipelineRowExt | null) => {
    setTriggerRow(record)
    setPrecheck(null)
    triggerForm.resetFields()
    if (record) {
      triggerForm.setFieldsValue({ appId: record.appId, pipelineId: record.id })
    }
    setTriggerOpen(true)
  }

  const submitTrigger = () => {
    const values = triggerForm.getFieldsValue()
    const body = { ...values, appId: values.appId ?? triggerRow?.appId, pipelineId: values.pipelineId ?? triggerRow?.id }
    triggerMutation.mutate(body)
  }

  const columns: TableColumnsType<PipelineRowExt> = [
    { title: 'ID', dataIndex: 'id', width: COL.code, render: (v: string) => <span className="ferry-mono">{v}</span> },
    { title: '流水线名称', dataIndex: 'name' },
    { title: '应用', dataIndex: 'appName', width: 140 },
    { title: '当前版本', dataIndex: 'currentVersionId', width: 100,
      render: (v: string, r) => v ? `V${r.currentVersionNo ?? '—'}` : '—' },
    { title: '最近状态', dataIndex: 'lastRunStatus', width: COL.status,
      render: (v: number) => v == null ? '—' : <Tag color={v === 4 ? 'green' : v === 5 || v === 6 ? 'red' : v === 3 ? 'orange' : 'blue'}>{STATUS_LABEL[v] ?? v}</Tag> },
    { title: '最近分支', dataIndex: 'lastBranch', width: 120 },
    { title: '最近运行时间', dataIndex: 'lastRunTime', width: COL.time },
    {
      title: '操作', width: COL.actionWide, fixed: 'right',
      render: (_: unknown, record) => (
        <div style={{ display: 'flex', gap: 4, flexWrap: 'wrap' }}>
          {can('devops:run:detail') && (
            <Button type="link" size="small" icon={<ExternalLink />}
              onClick={() => { window.location.hash = `#/devops/run/${record.lastRunId ?? ''}` }}>运行</Button>
          )}
          {can('devops:pipeline:trigger') && (
            <Button type="link" size="small" icon={<Play />} onClick={() => openTrigger(record)}>触发</Button>
          )}
          {can('devops:pipeline:save') && (
            <Button type="link" size="small" icon={<Pencil />} onClick={() => openEditor(record)}>编辑</Button>
          )}
          {can('devops:pipeline:enable') && record.isEnabled === 0 && (
            <Button type="link" size="small" onClick={() => enableMutation.mutate({ id: record.id })}>启用</Button>
          )}
          {can('devops:pipeline:disable') && record.isEnabled === 1 && (
            <Button type="link" size="small" onClick={() => disableMutation.mutate({ id: record.id, reason: '页面停用' })}>停用</Button>
          )}
          {can('devops:pipeline:deleted') && (
            <Button type="link" size="small" danger icon={<Trash2 />}
              onClick={() => modal.confirm({
                title: '删除流水线', content: '确认删除所选流水线？有活动运行时将拒绝删除。', okText: '删除', okButtonProps: { danger: true },
                onOk: async () => { await deleteMutation.mutateAsync([record.id]) },
              })}>删除</Button>
          )}
        </div>
      ),
    },
  ]

  return (
    <div>
      <PageHeader
        title="流水线管理"
        description="以应用维度查看流水线定义与运行情况，支持版本管理、模板派生与手动触发"
        extra={
          <div style={{ display: 'flex', gap: 8 }}>
            {can('devops:pipeline:trigger') && <Button type="primary" icon={<Play />} onClick={() => openTrigger(null)}>手动触发</Button>}
            {can('devops:pipeline:save') && <Button icon={<Plus />} onClick={() => openEditor(null)}>新建流水线</Button>}
          </div>
        }
      />

      <div className="ferry-panel" style={{ marginBottom: 12, display: 'flex', gap: 12, flexWrap: 'wrap' }}>
        {METRICS.map((m) => (
          <button
            key={m.key}
            type="button"
            className={`ferry-metric ${filters[m.key] ? 'is-active' : ''}`}
            onClick={() => setFilters((prev) => ({ ...prev, [m.key]: prev[m.key] ? undefined : 1 }))}
            style={{ cursor: 'pointer', display: 'flex', alignItems: 'center', gap: 6, padding: '8px 14px', border: '1px solid var(--border-1)', borderRadius: 6 }}
          >
            {m.icon}
            <span>{m.label}</span>
          </button>
        ))}
      </div>

      <div className="ferry-panel">
        <div style={{ display: 'flex', gap: 8, marginBottom: 12 }}>
          <Input prefix={<Search size={14} />} allowClear placeholder="流水线名称"
            value={filters.name as string ?? ''}
            onChange={(e) => setFilters((p) => ({ ...p, name: e.target.value || undefined }))}
            style={{ width: 240 }}
          />
          <Select mode="multiple" allowClear placeholder="运行状态" style={{ width: 240 }}
            value={filters.statuses as number[] ?? []}
            onChange={(v) => setFilters((p) => ({ ...p, statuses: v.length ? v : undefined }))}
            options={Object.entries(STATUS_LABEL).map(([k, v]) => ({ value: Number(k), label: v }))}
          />
        </div>
        <Table
          rowKey="id" columns={columns} dataSource={rows} loading={query.isFetching}
          pagination={{
            current: page.current, pageSize: page.size, total: query.data?.total ?? 0,
            showSizeChanger: true, pageSizeOptions: ['30', '100', '200', '500', '1000'],
            onChange: (current, size) => setPage({ current, size }),
          }}
        />
      </div>

      <Modal open={open} title={editing ? '编辑流水线' : '新建流水线'} onCancel={() => setOpen(false)}
        onOk={() => form.submit()} okText="保存" destroyOnHidden>
        <Form form={form} layout="vertical" onFinish={(v) => saveMutation.mutate(v)}>
          {editing && <Form.Item name="id" hidden><Input /></Form.Item>}
          <Form.Item name="appId" label="应用id" rules={[{ required: true, message: '请输入应用id' }]}>
            <Input placeholder="应用id" />
          </Form.Item>
          <Form.Item name="name" label="流水线名称" rules={[{ required: true, message: '请输入流水线名称' }]}>
            <Input placeholder="流水线名称" />
          </Form.Item>
          <Form.Item name="ownerId" label="负责人id"><Input placeholder="负责人用户id" /></Form.Item>
        </Form>
      </Modal>

      <Modal open={triggerOpen} title="手动触发" onCancel={() => setTriggerOpen(false)}
        onOk={submitTrigger} okText="校验并运行" confirmLoading={triggerMutation.isPending || precheckMutation.isPending}
        width={620} destroyOnHidden>
        <Form form={triggerForm} layout="vertical">
          <Form.Item name="appId" label="目标应用" rules={[{ required: true, message: '请选择应用' }]}>
            <Input placeholder="应用id（由列表带入）" disabled={!!triggerRow} />
          </Form.Item>
          <Form.Item name="pipelineId" label="流水线" hidden><Input /></Form.Item>
          <Form.Item name="branch" label="分支" rules={[{ required: true, message: '请输入分支' }]}>
            <Input placeholder="如 main" />
          </Form.Item>
          <Form.Item name="commitId" label="提交 ID / Tag / Ref" rules={[{ required: true, message: '请输入提交' }]}>
            <Input placeholder="7–40 位十六进制，或 Git ref" />
          </Form.Item>
          <Form.Item name="refType" label="引用类型" initialValue={1}>
            <Select options={[{ value: 1, label: 'Commit' }, { value: 2, label: 'Tag/Ref' }]} />
          </Form.Item>
          <Form.Item name="note" label="触发说明"><Input.TextArea rows={2} placeholder="选填" /></Form.Item>
          <Button block icon={<Search />} loading={precheckMutation.isPending}
            onClick={() => precheckMutation.mutate(triggerForm.getFieldsValue())}>预校验</Button>
          {precheck && (
            <div style={{ marginTop: 12 }}>
              {precheck.items?.map((item) => (
                <div key={item.type} style={{ display: 'flex', gap: 8, marginBottom: 4 }}>
                  <Tag color={item.conclusion === 1 ? 'green' : item.conclusion === 2 ? 'red' : 'blue'}>
                    {CONCLUSION_LABEL[item.conclusion] ?? item.conclusion}
                  </Tag>
                  <span>{item.detail}</span>
                </div>
              ))}
              <div style={{ marginTop: 8, fontWeight: 500 }}>
                {precheck.canTrigger ? '校验通过，可触发运行' : '预校验未通过，不可触发'}
              </div>
            </div>
          )}
        </Form>
      </Modal>
    </div>
  )
}
