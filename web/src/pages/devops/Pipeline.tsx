import { useMemo, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  App as AntApp,
  Button,
  Dropdown,
  Form,
  Input,
  Modal,
  Select,
  Table,
  Tag,
  Tooltip,
  type MenuProps,
  type TableColumnsType,
} from 'antd'
import {
  Download,
  Play,
  History,
  MoreHorizontal,
  Pencil,
  Plus,
  RotateCw,
  Search,
  Settings2,
  Trash2,
} from 'lucide-react'
import PageHeader from '@/components/PageHeader'
import AppSelect from '@/components/AppSelect'
import PipelineEditorDrawer from '@/components/PipelineEditorDrawer'
import PipelineVersionPanel, { type VersionPanelAdapter } from '@/components/PipelineVersionPanel'
import RunTriggerModal from '@/components/RunTriggerModal'
import RunListDrawer from '@/components/RunListDrawer'
import TemplateMergeModal from '@/components/TemplateMergeModal'
import { pipelineApi, pipelineVersionApi, type PipelineFilter } from '@/api/devops'
import { userApi } from '@/api/system'
import type { PipelineDetail, PipelineRow, PipelineVersionDetail } from '@/api/types'
import { COL } from '@/lib/table'
import { definitionToYaml } from '@/lib/pipeline'
import { showError } from '@/lib/notify'
import { useAuthStore } from '@/stores/auth'

interface MetaFormValues {
  appId: string
  name: string
  description?: string
}

interface EditorState {
  mode: 'create' | 'definition'
  pipeline: PipelineDetail | null
}

/** 版本面板的流水线侧适配器：面板 UI 与流水线版本 API 的对接 */
const pipelineVersionAdapter: VersionPanelAdapter<PipelineVersionDetail> = {
  list: pipelineVersionApi.list,
  detail: pipelineVersionApi.detail,
  yamlOf: (version) => definitionToYaml(version.definition),
  rollback: (pipelineId, versionId) => pipelineVersionApi.rollback({ pipelineId, versionId }),
  listKey: 'pipeline-versions',
  detailKey: 'pipeline-version',
  rollbackPerm: 'devops:pipeline:rollback',
}

