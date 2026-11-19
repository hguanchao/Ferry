import { useEffect, useMemo, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  App,
  Alert,
  Button,
  Drawer,
  Form,
  Input,
  InputNumber,
  Modal,
  Radio,
  Select,
  Table,
  Tabs,
  Tag,
  Tooltip,
  Tree,
  type TableColumnsType,
} from 'antd'
import { Copy, Download, Pencil, Plus, RotateCw, Search, ShieldCheck, Trash2 } from 'lucide-react'
import PageHeader from '@/components/PageHeader'
import { BuiltinTag, EnabledTag } from '@/components/StatusTag'
import { deptApi, menuApi, roleApi } from '@/api/system'
import type { DeptNode, MenuNode, RoleRow } from '@/api/types'
import { DATA_SCOPE_OPTIONS, dataScopeText } from '@/lib/dataScope'
import { exportCsv } from '@/lib/export'
import { COL } from '@/lib/table'
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

interface DeptTreeNode {
  key: string
  title: string
  children?: DeptTreeNode[]
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

function toDeptTree(nodes: DeptNode[]): DeptTreeNode[] {
  return nodes.map((node) => ({
    key: node.id,
    title: node.name,
    children: node.children?.length ? toDeptTree(node.children) : undefined,
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

  /** 授权抽屉：与新增/编辑完全独立，操作栏单独打开 */
  const [grantRole, setGrantRole] = useState<RoleRow | null>(null)
  const [grantTab, setGrantTab] = useState<'menu' | 'data'>('menu')
  const [checkedKeys, setCheckedKeys] = useState<string[]>([])
  /** 该角色的菜单授权是否已真正加载：加载失败时列表同样为空，点保存会以空集合覆盖授权 */
  const [menuLoaded, setMenuLoaded] = useState(false)
  /** 数据范围与已授权部门：同样要区分「没加载」与「就是空的」 */
  const [dataScope, setDataScope] = useState(2)
  const [deptKeys, setDeptKeys] = useState<string[]>([])
  const [dataLoaded, setDataLoaded] = useState(false)
  const [form] = Form.useForm()

  const listQuery = useQuery({
    queryKey: ['roles', filters, page],
    queryFn: () => roleApi.pageList({ ...filters, ...page }),
  })

  const menuQuery = useQuery({ queryKey: ['menu-tree'], queryFn: () => menuApi.treeList({}) })

  const menuTreeData = useMemo(() => toMenuTree(menuQuery.data ?? []), [menuQuery.data])
  const allMenuIds = useMemo(() => collectIds(menuQuery.data ?? []), [menuQuery.data])

  /** 部门树：授权范围只在超管手里配置，非超管看不到这个抽屉的保存按钮 */
  const deptQuery = useQuery({
    queryKey: ['dept-tree'],
    queryFn: () => deptApi.treeList({}),
    enabled: Boolean(grantRole),
  })

  const deptTreeData = useMemo(() => toDeptTree(deptQuery.data ?? []), [deptQuery.data])

  /** 已授权菜单单独拉：列表接口不下发 menuIds，授权抽屉每次打开都取最新值 */
  const grantedQuery = useQuery({
    queryKey: ['role-menus', grantRole?.id],
    queryFn: () => roleApi.menuList(grantRole!.id),
    enabled: Boolean(grantRole?.id) && !grantRole?.menuIds,
    staleTime: 0,
    retry: false,
  })

  /** 数据范围每次打开都取最新值，同样不吃缓存 */
  const dataScopeQuery = useQuery({
    queryKey: ['role-data-scope', grantRole?.id],
    queryFn: () => roleApi.dataScope(grantRole!.id),
    enabled: Boolean(grantRole?.id),
    staleTime: 0,
    retry: false,
  })

  useEffect(() => {
    if (!grantRole) return
    if (grantRole.menuIds) {
      setCheckedKeys(grantRole.menuIds)
      setMenuLoaded(true)
      return
    }
    if (grantedQuery.data) {
      setCheckedKeys(grantedQuery.data.map((menu) => String(menu.id)))
      setMenuLoaded(true)
    }
    // 加载失败时保持 false：提交空勾选等于清空授权，宁可让保存按钮不可用
  }, [grantRole, grantedQuery.data])

  useEffect(() => {
    if (!dataScopeQuery.data) return
    setDataScope(dataScopeQuery.data.dataScope)
    setDeptKeys((dataScopeQuery.data.deptIds ?? []).map(String))
    setDataLoaded(true)
    // 同上：取不到真实值时不能放开保存，否则会把数据范围改成默认的空自定义
  }, [dataScopeQuery.data])

  const invalidate = () => {
    void queryClient.invalidateQueries({ queryKey: ['roles'] })
    void queryClient.invalidateQueries({ queryKey: ['role-options'] })
    void queryClient.invalidateQueries({ queryKey: ['workbench-overview'] })
  }

  const saveMutation = useMutation({
    mutationFn: async (payload: { values: Record<string, unknown>; roleId?: string }) => {
      // 只提交角色主数据，菜单授权是独立操作，见 grantMutation
      return roleApi.save({ ...payload.values, id: payload.roleId })
    },
    onSuccess: (text) => {
      message.success(text || '保存成功')
      setOpen(false)
      invalidate()
    },
    onError: showError,
  })

  /** 菜单授权：与新增/编辑解耦，独立提交 */
  const grantMutation = useMutation({
    mutationFn: async (payload: { roleId: string; menuIds: string[] }) =>
      roleApi.authorizeMenu(payload.roleId, payload.menuIds),
    onSuccess: (text) => {
      message.success(text || '授权成功')
      setGrantRole(null)
      invalidate()
    },
    onError: showError,
  })

  /** 数据权限授权：同样独立提交，不与菜单授权互相覆盖 */
  const scopeMutation = useMutation({
    mutationFn: async (payload: { id: string; dataScope: number; deptIds: string[] }) =>
      roleApi.authorizeDataScope(payload.id, payload.dataScope, payload.deptIds),
    onSuccess: (text) => {
      message.success(text || '授权成功')
      setGrantRole(null)
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
    form.resetFields()
    if (record) {
      form.setFieldsValue({ ...record })
    } else {
      form.setFieldsValue({ status: 1, sort: 10 })
    }
    setOpen(true)
  }

  /** 打开授权抽屉：先清空上一轮的勾选，等查询拉回真实授权后回填 */
  const openGrant = (record: RoleRow) => {
    setGrantRole(record)
    setGrantTab('menu')
    setCheckedKeys([])
    setMenuLoaded(false)
    setDataScope(2)
    setDeptKeys([])
    setDataLoaded(false)
  }

  const submitBasic = async () => {
    const values = await form.validateFields()
    saveMutation.mutate({ values, roleId: editing?.id })
  }

  const copyRole = (record: RoleRow) => {
    modal.confirm({
      title: `复制角色「${record.name}」？`,
      content: '复制后生成独立角色，功能权限与源角色一致，之后各自维护、互不影响。',
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
      width: COL.code,
      render: (value: string) => (
        <span className="ferry-mono" style={{ fontSize: 12 }}>
          {value}
        </span>
      ),
    },
    {
      title: '排序',
      dataIndex: 'sort',
      width: COL.sort,
      align: 'right',
      render: (value: number) => <span className="ferry-mono">{value ?? '—'}</span>,
    },
    {
      title: '状态',
      dataIndex: 'status',
      width: COL.status,
      render: (value: number) => <EnabledTag value={value} />,
    },
    {
      title: '数据范围',
      dataIndex: 'dataScope',
      width: 130,
      render: (value?: number) => (
        <span style={{ fontSize: 12.5, color: 'var(--text-2)' }}>{dataScopeText(value)}</span>
      ),
    },
    {
      title: '创建时间',
      dataIndex: 'createTime',
      width: COL.time,
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
    width: COL.actionWide,
    render: (_, record) => (
      <div style={{ display: 'flex', alignItems: 'center', gap: 2 }}>
        <Tooltip title="编辑">
          <Button type="link" size="small" icon={<Pencil size={14} />} onClick={() => openEditor(record)} />
        </Tooltip>
        <Tooltip title="授权">
          <Button type="link" size="small" icon={<ShieldCheck size={14} />} onClick={() => openGrant(record)} />
        </Tooltip>
        <Tooltip title="复制">
          <Button type="link" size="small" icon={<Copy size={14} />} onClick={() => copyRole(record)} />
        </Tooltip>
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
            icon={<Trash2 size={14} />}
            disabled={Boolean(record.builtin) || (record.memberCount ?? 0) > 0}
            onClick={() => confirmDelete(record)}
          />
        </Tooltip>
      </div>
    ),
  })

  return (
    <>
      <PageHeader
        title="角色管理"
        description="功能权限决定「能进哪些页面、能点哪些按钮」，数据权限决定「能看到哪些应用与环境」，两层互相独立。生产操作权限单独授予并留痕。"
        extra={
          <>
            <Button
              icon={<Download size={15} />}
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
              <Button type="primary" icon={<Plus size={14} />} onClick={() => openEditor(null)}>
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
            prefix={<Search size={15} style={{ color: 'var(--text-4)' }} />}
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
            icon={<Search size={15} />}
            onClick={() => {
              setFilters(draft)
              setPage((prev) => ({ ...prev, current: 1 }))
            }}
          >
            查询
          </Button>
          <Button
            icon={<RotateCw size={15} />}
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
        title={editing ? `编辑角色 · ${editing.name}` : '新增角色'}
        width={560}
        okText="保存"
        cancelText="取消"
        confirmLoading={saveMutation.isPending}
        onCancel={() => setOpen(false)}
        onOk={() => void submitBasic()}
        destroyOnHidden
      >
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
      </Modal>

      <Drawer
        open={Boolean(grantRole)}
        size={620}
        title={`授权 · ${grantRole?.name ?? ''}`}
        onClose={() => setGrantRole(null)}
        destroyOnHidden
        footer={
          <div style={{ display: 'flex', justifyContent: 'flex-end', gap: 8 }}>
            <Button onClick={() => setGrantRole(null)}>取消</Button>
            {/* 两个 Tab 各自提交：一次点击只改一种授权，避免菜单与数据范围互相覆盖 */}
            {grantTab === 'menu' ? (
              <Button
                type="primary"
                loading={grantMutation.isPending}
                // 授权还没加载回来就点保存，会用空勾选覆盖掉真实授权
                disabled={!menuLoaded}
                onClick={() => {
                  if (!grantRole) return
                  grantMutation.mutate({ roleId: grantRole.id, menuIds: checkedKeys })
                }}
              >
                保存功能权限
              </Button>
            ) : (
              <Button
                type="primary"
                loading={scopeMutation.isPending}
                disabled={!dataLoaded}
                onClick={() => {
                  if (!grantRole) return
                  // 非自定义范围不提交部门，避免旧勾选留在库里，切回自定义时悄悄生效
                  scopeMutation.mutate({
                    id: grantRole.id,
                    dataScope,
                    deptIds: dataScope === 2 ? deptKeys : [],
                  })
                }}
              >
                保存数据权限
              </Button>
            )}
          </div>
        }
      >
        <Tabs
          activeKey={grantTab}
          onChange={(key) => setGrantTab(key as 'menu' | 'data')}
          items={[
            {
              key: 'menu',
              label: '功能权限',
              children: (
                <>
                  <div className="ferry-toolbar" style={{ padding: '0 0 10px', borderBottom: 'none' }}>
                    <span className="ferry-caption">
                      已选 {checkedKeys.length} / {allMenuIds.length} 个权限点
                    </span>
                    <div className="ferry-toolbar__spacer" />
                    <Button size="small" onClick={() => setCheckedKeys(allMenuIds)}>
                      全选
                    </Button>
                    <Button size="small" onClick={() => setCheckedKeys([])}>
                      清空
                    </Button>
                  </div>
                  <div className="ferry-caption" style={{ marginBottom: 10 }}>
                    勾选菜单即授予页面访问权，勾选按钮即授予操作权；父级菜单会自动保留，用于渲染导航。
                  </div>
                  <div
                    style={{
                      border: '1px solid var(--border-1)',
                      borderRadius: 8,
                      padding: 12,
                      minHeight: 420,
                      maxHeight: 'calc(100vh - 360px)',
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
                </>
              ),
            },
            {
              key: 'data',
              label: '数据权限',
              children: (
                <>
                  <div className="ferry-caption" style={{ marginBottom: 12 }}>
                    数据范围决定该角色能看到哪些部门的数据；能进哪个页面由「功能权限」决定，两者互不影响。
                  </div>
                  <Radio.Group
                    value={dataScope}
                    onChange={(event) => setDataScope(event.target.value)}
                    style={{ display: 'flex', flexDirection: 'column', gap: 10 }}
                  >
                    {DATA_SCOPE_OPTIONS.map((item) => (
                      <Radio key={item.value} value={item.value}>
                        {item.label}
                      </Radio>
                    ))}
                  </Radio.Group>
                  {dataScope === 2 && (
                    <>
                      <div className="ferry-caption" style={{ margin: '16px 0 8px' }}>
                        勾选的部门会自动包含其全部下级；父级与子级独立勾选，不会互相带选。
                      </div>
                      <div
                        style={{
                          border: '1px solid var(--border-1)',
                          borderRadius: 8,
                          padding: 12,
                          minHeight: 240,
                          maxHeight: 'calc(100vh - 460px)',
                          overflow: 'auto',
                        }}
                      >
                        <Tree
                          checkable
                          checkStrictly
                          selectable={false}
                          defaultExpandAll
                          treeData={deptTreeData}
                          checkedKeys={{ checked: deptKeys, halfChecked: [] }}
                          onCheck={(checked) => {
                            const keys = Array.isArray(checked) ? checked : checked.checked
                            setDeptKeys(keys.map(String))
                          }}
                        />
                      </div>
                    </>
                  )}
                  <Alert
                    style={{ marginTop: 16 }}
                    type="info"
                    showIcon
                    message="保存后不会踢人，已登录用户仍是旧范围"
                    description="数据范围在登录时算好并缓存在会话里，保存只改库、不动在线会话。被授权人重新登录后才会按新范围过滤；期间他自己退出即可刷新。"
                  />
                </>
              ),
            },
          ]}
        />
      </Drawer>

    </>
  )
}
