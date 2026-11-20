import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { App, Button, Descriptions, InputNumber, Space, Tag, Typography } from 'antd'
import FerryTable from '@/components/FerryTable'
import { ArrowLeft, Ban, CircleCheck, Play, ShieldAlert } from 'lucide-react'
import PageHeader from '@/components/PageHeader'
import { releaseApi, deployApi } from '@/api/devops'
import { useAuthStore } from '@/stores/auth'
import { showError } from '@/lib/notify'

const { Text } = Typography
const BATCH_STATUS: Record<number, { text: string; color: string }> = {
  1: { text: '未执行', color: 'default' }, 2: { text: '执行中', color: 'blue' },
  3: { text: '观察中', color: 'gold' }, 4: { text: '成功', color: 'green' },
  5: { text: '失败', color: 'red' }, 6: { text: '已回滚', color: 'volcano' }, 7: { text: '已取消', color: 'default' },
}

export default function ReleaseExecute({ id }: { id?: string }) {
  const { message, modal } = App.useApp()
  const queryClient = useQueryClient()
  const can = useAuthStore((s) => s.can)
  const rcId = id ?? new URLSearchParams(location.search).get('id') ?? ''
  const [confirmer, setConfirmer] = useState<number>()

  const detail = useQuery({ queryKey: ['release-detail', rcId], queryFn: () => releaseApi.detail(rcId), enabled: !!rcId })
  const timeline = useQuery({ queryKey: ['release-timeline', rcId], queryFn: () => releaseApi.timeline(rcId), enabled: !!rcId })
  const records = useQuery({ queryKey: ['release-records'], queryFn: () => releaseApi.recordPage({ current: 1, size: 50 }) })

  const invalidate = () => {
    void queryClient.invalidateQueries({ queryKey: ['release-detail', rcId] })
    void queryClient.invalidateQueries({ queryKey: ['release-records'] })
  }
  const startMutation = useMutation({
    mutationFn: () => releaseApi.start(rcId),
    onSuccess: () => { message.success('发布已启动'); invalidate() }, onError: showError,
  })
  const terminateMutation = useMutation({
    mutationFn: (body: Record<string, unknown>) => releaseApi.terminate(body),
    onSuccess: (t) => { message.success(t || '已终止'); invalidate() }, onError: showError,
  })
  const continueMutation = useMutation({
    mutationFn: (body: Record<string, unknown>) => deployApi.circuitContinue(body),
    onSuccess: (t) => { message.success(t || '已继续'); invalidate() }, onError: showError,
  })

  const d = (detail.data ?? {}) as Record<string, unknown>
  const artifacts = (d.artifacts ?? []) as Record<string, unknown>[]
  const rows = records.data?.records?.filter((r) => r.rcId === rcId) ?? []
  const observing = d.status === 5

  return (
    <>
      <PageHeader title="生产发布执行" description="应用×批次矩阵、熔断决策与终止。启动需生产权限和不同确认人。"
        tag={<Button type="link" icon={<ArrowLeft size={14} />} onClick={() => { location.href = '/release/list' }}>返回列表</Button>} />
      <section className="ferry-panel">
        <Descriptions bordered size="small" column={3}>
          <Descriptions.Item label="发布单">{String(d.rcNo ?? '—')}</Descriptions.Item>
          <Descriptions.Item label="标题">{String(d.title ?? '—')}</Descriptions.Item>
          <Descriptions.Item label="状态">{String(d.status ?? '—')}</Descriptions.Item>
          <Descriptions.Item label="窗口">{String(d.windowStart ?? '')} ~ {String(d.windowEnd ?? '')}</Descriptions.Item>
          <Descriptions.Item label="风险">{d.riskLevel === 3 ? <Tag color="red">高</Tag> : <Tag>低/中</Tag>}</Descriptions.Item>
          <Descriptions.Item label="确认人"><InputNumber size="small" value={confirmer} onChange={(v) => setConfirmer(v ?? undefined)} placeholder="用户id" /></Descriptions.Item>
        </Descriptions>
        <Space style={{ marginTop: 12 }}>
          {can('devops:release:start') && d.status === 3 && (
            <Button type="primary" icon={<Play size={14} />} loading={startMutation.isPending}
              onClick={() => modal.confirm({
                title: '启动生产发布？', content: '将校验封网期与窗口；确认人不得与启动人相同。',
                onOk: () => startMutation.mutate(),
              })}>启动发布</Button>
          )}
          {can('devops:release:start') && [4, 5].includes(d.status as number) && (
            <Button danger icon={<Ban size={14} />} onClick={() => modal.confirm({
              title: '终止发布？', content: '已发布批次保持当前版本，不触发回滚。',
              onOk: (): Promise<unknown> => terminateMutation.mutateAsync({ id: rcId, reason: '人工终止' }),
            })}>终止发布</Button>
          )}
          {can('devops:deploy:circuitContinue') && (
            <Button icon={<ShieldAlert size={14} />} onClick={() => modal.confirm({
              title: '熔断后继续发布？', content: '需填写理由并具备生产权限，将写审计。',
              onOk: (): Promise<unknown> => continueMutation.mutateAsync({ deployId: rows[0]?.id ?? '', reason: '熔断后人工确认继续' }),
            })}>熔断继续</Button>
          )}
        </Space>
        {observing && <div className="ferry-panel" style={{ marginTop: 12, borderLeft: '3px solid #1677ff' }}>
          <Space><CircleCheck color="#1677ff" size={16} /><Text>观察期中：回滚入口已置顶，倒计时结束后发布单自动完成。</Text></Space>
        </div>}
      </section>

      <section className="ferry-panel" style={{ marginTop: 12 }}>
        <h3>应用 × 批次矩阵</h3>
        <FerryTable size="small" rowKey="id" pagination={false} dataSource={artifacts}
          columns={[
            { title: '应用', dataIndex: 'appName', },
            { title: '制品版本', dataIndex: 'version', },
            { title: '门禁', dataIndex: 'gateConclusion',  render: (v: number) => <Tag color={v === 1 ? 'green' : 'gold'}>{v === 1 ? '通过' : '豁免'}</Tag> },
            { title: '锁定', dataIndex: 'isLocked',  render: (v: number) => v === 1 ? '已锁定' : '未锁' },
            { title: '发布批次', key: 'batches', render: () => (
              <Space size={4}>
                {[1, 2, 3].map((n) => <Tag key={n} color={BATCH_STATUS[n + 1]?.color}>批次{n}</Tag>)}
              </Space>
            ) },
          ]} />
      </section>

      <section className="ferry-panel" style={{ marginTop: 12 }}>
        <h3>时间线</h3>
        {(timeline.data ?? []).map((t) => (
          <div key={String(t.id)} style={{ fontSize: 13, padding: '4px 0', borderBottom: '1px solid var(--border-2)' }}>
            <Text type="secondary" className="ferry-mono" style={{ fontSize: 11 }}>{String(t.createTime)}</Text>{' '}
            <Text strong>{String(t.node)}</Text> · {String(t.remark ?? '')}
          </div>
        ))}
      </section>
    </>
  )
}
