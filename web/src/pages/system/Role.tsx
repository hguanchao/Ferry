import { useEffect, useMemo, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  App,
  Alert,
  Button,
  Checkbox,
  Drawer,
  Empty,
  Form,
  Input,
  InputNumber,
  Modal,
  Radio,
  Select,
  Switch,
  Table,
  Tabs,
  Tag,
  Tooltip,
  Tree,
  type TableColumnsType,
} from 'antd'
import {
  CopyOutlined,
  DeleteOutlined,
  EditOutlined,
  ExportOutlined,
  PlusOutlined,
  ReloadOutlined,
  SafetyCertificateOutlined,
  SearchOutlined,
} from '@ant-design/icons'
import PageHeader from '@/components/PageHeader'
import { BuiltinTag, EnabledTag } from '@/components/StatusTag'
import { appScopeApi, menuApi, roleApi } from '@/api/system'
import type { AppRow, DataScope, EnvRow, MenuNode, RoleRow } from '@/api/types'
import { exportCsv } from '@/lib/export'
import { useAuthStore } from '@/stores/auth'
import { showError } from '@/lib/notify'

interface Filters {
  name?: string
  code?: string
  status?: number
}

interface MenuTreeNode {
  key: string
  title: React.ReactNode
  children?: MenuTreeNode[]
}

function toMenuTree(nodes: MenuNode[], depth = 0): MenuTreeNode[] {
  return nodes.map((node) => ({
    key: node.id,
    title: (
      <span style={{ display: 'inline-flex', alignItems: 'center', gap: 6, fontSize: 13 }}>
        {node.name}
        {node.type === 2 && (
          <span className="ferry-mono" style={{ fontSize: 10.5, color: 'var(--text-4)' }}>
            {node.perms}
          </span>
        )}
        {node.type === 0 && <Tag style={{ fontSize: 10, lineHeight: '16px' }}>目录</Tag>}
      </span>
    ),
    children: node.children?.length ? toMenuTree(node.children, depth + 1) : undefined,
  }))
}

function collectIds(nodes: MenuNode[], acc: string[] = []): string[] {
  nodes.forEach((node) => {
    acc.push(node.id)
    if (node.children?.length) collectIds(node.children, acc)
  })
  return acc
}

