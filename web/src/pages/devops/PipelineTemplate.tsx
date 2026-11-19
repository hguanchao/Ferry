import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { App, Button, Form, Input, Modal, Select, Table, Tag, Tooltip, type TableColumnsType } from 'antd'
import { CirclePause, CirclePlay, CopyPlus, Pencil, Plus, Search, Trash2 } from 'lucide-react'
import PageHeader from '@/components/PageHeader'
import { templateApi } from '@/api/devops'
import { useAuthStore } from '@/stores/auth'
import { showError } from '@/lib/notify'
import { COL } from '@/lib/table'

interface TemplateRowExt {
  id: string
  scope: number
  teamId?: string
  name: string
  scene?: string
  isEnabled: number
  derivedCount: number
  currentVersionNo?: number
  stageCount?: number
  variableCount?: number
  createTime: string
}
interface DerivationRow {
  id: string
  pipelineId: string
  pipelineName?: string
  appName?: string
  isBehind: number
  lastMergeTime?: string
}

const SCOPE_LABEL: Record<number, string> = { 1: '组织级', 2: '团队级' }

export default function PipelineTemplate() {
  const { message, modal } = App.useApp()
  const queryClient = useQueryClient()
  const can = useAuthStore((state) => state.can)
  const [form] = Form.useForm()
  const [open, setOpen] = useState(false)
  const [editing, setEditing] = useState<TemplateRowExt | null>(null)
  const [scope, setScope] = useState<number>(1)
  const [filters, setFilters] = useState<Record<string, unknown>>({})
  const [page, setPage] = useState({ current: 1, size: 30 })
  const [deriveOpen, setDeriveOpen] = useState(false)
  const [deriveTemplate, setDeriveTemplate] = useState<TemplateRowExt | null>(null)
  const [deriveForm] = Form.useForm()

  const query = useQuery({
    queryKey: ['templates', scope, filters, page],
    queryFn: () => templateApi.pageList({ ...filters, scope, ...page }),
  })
  const rows = (query.data?.records ?? []) as TemplateRowExt[]

  const derivationsQuery = useQuery({
    queryKey: ['derivations', deriveTemplate?.id],
    queryFn: () => templateApi.derivations(deriveTemplate!.id),
    enabled: !!deriveTemplate,
  })

  const invalidate = () => void queryClient.invalidateQueries({ queryKey: ['templates'] })

  const saveMutation = useMutation({
    mutationFn: (values: Record<string, unknown>) => templateApi.save(values),
    onSuccess: (text) => { message.success(text || '保存成功'); setOpen(false); invalidate() },
    onError: showError,
  })
  const deleteMutation = useMutation({
    mutationFn: (ids: string[]) => templateApi.deleted(ids),
    onSuccess: (text) => { message.success(text || '删除成功'); invalidate() },
    onError: showError,
  })
  const enableMutation = useMutation({
    mutationFn: (id: string) => templateApi.enable(id),
    onSuccess: (text) => { message.success(text || '启用成功'); invalidate() },
    onError: showError,
  })
  const disableMutation = useMutation({
    mutationFn: (body: Record<string, unknown>) => templateApi.disable(body),
    onSuccess: (text) => { message.success(text || '停用成功'); invalidate() },
    onError: showError,
  })
  const deriveMutation = useMutation({
    mutationFn: (body: Record<string, unknown>) => pipelineApiCreate(body),
    onSuccess: () => { message.success('已从模板派生流水线'); setDeriveOpen(false); invalidate() },
    onError: showError,
  })

  // 派生走流水线新建接口，携带 templateId/templateVersionId
  const pipelineApiCreate = (body: Record<string, unknown>) =>
    import('@/api/devops').then((m) => m.pipelineApi.save(body))

  const openEditor = (record: TemplateRowExt | null) => {
    setEditing(record)
    form.resetFields()
    if (record) {
      form.setFieldsValue({ id: record.id, scope: record.scope, teamId: record.teamId, name: record.name, scene: record.scene })
    } else {
      form.setFieldsValue({ scope })
    }
    setOpen(true)
  }

  const openDerive = (record: TemplateRowExt) => {
    setDeriveTemplate(record)
    deriveForm.resetFields()
    deriveForm.setFieldsValue({ templateId: record.id, templateVersionId: record.currentVersionNo })
    setDeriveOpen(true)
  }

  const columns: TableColumnsType<TemplateRowExt> = [
    { title: '模板名称', dataIndex: 'name' },
    { title: '级别', dataIndex: 'scope', width: 100,
      render: (v: number) => <Tag color={v === 1 ? 'blue' : 'green'}>{SCOPE_LABEL[v] ?? v}</Tag> },
    { title: '适用场景', dataIndex: 'scene', width: 180 },
    { title: '版本', dataIndex: 'currentVersionNo', width: 80, render: (v: number) => v ? `V${v}` : '—' },
    { title: '阶段数', dataIndex: 'stageCount', width: 80 },
    { title: '变量数', dataIndex: 'variableCount', width: 80 },
    { title: '被派生数', dataIndex: 'derivedCount', width: 90 },
    { title: '状态', dataIndex: 'isEnabled', width: 90,
      render: (v: number) => <Tag color={v === 1 ? 'green' : 'default'}>{v === 1 ? '启用' : '停用'}</Tag> },
    {
      title: '操作', width: COL.actionWide, fixed: 'right',
      render: (_: unknown, record) => (
        <div style={{ display: 'flex', gap: 4, flexWrap: 'wrap' }}>
          {can('devops:template:save') && (
            <Tooltip title="派生">
              <Button type="link" size="small" icon={<CopyPlus />} onClick={() => openDerive(record)} />
            </Tooltip>
          )}
          {can('devops:template:save') && (
            <Tooltip title="编辑">
              <Button type="link" size="small" icon={<Pencil />} onClick={() => openEditor(record)} />
            </Tooltip>
          )}
          {can('devops:template:enable') && record.isEnabled === 0 && (
            <Tooltip title="启用">
              <Button type="link" size="small" icon={<CirclePlay />} onClick={() => enableMutation.mutate(record.id)} />
            </Tooltip>
          )}
          {can('devops:template:disable') && record.isEnabled === 1 && (
            <Tooltip title="停用">
              <Button type="link" size="small" icon={<CirclePause />} onClick={() => disableMutation.mutate({ templateId: record.id, changeNote: '页面停用' })} />
            </Tooltip>
          )}
          {can('devops:template:deleted') && (
            <Tooltip title="删除">
              <Button type="link" size="small" danger icon={<Trash2 />}
                onClick={() => modal.confirm({
                  title: '删除模板', content: '存在派生流水线的模板不可删除，仅可停用。确认删除？',
                  okText: '删除', okButtonProps: { danger: true },
                  onOk: async () => { await deleteMutation.mutateAsync([record.id]) },
                })} />
            </Tooltip>
          )}
        </div>
      ),
    },
  ]

  const derivationColumns: TableColumnsType<DerivationRow> = [
    { title: '流水线', dataIndex: 'pipelineName' },
    { title: '应用', dataIndex: 'appName' },
    { title: '状态', dataIndex: 'isBehind', width: 120,
      render: (v: number) => v === 1 ? <Tag color="orange">有新版本模板</Tag> : <Tag color="green">已同步</Tag> },
    { title: '最近合并', dataIndex: 'lastMergeTime', width: COL.time },
  ]

  return (
    <div>
      <PageHeader
        title="流水线模板"
        description="组织级与团队级模板的定义、版本、派生关系管理；派生为副本，模板更新不自动影响已派生流水线"
        extra={can('devops:template:save') && (
          <Button type="primary" icon={<Plus />} onClick={() => openEditor(null)}>新建模板</Button>
        )}
      />
      <div className="ferry-panel">
        <div style={{ display: 'flex', gap: 8, marginBottom: 12, alignItems: 'center' }}>
          <Select value={scope} onChange={(v) => setScope(v)} style={{ width: 140 }}
            options={[{ value: 1, label: '组织级' }, { value: 2, label: '团队级' }]} />
          <Input prefix={<Search size={14} />} allowClear placeholder="模板名称" style={{ width: 220 }}
            value={filters.name as string ?? ''}
            onChange={(e) => setFilters((p) => ({ ...p, name: e.target.value || undefined }))} />
        </div>
        <Table rowKey="id" columns={columns} dataSource={rows} loading={query.isFetching}
          pagination={{
            current: page.current, pageSize: page.size, total: query.data?.total ?? 0,
            showSizeChanger: true, onChange: (current, size) => setPage({ current, size }),
          }} />
      </div>

      <Modal open={open} title={editing ? '编辑模板' : '新建模板'} onCancel={() => setOpen(false)}
        onOk={() => form.submit()} okText="保存" destroyOnHidden>
        <Form form={form} layout="vertical" onFinish={(v) => saveMutation.mutate(v)}>
          {editing && <Form.Item name="id" hidden><Input /></Form.Item>}
          <Form.Item name="scope" label="级别" rules={[{ required: true }]}>
            <Select options={[{ value: 1, label: '组织级' }, { value: 2, label: '团队级' }]} />
          </Form.Item>
          <Form.Item name="teamId" label="团队id（团队级必填）"><Input /></Form.Item>
          <Form.Item name="name" label="模板名称" rules={[{ required: true, message: '请输入模板名称' }]}><Input /></Form.Item>
          <Form.Item name="scene" label="适用场景"><Input /></Form.Item>
        </Form>
      </Modal>

      <Modal open={deriveOpen} title="从模板派生流水线" onCancel={() => setDeriveOpen(false)}
        onOk={() => deriveMutation.mutate(deriveForm.getFieldsValue())} okText="确认派生"
        confirmLoading={deriveMutation.isPending} width={720} destroyOnHidden>
        <Form form={deriveForm} layout="vertical">
          <Form.Item name="templateId" label="模板id" hidden><Input /></Form.Item>
          <Form.Item name="appId" label="目标应用id" rules={[{ required: true, message: '请输入应用id' }]}><Input /></Form.Item>
          <Form.Item name="name" label="流水线名称" rules={[{ required: true, message: '请输入名称' }]}><Input /></Form.Item>
          <Form.Item name="templateVersionId" label="模板版本id"><Input /></Form.Item>
        </Form>
        {deriveTemplate && (
          <div style={{ marginTop: 12, color: 'var(--text-2)', fontSize: 12 }}>
            模板「{deriveTemplate.name}」的当前定义将被复制为独立流水线，占位变量可在创建后替换。
          </div>
        )}
        <div style={{ marginTop: 16 }}>
          <div style={{ marginBottom: 8, fontWeight: 500 }}>派生流水线（{derivationsQuery.data?.length ?? 0}）</div>
          <Table rowKey="id" size="small" columns={derivationColumns}
            dataSource={(derivationsQuery.data ?? []) as unknown as DerivationRow[]} loading={derivationsQuery.isFetching}
            pagination={false} />
          <div style={{ marginTop: 8, color: 'var(--text-2)', fontSize: 12 }}>
            标记「有新版本模板」的流水线可在定义页逐条合并；合并需人工确认，不做批量覆盖。
          </div>
        </div>
      </Modal>
    </div>
  )
}
