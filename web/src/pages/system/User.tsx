import { useEffect, useMemo, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  App,
  Avatar,
  Button,
  Dropdown,
  Form,
  Input,
  Modal,
  Radio,
  Select,
  Table,
  Tooltip,
  Tree,
  type MenuProps,
  type TableColumnsType,
} from 'antd'
import {
  Download,
  KeyRound,
  MoreHorizontal,
  Pencil,
  Plus,
  RotateCw,
  Search,
  Trash2,
  UserCog,
  Users,
} from 'lucide-react'
import PageHeader from '@/components/PageHeader'
import { EnabledTag, RoleTag } from '@/components/StatusTag'
import { deptApi, roleApi, userApi } from '@/api/system'
import type { DeptNode, RoleRow, UserRow } from '@/api/types'
import { exportCsv } from '@/lib/export'
import { dataScopeText } from '@/lib/dataScope'
import { COL } from '@/lib/table'
import { flattenTree } from '@/lib/tree'
import { useAuthStore } from '@/stores/auth'
import { showError } from '@/lib/notify'

interface Filters {
  account?: string
  nickname?: string
  phone?: string
  status?: number
  deptId?: string
}

function toTree(nodes: DeptNode[]): Array<{ key: string; title: string; children?: unknown[] }> {
  return nodes.map((node) => ({
    key: node.id,
    title: node.name,
    children: node.children?.length ? toTree(node.children) : undefined,
  }))
}

const ACCOUNT_PATTERN = /^[a-z][a-z0-9_]{1,19}$/

