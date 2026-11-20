import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { App, Button, Form, Input, Modal, Select, Tag, Tooltip, type TableColumnsType } from 'antd'
import FerryTable from '@/components/FerryTable'
import { Pencil, Plus, RotateCw, Search, Trash2 } from 'lucide-react'
import PageHeader from '@/components/PageHeader'
import { RoleTag } from '@/components/StatusTag'
import { systemExtApi } from '@/api/devops'
import { post } from '@/api/client'
import type { NotifyTemplateRow } from '@/api/types-devops'
import { useAuthStore } from '@/stores/auth'
import { showError } from '@/lib/notify'
const EVENTS = [
  'RUN_FAILED', 'GATE_BLOCKED', 'GATE_FETCH_FAILED', 'EXEMPTION_EXPIRED', 'RC_PENDING_APPROVAL',
  'RELEASE_STARTED', 'RELEASE_CIRCUIT', 'ROLLBACK_DONE', 'ACTION_SUCCEEDED',
]
const CHANNEL_LABEL: Record<number, string> = { 1: '企业 IM', 2: '邮件', 3: 'Webhook' }

/** 后端 NotifyTemplateVO 比共享类型多返回 channelType/titleTemplate 等字段 */
type TemplateExt = NotifyTemplateRow & {
  channelType?: number
  titleTemplate?: string
  bodyTemplate?: string
  variables?: string
  isEnabled?: number
}

