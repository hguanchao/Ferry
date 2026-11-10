import { useMemo, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  App,
  Button,
  Form,
  Input,
  InputNumber,
  Modal,
  Select,
  Switch,
  Table,
  Tooltip,
  TreeSelect,
  type TableColumnsType,
} from 'antd'
import { DeleteOutlined, EditOutlined, PlusOutlined, ReloadOutlined, SearchOutlined } from '@ant-design/icons'
import PageHeader from '@/components/PageHeader'
import { useAutoExpandRows } from '@/hooks/useAutoExpandRows'
import { RoleTag } from '@/components/StatusTag'
import { countTree } from '@/lib/tree'
import { deptApi } from '@/api/system'
import type { DeptNode } from '@/api/types'
import { useAuthStore } from '@/stores/auth'
import { showError } from '@/lib/notify'

function toTreeSelect(nodes: DeptNode[]): Array<{ value: string; title: string; children?: unknown[] }> {
  return nodes.map((node) => ({
    value: node.id,
    title: node.name,
    children: node.children?.length ? toTreeSelect(node.children) : undefined,
  }))
}

export default function Dept() {
  const { message, modal } = App.useApp()
  const queryClient = useQueryClient()
  const can = useAuthStore((state) => state.can)
  const [form] = Form.useForm()
  const [open, setOpen] = useState(false)
  const [editing, setEditing] = useState<DeptNode | null>(null)
  const [parentId, setParentId] = useState('0')
  const [keyword, setKeyword] = useState('')
  const [applied, setApplied] = useState('')
  const [status, setStatus] = useState<number | undefined>()
  const [appliedStatus, setAppliedStatus] = useState<number | undefined>()

  const query = useQuery({
    queryKey: ['depts', applied, appliedStatus],
    queryFn: () => deptApi.treeList({ name: applied || undefined, status: appliedStatus }),
  })

  const allQuery = useQuery({ queryKey: ['dept-tree'], queryFn: () => deptApi.treeList({}) })
  const expandProps = useAutoExpandRows(query.data)

  const treeSelectData = useMemo(
    () => [{ value: '0', title: '根部门', children: toTreeSelect(allQuery.data ?? []) }],
    [allQuery.data],
  )

  const invalidate = () => {
    void queryClient.invalidateQueries({ queryKey: ['depts'] })
    void queryClient.invalidateQueries({ queryKey: ['dept-tree'] })
    void queryClient.invalidateQueries({ queryKey: ['users'] })
  }

  const saveMutation = useMutation({
    mutationFn: (values: Record<string, unknown>) => deptApi.save(values),
    onSuccess: (text) => {
      message.success(text || '保存成功')
      setOpen(false)
      invalidate()
    },
    onError: showError,
  })

  const deleteMutation = useMutation({
    mutationFn: (ids: string[]) => deptApi.deleted(ids),
    onSuccess: (text) => {
      message.success(text || '删除成功')
      invalidate()
    },
    onError: showError,
  })

  const openEditor = (record: DeptNode | null, parent?: DeptNode) => {
    setEditing(record)
    form.resetFields()
    if (record) {
      form.setFieldsValue({ ...record })
      setParentId(record.parentId)
    } else {
      form.setFieldsValue({ status: 1, sort: 1, parentId: parent?.id ?? '0' })
      setParentId(parent?.id ?? '0')
    }
    setOpen(true)
  }

  const columns: TableColumnsType<DeptNode> = [
    {
      title: '部门名称',
      dataIndex: 'name',
      width: 250,

      render: (value: string, record) => (
        <span style={{ display: 'inline-flex', alignItems: 'center', gap: 8 }}>
          <span style={{ fontWeight: 500, color: 'var(--text-1)' }}>{value}</span>
          {record.parentId === '0' && <RoleTag>一级</RoleTag>}
        </span>
      ),
    },
    {
      title: '部门编码',
      dataIndex: 'code',
      width: 250,

      render: (value: string) => (
        <span className="ferry-mono" style={{ fontSize: 12 }}>
          {value}
        </span>
      ),
    },
    {
      title: '排序',
      dataIndex: 'sort',
      width: 250,

      align: 'right',
      render: (value: number) => <span className="ferry-mono">{value}</span>,
    },
    {
      title: '状态',
      dataIndex: 'status',
      width: 250,

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
      width: 190,

      fixed: 'right',
      render: (_, record) => (
        <div style={{ display: 'flex', alignItems: 'center', gap: 2 }}>
          {can('system:dept:save') && (
            <Tooltip title="新增下级部门">
              <Button type="link" size="small" icon={<PlusOutlined />} onClick={() => openEditor(null, record)} />
            </Tooltip>
          )}
          {can('system:dept:save') && (
            <Button type="link" size="small" icon={<EditOutlined />} onClick={() => openEditor(record)}>
              编辑
            </Button>
          )}
          {can('system:dept:deleted') && (
            <Button
              type="link"
              size="small"
              danger
              icon={<DeleteOutlined />}
              onClick={() =>
                modal.confirm({
                  title: `删除部门「${record.name}」？`,
                  content: '存在下级部门或仍有用户归属时不可删除，需先调整归属。',
                  okText: '删除',
                  okButtonProps: { danger: true },
                  cancelText: '取消',
                  onOk: () => deleteMutation.mutateAsync([record.id]),
                })
              }
            />
          )}
        </div>
      ),
    },
  ]

  return (
    <>
      <PageHeader
        title="部门管理"
        description="部门是数据权限的默认边界：用户只能看到本部门及下级的数据，跨部门需要单独授权。"
        tag={<RoleTag>{countTree(query.data ?? [])} 个部门</RoleTag>}
        extra={
          can('system:dept:save') && (
            <Button type="primary" icon={<PlusOutlined />} onClick={() => openEditor(null)}>
              新增部门
            </Button>
          )
        }
      />

      <section className="ferry-panel">
        <div className="ferry-toolbar">
          <Input
            allowClear
            prefix={<SearchOutlined style={{ color: 'var(--text-4)' }} />}
            placeholder="部门名称或编码"
            style={{ width: 200 }}
            value={keyword}
            onChange={(event) => setKeyword(event.target.value)}
            onPressEnter={() => setApplied(keyword)}
          />
          <Select
            allowClear
            placeholder="状态"
            style={{ width: 120 }}
            value={status}
            onChange={setStatus}
            options={[
              { value: 1, label: '正常' },
              { value: 0, label: '停用' },
            ]}
          />
          <Button
            type="primary"
            icon={<SearchOutlined />}
            onClick={() => {
              setApplied(keyword)
              setAppliedStatus(status)
            }}
          >
            查询
          </Button>
          <Button
            icon={<ReloadOutlined />}
            onClick={() => {
              setKeyword('')
              setStatus(undefined)
              setApplied('')
              setAppliedStatus(undefined)
            }}
          >
            重置
          </Button>
        </div>

        <div className="ferry-table-wrap">
          <Table<DeptNode>
            rowKey="id"
            columns={columns}
            dataSource={query.data ?? []}
            loading={query.isLoading || query.isFetching}
            pagination={false}
            expandable={expandProps}
            scroll={{ x: 1190 }}
          />
        </div>
      </section>

      <Modal
        open={open}
        title={editing ? `编辑部门 · ${editing.name}` : '新增部门'}
        okText="保存"
        cancelText="取消"
        width={560}
        confirmLoading={saveMutation.isPending}
        onCancel={() => setOpen(false)}
        onOk={async () => {
          const values = await form.validateFields()
          saveMutation.mutate({ ...values, id: editing?.id, parentId })
        }}
        destroyOnHidden
      >
        <Form form={form} layout="vertical" requiredMark="optional">
          <Form.Item name="parentId" label="上级部门" rules={[{ required: true, message: '请选择上级部门' }]}>
            <TreeSelect
              treeData={treeSelectData as never}
              treeDefaultExpandAll
              value={parentId}
              onChange={(value) => setParentId(String(value))}
              placeholder="请选择上级部门"
            />
          </Form.Item>
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(2, minmax(0, 1fr))', gap: '0 16px' }}>
            <Form.Item name="name" label="部门名称" rules={[{ required: true, message: '请输入部门名称' }]}>
              <Input placeholder="例如 后端开发组" maxLength={20} />
            </Form.Item>
            <Form.Item
              name="code"
              label="部门编码"
              rules={[
                { required: true, message: '请输入部门编码' },
                { pattern: /^[A-Z][A-Z0-9]{1,11}$/, message: '大写字母开头，2-12 位' },
              ]}
            >
              <Input placeholder="例如 BDEV" maxLength={12} />
            </Form.Item>
            <Form.Item name="sort" label="显示顺序">
              <InputNumber min={1} max={999} style={{ width: '100%' }} />
            </Form.Item>
            <Form.Item name="status" label="状态" valuePropName="checked" getValueFromEvent={(checked) => (checked ? 1 : 0)}>
              <Switch checkedChildren="正常" unCheckedChildren="停用" />
            </Form.Item>
          </div>
        </Form>
      </Modal>
    </>
  )
}
