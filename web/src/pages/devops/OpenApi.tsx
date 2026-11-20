import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { App, Button, Form, Input, InputNumber, Modal, Tag, Tooltip, type TableColumnsType } from 'antd'
import FerryTable from '@/components/FerryTable'
import { Copy, KeyRound, Plus, RefreshCw, RotateCw, Search, ShieldOff } from 'lucide-react'
import PageHeader from '@/components/PageHeader'
import { RoleTag } from '@/components/StatusTag'
import { systemExtApi } from '@/api/devops'
import type { OpenTokenRow } from '@/api/types-devops'
import { useAuthStore } from '@/stores/auth'
import { showError } from '@/lib/notify'
const STATUS_LABEL: Record<number, string> = { 1: '正常', 2: '禁用' }

/** 8 个开放接口目录（V1.2.0 已移除"触发运行"） */
const API_LIST = [
  { no: 1, method: 'GET', path: '/open/v1/runs/{run_id}', usage: '查询运行状态与阶段结果' },
  { no: 2, method: 'GET', path: '/open/v1/artifacts', usage: '查询制品（按应用、环境、摘要）' },
  { no: 3, method: 'POST', path: '/open/v1/artifacts/{id}/promote', usage: '制品晋级' },
  { no: 4, method: 'GET', path: '/open/v1/environments', usage: '查询环境与当前生效制品' },
  { no: 5, method: 'POST', path: '/open/v1/release-candidates', usage: '创建发布单（仅生产、草稿）' },
  { no: 6, method: 'GET', path: '/open/v1/release-candidates/{id}', usage: '查询发布单状态与审批进度' },
  { no: 7, method: 'POST', path: '/open/v1/rollbacks', usage: '发起回滚（仅健康版本）' },
  { no: 8, method: 'GET', path: '/open/v1/health-checks', usage: '查询健康检查结论' },
]

