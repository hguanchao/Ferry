import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  App,
  Button,
  Form,
  Input,
  Modal,
  Select,
  Tag,
  Tooltip,
  type TableColumnsType,
} from 'antd'
import FerryTable from '@/components/FerryTable'
import { Check, Pencil, Plus, RotateCw, Search, Trash2, Shuffle } from 'lucide-react'
import PageHeader from '@/components/PageHeader'
import { RoleTag } from '@/components/StatusTag'
import { appApi, envApi } from '@/api/devops'
import type { EnvironmentRow } from '@/api/types-devops'
import { useAuthStore } from '@/stores/auth'
import { showError } from '@/lib/notify'
const CATEGORY_LABEL: Record<number, string> = { 1: '测试', 2: '预发', 3: '生产' }
const CATEGORY_COLOR: Record<number, string> = { 1: 'blue', 2: 'orange', 3: 'red' }
const DEPLOY_LABEL: Record<number, string> = { 1: 'SSH', 2: 'Docker', 3: 'Kubernetes' }
const DEPLOY_OPTIONS = [
  { value: 1, label: 'SSH' },
  { value: 2, label: 'Docker' },
  { value: 3, label: 'Kubernetes' },
]
const CATEGORY_OPTIONS = [
  { value: 1, label: '测试' },
  { value: 2, label: '预发' },
  { value: 3, label: '生产' },
]

