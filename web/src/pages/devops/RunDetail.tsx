import { useEffect, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useNavigate, useParams } from 'react-router-dom'
import { App, Button, Descriptions, Input, Modal, Space, Steps, Table, Tag, Tooltip, type TableColumnsType } from 'antd'
import { CirclePause, CirclePlay, OctagonX, RotateCcw, Search } from 'lucide-react'
import PageHeader from '@/components/PageHeader'
import { runApi } from '@/api/devops'
import type { RunDetail, RunRow } from '@/api/types-devops'
import { useAuthStore } from '@/stores/auth'
import { showError } from '@/lib/notify'

const RUN_STATUS: Record<number, string> = {
  1: '排队中', 2: '运行中', 3: '阻塞', 4: '成功', 5: '失败', 6: '超时', 7: '已取消', 8: '结果待确认',
}
const STAGE_STATUS: Record<number, string> = {
  1: '未开始', 2: '进行中', 3: '成功', 4: '失败', 5: '阻塞', 6: '跳过', 7: '超时', 8: '已复用', 9: '已取消',
}
const STATUS_COLOR: Record<number, string> = {
  1: 'blue', 2: 'processing', 3: 'orange', 4: 'green', 5: 'red', 6: 'red', 7: 'default', 8: 'purple', 9: 'default',
}
/** 阶段/任务状态色：与运行状态语义不同，1 未开始应为灰色而非蓝色 */
const STAGE_STATUS_COLOR: Record<number, string> = {
  1: 'default', 2: 'processing', 3: 'green', 4: 'red', 5: 'orange', 6: 'default', 7: 'red', 8: 'purple', 9: 'default',
}

