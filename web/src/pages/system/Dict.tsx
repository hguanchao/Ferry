import { useEffect, useMemo, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  App,
  Button,
  Form,
  Input,
  InputNumber,
  Modal,
  Table,
  Tag,
  Tooltip,
  Tree,
  type TableColumnsType,
} from 'antd'
import { DeleteOutlined, EditOutlined, PlusOutlined, ReloadOutlined } from '@ant-design/icons'
import PageHeader from '@/components/PageHeader'
import { RoleTag } from '@/components/StatusTag'
import { dictApi } from '@/api/system'
import type { DictNode } from '@/api/types'
import { useAuthStore } from '@/stores/auth'
import { showError } from '@/lib/notify'

export default function Dict() {
  const { message, modal } = App.useApp()
  const queryClient = useQueryClient()
  const can = useAuthStore((state) => state.can)
  const [form] = Form.useForm()
  const [open, setOpen] = useState(false)
  const [editing, setEditing] = useState<DictNode | null>(null)
  const [parentId, setParentId] = useState('0')
  const [activeGroup, setActiveGroup] = useState<DictNode | null>(null)
  const [keyword, setKeyword] = useState('')

  const treeQuery = useQuery({ queryKey: ['dict-tree'], queryFn: () => dictApi.treeList({}) })

  const groups = useMemo(() => treeQuery.data ?? [], [treeQuery.data])

  useEffect(() => {
    if (!activeGroup && groups.length) {
      setActiveGroup(groups[0])
    }
  }, [groups, activeGroup])

  const itemsQuery = useQuery({
    queryKey: ['dict-items', activeGroup?.id, keyword],
    queryFn: () => dictApi.pageList({ parentId: activeGroup?.id, label: keyword || undefined, current: 1, size: 200 }),
    enabled: !!activeGroup,
  })

  const invalidate = () => {
    void queryClient.invalidateQueries({ queryKey: ['dict-tree'] })
    void queryClient.invalidateQueries({ queryKey: ['dict-items'] })
  }

  const saveMutation = useMutation({
    mutationFn: (values: Record<string, unknown>) => dictApi.save(values),
    onSuccess: (text) => {
      message.success(text || '保存成功')
      setOpen(false)
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

  const openGroupEditor = () => {
    setEditing(null)
    form.resetFields()
    form.setFieldsValue({ parentId: '0', sort: groups.length + 1 })
    setParentId('0')
    setOpen(true)
  }

  const openItemEditor = (record: DictNode | null) => {
    setEditing(record)
    form.resetFields()
    if (record) {
      form.setFieldsValue({ ...record })
      setParentId(record.parentId)
    } else {
      form.setFieldsValue({
        parentId: activeGroup?.id ?? '0',
        sort: (itemsQuery.data?.records.length ?? 0) + 1,
        code: activeGroup?.code,
      })
      setParentId(activeGroup?.id ?? '0')
    }
    setOpen(true)
  }

  const columns: TableColumnsType<DictNode> = [
    {
      title: '字典标签',
      dataIndex: 'label',
      width: 261,

      render: (value: string) => <span style={{ fontWeight: 500, color: 'var(--text-1)' }}>{value}</span>,
    },
    {
      title: '字典值',
      dataIndex: 'value',
      width: 261,

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
      width: 261,

      align: 'right',
      render: (value: number) => <span className="ferry-mono">{value}</span>,
    },
    {
      title: '操作',
      key: 'action',
      width: 150,

      render: (_, record) => (
        <div style={{ display: 'flex', alignItems: 'center', gap: 2 }}>
          {can('system:dict:save') && (
            <Button type="link" size="small" icon={<EditOutlined />} onClick={() => openItemEditor(record)}>
              编辑
            </Button>
          )}
          {can('system:dict:deleted') && (
            <Button
              type="link"
              size="small"
              danger
              icon={<DeleteOutlined />}
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
        tag={<RoleTag>{groups.length} 个字典分组</RoleTag>}
        extra={
          can('system:dict:save') && (
            <Button type="primary" icon={<PlusOutlined />} onClick={openGroupEditor}>
              新增字典
            </Button>
          )
        }
      />

      <div className="ferry-split">
        <aside className="ferry-split__aside ferry-split__aside--dict">
          <div className="ferry-split__aside-head">字典分组</div>
          <div className="ferry-tree-scroll">
            <Tree
              blockNode
              defaultExpandAll
              selectedKeys={activeGroup ? [activeGroup.id] : []}
              treeData={groups.map((group) => ({
                key: group.id,
                title: (
                  <span className="ferry-dict-node">
                    <span className="ferry-dict-node__label">{group.label}</span>
                    <span className="ferry-dict-node__code ferry-mono">{group.code}</span>
                  </span>
                ),
              }))}
              onSelect={(keys) => {
                const next = groups.find((group) => group.id === keys[0])
                if (next) setActiveGroup(next)
              }}
            />
          </div>
        </aside>

        <section className="ferry-panel">
          <div className="ferry-toolbar">
            <div style={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
              <span style={{ fontSize: 13, fontWeight: 600, color: 'var(--text-1)' }}>
                {activeGroup?.label ?? '请选择字典分组'}
              </span>
              <span className="ferry-caption">
                字典码 <span className="ferry-mono">{activeGroup?.code ?? '—'}</span>
              </span>
            </div>
            <div className="ferry-toolbar__spacer" />
            <Input.Search
              allowClear
              placeholder="搜索标签"
              style={{ width: 180 }}
              value={keyword}
              onChange={(event) => setKeyword(event.target.value)}
            />
            <Tooltip title="刷新">
              <Button icon={<ReloadOutlined />} onClick={invalidate} />
            </Tooltip>
            {can('system:dict:save') && activeGroup && (
              <Button type="primary" icon={<PlusOutlined />} onClick={() => openItemEditor(null)}>
                新增字典项
              </Button>
            )}
          </div>

          <div className="ferry-table-wrap">
            <Table<DictNode>
              rowKey="id"
              columns={columns}
              dataSource={itemsQuery.data?.records ?? []}
              loading={itemsQuery.isLoading || itemsQuery.isFetching}
              pagination={false}
              scroll={{ x: 933 }}
            />
          </div>
          <div className="ferry-table-footer">
            <span>字典项变更后即时生效，已使用该字典的业务表单会在下次加载时刷新选项。</span>
            <Tag color="blue">共 {itemsQuery.data?.total ?? 0} 项</Tag>
          </div>
        </section>
      </div>

      <Modal
        open={open}
        title={editing ? `编辑 · ${editing.label}` : parentId === '0' ? '新增字典分组' : '新增字典项'}
        okText="保存"
        cancelText="取消"
        width={520}
        confirmLoading={saveMutation.isPending}
        onCancel={() => setOpen(false)}
        onOk={async () => {
          const values = await form.validateFields()
          // 字典项没有独立字典码：表单只读展示分组码，提交仍以分组码为准；分组沿用用户输入
          const code = parentId === '0' ? values.code : activeGroup?.code
          saveMutation.mutate({ ...values, code, id: editing?.id, parentId })
        }}
        destroyOnHidden
      >
        <Form form={form} layout="vertical" requiredMark="optional">
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(2, minmax(0, 1fr))', gap: '0 16px' }}>
            <Form.Item
              name="code"
              label="字典码"
              extra={parentId === '0' ? undefined : '继承所属字典分组，不可修改'}
              // 分组码格式与库里 000000_XXX 的历史数据不一致，字典项只读展示时不能再拿它校验
              rules={
                parentId === '0'
                  ? [
                      { required: true, message: '请输入字典码' },
                      { pattern: /^[A-Z][A-Z0-9_]{1,29}$/, message: '大写字母开头，可含数字与下划线' },
                    ]
                  : []
              }
            >
              <Input placeholder="例如 GENDER" maxLength={30} disabled={parentId !== '0'} />
            </Form.Item>
            <Form.Item name="label" label="字典标签" rules={[{ required: true, message: '请输入标签' }]}>
              <Input placeholder="例如 性别" maxLength={20} />
            </Form.Item>
            {parentId !== '0' && (
              <Form.Item name="value" label="字典值" extra="存库值，留空则以标签展示">
                <Input placeholder="例如 1" maxLength={20} />
              </Form.Item>
            )}
            <Form.Item name="sort" label="显示顺序">
              <InputNumber min={1} max={999} style={{ width: '100%' }} />
            </Form.Item>
          </div>
        </Form>
      </Modal>
    </>
  )
}
