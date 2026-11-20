import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { App, Button, Descriptions, Input, Modal, Space, Table, Tag, Timeline, Tooltip, type TableColumnsType } from 'antd'
import { CirclePause, CirclePlay, OctagonX, RotateCcw, Search } from 'lucide-react'
import PageHeader from '@/components/PageHeader'
import { runApi } from '@/api/devops'
import type { RunDetail } from '@/api/types-devops'
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
  const queryClient = useQueryClient()
  const can = useAuthStore((state) => state.can)
  const [runId, setRunId] = useState<string | null>(() => runIdFromHash())
  const [inputId, setInputId] = useState('')
  const [logTask, setLogTask] = useState<TaskExt | null>(null)
  const [keyword, setKeyword] = useState('')
  const [searchOpen, setSearchOpen] = useState(false)
  const [abortOpen, setAbortOpen] = useState(false)
  const [abortReason, setAbortReason] = useState('')
  const [rerunOpen, setRerunOpen] = useState(false)

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

  const stageColumns: TableColumnsType<StageExt> = [
    { title: '顺序', dataIndex: 'seq', width: 70 },
    { title: '阶段', dataIndex: 'stageName' },
    { title: '类型', dataIndex: 'stageType', width: 120,
      render: (v: number) => ['构建', '单测', '静态扫描', '制品入库', '部署非生产', '集成测试', '准入', '生产发布'][v - 1] ?? v },
    { title: '状态', dataIndex: 'status', width: 100,
      render: (v: number) => <Tag color={STATUS_COLOR[v]}>{STAGE_STATUS[v] ?? v}</Tag> },
    { title: '耗时', dataIndex: 'duration', width: 120,
      render: (v: number) => v == null ? '—' : `${(v / 1000).toFixed(1)}s` },
    { title: '复用', dataIndex: 'reusedFromStageId', width: 90,
      render: (v: string) => v ? <Tag color="purple">已复用</Tag> : '—' },
  ]

  const taskColumns: TableColumnsType<TaskExt> = [
    { title: '任务', dataIndex: 'name' },
    { title: '执行器', dataIndex: 'executor', width: 120 },
    { title: '状态', dataIndex: 'status', width: 100,
      render: (v: number) => <Tag color={STATUS_COLOR[v] ?? 'default'}>{v}</Tag> },
    { title: '退出码', dataIndex: 'exitCode', width: 80 },
    { title: '日志行数', dataIndex: 'logLineCount', width: 90,
      render: (v: number, r) => <>{v}{r.isTruncated === 1 && <Tag color="orange" style={{ marginLeft: 4 }}>已截断</Tag>}</> },
    { title: '操作', width: 100,
      render: (_: unknown, r) => can('devops:run:detail') && (
        <Tooltip title="日志">
          <Button type="link" size="small" icon={<Search size={14} />} onClick={() => setLogTask(r)} />
        </Tooltip>
      ) },
  ]

  const status = Number(run?.status ?? 0)
  const canRerun = status === 5 || status === 6

  return (
    <div>
      <PageHeader
        title="运行详情"
        description="运行概要、阶段时序、任务与日志；支持暂停、继续、中止与断点重跑"
        extra={
          <Space>
            <Input placeholder="运行id" value={inputId} onChange={(e) => setInputId(e.target.value)} style={{ width: 160 }} />
            <Button onClick={() => setRunId(inputId || null)}>查询</Button>
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

      {!runId && <div className="ferry-panel">请输入运行id查询，或从流水线列表点击「运行」进入。</div>}

      {runId && detail && (
        <>
          <div className="ferry-panel" style={{ marginBottom: 12 }}>
            <Descriptions column={4} size="small" bordered>
              <Descriptions.Item label="运行ID">{String(run?.runNo ?? '')}</Descriptions.Item>
              <Descriptions.Item label="应用">{String(run?.appName ?? '')}</Descriptions.Item>
              <Descriptions.Item label="流水线">{String(run?.pipelineName ?? '')}</Descriptions.Item>
              <Descriptions.Item label="版本">{run?.pipelineVersionNo ? `V${run.pipelineVersionNo}` : '—'}</Descriptions.Item>
              <Descriptions.Item label="分支">{String(run?.branch ?? '')}</Descriptions.Item>
              <Descriptions.Item label="Commit"><span className="ferry-mono">{String(run?.commitId ?? '')}</span></Descriptions.Item>
              <Descriptions.Item label="状态">
                <Tag color={STATUS_COLOR[status]}>{RUN_STATUS[status] ?? status}</Tag>
              </Descriptions.Item>
              <Descriptions.Item label="触发人">{String(run?.triggerUser ?? '')}</Descriptions.Item>
              <Descriptions.Item label="开始时间">{String(run?.startTime ?? '—')}</Descriptions.Item>
              <Descriptions.Item label="结束时间">{String(run?.endTime ?? '—')}</Descriptions.Item>
              <Descriptions.Item label="总耗时">{run?.duration != null ? `${(Number(run.duration) / 1000).toFixed(1)}s` : '—'}</Descriptions.Item>
              <Descriptions.Item label="制品">{run?.artifactId ? String(run.artifactId) : '—'}</Descriptions.Item>
            </Descriptions>
          </div>

          <div className="ferry-panel" style={{ marginBottom: 12 }}>
            <div style={{ fontWeight: 500, marginBottom: 8 }}>阶段时序</div>
            <Timeline
              items={stages.map((s) => ({
                color: STATUS_COLOR[s.status] ?? 'gray',
                children: (
                  <span>
                    <b>{s.stageName}</b> — {STAGE_STATUS[s.status] ?? s.status}
                    {s.duration != null && <span style={{ marginLeft: 8, color: 'var(--text-2)' }}>{(s.duration / 1000).toFixed(1)}s</span>}
                    {s.reusedFromStageId && <Tag color="purple" style={{ marginLeft: 8 }}>已复用</Tag>}
                  </span>
                ),
              }))}
            />
            <Table rowKey="id" size="small" columns={stageColumns} dataSource={stages} pagination={false} />
          </div>

          <div className="ferry-panel">
            <div style={{ fontWeight: 500, marginBottom: 8 }}>任务列表</div>
            <Table rowKey="id" size="small" columns={taskColumns} dataSource={tasks} pagination={false} />
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
        <Table rowKey="id" size="small" pagination={false}
          columns={[
            { title: '阶段', dataIndex: 'stageName' },
            { title: '状态', dataIndex: 'status', render: (v: number) => STAGE_STATUS[v] ?? v },
            { title: '可复用', dataIndex: 'reusable', render: (v: boolean) => v ? <Tag color="purple">是</Tag> : '否' },
          ]}
          dataSource={(rerunPlanQuery.data ?? []) as unknown as StageExt[]}
        />
      </Modal>
    </div>
  )
}
