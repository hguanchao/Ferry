import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { App, Button, Form, Input, InputNumber, Modal, Table, Tooltip, type TableColumnsType } from 'antd'
import { Pencil, Plus, RotateCw, Search, Trash2 } from 'lucide-react'
import PageHeader from '@/components/PageHeader'
import { RoleTag } from '@/components/StatusTag'
import { dictApi } from '@/api/system'
import type { DictNode } from '@/api/types'
import { useAuthStore } from '@/stores/auth'
import { confirmTreeDelete } from '@/lib/confirm'
import { COL } from '@/lib/table'
import { showError } from '@/lib/notify'

export default function Dict() {
  const { message, modal } = App.useApp()
  const queryClient = useQueryClient()
  const can = useAuthStore((state) => state.can)
  const [groupForm] = Form.useForm()
  const [itemForm] = Form.useForm()

  /* ------------------------------ 字典分组 ------------------------------ */
  const [groupOpen, setGroupOpen] = useState(false)
  const [editingGroup, setEditingGroup] = useState<DictNode | null>(null)
  const [groupDraft, setGroupDraft] = useState<{ code?: string; label?: string }>({})
  const [groupFilters, setGroupFilters] = useState<{ code?: string; label?: string }>({})
  const [groupPage, setGroupPage] = useState({ current: 1, size: 30 })

  /* ------------------------------ 字典项 ------------------------------ */
  /** 当前弹窗对应的字典分组，null 表示弹窗关闭 */
  const [itemsOf, setItemsOf] = useState<DictNode | null>(null)
  const [itemOpen, setItemOpen] = useState(false)
  const [editingItem, setEditingItem] = useState<DictNode | null>(null)
  const [itemKeyword, setItemKeyword] = useState('')
  const [itemPage, setItemPage] = useState({ current: 1, size: 20 })

  const groupQuery = useQuery({
    queryKey: ['dict-groups', groupFilters, groupPage],
    queryFn: () => dictApi.pageList({ parentId: '0', ...groupFilters, ...groupPage }),
  })

  const itemQuery = useQuery({
    queryKey: ['dict-items', itemsOf?.id, itemKeyword, itemPage],
    queryFn: () => dictApi.pageList({ parentId: itemsOf?.id, label: itemKeyword || undefined, ...itemPage }),
    enabled: !!itemsOf,
  })

  const invalidate = () => {
    void queryClient.invalidateQueries({ queryKey: ['dict-groups'] })
    void queryClient.invalidateQueries({ queryKey: ['dict-items'] })
  }

  const saveGroupMutation = useMutation({
    mutationFn: (values: Record<string, unknown>) => dictApi.save(values),
    onSuccess: (text) => {
      message.success(text || '保存成功')
      setGroupOpen(false)
      invalidate()
    },
    onError: showError,
  })

  const saveItemMutation = useMutation({
    mutationFn: (values: Record<string, unknown>) => dictApi.save(values),
    onSuccess: (text) => {
      message.success(text || '保存成功')
      setItemOpen(false)
      invalidate()
    },
    onError: showError,
  })

  const deleteMutation = useMutation({
    mutationFn: (ids: string[]) => dictApi.deleted(ids),
    onSuccess: (text) => {
      message.success(text || '删除成功')
      invalidate()
    },
    onError: showError,
  })

  const openGroupEditor = (record: DictNode | null) => {
    setEditingGroup(record)
    groupForm.resetFields()
    groupForm.setFieldsValue(record ? { ...record } : { sort: (groupQuery.data?.records.length ?? 0) + 1 })
    setGroupOpen(true)
  }

  const openItems = (record: DictNode) => {
    setItemsOf(record)
    setItemKeyword('')
    setItemPage({ current: 1, size: 20 })
  }

  const openItemEditor = (record: DictNode | null) => {
    setEditingItem(record)
    itemForm.resetFields()
    itemForm.setFieldsValue(record ? { ...record } : { sort: (itemQuery.data?.total ?? 0) + 1 })
    setItemOpen(true)
  }

  const groupColumns: TableColumnsType<DictNode> = [
    {
      title: '字典名称',
      dataIndex: 'label',
      render: (value: string) => <span style={{ fontWeight: 500, color: 'var(--text-1)' }}>{value}</span>,
    },
    {
      title: '字典编码',
      dataIndex: 'code',
      width: 240,
      render: (value: string, record) => (
        <Button type="link" size="small" style={{ padding: 0, height: 'auto' }} onClick={() => openItems(record)}>
          <span className="ferry-mono" style={{ fontSize: 12 }}>
            {value}
          </span>
        </Button>
      ),
    },
    {
      title: '排序',
      dataIndex: 'sort',
      width: COL.sort,
      align: 'right',
      render: (value: number) => <span className="ferry-mono">{value}</span>,
    },
    {
      title: '创建时间',
      dataIndex: 'createTime',
      width: COL.time,
      render: (value: string | null) => (
        <span className="ferry-mono" style={{ fontSize: 12, color: 'var(--text-3)' }}>
          {value ?? '—'}
        </span>
      ),
    },
    {
      title: '操作',
      key: 'action',
      width: COL.action,
      render: (_, record) => (
        <div style={{ display: 'flex', alignItems: 'center', gap: 2 }}>
          {can('system:dict:save') && (
            <Tooltip title="编辑">
              <Button type="link" size="small" icon={<Pencil size={14} />} onClick={() => openGroupEditor(record)} />
            </Tooltip>
          )}
          {can('system:dict:deleted') && (
            <Tooltip title="删除">
              <Button
                type="link"
                size="small"
                danger
                icon={<Trash2 size={14} />}
                onClick={() =>
                  confirmTreeDelete({
                    modal,
                    label: '字典',
                    name: record.label,
                    childLabel: '字典项',
                    // 分组列表不返回子项数量，按「存在字典项」提示，具体以实际为准
                    childCount: undefined,
                    note: '被业务引用的取值删除后，新建数据将不再可选。',
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

  const itemColumns: TableColumnsType<DictNode> = [
    {
      title: '字典标签',
      dataIndex: 'label',
      render: (value: string) => <span style={{ fontWeight: 500, color: 'var(--text-1)' }}>{value}</span>,
    },
    {
      title: '字典值',
      dataIndex: 'value',
      width: 200,
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
      title: '排序',
      dataIndex: 'sort',
      width: COL.sort,
      align: 'right',
      render: (value: number) => <span className="ferry-mono">{value}</span>,
    },
    {
      title: '操作',
      key: 'action',
      width: COL.action,
      render: (_, record) => (
        <div style={{ display: 'flex', alignItems: 'center', gap: 2 }}>
          {can('system:dict:save') && (
            <Tooltip title="编辑">
              <Button type="link" size="small" icon={<Pencil size={14} />} onClick={() => openItemEditor(record)} />
            </Tooltip>
          )}
          {can('system:dict:deleted') && (
            <Tooltip title="删除">
              <Button
                type="link"
                size="small"
                danger
                icon={<Trash2 size={14} />}
                onClick={() =>
                  modal.confirm({
                    title: `删除字典项「${record.label}」？`,
                    content: '被业务引用的字典项删除后，历史数据仍保留原值，新建数据将不再可选。',
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
        title="字典管理"
        description="字典是枚举的唯一来源。性别、菜单类型、部署类型、门禁结论等取值统一在此维护，业务代码不再硬编码。"
        tag={<RoleTag>共 {groupQuery.data?.total ?? 0} 个字典</RoleTag>}
        extra={
          can('system:dict:save') && (
            <Button type="primary" icon={<Plus size={14} />} onClick={() => openGroupEditor(null)}>
              新增字典
            </Button>
          )
        }
      />

      <section className="ferry-panel">
        <div className="ferry-toolbar">
          <Input
            allowClear
            prefix={<Search size={15} style={{ color: 'var(--text-4)' }} />}
            placeholder="字典编码"
            style={{ width: 180 }}
            value={groupDraft.code}
            onChange={(event) => setGroupDraft((prev) => ({ ...prev, code: event.target.value }))}
            onPressEnter={() => setGroupFilters(groupDraft)}
          />
          <Input
            allowClear
            placeholder="字典名称"
            style={{ width: 180 }}
            value={groupDraft.label}
            onChange={(event) => setGroupDraft((prev) => ({ ...prev, label: event.target.value }))}
          />
          <Button
            type="primary"
            icon={<Search size={15} />}
            onClick={() => {
              setGroupFilters(groupDraft)
              setGroupPage((prev) => ({ ...prev, current: 1 }))
            }}
          >
            查询
          </Button>
          <Button
            icon={<RotateCw size={15} />}
            onClick={() => {
              setGroupDraft({})
              setGroupFilters({})
              setGroupPage((prev) => ({ ...prev, current: 1 }))
            }}
          >
            重置
          </Button>
        </div>

        <div className="ferry-table-wrap">
          <Table<DictNode>
            rowKey="id"
            columns={groupColumns}
            dataSource={groupQuery.data?.records ?? []}
            loading={groupQuery.isLoading || groupQuery.isFetching}
            scroll={{ x: 'max-content' }}
            pagination={{
              current: groupPage.current,
              pageSize: groupPage.size,
              total: groupQuery.data?.total ?? 0,
              showSizeChanger: true,
              pageSizeOptions: ['30', '100', '200'],
              showTotal: (total) => `共 ${total} 条`,
              onChange: (current, size) => setGroupPage({ current, size }),
            }}
          />
        </div>
      </section>

      <Modal
        open={groupOpen}
        title={editingGroup ? `编辑字典 · ${editingGroup.label}` : '新增字典'}
        okText="保存"
        cancelText="取消"
        width={520}
        confirmLoading={saveGroupMutation.isPending}
        onCancel={() => setGroupOpen(false)}
        onOk={async () => {
          const values = await groupForm.validateFields()
          saveGroupMutation.mutate({ ...values, id: editingGroup?.id, parentId: '0' })
        }}
        destroyOnHidden
      >
        <Form form={groupForm} layout="vertical" requiredMark="optional">
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(2, minmax(0, 1fr))', gap: '0 16px' }}>
            <Form.Item
              name="code"
              label="字典编码"
              extra="分组之间唯一，作为取值归属的标识"
              rules={[
                { required: true, message: '请输入字典编码' },
                { pattern: /^[A-Z][A-Z0-9_]{1,29}$/, message: '大写字母开头，可含数字与下划线' },
              ]}
            >
              <Input placeholder="例如 GENDER" maxLength={30} />
            </Form.Item>
            <Form.Item name="label" label="字典名称" rules={[{ required: true, message: '请输入字典名称' }]}>
              <Input placeholder="例如 性别" maxLength={20} />
            </Form.Item>
            <Form.Item name="sort" label="显示顺序">
              <InputNumber min={1} max={999} style={{ width: '100%' }} />
            </Form.Item>
          </div>
        </Form>
      </Modal>

      <Modal
        open={!!itemsOf}
        title={`字典项 · ${itemsOf?.label ?? ''}`}
        width={720}
        footer={null}
        onCancel={() => setItemsOf(null)}
        destroyOnHidden
      >
        <div className="ferry-toolbar" style={{ padding: '0 0 12px', borderBottom: 'none' }}>
          <Input.Search
            allowClear
            placeholder="搜索标签"
            style={{ width: 200 }}
            value={itemKeyword}
            onChange={(event) => {
              setItemKeyword(event.target.value)
              setItemPage((prev) => ({ ...prev, current: 1 }))
            }}
          />
          <span className="ferry-caption ferry-mono" style={{ fontSize: 12 }}>
            {itemsOf?.code}
          </span>
          <div className="ferry-toolbar__spacer" />
          {can('system:dict:save') && (
            <Button type="primary" icon={<Plus size={14} />} onClick={() => openItemEditor(null)}>
              新增字典项
            </Button>
          )}
        </div>

        <Table<DictNode>
          rowKey="id"
          size="small"
          columns={itemColumns}
          dataSource={itemQuery.data?.records ?? []}
          loading={itemQuery.isLoading || itemQuery.isFetching}
          scroll={{ x: 'max-content' }}
          pagination={{
            current: itemPage.current,
            pageSize: itemPage.size,
            total: itemQuery.data?.total ?? 0,
            size: 'small',
            showSizeChanger: true,
            pageSizeOptions: ['20', '50', '100'],
            showTotal: (total) => `共 ${total} 项`,
            onChange: (current, size) => setItemPage({ current, size }),
          }}
        />
      </Modal>

      <Modal
        open={itemOpen}
        title={editingItem ? `编辑字典项 · ${editingItem.label}` : '新增字典项'}
        okText="保存"
        cancelText="取消"
        width={480}
        confirmLoading={saveItemMutation.isPending}
        onCancel={() => setItemOpen(false)}
        onOk={async () => {
          const values = await itemForm.validateFields()
          // 字典项没有独立字典码，统一继承所属分组；带上分组码只为满足入参校验
          saveItemMutation.mutate({
            ...values,
            code: itemsOf?.code,
            id: editingItem?.id,
            parentId: itemsOf?.id,
          })
        }}
        destroyOnHidden
      >
        <Form form={itemForm} layout="vertical" requiredMark="optional">
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(2, minmax(0, 1fr))', gap: '0 16px' }}>
            <Form.Item name="label" label="字典标签" rules={[{ required: true, message: '请输入标签' }]}>
              <Input placeholder="例如 男" maxLength={20} />
            </Form.Item>
            <Form.Item name="value" label="字典值" extra="存库值，留空则以标签展示">
              <Input placeholder="例如 1" maxLength={20} />
            </Form.Item>
            <Form.Item name="sort" label="显示顺序">
              <InputNumber min={1} max={999} style={{ width: '100%' }} />
            </Form.Item>
          </div>
        </Form>
      </Modal>
    </>
  )
}
