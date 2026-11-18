import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { App, Button, Form, Input, Modal, Select, Table, Tag, Tooltip, type TableColumnsType } from 'antd'
import { Pencil, Plus, RefreshCw, RotateCw, Search, Trash2 } from 'lucide-react'
import PageHeader from '@/components/PageHeader'
import { RoleTag } from '@/components/StatusTag'
import { credentialApi } from '@/api/devops'
import type { CredentialRow } from '@/api/types-devops'
import { useAuthStore } from '@/stores/auth'
import { showError } from '@/lib/notify'
import { COL } from '@/lib/table'

const TYPE_LABEL: Record<number, string> = {
  1: 'SSH 密钥',
  2: '对象存储 AK/SK',
  3: 'Registry 账号',
  4: 'K8s 凭据',
  5: '第三方 Token',
}
const TYPE_OPTIONS = [
  { value: 1, label: 'SSH 密钥' },
  { value: 2, label: '对象存储 AK/SK' },
  { value: 3, label: 'Registry 账号' },
  { value: 4, label: 'K8s 凭据' },
  { value: 5, label: '第三方 Token' },
]

export default function Credential() {
  const { message, modal } = App.useApp()
  const queryClient = useQueryClient()
  const can = useAuthStore((state) => state.can)
  const [form] = Form.useForm()
  const [rotateForm] = Form.useForm()
  const [open, setOpen] = useState(false)
  const [rotateOpen, setRotateOpen] = useState(false)
  const [editing, setEditing] = useState<CredentialRow | null>(null)
  const [rotating, setRotating] = useState<CredentialRow | null>(null)
  const [draft, setDraft] = useState<Record<string, unknown>>({})
  const [filters, setFilters] = useState<Record<string, unknown>>({})
  const [page, setPage] = useState({ current: 1, size: 30 })

  const query = useQuery({
    queryKey: ['credentials', filters, page],
    queryFn: () => credentialApi.pageList({ ...filters, ...page }),
  })

  const invalidate = () => {
    void queryClient.invalidateQueries({ queryKey: ['credentials'] })
  }

  const saveMutation = useMutation({
    mutationFn: (values: Record<string, unknown>) => credentialApi.save(values),
    onSuccess: (text) => {
      message.success(text || '保存成功')
      setOpen(false)
      invalidate()
    },
    onError: showError,
  })

  const deleteMutation = useMutation({
    mutationFn: (ids: string[]) => credentialApi.deleted(ids),
    onSuccess: (text) => {
      message.success(text || '删除成功')
      invalidate()
    },
    onError: showError,
  })

  const rotateMutation = useMutation({
    mutationFn: ({ id, value }: { id: string; value: string }) => credentialApi.rotate(id, { value }),
    onSuccess: (text) => {
      message.success(text || '轮换成功')
      setRotateOpen(false)
      invalidate()
    },
    onError: showError,
  })

  const openEditor = (record: CredentialRow | null) => {
    setEditing(record)
    form.resetFields()
    if (record) {
      form.setFieldsValue({ name: record.name, type: record.type })
    }
    setOpen(true)
  }

  const openRotate = (record: CredentialRow) => {
    setRotating(record)
    rotateForm.resetFields()
    setRotateOpen(true)
  }

  const columns: TableColumnsType<CredentialRow> = [
    { title: 'ID', dataIndex: 'id', width: COL.code,
      render: (value: string) => <span className="ferry-mono" style={{ fontSize: 12 }}>{value}</span>,
    },
    { title: '名称', dataIndex: 'name',
      render: (value: string) => <span style={{ fontWeight: 500, color: 'var(--text-1)' }}>{value}</span>,
    },
    { title: '类型', dataIndex: 'type', width: 150,
      render: (value: number) => TYPE_LABEL[value] ?? '—',
    },
    { title: '凭据值', key: 'cipherText', width: 110,
      render: () => <span className="ferry-mono" style={{ color: 'var(--text-3)', letterSpacing: 2 }}>******</span>,
    },
    { title: '引用数', dataIndex: 'refCount', width: 90,
      render: (value: number) => (
        <Tooltip title={value > 0 ? '被引用期间不可删除，仅可轮换' : '无引用'}>
          <Tag color={value > 0 ? 'blue' : 'default'}>{value ?? 0}</Tag>
        </Tooltip>
      ),
    },
    { title: '最后轮换', dataIndex: 'lastRotateTime', width: COL.time,
      render: (value: string) => <span className="ferry-mono" style={{ fontSize: 12, color: 'var(--text-3)' }}>{value ?? '—'}</span>,
    },
    { title: '创建时间', dataIndex: 'createTime', width: COL.time,
      render: (value: string) => <span className="ferry-mono" style={{ fontSize: 12, color: 'var(--text-3)' }}>{value ?? '—'}</span>,
    },
    { title: '操作', key: 'action', width: COL.actionWide, fixed: 'right',
      render: (_, record) => (
        <div style={{ display: 'flex', alignItems: 'center', gap: 2 }}>
          {can('devops:credential:save') && (
            <Button type="link" size="small" icon={<Pencil size={14} />} onClick={() => openEditor(record)}>编辑</Button>
          )}
          {can('devops:credential:rotate') && (
            <Button type="link" size="small" icon={<RefreshCw size={14} />} onClick={() => openRotate(record)}>轮换</Button>
          )}
          {can('devops:credential:deleted') && (
            <Tooltip title={record.refCount > 0 ? '凭据被引用，不可删除' : '删除'}>
              <Button type="link" size="small" danger icon={<Trash2 size={14} />}
                disabled={record.refCount > 0}
                onClick={() => modal.confirm({
                  title: `删除凭据「${record.name}」？`,
                  content: '被引用期间不可删除，仅可轮换。删除后不可恢复。',
                  okText: '删除', okButtonProps: { danger: true }, cancelText: '取消',
                  onOk: () => deleteMutation.mutateAsync([record.id]),
                })}
              />
            </Tooltip>
          )}
        </div>
      ),
    },
  ]

  return (
    <>
      <PageHeader title="凭据管理" description="凭据集中托管，加密存储；列表与详情仅返回掩码，明文只在执行器运行时按需获取，被引用期间仅可轮换。"
        tag={<RoleTag>共 {query.data?.total ?? 0} 项</RoleTag>}
        extra={can('devops:credential:save') && (
          <Button type="primary" icon={<Plus size={14} />} onClick={() => openEditor(null)}>新增凭据</Button>
        )}
      />
      <section className="ferry-panel">
        <div className="ferry-toolbar">
          <Input allowClear prefix={<Search size={15} style={{ color: 'var(--text-4)' }} />}
            placeholder="凭据名称" style={{ width: 200 }} value={draft.name as string}
            onChange={(e) => setDraft((prev) => ({ ...prev, name: e.target.value }))}
            onPressEnter={() => { setFilters(draft); setPage((p) => ({ ...p, current: 1 })) }}
          />
          <Select allowClear placeholder="凭据类型" style={{ width: 160 }} value={draft.type as number}
            onChange={(value) => setDraft((prev) => ({ ...prev, type: value }))}
            options={TYPE_OPTIONS}
          />
          <Button type="primary" icon={<Search size={15} />}
            onClick={() => { setFilters(draft); setPage((p) => ({ ...p, current: 1 })) }}>查询</Button>
          <Button icon={<RotateCw size={15} />}
            onClick={() => { setDraft({}); setFilters({}); setPage((p) => ({ ...p, current: 1 })) }}>重置</Button>
        </div>
        <div className="ferry-table-wrap">
          <Table<CredentialRow> rowKey="id" columns={columns} dataSource={query.data?.records ?? []}
            loading={query.isLoading || query.isFetching} scroll={{ x: 'max-content' }}
            pagination={{
              current: page.current, pageSize: page.size, total: query.data?.total ?? 0,
              showSizeChanger: true, pageSizeOptions: ['30', '100', '200'],
              showTotal: (total) => `共 ${total} 条`, onChange: (current, size) => setPage({ current, size }),
            }}
          />
        </div>
      </section>

      <Modal open={open} title={editing ? `编辑凭据 · ${editing.name}` : '新增凭据'}
        okText="保存" cancelText="取消" width={520} confirmLoading={saveMutation.isPending}
        onCancel={() => setOpen(false)}
        onOk={async () => {
          const values = await form.validateFields()
          saveMutation.mutate({ ...values, id: editing?.id })
        }}
        destroyOnHidden>
        <Form form={form} layout="vertical" requiredMark="optional">
          <Form.Item name="name" label="凭据名称" rules={[{ required: true, message: '请输入凭据名称' }]}>
            <Input placeholder="例如 prod-db-ssh-key" maxLength={100} />
          </Form.Item>
          <Form.Item name="type" label="凭据类型" rules={[{ required: true, message: '请选择凭据类型' }]}>
            <Select options={TYPE_OPTIONS} />
          </Form.Item>
          <Form.Item name="value" label="凭据值" rules={editing ? [] : [{ required: true, message: '请输入凭据值' }]}
            extra={editing ? '编辑时留空表示保持原值' : '保存时加密存储，读取接口仅返回掩码'}>
            <Input.TextArea rows={3} placeholder="密钥 / AK-SK / 令牌内容" />
          </Form.Item>
        </Form>
      </Modal>

      <Modal open={rotateOpen} title={`轮换凭据 · ${rotating?.name ?? ''}`}
        okText="确认轮换" cancelText="取消" width={520} confirmLoading={rotateMutation.isPending}
        onCancel={() => setRotateOpen(false)}
        onOk={async () => {
          const values = await rotateForm.validateFields()
          if (!rotating) return
          rotateMutation.mutate({ id: rotating.id, value: values.value })
        }}
        destroyOnHidden>
        <Form form={rotateForm} layout="vertical" requiredMark="optional">
          <Form.Item name="value" label="新凭据值" rules={[{ required: true, message: '请输入新的凭据值' }]}
            extra="轮换后所有引用自动生效，无需修改配置">
            <Input.TextArea rows={3} placeholder="输入新的密钥 / 令牌内容" />
          </Form.Item>
        </Form>
      </Modal>
    </>
  )
}
