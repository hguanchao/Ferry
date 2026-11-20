import { useMemo, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  App,
  Button,
  Form,
  Input,
  InputNumber,
  Modal,
  Radio,
  Select,
  Switch,
  Tag,
  Tooltip,
  TreeSelect,
  type TableColumnsType,
} from 'antd'
import FerryTable from '@/components/FerryTable'
import { Pencil, Plus, RotateCw, Search, Trash2 } from 'lucide-react'
import PageHeader from '@/components/PageHeader'
import { useAutoExpandRows } from '@/hooks/useAutoExpandRows'
import { MenuIcon, ICON_OPTIONS } from '@/components/MenuIcon'
import { RoleTag } from '@/components/StatusTag'
import { menuApi } from '@/api/system'
import type { MenuNode } from '@/api/types'
import { useAuthStore } from '@/stores/auth'
import { showError } from '@/lib/notify'
const TYPE_LABEL: Record<number, { text: string; color: string }> = {
  0: { text: '目录', color: 'blue' },
  1: { text: '菜单', color: 'green' },
  2: { text: '按钮', color: 'default' },
}

function toTreeSelect(nodes: MenuNode[]): Array<{ value: string; title: string; children?: unknown[] }> {
  return nodes
    .filter((node) => node.type !== 2)
    .map((node) => ({
      value: node.id,
      title: node.name,
      children: node.children?.length ? toTreeSelect(node.children) : undefined,
    }))
}

