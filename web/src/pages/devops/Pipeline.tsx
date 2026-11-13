import { useMemo, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  App as AntApp,
  Button,
  Dropdown,
  Form,
  Input,
  Modal,
  Table,
  Tag,
  type MenuProps,
  type TableColumnsType,
} from 'antd'
import { History, MoreHorizontal, Pencil, Plus, RotateCw, Search, Settings2, Trash2 } from 'lucide-react'
import PageHeader from '@/components/PageHeader'
import AppSelect from '@/components/AppSelect'
import PipelineEditorDrawer from '@/components/PipelineEditorDrawer'
import PipelineVersionPanel from '@/components/PipelineVersionPanel'
import { pipelineApi } from '@/api/devops'
import type { PipelineDetail, PipelineRow } from '@/api/types'
import { COL } from '@/lib/table'
import { showError } from '@/lib/notify'
import { useAuthStore } from '@/stores/auth'

interface Filters {
  keyword?: string
  appIds?: string[]
}

interface MetaFormValues {
  appId: string
  name: string
  description?: string
}

interface EditorState {
  mode: 'create' | 'definition'
  pipeline: PipelineDetail | null
}

export default function Pipeline() {
  const { message, modal } = AntApp.useApp()
  const queryClient = useQueryClient()
  const can = useAuthStore((state) => state.can)

  const [filters, setFilters] = useState<Filters>({})
  const [draft, setDraft] = useState<Filters>({})
  const [page, setPage] = useState({ current: 1, size: 30 })
  const [editor, setEditor] = useState<EditorState | null>(null)
  const [metaTarget, setMetaTarget] = useState<PipelineRow | null>(null)
  const [metaOpen, setMetaOpen] = useState(false)
  const [metaForm] = Form.useForm<MetaFormValues>()
  const [versionTarget, setVersionTarget] = useState<PipelineRow | null>(null)
  const [versionOpen, setVersionOpen] = useState(false)

  const listQuery = useQuery({
    queryKey: ['pipelines', filters, page],
    queryFn: () => pipelineApi.pageList({ ...filters, ...page }),
  })

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
      render: (value: string) => <span style={{ fontWeight: 550 }}>{value}</span>,
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
    can('devops:pipeline:saveVersion')

  if (hasRowAction) {
    columns.push({
      title: '操作',
      key: 'action',
      width: COL.actionWide,
      render: (_, record) => {
        const items = menuItems()
        return (
          <div style={{ display: 'flex', alignItems: 'center', gap: 2 }}>
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
          can('devops:pipeline:save') && (
            <Button
              type="primary"
              icon={<Plus size={14} />}
              onClick={() => setEditor({ mode: 'create', pipeline: null })}
            >
              新建流水线
            </Button>
          )
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
            pagination={{
              current: page.current,
              pageSize: page.size,
              total: listQuery.data?.total ?? 0,
              showSizeChanger: true,
              pageSizeOptions: ['30', '100'],
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
        pipeline={versionTarget}
        onClose={() => setVersionOpen(false)}
        onChanged={invalidate}
      />
    </>
  )
}