export default function Pipeline() {
  const { message, modal } = AntApp.useApp()
  const queryClient = useQueryClient()
  const can = useAuthStore((state) => state.can)

  const [filters, setFilters] = useState<PipelineFilter>({})
  const [draft, setDraft] = useState<PipelineFilter>({})
  const [page, setPage] = useState({ current: 1, size: 30 })
  const [selected, setSelected] = useState<string[]>([])
  const [editor, setEditor] = useState<EditorState | null>(null)
  const [metaTarget, setMetaTarget] = useState<PipelineRow | null>(null)
  const [metaOpen, setMetaOpen] = useState(false)
  const [metaForm] = Form.useForm<MetaFormValues>()
  const [versionTarget, setVersionTarget] = useState<PipelineRow | null>(null)
  const [versionOpen, setVersionOpen] = useState(false)
  const [mergeTarget, setMergeTarget] = useState<PipelineDetail | null>(null)
  const [triggerOpen, setTriggerOpen] = useState(false)
  const [triggerPipeline, setTriggerPipeline] = useState<PipelineRow | null>(null)
  const [runsTarget, setRunsTarget] = useState<PipelineRow | null>(null)

  const listQuery = useQuery({
    queryKey: ['pipelines', filters, page],
    queryFn: () => pipelineApi.pageList({ ...filters, ...page }),
  })

  // 负责人候选来自用户列表；没有用户列表权限时下拉为空，筛选仍可用其它条件
  const userQuery = useQuery({
    queryKey: ['user-options'],
    queryFn: () => userApi.pageList({ current: 1, size: 200 }),
    retry: false,
  })
  const ownerOptions = useMemo(
    () => (userQuery.data?.records ?? []).map((user) => ({ value: user.id, label: user.nickname || user.account })),
    [userQuery.data],
  )

  // 编辑基本信息时应用不允许改，下拉只需要把当前应用名称显示出来
  const metaAppOption = useMemo(
    () =>
      metaTarget
        ? [{ id: metaTarget.appId, code: metaTarget.appCode, name: metaTarget.appName }]
        : undefined,
    [metaTarget],
  )

  const invalidate = () => {
    void queryClient.invalidateQueries({ queryKey: ['pipelines'] })
    void queryClient.invalidateQueries({ queryKey: ['pipeline-detail'] })
    void queryClient.invalidateQueries({ queryKey: ['pipeline-versions'] })
    void queryClient.invalidateQueries({ queryKey: ['runs'] })
    void queryClient.invalidateQueries({ queryKey: ['run-detail'] })
    void queryClient.invalidateQueries({ queryKey: ['run-triggers'] })
    void queryClient.invalidateQueries({ queryKey: ['workbench-overview'] })
  }

  const metaMutation = useMutation({
    mutationFn: (body: Record<string, unknown>) => pipelineApi.save(body),
    onSuccess: (text) => {
      message.success(text || '修改成功')
      setMetaOpen(false)
      invalidate()
    },
    onError: showError,
  })

  const deleteMutation = useMutation({
    mutationFn: (ids: string[]) => pipelineApi.deleted(ids),
    onSuccess: (text) => {
      message.success(text || '删除成功')
      setSelected([])
      invalidate()
    },
    onError: showError,
  })

  const exportMutation = useMutation({
    mutationFn: () => pipelineApi.export({ ...filters, ids: selected }),
    onSuccess: () => message.success(selected.length ? `已导出勾选的 ${selected.length} 条` : '已按当前筛选条件导出'),
    onError: showError,
  })

  const applyFilters = () => {
    setFilters(draft)
    setPage((prev) => ({ ...prev, current: 1 }))
    // 筛选条件变了，原勾选项可能已不在结果里，继续保留会让「导出勾选」导出看不见的数据
    setSelected([])
  }

  const resetFilters = () => {
    setDraft({})
    setFilters({})
    setPage({ current: 1, size: 30 })
    setSelected([])
  }

  const openMeta = (record: PipelineRow) => {
    setMetaTarget(record)
    metaForm.setFieldsValue({
      appId: record.appId,
      name: record.name,
      description: record.description ?? '',
    })
    setMetaOpen(true)
  }

  const submitMeta = async () => {
    if (!metaTarget) {
      return
    }
    const values = await metaForm.validateFields()
    metaMutation.mutate({
      id: metaTarget.id,
      appId: metaTarget.appId,
      name: values.name,
      description: values.description ?? '',
    })
  }

  const openDefinition = async (record: PipelineRow) => {
    try {
      const detail = await queryClient.fetchQuery({
        queryKey: ['pipeline-detail', record.id],
        queryFn: () => pipelineApi.detail(record.id),
      })
      setEditor({ mode: 'definition', pipeline: detail })
    } catch (error) {
      showError(error)
    }
  }

  const openVersions = (record: PipelineRow) => {
    setVersionTarget(record)
    setVersionOpen(true)
  }

  /** 打开模板合并弹窗前先取详情：合并需要定义与派生时的变量取值 */
  const openMerge = async (record: PipelineRow) => {
    try {
      const detail = await queryClient.fetchQuery({
        queryKey: ['pipeline-detail', record.id],
        queryFn: () => pipelineApi.detail(record.id),
      })
      setMergeTarget(detail)
    } catch (error) {
      showError(error)
    }
  }

  const confirmDelete = (record: PipelineRow) => {
    modal.confirm({
      title: `删除流水线「${record.name}」？`,
      content: '逻辑删除后列表不再展示，历史版本保留；已生成的流水线 id 不会被复用。',
      okText: '删除',
      okButtonProps: { danger: true },
      cancelText: '取消',
      onOk: () => deleteMutation.mutateAsync([record.id]),
    })
  }

  const columns: TableColumnsType<PipelineRow> = [
    {
      title: '流水线ID',
      dataIndex: 'id',
      width: 200,
      render: (value: string) => <span className="ferry-mono" style={{ fontSize: 12 }}>PL-{value}</span>,
    },
    {
      title: '应用',
      dataIndex: 'appName',
      render: (value: string, record) => (
        <div>
          <div>{value}</div>
          <div className="ferry-mono" style={{ fontSize: 12, color: 'var(--text-3)' }}>{record.appCode}</div>
        </div>
      ),
    },
    {
      title: '流水线名称',
      dataIndex: 'name',
      render: (value: string, record) => (
        <span className="ferry-hstack" style={{ gap: 6 }}>
          <span style={{ fontWeight: 550 }}>{value}</span>
          {record.sourceTemplateOutdated && (
            <Tooltip title="模板已发布新版本，点击查看差异">
              <Tag color="orange" style={{ cursor: 'pointer' }} onClick={() => void openMerge(record)}>
                有新版本模板
              </Tag>
            </Tooltip>
          )}
        </span>
      ),
    },
    {
      title: '当前版本',
      dataIndex: 'currentVersionNo',
      width: 110,
      render: (value: number | null | undefined, record) =>
        value ? (
          can('devops:pipeline:versionList') ? (
            <Button type="link" size="small" style={{ paddingInline: 0 }} onClick={() => openVersions(record)}>
              V{value}
            </Button>
          ) : (
            <span className="ferry-mono">V{value}</span>
          )
        ) : (
          <Tag>未配置</Tag>
        ),
    },
    { title: '阶段数', dataIndex: 'stageCount', width: 80 },
    { title: '默认分支', dataIndex: 'defaultBranch', width: 120, render: (value?: string) => value || '—' },
    {
      title: '负责人',
      dataIndex: 'ownerNames',
      render: (value?: string) => (value ? value.split(',').filter(Boolean).join('、') : '—'),
    },
    { title: '更新时间', dataIndex: 'updateTime', width: COL.time },
  ]
  const menuItems = (): MenuProps['items'] => {
    const items: MenuProps['items'] = []
    if (can('devops:pipeline:save')) {
      items.push({ key: 'edit', label: '编辑基本信息', icon: <Pencil size={14} /> })
    }
    if (can('devops:pipeline:versionList')) {
      items.push({ key: 'versions', label: '版本历史', icon: <History size={14} /> })
    }
    if (can('devops:pipeline:deleted')) {
      if (items.length > 0) {
        items.push({ type: 'divider' })
      }
      items.push({ key: 'delete', label: '删除', danger: true, icon: <Trash2 size={14} /> })
    }
    return items
  }

  const handleMenuClick = (record: PipelineRow, key: string) => {
    if (key === 'edit') {
      openMeta(record)
    } else if (key === 'versions') {
      openVersions(record)
    } else if (key === 'delete') {
      confirmDelete(record)
    }
  }

  const hasRowAction =
    can('devops:pipeline:save') ||
    can('devops:pipeline:deleted') ||
    can('devops:pipeline:versionList') ||
    can('devops:pipeline:saveVersion') ||
    can('devops:pipeline:trigger') ||
    can('devops:pipeline:runPageList')

  if (hasRowAction) {
    columns.push({
      title: '操作',
      key: 'action',
      width: 300,
      render: (_, record) => {
        const items = menuItems()
        return (
          <div style={{ display: 'flex', alignItems: 'center', gap: 2 }}>
            {can('devops:pipeline:trigger') && (
              <Button
                type="link"
                size="small"
                onClick={() => { setTriggerPipeline(record); setTriggerOpen(true) }}
              >
                手动触发
              </Button>
            )}
            {can('devops:pipeline:runPageList') && (
              <Button
                type="link"
                size="small"
                onClick={() => setRunsTarget(record)}
              >
                查看运行
              </Button>
            )}
            {can('devops:pipeline:saveVersion') && (
              <Button
                type="link"
                size="small"
                icon={<Settings2 size={14} />}
                onClick={() => void openDefinition(record)}
              >
                编辑定义
              </Button>
            )}
            {items && items.length > 0 && (
              <Dropdown
                menu={{ items, onClick: ({ key }) => handleMenuClick(record, key) }}
                trigger={['click']}
              >
                <Button type="text" size="small" icon={<MoreHorizontal size={15} />} />
              </Dropdown>
            )}
          </div>
        )
      },
    })
  }

  return (
    <>
      <PageHeader
        title="流水线管理"
        description="流水线定义按版本快照管理，历史版本可对比与回退。"
        extra={
          <>
            {can('devops:pipeline:trigger') && (
              <Button icon={<Play size={15} />} onClick={() => { setTriggerPipeline(null); setTriggerOpen(true) }}>
                手动触发
              </Button>
            )}
            {can('devops:pipeline:export') && (
              <Button
                icon={<Download size={15} />}
                loading={exportMutation.isPending}
                onClick={() => exportMutation.mutate()}
              >
                {selected.length ? `导出勾选（${selected.length}）` : '导出'}
              </Button>
            )}
            {can('devops:pipeline:save') && (
              <Button
                type="primary"
                icon={<Plus size={14} />}
                onClick={() => setEditor({ mode: 'create', pipeline: null })}
              >
                新建流水线
              </Button>
            )}
          </>
        }
      />

      <section className="ferry-panel">
        <div className="ferry-toolbar">
          <Input
            allowClear
            prefix={<Search size={15} style={{ color: 'var(--text-4)' }} />}
            placeholder="流水线名称"
            style={{ width: 220 }}
            value={draft.keyword}
            onChange={(event) => setDraft((prev) => ({ ...prev, keyword: event.target.value }))}
            onPressEnter={applyFilters}
          />
          <AppSelect
            multiple
            allowClear
            maxTagCount="responsive"
            placeholder="应用"
            style={{ width: 300 }}
            value={draft.appIds}
            onChange={(value) =>
              setDraft((prev) => ({
                ...prev,
                appIds: Array.isArray(value) ? value : value ? [value] : undefined,
              }))
            }
          />
          <Select<string[]>
            mode="multiple"
            allowClear
            showSearch
            optionFilterProp="label"
            maxTagCount="responsive"
            placeholder="负责人"
            style={{ width: 240 }}
            options={ownerOptions}
            value={draft.ownerIds}
            onChange={(value) => setDraft((prev) => ({ ...prev, ownerIds: value?.length ? value : undefined }))}
          />
          <Button type="primary" icon={<Search size={15} />} onClick={applyFilters}>查询</Button>
          <Button icon={<RotateCw size={15} />} onClick={resetFilters}>重置</Button>
        </div>

        <div className="ferry-table-wrap">
          <Table<PipelineRow>
            rowKey="id"
            columns={columns}
            dataSource={listQuery.data?.records ?? []}
            loading={listQuery.isLoading || listQuery.isFetching}
            scroll={{ x: 'max-content' }}
            rowSelection={{
              selectedRowKeys: selected,
              onChange: (keys) => setSelected(keys.map(String)),
            }}
            pagination={{
              current: page.current,
              pageSize: page.size,
              total: listQuery.data?.total ?? 0,
              showSizeChanger: true,
              pageSizeOptions: ['30', '100', '200', '500', '1000'],
              showTotal: (total) => `共 ${total} 条`,
              onChange: (current, size) => setPage({ current, size }),
            }}
          />
        </div>
      </section>

      <Modal
        open={metaOpen}
        title="编辑流水线基本信息"
        width={560}
        okText="保存"
        cancelText="取消"
        confirmLoading={metaMutation.isPending}
        onCancel={() => setMetaOpen(false)}
        onOk={() => void submitMeta()}
        destroyOnHidden
      >
        <Form<MetaFormValues> form={metaForm} layout="vertical" requiredMark="optional">
          <Form.Item name="appId" label="所属应用" extra="创建后不可修改">
            <AppSelect disabled presetOptions={metaAppOption} />
          </Form.Item>
          <Form.Item
            name="name"
            label="流水线名称"
            rules={[{ required: true, message: '请输入流水线名称' }]}
          >
            <Input maxLength={100} placeholder="例如 订单服务主干流水线" />
          </Form.Item>
          <Form.Item name="description" label="描述">
            <Input.TextArea rows={3} maxLength={500} showCount placeholder="选填" />
          </Form.Item>
        </Form>
      </Modal>

      <PipelineEditorDrawer
        open={Boolean(editor)}
        mode={editor?.mode ?? 'create'}
        pipeline={editor?.pipeline ?? null}
        onClose={() => setEditor(null)}
        onSaved={invalidate}
      />

      <PipelineVersionPanel
        open={versionOpen}
        owner={versionTarget}
        adapter={pipelineVersionAdapter}
        onClose={() => setVersionOpen(false)}
        onChanged={invalidate}
      />

      <TemplateMergeModal
        open={Boolean(mergeTarget)}
        pipeline={mergeTarget}
        onClose={() => setMergeTarget(null)}
        onChanged={invalidate}
      />

      <RunTriggerModal
        open={triggerOpen}
        pipeline={triggerPipeline}
        onClose={() => setTriggerOpen(false)}
        onTriggered={() => invalidate()}
      />

      <RunListDrawer
        open={Boolean(runsTarget)}
        pipeline={runsTarget ? { pipelineId: runsTarget.id, pipelineName: runsTarget.name } : null}
        onClose={() => setRunsTarget(null)}
        onChanged={invalidate}
      />
    </>
  )
}
