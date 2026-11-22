import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { App, Button, Form, Input, Modal, Select, Space, Statistic, Tag } from 'antd'
import { RotateCw, RotateCcw } from 'lucide-react'
import PageHeader from '@/components/PageHeader'
import { RoleTag } from '@/components/StatusTag'
import { gateApi, appApi } from '@/api/devops'
import { useAuthStore } from '@/stores/auth'
import { showError } from '@/lib/notify'

/** 后端 BaselineVO 的扩展字段（types-devops 的 GateBaseline 为精简契约） */
interface BaselineDetail {
  id: string
  appId: string
  tool: string
  totalCount: number
  isActive: number
  createTime: string
  ruleVersion?: string
  fingerprintVersion: string
  newCount?: number
  fixedCount?: number
  legacyCount?: number
  pendingResetIds?: string[]
  daysSinceReset?: number
}

export default function GateBaselinePage() {
  const { message } = App.useApp()
  const queryClient = useQueryClient()
  const can = useAuthStore((state) => state.can)
  const [form] = Form.useForm()
  const [appId, setAppId] = useState<string | undefined>()
  const [open, setOpen] = useState(false)
  const [approveOpen, setApproveOpen] = useState(false)
  const [applyId, setApplyId] = useState<string | undefined>()

  const appsQuery = useQuery({ queryKey: ['app-options'], queryFn: () => appApi.options() })
  const query = useQuery({
    queryKey: ['baseline', appId],
    queryFn: async () => await gateApi.baselineDetail(appId!) as unknown as BaselineDetail,
    enabled: !!appId,
  })

  const resetMutation = useMutation({
    mutationFn: (values: Record<string, unknown>) => gateApi.baselineReset(values),
    onSuccess: (text) => {
      message.success(text || '申请已提交')
      setOpen(false)
      void queryClient.invalidateQueries({ queryKey: ['baseline'] })
    },
    onError: showError,
  })
  const approveMutation = useMutation({
    mutationFn: (values: Record<string, unknown>) => gateApi.baselineApprove(values),
    onSuccess: (text) => {
      message.success(text || '审批完成')
      setApproveOpen(false)
      void queryClient.invalidateQueries({ queryKey: ['baseline'] })
    },
    onError: showError,
  })

  const data = query.data
  const pending = data?.pendingResetIds?.[0]

  return (
    <>
      <PageHeader title="存量问题基线"
        description="首次扫描自动生成指纹快照，仅不在基线的问题计新增。重设基线需审批，审批通过后旧快照保留可查。"
        tag={<RoleTag>应用基线</RoleTag>}
        extra={<Button icon={<RotateCw size={14} />} onClick={() => void queryClient.invalidateQueries({ queryKey: ['baseline'] })}>刷新</Button>}
      />
      <section className="ferry-panel">
        <div className="ferry-toolbar">
          <Select allowClear showSearch placeholder="选择应用" style={{ width: 240 }} value={appId} onChange={setAppId}
            options={(appsQuery.data ?? []).map((a) => ({ value: a.value, label: a.label }))} />
          {can('devops:baseline:reset') && (
            <Button type="primary" icon={<RotateCcw size={14} />} onClick={() => { form.resetFields(); setOpen(true) }} disabled={!appId}>重设基线</Button>
          )}
        </div>

        {data ? (
          <div style={{ padding: 16 }}>
            {data.daysSinceReset != null && data.daysSinceReset <= 30 && (
              <div style={{ padding: 10, background: '#fffbe6', color: '#ad6800', borderRadius: 4, marginBottom: 16 }}>
                基线于 {data.daysSinceReset} 日前重设，存量新增问题数已按新基线计算。
              </div>
            )}
            <Space size={48} wrap>
              <Statistic title="问题总数" value={data.totalCount ?? 0} />
              <Statistic title="新增" value={data.newCount ?? 0} valueStyle={{ color: '#cf1322' }} />
              <Statistic title="遗留" value={data.legacyCount ?? 0} />
              <Statistic title="已修复" value={data.fixedCount ?? 0} valueStyle={{ color: '#3f8600' }} />
              <Statistic title="指纹版本" value={data.fingerprintVersion ?? '—'} />
            </Space>
            <div style={{ marginTop: 16, lineHeight: 2 }}>
              <div>扫描工具：<b>{data.tool}</b></div>
              <div>规则版本：{data.ruleVersion ?? '—'}</div>
              <div>状态：<Tag color={data.isActive === 1 ? 'green' : 'default'}>{data.isActive === 1 ? '生效中' : '已失效'}</Tag></div>
              <div>创建时间：{data.createTime ?? '—'}</div>
            </div>
            {pending && (
              <div style={{ marginTop: 16, padding: 12, background: '#f0f5ff', borderRadius: 4 }}>
                <span>存在待审批的重设申请（#{pending}）</span>
                {can('devops:baseline:approve') && (
                  <Button size="small" type="primary" style={{ marginLeft: 12 }}
                    onClick={() => { setApplyId(pending); setApproveOpen(true) }}>审批</Button>
                )}
              </div>
            )}
          </div>
        ) : <div style={{ padding: 16, color: 'var(--text-3)' }}>{appId ? '该应用暂无基线' : '请选择应用查看基线'}</div>}
      </section>

      <Modal open={open} title="重设基线" okText="提交申请" cancelText="取消" width={480}
        confirmLoading={resetMutation.isPending} onCancel={() => setOpen(false)}
        onOk={async () => {
          const values = await form.validateFields()
          resetMutation.mutate({ ...values, appId })
        }} destroyOnHidden>
        <Form form={form} layout="vertical" requiredMark="optional">
          <Form.Item name="reason" label="重设理由" rules={[{ required: true, message: '请填写重设理由' }]}>
            <Input.TextArea rows={4} placeholder="说明为何以当次扫描结果作为新基线" />
          </Form.Item>
          <Form.Item name="approverId" label="审批人" rules={[{ required: true, message: '请选择审批人' }]}>
            <Select showSearch options={(appsQuery.data ?? []).map((a) => ({ value: a.value, label: a.label }))} placeholder="选择审批人" />
          </Form.Item>
        </Form>
      </Modal>

      <Modal open={approveOpen} title="审批基线重设" footer={null} width={420} onCancel={() => setApproveOpen(false)} destroyOnHidden>
        <Form layout="vertical">
          <Form.Item name="opinion" label="审批意见"><Input.TextArea rows={3} /></Form.Item>
          <Space>
            <Button type="primary" loading={approveMutation.isPending}
              onClick={() => approveMutation.mutate({ applyId, pass: true })}>通过</Button>
            <Button danger loading={approveMutation.isPending}
              onClick={() => approveMutation.mutate({ applyId, pass: false })}>驳回</Button>
          </Space>
        </Form>
      </Modal>
    </>
  )
}
