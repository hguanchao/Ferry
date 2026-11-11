import { useMemo, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  App as AntApp,
  Button,
  Form,
  Input,
  Modal,
  Radio,
  Select,
  Table,
  Tag,
  Tooltip,
  type TableColumnsType,
} from 'antd'
import { Pencil, Plus, RotateCw, Search, Trash2 } from 'lucide-react'
import PageHeader from '@/components/PageHeader'
import { EnabledTag } from '@/components/StatusTag'
import { appApi } from '@/api/devops'
import { deptApi, userApi } from '@/api/system'
import type { AppRow } from '@/api/types'
import { COL } from '@/lib/table'
import { flattenTree } from '@/lib/tree'
import { useAuthStore } from '@/stores/auth'
import { showError } from '@/lib/notify'

interface Filters {
  keyword?: string
  status?: number
  isCore?: number
  repoType?: string
}

const REPO_TYPES = [
  { value: 'GITLAB', label: 'GitLab' },
  { value: 'GITHUB', label: 'GitHub' },
  { value: 'GITEE', label: 'Gitee' },
]

export default function App() {
  const { message, modal } = AntApp.useApp()
  const queryClient = useQueryClient()
  const can = useAuthStore((state) => state.can)

  const [filters, setFilters] = useState<Filters>({})
  const [draft, setDraft] = useState<Filters>({})
  const [page, setPage] = useState({ current: 1, size: 30 })
  const [open, setOpen] = useState(false)
  const [editing, setEditing] = useState<AppRow | null>(null)
  const [form] = Form.useForm()

  const listQuery = useQuery({
    queryKey: ['apps', filters, page],
    queryFn: () => appApi.pageList({ ...filters, ...page }),
  })

  const deptQuery = useQuery({ queryKey: ['dept-tree'], queryFn: () => deptApi.treeList({}) })
  const depts = useMemo(() => flattenTree(deptQuery.data ?? []), [deptQuery.data])
  const deptName = useMemo(() => new Map(depts.map((dept) => [dept.id, dept.name])), [depts])

  // 负责人候选来自用户列表；应用管理员若没有用户列表权限，这里会明确报出接口错误，不静默降级
  const userQuery = useQuery({
    queryKey: ['user-options'],
    queryFn: () => userApi.pageList({ current: 1, size: 200 }),
    retry: false,
  })
  const userOptions = useMemo(
    () => (userQuery.data?.records ?? []).map((user) => ({ value: user.id, label: user.nickname || user.account })),
    [userQuery.data],
  )

  const invalidate = () => {
    void queryClient.invalidateQueries({ queryKey: ['apps'] })
    void queryClient.invalidateQueries({ queryKey: ['workbench-overview'] })
  }

  const saveMutation = useMutation({
    mutationFn: (values: Record<string, unknown>) => appApi.save({ ...values, id: editing?.id }),
    onSuccess: (text) => {
      message.success(text || '保存成功')
      setOpen(false)
      invalidate()
    },
    onError: showError,
  })

  const deleteMutation = useMutation({
    mutationFn: (ids: string[]) => appApi.deleted(ids),
    onSuccess: (text) => {
      message.success(text || '删除成功')
      invalidate()
    },
    onError: showError,
  })

  const openEditor = (record: AppRow | null) => {
    setEditing(record)
    form.resetFields()
    if (record) {
      // 后端原样下发逗号串，多选控件要的是数组
      form.setFieldsValue({ ...record, ownerIds: record.ownerIds ? record.ownerIds.split(',').filter(Boolean) : [] })
    } else {
      form.setFieldsValue({ status: 1, isCore: 0, ownerIds: [] })
    }
    setOpen(true)
  }

  const submit = async () => {
    const values = await form.validateFields()
    // 后端按逗号串收发：名称从下拉候选项取出，与 id 保持同一顺序
    const ownerIds = (values.ownerIds ?? []) as string[]
    saveMutation.mutate({
      ...values,
      ownerIds: ownerIds.join(','),
      ownerNames: ownerIds
        .map((id) => userOptions.find((option) => option.value === id)?.label ?? id)
        .join(','),
    })
  }

  const confirmDelete = (record: AppRow) => {
    modal.confirm({
      title: `删除应用「${record.name}」？`,
      content: '逻辑删除后列表不再展示，历史流水线与发布单仍按应用编码保留引用。',
      okText: '删除',
      okButtonProps: { danger: true },
      cancelText: '取消',
      onOk: () => deleteMutation.mutateAsync([record.id]),
    })
  }

  const columns: TableColumnsType<AppRow> = [
    { title: '应用名称', dataIndex: 'name', render: (value: string) => <span style={{ fontWeight: 550 }}>{value}</span> },
    {
      title: '应用编码',
      dataIndex: 'code',
      width: COL.code,
      render: (value: string) => <span className="ferry-mono" style={{ fontSize: 12 }}>{value}</span>,
    },
    { title: '所属团队', dataIndex: 'deptId', render: (value: string) => deptName.get(value) ?? value },
    {
      title: '负责人',
      dataIndex: 'ownerNames',
      render: (value: string) => value ? value.split(',').filter(Boolean).join('、') : '—',
    },
    {
      title: '仓库地址',
      dataIndex: 'repoUrl',
      ellipsis: true,
      render: (value?: string) => value ? <span className="ferry-mono" style={{ fontSize: 12 }}>{value}</span> : '—',
    },
    { title: '默认分支', dataIndex: 'repoBranch', render: (value?: string) => value ?? '—' },
    {
      title: '核心链路',
      dataIndex: 'isCore',
      width: 90,
      render: (value: number) => (value === 1 ? <Tag color="red">是</Tag> : <Tag>否</Tag>),
    },
    { title: '状态', dataIndex: 'status', width: COL.status, render: (value: number) => <EnabledTag value={value} /> },
    { title: '创建时间', dataIndex: 'createTime', width: COL.time, render: (value: string) => value || '—' },
  ]

  columns.push({
    title: '操作',
    key: 'action',
    width: COL.action,
    render: (_, record) => (
      <div style={{ display: 'flex', alignItems: 'center', gap: 2 }}>
        {can('devops:app:save') && (
          <Button type="link" size="small" icon={<Pencil size={14} />} onClick={() => openEditor(record)}>编辑</Button>
        )}
        {can('devops:app:deleted') && (
          <Tooltip title="删除">
            <Button type="link" size="small" danger icon={<Trash2 size={14} />} onClick={() => confirmDelete(record)} />
          </Tooltip>
        )}
      </div>
    ),
  })

  return (
    <>
      <PageHeader
        title="应用管理"
        description="应用是交付管理的最小对象，下游流水线、制品、环境与发布单都按应用编码挂载，编码创建后不可修改。"
        extra={
          can('devops:app:save') && (
            <Button type="primary" icon={<Plus size={14} />} onClick={() => openEditor(null)}>新建应用</Button>
          )
        }
      />

      <section className="ferry-panel">
        <div className="ferry-toolbar">
          <Input
            allowClear
            prefix={<Search size={15} style={{ color: 'var(--text-4)' }} />}
            placeholder="应用编码 / 名称"
            style={{ width: 200 }}
            value={draft.keyword}
            onChange={(event) => setDraft((prev) => ({ ...prev, keyword: event.target.value }))}
            onPressEnter={() => { setFilters(draft); setPage((prev) => ({ ...prev, current: 1 })) }}
          />
          <Select
            allowClear
            placeholder="代码平台"
            style={{ width: 130 }}
            value={draft.repoType}
            onChange={(value) => setDraft((prev) => ({ ...prev, repoType: value }))}
            options={REPO_TYPES}
          />
          <Select
            allowClear
            placeholder="核心链路"
            style={{ width: 120 }}
            value={draft.isCore}
            onChange={(value) => setDraft((prev) => ({ ...prev, isCore: value }))}
            options={[{ value: 1, label: '是' }, { value: 0, label: '否' }]}
          />
          <Select
            allowClear
            placeholder="状态"
            style={{ width: 120 }}
            value={draft.status}
            onChange={(value) => setDraft((prev) => ({ ...prev, status: value }))}
            options={[{ value: 1, label: '正常' }, { value: 0, label: '已停用' }]}
          />
          <Button type="primary" icon={<Search size={15} />} onClick={() => { setFilters(draft); setPage((prev) => ({ ...prev, current: 1 })) }}>查询</Button>
          <Button icon={<RotateCw size={15} />} onClick={() => { setDraft({}); setFilters({}); setPage((prev) => ({ ...prev, current: 1 })) }}>重置</Button>
        </div>

        <div className="ferry-table-wrap">
          <Table<AppRow>
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
              pageSizeOptions: ['30', '100', '200'],
              showTotal: (total) => `共 ${total} 条`,
              onChange: (current, size) => setPage({ current, size }),
            }}
          />
        </div>
      </section>

      <Modal
        open={open}
        title={editing ? `编辑应用 · ${editing.name}` : '新建应用'}
        width={620}
        okText="保存"
        cancelText="取消"
        confirmLoading={saveMutation.isPending}
        onCancel={() => setOpen(false)}
        onOk={() => void submit()}
        destroyOnHidden
      >
        <Form form={form} layout="vertical" requiredMark="optional">
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(2, minmax(0, 1fr))', gap: '0 16px' }}>
            <Form.Item name="code" label="应用编码" rules={[
              { required: true, message: '请输入应用编码' },
              { pattern: /^[a-z0-9][a-z0-9-]{1,39}$/, message: '小写字母或数字开头，可含中划线，2-40 位' },
            ]} extra={editing ? '创建后不可修改' : undefined}>
              <Input placeholder="例如 order-service" disabled={Boolean(editing)} />
            </Form.Item>
            <Form.Item name="name" label="应用名称" rules={[{ required: true, message: '请输入应用名称' }]}>
              <Input placeholder="例如 订单服务" maxLength={100} />
            </Form.Item>
            <Form.Item name="deptId" label="所属团队" rules={[{ required: true, message: '请选择所属团队' }]}>
              <Select placeholder="请选择" options={depts.map((dept) => ({ value: dept.id, label: dept.name }))} showSearch optionFilterProp="label" />
            </Form.Item>
            <Form.Item name="ownerIds" label="应用负责人" rules={[{ required: true, message: '请选择应用负责人' }]}>
              <Select mode="multiple" maxCount={3} placeholder="最多 3 人" options={userOptions} showSearch optionFilterProp="label" />
            </Form.Item>
            <Form.Item name="repoType" label="代码托管平台">
              <Select allowClear placeholder="请选择" options={REPO_TYPES} />
            </Form.Item>
            <Form.Item name="repoUrl" label="仓库地址">
              <Input placeholder="https://..." />
            </Form.Item>
            <Form.Item name="repoBranch" label="默认分支">
              <Input placeholder="例如 main" />
            </Form.Item>
            <Form.Item name="repoCommitId" label="提交ID">
              <Input placeholder="选填，7-40 位十六进制" />
            </Form.Item>
            <Form.Item name="isCore" label="核心链路">
              <Radio.Group options={[{ value: 1, label: '是' }, { value: 0, label: '否' }]} />
            </Form.Item>
            <Form.Item name="status" label="状态">
              <Radio.Group options={[{ value: 1, label: '正常' }, { value: 0, label: '停用' }]} />
            </Form.Item>
          </div>
          <Form.Item name="description" label="应用描述">
            <Input.TextArea rows={2} maxLength={500} showCount />
          </Form.Item>
        </Form>
      </Modal>
    </>
  )
}