export default function Menu() {
  const { message, modal } = App.useApp()
  const queryClient = useQueryClient()
  const can = useAuthStore((state) => state.can)
  const [form] = Form.useForm()
  const [open, setOpen] = useState(false)
  const [editing, setEditing] = useState<MenuNode | null>(null)
  const [parentId, setParentId] = useState<string>('0')
  const [keyword, setKeyword] = useState('')
  const [applied, setApplied] = useState('')
  const [type, setType] = useState<number | undefined>()
  const [appliedType, setAppliedType] = useState<number | undefined>()

  const query = useQuery({
    queryKey: ['menus', applied, appliedType],
    queryFn: () => menuApi.treeList({ name: applied || undefined, type: appliedType }),
  })

  const expandProps = useAutoExpandRows(query.data)

  const treeSelectData = useMemo(
    () => [{ value: '0', title: '根目录', children: toTreeSelect(query.data ?? []) }],
    [query.data],
  )

  const invalidate = () => {
    void queryClient.invalidateQueries({ queryKey: ['menus'] })
    void queryClient.invalidateQueries({ queryKey: ['menu-tree'] })
  }

  const saveMutation = useMutation({
    mutationFn: (values: Record<string, unknown>) => menuApi.save(values),
    onSuccess: (text) => {
      message.success(text || '保存成功')
      setOpen(false)
      invalidate()
    },
    onError: showError,
  })

  const deleteMutation = useMutation({
    mutationFn: (ids: string[]) => menuApi.deleted(ids),
    onSuccess: (text) => {
      message.success(text || '删除成功')
      invalidate()
    },
    onError: showError,
  })

  const openEditor = (record: MenuNode | null, parent?: MenuNode) => {
    setEditing(record)
    form.resetFields()
    if (record) {
      form.setFieldsValue({ ...record })
      setParentId(record.parentId)
    } else {
      form.setFieldsValue({
        type: parent ? 1 : 0,
        status: 1,
        isShow: 1,
        isCache: 1,
        isFrame: 0,
        sort: 1,
        parentId: parent?.id ?? '0',
      })
      setParentId(parent?.id ?? '0')
    }
    setOpen(true)
  }

  const columns: TableColumnsType<MenuNode> = [
    {
      title: '菜单名称',
      dataIndex: 'name',

      render: (_, record) => (
        <span style={{ display: 'inline-flex', alignItems: 'center', gap: 8 }}>
          {record.type !== 2 && <MenuIcon name={record.icon} />}
          <span style={{ fontWeight: record.type === 2 ? 400 : 500, color: 'var(--text-1)' }}>{record.name}</span>
          {record.isShow === 0 && <Tag style={{ fontSize: 10 }}>隐藏</Tag>}
        </span>
      ),
    },
    {
      title: '类型',
      dataIndex: 'type',

      render: (value: number) => (
        <Tag color={TYPE_LABEL[value]?.color} style={{ fontSize: 11 }}>
          {TYPE_LABEL[value]?.text ?? value}
        </Tag>
      ),
    },
    {
      title: '路由地址',
      dataIndex: 'path',

      render: (value: string | null) =>
        value ? (
          <span className="ferry-mono" style={{ fontSize: 12 }}>
            {value}
          </span>
        ) : (
          <span className="ferry-muted">—</span>
        ),
    },
    {
      title: '权限标识',
      dataIndex: 'perms',

      render: (value: string | null) =>
        value ? (
          <span className="ferry-mono" style={{ fontSize: 11.5, color: 'var(--text-2)' }}>
            {value}
          </span>
        ) : (
          <span className="ferry-muted">—</span>
        ),
    },
    {
      title: '排序',
      dataIndex: 'sort',

      align: 'right',
      render: (value: number) => <span className="ferry-mono">{value}</span>,
    },
    {
      title: '状态',
      dataIndex: 'status',

      render: (value: number) => (
        <span className="ferry-status" data-tone={value === 1 ? 'success' : 'default'}>
          <span className="ferry-status__dot" />
          {value === 1 ? '正常' : '停用'}
        </span>
      ),
    },
    {
      title: '操作',
      key: 'action',

      render: (_, record) => (
        <div style={{ display: 'flex', alignItems: 'center', gap: 2 }}>
          {can('system:menu:save') && record.type !== 2 && (
            <Tooltip title="在该菜单下新增子项">
              <Button type="link" size="small" icon={<Plus size={14} />} onClick={() => openEditor(null, record)} />
            </Tooltip>
          )}
          {can('system:menu:save') && (
            <Tooltip title="编辑">
              <Button type="link" size="small" icon={<Pencil size={14} />} onClick={() => openEditor(record)} />
            </Tooltip>
          )}
          {can('system:menu:deleted') && (
            <Tooltip title="存在下级时不可删除">
              <Button
                type="link"
                size="small"
                danger
                icon={<Trash2 size={14} />}
                onClick={() =>
                  modal.confirm({
                    title: `删除「${record.name}」？`,
                    content: '已分配给角色的权限会被同步移除。存在下级菜单时需先删除下级。',
                    okText: '删除',
                    okButtonProps: { danger: true },
                    cancelText: '取消',
                    onOk: () => deleteMutation.mutateAsync([record.id]),
                  })
                }
              />
            </Tooltip>
          )}
        </div>
      ),
    },
  ]

  return (
    <>
      <PageHeader
        title="菜单管理"
        description="菜单与权限点同源：页面路由决定「进得去」，权限标识决定「点得动」。改动会即时影响角色授权与导航渲染。"
        tag={<RoleTag>{query.data?.length ?? 0} 个根节点</RoleTag>}
        extra={
          can('system:menu:save') && (
            <Button type="primary" icon={<Plus size={14} />} onClick={() => openEditor(null)}>
              新增菜单
            </Button>
          )
        }
      />

      <section className="ferry-panel">
        <div className="ferry-toolbar">
          <Input
            allowClear
            prefix={<Search size={15} style={{ color: 'var(--text-4)' }} />}
            placeholder="菜单名称"
            style={{ width: 180 }}
            value={keyword}
            onChange={(event) => setKeyword(event.target.value)}
            onPressEnter={() => setApplied(keyword)}
          />
          <Select
            allowClear
            placeholder="菜单类型"
            style={{ width: 130 }}
            value={type}
            onChange={setType}
            options={[
              { value: 0, label: '目录' },
              { value: 1, label: '菜单' },
              { value: 2, label: '按钮' },
            ]}
          />
          <Button
            type="primary"
            icon={<Search size={15} />}
            onClick={() => {
              setApplied(keyword)
              setAppliedType(type)
            }}
          >
            查询
          </Button>
          <Button
            icon={<RotateCw size={15} />}
            onClick={() => {
              setKeyword('')
              setType(undefined)
              setApplied('')
              setAppliedType(undefined)
            }}
          >
            重置
          </Button>
        </div>

        <div className="ferry-table-wrap">
          <FerryTable<MenuNode>
            rowKey="id"
            columns={columns}
            dataSource={query.data ?? []}
            loading={query.isLoading || query.isFetching}
            pagination={false}
            expandable={expandProps}
            scroll={{ x: 'max-content' }}
          />
        </div>
      </section>

      <Modal
        open={open}
        title={editing ? `编辑菜单 · ${editing.name}` : '新增菜单'}
        okText="保存"
        cancelText="取消"
        width={640}
        confirmLoading={saveMutation.isPending}
        onCancel={() => setOpen(false)}
        onOk={async () => {
          const values = await form.validateFields()
          saveMutation.mutate({ ...values, id: editing?.id, parentId })
        }}
        destroyOnHidden
      >
        <Form form={form} layout="vertical" requiredMark="optional">
          <Form.Item name="parentId" label="上级菜单" rules={[{ required: true, message: '请选择上级菜单' }]}>
            <TreeSelect
              treeData={treeSelectData as never}
              treeDefaultExpandAll
              value={parentId}
              onChange={(value) => setParentId(String(value))}
              placeholder="请选择上级菜单"
            />
          </Form.Item>
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(2, minmax(0, 1fr))', gap: '0 16px' }}>
            <Form.Item name="type" label="类型" rules={[{ required: true }]}>
              <Radio.Group
                optionType="button"
                buttonStyle="solid"
                options={[
                  { value: 0, label: '目录' },
                  { value: 1, label: '菜单' },
                  { value: 2, label: '按钮' },
                ]}
              />
            </Form.Item>
            <Form.Item name="name" label="名称" rules={[{ required: true, message: '请输入菜单名称' }]}>
              <Input placeholder="显示在导航或按钮上的名称" maxLength={20} />
            </Form.Item>
            <Form.Item name="path" label="路由地址" extra="目录/菜单填路由，按钮可留空">
              <Input placeholder="/system/user" maxLength={100} />
            </Form.Item>
            <Form.Item name="component" label="组件路径" extra="前端页面组件标识">
              <Input placeholder="/system/User" maxLength={100} />
            </Form.Item>
            <Form.Item name="perms" label="权限标识" extra="接口鉴权用，段数与后端注解一致">
              <Input placeholder="system:user:save" maxLength={100} />
            </Form.Item>
            <Form.Item name="icon" label="图标">
              <Select
                allowClear
                showSearch
                placeholder="选择图标"
                options={ICON_OPTIONS}
                optionRender={(option) => (
                  <span style={{ display: 'inline-flex', alignItems: 'center', gap: 8 }}>
                    <MenuIcon name={String(option.value)} />
                    <span className="ferry-mono" style={{ fontSize: 12 }}>
                      {option.label}
                    </span>
                  </span>
                )}
              />
            </Form.Item>
            <Form.Item name="sort" label="显示顺序">
              <InputNumber min={1} max={999} style={{ width: '100%' }} />
            </Form.Item>
            <Form.Item name="status" label="状态" valuePropName="checked" getValueFromEvent={(checked) => (checked ? 1 : 0)}>
              <Switch checkedChildren="正常" unCheckedChildren="停用" />
            </Form.Item>
            <Form.Item name="isShow" label="导航中显示" valuePropName="checked" getValueFromEvent={(checked) => (checked ? 1 : 0)}>
              <Switch checkedChildren="显示" unCheckedChildren="隐藏" />
            </Form.Item>
            <Form.Item name="isCache" label="页面缓存" valuePropName="checked" getValueFromEvent={(checked) => (checked ? 1 : 0)}>
              <Switch checkedChildren="缓存" unCheckedChildren="不缓存" />
            </Form.Item>
          </div>
        </Form>
      </Modal>
    </>
  )
}
