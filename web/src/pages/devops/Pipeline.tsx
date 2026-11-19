import { useEffect, useRef, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { App, Button, Drawer, Form, Input, Modal, Select, Table, Tag, Tooltip, type TableColumnsType } from 'antd'
import {
  CircleCheck, CirclePause, CirclePlay, OctagonAlert, Pencil, Play, Plus, RotateCw,
  Search, Trash2, XCircle,
} from 'lucide-react'
import PageHeader from '@/components/PageHeader'
import PipelineCanvasEditor from '@/components/PipelineCanvasEditor'
import { appApi, pipelineApi } from '@/api/devops'
import type { AppOption, PipelineStageDef, PrecheckResult, TriggerResult } from '@/api/types-devops'
import { useAuthStore } from '@/stores/auth'
import { showError } from '@/lib/notify'
import { COL } from '@/lib/table'

/** 后端 PipelineVO 比前端共享类型多几个冗余展示字段 */
interface PipelineRowExt {
  id: string
  appId: string
  name: string
  isEnabled: number
  currentVersionNo?: number
  appName?: string
  createTime: string
}

const STATUS_LABEL: Record<number, string> = {
  1: '排队中', 2: '运行中', 3: '阻塞', 4: '成功', 5: '失败', 6: '超时', 7: '已取消', 8: '结果待确认',
}
const CONCLUSION_LABEL: Record<number, string> = { 1: '通过', 2: '不通过', 3: '合并', 4: '复用' }

/** stages -> definition JSON，与后端 PipelineDefinitionDTO 结构一致 */
function buildDefinition(stages: PipelineStageDef[]): string {
  return JSON.stringify({ schemaVersion: 1, stages })
}

/** definition JSON -> stages，解析失败返回空 */
function parseDefinition(json?: string): PipelineStageDef[] {
  if (!json) return []
  try {
    const parsed = JSON.parse(json)
    return Array.isArray(parsed?.stages) ? parsed.stages : []
  } catch {
    return []
  }
}

const METRICS = [
  { key: 'metricRunning', label: '运行中', icon: <RotateCw size={15} />, tone: 'primary' },
  { key: 'metricFailedToday', label: '今日失败', icon: <XCircle size={15} />, tone: 'danger' },
  { key: 'metricGateBlocked', label: '门禁阻塞', icon: <OctagonAlert size={15} />, tone: 'warning' },
  { key: 'metricPendingAdmission', label: '待准入', icon: <CirclePause size={15} />, tone: 'warning' },
  { key: 'metricObserving', label: '发布观察中', icon: <CircleCheck size={15} />, tone: 'success' },
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
  const [filters, setFilters] = useState<Record<string, unknown>>({})
  const [page, setPage] = useState({ current: 1, size: 30 })
  const [stages, setStages] = useState<PipelineStageDef[]>([])
  const originalDefinitionRef = useRef<string>('')

  const query = useQuery({
    queryKey: ['pipelines', filters, page],
    queryFn: () => pipelineApi.pageList({ ...filters, ...page }),
  })
  const appOptionsQuery = useQuery({
    queryKey: ['app-options'],
    queryFn: () => appApi.options(),
  })

  const definitionQuery = useQuery({
    queryKey: ['pipeline-definition', editing?.id],
    queryFn: () => pipelineApi.definition(editing!.id),
    enabled: open && !!editing?.id,
    retry: false,
  })

  const rows = (query.data?.records ?? []) as PipelineRowExt[]

  const invalidate = () => void queryClient.invalidateQueries({ queryKey: ['pipelines'] })

  // 保存：新建时把 stages 转成 definition 随档案一起提交；编辑时先存档案再存定义生成新版本
  const saveMutation = useMutation({
    mutationFn: async (values: Record<string, unknown>) => {
      const definition = stages.length ? buildDefinition(stages) : undefined
      const text = await pipelineApi.save({ ...values, ...(definition ? { definition } : {}) })
      return { text, base: values, definition, isNew: !values.id }
    },
    onSuccess: async ({ text, base, definition, isNew }) => {
      // 编辑场景：仅当阶段定义确实变化时才保存生成新版本（避免"内容未变更"报错）
      if (!isNew && definition && definition !== originalDefinitionRef.current) {
        try {
          await pipelineApi.saveDefinition({ id: base.id, definition, mode: 1, changeNote: '画布编辑阶段配置' })
        } catch (error) {
          showError(error)
        }
      }
      message.success(text || '保存成功')
      setOpen(false)
      invalidate()
    },
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
    originalDefinitionRef.current = ''
    if (record) {
      form.setFieldsValue({ id: record.id, appId: record.appId, name: record.name })
    }
    // 新建与编辑都先留空画布：编辑阶段稍后由 definition 查询异步回填，新建则由用户通过「添加阶段」编排
    setStages([])
    setOpen(true)
  }

  // 编辑时 definition 异步返回后回填画布阶段
  useEffect(() => {
    if (editing && definitionQuery.data?.json) {
      const loaded = parseDefinition(definitionQuery.data.json)
      originalDefinitionRef.current = buildDefinition(loaded)
      if (loaded.length) setStages(loaded)
    }
  }, [editing, definitionQuery.data])

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
    { title: '流水线名称', dataIndex: 'name', minWidth: 220, ellipsis: true },
    { title: '应用', dataIndex: 'appName', width: 140 },
    { title: '当前版本', dataIndex: 'currentVersionNo', width: 100,
      render: (v: number) => v ? `V${v}` : '—' },
    {
      title: '操作', width: 160, fixed: 'right',
      render: (_: unknown, record) => (
        <div style={{ display: 'flex', gap: 2, flexWrap: 'nowrap', whiteSpace: 'nowrap' }}>
          {can('devops:pipeline:trigger') && (
            <Tooltip title="触发运行">
              <Button type="link" size="small" icon={<Play size={15} />} onClick={() => openTrigger(record)} />
            </Tooltip>
          )}
          {can('devops:pipeline:save') && (
            <Tooltip title="编辑">
              <Button type="link" size="small" icon={<Pencil size={15} />} onClick={() => openEditor(record)} />
            </Tooltip>
          )}
          {can('devops:pipeline:enable') && record.isEnabled === 0 && (
            <Tooltip title="启用">
              <Button type="link" size="small" icon={<CirclePlay size={15} />}
                onClick={() => enableMutation.mutate({ id: record.id })} />
            </Tooltip>
          )}
          {can('devops:pipeline:disable') && record.isEnabled === 1 && (
            <Tooltip title="停用">
              <Button type="link" size="small" icon={<CirclePause size={15} />}
                onClick={() => disableMutation.mutate({ id: record.id, reason: '页面停用' })} />
            </Tooltip>
          )}
          {can('devops:pipeline:deleted') && (
            <Tooltip title="删除">
              <Button type="link" size="small" danger icon={<Trash2 size={15} />}
                onClick={() => modal.confirm({
                  title: '删除流水线', content: '确认删除所选流水线？有活动运行时将拒绝删除。', okText: '删除', okButtonProps: { danger: true },
                  onOk: async () => { await deleteMutation.mutateAsync([record.id]) },
                })} />
            </Tooltip>
          )}
        </div>
      ),
    },
  ]

  return (
    <div>
      <PageHeader
        title="流水线管理"
        description="以应用维度查看流水线定义与运行情况，支持版本管理与手动触发"
        extra={
          <div style={{ display: 'flex', gap: 8 }}>
            {can('devops:pipeline:trigger') && <Button type="primary" icon={<Play size={14} />} onClick={() => openTrigger(null)}>手动触发</Button>}
            {can('devops:pipeline:save') && <Button icon={<Plus size={14} />} onClick={() => openEditor(null)}>新建流水线</Button>}
          </div>
        }
      />

      <div className="ferry-stat-grid" style={{ gridTemplateColumns: 'repeat(5, minmax(0, 1fr))', marginBottom: 12 }}>
        {METRICS.map((m) => {
          const active = !!filters[m.key]
          return (
            <button
              key={m.key}
              type="button"
              className="ferry-stat"
              data-tone={m.tone}
              data-active={active}
              aria-pressed={active}
              onClick={() => setFilters((prev) => ({ ...prev, [m.key]: prev[m.key] ? undefined : 1 }))}
            >
              <span className="ferry-stat__stripe" />
              <span className="ferry-stat__label">{m.icon}{m.label}</span>
              <span className="ferry-stat__hint">{active ? '已筛选 · 点击取消' : '点击筛选此类'}</span>
            </button>
          )
        })}
      </div>

      <div className="ferry-panel">
        <div className="ferry-toolbar">
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
          <div className="ferry-toolbar__spacer" />
          {METRICS.filter((m) => filters[m.key]).map((m) => (
            <Tag key={m.key} closable color="blue" onClose={() => setFilters((p) => ({ ...p, [m.key]: undefined }))}>
              {m.label}
            </Tag>
          ))}
          <Button size="small" icon={<RotateCw size={14} />}
            onClick={() => setFilters({})}>重置</Button>
        </div>
        <div className="ferry-table-wrap">
          <Table
            rowKey="id" columns={columns} dataSource={rows} loading={query.isFetching}
            pagination={{
              current: page.current, pageSize: page.size, total: query.data?.total ?? 0,
              showSizeChanger: true, pageSizeOptions: ['30', '100', '200', '500', '1000'],
              onChange: (current, size) => setPage({ current, size }),
            }}
          />
        </div>
      </div>

      <Drawer open={open} title={editing ? `编辑流水线 · ${editing.name}` : '新建流水线'}
        onClose={() => setOpen(false)} width="100%" placement="right"
        destroyOnHidden styles={{ body: { padding: 0, display: 'flex', flexDirection: 'column' } }}
        extra={
          <div style={{ display: 'flex', gap: 8 }}>
            <Button onClick={() => setOpen(false)}>取消</Button>
            <Button type="primary" loading={saveMutation.isPending} onClick={() => form.submit()}>保存</Button>
          </div>
        }>
        <div style={{ display: 'flex', flexDirection: 'column', flex: 1, minHeight: 0 }}>
          <Form form={form} layout="vertical" onFinish={(v) => saveMutation.mutate(v)}
            style={{ padding: '16px 20px 0' }}>
            {editing && <Form.Item name="id" hidden><Input /></Form.Item>}
            <div style={{ fontSize: 13, fontWeight: 600, color: 'var(--text-1)', marginBottom: 12,
              paddingLeft: 8, borderLeft: '3px solid var(--color-primary)' }}>基础信息</div>
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(2, minmax(0, 1fr))', gap: '0 16px' }}>
              <Form.Item name="appId" label="所属应用" rules={[{ required: true, message: '请选择应用' }]}>
                <Select showSearch allowClear placeholder="选择应用"
                  loading={appOptionsQuery.isLoading}
                  options={(appOptionsQuery.data ?? []).map((a: AppOption) => ({ value: a.value, label: `${a.label}（${a.code}）` }))}
                />
              </Form.Item>
              <Form.Item name="name" label="流水线名称" rules={[{ required: true, message: '请输入流水线名称' }]}>
                <Input placeholder="例如 主干流水线" maxLength={100} />
              </Form.Item>
            </div>
          </Form>
          <div style={{ flex: 1, minHeight: 0, padding: '8px 20px 16px' }}>
            <PipelineCanvasEditor value={stages} onChange={setStages} />
          </div>
        </div>
      </Drawer>

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
          <Button block icon={<Search size={14} />} loading={precheckMutation.isPending}
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
