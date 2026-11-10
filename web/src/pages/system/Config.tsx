import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  App,
  Button,
  Form,
  Input,
  Modal,
  Select,
  Table,
  Tag,
  Tooltip,
  type TableColumnsType,
} from 'antd'
import { DeleteOutlined, EditOutlined, PlusOutlined, ReloadOutlined, SearchOutlined } from '@ant-design/icons'
import PageHeader from '@/components/PageHeader'
import { RoleTag } from '@/components/StatusTag'
import { configApi } from '@/api/system'
import type { ConfigRow } from '@/api/types'
import { useAuthStore } from '@/stores/auth'
import { showError } from '@/lib/notify'

export default function Config() {
  const { message, modal } = App.useApp()
  const queryClient = useQueryClient()
  const can = useAuthStore((state) => state.can)
  const [form] = Form.useForm()
  const [open, setOpen] = useState(false)
  const [editing, setEditing] = useState<ConfigRow | null>(null)
  const [draft, setDraft] = useState<{ name?: string; code?: string; isInternal?: number }>({})
  const [filters, setFilters] = useState<{ name?: string; code?: string; isInternal?: number }>({})
  const [page, setPage] = useState({ current: 1, size: 30 })

  const query = useQuery({
    queryKey: ['configs', filters, page],
    queryFn: () => configApi.pageList({ ...filters, ...page }),
  })

  const invalidate = () => {
    void queryClient.invalidateQueries({ queryKey: ['configs'] })
    void queryClient.invalidateQueries({ queryKey: ['workbench-overview'] })
  }

  const saveMutation = useMutation({
    mutationFn: (values: Record<string, unknown>) => configApi.save(values),
    onSuccess: (text) => {
      message.success(text || '保存成功')
      setOpen(false)
      invalidate()
    },
    onError: showError,
  })

  const deleteMutation = useMutation({
    mutationFn: (ids: string[]) => configApi.deleted(ids),
    onSuccess: (text) => {
      message.success(text || '删除成功')
      invalidate()
    },
    onError: showError,
  })

  const columns: TableColumnsType<ConfigRow> = [
    {
      title: '参数名称',
      dataIndex: 'name',
      width: 260,

      render: (value: string, record) => (
        <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
          <span style={{ fontWeight: 500, color: 'var(--text-1)' }}>{value}</span>
          {record.isInternal === 1 && (
            <Tag color="blue" style={{ fontSize: 10 }}>
              内置
            </Tag>
          )}
        </div>
      ),
    },
    {
      title: '参数键名',
      dataIndex: 'code',
      width: 260,

      render: (value: string) => (
        <span className="ferry-mono" style={{ fontSize: 12, color: 'var(--text-2)' }}>
          {value}
        </span>
      ),
    },
    {
      title: '参数值',
      dataIndex: 'value',
      width: 260,
      render: (value: string) => (
        <Tooltip title={value}>
          <span className="ferry-mono" style={{ fontSize: 12, color: 'var(--text-1)' }}>
            {value || '—'}
          </span>
        </Tooltip>
      ),
    },
    {
      title: '更新时间',
      dataIndex: 'updateTime',
      width: 260,

      render: (value: string | null) => (
        <span className="ferry-mono" style={{ fontSize: 12, color: 'var(--text-3)' }}>
          {value ?? '—'}
        </span>
      ),
    },
    {
      title: '操作',
      key: 'action',
      width: 150,

      fixed: 'right',
      render: (_, record) => (
        <div style={{ display: 'flex', alignItems: 'center', gap: 2 }}>
          {can('system:config:save') && (
            <Button
              type="link"
              size="small"
              icon={<EditOutlined />}
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
          {can('system:config:deleted') && (
            <Tooltip title={record.isInternal === 1 ? '系统内置参数不可删除' : '删除'}>
              <Button
                type="link"
                size="small"
                danger
                disabled={record.isInternal === 1}
                icon={<DeleteOutlined />}
                onClick={() =>
                  modal.confirm({
                    title: `删除参数「${record.name}」？`,
                    content: '删除后，依赖该参数的配置项将回落到代码默认值。该操作写入审计。',
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
        title="参数配置"
        description="登录策略、留存口径、门禁阈值等运行参数集中在此。改动即时生效并写入审计，无需发版。"
        tag={<RoleTag>共 {query.data?.total ?? 0} 项</RoleTag>}
        extra={
          can('system:config:save') && (
            <Button
              type="primary"
              icon={<PlusOutlined />}
              onClick={() => {
                setEditing(null)
                form.resetFields()
                setOpen(true)
              }}
            >
              新增参数
            </Button>
          )
        }
      />

      <section className="ferry-panel">
        <div className="ferry-toolbar">
          <Input
            allowClear
            prefix={<SearchOutlined style={{ color: 'var(--text-4)' }} />}
            placeholder="参数名称"
            style={{ width: 180 }}
            value={draft.name}
            onChange={(event) => setDraft((prev) => ({ ...prev, name: event.target.value }))}
            onPressEnter={() => setFilters(draft)}
          />
          <Input
            allowClear
            placeholder="参数键名"
            style={{ width: 240 }}
            value={draft.code}
            onChange={(event) => setDraft((prev) => ({ ...prev, code: event.target.value }))}
          />
          <Select
            allowClear
            placeholder="类型"
            style={{ width: 130 }}
            value={draft.isInternal}
            onChange={(value) => setDraft((prev) => ({ ...prev, isInternal: value }))}
            options={[
              { value: 1, label: '系统内置' },
              { value: 0, label: '自定义' },
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
          <Table<ConfigRow>
            rowKey="id"
            columns={columns}
            dataSource={query.data?.records ?? []}
            loading={query.isLoading || query.isFetching}
            scroll={{ x: 1190 }}
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
        title={editing ? `编辑参数 · ${editing.name}` : '新增参数'}
        okText="保存"
        cancelText="取消"
        width={560}
        confirmLoading={saveMutation.isPending}
        onCancel={() => setOpen(false)}
        onOk={async () => {
          const values = await form.validateFields()
          saveMutation.mutate({ ...values, id: editing?.id })
        }}
        destroyOnHidden
      >
        <Form form={form} layout="vertical" requiredMark="optional">
          <Form.Item name="name" label="参数名称" rules={[{ required: true, message: '请输入参数名称' }]}>
            <Input placeholder="例如 登录图形验证码开关" maxLength={100} />
          </Form.Item>
          <Form.Item
            name="code"
            label="参数键名"
            rules={[
              { required: true, message: '请输入参数键名' },
              { pattern: /^[A-Z][A-Z0-9_]{2,99}$/, message: '大写字母开头，可含数字与下划线' },
            ]}
            extra={editing?.isInternal === 1 ? '内置参数的键名不可修改' : '业务代码通过该键名读取，创建后不可修改'}
          >
            <Input placeholder="例如 SYS_LOGIN_CAPTCHA_ENABLED" disabled={!!editing} maxLength={100} />
          </Form.Item>
          <Form.Item name="value" label="参数值" rules={[{ required: true, message: '请输入参数值' }]}>
            <Input.TextArea rows={3} placeholder="例如 true" maxLength={500} showCount />
          </Form.Item>
        </Form>
      </Modal>
    </>
  )
}
