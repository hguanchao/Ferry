import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  App as AntApp,
  Button,
  Drawer,
  Dropdown,
  Form,
  Input,
  Modal,
  Select,
  Table,
  Tabs,
  Tag,
  Tooltip,
  type MenuProps,
  type TableColumnsType,
} from 'antd'
import {
  Blocks,
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
import PipelineVersionPanel, { type VersionPanelAdapter } from '@/components/PipelineVersionPanel'
import TemplateEditorDrawer from '@/components/TemplateEditorDrawer'
import { pipelineTemplateApi } from '@/api/devops'
import type { PipelineRow, PipelineTemplateDetail, PipelineTemplateRow, TemplateVersionDetail } from '@/api/types'
import { COL } from '@/lib/table'
import { templateToYaml } from '@/lib/pipeline'
import { showError } from '@/lib/notify'
import { useAuthStore } from '@/stores/auth'

interface TemplateFilter {
  keyword?: string
  status?: number
}

interface MetaFormValues {
  name: string
  description?: string
}

interface EditorState {
  mode: 'create' | 'definition'
  template: PipelineTemplateDetail | null
}

/** 版本面板的模板侧适配器：面板 UI 与模板版本 API 的对接 */
const templateVersionAdapter: VersionPanelAdapter<TemplateVersionDetail> = {
  list: pipelineTemplateApi.versionList,
  detail: pipelineTemplateApi.versionDetail,
  yamlOf: (version) => templateToYaml(version.definition),
  rollback: (templateId, versionId) => pipelineTemplateApi.rollback({ templateId, versionId }),
  listKey: 'pipeline-template-versions',
  detailKey: 'pipeline-template-version',
  rollbackPerm: 'devops:template:rollback',
}

export default function PipelineTemplate() {
  const { message, modal } = AntApp.useApp()
  const queryClient = useQueryClient()
  const can = useAuthStore((state) => state.can)
  const isSuper = useAuthStore((state) => state.user?.isSuper === 1)

  const [activeScope, setActiveScope] = useState(0)
  const [filters, setFilters] = useState<TemplateFilter>({})
  const [draft, setDraft] = useState<TemplateFilter>({})
  const [page, setPage] = useState({ current: 1, size: 30 })
  const [editor, setEditor] = useState<EditorState | null>(null)
  const [metaTarget, setMetaTarget] = useState<PipelineTemplateRow | null>(null)
  const [metaOpen, setMetaOpen] = useState(false)
  const [metaForm] = Form.useForm<MetaFormValues>()
  const [versionTarget, setVersionTarget] = useState<PipelineTemplateRow | null>(null)
  const [versionOpen, setVersionOpen] = useState(false)
  const [derivedTarget, setDerivedTarget] = useState<PipelineTemplateRow | null>(null)
  const [derivedOpen, setDerivedOpen] = useState(false)
  const [derivedPage, setDerivedPage] = useState({ current: 1, size: 10 })

  const listQuery = useQuery({
    queryKey: ['pipeline-templates', activeScope, filters, page],
    queryFn: () => pipelineTemplateApi.pageList({ scope: activeScope, ...filters, ...page }),
  })

  const derivedQuery = useQuery({
    queryKey: ['template-derived', derivedTarget?.id, derivedPage],
    queryFn: () => pipelineTemplateApi.derivedList({ id: derivedTarget?.id ?? '', ...derivedPage }),
    enabled: derivedOpen && Boolean(derivedTarget),
  })

  const invalidate = () => {
    void queryClient.invalidateQueries({ queryKey: ['pipeline-templates'] })
    void queryClient.invalidateQueries({ queryKey: ['pipeline-template-detail'] })
    void queryClient.invalidateQueries({ queryKey: ['pipeline-template-versions'] })
    void queryClient.invalidateQueries({ queryKey: ['template-options'] })
    // 模板发新版本会影响流水线列表的「有新版本模板」提示
    void queryClient.invalidateQueries({ queryKey: ['pipelines'] })
  }

  const metaMutation = useMutation({
    mutationFn: (body: Record<string, unknown>) => pipelineTemplateApi.save(body),
    onSuccess: (text) => {
      message.success(text || '修改成功')
      setMetaOpen(false)
      invalidate()
    },
    onError: showError,
  })

  const deleteMutation = useMutation({
    mutationFn: (ids: string[]) => pipelineTemplateApi.deleted(ids),
    onSuccess: (text) => {
      message.success(text || '删除成功')
      invalidate()
    },
    onError: showError,
  })

  const statusMutation = useMutation({
    mutationFn: (body: { id: string; status: number }) => pipelineTemplateApi.changeStatus(body),
    onSuccess: (text) => {
      message.success(text || '修改成功')
      invalidate()
    },
    onError: showError,
  })

  const applyFilters = () => {
    setFilters(draft)
    setPage((prev) => ({ ...prev, current: 1 }))
  }

  const resetFilters = () => {
    setDraft({})
    setFilters({})
    setPage({ current: 1, size: 30 })
  }

  const changeScope = (key: string) => {
    setActiveScope(Number(key))
    setPage({ current: 1, size: 30 })
    setFilters({})
    setDraft({})
  }

  const openMeta = (record: PipelineTemplateRow) => {
    setMetaTarget(record)
    metaForm.setFieldsValue({
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
      scope: metaTarget.scope,
      deptId: metaTarget.deptId ?? undefined,
      name: values.name,
      description: values.description ?? '',
    })
  }

  const openDefinition = async (record: PipelineTemplateRow) => {
    try {
      const detail = await queryClient.fetchQuery({
        queryKey: ['pipeline-template-detail', record.id],
        queryFn: () => pipelineTemplateApi.detail(record.id),
      })
      setEditor({ mode: 'definition', template: detail })
    } catch (error) {
      showError(error)
    }
  }

  const openVersions = (record: PipelineTemplateRow) => {
    setVersionTarget(record)
    setVersionOpen(true)
  }

  const openDerived = (record: PipelineTemplateRow) => {
    setDerivedTarget(record)
    setDerivedPage({ current: 1, size: 10 })
    setDerivedOpen(true)
  }

  const changeStatus = (record: PipelineTemplateRow) => {
    const next = record.status === 1 ? 0 : 1
    statusMutation.mutate({ id: record.id, status: next })
  }

  const confirmDelete = (record: PipelineTemplateRow) => {
    modal.confirm({
      title: `删除模板「${record.name}」？`,
      content: '逻辑删除后列表不再展示，历史版本保留；已有派生流水线的模板不能删除，只能停用。',
      okText: '删除',
      okButtonProps: { danger: true },
      cancelText: '取消',
      onOk: () => deleteMutation.mutateAsync([record.id]),
    })
  }

  const menuItems = (record: PipelineTemplateRow): MenuProps['items'] => {
    const items: MenuProps['items'] = []
    if (can('devops:template:save')) {
      items.push({ key: 'meta', label: '编辑基本信息', icon: <Pencil size={14} /> })
    }
    if (can('devops:template:version:list')) {
      items.push({ key: 'versions', label: '版本历史', icon: <History size={14} /> })
    }
    if (can('devops:template:derivedList')) {
      items.push({ key: 'derived', label: '派生流水线', icon: <Blocks size={14} /> })
    }
    if (can('devops:template:status')) {
      items.push({
        key: 'status',
        label: record.status === 1 ? '停用' : '启用',
        icon: <RotateCw size={14} />,
      })
    }
    if (can('devops:template:deleted')) {
      if (items.length > 0) {
        items.push({ type: 'divider' })
      }
      items.push({
        key: 'delete',
        label: '删除',
        danger: true,
        disabled: record.derivedCount > 0,
        icon: <Trash2 size={14} />,
      })
    }
    return items
  }

  const handleMenuClick = (record: PipelineTemplateRow, key: string) => {
    if (key === 'meta') {
      openMeta(record)
    } else if (key === 'versions') {
      openVersions(record)
    } else if (key === 'derived') {
      openDerived(record)
    } else if (key === 'status') {
      changeStatus(record)
    } else if (key === 'delete') {
      confirmDelete(record)
    }
  }

  const columns: TableColumnsType<PipelineTemplateRow> = [
    {
      title: '模板名称',
      dataIndex: 'name',
      render: (value: string) => <span style={{ fontWeight: 550 }}>{value}</span>,
    },
    {
      title: '适用场景',
      dataIndex: 'description',
      ellipsis: true,
      render: (value?: string | null) => value || '—',
    },
    { title: '阶段数', dataIndex: 'stageCount', width: 80 },
    { title: '变量数', dataIndex: 'variableCount', width: 80 },
    { title: '派生数', dataIndex: 'derivedCount', width: 80 },
    {
      title: '版本',
      dataIndex: 'currentVersionNo',
      width: 100,
      render: (value: number | null | undefined, record) =>
        value ? (
          can('devops:template:version:list') ? (
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
    {
      title: '状态',
      dataIndex: 'status',
      width: 90,
      render: (value: number) =>
        value === 1 ? <Tag color="green">启用</Tag> : <Tag color="orange">停用</Tag>,
    },
    {
      title: '创建人',
      dataIndex: 'createUserName',
      width: 120,
      render: (value?: string | null) => value || '—',
    },
    { title: '更新时间', dataIndex: 'updateTime', width: COL.time },
  ]

  const hasRowAction =
    can('devops:template:save') ||
    can('devops:template:version:save') ||
    can('devops:template:version:list') ||
    can('devops:template:derivedList') ||
    can('devops:template:status') ||
    can('devops:template:deleted')

  if (hasRowAction) {
    columns.push({
      title: '操作',
      key: 'action',
      width: 240,
      render: (_, record) => {
        const items = menuItems(record)
        return (
          <div style={{ display: 'flex', alignItems: 'center', gap: 2 }}>
            {can('devops:template:version:save') && (
              <Tooltip title={record.derivedCount > 0 ? '已有派生流水线，合并由流水线侧发起，此处仍可维护模板定义' : '编辑模板定义'}>
                <Button
                  type="link"
                  size="small"
                  icon={<Settings2 size={14} />}
                  onClick={() => void openDefinition(record)}
                >
                  编辑定义
                </Button>
              </Tooltip>
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

  // 组织级模板仅平台管理员可建
  const canCreate = can('devops:template:save') && (activeScope === 1 || isSuper)

  return (
    <>
      <PageHeader
        title="流水线模板"
        description="组织级与团队级模板按版本快照管理，新应用从模板派生独立副本，保证交付流程口径一致。"
        tag={<span className="ferry-caption">{activeScope === 0 ? '组织级全员可见' : '团队级按数据权限可见'}</span>}
        extra={
          canCreate && (
            <Button
              type="primary"
              icon={<Plus size={14} />}
              onClick={() => setEditor({ mode: 'create', template: null })}
            >
              新建模板
            </Button>
          )
        }
      />

      <section className="ferry-panel">
        <Tabs
          activeKey={String(activeScope)}
          onChange={changeScope}
          items={[
            { key: '0', label: '组织级' },
            { key: '1', label: '团队级' },
          ]}
        />
        <div className="ferry-toolbar">
          <Input
            allowClear
            prefix={<Search size={15} style={{ color: 'var(--text-4)' }} />}
            placeholder="模板名称"
            style={{ width: 220 }}
            value={draft.keyword}
            onChange={(event) => setDraft((prev) => ({ ...prev, keyword: event.target.value }))}
            onPressEnter={applyFilters}
          />
          <Select<number>
            allowClear
            placeholder="状态"
            style={{ width: 140 }}
            options={[
              { value: 1, label: '启用' },
              { value: 0, label: '停用' },
            ]}
            value={draft.status}
            onChange={(value) => setDraft((prev) => ({ ...prev, status: value }))}
          />
          <Button type="primary" icon={<Search size={15} />} onClick={applyFilters}>查询</Button>
          <Button icon={<RotateCw size={15} />} onClick={resetFilters}>重置</Button>
        </div>

        <div className="ferry-table-wrap">
          <Table<PipelineTemplateRow>
            rowKey="id"
            columns={columns}
            dataSource={listQuery.data?.records ?? []}
            loading={listQuery.isLoading || listQuery.isFetching}
            scroll={{ x: 'max-content' }}
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
        title="编辑模板基本信息"
        width={560}
        okText="保存"
        cancelText="取消"
        confirmLoading={metaMutation.isPending}
        onCancel={() => setMetaOpen(false)}
        onOk={() => void submitMeta()}
        destroyOnHidden
      >
        <Form<MetaFormValues> form={metaForm} layout="vertical" requiredMark="optional">
          <Form.Item label="层级" extra="层级与归属部门创建后不可修改">
            <Tag>{metaTarget?.scope === 0 ? '组织级' : '团队级'}</Tag>
          </Form.Item>
          <Form.Item
            name="name"
            label="模板名称"
            rules={[{ required: true, message: '请输入模板名称' }]}
          >
            <Input maxLength={100} placeholder="例如 后端服务主干流水线" />
          </Form.Item>
          <Form.Item name="description" label="适用场景">
            <Input.TextArea rows={3} maxLength={500} showCount placeholder="选填" />
          </Form.Item>
        </Form>
      </Modal>

      <TemplateEditorDrawer
        open={Boolean(editor)}
        mode={editor?.mode ?? 'create'}
        template={editor?.template ?? null}
        onClose={() => setEditor(null)}
        onSaved={invalidate}
      />

      <PipelineVersionPanel
        open={versionOpen}
        owner={versionTarget}
        adapter={templateVersionAdapter}
        onClose={() => setVersionOpen(false)}
        onChanged={invalidate}
      />

      <Drawer
        open={derivedOpen}
        width={820}
        title={derivedTarget ? `派生流水线 · ${derivedTarget.name}` : '派生流水线'}
        onClose={() => setDerivedOpen(false)}
        destroyOnHidden
      >
        <Table<PipelineRow>
          rowKey="id"
          size="small"
          columns={[
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
              render: (value: string) => <span style={{ fontWeight: 550 }}>{value}</span>,
            },
            {
              title: '当前版本',
              dataIndex: 'currentVersionNo',
              width: 100,
              render: (value?: number | null) =>
                value ? <span className="ferry-mono">V{value}</span> : <Tag>未配置</Tag>,
            },
            {
              title: '模板状态',
              key: 'outdated',
              width: 130,
              render: (_, record) =>
                record.sourceTemplateOutdated ? (
                  <Tag color="orange">有新版本模板</Tag>
                ) : (
                  <Tag color="green">已同步</Tag>
                ),
            },
            {
              title: '最后合并时间',
              dataIndex: 'sourceTemplateMergedTime',
              width: COL.time,
              render: (value?: string | null) => value || '—',
            },
            { title: '更新时间', dataIndex: 'updateTime', width: COL.time },
          ]}
          dataSource={derivedQuery.data?.records ?? []}
          loading={derivedQuery.isLoading || derivedQuery.isFetching}
          scroll={{ x: 'max-content' }}
          pagination={{
            current: derivedPage.current,
            pageSize: derivedPage.size,
            total: derivedQuery.data?.total ?? 0,
            showSizeChanger: false,
            showTotal: (total) => `共 ${total} 条`,
            onChange: (current, size) => setDerivedPage({ current, size }),
          }}
        />
      </Drawer>
    </>
  )
}
