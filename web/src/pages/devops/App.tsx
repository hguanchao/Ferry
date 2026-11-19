import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { App, Button, Form, Input, Modal, Select, Table, Tag, Tooltip, type TableColumnsType } from 'antd'
import { Pencil, Plus, RotateCw, Search, Trash2, WandSparkles, GitBranch } from 'lucide-react'
import PageHeader from '@/components/PageHeader'
import { RoleTag } from '@/components/StatusTag'
import { appApi } from '@/api/devops'
import type { AppRepoRow, AppRow } from '@/api/types-devops'
import { useAuthStore } from '@/stores/auth'
import { showError } from '@/lib/notify'
import { COL } from '@/lib/table'

const PLATFORM_LABEL: Record<number, string> = { 1: 'GitLab', 2: 'GitHub', 3: 'Gitee' }
const SETUP_LABEL: Record<number, string> = { 1: '待完善', 2: '已完善' }

export default function AppList() {
  const { message, modal } = App.useApp()
  const queryClient = useQueryClient()
  const can = useAuthStore((state) => state.can)
  const [form] = Form.useForm()
  const [repoForm] = Form.useForm()
  const [open, setOpen] = useState(false)
  const [editing, setEditing] = useState<AppRow | null>(null)
  const [repoOpen, setRepoOpen] = useState(false)
  const [repoApp, setRepoApp] = useState<AppRow | null>(null)
  const [repoId, setRepoId] = useState<string | undefined>()
  const [repoLoading, setRepoLoading] = useState(false)
  const [draft, setDraft] = useState<{ code?: string; name?: string; platform?: number; isCore?: number }>({})
  const [filters, setFilters] = useState<Record<string, unknown>>({})
  const [page, setPage] = useState({ current: 1, size: 30 })

  const query = useQuery({
    queryKey: ['apps', filters, page],
    queryFn: () => appApi.pageList({ ...filters, ...page }),
  })

  const invalidate = () => {
    void queryClient.invalidateQueries({ queryKey: ['apps'] })
    void queryClient.invalidateQueries({ queryKey: ['workbench-overview'] })
  }

  const saveMutation = useMutation({
    mutationFn: (values: Record<string, unknown>) => appApi.save(values),
    onSuccess: (text) => {
      message.success(text || '保存成功')
      setOpen(false)
      invalidate()
    },
    onError: showError,
  })

  const saveRepoMutation = useMutation({
    mutationFn: (values: Record<string, unknown>) => appApi.saveRepo(values),
    onSuccess: (text) => {
      message.success(text || '保存成功')
      setRepoOpen(false)
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
      form.setFieldsValue({
        code: record.code,
        name: record.name,
        teamId: record.teamId,
        ownerIds: record.ownerIds,
        platform: record.platform,
        isCore: record.isCore,
        repoPath: record.defaultBranch ? '' : undefined,
        description: undefined,
      })
    }
    setOpen(true)
  }

  const openRepo = async (record: AppRow) => {
    setRepoApp(record)
    setRepoId(undefined)
    repoForm.resetFields()
    repoForm.setFieldsValue({ platform: record.platform, isPublic: 0 })
    setRepoOpen(true)
    setRepoLoading(true)
    try {
      const repos = await appApi.repoList(record.id)
      const main = repos.find((repo: AppRepoRow) => repo.isMain === 1) ?? repos[0]
      if (main) {
        setRepoId(main.id)
        repoForm.setFieldsValue({ ...main, repoToken: main.repoToken ?? '' })
      }
    } catch (error) {
      showError(error)
    } finally {
      setRepoLoading(false)
    }
  }

  const columns: TableColumnsType<AppRow> = [
    {
      title: '应用编码', dataIndex: 'code', width: COL.code,
      render: (value: string) => <span className="ferry-mono" style={{ fontSize: 12 }}>{value}</span>,
    },
    { title: '应用名称', dataIndex: 'name',
      render: (value: string, record) => (
        <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
          <span style={{ fontWeight: 500, color: 'var(--text-1)' }}>{value}</span>
          {record.isCore === 1 && <Tag color="red" style={{ fontSize: 10 }}>核心</Tag>}
        </div>
      ),
    },
    { title: '负责人', dataIndex: 'ownerNames', width: 140 },
    { title: '代码平台', dataIndex: 'platform', width: 100,
      render: (value: number) => PLATFORM_LABEL[value] ?? '—',
    },
    { title: '仓库地址', dataIndex: 'repoUrl',
      render: (value?: string) => <Tooltip title={value}><span className="ferry-mono" style={{ fontSize: 12 }}>{value || '—'}</span></Tooltip>,
    },
    { title: '接入状态', dataIndex: 'setupStatus', width: 100,
      render: (value: number) => <Tag color={value === 2 ? 'green' : 'orange'}>{SETUP_LABEL[value] ?? '—'}</Tag>,
    },
    { title: '创建时间', dataIndex: 'createTime', width: COL.time,
      render: (value: string) => <span className="ferry-mono" style={{ fontSize: 12, color: 'var(--text-3)' }}>{value ?? '—'}</span>,
    },
    { title: '操作', key: 'action', width: COL.actionWide, fixed: 'right',
      render: (_, record) => (
        <div style={{ display: 'flex', alignItems: 'center', gap: 2 }}>
          {can('devops:app:save') && (
            <Button type="link" size="small" icon={<Pencil size={14} />} onClick={() => openEditor(record)}>编辑</Button>
          )}
          <Button type="link" size="small" icon={<GitBranch size={14} />} onClick={() => void openRepo(record)}>代码源</Button>
          <Button type="link" size="small" icon={<WandSparkles size={14} />}>向导</Button>
          {can('devops:app:deleted') && (
            <Button type="link" size="small" danger icon={<Trash2 size={14} />}
              onClick={() => modal.confirm({
                title: `删除应用「${record.name}」？`,
                content: '存在进行中运行、部署或未关闭发布单时不可删除，删除后历史数据仍可查。',
                okText: '删除', okButtonProps: { danger: true }, cancelText: '取消',
                onOk: () => deleteMutation.mutateAsync([record.id]),
              })}
            />
          )}
        </div>
      ),
    },
  ]

  return (
    <>
      <PageHeader title="应用管理" description="应用是交付域的主数据，编码创建后不可修改。建档后可配置代码源、接入向导与详情聚合。"
        tag={<RoleTag>共 {query.data?.total ?? 0} 项</RoleTag>}
        extra={can('devops:app:save') && (
          <Button type="primary" icon={<Plus size={14} />} onClick={() => openEditor(null)}>新建应用</Button>
        )}
      />
      <section className="ferry-panel">
        <div className="ferry-toolbar">
          <Input allowClear prefix={<Search size={15} style={{ color: 'var(--text-4)' }} />}
            placeholder="应用编码/名称" style={{ width: 200 }} value={draft.code}
            onChange={(e) => setDraft((prev) => ({ ...prev, code: e.target.value }))}
            onPressEnter={() => { setFilters(draft); setPage((p) => ({ ...p, current: 1 })) }}
          />
          <Select allowClear placeholder="代码平台" style={{ width: 130 }} value={draft.platform}
            onChange={(value) => setDraft((prev) => ({ ...prev, platform: value }))}
            options={[{ value: 1, label: 'GitLab' }, { value: 2, label: 'GitHub' }, { value: 3, label: 'Gitee' }]}
          />
          <Select allowClear placeholder="核心链路" style={{ width: 120 }} value={draft.isCore}
            onChange={(value) => setDraft((prev) => ({ ...prev, isCore: value }))}
            options={[{ value: 1, label: '是' }, { value: 0, label: '否' }]}
          />
          <Button type="primary" icon={<Search size={15} />}
            onClick={() => { setFilters(draft); setPage((p) => ({ ...p, current: 1 })) }}>查询</Button>
          <Button icon={<RotateCw size={15} />}
            onClick={() => { setDraft({}); setFilters({}); setPage((p) => ({ ...p, current: 1 })) }}>重置</Button>
        </div>
        <div className="ferry-table-wrap">
          <Table<AppRow> rowKey="id" columns={columns} dataSource={query.data?.records ?? []}
            loading={query.isLoading || query.isFetching} scroll={{ x: 'max-content' }}
            pagination={{
              current: page.current, pageSize: page.size, total: query.data?.total ?? 0,
              showSizeChanger: true, pageSizeOptions: ['30', '100', '200'],
              showTotal: (total) => `共 ${total} 条`, onChange: (current, size) => setPage({ current, size }),
            }}
          />
        </div>
      </section>

      <Modal open={open} title={editing ? `编辑应用 · ${editing.name}` : '新建应用'}
        okText="保存" cancelText="取消" width={560} confirmLoading={saveMutation.isPending}
        onCancel={() => setOpen(false)}
        onOk={async () => {
          const values = await form.validateFields()
          saveMutation.mutate({ ...values, id: editing?.id,
            ownerIds: Array.isArray(values.ownerIds) ? values.ownerIds.join(',') : values.ownerIds })
        }}
        destroyOnHidden>
        <Form form={form} layout="vertical" requiredMark="optional">
          <Form.Item name="code" label="应用编码" rules={[{ required: true, message: '请输入应用编码' }, { pattern: /^[a-z0-9-]{2,40}$/, message: '小写字母、数字、中划线，2-40 位' }]}
            extra="创建后不可修改">
            <Input placeholder="例如 order-service" maxLength={40} disabled={!!editing} />
          </Form.Item>
          <Form.Item name="name" label="应用名称" rules={[{ required: true, message: '请输入应用名称' }]}>
            <Input placeholder="例如 订单服务" maxLength={100} />
          </Form.Item>
          <Form.Item name="teamId" label="所属团队" rules={[{ required: true, message: '请选择所属团队' }]}>
            <Input placeholder="团队id" />
          </Form.Item>
          <Form.Item name="ownerIds" label="应用负责人（1-3 人）" rules={[{ required: true, message: '请输入负责人用户id，逗号分隔' }]}
            extra="多个用户id用逗号分隔，负责人变更立即生效">
            <Input placeholder="例如 1001,1002" />
          </Form.Item>
          <Form.Item name="platform" label="代码托管平台" rules={[{ required: true, message: '请选择平台' }]}>
            <Select options={[{ value: 1, label: 'GitLab' }, { value: 2, label: 'GitHub' }, { value: 3, label: 'Gitee' }]} />
          </Form.Item>
          <Form.Item name="isCore" label="核心链路">
            <Select options={[{ value: 0, label: '否' }, { value: 1, label: '是' }]} />
          </Form.Item>
        </Form>
      </Modal>

      <Modal open={repoOpen} title={repoApp ? `代码源 · ${repoApp.name}` : '代码源'}
        okText="保存" cancelText="取消" width={560} confirmLoading={saveRepoMutation.isPending || repoLoading}
        onCancel={() => setRepoOpen(false)}
        onOk={async () => {
          const values = await repoForm.validateFields()
          saveRepoMutation.mutate({ ...values, id: repoId, appId: repoApp?.id, isMain: 1 })
        }}
        destroyOnHidden>
        <Form form={repoForm} layout="vertical" requiredMark="optional">
          <Form.Item name="platform" label="代码托管平台" rules={[{ required: true, message: '请选择平台' }]}>
            <Select options={[{ value: 1, label: 'GitLab' }, { value: 2, label: 'GitHub' }, { value: 3, label: 'Gitee' }]} />
          </Form.Item>
          <Form.Item name="repoUrl" label="仓库地址（Git）" rules={[{ required: true, message: '请输入仓库地址' }]}>
            <Input placeholder="例如 https://gitlab.example.com/group/repo.git" />
          </Form.Item>
          <Form.Item name="repoPath" label="仓库路径" rules={[{ required: true, message: '请输入仓库路径' }]}
            extra="group/project 形式，供代码平台接口定位仓库">
            <Input placeholder="例如 group/repo" />
          </Form.Item>
          <Form.Item name="defaultBranch" label="默认分支">
            <Input placeholder="例如 main" />
          </Form.Item>
          <Form.Item name="repoToken" label="仓库访问令牌" extra="明文保存；公开仓库可留空">
            <Input.Password placeholder="私有仓库填写 Token" autoComplete="new-password" />
          </Form.Item>
          <Form.Item name="isPublic" label="仓库可见性">
            <Select options={[{ value: 0, label: '私有' }, { value: 1, label: '公开' }]} />
          </Form.Item>
        </Form>
      </Modal>
    </>
  )
}