export default function OpenApi() {
  const { message, modal } = App.useApp()
  const queryClient = useQueryClient()
  const can = useAuthStore((state) => state.can)
  const [form] = Form.useForm()
  const [open, setOpen] = useState(false)
  const [plain, setPlain] = useState<string | null>(null)
  const [draft, setDraft] = useState<Record<string, unknown>>({})
  const [filters, setFilters] = useState<Record<string, unknown>>({})
  const [page, setPage] = useState({ current: 1, size: 30 })

  const query = useQuery({
    queryKey: ['open-token', filters, page],
    queryFn: () => systemExtApi.openTokenPage({ ...filters, ...page }),
  })
  const invalidate = () => void queryClient.invalidateQueries({ queryKey: ['open-token'] })

  const showPlain = (token?: string) => {
    if (token) setPlain(token)
    message.success('操作成功，明文 Token 仅展示这一次，请妥善保存')
  }

  const issueMutation = useMutation({
    mutationFn: (values: Record<string, unknown>) => systemExtApi.openTokenIssue(values),
    onSuccess: (text) => { showPlain(text); setOpen(false); invalidate() },
    onError: showError,
  })
  const rotateMutation = useMutation({
    mutationFn: (id: string) => systemExtApi.openTokenRotate(id),
    onSuccess: (text) => { showPlain(text); invalidate() },
    onError: showError,
  })
  const disableMutation = useMutation({
    mutationFn: (id: string) => systemExtApi.openTokenDisable(id),
    onSuccess: (text) => { message.success(text || '禁用成功'); invalidate() },
    onError: showError,
  })

  const columns: TableColumnsType<OpenTokenRow> = [
    { title: '应用ID', dataIndex: 'appId',
      render: (v: string) => <span className="ferry-mono" style={{ fontSize: 12 }}>{v}</span> },
    { title: '名称', dataIndex: 'name' },
    { title: 'Token 掩码', dataIndex: 'tokenMask',
      render: (v?: string) => <span className="ferry-mono" style={{ fontSize: 12 }}>{v || '—'}</span> },
    { title: '状态', dataIndex: 'status',
      render: (v: number) => <Tag color={v === 1 ? 'green' : 'red'}>{STATUS_LABEL[v] ?? v}</Tag> },
    { title: '最近轮换', dataIndex: 'lastRotateTime',
      render: (v?: string) => <span className="ferry-mono" style={{ fontSize: 12, color: 'var(--text-3)' }}>{v || '—'}</span> },
    { title: '创建时间', dataIndex: 'createTime',
      render: (v: string) => <span className="ferry-mono" style={{ fontSize: 12, color: 'var(--text-3)' }}>{v ?? '—'}</span> },
    { title: '操作', key: 'action',
      render: (_, record) => (
        <div style={{ display: 'flex', alignItems: 'center', gap: 2 }}>
          {record.status === 1 && can('system:openApi:rotate') && (
            <Tooltip title="轮换">
              <Button type="link" size="small" icon={<RefreshCw size={14} />}
                onClick={() => modal.confirm({
                  title: '轮换 Token？', content: '旧 Token 立即失效，仅内部在途调用保留 5 分钟宽限期。',
                  okText: '轮换', cancelText: '取消',
                  onOk: () => rotateMutation.mutateAsync(record.id),
                })} />
            </Tooltip>
          )}
          {record.status === 1 && can('system:openApi:disable') && (
            <Tooltip title="禁用">
              <Button type="link" size="small" danger icon={<ShieldOff size={14} />}
                onClick={() => modal.confirm({
                  title: '禁用 Token？', content: '禁用后该 Token 立即不可用于开放 API 调用。',
                  okText: '禁用', okButtonProps: { danger: true }, cancelText: '取消',
                  onOk: () => disableMutation.mutateAsync(record.id),
                })} />
            </Tooltip>
          )}
        </div>
      ),
    },
  ]

  const apiColumns: TableColumnsType<(typeof API_LIST)[number]> = [
    { title: '#', dataIndex: 'no', },
    { title: '方法', dataIndex: 'method',
      render: (v: string) => <Tag color={v === 'GET' ? 'green' : 'orange'}>{v}</Tag> },
    { title: '路径', dataIndex: 'path',
      render: (v: string) => <span className="ferry-mono" style={{ fontSize: 12 }}>{v}</span> },
    { title: '用途', dataIndex: 'usage' },
  ]

  return (
    <>
      <PageHeader title="开放 API 与 Token 管理" description="按应用签发 Token，权限继承应用数据权限、不得跨应用。默认配额 100 次/分钟，轮换后旧值立即失效。"
        tag={<RoleTag>共 {query.data?.total ?? 0} 个 Token</RoleTag>}
        extra={can('system:openApi:issue') && (
          <Button type="primary" icon={<Plus size={14} />} onClick={() => { form.resetFields(); setOpen(true) }}>签发 Token</Button>
        )}
      />

      <section className="ferry-panel">
        <div className="ferry-toolbar">
          <Input allowClear prefix={<Search size={15} />} placeholder="应用ID" style={{ width: 180 }}
            value={draft.appId as string}
            onChange={(e) => setDraft((prev) => ({ ...prev, appId: e.target.value }))}
            onPressEnter={() => { setFilters(draft); setPage((p) => ({ ...p, current: 1 })) }} />
          <Button type="primary" icon={<Search size={15} />}
            onClick={() => { setFilters(draft); setPage((p) => ({ ...p, current: 1 })) }}>查询</Button>
          <Button icon={<RotateCw size={15} />}
            onClick={() => { setDraft({}); setFilters({}); setPage((p) => ({ ...p, current: 1 })) }}>重置</Button>
        </div>
        <div className="ferry-table-wrap">
          <FerryTable<OpenTokenRow> rowKey="id" columns={columns} dataSource={query.data?.records ?? []}
            loading={query.isLoading || query.isFetching} scroll={{ x: 'max-content' }}
            pagination={{
              current: page.current, pageSize: page.size, total: query.data?.total ?? 0,
              showSizeChanger: true, pageSizeOptions: ['30', '100', '200'],
              showTotal: (total) => `共 ${total} 条`, onChange: (current, size) => setPage({ current, size }),
            }} />
        </div>
      </section>

      <section className="ferry-panel" style={{ marginTop: 16 }}>
        <div className="ferry-panel__head">
          <span className="ferry-panel__title">开放接口目录（8 个，触发运行接口已下线）</span>
          <span className="ferry-panel__extra"><span className="ferry-caption">鉴权与限流由网关负责</span></span>
        </div>
        <div className="ferry-table-wrap">
          <FerryTable rowKey="no" columns={apiColumns} dataSource={API_LIST} pagination={false} size="small" />
        </div>
      </section>

      {plain && (
        <Modal open title="请保存明文 Token" footer={null} onCancel={() => setPlain(null)} width={560}>
          <p style={{ color: 'var(--text-3)' }}>明文仅展示这一次，关闭后无法再次查看。请立即复制并妥善保存。</p>
          <div style={{ display: 'flex', gap: 8 }}>
            <Input readOnly value={plain} prefix={<KeyRound size={14} />} />
            <Button icon={<Copy size={14} />}
              onClick={() => { void navigator.clipboard.writeText(plain); message.success('已复制') }}>复制</Button>
          </div>
        </Modal>
      )}

      <Modal open={open} title="签发开放 API Token" okText="签发" cancelText="取消" width={520}
        confirmLoading={issueMutation.isPending} onCancel={() => setOpen(false)}
        onOk={async () => {
          const values = await form.validateFields()
          issueMutation.mutate(values)
        }} destroyOnHidden>
        <Form form={form} layout="vertical" requiredMark="optional" initialValues={{ quotaPerMinute: 100 }}>
          <Form.Item name="appId" label="绑定应用ID" rules={[{ required: true, message: '请输入应用ID' }]}>
            <Input placeholder="应用id" />
          </Form.Item>
          <Form.Item name="name" label="Token 名称">
            <Input placeholder="例如 CI 流水线调用" />
          </Form.Item>
          <Form.Item name="quotaPerMinute" label="每分钟配额" extra="默认 100 次/分钟，超限返回 429">
            <InputNumber min={1} max={10000} style={{ width: '100%' }} />
          </Form.Item>
        </Form>
      </Modal>
    </>
  )
}
