import { useEffect, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useNavigate, useParams } from 'react-router-dom'
import {
  App, Button, Descriptions, Input, InputNumber, Modal, Popover, Space, Steps, Table, Tag, Tooltip,
  type TableColumnsType,
} from 'antd'
import { CirclePause, CirclePlay, OctagonX, RotateCcw } from 'lucide-react'
import PageHeader from '@/components/PageHeader'
import { gateApi, runApi } from '@/api/devops'
import type { RunStageGate, RunDetail, Run } from '@/api/types-devops'
import { useAuthStore } from '@/stores/auth'
import { showError } from '@/lib/notify'

const RUN_STATUS: Record<number, string> = {
  0: '待确认', 1: '排队中', 2: '运行中', 3: '阻塞', 4: '成功', 5: '失败', 6: '超时', 7: '已取消', 8: '预检未通过',
}
const STAGE_STATUS: Record<number, string> = {
  1: '未开始', 2: '进行中', 3: '成功', 4: '失败', 5: '阻塞', 6: '跳过', 7: '超时', 8: '已复用', 9: '已取消', 10: '待门禁',
}
const STATUS_COLOR: Record<number, string> = {
  0: 'purple', 1: 'blue', 2: 'processing', 3: 'orange', 4: 'green', 5: 'red', 6: 'red', 7: 'default', 8: 'red',
}
/** 阶段/任务状态色：与运行状态语义不同，1 未开始应为灰色而非蓝色 */
const STAGE_STATUS_COLOR: Record<number, string> = {
  1: 'default', 2: 'processing', 3: 'green', 4: 'red', 5: 'orange', 6: 'default', 7: 'red', 8: 'purple', 9: 'default', 10: 'purple',
}
const GATE_STATUS: Record<number, string> = {
  1: '通过', 2: '不通过', 3: '不适用', 4: '已豁免', 5: '数据获取失败',
}
const GATE_STATUS_COLOR: Record<number, string> = {
  1: 'green', 2: 'red', 3: 'default', 4: 'gold', 5: 'volcano',
}
const RULE_TYPE: Record<number, string> = {
  1: '单元测试通过率', 2: '行覆盖率', 3: '分支覆盖率', 4: '阻断级漏洞数', 5: '高危依赖漏洞数', 6: '许可证黑名单',
}

/** 阶段状态映射到 AntD Steps 状态 */
function stepStatus(value: number): 'wait' | 'process' | 'finish' | 'error' {
  if (value === 2 || value === 10) return 'process'
  if (value === 3 || value === 8) return 'finish'
  if (value === 4 || value === 5 || value === 7) return 'error'
  return 'wait'
}
function elapsedSeconds(start?: string, end?: string): number | null {
  if (!start || !end) return null
  const value = (new Date(end).getTime() - new Date(start).getTime()) / 1000
  return Number.isFinite(value) && value >= 0 ? value : null
}
/** 预检阶段的 result 是 PrecheckItemVO[]：type/conclusion/detail */
function parsePrecheckItems(result?: string): { type: number; conclusion: number; detail?: string }[] {
  if (!result) return []
  try {
    const parsed = JSON.parse(result)
    return Array.isArray(parsed) ? parsed : []
  } catch {
    return []
  }
}
const PRECHECK_CONCLUSION: Record<number, { label: string; color: string }> = {
  1: { label: '通过', color: 'green' },
  2: { label: '不通过', color: 'red' },
  3: { label: '合并', color: 'blue' },
}
interface StageExt {
  id: string
  runId: string
  type: number
  name: string
  status: number
  result?: string
  sort: number
  startTime?: string
  endTime?: string
  tasks?: TaskExt[]
}
interface TaskExt {
  id: string
  runId: string
  runStageId: string
  name: string
  executor?: string
  scriptRef?: string
  status: number
  result?: string
  sort: number
  startTime?: string
  endTime?: string
}
function runIdFromHash(): string | null {
  const m = window.location.hash.match(/\/run\/(\d+)/)
  return m ? m[1] : null
}

