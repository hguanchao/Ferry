import { useEffect, useRef, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useNavigate } from 'react-router-dom'
import { App, Alert, AutoComplete, Button, Drawer, Form, Input, Modal, Select, Tag, Tooltip, type TableColumnsType } from 'antd'
import FerryTable from '@/components/FerryTable'
import {
  CirclePause, CirclePlay, Code2, Pencil, Play, Plus, RotateCw,
  Search, Trash2,
} from 'lucide-react'
import PageHeader from '@/components/PageHeader'
import PipelineCanvasEditor from '@/components/PipelineCanvasEditor'
import { appApi, pipelineApi } from '@/api/devops'
import type { AppOption, PipelinePreview, PipelineRow, PipelineStageDef, PrecheckResult, TriggerResult } from '@/api/types-devops'
import { useAuthStore } from '@/stores/auth'
import { showError } from '@/lib/notify'
import { useDebouncedValue } from '@/lib/debounce'

const RUN_STATUS_LABEL: Record<number, string> = {
  1: '排队中', 2: '运行中', 3: '阻塞', 4: '成功', 5: '失败', 6: '超时', 7: '已取消', 8: '结果待确认',
}
const CONCLUSION_LABEL: Record<number, string> = { 1: '通过', 2: '不通过', 3: '合并' }
/** 流水线状态（含 Jenkins 同步态，与后端 dev_pipeline.status 口径一致） */
const PIPELINE_STATUS: Record<number, { label: string; color: string }> = {
  0: { label: '停用', color: 'default' },
  1: { label: '启用', color: 'green' },
  2: { label: '待配置', color: 'orange' },
  3: { label: '同步失败', color: 'red' },
}

interface TriggerValues {
  appId: string
  pipelineId: string
  branch: string
  commitId: string
  refType: number
  note?: string
}

interface TriggerPrecheck extends PrecheckResult {
  values: TriggerValues
}

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

function newRequestId() {
  return `${Date.now()}-${Math.random().toString(16).slice(2)}`
}

/** 提交时间展示：无法解析时保留平台原值 */
function formatCommitTime(value?: string) {
  if (!value) return ''
  const date = new Date(value)
  return Number.isNaN(date.getTime()) ? value : date.toLocaleString('zh-CN', { hour12: false })
}

