import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { App, Button, Descriptions, Input, List, Select, Space, Table, Tag, Typography } from 'antd'
import { ArrowLeft, Check, ShieldAlert, X } from 'lucide-react'
import PageHeader from '@/components/PageHeader'
import { releaseApi } from '@/api/devops'
import { showError } from '@/lib/notify'

const { Text } = Typography
const CONCLUSION: Record<number, { text: string; color: string }> = {
  1: { text: '通过', color: 'green' }, 2: { text: '不通过', color: 'red' },
  3: { text: '不适用', color: 'default' }, 4: { text: '已知悉', color: 'gold' },
}

export default function ReleaseReview({ id }: { id?: string }) {
  const { message, modal } = App.useApp()
  const queryClient = useQueryClient()
  const rcId = id ?? new URLSearchParams(location.search).get('id') ?? ''
  const [opinion, setOpinion] = useState('')

  const detail = useQuery({ queryKey: ['release-detail', rcId], queryFn: () => releaseApi.detail(rcId), enabled: !!rcId })
  const impact = useQuery({ queryKey: ['release-impact', rcId], queryFn: () => releaseApi.impact(rcId), enabled: !!rcId })
  const risk = useQuery({ queryKey: ['release-risk', rcId], queryFn: () => releaseApi.risk(rcId), enabled: !!rcId })
  const checks = useQuery({ queryKey: ['release-checks', rcId], queryFn: () => releaseApi.checks(rcId), enabled: !!rcId })

  const invalidate = () => {
    void queryClient.invalidateQueries({ queryKey: ['release-detail', rcId] })
    void queryClient.invalidateQueries({ queryKey: ['release-checks', rcId] })
  }
  const approveMutation = useMutation({
    mutationFn: (body: Record<string, unknown>) => releaseApi.approve(body),
    onSuccess: (t) => { message.success(t || '审批完成'); invalidate() }, onError: showError,
  })
  const rejectMutation = useMutation({
    mutationFn: (body: Record<string, unknown>) => releaseApi.reject(body),
    onSuccess: (t) => { message.success(t || '已驳回'); invalidate() }, onError: showError,
  })
  const checkSave = useMutation({
    mutationFn: (body: Record<string, unknown>) => releaseApi.saveChecks(body),
    onSuccess: (t) => { message.success(t || '保存成功'); invalidate() }, onError: showError,
  })

  const d = (detail.data ?? {}) as Record<string, unknown>
  const riskVal = risk.data
  const highRisk = riskVal?.level === 3
  const checkRows = checks.data ?? []
  const blocked = checkRows.some((c) => (c.manualConclusion ?? c.systemConclusion) === 2)

  return (
    <>
      <PageHeader title="准入评审" description="影响面三层、五因子风险、六项检查与双人审批。创建人不可自批。"
        tag={<Button type="link" icon={<ArrowLeft size={14} />} onClick={() => { location.href = '/release/list' }}>返回列表</Button>} />
      {highRisk && <div className="ferry-panel" style={{ borderLeft: '3px solid #ff4d4f', marginBottom: 12 }}>
        <Space><ShieldAlert color="#ff4d4f" size={18} /><Text strong type="danger">高风险发布：需两级审批，回滚方案必填，审批期间风险评级锁定。</Text></Space>
      </div>}
      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12 }}>
        <section className="ferry-panel">
          <h3>基础信息</h3>
          <Descriptions column={1} size="small" bordered>
            <Descriptions.Item label="发布单号">{String(d.rcNo ?? '—')}</Descriptions.Item>
            <Descriptions.Item label="标题">{String(d.title ?? '—')}</Descriptions.Item>
            <Descriptions.Item label="风险等级">{riskVal ? <Tag color={highRisk ? 'red' : riskVal.level === 2 ? 'gold' : 'green'}>{riskVal.level === 3 ? '高' : riskVal.level === 2 ? '中' : '低'} · {String(riskVal.score)}</Tag> : '—'}</Descriptions.Item>
            <Descriptions.Item label="五因子明细"><Text code style={{ fontSize: 11 }}>{String(riskVal?.detail ?? '—')}</Text></Descriptions.Item>
          </Descriptions>
          <h3 style={{ marginTop: 16 }}>影响面（三层）</h3>
          <List size="small" dataSource={(impact.data?.layers ?? []) as Record<string, unknown>[]}
            renderItem={(layer) => <List.Item>
              <strong>第 {String(layer.layer)} 层 · {String(layer.name)}</strong>
              <div style={{ color: 'var(--text-3)', fontSize: 12 }}>{(layer.items as string[])?.join('、') || '—'} {layer.inferred ? `（${String(layer.inferred)}）` : ''}</div>
            </List.Item>} />
        </section>
        <section className="ferry-panel">
          <h3>六项准入检查</h3>
          <Table size="small" rowKey="id" pagination={false} dataSource={checkRows as Record<string, unknown>[]}
            columns={[
              { title: '检查项', dataIndex: 'checkName', width: 110 },
              { title: '系统', dataIndex: 'systemConclusion', width: 90, render: (v: number) => <Tag color={CONCLUSION[v]?.color}>{CONCLUSION[v]?.text ?? '—'}</Tag> },
              { title: '人工', dataIndex: 'manualConclusion', width: 90, render: (v?: number) => v ? <Tag color={CONCLUSION[v]?.color}>{CONCLUSION[v]?.text}</Tag> : '—' },
              { title: '人工结论', key: 'edit', width: 120, render: (_, c) => (
                <Select size="small" style={{ width: 110 }} placeholder="改结论" value={c.manualConclusion as number}
                  options={[3, 4].map((v) => ({ value: v, label: CONCLUSION[v].text }))}
                  onChange={(v) => checkSave.mutate({ itemId: c.id, conclusion: v, remark: String(c.remark ?? '') })} />
              ) },
            ]} />
          {blocked && <Text type="danger" style={{ fontSize: 12 }}>存在不通过项，审批按钮已禁用。</Text>}
          <h3 style={{ marginTop: 16 }}>审批操作</h3>
          <Input.TextArea rows={2} value={opinion} onChange={(e) => setOpinion(e.target.value)} placeholder="审批意见不少于 10 字" />
          <Space style={{ marginTop: 8 }}>
            <Button type="primary" icon={<Check size={14} />} disabled={blocked || opinion.length < 10}
              onClick={() => modal.confirm({ title: '审批通过？', content: opinion, onOk: () => approveMutation.mutate({ id: rcId, opinion }) })}>通过</Button>
            <Button danger icon={<X size={14} />} disabled={opinion.length < 10}
              onClick={() => modal.confirm({ title: '驳回发布单？', onOk: () => rejectMutation.mutate({ id: rcId, opinion, returnStage: '草稿' }) })}>驳回</Button>
          </Space>
        </section>
      </div>
    </>
  )
}
