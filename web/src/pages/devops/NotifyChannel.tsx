import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { App, Button, Form, Input, Modal, Select, Table, Tag, Tooltip, type TableColumnsType } from 'antd'
import { Pencil, Plus, RotateCw, Search, Send, Trash2 } from 'lucide-react'
import PageHeader from '@/components/PageHeader'
import { RoleTag } from '@/components/StatusTag'
import { systemExtApi } from '@/api/devops'
import { post } from '@/api/client'
import type { NotifyChannelRow } from '@/api/types-devops'
import { useAuthStore } from '@/stores/auth'
import { showError } from '@/lib/notify'
import { COL } from '@/lib/table'

const TYPE_LABEL: Record<number, string> = { 1: '企业 IM', 2: '邮件', 3: 'Webhook' }
const STATUS_LABEL: Record<number, string> = { 1: '正常', 2: '异常' }

/** 后端 NotifyChannelVO 比共享类型多返回 configJson/status 等字段 */
type ChannelExt = NotifyChannelRow & {
  configJson?: string
  status?: number
  lastSendTime?: string
  lastError?: string
}

export default function NotifyChannel() {
  const { message, modal } = App.useApp()
  const queryClient = useQueryClient()
  const can = useAuthStore((state) => state.can)
  const [form] = Form.useForm()
  const [open, setOpen] = useState(false)
  const [editing, setEditing] = useState<ChannelExt | null>(null)
  const [draft, setDraft] = useState<Record<string, unknown>>({})
  const [filters, setFilters] = useState<Record<string, unknown>>({})
  const [page, setPage] = useState({ current: 1, size: 30 })

  const query = useQuery({
    queryKey: ['notify-channel', filters, page],
    queryFn: () => systemExtApi.notifyChannelPage({ ...filters, ...page }),
  })
  const invalidate = () => void queryClient.invalidateQueries({ queryKey: ['notify-channel'] })

  const saveMutation = useMutation({
    mutationFn: (values: Record<string, unknown>) => systemExtApi.notifyChannelSave(values),
    onSuccess: (text) => { message.success(text || '保存成功'); setOpen(false); invalidate() },
    onError: showError,
  })
  const testMutation = useMutation({
    mutationFn: (id: string) => postTest(id),
    onSuccess: (text) => message.success(text || '测试消息已投递'),
    onError: showError,
  })
  const deleteMutation = useMutation({
    mutationFn: (ids: string[]) => post(`/system/notifyChannel/deleted`, ids),
    onSuccess: () => { message.success('删除成功'); invalidate() },
    onError: showError,
  })

  const openEditor = (record: ChannelExt | null) => {
    setEditing(record)
    form.resetFields()
    if (record) {
      form.setFieldsValue({ id: record.id, name: record.name, type: record.type, configJson: record.configJson, status: record.status })
    }
    setOpen(true)
  }

  const columns: TableColumnsType<ChannelExt> = [
    { title: '渠道名', dataIndex: 'name' },
    { title: '类型', dataIndex: 'type', width: 110,
      render: (v: number) => <Tag color="blue">{TYPE_LABEL[v] ?? v}</Tag> },
    { title: '配置摘要', dataIndex: 'configJson',
      render: (v?: string) => <span className="ferry-mono" style={{ fontSize: 12, color: 'var(--text-3)' }}>
        {v ? v.replace(/https?:\/\/[^\s",}]+/g, '***').slice(0, 60) : '—'}</span> },
    { title: '状态', dataIndex: 'status', width: 90,
      render: (v: number) => <Tag color={v === 1 ? 'green' : 'red'}>{STATUS_LABEL[v] ?? v}</Tag> },
    { title: '最近发送', dataIndex: 'lastSendTime', width: COL.time,
      render: (v?: string) => <span className="ferry-mono" style={{ fontSize: 12, color: 'var(--text-3)' }}>{v || '—'}</span> },
    { title: '操作', key: 'action', width: COL.actionWide, fixed: 'right',
      render: (_, record) => (
        <div style={{ display: 'flex', alignItems: 'center', gap: 2 }}>
          <Tooltip title="测试">
            <Button type="link" size="small" icon={<Send size={14} />}
              onClick={() => testMutation.mutate(record.id)} />
          </Tooltip>
          {can('system:notify:save') && (
            <Tooltip title="编辑">
              <Button type="link" size="small" icon={<Pencil size={14} />} onClick={() => openEditor(record)} />
            </Tooltip>
          )}
          {can('system:notify:deleted') && (
            <Tooltip title="删除">
              <Button type="link" size="small" danger icon={<Trash2 size={14} />}
                onClick={() => modal.confirm({
                  title: `删除渠道「${record.name}」？`, okText: '删除', okButtonProps: { danger: true }, cancelText: '取消',
                  onOk: () => deleteMutation.mutateAsync([record.id]),
                })} />
            </Tooltip>
          )}
        </div>
      ),
    },
  ]

  return (
    <>
      <PageHeader title="通知渠道管理" description="维护企业 IM / 邮件 / Webhook 三类渠道连接。敏感配置按掩码展示，密钥保存时由服务端加密。"
        tag={<RoleTag>共 {query.data?.total ?? 0} 项</RoleTag>}
        extra={can('system:notify:save') && (
          <Button type="primary" icon={<Plus size={14} />} onClick={() => openEditor(null)}>新建渠道</Button>
        )}
      />
      <section className="ferry-panel">
        <div className="ferry-toolbar">
          <Input allowClear prefix={<Search size={15} />} placeholder="渠道名" style={{ width: 200 }}
            value={draft.name as string}
            onChange={(e) => setDraft((prev) => ({ ...prev, name: e.target.value }))}
            onPressEnter={() => { setFilters(draft); setPage((p) => ({ ...p, current: 1 })) }} />
          <Select allowClear placeholder="类型" style={{ width: 130 }} value={draft.type as number}
            onChange={(value) => setDraft((prev) => ({ ...prev, type: value }))}
            options={Object.entries(TYPE_LABEL).map(([k, v]) => ({ value: Number(k), label: v }))} />
          <Button type="primary" icon={<Search size={15} />}
            onClick={() => { setFilters(draft); setPage((p) => ({ ...p, current: 1 })) }}>查询</Button>
          <Button icon={<RotateCw size={15} />}
            onClick={() => { setDraft({}); setFilters({}); setPage((p) => ({ ...p, current: 1 })) }}>重置</Button>
        </div>
        <div className="ferry-table-wrap">
          <Table<ChannelExt> rowKey="id" columns={columns} dataSource={(query.data?.records ?? []) as ChannelExt[]}
            loading={query.isLoading || query.isFetching} scroll={{ x: 'max-content' }}
            pagination={{
              current: page.current, pageSize: page.size, total: query.data?.total ?? 0,
              showSizeChanger: true, pageSizeOptions: ['30', '100', '200'],
              showTotal: (total) => `共 ${total} 条`, onChange: (current, size) => setPage({ current, size }),
            }} />
        </div>
      </section>

      <Modal open={open} title={editing ? `编辑渠道 · ${editing.name}` : '新建通知渠道'}
        okText="保存" cancelText="取消" width={560} confirmLoading={saveMutation.isPending}
        onCancel={() => setOpen(false)}
        onOk={async () => {
          const values = await form.validateFields()
          saveMutation.mutate({ ...values, id: editing?.id })
        }} destroyOnHidden>
        <Form form={form} layout="vertical" requiredMark="optional" initialValues={{ type: 1, status: 1 }}>
          <Form.Item name="name" label="渠道名" rules={[{ required: true, message: '请输入渠道名' }]}>
            <Input placeholder="例如 发布通知机器人" maxLength={100} />
          </Form.Item>
          <Form.Item name="type" label="渠道类型" rules={[{ required: true }]}>
            <Select options={Object.entries(TYPE_LABEL).map(([k, v]) => ({ value: Number(k), label: v }))} />
          </Form.Item>
          <Form.Item name="configJson" label="连接配置 JSON" extra="机器人地址 / 发件人 / 签名密钥等，敏感项由服务端加密">
            <Input.TextArea rows={4} placeholder='{"webhook":"https://...","secret":"..."}' />
          </Form.Item>
          <Form.Item name="status" label="状态">
            <Select options={[{ value: 1, label: '正常' }, { value: 2, label: '异常' }]} />
          </Form.Item>
        </Form>
      </Modal>
    </>
  )
}

/** 测试消息走 api client（devops.ts 未显式声明该路径时兜底） */
function postTest(id: string) { return post<string>(`/ferry-devops/system/notifyChannel/test/${id}`) }