export default function Role() {
  const { message, modal } = App.useApp()
  const queryClient = useQueryClient()
  const can = useAuthStore((state) => state.can)
  const currentUser = useAuthStore((state) => state.user)

  const [filters, setFilters] = useState<Filters>({})
  const [draft, setDraft] = useState<Filters>({})
  const [page, setPage] = useState({ current: 1, size: 30 })
  const [open, setOpen] = useState(false)
  const [editing, setEditing] = useState<RoleRow | null>(null)
  const [tab, setTab] = useState('basic')
  const [checkedKeys, setCheckedKeys] = useState<string[]>([])
  const [dataScope, setDataScope] = useState<DataScope>({ mode: 'custom', pairs: [], prodOps: false })
  const [prodModal, setProdModal] = useState(false)
  const [prodReason, setProdReason] = useState('')
  const [prodConfirmed, setProdConfirmed] = useState(false)
  const [form] = Form.useForm()

  const listQuery = useQuery({
    queryKey: ['roles', filters, page],
    queryFn: () => roleApi.pageList({ ...filters, ...page }),
  })

  const menuQuery = useQuery({ queryKey: ['menu-tree'], queryFn: () => menuApi.treeList({}) })

  const scopeQuery = useQuery({
    queryKey: ['scope-options'],
    queryFn: appScopeApi.options,
    retry: false,
    staleTime: 5 * 60_000,
  })

  const menuTreeData = useMemo(() => toMenuTree(menuQuery.data ?? []), [menuQuery.data])
  const allMenuIds = useMemo(() => collectIds(menuQuery.data ?? []), [menuQuery.data])
  const apps: AppRow[] = scopeQuery.data?.apps ?? []
  const envs: EnvRow[] = scopeQuery.data?.envs ?? []

  /**
   * 已授权菜单需要单独拉：列表接口不下发 menuIds。
   * 不拉就会拿空集合去覆盖授权（打开抽屉只改个名字，保存后权限被清空）。
   */
  const grantedQuery = useQuery({
    queryKey: ['role-menus', editing?.id],
    queryFn: () => roleApi.menuList(editing!.id),
    enabled: Boolean(editing?.id) && !editing?.menuIds,
    staleTime: 0,
    retry: false,
  })

  useEffect(() => {
    if (!open || !editing) return
    if (editing.menuIds) {
      setCheckedKeys(editing.menuIds)
      return
    }
    if (grantedQuery.data) {
      setCheckedKeys(grantedQuery.data.map((menu) => String(menu.id)))
    }
  }, [open, editing, grantedQuery.data])

  const invalidate = () => {
    void queryClient.invalidateQueries({ queryKey: ['roles'] })
    void queryClient.invalidateQueries({ queryKey: ['role-options'] })
    void queryClient.invalidateQueries({ queryKey: ['workbench-overview'] })
  }

  const saveMutation = useMutation({
    mutationFn: async (payload: { values: Record<string, unknown>; roleId?: string }) => {
      const text = await roleApi.save({ ...payload.values, id: payload.roleId })
      const roleId = payload.roleId ?? listQuery.data?.records.find((item) => item.code === payload.values.code)?.id
      // 空集合一律不提交：既可能是「本来就没授权」，也可能是「授权还没加载回来」，
      // 后端 authorizeMenu 是先删后插，误提交等于把角色权限清空
      if (roleId && checkedKeys.length > 0) {
        await roleApi.authorizeMenu(roleId, checkedKeys, dataScope)
      }
      return text
    },
    onSuccess: (text) => {
      message.success(text || '保存成功')
      setOpen(false)
      invalidate()
    },
    onError: showError,
  })

  const deleteMutation = useMutation({
    mutationFn: (ids: string[]) => roleApi.deleted(ids),
    onSuccess: (text) => {
      message.success(text || '删除成功')
      invalidate()
    },
    onError: showError,
  })

  const openEditor = (record: RoleRow | null) => {
    setEditing(record)
    setTab('basic')
    form.resetFields()
    if (record) {
      form.setFieldsValue({ ...record })
      // 授权菜单由 grantedQuery 拉回后回填（见上方 useEffect），这里只先清空避免串上一个角色的勾选
      setCheckedKeys(record.menuIds ?? [])
      // 后端暂未下发 dataScope 时用「按应用授权」的空集兜底：
      // 若默认成 all，保存时会把本来受限的角色悄悄放大成全部应用
      setDataScope(record.dataScope ?? { mode: 'custom', pairs: [], prodOps: false })
    } else {
      form.setFieldsValue({ status: 1, sort: 10 })
      setCheckedKeys([])
      setDataScope({ mode: 'custom', pairs: [], prodOps: false })
    }
    setOpen(true)
  }

  const submitBasic = async () => {
    const values = await form.validateFields()
    saveMutation.mutate({ values, roleId: editing?.id })
  }

  const copyRole = (record: RoleRow) => {
    modal.confirm({
      title: `复制角色「${record.name}」？`,
      content: '复制后生成独立角色，功能权限与数据权限与源角色一致，互不影响。',
      okText: '复制',
      cancelText: '取消',
      onOk: async () => {
        await roleApi.save({
          code: `${record.code}-copy`,
          name: `${record.name}（副本）`,
          status: 1,
          sort: record.sort + 1,
          remark: `复制自 ${record.name}`,
          menuIds: record.menuIds ?? [],
          dataScope: record.dataScope ?? { mode: 'custom', pairs: [], prodOps: false },
        })
        message.success('已复制角色')
        invalidate()
      },
    })
  }

  const confirmDelete = (record: RoleRow) => {
    modal.confirm({
      title: `删除角色「${record.name}」？`,
      content: '仅未绑定用户的非内置角色可删除。删除后该角色的授权记录不可恢复，审计保留。',
      okText: '删除',
      okButtonProps: { danger: true },
      cancelText: '取消',
      onOk: () => deleteMutation.mutateAsync([record.id]),
    })
  }

  const columns: TableColumnsType<RoleRow> = [
    {
      title: '角色名称',
      dataIndex: 'name',
      width: 204,
      render: (_, record) => (
        <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
          <span style={{ fontWeight: 550, color: 'var(--text-1)' }}>{record.name}</span>
          {record.builtin && <BuiltinTag />}
        </div>
      ),
    },
    {
      title: '角色编码',
      dataIndex: 'code',
      width: 204,
      render: (value: string) => (
        <span className="ferry-mono" style={{ fontSize: 12 }}>
          {value}
        </span>
      ),
    },
    {
      title: '排序',
      dataIndex: 'sort',
      width: 204,
      align: 'right',
      render: (value: number) => <span className="ferry-mono">{value ?? '—'}</span>,
    },
    {
      title: '状态',
      dataIndex: 'status',
      width: 204,
      render: (value: number) => <EnabledTag value={value} />,
    },
    {
      title: '创建时间',
      dataIndex: 'createTime',
      width: 204,
      render: (value: string) => (
        <span className="ferry-mono" style={{ fontSize: 12, color: 'var(--text-3)' }}>
          {value || '—'}
        </span>
      ),
    },
  ]

  columns.push({
    title: '操作',
    key: 'action',
    width: 170,
    render: (_, record) => (
      <div style={{ display: 'flex', alignItems: 'center', gap: 2 }}>
        <Button type="link" size="small" icon={<EditOutlined />} onClick={() => openEditor(record)}>
          编辑
        </Button>
        <Button type="link" size="small" icon={<CopyOutlined />} onClick={() => copyRole(record)}>
          复制
        </Button>
        <Tooltip
          title={
            record.builtin
              ? '系统预置角色不可删除'
              : (record.memberCount ?? 0) > 0
                ? '仍有成员绑定'
                : record.memberCount == null
                  ? '成员占用由后端校验，确认后删除'
                  : '删除'
          }
        >
          <Button
            type="link"
            size="small"
            danger
            icon={<DeleteOutlined />}
            disabled={Boolean(record.builtin) || (record.memberCount ?? 0) > 0}
            onClick={() => confirmDelete(record)}
          />
        </Tooltip>
      </div>
    ),
  })

  const prodGrant = () => {
    setProdReason('')
    setProdConfirmed(false)
    setProdModal(true)
  }

  return (
    <>
      <PageHeader
        title="角色管理"
        description="功能权限决定「能进哪些页面、能点哪些按钮」，数据权限决定「能看到哪些应用与环境」，两层互相独立。生产操作权限单独授予并留痕。"
        extra={
          <>
            <Button
              icon={<ExportOutlined />}
              onClick={() => {
                const rows = listQuery.data?.records ?? []
                exportCsv<RoleRow>({
                  filename: '角色列表',
                  rows,
                  operator: currentUser?.nickname ?? '未知',
                  filters: [filters.name && `角色名包含 ${filters.name}`].filter(Boolean).join('；'),
                  columns: [
                    { title: '角色名称', value: (row) => row.name },
                    { title: '角色编码', value: (row) => row.code },
                    { title: '排序', value: (row) => row.sort },
                    { title: '状态', value: (row) => (row.status === 1 ? '正常' : '停用') },
                  ],
                })
                message.success(`已导出 ${rows.length} 条记录`)
              }}
            >
              导出
            </Button>
            {can('system:role:save') && (
              <Button type="primary" icon={<PlusOutlined />} onClick={() => openEditor(null)}>
                新增角色
              </Button>
            )}
          </>
        }
      />

      <section className="ferry-panel">
        <div className="ferry-toolbar">
          <Input
            allowClear
            prefix={<SearchOutlined style={{ color: 'var(--text-4)' }} />}
            placeholder="角色名称"
            style={{ width: 180 }}
            value={draft.name}
            onChange={(event) => setDraft((prev) => ({ ...prev, name: event.target.value }))}
            onPressEnter={() => {
              setFilters(draft)
              setPage((prev) => ({ ...prev, current: 1 }))
            }}
          />
          <Input
            allowClear
            placeholder="角色编码"
            style={{ width: 160 }}
            value={draft.code}
            onChange={(event) => setDraft((prev) => ({ ...prev, code: event.target.value }))}
          />
          <Select
            allowClear
            placeholder="状态"
            style={{ width: 120 }}
            value={draft.status}
            onChange={(value) => setDraft((prev) => ({ ...prev, status: value }))}
            options={[
              { value: 1, label: '正常' },
              { value: 0, label: '已停用' },
            ]}
          />
          <Button
            type="primary"
            icon={<SearchOutlined />}
            onClick={() => {
              setFilters(draft)
              setPage((prev) => ({ ...prev, current: 1 }))
            }}
          >
            查询
          </Button>
          <Button
            icon={<ReloadOutlined />}
            onClick={() => {
              setDraft({})
              setFilters({})
              setPage((prev) => ({ ...prev, current: 1 }))
            }}
          >
            重置
          </Button>
        </div>

        <div className="ferry-table-wrap">
          <Table<RoleRow>
            rowKey="id"
            columns={columns}
            dataSource={listQuery.data?.records ?? []}
            loading={listQuery.isLoading || listQuery.isFetching}
            scroll={{ x: 1190 }}
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

      <Drawer
        open={open}
        size={760}
        title={editing ? `编辑角色 · ${editing.name}` : '新增角色'}
        onClose={() => setOpen(false)}
        destroyOnHidden
        footer={
          <div style={{ display: 'flex', justifyContent: 'flex-end', gap: 8 }}>
            <Button onClick={() => setOpen(false)}>取消</Button>
            <Button type="primary" loading={saveMutation.isPending} onClick={() => void submitBasic()}>
              保存
            </Button>
          </div>
        }
      >
        <Tabs
          activeKey={tab}
          onChange={setTab}
          items={[
            {
              key: 'basic',
              label: '基本信息',
              children: (
                <Form form={form} layout="vertical" requiredMark="optional">
                  <div style={{ display: 'grid', gridTemplateColumns: 'repeat(2, minmax(0, 1fr))', gap: '0 16px' }}>
                    <Form.Item name="name" label="角色名称" rules={[{ required: true, message: '请输入角色名称' }]}>
                      <Input placeholder="例如 发布经理" maxLength={20} />
                    </Form.Item>
                    <Form.Item
                      name="code"
                      label="角色编码"
                      rules={[
                        { required: true, message: '请输入角色编码' },
                        { pattern: /^[a-z][a-z0-9-]{1,19}$/, message: '小写字母开头，可含数字与中划线' },
                      ]}
                      extra={editing?.builtin ? '内置角色编码不可修改' : '用于接口鉴权与审计，创建后建议不改'}
                    >
                      <Input placeholder="例如 release" disabled={editing?.builtin} />
                    </Form.Item>
                    <Form.Item name="sort" label="显示顺序">
                      <InputNumber min={1} max={999} style={{ width: '100%' }} />
                    </Form.Item>
                    <Form.Item name="status" label="状态">
                      <Radio.Group
                        options={[
                          { value: 1, label: '正常' },
                          { value: 0, label: '停用' },
                        ]}
                      />
                    </Form.Item>
                  </div>
                  <Form.Item name="remark" label="职责描述">
                    <Input.TextArea rows={3} placeholder="说明该角色的职责边界，便于审计与交接" maxLength={200} showCount />
                  </Form.Item>
                  {editing?.builtin && (
                    <Alert
                      type="info"
                      showIcon
                      message="系统预置角色"
                      description="五个预置角色可复制、可修改权限，但不可删除。这是审计与应急恢复的兜底。"
                    />
                  )}
                </Form>
              ),
            },
            {
              key: 'menu',
              label: `功能权限（${checkedKeys.length}）`,
              children: (
                <div>
                  <Alert
                    type="info"
                    showIcon
                    style={{ marginBottom: 12 }}
                    message="勾选菜单即授予页面访问权，勾选按钮即授予操作权。父级菜单会自动保留，用于渲染导航。"
                  />
                  <div style={{ display: 'flex', gap: 8, marginBottom: 8 }}>
                    <Button size="small" onClick={() => setCheckedKeys(allMenuIds)}>
                      全选
                    </Button>
                    <Button size="small" onClick={() => setCheckedKeys([])}>
                      清空
                    </Button>
                    <span className="ferry-spacer" />
                    <span className="ferry-caption">共 {allMenuIds.length} 个权限点</span>
                  </div>
                  <div
                    style={{
                      border: '1px solid var(--border-1)',
                      borderRadius: 8,
                      padding: 12,
                      maxHeight: 460,
                      overflow: 'auto',
                    }}
                  >
                    <Tree
                      checkable
                      selectable={false}
                      defaultExpandAll
                      treeData={menuTreeData}
                      checkedKeys={checkedKeys}
                      onCheck={(checked) => {
                        const keys = Array.isArray(checked) ? checked : checked.checked
                        const half = Array.isArray(checked) ? [] : checked.halfChecked
                        setCheckedKeys(Array.from(new Set([...keys, ...half].map(String))))
                      }}
                    />
                  </div>
                </div>
              ),
            },
            {
              key: 'data',
              label: '数据权限',
              children: (
                <div>
                  <Alert
                    type="info"
                    showIcon
                    style={{ marginBottom: 14 }}
                    message="数据权限按「应用-环境」二元组生效"
                    description="未授权的组合既不展示也不可操作，接口层强制校验。生产环境操作需单独授予，且提交时要求操作人与确认人非同一人。"
                  />
                  <Radio.Group
                    value={dataScope.mode}
                    onChange={(event) => setDataScope((prev) => ({ ...prev, mode: event.target.value }))}
                    style={{ marginBottom: 14 }}
                  >
                    <Radio.Button value="all">全部应用</Radio.Button>
                    <Radio.Button value="custom">按应用授权</Radio.Button>
                  </Radio.Group>

                  {dataScope.mode === 'all' ? (
                    <div className="ferry-placeholder" style={{ padding: 32, border: '1px dashed var(--border-1)', borderRadius: 10 }}>
                      <SafetyCertificateOutlined style={{ fontSize: 22, color: 'var(--color-primary)' }} />
                      <span className="ferry-placeholder__title">已授予全部应用与环境的可见范围</span>
                      <span className="ferry-caption">谨慎使用。生产环境的写操作权限仍需下方单独开启。</span>
                    </div>
                  ) : (
                    <div className="ferry-matrix">
                      {apps.length === 0 && (
                        <Empty
                          image={Empty.PRESENTED_IMAGE_SIMPLE}
                          description="应用主数据尚未接入（应用管理模块规划中），当前无可授权对象"
                        />
                      )}
                      {apps.map((app) => {
                        const pair = dataScope.pairs.find((item) => item.appId === app.id)
                        return (
                          <div className="ferry-matrix__row" key={app.id} data-active={!!pair}>
                            <div className="ferry-matrix__app">
                              <span style={{ fontWeight: 500, color: 'var(--text-1)' }}>{app.name}</span>
                              <span className="ferry-matrix__code">{app.code}</span>
                              {app.core && <Tag color="red">核心链路</Tag>}
                              <span className="ferry-caption" style={{ marginLeft: 'auto' }}>
                                {app.team}
                              </span>
                            </div>
                            <div className="ferry-matrix__envs">
                              {envs.map((env) => {
                                const checked = pair?.envIds.includes(env.id) ?? false
                                return (
                                  <Checkbox
                                    key={env.id}
                                    checked={checked}
                                    onChange={(event) => {
                                      setDataScope((prev) => {
                                        const exists = prev.pairs.find((item) => item.appId === app.id)
                                        let pairs = [...prev.pairs]
                                        if (!exists) {
                                          if (!event.target.checked) return prev
                                          pairs.push({ appId: app.id, envIds: [env.id] })
                                        } else {
                                          const envIds = event.target.checked
                                            ? Array.from(new Set([...exists.envIds, env.id]))
                                            : exists.envIds.filter((id) => id !== env.id)
                                          pairs = pairs
                                            .map((item) => (item.appId === app.id ? { ...item, envIds } : item))
                                            .filter((item) => item.envIds.length > 0)
                                        }
                                        return { ...prev, pairs }
                                      })
                                    }}
                                  >
                                    <span style={{ fontSize: 12 }}>{env.short}</span>
                                  </Checkbox>
                                )
                              })}
                            </div>
                          </div>
                        )
                      })}
                    </div>
                  )}

                  <div
                    style={{
                      marginTop: 18,
                      padding: 14,
                      border: '1px solid var(--border-1)',
                      borderRadius: 10,
                      background: 'var(--bg-subtle)',
                    }}
                  >
                    <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
                      <Switch
                        checked={dataScope.prodOps}
                        onChange={(checked) => {
                          if (checked) prodGrant()
                          else
                            setDataScope((prev) => ({
                              ...prev,
                              prodOps: false,
                              prodOpsReason: undefined,
                              prodOpsGrantedAt: undefined,
                            }))
                        }}
                      />
                      <div>
                        <div style={{ fontSize: 13, fontWeight: 550, color: 'var(--text-1)' }}>生产环境操作权限</div>
                        <div className="ferry-caption">
                          {dataScope.prodOps
                            ? `已授予${dataScope.prodOpsReason ? `：${dataScope.prodOpsReason}` : ''}`
                            : '未授予。开启需二次确认并填写授权理由，记录进入审计。'}
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
              ),
            },
          ]}
        />
      </Drawer>

      <Modal
        open={prodModal}
        title="授予生产环境操作权限"
        okText="确认授予"
        cancelText="取消"
        width={520}
        okButtonProps={{ disabled: !prodConfirmed || prodReason.trim().length < 8 }}
        onCancel={() => setProdModal(false)}
        onOk={() => {
          setDataScope((prev) => ({
            ...prev,
            prodOps: true,
            prodOpsReason: prodReason.trim(),
            prodOpsGrantedAt: new Date().toLocaleString('zh-CN', { hour12: false }),
          }))
          setProdModal(false)
          message.success('已授予生产操作权限，保存后生效')
        }}
        destroyOnHidden
      >
        <Alert
          type="warning"
          showIcon
          style={{ marginBottom: 14 }}
          message="这是高风险授权"
          description="获得该权限的角色可执行生产回滚、生产发布等高危动作。授权理由与时间将写入审计，请确保有对应的变更单或保障任务。"
        />
        <div style={{ marginBottom: 8, fontSize: 13, color: 'var(--text-2)' }}>授权理由（不少于 8 个字）</div>
        <Input.TextArea
          rows={3}
          value={prodReason}
          onChange={(event) => setProdReason(event.target.value)}
          placeholder="例如：双十一保障期间，SRE 值班需要执行生产回滚"
          maxLength={200}
          showCount
        />
        <Checkbox
          style={{ marginTop: 12 }}
          checked={prodConfirmed}
          onChange={(event) => setProdConfirmed(event.target.checked)}
        >
          我已确认该授权的风险，并已获得对应负责人批准
        </Checkbox>
      </Modal>
    </>
  )
}