export default function NotifyTemplate() {
  const { message, modal } = App.useApp()
  const queryClient = useQueryClient()
  const can = useAuthStore((state) => state.can)
  const [form] = Form.useForm()
  const [open, setOpen] = useState(false)
  const [editing, setEditing] = useState<TemplateExt | null>(null)
  const [draft, setDraft] = useState<Record<string, unknown>>({})
  const [filters, setFilters] = useState<Record<string, unknown>>({})
  const [page, setPage] = useState({ current: 1, size: 30 })

  const query = useQuery({
    queryKey: ['notify-template', filters, page],
    queryFn: () => systemExtApi.notifyTemplatePage({ ...filters, ...page }),
  })
  const invalidate = () => void queryClient.invalidateQueries({ queryKey: ['notify-template'] })

  const saveMutation = useMutation({
    mutationFn: (values: Record<string, unknown>) => systemExtApi.notifyTemplateSave(values),
    onSuccess: (text) => { message.success(text || '保存成功'); setOpen(false); invalidate() },
    onError: showError,
  })
  const deleteMutation = useMutation({
    mutationFn: (ids: string[]) => post<string>('/ferry-devops/system/notifyTemplate/deleted', ids),
    onSuccess: () => { message.success('删除成功'); invalidate() },
    onError: showError,
  })

  const openEditor = (record: TemplateExt | null) => {
    setEditing(record)
    form.resetFields()
    if (record) {
      form.setFieldsValue({
        id: record.id, event: record.event, channelType: record.channelType,
        titleTemplate: record.titleTemplate, bodyTemplate: record.bodyTemplate,
        variables: record.variables, isEnabled: record.isEnabled,
      })
    }
    setOpen(true)
  }

  const columns: TableColumnsType<TemplateExt> = [
    { title: '事件', dataIndex: 'event',
      render: (v: string) => <span className="ferry-mono" style={{ fontSize: 12 }}>{v}</span> },
    { title: '渠道类型', dataIndex: 'channelType',
      render: (v: number) => <Tag color="blue">{CHANNEL_LABEL[v] ?? v}</Tag> },
    { title: '标题模板', dataIndex: 'titleTemplate',
      render: (v: string) => <Tooltip title={v}><span>{v}</span></Tooltip> },
    { title: '变量清单', dataIndex: 'variables',
      render: (v?: string) => <span style={{ color: 'var(--text-3)' }}>{v || '—'}</span> },
    { title: '状态', dataIndex: 'isEnabled',
      render: (v: number) => <Tag color={v === 1 ? 'green' : 'default'}>{v === 1 ? '启用' : '停用'}</Tag> },
    { title: '操作', key: 'action',
      render: (_, record) => (
        <div style={{ display: 'flex', alignItems: 'center', gap: 2 }}>
          {can('system:notify:templateSave') && (
            <Tooltip title="编辑">
              <Button type="link" size="small" icon={<Pencil size={14} />} onClick={() => openEditor(record)} />
            </Tooltip>
          )}
          {can('system:notify:templateSave') && (
            <Tooltip title="删除">
              <Button type="link" size="small" danger icon={<Trash2 size={14} />}
                onClick={() => modal.confirm({
                  title: `删除模板「${record.event}」？`, okText: '删除', okButtonProps: { danger: true }, cancelText: '取消',
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
      <PageHeader title="通知模板管理" description="按事件类型与渠道维护消息模板。变量须在变量清单中声明，否则服务端校验报错。"
        tag={<RoleTag>共 {query.data?.total ?? 0} 项</RoleTag>}
        extra={can('system:notify:templateSave') && (
          <Button type="primary" icon={<Plus size={14} />} onClick={() => openEditor(null)}>新建模板</Button>
        )}
      />
      <section className="ferry-panel">
        <div className="ferry-toolbar">
          <Input allowClear prefix={<Search size={15} />} placeholder="事件" style={{ width: 200 }}
            value={draft.event as string}
            onChange={(e) => setDraft((prev) => ({ ...prev, event: e.target.value }))}
            onPressEnter={() => { setFilters(draft); setPage((p) => ({ ...p, current: 1 })) }} />
          <Select allowClear placeholder="渠道类型" style={{ width: 130 }} value={draft.channelType as number}
            onChange={(value) => setDraft((prev) => ({ ...prev, channelType: value }))}
            options={Object.entries(CHANNEL_LABEL).map(([k, v]) => ({ value: Number(k), label: v }))} />
          <Button type="primary" icon={<Search size={15} />}
            onClick={() => { setFilters(draft); setPage((p) => ({ ...p, current: 1 })) }}>查询</Button>
          <Button icon={<RotateCw size={15} />}
            onClick={() => { setDraft({}); setFilters({}); setPage((p) => ({ ...p, current: 1 })) }}>重置</Button>
        </div>
        <div className="ferry-table-wrap">
          <FerryTable<TemplateExt> rowKey="id" columns={columns} dataSource={(query.data?.records ?? []) as TemplateExt[]}
            loading={query.isLoading || query.isFetching} scroll={{ x: 'max-content' }}
            pagination={{
              current: page.current, pageSize: page.size, total: query.data?.total ?? 0,
              showSizeChanger: true, pageSizeOptions: ['30', '100', '200'],
              showTotal: (total) => `共 ${total} 条`, onChange: (current, size) => setPage({ current, size }),
            }} />
        </div>
      </section>

      <Modal open={open} title={editing ? '编辑通知模板' : '新建通知模板'}
        okText="保存" cancelText="取消" width={620} confirmLoading={saveMutation.isPending}
        onCancel={() => setOpen(false)}
        onOk={async () => {
          const values = await form.validateFields()
          saveMutation.mutate({ ...values, id: editing?.id })
        }} destroyOnHidden>
        <Form form={form} layout="vertical" requiredMark="optional" initialValues={{ channelType: 1, isEnabled: 1 }}>
          <Form.Item name="event" label="事件类型" rules={[{ required: true, message: '请选择事件' }]}>
            <Select showSearch options={EVENTS.map((e) => ({ value: e, label: e }))} />
          </Form.Item>
          <Form.Item name="channelType" label="渠道类型" rules={[{ required: true }]}>
            <Select options={Object.entries(CHANNEL_LABEL).map(([k, v]) => ({ value: Number(k), label: v }))} />
          </Form.Item>
          <Form.Item name="titleTemplate" label="标题模板" rules={[{ required: true, message: '请输入标题模板' }]}>
            <Input placeholder="例如【${appName}】运行失败" />
          </Form.Item>
          <Form.Item name="bodyTemplate" label="正文模板" rules={[{ required: true, message: '请输入正文模板' }]}>
            <Input.TextArea rows={4} placeholder="应用：${appName}\n环境：${env}\n失败阶段：${stage}" />
          </Form.Item>
          <Form.Item name="variables" label="变量清单" extra="逗号分隔，如 appName,env,stage,riskLevel">
            <Input placeholder="appName,env,stage" />
          </Form.Item>
          <Form.Item name="isEnabled" label="状态">
            <Select options={[{ value: 1, label: '启用' }, { value: 0, label: '停用' }]} />
          </Form.Item>
        </Form>
      </Modal>
    </>
  )
}
