import { useState, type ReactNode } from 'react'
import { useQuery } from '@tanstack/react-query'
import { App as AntApp, Button, Collapse, Drawer, Table, Tag, Tooltip, type TableColumnsType } from 'antd'
import { CheckCircle2, CircleAlert, Info, StopCircle } from 'lucide-react'
import { runApi } from '@/api/devops'
import type { RunStageRow } from '@/api/types'
import { showError } from '@/lib/notify'
import { formatDuration, runStatusLabel } from '@/lib/platform'

interface RunDetailDrawerProps {
  open: boolean
  runId: string | null
  onClose: () => void
  /** 取消运行后通知外部刷新 */
  onChanged: () => void
}

/** 阶段状态色：色块背景按状态取设计变量，与列表 Tag 色系一致 */
function stageBackground(status: string) {
  if (status === 'SUCCESS') return 'var(--color-success-bg)'
  if (status === 'FAILED') return 'var(--color-danger-bg)'
  if (status === 'BLOCKED') return 'var(--color-warning-bg)'
  if (status === 'RUNNING') return 'var(--color-primary-bg)'
  return 'var(--bg-subtle)'
}

/** 运行状态展示色，与列表口径一致 */
function statusColor(status: string) {
  if (status === 'SUCCESS') return 'success'
  if (status === 'FAILED' || status === 'TIMEOUT') return 'error'
  if (status === 'RUNNING') return 'processing'
  if (status === 'BLOCKED') return 'warning'
  return 'default'
}

/** 预校验结论图标 */
function conclusionIcon(conclusion: string) {
  if (conclusion === 'FAIL') {
    return <CircleAlert size={14} style={{ color: 'var(--color-danger)', flex: 'none' }} />
  }
  if (conclusion === 'NA') {
    return <Info size={14} style={{ color: 'var(--text-3)', flex: 'none' }} />
  }
  return <CheckCircle2 size={14} style={{ color: 'var(--color-success)', flex: 'none' }} />
}

/**
 * 运行详情抽屉：运行概要（信息条）→ 阶段横向时序（进行中每 3 秒轮询）→ 阶段任务明细。
 * 本期执行引擎未接入，运行停留在排队中，阶段状态全部为待执行。
 */
