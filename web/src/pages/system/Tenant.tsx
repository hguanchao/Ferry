import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Alert, App, Button, Form, Input, Modal, Switch, Table, Tag, Tooltip, type TableColumnsType } from 'antd'
import { Pencil, Plus, RotateCw, Search, Trash2 } from 'lucide-react'
import PageHeader from '@/components/PageHeader'
import { RoleTag } from '@/components/StatusTag'
import { tenantApi } from '@/api/system'
import type { TenantRow } from '@/api/types'
import { useAuthStore } from '@/stores/auth'
import { showError } from '@/lib/notify'
import { COL } from '@/lib/table'

export default function Tenant() {
  const { message, modal } = App.useApp()
  const queryClient = useQueryClient()
  const can = useAuthStore((state) => state.can)
  const [form] = Form.useForm()
  const [open, setOpen] = useState(false)
  const [editing, setEditing] = useState<TenantRow | null>(null)
  const [draft, setDraft] = useState<{ code?: string; name?: string }>({})
  const [filters, setFilters] = useState<{ code?: string; name?: string }>({})
  const [page, setPage] = useState({ current: 1, size: 30 })

  const query = useQuery({
    queryKey: ['tenants', filters, page],
    queryFn: () => tenantApi.pageList({ ...filters, ...page }),
  })

  const invalidate = () => {
    void queryClient.invalidateQueries({ queryKey: ['tenants'] })
    void queryClient.invalidateQueries({ queryKey: ['workbench-overview'] })
  }

  const saveMutation = useMutation({
    mutationFn: (values: Record<string, unknown>) => tenantApi.save(values),
    onSuccess: (text) => {
      message.success(text || '保存成功')
      setOpen(false)
      invalidate()
    },
    onError: showError,
  })

  const deleteMutation = useMutation({
    mutationFn: (ids: string[]) => tenantApi.deleted(ids),
    onSuccess: (text) => {
      message.success(text || '删除成功')
      invalidate()
    },
    onError: showError,
  })

  const columns: TableColumnsType<TenantRow> = [
    {
      title: '租户名称',
      dataIndex: 'name',
      render: (value: string) => <span style={{ fontWeight: 500, color: 'var(--text-1)' }}>{value}</span>,
    },
    {
      title: '租户编码',
      dataIndex: 'code',
      width: COL.code,

      render: (value: string) => (
        <span className="ferry-mono" style={{ fontSize: 12 }}>
          {value}
        </span>
      ),
    },
    {
      title: '类型',
      dataIndex: 'code',
      width: 172,

      render: (value: string) =>
        value === '000000' ? (
          <Tag color="blue">默认租户</Tag>
        ) : (
          <Tag>试点租户</Tag>
        ),
    },
    {
      title: '状态',
      dataIndex: 'status',
      width: 172,

      render: (value: number) => (
        <span className="ferry-status" data-tone={value === 1 ? 'success' : 'default'}>
          <span className="ferry-status__dot" />
          {value === 1 ? '正常' : '停用'}
        </span>
      ),
    },
    {
      title: '创建人',
      dataIndex: 'createUser',
      width: COL.creator,

      render: (value: string) => value || '—',
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
      width: 160,

      fixed: 'right',
      render: (_, record) => (
        <div style={{ display: 'flex', alignItems: 'center', gap: 2 }}>
          {can('system:tenant:save') && (
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
            >
              编辑
            </Button>
          )}
          {can('system:tenant:deleted') && (
            <Tooltip title={record.code === '000000' ? '默认租户不可删除' : '删除'}>
              <Button
                type="link"
                size="small"
                danger
                disabled={record.code === '000000'}
                icon={<Trash2 size={14} />}
                onClick={() =>
                  modal.confirm({
                    title: `删除租户「${record.name}」？`,
                    content: '租户下的账号将无法登录。该操作不可撤销，需先完成数据归档。',
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
        title="租户管理"
        description="本期服务内部研发团队，表结构统一携带 tenant_id 并固定写入默认值。多租户自助注册、计费与租户管理后台不在本期范围。"
        tag={<RoleTag>共 {query.data?.total ?? 0} 个租户</RoleTag>}
        extra={
          can('system:tenant:save') && (
            <Button
              type="primary"
              icon={<Plus size={14} />}
              onClick={() => {
                setEditing(null)
                form.resetFields()
                form.setFieldsValue({ status: 1 })
                setOpen(true)
              }}
            >
              新增租户
            </Button>
          )
        }
      />

      <Alert
        type="info"
        showIcon
        style={{ marginBottom: 12 }}
        message="单租户运行"
        description="PRD 特殊说明第 7 条：多租户自助注册与租户管理后台本期不建设，此页仅保留租户主数据，用于未来数据隔离与计费扩展。"
      />

      <section className="ferry-panel">
        <div className="ferry-toolbar">
          <Input
            allowClear
            prefix={<Search size={15} style={{ color: 'var(--text-4)' }} />}
            placeholder="租户名称"
            style={{ width: 180 }}
            value={draft.name}
            onChange={(event) => setDraft((prev) => ({ ...prev, name: event.target.value }))}
            onPressEnter={() => setFilters(draft)}
          />
          <Input
            allowClear
            placeholder="租户编码"
            style={{ width: 150 }}
            value={draft.code}
            onChange={(event) => setDraft((prev) => ({ ...prev, code: event.target.value }))}
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
          <Table<TenantRow>
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
              pageSizeOptions: ['30', '100'],
              showTotal: (total) => `共 ${total} 条`,
              onChange: (current, size) => setPage({ current, size }),
            }}
          />
        </div>
      </section>

      <Modal
        open={open}
        title={editing ? `编辑租户 · ${editing.name}` : '新增租户'}
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
          <Form.Item name="name" label="租户名称" rules={[{ required: true, message: '请输入租户名称' }]}>
            <Input placeholder="例如 研发中心（试点）" maxLength={20} />
          </Form.Item>
          <Form.Item
            name="code"
            label="租户编码"
            rules={[
              { required: true, message: '请输入租户编码' },
              { pattern: /^\d{6}$/, message: '6 位数字' },
            ]}
            extra={editing?.code === '000000' ? '默认租户编码不可修改' : '写入所有业务表的 tenant_id'}
          >
            <Input placeholder="例如 000001" maxLength={6} disabled={editing?.code === '000000'} />
          </Form.Item>
          <Form.Item name="status" label="状态" valuePropName="checked" getValueFromEvent={(checked) => (checked ? 1 : 0)}>
            <Switch checkedChildren="正常" unCheckedChildren="停用" />
          </Form.Item>
        </Form>
      </Modal>
    </>
  )
}
