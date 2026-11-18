import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { App, Button, Form, Input, InputNumber, Modal, Select, Space, Switch, Table, Tag, type TableColumnsType } from 'antd'
import { Eye, Play, RotateCw, Save } from 'lucide-react'
import PageHeader from '@/components/PageHeader'
import { RoleTag } from '@/components/StatusTag'
import { artifactApi, appApi } from '@/api/devops'
import { get, post } from '@/api/client'
import type { CleanTaskRow } from '@/api/types-devops'
import { useAuthStore } from '@/stores/auth'
import { showError } from '@/lib/notify'
import { COL } from '@/lib/table'

const TASK_STATUS: Record<number, string> = { 1: '待确认', 2: '已执行', 3: '失败' }

export default function ArtifactClean() {
  const { message, modal } = App.useApp()
  const queryClient = useQueryClient()
  const can = useAuthStore((state) => state.can)
  const [form] = Form.useForm()
  const [appId, setAppId] = useState<string | undefined>()
  const [open, setOpen] = useState(false)
  const [manifestText, setManifestText] = useState<string | null>(null)
  const manifestMutation = useMutation({
    mutationFn: (taskId: string) => get<string>(`/ferry-devops/artifactClean/task/manifest`, { taskId }),
    onSuccess: (text) => setManifestText(text),
    onError: showError,
  })

  const appsQuery = useQuery({ queryKey: ['app-options'], queryFn: () => appApi.options() })
  const policyQuery = useQuery({
    queryKey: ['clean-policy', appId],
    queryFn: () => artifactApi.policy(appId!),
    enabled: !!appId,
  })
  const runsQuery = useQuery({
    queryKey: ['clean-runs', appId],
    queryFn: () => artifactApi.cleanRuns(appId!),
    enabled: !!appId,
  })

  const saveMutation = useMutation({
    mutationFn: (values: Record<string, unknown>) => artifactApi.savePolicy(values),
    onSuccess: (text) => {
      message.success(text || '保存成功')
      setOpen(false)
      void queryClient.invalidateQueries({ queryKey: ['clean-policy'] })
    },
    onError: showError,
  })
  const runMutation = useMutation({
    mutationFn: (confirm: boolean) => post<string>(`/ferry-devops/artifactClean/run?appId=${appId}&confirm=${confirm}`),
    onSuccess: (text) => {
      message.success(text || '清理任务已提交')
      void queryClient.invalidateQueries({ queryKey: ['clean-runs'] })
    },
    onError: showError,
  })

  const openEditor = () => {
    form.resetFields()
    const p = policyQuery.data
    form.setFieldsValue({
      appId, keepLatest: p?.keepLatest ?? 20, isKeepProd: p?.isKeepProd ?? 1,
      softDeleteDays: p?.softDeleteDays ?? 30, cron: p?.cron ?? '0 30 2 * * ?', isAutoExec: p?.isAutoExec ?? 0,
    })
    setOpen(true)
  }

  const columns: TableColumnsType<CleanTaskRow> = [
    { title: '任务ID', dataIndex: 'id', width: 140, render: (v: string) => <span className="ferry-mono" style={{ fontSize: 12 }}>{v}</span> },
    { title: '任务时间', dataIndex: 'taskTime', width: COL.time },
    { title: '待清理数', dataIndex: 'pendingCount', width: 100 },
    { title: '实际清理数', dataIndex: 'deletedCount', width: 110 },
    { title: '状态', dataIndex: 'status', width: 100,
      render: (v: number) => <Tag color={v === 2 ? 'green' : v === 3 ? 'red' : 'orange'}>{TASK_STATUS[v] ?? v}</Tag>,
    },
    { title: '操作', key: 'action', width: COL.action, fixed: 'right', render: (_, r) => (
      <Button type="link" size="small" icon={<Eye size={14} />} onClick={() => manifestMutation.mutate(r.id)}>清单</Button>
    ) },
  ]

  return (
    <>
      <PageHeader title="制品清理策略"
        description="保留集合＝最近 N 个 ∪ 生产用过的版本。清理为软删除，30 天内可恢复；物理删除由独立任务到期后执行。"
        tag={<RoleTag>应用级策略</RoleTag>}
      />
      <section className="ferry-panel">
        <div className="ferry-toolbar">
          <Select allowClear showSearch placeholder="选择应用" style={{ width: 220 }} value={appId} onChange={setAppId}
            options={(appsQuery.data ?? []).map((a) => ({ value: a.value, label: a.label }))} />
          {can('devops:artifactClean:save') && (
            <Button type="primary" icon={<Save size={14} />} onClick={openEditor} disabled={!appId}>配置策略</Button>
          )}
          <Button icon={<RotateCw size={14} />} onClick={() => void queryClient.invalidateQueries({ queryKey: ['clean-runs'] })}>刷新</Button>
        </div>

        {appId && (
          <div className="ferry-toolbar" style={{ flexWrap: 'wrap', gap: 16 }}>
            <span>保留最近 <b>{policyQuery.data?.keepLatest ?? 20}</b> 个</span>
            <span>生产保护：<b>{policyQuery.data?.isKeepProd === 1 ? '开启' : '关闭'}</b></span>
            <span>软删保留 <b>{policyQuery.data?.softDeleteDays ?? 30}</b> 天</span>
            <span>自动执行：<b>{policyQuery.data?.isAutoExec === 1 ? '开启' : '关闭'}</b></span>
            <Space>
              <Button size="small" icon={<Eye size={14} />}
                onClick={() => runMutation.mutate(false)} disabled={!can('devops:artifactClean:run')}>生成清单</Button>
              <Button size="small" danger icon={<Play size={14} />}
                onClick={() => modal.confirm({ title: '执行清理？', content: '将按清单软删除制品，30 天内可恢复。',
                  okText: '执行', okButtonProps: { danger: true }, cancelText: '取消', onOk: () => runMutation.mutateAsync(true) })}
                disabled={!can('devops:artifactClean:run')}>执行清理</Button>
            </Space>
          </div>
        )}

        <div className="ferry-table-wrap">
          <Table<CleanTaskRow> rowKey="id" columns={columns} dataSource={runsQuery.data ?? []}
            loading={runsQuery.isFetching} scroll={{ x: 'max-content' }} pagination={false} />
        </div>
      </section>

      <Modal open={open} title="清理策略配置" okText="保存" cancelText="取消" width={520}
        confirmLoading={saveMutation.isPending} onCancel={() => setOpen(false)}
        onOk={async () => {
          const values = await form.validateFields()
          saveMutation.mutate({ ...values, isKeepProd: values.isKeepProd ? 1 : 0, isAutoExec: values.isAutoExec ? 1 : 0 })
        }} destroyOnHidden>
        <Form form={form} layout="vertical" requiredMark="optional">
          <Form.Item name="appId" label="应用id"><Input disabled /></Form.Item>
          <Form.Item name="keepLatest" label="保留最近 N 个版本" rules={[{ required: true }]}>
            <InputNumber min={1} style={{ width: '100%' }} />
          </Form.Item>
          <Form.Item name="isKeepProd" label="永久保留生产用过的版本" valuePropName="checked">
            <Switch />
          </Form.Item>
          <Form.Item name="softDeleteDays" label="软删除保留天数" rules={[{ required: true }]}>
            <InputNumber min={1} style={{ width: '100%' }} addonAfter="天" />
          </Form.Item>
          <Form.Item name="cron" label="执行时间（cron）"><Input placeholder="0 30 2 * * ?" /></Form.Item>
          <Form.Item name="isAutoExec" label="自动执行（免确认）" valuePropName="checked">
            <Switch />
          </Form.Item>
        </Form>
      </Modal>

      <Modal open={!!manifestText} title="待清理清单" footer={null} width={720} onCancel={() => setManifestText(null)} destroyOnHidden>
        <pre style={{ whiteSpace: 'pre-wrap', fontSize: 12 }}>{manifestText}</pre>
      </Modal>
    </>
  )
}