export default function RunDetailDrawer({ open, runId, onClose, onChanged }: RunDetailDrawerProps) {
  const { modal } = AntApp.useApp()
  const [selectedStage, setSelectedStage] = useState<string | null>(null)

  const detailQuery = useQuery({
    queryKey: ['run-detail', runId],
    queryFn: () => runApi.detail(runId as string),
    enabled: open && Boolean(runId),
    // 进行中的运行每 3 秒轮询刷新，终态停止轮询
    refetchInterval: (query) => {
      const status = query.state.data?.status
      return status === 'QUEUED' || status === 'RUNNING' ? 3000 : false
    },
  })

  const detail = detailQuery.data ?? null

  const stageColumns: TableColumnsType<RunStageRow['tasks'][number]> = [
    { title: '任务名称', dataIndex: 'name' },
    { title: '执行命令', dataIndex: 'command', ellipsis: true, render: (value?: string) => value || '—' },
    { title: '超时（分钟）', dataIndex: 'timeoutMinutes', width: 110 },
  ]

  const selected = detail?.stages.find((stage) => stage.id === selectedStage) ?? detail?.stages[0]

  const confirmCancel = () => {
    if (!runId) {
      return
    }
    modal.confirm({
      title: '取消运行？',
      content: '仅排队中的运行可以取消，取消后不进入执行队列。',
      okText: '取消运行',
      okButtonProps: { danger: true },
      cancelText: '返回',
      onOk: async () => {
        try {
          await runApi.cancel(runId)
          onChanged()
        } catch (error) {
          showError(error)
        }
      },
    })
  }

  const kv = (label: string, value: ReactNode) => (
    <div style={{ display: 'flex', gap: 8, fontSize: 13 }}>
      <span style={{ color: 'var(--text-3)', flex: 'none' }}>{label}</span>
      <span>{value}</span>
    </div>
  )

  return (
    <Drawer
      open={open}
      width={860}
      title={detail ? `运行详情 · RUN-${detail.id}` : '运行详情'}
      onClose={onClose}
      destroyOnHidden
      extra={
        detail?.status === 'QUEUED' && (
          <Button danger icon={<StopCircle size={14} />} onClick={confirmCancel}>
            取消运行
          </Button>
        )
      }
    >
      {detail && (
        <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
          <section style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '6px 24px' }}>
            {kv('应用', `${detail.appName}（${detail.appCode}）`)}
            {kv('流水线', `${detail.pipelineName} V${detail.pipelineVersionNo}`)}
            {kv('状态', <Tag color={statusColor(detail.status)}>{runStatusLabel(detail.status)}</Tag>)}
            {kv('来源', detail.reusedRunId ? `复用运行（RUN-${detail.reusedRunId}）` : '手动新建')}
            {kv('分支', detail.branch)}
            {kv('目标', <span className="ferry-mono" style={{ fontSize: 12 }}>{detail.commitId}</span>)}
            {kv('提交人', detail.commitAuthor || '—')}
            {kv('触发人', detail.triggerUserName || '—')}
            {kv('耗时', formatDuration(detail.startedTime, detail.finishedTime))}
            {kv('触发说明', detail.triggerNote || '—')}
          </section>

          <section>
            <div className="ferry-caption" style={{ marginBottom: 8 }}>阶段时序</div>
            <div style={{ display: 'flex', gap: 8, overflowX: 'auto', paddingBottom: 4 }}>
              {detail.stages.map((stage) => (
                <Tooltip key={stage.id} title={`${stage.stageName} · ${runStatusLabel(stage.status)}`}>
                  <div
                    onClick={() => setSelectedStage(stage.id)}
                    style={{
                      flex: '1 0 96px',
                      cursor: 'pointer',
                      borderRadius: 'var(--r-sm)',
                      border: `1px solid ${selected?.id === stage.id ? 'var(--color-primary-border)' : 'transparent'}`,
                      background: stageBackground(stage.status),
                      padding: '10px 10px 8px',
                    }}
                  >
                    <div style={{ fontSize: 13, fontWeight: 550, whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>
                      {stage.stageName}
                    </div>
                    <div style={{ fontSize: 12, color: 'var(--text-3)', marginTop: 4 }}>
                      {runStatusLabel(stage.status)}
                    </div>
                  </div>
                </Tooltip>
              ))}
            </div>
          </section>

          <section>
            <div className="ferry-caption" style={{ marginBottom: 8 }}>
              任务明细 · {selected?.stageName ?? ''}
            </div>
            <Table<RunStageRow['tasks'][number]>
              rowKey={(row) => `${row.name}-${row.timeoutMinutes}`}
              size="small"
              columns={stageColumns}
              dataSource={selected?.tasks ?? []}
              pagination={false}
              scroll={{ x: 'max-content' }}
            />
          </section>

          <Collapse
            ghost
            items={[
              {
                key: 'prechecks',
                label: '预校验结论（随运行落库）',
                children: (
                  <div style={{ display: 'flex', flexDirection: 'column', gap: 6 }}>
                    {detail.prechecks.map((item) => (
                      <div key={item.item} style={{ display: 'flex', alignItems: 'flex-start', gap: 6, fontSize: 13 }}>
                        {conclusionIcon(item.conclusion)}
                        <span>
                          <b>{item.itemName}</b>
                          <span style={{ color: 'var(--text-2)' }}>　{item.detail}</span>
                        </span>
                      </div>
                    ))}
                  </div>
                ),
              },
            ]}
          />
        </div>
      )}
    </Drawer>
  )
}