export default function Pipeline() {
  const { message, modal } = App.useApp()
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const can = useAuthStore((state) => state.can)
  const [form] = Form.useForm()
  const [triggerForm] = Form.useForm()
  const triggerAppId = Form.useWatch('appId', triggerForm)
  const [open, setOpen] = useState(false)
  const [triggerOpen, setTriggerOpen] = useState(false)
  const [editing, setEditing] = useState<PipelineRow | null>(null)
  const [triggerRow, setTriggerRow] = useState<PipelineRow | null>(null)
  const [precheck, setPrecheck] = useState<TriggerPrecheck | null>(null)
  const [filters, setFilters] = useState<Record<string, unknown>>({})
  const [page, setPage] = useState({ current: 1, size: 30 })
  const [stages, setStages] = useState<PipelineStageDef[]>([])
  const [previewOpen, setPreviewOpen] = useState(false)
  const [preview, setPreview] = useState<PipelinePreview | null>(null)
  // 用户是否手动改过画布：为 true 时定义回填不再覆盖草稿（防缓存回填吞掉草稿）
  const draftDirtyRef = useRef(false)

  const query = useQuery({
    queryKey: ['pipelines', filters, page],
    queryFn: () => pipelineApi.pageList({ ...filters, ...page }),
  })
  const appOptionsQuery = useQuery({
    queryKey: ['app-options'],
    queryFn: () => appApi.options(),
  })
  const triggerPipelinesQuery = useQuery({
    queryKey: ['trigger-pipeline-options', triggerAppId],
    queryFn: () => pipelineApi.pageList({ appIds: triggerAppId ? [triggerAppId] : [], status: 1, current: 1, size: 1000 }),
    enabled: triggerOpen && !triggerRow && !!triggerAppId,
  })
  const triggerBranch = Form.useWatch('branch', triggerForm)
  const triggerRefType = Form.useWatch('refType', triggerForm)
  const debouncedBranch = useDebouncedValue(triggerBranch ?? '', 500)
  const triggerBranchesQuery = useQuery({
    queryKey: ['trigger-branches', triggerAppId],
    queryFn: () => pipelineApi.triggerBranches(String(triggerAppId)),
    enabled: triggerOpen && !!triggerAppId,
    staleTime: 60_000,
    retry: false,
  })
  const triggerCommitsQuery = useQuery({
    queryKey: ['trigger-commits', triggerAppId, debouncedBranch],
    queryFn: () => pipelineApi.triggerCommits(String(triggerAppId), debouncedBranch),
    enabled: triggerOpen && !!triggerAppId && !!debouncedBranch && triggerRefType !== 2,
    staleTime: 60_000,
    retry: false,
  })

  const definitionQuery = useQuery({
    queryKey: ['pipeline-definition', editing?.id],
    queryFn: () => pipelineApi.definition(editing!.id),
    enabled: open && !!editing?.id,
    retry: false,
  })

  const rows = query.data?.records ?? []
  const appOptions = appOptionsQuery.data ?? []
  // 触发只列已配置代码源的应用：未登记仓库地址/默认分支时无法确定构建来源
  const triggerAppOptions = appOptions.filter((app) => app.repoUrl && app.repoBranch)
  const triggerApp = appOptions.find((app) => app.value === triggerAppId)
  const branchOptions = (triggerBranchesQuery.data ?? []).map((branch) => ({ value: branch }))
  const commitOptions = triggerRefType === 2
    ? []
    : (triggerCommitsQuery.data ?? []).map((commit) => ({
      value: commit.sha,
      label: (
        <div style={{ display: 'flex', gap: 8, alignItems: 'baseline' }}>
          <span className="ferry-mono" style={{ fontSize: 12 }}>{commit.shortSha}</span>
          <span style={{ flex: 1, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>{commit.message}</span>
          <span style={{ color: 'var(--text-3)', fontSize: 12, whiteSpace: 'nowrap' }}>
            {commit.authorName || '—'} {formatCommitTime(commit.committedAt)}
          </span>
        </div>
      ),
    }))
  const branchExtra = triggerBranchesQuery.isError
    ? `${triggerBranchesQuery.error?.message || '分支列表获取失败'}，可手动输入`
    : triggerBranchesQuery.isFetching
      ? '正在获取分支列表…'
      : triggerApp?.repoBranch ? `默认取应用登记分支：${triggerApp.repoBranch}` : undefined
  const commitExtra = triggerRefType === 2
    ? 'Tag/Ref 模式：可直接输入标签或引用名'
    : triggerCommitsQuery.isError
      ? `${triggerCommitsQuery.error?.message || '提交候选获取失败'}，可手动输入`
      : triggerCommitsQuery.isFetching
        ? '正在获取最近提交…'
        : triggerApp?.repoCommitId
          ? `默认取应用登记提交：${triggerApp.repoCommitId}`
          : '应用未登记默认提交，请选择或手动填写'

  const invalidate = () => {
    void queryClient.invalidateQueries({ queryKey: ['pipelines'] })
    void queryClient.invalidateQueries({ queryKey: ['pipeline-definition'] })
  }

  // 保存：单一请求同时提交档案与 stages（空数组=显式清空旧脚本），后端内容比对决定是否生成新版本并同步 Job
  const saveMutation = useMutation({
    mutationFn: async (values: Record<string, unknown>) => {
      const text = await pipelineApi.save({ ...values, stages })
      return text
    },
    onSuccess: (text) => {
      // 同步失败已在服务端落「同步失败」状态并记日志：提示但不算保存失败
      if (text?.includes('同步失败')) message.warning(text)
      else message.success(text || '保存成功')
      draftDirtyRef.current = false
      closeEditor()
      invalidate()
    },
    onError: showError,
  })
  const previewMutation = useMutation({
    mutationFn: () => pipelineApi.previewDefinition({ definition: buildDefinition(stages), mode: 1 }),
    onSuccess: (result) => {
      setPreview(result)
      setPreviewOpen(true)
    },
    onError: showError,
  })
  const copyPreviewScript = async () => {
    if (!preview?.script) return
    try {
      await navigator.clipboard.writeText(preview.script)
      message.success('脚本已复制')
    } catch {
      message.error('复制失败，请手动选择文本')
    }
  }
  const deleteMutation = useMutation({
    mutationFn: (ids: string[]) => pipelineApi.deleted(ids),
    onSuccess: (text) => { message.success(text || '删除成功'); invalidate() },
    onError: showError,
  })
  const syncMutation = useMutation({
    mutationFn: (id: string) => pipelineApi.syncRetry(id),
    onSuccess: (text) => {
      if (text?.includes('同步失败')) message.warning(text)
      else message.success(text || '同步成功')
      invalidate()
    },
    onError: showError,
  })
  const enableMutation = useMutation({
    mutationFn: (body: Record<string, unknown>) => pipelineApi.enable(body),
    onSuccess: (text) => {
      if (text?.includes('同步失败')) message.warning(text)
      else message.success(text || '操作成功')
      invalidate()
    },
    onError: showError,
  })
  const disableMutation = useMutation({
    mutationFn: (body: Record<string, unknown>) => pipelineApi.disable(body),
    onSuccess: (text) => {
      if (text?.includes('同步失败')) message.warning(text)
      else message.success(text || '操作成功')
      invalidate()
    },
    onError: showError,
  })
  const precheckMutation = useMutation({
    mutationFn: (body: TriggerValues) => pipelineApi.triggerPrecheck(body as unknown as Record<string, unknown>),
    onSuccess: (result, values) => {
      setPrecheck({ ...result, values })
      if (!result.canTrigger) message.warning('预校验未全部通过')
    },
    onError: showError,
  })
  const triggerMutation = useMutation({
    mutationFn: (body: Record<string, unknown>) => pipelineApi.trigger(body, newRequestId()),
    onSuccess: (result: TriggerResult) => {
      setTriggerOpen(false)
      setPrecheck(null)
      invalidate()
      if (result.runId) {
        modal.success({
          title: '触发完成',
          content: `${result.message || result.result}，运行 ID：${result.runId}`,
          okText: '查看运行详情',
          onOk: () => navigate(`/pipeline/run/${result.runId}`),
        })
      } else {
        message.success(`触发成功：${result.result}`)
      }
    },
    onError: showError,
  })

  const currentTriggerValues = triggerForm.getFieldsValue() as Partial<TriggerValues>
  const hasCurrentPrecheck = precheck?.canTrigger === true
    && precheck.values.appId === currentTriggerValues.appId
    && precheck.values.pipelineId === currentTriggerValues.pipelineId
    && precheck.values.branch === currentTriggerValues.branch
    && precheck.values.commitId === currentTriggerValues.commitId
    && precheck.values.refType === currentTriggerValues.refType

  // 关闭编辑器：失效当前流水线的定义缓存，保证下次打开拉到最新版本（避免旧缓存回填）
  const closeEditor = () => {
    setOpen(false)
    draftDirtyRef.current = false
    if (editing) void queryClient.invalidateQueries({ queryKey: ['pipeline-definition', editing.id] })
  }

  const openEditor = (record: PipelineRow | null) => {
    setEditing(record)
    form.resetFields()
    draftDirtyRef.current = false
    if (record) {
      form.setFieldsValue({ id: record.id, appId: record.appId, name: record.name })
    }
    // 新建与编辑都先留空画布：编辑阶段稍后由 definition 查询异步回填，新建则由用户通过「添加阶段」编排
    setStages([])
    setOpen(true)
  }

  // 编辑时 definition 异步返回后回填画布阶段；用户已手动编辑过画布则不覆盖草稿
  useEffect(() => {
    if (editing && definitionQuery.data?.json && !draftDirtyRef.current) {
      const loaded = parseDefinition(definitionQuery.data.json)
      if (loaded.length) setStages(loaded)
    }
  }, [editing, definitionQuery.data])

  const openTrigger = (record: PipelineRow | null) => {
    setTriggerRow(record)
    setPrecheck(null)
    triggerForm.resetFields()
    if (record) {
      const appOption = appOptions.find((app) => app.value === record.appId)
      triggerForm.setFieldsValue({
        appId: record.appId,
        pipelineId: record.id,
        branch: record.repoBranch ?? appOption?.repoBranch,
        commitId: appOption?.repoCommitId,
      })
    }
    setTriggerOpen(true)
  }

  const runPrecheck = async () => {
    try {
      const values = await triggerForm.validateFields()
      precheckMutation.mutate(values)
    } catch {
      // Form renders validation messages next to invalid fields.
    }
  }

  const submitTrigger = async () => {
    if (!precheck?.canTrigger) return
    try {
      const values = await triggerForm.validateFields()
      triggerMutation.mutate({
        ...values,
        appId: values.appId ?? triggerRow?.appId,
        pipelineId: values.pipelineId ?? triggerRow?.id,
      })
    } catch {
      // Form renders validation messages next to invalid fields.
    }
  }

  const columns: TableColumnsType<PipelineRow> = [
    { title: '流水线名称', dataIndex: 'name' },
    { title: '应用名称', dataIndex: 'appName', render: (v?: string) => v || '—' },
    {
      // 流水线不持有独立分支，这里展示应用管理登记的默认分支
      title: <Tooltip title="取自应用管理登记的仓库分支"><span>默认分支</span></Tooltip>,
      dataIndex: 'repoBranch',
      render: (v?: string) => v ? <span className="ferry-mono" style={{ fontSize: 12 }}>{v}</span> : '—',
    },
    { title: '当前版本', dataIndex: 'version', render: (v: number) => v ? `V${v}` : '—' },
    { title: '阶段数', dataIndex: 'stageCount', render: (v?: number) => v == null ? '—' : v },
    {
      // 状态含 Jenkins 同步态：待配置（无定义/已清空）与同步失败时禁止触发，可重试同步
      title: '状态', dataIndex: 'status', width: 96,
      render: (v: number) => {
        const meta = PIPELINE_STATUS[v]
        return <Tag color={meta?.color ?? 'default'}>{meta?.label ?? `未知(${v})`}</Tag>
      },
    },
    { title: '更新时间', dataIndex: 'updateTime', render: (v?: string) => v || '—' },
    {
      title: '操作',
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
          {can('devops:pipeline:enable') && record.status === 0 && (
            <Tooltip title="启用">
              <Button type="link" size="small" icon={<CirclePlay size={15} />}
                onClick={() => enableMutation.mutate({ id: record.id })} />
            </Tooltip>
          )}
          {can('devops:pipeline:disable') && record.status === 1 && (
            <Tooltip title="停用">
              <Button type="link" size="small" icon={<CirclePause size={15} />}
                onClick={() => disableMutation.mutate({ id: record.id })} />
            </Tooltip>
          )}
          {can('devops:pipeline:save') && record.status === 3 && (
            <Tooltip title="重新同步 Jenkins Job">
              <Button type="link" size="small" icon={<RotateCw size={15} />}
                onClick={() => syncMutation.mutate(record.id)} />
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
            options={Object.entries(RUN_STATUS_LABEL).map(([k, v]) => ({ value: Number(k), label: v }))}
          />
          <div className="ferry-toolbar__spacer" />
          <Button size="small" icon={<RotateCw size={14} />}
            onClick={() => setFilters({})}>重置</Button>
        </div>
        <div className="ferry-table-wrap">
          <FerryTable
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
        onClose={closeEditor} width="100%" placement="right"
        destroyOnHidden styles={{ body: { padding: 0, display: 'flex', flexDirection: 'column' } }}
        extra={
          <div style={{ display: 'flex', gap: 8 }}>
            <Button icon={<Code2 size={14} />} loading={previewMutation.isPending}
              disabled={!!editing && definitionQuery.isFetching}
              onClick={() => previewMutation.mutate()}>预览脚本</Button>
            <Button onClick={closeEditor}>取消</Button>
            <Button type="primary" loading={saveMutation.isPending}
              disabled={!!editing && (definitionQuery.isPending || definitionQuery.isFetching)}
              onClick={() => form.submit()}>保存</Button>
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
            <PipelineCanvasEditor value={stages} onChange={(s) => { draftDirtyRef.current = true; setStages(s) }} />
          </div>
        </div>
      </Drawer>

      <Modal open={previewOpen} title="脚本预览（基于当前定义）" width={900} destroyOnHidden
        onCancel={() => setPreviewOpen(false)}
        footer={[
          <Button key="copy" onClick={copyPreviewScript}>复制脚本</Button>,
          <Button key="close" type="primary" onClick={() => setPreviewOpen(false)}>关闭</Button>,
        ]}>
        {preview?.warnings?.length ? (
          <Alert type="warning" showIcon style={{ marginBottom: 12 }} message="预览告警"
            description={
              <ul style={{ margin: 0, paddingLeft: 18 }}>
                {preview.warnings.map((warning, index) => <li key={index}>{warning}</li>)}
              </ul>
            } />
        ) : null}
        <pre className="ferry-mono" style={{
          background: 'var(--bg-subtle)', padding: 16, borderRadius: 8, margin: 0,
          maxHeight: 560, overflow: 'auto', whiteSpace: 'pre-wrap', fontSize: 12,
        }}>
          {preview?.script ?? ''}
        </pre>
      </Modal>

      <Modal
        open={triggerOpen}
        title="手动触发"
        onCancel={() => setTriggerOpen(false)}
        footer={[
          <Button key="cancel" onClick={() => setTriggerOpen(false)}>取消</Button>,
          <Button key="precheck" loading={precheckMutation.isPending} onClick={runPrecheck}>预校验</Button>,
          <Button key="run" type="primary" disabled={!hasCurrentPrecheck}
            loading={triggerMutation.isPending} onClick={submitTrigger}>开始运行</Button>,
        ]}
        width={620}
        destroyOnHidden
      >
        <Form form={triggerForm} layout="vertical">
          <Form.Item name="appId" label="目标应用" rules={[{ required: true, message: '请选择应用' }]}>
            {triggerRow ? (
              <Select
                disabled
                options={[{
                  value: triggerRow.appId,
                  label: (() => {
                    const appOption = appOptionsQuery.data?.find((app) => app.value === triggerRow.appId)
                    const appName = triggerRow.appName ?? appOption?.label ?? '未知应用'
                    return appOption?.code ? `${appName}（${appOption.code}）` : appName
                  })(),
                }]}
              />
            ) : (
              <Select
                showSearch
                optionFilterProp="label"
                loading={appOptionsQuery.isLoading}
                placeholder="请选择应用"
                options={triggerAppOptions.map((app) => ({
                  value: app.value,
                  label: `${app.label}（${app.code}）`,
                }))}
                onChange={(appId) => {
                  const selected = appOptions.find((app) => app.value === appId)
                  setPrecheck(null)
                  triggerForm.setFieldsValue({
                    pipelineId: undefined,
                    branch: selected?.repoBranch,
                    commitId: selected?.repoCommitId,
                  })
                }}
              />
            )}
          </Form.Item>
          <Form.Item label="仓库地址">
            <Input value={triggerApp?.repoUrl} disabled placeholder="选择应用后自动带出" />
          </Form.Item>
          <Form.Item name="pipelineId" label="流水线" rules={[{ required: true, message: '请选择流水线' }]}>
            {triggerRow ? (
              <Select disabled options={[{ value: triggerRow.id, label: triggerRow.name }]} />
            ) : (
              <Select
                showSearch
                optionFilterProp="label"
                disabled={!triggerAppId}
                loading={triggerPipelinesQuery.isLoading}
                placeholder={triggerAppId ? '请选择流水线' : '请先选择应用'}
                options={(triggerPipelinesQuery.data?.records ?? []).map((row) => ({
                  value: row.id,
                  label: row.name,
                }))}
                onChange={(pipelineId) => {
                  const selected = triggerPipelinesQuery.data?.records.find((row) => row.id === pipelineId)
                  const currentCommit = triggerForm.getFieldValue('commitId') as string | undefined
                  setPrecheck(null)
                  triggerForm.setFieldsValue({
                    branch: selected?.repoBranch ?? triggerApp?.repoBranch,
                    // 选流水线只补默认提交，不覆盖用户已输入的提交
                    commitId: currentCommit || triggerApp?.repoCommitId,
                  })
                }}
              />
            )}
          </Form.Item>
          <Form.Item
            name="branch"
            label="分支"
            rules={[{ required: true, message: '请输入分支' }, { max: 100, message: '分支长度不能超过 100' }]}
            extra={branchExtra}
          >
            <AutoComplete
              allowClear
              disabled={!triggerAppId}
              options={branchOptions}
              placeholder={triggerRow?.repoBranch ? `默认分支：${triggerRow.repoBranch}` : '选择或输入已登记仓库中的分支'}
              filterOption={(input, option) =>
                String(option?.value ?? '').toLowerCase().includes(input.toLowerCase())}
              notFoundContent={triggerBranchesQuery.isFetching ? '加载中…' : '无匹配分支，可直接输入'}
              onSelect={() => {
                triggerForm.setFieldsValue({ commitId: undefined })
                setPrecheck(null)
              }}
              onChange={() => setPrecheck(null)}
            />
          </Form.Item>
          <Form.Item
            name="commitId"
            label="提交 ID / Tag / Ref"
            rules={[{ required: true, message: '请输入提交' }, { max: 100, message: '提交或引用值长度不能超过 100' }]}
            extra={commitExtra}
          >
            <AutoComplete
              allowClear
              disabled={!triggerAppId}
              options={commitOptions}
              filterOption={false}
              placeholder="Commit：7–40 位十六进制；Tag/Ref：合法 Git ref"
              notFoundContent={triggerRefType === 2
                ? 'Tag/Ref 模式可直接输入'
                : triggerCommitsQuery.isFetching ? '加载中…' : '无匹配提交，可直接输入'}
              onChange={() => setPrecheck(null)}
            />
          </Form.Item>
          <Form.Item name="refType" label="引用类型" initialValue={1}>
            <Select
              options={[{ value: 1, label: 'Commit' }, { value: 2, label: 'Tag/Ref' }]}
              onChange={() => setPrecheck(null)}
            />
          </Form.Item>
          <Form.Item name="note" label="触发说明"><Input.TextArea rows={2} placeholder="选填" /></Form.Item>
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
                {precheck.canTrigger ? '预校验通过，请确认后开始运行' : '预校验未通过，请根据结果修正'}
              </div>
            </div>
          )}
        </Form>
      </Modal>
    </div>
  )
}