export default function Environment() {
  const { message, modal } = App.useApp()
  const queryClient = useQueryClient()
  const can = useAuthStore((state) => state.can)
  const [form] = Form.useForm()
  const [changeForm] = Form.useForm()
  const [open, setOpen] = useState(false)
  const [changeOpen, setChangeOpen] = useState(false)
  const [editing, setEditing] = useState<EnvironmentRow | null>(null)
  const [changing, setChanging] = useState<EnvironmentRow | null>(null)
  const [pendingTargets, setPendingTargets] = useState<Record<string, number>>({})
  const [draft, setDraft] = useState<Record<string, unknown>>({})
  const [filters, setFilters] = useState<Record<string, unknown>>({})
  const [page, setPage] = useState({ current: 1, size: 30 })

  const query = useQuery({
    queryKey: ['environments', filters, page],
    queryFn: () => envApi.pageList({ ...filters, ...page }),
  })
  const appOptions = useQuery({ queryKey: ['app-options'], queryFn: () => appApi.options() })

  const invalidate = () => {
    void queryClient.invalidateQueries({ queryKey: ['environments'] })
    void queryClient.invalidateQueries({ queryKey: ['workbench-overview'] })
  }

  const saveMutation = useMutation({
    mutationFn: (values: Record<string, unknown>) => envApi.save(values),
    onSuccess: (text) => {
      message.success(text || '保存成功')
      setOpen(false)
      invalidate()
    },
    onError: showError,
  })

  const deleteMutation = useMutation({
    mutationFn: (ids: string[]) => envApi.deleted(ids),
    onSuccess: (text) => {
      message.success(text || '删除成功')
      invalidate()
    },
    onError: showError,
  })

  const applyChangeMutation = useMutation({
    mutationFn: (values: Record<string, unknown>) => envApi.changeTypeApply(values),
    onSuccess: (_text, values) => {
      message.success(_text || '申请已提交')
      if (changing) {
        setPendingTargets((prev) => ({ ...prev, [changing.id]: values.deployType as number }))
      }
      setChangeOpen(false)
      invalidate()
    },
    onError: showError,
  })

  const approveChangeMutation = useMutation({
    mutationFn: (values: Record<string, unknown>) => envApi.changeTypeApprove(values),
    onSuccess: (text) => {
      message.success(text || '审批完成')
      invalidate()
    },
    onError: showError,
  })

  const openEditor = (record: EnvironmentRow | null) => {
    setEditing(record)
    form.resetFields()
    if (record) {
      form.setFieldsValue({
        appId: record.appId,
        name: record.name,
        category: record.category,
        deployType: record.deployType,
        observeMinutes: record.observeMinutes,
        targetConfig: undefined,
      })
    }
    setOpen(true)
  }

  const openChange = (record: EnvironmentRow) => {
    setChanging(record)
    changeForm.resetFields()
    changeForm.setFieldsValue({ id: record.id })
    setChangeOpen(true)
  }

  const submitEditor = async () => {
    const values = await form.validateFields()
    // 生产类别需二次确认
    if (values.category === 3 && !editing) {
      modal.confirm({
        title: '确认创建生产环境？',
        content: '生产环境将自动开启保护标记，部署需独立授权且操作人与确认人不可为同一人。',
        okText: '确认创建',
        cancelText: '取消',
        onOk: () => saveMutation.mutate({ ...values, prodConfirm: true }),
      })
      return
    }
    saveMutation.mutate({ ...values, id: editing?.id })
  }

  const submitChange = async () => {
    const values = await changeForm.validateFields()
    modal.confirm({
      title: `确认变更部署类型？`,
      content: '变更需平台管理员审批；审批通过后将清理历史版本指针，部分制品将失去回滚可用性。',
      okText: '提交申请',
      cancelText: '取消',
      onOk: () => applyChangeMutation.mutate({ id: changing?.id, deployType: values.deployType, reason: values.reason }),
    })
  }

  const columns: TableColumnsType<EnvironmentRow> = [
    { title: '应用', dataIndex: 'appId',
      render: (value: string) => appOptions.data?.find((item) => item.value === value)?.label ?? value,
    },
    { title: '环境名', dataIndex: 'name',
      render: (value: string) => <span style={{ fontWeight: 500, color: 'var(--text-1)' }}>{value}</span>,
    },
    { title: '环境类别', dataIndex: 'category',
      render: (value: number) => <Tag color={CATEGORY_COLOR[value]}>{CATEGORY_LABEL[value] ?? '—'}</Tag>,
    },
    { title: '部署类型', dataIndex: 'deployType',
      render: (value: number, record) => (
        <span>
          {DEPLOY_LABEL[value] ?? '—'}
          {record.status === 2 && pendingTargets[record.id] != null && (
            <Tag color="orange" style={{ marginLeft: 6, fontSize: 10 }}>→ {DEPLOY_LABEL[pendingTargets[record.id]]}</Tag>
          )}
        </span>
      ),
    },
    { title: '配置版本', dataIndex: 'configVersion',
      render: (value: number) => <span className="ferry-mono">v{value ?? 0}</span>,
    },
    { title: '保护标记', dataIndex: 'isProtected',
      render: (value: number) => (value === 1 ? <Tag color="red">受保护</Tag> : <span style={{ color: 'var(--text-3)' }}>—</span>),
    },
    { title: '状态', dataIndex: 'status',
      render: (value: number) => (value === 2 ? <Tag color="orange">待审批</Tag> : <Tag color="green">正常</Tag>),
    },
    { title: '创建时间', dataIndex: 'createTime',
      render: (value: string) => <span className="ferry-mono" style={{ fontSize: 12, color: 'var(--text-3)' }}>{value ?? '—'}</span>,
    },
    { title: '操作', key: 'action',
      render: (_, record) => (
        <div style={{ display: 'flex', alignItems: 'center', gap: 2 }}>
          {can('devops:environment:save') && (
            <Tooltip title="编辑">
              <Button type="link" size="small" icon={<Pencil size={14} />} onClick={() => openEditor(record)} />
            </Tooltip>
          )}
          {can('devops:environment:changeType') && (
            <Tooltip title="变更类型">
              <Button type="link" size="small" icon={<Shuffle size={14} />} onClick={() => openChange(record)} />
            </Tooltip>
          )}
          {can('devops:environment:changeTypeApprove') && record.status === 2 && (
            <Tooltip title="审批">
              <Button type="link" size="small" icon={<Check size={14} />}
                onClick={() => modal.confirm({
                  title: '审批通过部署类型变更？',
                  content: '通过后立即生效并清理历史版本指针。',
                  okText: '通过', cancelText: '取消',
                  onOk: () => approveChangeMutation.mutateAsync({
                    id: record.id,
                    deployType: pendingTargets[record.id] ?? record.deployType,
                    pass: true,
                  }),
                })} />
            </Tooltip>
          )}
          {can('devops:environment:deleted') && (
            <Tooltip title="删除">
              <Button type="link" size="small" danger icon={<Trash2 size={14} />}
                onClick={() => modal.confirm({
                  title: `删除环境「${record.name}」？`,
                  content: '存在进行中部署或生效制品时不可删除。',
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
      <PageHeader title="环境管理" description="环境与实际应用绑定，每个环境固定一种部署目标类型；生产环境自动保护，部署类型变更需走审批。"
        tag={<RoleTag>共 {query.data?.total ?? 0} 项</RoleTag>}
        extra={can('devops:environment:save') && (
          <Button type="primary" icon={<Plus size={14} />} onClick={() => openEditor(null)}>新建环境</Button>
        )}
      />
      <section className="ferry-panel">
        <div className="ferry-toolbar">
          <Select allowClear showSearch placeholder="应用" style={{ width: 200 }}
            value={draft.appId}
            onChange={(value) => setDraft((prev) => ({ ...prev, appId: value }))}
            options={appOptions.data?.map((item) => ({ value: item.value, label: item.label })) ?? []}
          />
          <Input allowClear prefix={<Search size={15} style={{ color: 'var(--text-4)' }} />}
            placeholder="环境名" style={{ width: 180 }} value={draft.name as string}
            onChange={(e) => setDraft((prev) => ({ ...prev, name: e.target.value }))}
            onPressEnter={() => { setFilters(draft); setPage((p) => ({ ...p, current: 1 })) }}
          />
          <Select allowClear placeholder="环境类别" style={{ width: 120 }} value={draft.category as number}
            onChange={(value) => setDraft((prev) => ({ ...prev, category: value }))}
            options={CATEGORY_OPTIONS}
          />
          <Select allowClear placeholder="部署类型" style={{ width: 130 }} value={draft.deployType as number}
            onChange={(value) => setDraft((prev) => ({ ...prev, deployType: value }))}
            options={DEPLOY_OPTIONS}
          />
          <Button type="primary" icon={<Search size={15} />}
            onClick={() => { setFilters(draft); setPage((p) => ({ ...p, current: 1 })) }}>查询</Button>
          <Button icon={<RotateCw size={15} />}
            onClick={() => { setDraft({}); setFilters({}); setPage((p) => ({ ...p, current: 1 })) }}>重置</Button>
        </div>
        <div className="ferry-table-wrap">
          <FerryTable<EnvironmentRow> rowKey="id" columns={columns} dataSource={query.data?.records ?? []}
            loading={query.isLoading || query.isFetching} scroll={{ x: 'max-content' }}
            pagination={{
              current: page.current, pageSize: page.size, total: query.data?.total ?? 0,
              showSizeChanger: true, pageSizeOptions: ['30', '100', '200'],
              showTotal: (total) => `共 ${total} 条`, onChange: (current, size) => setPage({ current, size }),
            }}
          />
        </div>
      </section>

      <Modal open={open} title={editing ? `编辑环境 · ${editing.name}` : '新建环境'}
        okText="保存" cancelText="取消" width={560} confirmLoading={saveMutation.isPending}
        onCancel={() => setOpen(false)} onOk={submitEditor} destroyOnHidden>
        <Form form={form} layout="vertical" requiredMark="optional">
          <Form.Item name="appId" label="应用" rules={[{ required: true, message: '请选择应用' }]}>
            <Select showSearch placeholder="选择应用"
              options={appOptions.data?.map((item) => ({ value: item.value, label: item.label })) ?? []} />
          </Form.Item>
          <Form.Item name="name" label="环境名称" rules={[{ required: true, message: '请输入环境名称' }]}
            extra="同一应用内唯一">
            <Input placeholder="例如 prod、staging" maxLength={50} />
          </Form.Item>
          <Form.Item name="category" label="环境类别" rules={[{ required: true, message: '请选择环境类别' }]}>
            <Select options={CATEGORY_OPTIONS} />
          </Form.Item>
          <Form.Item name="deployType" label="部署目标类型" rules={[{ required: true, message: '请选择部署目标类型' }]}
            extra="已存在部署记录时修改需走变更单">
            <Select options={DEPLOY_OPTIONS} disabled={!!editing} />
          </Form.Item>
          <Form.Item name="observeMinutes" label="观察时长（分钟）">
            <Input type="number" placeholder="例如 SSH 30 / Docker 10 / Kubernetes 5" />
          </Form.Item>
          <Form.Item name="targetConfig" label="目标资源（JSON）">
            <Input.TextArea rows={3} placeholder='例如 {"hosts": [...]}' />
          </Form.Item>
        </Form>
      </Modal>

      <Modal open={changeOpen} title={`变更部署类型 · ${changing?.name ?? ''}`}
        okText="提交申请" cancelText="取消" width={520} confirmLoading={applyChangeMutation.isPending}
        onCancel={() => setChangeOpen(false)} onOk={submitChange} destroyOnHidden>
        <Form form={changeForm} layout="vertical" requiredMark="optional">
          <Form.Item name="deployType" label="目标部署类型" rules={[{ required: true, message: '请选择目标部署类型' }]}>
            <Select options={DEPLOY_OPTIONS} />
          </Form.Item>
          <Form.Item name="reason" label="申请理由" rules={[{ required: true, message: '请输入申请理由' }]}>
            <Input.TextArea rows={3} placeholder="说明变更原因与影响面" />
          </Form.Item>
        </Form>
      </Modal>
    </>
  )
}
