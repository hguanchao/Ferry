import { useMemo, useRef, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { App, Button, Form, Input, Modal, Select, Tag, Tooltip, TreeSelect, type SelectProps, type TableColumnsType } from 'antd'
import FerryTable from '@/components/FerryTable'
import { Pencil, Plus, RotateCw, Search, Trash2, WandSparkles } from 'lucide-react'
import PageHeader from '@/components/PageHeader'
import { appApi } from '@/api/devops'
import { deptApi, userApi } from '@/api/system'
import type { DeptNode, UserRow } from '@/api/types'
// TODO(dev_app-schema): dev_app_repo 已删除，AppRepoRow 及相关代码源功能已移除。
import type { AppRow } from '@/api/types-devops'
import { useAuthStore } from '@/stores/auth'
import { showError } from '@/lib/notify'
const PLATFORM_LABEL: Record<number, string> = { 1: 'GitLab', 2: 'GitHub', 3: 'Gitee' }
const STATUS_LABEL: Record<number, string> = { 0: '停用', 1: '启用', 2: '草稿', 3: '完善' }

const USER_PAGE_SIZE = 20

interface TreeNodeOption {
  value: string
  title: string
  children?: TreeNodeOption[]
}

function toTreeSelect(nodes: DeptNode[]): TreeNodeOption[] {
  return nodes.map((node) => ({
    value: node.id,
    title: node.name,
    children: node.children?.length ? toTreeSelect(node.children) : undefined,
  }))
}

interface UserOption {
  value: string
  label: string
}

function toUserOption(user: UserRow): UserOption {
  return { value: user.id, label: `${user.nickname}（${user.account}）` }
}

function mergeOptions(...groups: UserOption[][]): UserOption[] {
  const map = new Map<string, UserOption>()
  for (const group of groups) {
    for (const option of group) map.set(option.value, option)
  }
  return [...map.values()]
}

function SectionTitle({ title }: { title: string }) {
  return (
    <div style={{ fontSize: 13, fontWeight: 600, color: 'var(--text-1)', margin: '4px 0 12px',
      paddingLeft: 8, borderLeft: '3px solid var(--color-primary)' }}>{title}</div>
  )
}

export default function AppList() {
  const { message, modal } = App.useApp()
  const queryClient = useQueryClient()
  const can = useAuthStore((state) => state.can)
  const [form] = Form.useForm()
  const [open, setOpen] = useState(false)
  const [editing, setEditing] = useState<AppRow | null>(null)
  const [draft, setDraft] = useState<{ code?: string; name?: string; repoTypes?: number[]; coreLink?: number }>({})
  const [filters, setFilters] = useState<Record<string, unknown>>({})
  const [page, setPage] = useState({ current: 1, size: 30 })

  // 用户下拉：分页加载 + 搜索，已加载选项去重累加
  const [userOptions, setUserOptions] = useState<UserOption[]>([])
  // 编辑回显的负责人选项：单独保存，避免被分页刷新覆盖
  const [presetOptions, setPresetOptions] = useState<UserOption[]>([])
  const userPageRef = useRef(1)
  const userTotalRef = useRef(0)
  const userKeywordRef = useRef('')
  const [userLoading, setUserLoading] = useState(false)

  const query = useQuery({
    queryKey: ['apps', filters, page],
    queryFn: () => appApi.pageList({ ...filters, ...page }),
  })

  const deptTreeQuery = useQuery({
    queryKey: ['dept-tree-options'],
    queryFn: () => deptApi.treeList(),
  })

  const deptTreeData = useMemo(
    () => toTreeSelect(deptTreeQuery.data ?? []),
    [deptTreeQuery.data],
  )

  // 选中负责人的标签选项（含编辑回显）始终可见，不受搜索分页刷新影响
  const userSelectOptions = useMemo(
    () => mergeOptions(userOptions, presetOptions),
    [presetOptions, userOptions],
  )

  const loadUsers = async (keyword: string, reset: boolean) => {
    if (userLoading) return
    if (reset) {
      userPageRef.current = 1
      userTotalRef.current = 0
    } else if (userTotalRef.current > 0 && (userPageRef.current - 1) * USER_PAGE_SIZE >= userTotalRef.current) {
      return
    }
    setUserLoading(true)
    try {
      const result = await userApi.pageList({ current: userPageRef.current, size: USER_PAGE_SIZE, nickname: keyword || undefined })
      userTotalRef.current = result.total
      setUserOptions((prev) => mergeOptions(reset ? [] : prev, result.records.map(toUserOption)))
      userPageRef.current += 1
    } catch (error) {
      showError(error)
    } finally {
      setUserLoading(false)
    }
  }

  const handleUserSearch: SelectProps['onSearch'] = (value) => {
    userKeywordRef.current = value ?? ''
    void loadUsers(userKeywordRef.current, true)
  }

  const handleUserScroll: SelectProps['onPopupScroll'] = (e) => {
    const target = e.target as HTMLDivElement
    if (target.scrollTop + target.clientHeight >= target.scrollHeight - 20) {
      void loadUsers(userKeywordRef.current, false)
    }
  }

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
    userKeywordRef.current = ''
    setPresetOptions([])
    void loadUsers('', true)
    if (record) {
      const ownerId = record.ownerId ? String(record.ownerId) : undefined
      if (ownerId) {
        const preset: UserOption[] = [{ value: ownerId, label: record.ownerName ? `${record.ownerName}（${ownerId}）` : ownerId }]
        setPresetOptions(preset)
      }
      form.setFieldsValue({
        code: record.code,
        name: record.name,
        deptId: record.deptId,
        ownerId,
        coreLink: record.coreLink,
        repoType: record.repoType,
        repoUrl: record.repoUrl,
        repoBranch: record.repoBranch,
        repoCommitId: record.repoCommitId,
        description: record.description,
      })
    }
    setOpen(true)
  }

  const columns: TableColumnsType<AppRow> = [
    {
      title: '应用编码', dataIndex: 'code',
      render: (value: string) => <span className="ferry-mono" style={{ fontSize: 12 }}>{value}</span>,
    },
    { title: '应用名称', dataIndex: 'name',
      render: (value: string, record) => (
        <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
          <span style={{ fontWeight: 500, color: 'var(--text-1)' }}>{value}</span>
          {record.coreLink === 1 && <Tag color="red" style={{ fontSize: 10 }}>核心</Tag>}
        </div>
      ),
    },
    { title: '所属团队', dataIndex: 'deptName',
      render: (value?: string) => value || '—',
    },
    { title: '负责人', dataIndex: 'ownerName', },
    { title: '代码平台', dataIndex: 'repoType',
      render: (value: number) => PLATFORM_LABEL[value] ?? '—',
    },
    { title: '仓库地址', dataIndex: 'repoUrl',
      render: (value?: string) => <Tooltip title={value}><span className="ferry-mono" style={{ fontSize: 12 }}>{value || '—'}</span></Tooltip>,
    },
    { title: '应用状态', dataIndex: 'status',
      render: (value: number) => <Tag color={value === 3 ? 'green' : value === 0 ? 'default' : 'orange'}>{STATUS_LABEL[value] ?? '—'}</Tag>,
    },
    { title: '创建时间', dataIndex: 'createTime',
      render: (value: string) => <span className="ferry-mono" style={{ fontSize: 12, color: 'var(--text-3)' }}>{value ?? '—'}</span>,
    },
    { title: '操作', key: 'action',
      render: (_, record) => (
        <div style={{ display: 'flex', alignItems: 'center', gap: 2 }}>
          {can('devops:app:save') && (
            <Tooltip title="编辑">
              <Button type="link" size="small" icon={<Pencil size={14} />} onClick={() => openEditor(record)} />
            </Tooltip>
          )}
          {/* TODO(dev_app-schema): dev_app_repo 已删除，代码源操作移除；仓库信息在应用表单维护。 */}
          <Tooltip title="向导">
            <Button type="link" size="small" icon={<WandSparkles size={14} />} />
          </Tooltip>
          {can('devops:app:deleted') && (
            <Tooltip title="删除">
              <Button type="link" size="small" danger icon={<Trash2 size={14} />}
                onClick={() => modal.confirm({
                  title: `删除应用「${record.name}」？`,
                  content: '存在进行中运行、部署或未关闭发布单时不可删除，删除后历史数据仍可查。',
                  okText: '删除', okButtonProps: { danger: true }, cancelText: '取消',
                  onOk: () => deleteMutation.mutateAsync([record.id]),
                })}
              />
            </Tooltip>
          )}
        </div>
      ),
    },
  ]

  return (
    <>
      <PageHeader title="应用管理" description="应用是交付域的主数据，编码创建后不可修改。建档后可配置代码源、接入向导与详情聚合。"
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
          <Select allowClear placeholder="代码平台" style={{ width: 130 }} value={draft.repoTypes?.[0]}
            onChange={(value) => setDraft((prev) => ({ ...prev, repoTypes: value == null ? undefined : [value] }))}
            options={[{ value: 1, label: 'GitLab' }, { value: 2, label: 'GitHub' }, { value: 3, label: 'Gitee' }]}
          />
          <Select allowClear placeholder="核心链路" style={{ width: 120 }} value={draft.coreLink}
            onChange={(value) => setDraft((prev) => ({ ...prev, coreLink: value }))}
            options={[{ value: 1, label: '是' }, { value: 0, label: '否' }]}
          />
          <Button type="primary" icon={<Search size={15} />}
            onClick={() => { setFilters(draft); setPage((p) => ({ ...p, current: 1 })) }}>查询</Button>
          <Button icon={<RotateCw size={15} />}
            onClick={() => { setDraft({}); setFilters({}); setPage((p) => ({ ...p, current: 1 })) }}>重置</Button>
        </div>
        <div className="ferry-table-wrap">
          <FerryTable<AppRow> rowKey="id" columns={columns} dataSource={query.data?.records ?? []}
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
        okText="保存" cancelText="取消" width={720} confirmLoading={saveMutation.isPending}
        onCancel={() => setOpen(false)}
        onOk={async () => {
          const values = await form.validateFields()
          saveMutation.mutate({ ...values, id: editing?.id })
        }}
        destroyOnHidden>
        <Form form={form} layout="vertical" requiredMark="optional">
          <SectionTitle title="基础信息" />
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(2, minmax(0, 1fr))', gap: '0 16px' }}>
            <Form.Item name="code" label="应用编码" rules={[{ required: true, message: '请输入应用编码' }, { pattern: /^[a-z0-9-]{2,40}$/, message: '小写字母、数字、中划线，2-40 位' }]}
              extra="创建后不可修改">
              <Input placeholder="例如 order-service" maxLength={40} disabled={!!editing} />
            </Form.Item>
            <Form.Item name="name" label="应用名称" rules={[{ required: true, message: '请输入应用名称' }]}>
              <Input placeholder="例如 订单服务" maxLength={100} />
            </Form.Item>
            <Form.Item name="deptId" label="所属团队" rules={[{ required: true, message: '请选择所属团队' }]}>
              <TreeSelect
                treeData={deptTreeData}
                treeDefaultExpandAll
                showSearch
                treeNodeFilterProp="title"
                allowClear
                placeholder="请选择所属团队"
                loading={deptTreeQuery.isLoading}
              />
            </Form.Item>
            <Form.Item name="ownerId" label="应用负责人" rules={[{ required: true, message: '请选择应用负责人' }]}
              extra="负责人关联 sys_user，变更后立即生效">
              <Select
                showSearch
                allowClear
                placeholder="搜索并选择负责人"
                options={userSelectOptions}
                loading={userLoading}
                filterOption={false}
                onSearch={handleUserSearch}
                onPopupScroll={handleUserScroll}
                onFocus={() => { if (userOptions.length === 0) void loadUsers('', true) }}
              />
            </Form.Item>
          </div>

          <SectionTitle title="仓库信息" />
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(2, minmax(0, 1fr))', gap: '0 16px' }}>
            <Form.Item name="repoType" label="代码平台" rules={[{ required: true, message: '请选择代码平台' }]}>
              <Select options={[{ value: 1, label: 'GitLab' }, { value: 2, label: 'GitHub' }, { value: 3, label: 'Gitee' }]} />
            </Form.Item>
            <Form.Item name="repoBranch" label="仓库分支">
              <Input placeholder="例如 main" />
            </Form.Item>
            <Form.Item name="repoUrl" label="仓库 Git 地址" rules={[{ required: true, message: '请输入仓库地址' }]}>
              <Input placeholder="例如 https://gitlab.example.com/group/repo.git" />
            </Form.Item>
            <Form.Item name="repoToken" label="仓库访问令牌" extra="公开仓库可留空">
              <Input.Password placeholder="私有仓库填写 Token" autoComplete="new-password" />
            </Form.Item>
          </div>

          <SectionTitle title="补充信息" />
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(2, minmax(0, 1fr))', gap: '0 16px' }}>
            <Form.Item name="coreLink" label="核心链路">
              <Select options={[{ value: 0, label: '否' }, { value: 1, label: '是' }]} />
            </Form.Item>
            <Form.Item
              name="repoCommitId"
              label="指定提交 Id"
              rules={[{ pattern: /^[0-9a-fA-F]{7,40}$/, message: '需为 7–40 位十六进制提交 ID' }]}
              extra="选填；作为手动触发的默认提交，留空表示由触发时指定"
            >
              <Input placeholder="例如 29aaf3f" />
            </Form.Item>
            <Form.Item name="description" label="应用描述" style={{ gridColumn: '1 / -1' }}>
              <Input.TextArea placeholder="应用用途说明" rows={2} maxLength={500} />
            </Form.Item>
          </div>
        </Form>
      </Modal>
    </>
  )
}