export default function RunDetail() {
  const { message } = App.useApp()
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const can = useAuthStore((state) => state.can)
  const routeRunId = useParams().id
  const [runId, setRunId] = useState<string | null>(() => routeRunId ?? runIdFromHash())
  const [inputId, setInputId] = useState('')
  const [runPage, setRunPage] = useState({ current: 1, size: 20 })
  const [abortOpen, setAbortOpen] = useState(false)
  const [abortReason, setAbortReason] = useState('')
  const [rerunOpen, setRerunOpen] = useState(false)
  const [exemptTarget, setExemptTarget] = useState<RunStageGate | null>(null)
  const [exemptReason, setExemptReason] = useState('')
  const [exemptHours, setExemptHours] = useState(24)

  useEffect(() => {
    setRunId(routeRunId ?? null)
    if (routeRunId) setInputId(routeRunId)
  }, [routeRunId])

  const listQuery = useQuery({
    queryKey: ['run-list', runPage],
    queryFn: () => runApi.pageList({ ...runPage }),
    enabled: !runId,
    refetchInterval: 5000,
  })

  const detailQuery = useQuery({
    queryKey: ['run-detail', runId],
    queryFn: () => runApi.detail(runId!),
    enabled: !!runId,
    refetchInterval: (q) => {
      const status = (q.state.data as RunDetail | undefined)?.run?.status
      // 1排队 2运行中 3阻塞为活跃态；8预检未通过是终态，不再轮询
      return status === 1 || status === 2 || status === 3 ? 5000 : false
    },
  })
  const rerunPlanQuery = useQuery({
    queryKey: ['rerun-plan', runId],
    queryFn: () => runApi.rerunPlan(runId!),
    enabled: rerunOpen && !!runId,
  })
  const gateQuery = useQuery({
    queryKey: ['run-gate', runId],
    queryFn: () => gateApi.resultList(runId!),
    enabled: !!runId,
    refetchInterval: () => {
      const status = detailQuery.data?.run?.status
      return status === 1 || status === 2 || status === 3 ? 5000 : false
    },
  })

  const invalidate = () => {
    void queryClient.invalidateQueries({ queryKey: ['run-detail'] })
    void queryClient.invalidateQueries({ queryKey: ['run-gate'] })
  }
  const pauseMutation = useMutation({
    mutationFn: (body: Record<string, unknown>) => runApi.pause(body),
    onSuccess: (t) => { message.success(t || '已暂停'); invalidate() }, onError: showError,
  })
  const resumeMutation = useMutation({
    mutationFn: (id: string) => runApi.resume(id),
    onSuccess: (t) => { message.success(t || '已继续'); invalidate() }, onError: showError,
  })
  const abortMutation = useMutation({
    mutationFn: (body: Record<string, unknown>) => runApi.abort(body),
    onSuccess: (t) => { message.success(t || '已中止'); setAbortOpen(false); invalidate() }, onError: showError,
  })
  const rerunMutation = useMutation({
    mutationFn: (body: Record<string, unknown>) => runApi.rerun(body),
    onSuccess: () => { message.success('断点重跑已创建'); setRerunOpen(false); invalidate() }, onError: showError,
  })
  const exemptMutation = useMutation({
    mutationFn: () => gateApi.exempt({
      resultId: exemptTarget!.id,
      reason: exemptReason,
      hours: exemptHours,
    }),
    onSuccess: () => {
      message.success('豁免已生效')
      setExemptTarget(null)
      setExemptReason('')
      invalidate()
    },
    onError: showError,
  })

  const detail = detailQuery.data
  const run = detail?.run
  const stages = ((detail?.stages ?? []) as unknown as StageExt[])
  const tasks = (detail?.tasks ?? []) as unknown as TaskExt[]
  const gateResults = (gateQuery.data ?? []) as unknown as RunStageGate[]
  const stageNameById = new Map(stages.map((stage) => [String(stage.id), stage.name]))

  const taskColumns: TableColumnsType<TaskExt> = [
    { title: '任务', dataIndex: 'name', width: 320, ellipsis: true },
    { title: '所属阶段', dataIndex: 'runStageId', width: 160, ellipsis: true,
      render: (v: string) => stageNameById.get(String(v)) ?? '—' },
    { title: '执行器', dataIndex: 'executor', width: 120, render: (v?: string) => v || '—' },
    { title: '脚本引用', dataIndex: 'scriptRef', width: 200, ellipsis: true, render: (v?: string) => v || '—' },
    { title: '状态', dataIndex: 'status', width: 110, align: 'center',
      render: (v: number) => <Tag color={STAGE_STATUS_COLOR[v] ?? 'default'}>{STAGE_STATUS[v] ?? v}</Tag> },
  ]

  const gateColumns: TableColumnsType<RunStageGate> = [
    { title: '规则类型', dataIndex: 'ruleType', width: 150,
      render: (v: number) => RULE_TYPE[v] ?? v },
    { title: '状态', dataIndex: 'status', width: 120, align: 'center',
      render: (v: number) => <Tag color={GATE_STATUS_COLOR[v] ?? 'default'}>{GATE_STATUS[v] ?? v}</Tag> },
    { title: '当前值', dataIndex: 'currentValue', width: 120, render: (v?: string) => v ?? '—' },
    { title: '阈值', dataIndex: 'threshold', width: 120, render: (v?: string) => v ?? '—' },
    { title: '来源', dataIndex: 'source', ellipsis: true, render: (v?: string) => v || '—' },
    { title: '原因', dataIndex: 'fetchError', ellipsis: true, render: (v?: string) => v || '—' },
    { title: '操作', width: 100, align: 'center',
      render: (_: unknown, r) => r.status === 2 && can('devops:gateExemption:apply') && (
        <Button type="link" size="small" onClick={() => setExemptTarget(r)}>申请豁免</Button>
      ) },
  ]

  const status = Number(run?.status ?? 0)
  const canRerun = status === 5 || status === 6
  const finishedStages = stages.filter((stage) => [3, 4, 6, 7, 8, 9].includes(stage.status)).length
  const totalDuration = elapsedSeconds(run?.startTime, run?.endTime)

  const openRun = (id: string) => {
    const next = id.trim()
    if (!next) return
    if (!/^\d+$/.test(next)) {
      message.warning('运行 ID 需为数字')
      return
    }
    setRunId(next)
    navigate(`/pipeline/run/${next}`)
  }

  const backToList = () => {
    setRunId(null)
    navigate('/pipeline/run')
  }

  const runColumns: TableColumnsType<Run> = [
    { title: '运行编号', dataIndex: 'code', width: 180,
      render: (v: string) => <span className="ferry-mono" style={{ fontSize: 12 }}>{v}</span> },
    { title: '应用', dataIndex: 'appName', width: 120, ellipsis: true, render: (v?: string) => v || '—' },
    { title: '流水线', dataIndex: 'pipelineName', ellipsis: true, render: (v?: string) => v || '—' },
    { title: '分支', dataIndex: 'branch', width: 120, ellipsis: true,
      render: (v?: string) => v ? <span className="ferry-mono" style={{ fontSize: 12 }}>{v}</span> : '—' },
    { title: '引用值', dataIndex: 'refValue', width: 120,
      render: (v?: string) => v
        ? <Tooltip title={v}><span className="ferry-mono" style={{ fontSize: 12 }}>{v.slice(0, 8)}</span></Tooltip>
        : '—' },
    { title: '状态', dataIndex: 'status', width: 110, align: 'center',
      render: (v: number) => <Tag color={STATUS_COLOR[v] ?? 'default'}>{RUN_STATUS[v] ?? v}</Tag> },
    { title: '触发时间', dataIndex: 'createTime', width: 170 },
    { title: '操作', width: 80, align: 'center',
      render: (_: unknown, record: Run) => (
        <Button type="link" size="small" onClick={() => openRun(String(record.id))}>查看</Button>
      ) },
  ]

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
      <PageHeader
        title={runId ? '运行详情' : '运行记录'}
        tag={runId && run?.code ? <Tag className="ferry-mono">{String(run.code)}</Tag> : undefined}
        description={runId
          ? '运行概要、阶段进度、任务与门禁结果；支持暂停、继续、中止与断点重跑'
          : '最近运行列表，每 5 秒刷新；点击查看进入详情，也可输入运行 ID 快速查询'}
        extra={
          <Space>
            <Input placeholder="运行 ID" value={inputId} onChange={(e) => setInputId(e.target.value)}
              onPressEnter={() => openRun(inputId)} style={{ width: 180 }} />
            <Button onClick={() => openRun(inputId)}>查询</Button>
            {runId && <Button onClick={backToList}>返回列表</Button>}
            {can('devops:run:pause') && (status === 1 || status === 2) && (
              <Button icon={<CirclePause size={14} />} onClick={() => pauseMutation.mutate({ id: runId, reason: '页面暂停' })}>暂停</Button>
            )}
            {can('devops:run:resume') && status === 3 && (
              <Button icon={<CirclePlay size={14} />} onClick={() => resumeMutation.mutate(runId!)}>继续</Button>
            )}
            {can('devops:run:abort') && (status === 1 || status === 2) && (
              <Button danger icon={<OctagonX size={14} />} onClick={() => setAbortOpen(true)}>中止</Button>
            )}
            {can('devops:run:rerun') && canRerun && (
              <Button icon={<RotateCcw size={14} />} onClick={() => setRerunOpen(true)}>断点重跑</Button>
            )}
          </Space>
        }
      />

      {!runId && (
        <div className="ferry-panel">
          <Table<Run>
            rowKey="id"
            tableLayout="fixed"
            size="small"
            columns={runColumns}
            dataSource={listQuery.data?.records ?? []}
            loading={listQuery.isLoading || listQuery.isFetching}
            scroll={{ x: 1040 }}
            pagination={{
              current: runPage.current,
              pageSize: runPage.size,
              total: listQuery.data?.total ?? 0,
              showSizeChanger: true,
              onChange: (current, size) => setRunPage({ current, size }),
            }}
          />
        </div>
      )}

      {runId && detailQuery.isError && (
        <div className="ferry-panel">
          <div style={{ padding: 24 }}>
            <div style={{ marginBottom: 12 }}>运行详情加载失败：{detailQuery.error?.message || '无权限或运行不存在'}</div>
            <Space>
              <Button onClick={() => void detailQuery.refetch()}>重试</Button>
              <Button onClick={backToList}>返回列表</Button>
            </Space>
          </div>
        </div>
      )}

      {runId && detail && (
        <>
          <div className="ferry-panel">
            <div className="ferry-panel__head">
              <span className="ferry-panel__title">运行概要</span>
              <span className="ferry-panel__extra">
                <Tag color={STATUS_COLOR[status] ?? 'default'}>{RUN_STATUS[status] ?? status}</Tag>
              </span>
            </div>
            <div style={{ padding: '14px 18px' }}>
              <Descriptions column={{ xs: 1, sm: 2, lg: 4 }} size="small" colon={false}
                labelStyle={{ color: 'var(--text-3)', width: 84 }}>
                <Descriptions.Item label="应用">{String(run?.appName ?? '—')}</Descriptions.Item>
                <Descriptions.Item label="流水线">{String(run?.pipelineName ?? '—')}</Descriptions.Item>
                <Descriptions.Item label="版本">{run?.pipelineVersionNo ? `V${run.pipelineVersionNo}` : '—'}</Descriptions.Item>
                <Descriptions.Item label="触发人">{String(run?.triggerUser ?? '—')}</Descriptions.Item>
                <Descriptions.Item label="分支">{String(run?.branch ?? '—')}</Descriptions.Item>
                <Descriptions.Item label="引用值" span={2}>
                  <Tooltip title={run?.refValue}>
                    <span className="ferry-mono">{String(run?.refValue ?? '—')}</span>
                  </Tooltip>
                </Descriptions.Item>
                <Descriptions.Item label="开始时间">{String(run?.startTime ?? '—')}</Descriptions.Item>
                <Descriptions.Item label="结束时间">{String(run?.endTime ?? '—')}</Descriptions.Item>
                <Descriptions.Item label="总耗时">
                  {totalDuration != null ? `${totalDuration.toFixed(1)}s` : '—'}
                </Descriptions.Item>
                <Descriptions.Item label="预检">
                  {run?.status === 8
                    ? <Tag color="red">未通过</Tag>
                    : run?.preCheckResult
                      ? <Tooltip title={run.preCheckResult}><Tag color="green">通过</Tag></Tooltip>
                      : '—'}
                </Descriptions.Item>
              </Descriptions>
            </div>
          </div>

          <div className="ferry-panel">
            <div className="ferry-panel__head">
              <span className="ferry-panel__title">阶段进度</span>
              <span className="ferry-panel__extra ferry-caption">{finishedStages}/{stages.length} 已结束</span>
            </div>
            <div style={{ padding: '18px 20px 8px' }}>
              <Steps
                size="small"
                labelPlacement="vertical"
                current={stages.findIndex((stage) => stage.status === 2 || stage.status === 10)}
                items={stages.map((stage) => {
                  const duration = elapsedSeconds(stage.startTime, stage.endTime)
                  const precheckItems = stage.type === 0 ? parsePrecheckItems(stage.result) : []
                  const description = (
                    <span>
                      {STAGE_STATUS[stage.status] ?? stage.status}
                      {duration != null && ` · ${duration.toFixed(1)}s`}
                      {stage.status === 8 && <Tag color="purple" style={{ marginLeft: 4 }}>复用</Tag>}
                    </span>
                  )
                  return {
                    title: stage.name,
                    status: stepStatus(stage.status),
                    description: precheckItems.length ? (
                      <Popover
                        placement="bottom"
                        content={
                          <div style={{ minWidth: 260 }}>
                            {precheckItems.map((item) => (
                              <div key={item.type} style={{ display: 'flex', gap: 8, marginBottom: 4 }}>
                                <Tag color={PRECHECK_CONCLUSION[item.conclusion]?.color ?? 'default'}>
                                  {PRECHECK_CONCLUSION[item.conclusion]?.label ?? item.conclusion}
                                </Tag>
                                <span>{item.detail}</span>
                              </div>
                            ))}
                          </div>
                        }
                      >
                        <span style={{ cursor: 'pointer', color: 'var(--color-link, #1677ff)' }}>
                          {description} · 查看明细
                        </span>
                      </Popover>
                    ) : description,
                  }
                })}
              />
            </div>
          </div>

          <div className="ferry-panel">
            <div className="ferry-panel__head">
              <span className="ferry-panel__title">任务列表</span>
            </div>
            <Table<TaskExt>
              rowKey="id"
              tableLayout="fixed"
              size="small"
              columns={taskColumns}
              dataSource={tasks}
              pagination={false}
              scroll={{ x: 900 }}
            />
          </div>

          <div className="ferry-panel">
            <div className="ferry-panel__head">
              <span className="ferry-panel__title">门禁结果</span>
            </div>
            <Table<RunStageGate>
              rowKey="id"
              tableLayout="fixed"
              size="small"
              columns={gateColumns}
              dataSource={gateResults}
              pagination={false}
              scroll={{ x: 1000 }}
              locale={{ emptyText: '暂无门禁结果' }}
            />
          </div>
        </>
      )}

      <Modal open={abortOpen} title="中止运行" onCancel={() => setAbortOpen(false)}
        onOk={() => {
          if (abortReason.trim().length < 10) {
            message.warning('中止原因不少于 10 字')
            return
          }
          abortMutation.mutate({ id: runId, reason: abortReason })
        }} okText="确认中止"
        okButtonProps={{ danger: true, disabled: abortReason.trim().length < 10 }} confirmLoading={abortMutation.isPending}>
        <div style={{ color: 'var(--text-2)', marginBottom: 8 }}>中止后不可恢复，已产出制品保留不删除。请输入中止原因（不少于 10 字）：</div>
        <Input.TextArea rows={3} value={abortReason} onChange={(e) => setAbortReason(e.target.value)} />
      </Modal>

      <Modal open={rerunOpen} title="断点重跑计划" onCancel={() => setRerunOpen(false)}
        onOk={() => rerunMutation.mutate({ id: runId })} okText="确认重跑"
        confirmLoading={rerunMutation.isPending} width={640}>
        <div style={{ color: 'var(--text-2)', marginBottom: 8 }}>
          创建新运行，前序成功阶段复用（标记「已复用」），仅重跑失败及后续阶段。原运行保持不变。
        </div>
        <Table rowKey="id" size="small" pagination={false} tableLayout="fixed"
          columns={[
            { title: '阶段', dataIndex: 'name', ellipsis: true },
            { title: '状态', dataIndex: 'status', width: 110,
              render: (v: number) => <Tag color={STAGE_STATUS_COLOR[v] ?? 'default'}>{STAGE_STATUS[v] ?? v}</Tag> },
            { title: '可复用', dataIndex: 'reusable', width: 90, align: 'center',
              render: (v: boolean) => v ? <Tag color="purple">是</Tag> : '否' },
          ]}
          dataSource={(rerunPlanQuery.data ?? []) as unknown as StageExt[]}
        />
      </Modal>

      <Modal open={!!exemptTarget} title="申请门禁豁免" onCancel={() => setExemptTarget(null)}
        onOk={() => {
          if (exemptReason.trim().length < 20) {
            message.warning('豁免理由不少于 20 字')
            return
          }
          exemptMutation.mutate()
        }} okText="确认豁免"
        okButtonProps={{ disabled: exemptReason.trim().length < 20 }} confirmLoading={exemptMutation.isPending}>
        <div style={{ color: 'var(--text-2)', marginBottom: 8 }}>
          规则：{RULE_TYPE[exemptTarget?.ruleType ?? 0] ?? '—'}；理由不少于 20 字，有效期不超过 72 小时。
        </div>
        <Input.TextArea rows={3} placeholder="豁免理由（不少于 20 字）" value={exemptReason}
          onChange={(e) => setExemptReason(e.target.value)} style={{ marginBottom: 8 }} />
        <Space>
          <span>有效期</span>
          <InputNumber min={1} max={72} value={exemptHours} onChange={(v) => setExemptHours(Number(v ?? 24))} />
          <span>小时</span>
        </Space>
      </Modal>
    </div>
  )
}