export default function User() {
  const { message, modal } = App.useApp()
  const queryClient = useQueryClient()
  const currentUser = useAuthStore((state) => state.user)
  const can = useAuthStore((state) => state.can)

  const [form] = Form.useForm()
  const [roleForm] = Form.useForm()
  const [filters, setFilters] = useState<Filters>({})
  const [draft, setDraft] = useState<Filters>({})
  const [page, setPage] = useState({ current: 1, size: 30 })
  const [selected, setSelected] = useState<string[]>([])
  const [expandedDepts, setExpandedDepts] = useState<string[]>([])
  const [editing, setEditing] = useState<UserRow | null>(null)
  const [editorOpen, setEditorOpen] = useState(false)
  const [roleTarget, setRoleTarget] = useState<UserRow | null>(null)

  const deptQuery = useQuery({ queryKey: ['dept-tree'], queryFn: () => deptApi.treeList({}) })
  const roleQuery = useQuery({
    queryKey: ['role-options'],
    queryFn: () => roleApi.pageList({ current: 1, size: 200 }),
  })

  const listQuery = useQuery({
    queryKey: ['users', filters, page],
    queryFn: () => userApi.pageList({ ...filters, ...page }),
  })

  const depts = useMemo(() => flattenTree(deptQuery.data ?? []), [deptQuery.data])

  useEffect(() => {
    if (!depts.length) return
    setExpandedDepts(depts.filter((dept) => dept.children?.length).map((dept) => dept.id))
  }, [depts])
  const deptName = useMemo(() => new Map(depts.map((dept) => [dept.id, dept.name])), [depts])
  const roles = roleQuery.data?.records ?? []
  const roleName = useMemo(() => new Map(roles.map((role) => [role.id, role.name])), [roles])
  const roleMap = useMemo(() => new Map(roles.map((role) => [role.id, role])), [roles])

  /** 列表上的数据权限摘要：超管等同于全部数据，其余取所属角色的数据范围 */
  const scopeOf = (record: UserRow) => {
    if (record.isSuper === 1) return '全部数据'
    const role = record.roleId ? roleMap.get(record.roleId) : undefined
    if (!role) return '未分配角色'
    // 后端未下发时不能假装成「什么都看不到」，明确标为待接入
    if (!role.dataScope) return '待接入'
    return dataScopeText(role.dataScope)
  }

  const invalidate = () => {
    void queryClient.invalidateQueries({ queryKey: ['users'] })
    void queryClient.invalidateQueries({ queryKey: ['workbench-overview'] })
  }

  const saveMutation = useMutation({
    mutationFn: (values: Record<string, unknown>) => userApi.save(values),
    onSuccess: (text) => {
      message.success(text || '保存成功')
      setEditorOpen(false)
      invalidate()
    },
    onError: showError,
  })

  const deleteMutation = useMutation({
    mutationFn: (ids: string[]) => userApi.deleted(ids),
    onSuccess: (text) => {
      message.success(text || '删除成功')
      setSelected([])
      invalidate()
    },
    onError: showError,
  })

  const resetMutation = useMutation({
    mutationFn: (id: string) => userApi.resetPassword(id),
    onSuccess: (text) => {
      message.success(text || '重置成功')
      invalidate()
    },
    onError: showError,
  })

  const roleMutation = useMutation({
    mutationFn: ({ userId, roleId }: { userId: string; roleId: string }) => userApi.authorizeRole(userId, roleId),
    onSuccess: (text) => {
      message.success(text || '授权成功')
      setRoleTarget(null)
      invalidate()
    },
    onError: showError,
  })

  const openCreate = () => {
    setEditing(null)
    form.resetFields()
    form.setFieldsValue({ status: 1, gender: 1 })
    setEditorOpen(true)
  }

  const openEdit = (record: UserRow) => {
    setEditing(record)
    form.resetFields()
    form.setFieldsValue({
      ...record,
      roleId: record.roleId ?? undefined,
      deptId: record.deptId ?? undefined,
      postId: record.postId ?? undefined,
    })
    setEditorOpen(true)
  }

  const submit = async () => {
    const values = await form.validateFields()
    saveMutation.mutate({ ...values, id: editing?.id })
  }

  const confirmDelete = (records: UserRow[]) => {
    const names = records.map((item) => item.nickname).join('、')
    modal.confirm({
      title: `删除 ${records.length} 个用户？`,
      content: `将删除：${names}。删除后其会话立即失效，审计记录保留。该操作不可撤销。`,
      okText: '删除',
      okButtonProps: { danger: true },
      cancelText: '取消',
      onOk: () => deleteMutation.mutateAsync(records.map((item) => item.id)),
    })
  }

  const confirmReset = (record: UserRow) => {
    modal.confirm({
      title: `重置「${record.nickname}」的密码？`,
      content: '重置为系统默认密码，并强制该账号所有在线会话下线。请通过安全渠道告知本人。',
      okText: '重置密码',
      cancelText: '取消',
      onOk: () => resetMutation.mutateAsync(record.id),
    })
  }

  const applyFilters = () => {
    setFilters(draft)
    setPage((prev) => ({ ...prev, current: 1 }))
  }

  const resetFilters = () => {
    setDraft({})
    setFilters({})
    setPage((prev) => ({ ...prev, current: 1 }))
  }

  const doExport = async () => {
    try {
      const result = await userApi.pageList({ ...filters, current: 1, size: 1000 })
      exportCsv<UserRow>({
        filename: '用户列表',
        rows: result.records,
        operator: currentUser?.nickname ?? '未知',
        filters: [
          filters.account && `账号包含 ${filters.account}`,
          filters.nickname && `昵称包含 ${filters.nickname}`,
          filters.status !== undefined && `状态 ${filters.status === 1 ? '正常' : '停用'}`,
          filters.deptId && `部门 ${deptName.get(filters.deptId) ?? filters.deptId}`,
        ]
          .filter(Boolean)
          .join('；'),
        columns: [
          { title: '账号', value: (row) => row.account },
          { title: '昵称', value: (row) => row.nickname },
          { title: '部门', value: (row) => (row.deptId ? deptName.get(row.deptId) ?? row.deptId : '') },
          { title: '角色', value: (row) => (row.roleId ? roleName.get(row.roleId) ?? row.roleId : '') },
          { title: '手机号', value: (row) => row.phone ?? '' },
          { title: '邮箱', value: (row) => row.email ?? '' },
          { title: '状态', value: (row) => (row.status === 1 ? '正常' : '停用') },
          { title: '最后登录', value: (row) => row.lastLoginTime ?? '' },
        ],
      })
      message.success(`已导出 ${result.records.length} 条记录`)
    } catch (error) {
      showError(error)
    }
  }

  const columns: TableColumnsType<UserRow> = [
    {
      title: '用户',
      dataIndex: 'nickname',

      render: (_, record) => (
        <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
          <Avatar
            size={30}
            style={{
              background: record.isSuper === 1 ? 'linear-gradient(145deg,#1c4fd6,#35b6ff)' : 'var(--bg-hover)',
              color: record.isSuper === 1 ? '#fff' : 'var(--text-2)',
              fontSize: 12.5,
              fontWeight: 600,
              flex: '0 0 auto',
            }}
          >
            {record.nickname.slice(0, 1)}
          </Avatar>
          <div style={{ minWidth: 0 }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
              <span style={{ fontWeight: 550, color: 'var(--text-1)' }}>{record.nickname}</span>
              {record.isSuper === 1 && <RoleTag>超管</RoleTag>}
            </div>
            <div style={{ display: 'flex', gap: 6, fontSize: 11.5, color: 'var(--text-3)' }}>
              <span className="ferry-mono">{record.account}</span>
              <span style={{ color: 'var(--text-4)' }}>·</span>
              <span className="ferry-mono">
                {record.lastLoginTime ? record.lastLoginTime.slice(5, 16) : '从未登录'}
              </span>
            </div>
          </div>
        </div>
      ),
    },
    {
      title: '部门',
      dataIndex: 'deptId',
      width: 180,

      ellipsis: true,
      render: (value: string | null) => (value ? deptName.get(value) ?? value : <span className="ferry-muted">未分配</span>),
    },
    {
      title: '角色 / 数据权限',
      dataIndex: 'roleId',
      width: 200,

      render: (value: string | null, record) => {
        const scope = scopeOf(record)
        return (
          <div style={{ lineHeight: 1.5 }}>
            {value ? <RoleTag>{roleName.get(value) ?? value}</RoleTag> : <span className="ferry-muted">未分配角色</span>}
            <div style={{ marginTop: 2 }}>
              <span style={{ fontSize: 11.5, color: 'var(--text-3)' }}>{scope}</span>
            </div>
          </div>
        )
      },
    },
    {
      title: '状态',
      dataIndex: 'status',
      width: COL.status,

      render: (value: number) => <EnabledTag value={value} />,
    },
  ]

  if (can('system:user:save') || can('system:user:deleted') || can('system:user:resetPassword')) {
    columns.push({
      title: '操作',
      key: 'action',
      width: COL.actionWide,

      render: (_, record) => {
        const moreItems: MenuProps['items'] = [
          can('system:user:authorizeRole') && {
            key: 'role',
            icon: <UserCog size={15} />,
            label: '分配角色',
            disabled: record.isSuper === 1,
          },
          can('system:user:resetPassword') && { key: 'reset', icon: <KeyRound size={14} />, label: '重置密码' },
          { type: 'divider' },
          can('system:user:deleted') && {
            key: 'delete',
            icon: <Trash2 size={14} />,
            label: '删除用户',
            danger: true,
            disabled: record.isSuper === 1 || record.id === currentUser?.userId,
          },
        ].filter(Boolean) as MenuProps['items']

        return (
          <div style={{ display: 'flex', alignItems: 'center', gap: 2 }}>
            {can('system:user:save') && (
              <Tooltip title="编辑">
                <Button type="link" size="small" icon={<Pencil size={14} />} onClick={() => openEdit(record)} />
              </Tooltip>
            )}
            <Tooltip title="更多">
              <Dropdown
                trigger={['click']}
                menu={{
                  items: moreItems,
                  onClick: ({ key }) => {
                    if (key === 'role') {
                      setRoleTarget(record)
                      roleForm.setFieldsValue({ roleId: record.roleId ?? undefined })
                    }
                    if (key === 'reset') confirmReset(record)
                    if (key === 'delete') confirmDelete([record])
                  },
                }}
              >
                <Button type="link" size="small" icon={<MoreHorizontal size={16} />} />
              </Dropdown>
            </Tooltip>
          </div>
        )
      },
    })
  }

  return (
    <>
      <PageHeader
        title="用户管理"
        description="账号是权限的载体。停用即时生效并踢下线；删除保留审计记录；密码只在本人手中。"
        tag={<RoleTag>共 {listQuery.data?.total ?? 0} 个账号</RoleTag>}
        extra={
          <>
            {can('system:user:export') && (
              <Button icon={<Download size={15} />} onClick={() => void doExport()}>
                导出
              </Button>
            )}
            {can('system:user:deleted') && (
              <Button
                danger
                icon={<Trash2 size={14} />}
                disabled={!selected.length}
                onClick={() => {
                  const records = (listQuery.data?.records ?? []).filter((item) => selected.includes(item.id))
                  if (records.length) confirmDelete(records)
                }}
              >
                批量删除{selected.length ? `（${selected.length}）` : ''}
              </Button>
            )}
            {can('system:user:save') && (
              <Button type="primary" icon={<Plus size={14} />} onClick={openCreate}>
                新增用户
              </Button>
            )}
          </>
        }
      />

      <div className="ferry-split">
        <aside className="ferry-split__aside">
          <div className="ferry-split__aside-head">
            <Users size={15} style={{ color: 'var(--text-3)' }} />
            组织架构
            {filters.deptId && (
              <Button
                type="link"
                size="small"
                style={{ marginLeft: 'auto', padding: 0 }}
                onClick={() => {
                  setDraft((prev) => ({ ...prev, deptId: undefined }))
                  setFilters((prev) => ({ ...prev, deptId: undefined }))
                }}
              >
                清除
              </Button>
            )}
          </div>
          <div className="ferry-tree-scroll">
            <Tree
              blockNode
              expandedKeys={expandedDepts}
              onExpand={(keys) => setExpandedDepts(keys.map(String))}
              selectedKeys={filters.deptId ? [filters.deptId] : []}
              treeData={[
                { key: '__all__', title: '全部部门' },
                ...toTree(deptQuery.data ?? []),
              ] as never}
              onSelect={(keys) => {
                const key = String(keys[0] ?? '')
                const deptId = key === '__all__' || !key ? undefined : key
                setDraft((prev) => ({ ...prev, deptId }))
                setFilters((prev) => ({ ...prev, deptId }))
                setPage((prev) => ({ ...prev, current: 1 }))
              }}
            />
          </div>
        </aside>

        <section className="ferry-panel">
          <div className="ferry-toolbar">
            <Input
              allowClear
              prefix={<Search size={15} style={{ color: 'var(--text-4)' }} />}
              placeholder="账号"
              style={{ width: 132 }}
              value={draft.account}
              onChange={(event) => setDraft((prev) => ({ ...prev, account: event.target.value }))}
              onPressEnter={applyFilters}
            />
            <Input
              allowClear
              placeholder="昵称"
              style={{ width: 124 }}
              value={draft.nickname}
              onChange={(event) => setDraft((prev) => ({ ...prev, nickname: event.target.value }))}
              onPressEnter={applyFilters}
            />
            <Input
              allowClear
              placeholder="手机号"
              style={{ width: 124 }}
              value={draft.phone}
              onChange={(event) => setDraft((prev) => ({ ...prev, phone: event.target.value }))}
              onPressEnter={applyFilters}
            />
            <Select
              allowClear
              placeholder="账号状态"
              style={{ width: 112 }}
              value={draft.status}
              onChange={(value) => setDraft((prev) => ({ ...prev, status: value }))}
              options={[
                { value: 1, label: '正常' },
                { value: 0, label: '已停用' },
              ]}
            />
            <Button type="primary" icon={<Search size={15} />} onClick={applyFilters}>
              查询
            </Button>
            <Button icon={<RotateCw size={15} />} onClick={resetFilters}>
              重置
            </Button>
          </div>

          <div className="ferry-table-wrap">
            <Table<UserRow>
              rowKey="id"
              columns={columns}
              dataSource={listQuery.data?.records ?? []}
              loading={listQuery.isLoading || listQuery.isFetching}
              scroll={{ x: 'max-content' }}
              rowSelection={{
                selectedRowKeys: selected,
                onChange: (keys) => setSelected(keys.map(String)),
                getCheckboxProps: (record) => ({
                  disabled: record.isSuper === 1 || record.id === currentUser?.userId,
                }),
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
      </div>

      <Modal
        open={editorOpen}
        title={editing ? `编辑用户 · ${editing.nickname}` : '新增用户'}
        okText="保存"
        cancelText="取消"
        confirmLoading={saveMutation.isPending}
        onOk={() => void submit()}
        onCancel={() => setEditorOpen(false)}
        width={640}
        destroyOnHidden
      >
        <Form form={form} layout="vertical" requiredMark="optional">
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(2, minmax(0, 1fr))', gap: '0 16px' }}>
            <Form.Item
              name="account"
              label="账号"
              rules={[
                { required: true, message: '请输入账号' },
                { pattern: ACCOUNT_PATTERN, message: '2-20 位小写字母开头，可含数字与下划线' },
              ]}
              extra={editing ? '账号创建后不可修改' : '创建后不可修改，用于登录'}
            >
              <Input placeholder="例如 caiyun" disabled={!!editing} maxLength={20} />
            </Form.Item>
            <Form.Item name="nickname" label="昵称" rules={[{ required: true, message: '请输入昵称' }]}>
              <Input placeholder="真实姓名或工号" maxLength={20} />
            </Form.Item>
            <Form.Item name="deptId" label="所属部门">
              <Select
                allowClear
                placeholder="请选择部门"
                options={depts.map((dept) => ({
                  value: dept.id,
                  label: `${dept.name}（${dept.code}）`,
                }))}
                showSearch
                optionFilterProp="label"
              />
            </Form.Item>
            <Form.Item name="roleId" label="角色">
              <Select
                allowClear
                placeholder="请选择角色"
                options={roles.map((role: RoleRow) => ({ value: role.id, label: role.name }))}
              />
            </Form.Item>
            <Form.Item
              name="phone"
              label="手机号"
              rules={[{ pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确' }]}
            >
              <Input placeholder="用于接收通知" maxLength={11} />
            </Form.Item>
            <Form.Item name="email" label="邮箱" rules={[{ type: 'email', message: '邮箱格式不正确' }]}>
              <Input placeholder="用于接收通知" maxLength={60} />
            </Form.Item>
            <Form.Item name="gender" label="性别">
              <Radio.Group
                options={[
                  { value: 1, label: '男' },
                  { value: 2, label: '女' },
                ]}
              />
            </Form.Item>
            <Form.Item name="status" label="账号状态" extra="停用后立即失效，在线会话被强制下线">
              <Radio.Group
                options={[
                  { value: 1, label: '正常' },
                  { value: 0, label: '停用' },
                ]}
              />
            </Form.Item>
          </div>
          {!editing && (
            <div className="ferry-caption" style={{ marginTop: -4 }}>
              新账号初始密码为系统参数 <code>SYS_DEFAULT_PASSWORD</code>，首次登录后请提醒本人修改。
            </div>
          )}
        </Form>
      </Modal>

      <Modal
        open={!!roleTarget}
        title={`分配角色 · ${roleTarget?.nickname ?? ''}`}
        okText="确认分配"
        cancelText="取消"
        confirmLoading={roleMutation.isPending}
        onCancel={() => setRoleTarget(null)}
        onOk={async () => {
          const values = await roleForm.validateFields()
          if (roleTarget) roleMutation.mutate({ userId: roleTarget.id, roleId: values.roleId })
        }}
        width={460}
        destroyOnHidden
      >
        <Form form={roleForm} layout="vertical">
          <Form.Item name="roleId" label="角色" rules={[{ required: true, message: '请选择角色' }]}>
            <Select
              placeholder="请选择角色"
              options={roles.map((role: RoleRow) => ({
                value: role.id,
                label: `${role.name}（${role.code}）`,
              }))}
            />
          </Form.Item>
        </Form>
        <div className="ferry-caption">
          角色调整即时生效。数据权限按角色下发的「应用-环境」范围计算，越权数据不可见。
        </div>
      </Modal>
    </>
  )
}