/** 阶段状态映射到 AntD Steps 状态 */
function stepStatus(value: number): 'wait' | 'process' | 'finish' | 'error' {
  if (value === 2) return 'process'
  if (value === 3 || value === 8) return 'finish'
  if (value === 4 || value === 5 || value === 7) return 'error'
  return 'wait'
}
interface StageExt {
  id: string
  runId: string
  stageType: number
  stageName: string
  seq: number
  status: number
  duration?: number
  reusedFromStageId?: string
  tasks?: TaskExt[]
}
interface TaskExt {
  id: string
  stageId: string
  name: string
  executor?: string
  status: number
  exitCode?: number
  duration?: number
  logLineCount: number
  isTruncated: number
}
interface LogSegment {
  segmentNo: number
  startLine: number
  endLine: number
  content: string
  isTruncated: number
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
  const [logTask, setLogTask] = useState<TaskExt | null>(null)
  const [keyword, setKeyword] = useState('')
  const [searchOpen, setSearchOpen] = useState(false)
  const [abortOpen, setAbortOpen] = useState(false)
  const [abortReason, setAbortReason] = useState('')
  const [rerunOpen, setRerunOpen] = useState(false)

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
      return status === 2 || status === 1 ? 3000 : false
    },
  })
  const stagesQuery = useQuery({
    queryKey: ['run-stages', runId],
    queryFn: () => runApi.stages(runId!),
    enabled: !!runId,
  })
  const rerunPlanQuery = useQuery({
    queryKey: ['rerun-plan', runId],
    queryFn: () => runApi.rerunPlan(runId!),
    enabled: rerunOpen && !!runId,
  })
  const logQuery = useQuery({
    queryKey: ['run-log', logTask?.id],
    queryFn: () => runApi.log(logTask!.id),
    enabled: !!logTask,
  })

  const invalidate = () => {
    void queryClient.invalidateQueries({ queryKey: ['run-detail'] })
    void queryClient.invalidateQueries({ queryKey: ['run-stages'] })
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
  const searchMutation = useMutation({
    mutationFn: (body: Record<string, unknown>) => runApi.logSearch(body),
    onSuccess: () => setSearchOpen(true), onError: showError,
  })

  const detail = detailQuery.data
  const run = detail?.run
  const stages = ((detail?.stages ?? stagesQuery.data ?? []) as unknown as StageExt[])
  const tasks = (detail?.tasks ?? []) as unknown as TaskExt[]
  const stageNameById = new Map(stages.map((stage) => [String(stage.id), stage.stageName]))

  const taskColumns: TableColumnsType<TaskExt> = [
    { title: '任务', dataIndex: 'name', width: 360, ellipsis: true },
    { title: '所属阶段', dataIndex: 'stageId', width: 160, ellipsis: true,
      render: (v: string) => stageNameById.get(String(v)) ?? '—' },
    { title: '执行器', dataIndex: 'executor', width: 120, render: (v?: string) => v || '—' },
    { title: '状态', dataIndex: 'status', width: 110, align: 'center',
      render: (v: number) => <Tag color={STAGE_STATUS_COLOR[v] ?? 'default'}>{STAGE_STATUS[v] ?? v}</Tag> },
    { title: '退出码', dataIndex: 'exitCode', width: 90, align: 'center', render: (v?: number) => v ?? '—' },
    { title: '耗时', dataIndex: 'duration', width: 100, align: 'right',
      render: (v?: number) => v == null ? '—' : `${(v / 1000).toFixed(1)}s` },
    { title: '日志行数', dataIndex: 'logLineCount', width: 120, align: 'right',
      render: (v: number, r) => <>{v}{r.isTruncated === 1 && <Tag color="orange" style={{ marginLeft: 6 }}>已截断</Tag>}</> },
    { title: '操作', width: 80, align: 'center',
      render: (_: unknown, r) => can('devops:run:detail') && (
        <Tooltip title="查看日志">
          <Button type="link" size="small" icon={<Search size={14} />} onClick={() => setLogTask(r)} />
        </Tooltip>
      ) },
  ]

  const status = Number(run?.status ?? 0)
  const canRerun = status === 5 || status === 6
  const finishedStages = stages.filter((stage) => [3, 4, 6, 7, 8, 9].includes(stage.status)).length

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

  const runColumns: TableColumnsType<RunRow> = [
    { title: '运行编号', dataIndex: 'runNo', width: 180,
      render: (v: string) => <span className="ferry-mono" style={{ fontSize: 12 }}>{v}</span> },
    { title: '应用', dataIndex: 'appName', width: 120, ellipsis: true, render: (v?: string) => v || '—' },
    { title: '流水线', dataIndex: 'pipelineName', ellipsis: true, render: (v?: string) => v || '—' },
    { title: '分支', dataIndex: 'branch', width: 120, ellipsis: true,
      render: (v?: string) => v ? <span className="ferry-mono" style={{ fontSize: 12 }}>{v}</span> : '—' },
    { title: 'Commit', dataIndex: 'commitId', width: 110,
      render: (v?: string) => v
        ? <Tooltip title={v}><span className="ferry-mono" style={{ fontSize: 12 }}>{v.slice(0, 8)}</span></Tooltip>
        : '—' },
    { title: '状态', dataIndex: 'status', width: 110, align: 'center',
      render: (v: number) => <Tag color={STATUS_COLOR[v] ?? 'default'}>{RUN_STATUS[v] ?? v}</Tag> },
    { title: '触发时间', dataIndex: 'createTime', width: 170 },
    { title: '操作', width: 80, align: 'center',
      render: (_: unknown, record: RunRow) => (
        <Button type="link" size="small" onClick={() => openRun(String(record.id))}>查看</Button>
      ) },
  ]

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
      <PageHeader
        title={runId ? '运行详情' : '运行记录'}
        tag={runId && run?.runNo ? <Tag className="ferry-mono">{String(run.runNo)}</Tag> : undefined}
        description={runId
          ? '运行概要、阶段进度、任务与日志；支持暂停、继续、中止与断点重跑'
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
          <Table<RunRow>
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
                <Descriptions.Item label="Commit" span={2}>
                  <Tooltip title={run?.commitId}>
                    <span className="ferry-mono">{String(run?.commitId ?? '—')}</span>
                  </Tooltip>
                </Descriptions.Item>
                <Descriptions.Item label="开始时间">{String(run?.startTime ?? '—')}</Descriptions.Item>
                <Descriptions.Item label="结束时间">{String(run?.endTime ?? '—')}</Descriptions.Item>
                <Descriptions.Item label="总耗时">
                  {run?.duration != null ? `${(Number(run.duration) / 1000).toFixed(1)}s` : '—'}
                </Descriptions.Item>
                <Descriptions.Item label="制品">{run?.artifactId ? String(run.artifactId) : '—'}</Descriptions.Item>
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
                current={stages.findIndex((stage) => stage.status === 2)}
                items={stages.map((stage) => ({
                  title: stage.stageName,
                  status: stepStatus(stage.status),
                  description: (
                    <span>
                      {STAGE_STATUS[stage.status] ?? stage.status}
                      {stage.duration != null && ` · ${(stage.duration / 1000).toFixed(1)}s`}
                      {stage.reusedFromStageId && <Tag color="purple" style={{ marginLeft: 4 }}>复用</Tag>}
                    </span>
                  ),
                }))}
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
              scroll={{ x: 1000 }}
            />
          </div>
        </>
      )}

      <Modal open={!!logTask} title={`任务日志 - ${logTask?.name ?? ''}`} onCancel={() => setLogTask(null)}
        footer={null} width={860} destroyOnHidden>
        <div style={{ display: 'flex', gap: 8, marginBottom: 8 }}>
          <Input prefix={<Search size={14} />} placeholder="关键词搜索日志" value={keyword}
            onChange={(e) => setKeyword(e.target.value)} style={{ width: 280 }} />
          <Button loading={searchMutation.isPending}
            onClick={() => searchMutation.mutate({ taskId: logTask?.id, keyword, fromLine: 1 })}>搜索</Button>
        </div>
        {(logQuery.data as LogSegment[] | undefined)?.map((seg) => (
          <div key={seg.segmentNo} style={{ marginBottom: 12 }}>
            <div style={{ fontSize: 12, color: 'var(--text-2)', marginBottom: 4 }}>
              段 {seg.segmentNo} · 行 {seg.startLine}-{seg.endLine}
              {seg.isTruncated === 1 && <Tag color="orange" style={{ marginLeft: 6 }}>日志已截断</Tag>}
            </div>
            <pre className="ferry-mono" style={{ background: 'var(--bg-subtle)', padding: 12, borderRadius: 6,
              maxHeight: 360, overflow: 'auto', whiteSpace: 'pre-wrap', fontSize: 12 }}>{seg.content}</pre>
          </div>
        ))}
        {searchOpen && (
          <div style={{ marginTop: 12 }}>
            <div style={{ fontWeight: 500, marginBottom: 4 }}>搜索结果</div>
            <pre className="ferry-mono" style={{ background: 'var(--bg-subtle)', padding: 12, borderRadius: 6,
              maxHeight: 240, overflow: 'auto', whiteSpace: 'pre-wrap', fontSize: 12 }}>
              {(searchMutation.data as Array<{ lineNo: number; content: string }> | undefined)
                ?.map((h) => `${h.lineNo}: ${h.content}`).join('\n') ?? '无匹配'}
            </pre>
          </div>
        )}
      </Modal>

      <Modal open={abortOpen} title="中止运行" onCancel={() => setAbortOpen(false)}
        onOk={() => abortMutation.mutate({ id: runId, reason: abortReason })} okText="确认中止"
        okButtonProps={{ danger: true }} confirmLoading={abortMutation.isPending}>
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
            { title: '阶段', dataIndex: 'stageName', ellipsis: true },
            { title: '状态', dataIndex: 'status', width: 110,
              render: (v: number) => <Tag color={STAGE_STATUS_COLOR[v] ?? 'default'}>{STAGE_STATUS[v] ?? v}</Tag> },
            { title: '可复用', dataIndex: 'reusable', width: 90, align: 'center',
              render: (v: boolean) => v ? <Tag color="purple">是</Tag> : '否' },
          ]}
          dataSource={(rerunPlanQuery.data ?? []) as unknown as StageExt[]}
        />
      </Modal>
    </div>
  )
}
