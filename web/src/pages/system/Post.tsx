import { useState } from 'react'
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
  type TableColumnsType,
} from 'antd'
import { Pencil, Plus, RotateCw, Search, Trash2 } from 'lucide-react'
import PageHeader from '@/components/PageHeader'
import { RoleTag } from '@/components/StatusTag'
import { postApi } from '@/api/system'
import type { PostRow } from '@/api/types'
import { useAuthStore } from '@/stores/auth'
import { showError } from '@/lib/notify'
import { COL } from '@/lib/table'

export default function Post() {
  const { message, modal } = App.useApp()
  const queryClient = useQueryClient()
  const can = useAuthStore((state) => state.can)
  const [form] = Form.useForm()
  const [open, setOpen] = useState(false)
  const [editing, setEditing] = useState<PostRow | null>(null)
  const [draft, setDraft] = useState<{ code?: string; name?: string; status?: number }>({})
  const [filters, setFilters] = useState<{ code?: string; name?: string; status?: number }>({})
  const [page, setPage] = useState({ current: 1, size: 30 })

  const query = useQuery({
    queryKey: ['posts', filters, page],
    queryFn: () => postApi.pageList({ ...filters, ...page }),
  })

  const invalidate = () => {
    void queryClient.invalidateQueries({ queryKey: ['posts'] })
    void queryClient.invalidateQueries({ queryKey: ['workbench-overview'] })
  }

  const saveMutation = useMutation({
    mutationFn: (values: Record<string, unknown>) => postApi.save(values),
    onSuccess: (text) => {
      message.success(text || '保存成功')
      setOpen(false)
      invalidate()
    },
    onError: showError,
  })

  const deleteMutation = useMutation({
    mutationFn: (ids: string[]) => postApi.deleted(ids),
    onSuccess: (text) => {
      message.success(text || '删除成功')
      invalidate()
    },
    onError: showError,
  })

  const columns: TableColumnsType<PostRow> = [
    {
      title: '岗位编码',
      dataIndex: 'code',
      width: COL.code,

      render: (value: string) => (
        <span className="ferry-mono" style={{ fontSize: 12, color: 'var(--text-2)' }}>
          {value}
        </span>
      ),
    },
    {
      title: '岗位名称',
      dataIndex: 'name',
      render: (value: string) => <span style={{ fontWeight: 500, color: 'var(--text-1)' }}>{value}</span>,
    },
    {
      title: '排序',
      dataIndex: 'sort',
      width: COL.sort,

      align: 'right',
      render: (value: number) => <span className="ferry-mono">{value}</span>,
    },
    {
      title: '状态',
      dataIndex: 'status',
      width: COL.status,

      render: (value: number) => (
        <span className="ferry-status" data-tone={value === 1 ? 'success' : 'default'}>
          <span className="ferry-status__dot" />
          {value === 1 ? '正常' : '停用'}
        </span>
      ),
    },
    {
      title: '创建时间',
      dataIndex: 'createTime',
      width: COL.time,

      render: (value: string) => (
        <span className="ferry-mono" style={{ fontSize: 12, color: 'var(--text-3)' }}>
          {value ?? '—'}
        </span>
      ),
    },
    {
      title: '操作',
      key: 'action',
      width: COL.action,

      fixed: 'right',
      render: (_, record) => (
        <div style={{ display: 'flex', alignItems: 'center', gap: 2 }}>
          {can('system:post:save') && (
            <Tooltip title="编辑">
              <Button
                type="link"
                size="small"
                icon={<Pencil size={14} />}
                onClick={() => {
                  setEditing(record)
                  form.resetFields()
                  form.setFieldsValue({ ...record })
                  setOpen(true)
                }}
              />
            </Tooltip>
          )}
          {can('system:post:deleted') && (
            <Tooltip title="删除">
              <Button
                type="link"
                size="small"
                danger
                icon={<Trash2 size={14} />}
                onClick={() =>
                  modal.confirm({
                    title: `删除岗位「${record.name}」？`,
                    content: '仍有用户绑定该岗位时不可删除。',
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
        title="岗位管理"
        description="岗位用于描述职责序列，与角色互补：岗位决定「在哪个位置」，角色决定「能做什么」。"
        tag={<RoleTag>共 {query.data?.total ?? 0} 个岗位</RoleTag>}
        extra={
          can('system:post:save') && (
            <Button
              type="primary"
              icon={<Plus size={14} />}
              onClick={() => {
                setEditing(null)
                form.resetFields()
                form.setFieldsValue({ status: 1, sort: 10 })
                setOpen(true)
              }}
            >
              新增岗位
            </Button>
          )
        }
      />

      <section className="ferry-panel">
        <div className="ferry-toolbar">
          <Input
            allowClear
            prefix={<Search size={15} style={{ color: 'var(--text-4)' }} />}
            placeholder="岗位编码"
            style={{ width: 150 }}
            value={draft.code}
            onChange={(event) => setDraft((prev) => ({ ...prev, code: event.target.value }))}
            onPressEnter={() => setFilters(draft)}
          />
          <Input
            allowClear
            placeholder="岗位名称"
            style={{ width: 160 }}
            value={draft.name}
            onChange={(event) => setDraft((prev) => ({ ...prev, name: event.target.value }))}
          />
          <Select
            allowClear
            placeholder="状态"
            style={{ width: 120 }}
            value={draft.status}
            onChange={(value) => setDraft((prev) => ({ ...prev, status: value }))}
            options={[
              { value: 1, label: '正常' },
              { value: 0, label: '停用' },
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
          <Table<PostRow>
            rowKey="id"
            columns={columns}
            dataSource={query.data?.records ?? []}
            loading={query.isLoading || query.isFetching}
            scroll={{ x: 'max-content' }}
            pagination={{
              current: page.current,
              pageSize: page.size,
              total: query.data?.total ?? 0,
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
        title={editing ? `编辑岗位 · ${editing.name}` : '新增岗位'}
        okText="保存"
        cancelText="取消"
        width={520}
        confirmLoading={saveMutation.isPending}
        onCancel={() => setOpen(false)}
        onOk={async () => {
          const values = await form.validateFields()
          saveMutation.mutate({ ...values, id: editing?.id })
        }}
        destroyOnHidden
      >
        <Form form={form} layout="vertical" requiredMark="optional">
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(2, minmax(0, 1fr))', gap: '0 16px' }}>
            <Form.Item
              name="code"
              label="岗位编码"
              rules={[
                { required: true, message: '请输入岗位编码' },
                { pattern: /^[A-Z][A-Z0-9]{1,9}$/, message: '大写字母开头，2-10 位' },
              ]}
            >
              <Input placeholder="例如 BRDE" maxLength={10} />
            </Form.Item>
            <Form.Item name="name" label="岗位名称" rules={[{ required: true, message: '请输入岗位名称' }]}>
              <Input placeholder="例如 后端研发工程师" maxLength={20} />
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
