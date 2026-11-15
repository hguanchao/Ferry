import { useEffect, useMemo, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  App as AntApp,
  Alert,
  AutoComplete,
  Button,
  Collapse,
  Form,
  Input,
  Modal,
  Segmented,
  Select,
  Table,
  Tag,
  type TableColumnsType,
} from 'antd'
import { CheckCircle2, CircleAlert, Info, Play, RefreshCw, ShieldCheck } from 'lucide-react'
import { appApi, pipelineApi, runApi } from '@/api/devops'
import type { CommitBrief, PipelineRow, PrecheckItem, TriggerRecordRow, TriggerResult } from '@/api/types'
import AppSelect from '@/components/AppSelect'
import { showError } from '@/lib/notify'
import { platformLabel, triggerResultLabel } from '@/lib/platform'

interface RunTriggerModalProps {
  open: boolean
  /** 行内进入时预填的流水线上下文；为空表示从列表顶部进入，需要选择应用与流水线 */
  pipeline: Pick<PipelineRow, 'id' | 'name' | 'appId' | 'appName'> | null
  onClose: () => void
  /** 触发产生运行后通知外部刷新，参数为本次的运行id */
  onTriggered: (runId?: string) => void
}

interface TriggerFormValues {
  appId?: string
  pipelineId?: string
  branch?: string
  commitId?: string
  triggerNote?: string
}

const REF_OPTIONS = [
  { value: 0, label: '提交' },
  { value: 1, label: '标签/引用' },
]

/** 触发记录列定义，弹窗内折叠区使用 */
const recordColumns: TableColumnsType<TriggerRecordRow> = [
  { title: '触发时间', dataIndex: 'createTime', width: 170 },
  {
    title: '结果',
    dataIndex: 'result',
    width: 110,
    render: (value: string) =>
      value === 'REJECTED' ? (
        <Tag color="error">{triggerResultLabel(value)}</Tag>
      ) : (
        <Tag color="green">{triggerResultLabel(value)}</Tag>
      ),
  },
  { title: '分支', dataIndex: 'branch', ellipsis: true },
  { title: '提交', dataIndex: 'commitId', width: 150, ellipsis: true, render: (value?: string) => <span className="ferry-mono">{value}</span> },
  { title: '触发人', dataIndex: 'triggerUserName', width: 110, render: (value?: string) => value || '—' },
  { title: '说明', dataIndex: 'rejectReason', ellipsis: true, render: (value?: string) => value || '—' },
]

/**
 * 手动触发弹窗：仓库地址取应用登记信息只读展示，分支与提交为代码平台实时候选，
 * 五项预校验逐条展示；幂等命中成功运行时需确认复用。
 */
export default function RunTriggerModal({ open, pipeline, onClose, onTriggered }: RunTriggerModalProps) {
  const { modal } = AntApp.useApp()
  const queryClient = useQueryClient()
  const [form] = Form.useForm<TriggerFormValues>()
  const [appId, setAppId] = useState<string | undefined>(undefined)
  const [branch, setBranch] = useState<string | undefined>(undefined)
  const [commitId, setCommitId] = useState<string>('')
  const [refType, setRefType] = useState<number>(0)
  const [prechecks, setPrechecks] = useState<PrecheckItem[] | null>(null)
  const [result, setResult] = useState<TriggerResult | null>(null)

  // 行内进入时应用与流水线由上下文锁定；顶部进入时由用户选择
  const locked = Boolean(pipeline)
  const watchedPipelineId = Form.useWatch('pipelineId', form)
  const effectiveAppId = locked ? pipeline?.appId : appId
  const effectivePipelineId = locked ? pipeline?.id : watchedPipelineId

  // 顶部模式：应用变化后重载该应用的流水线候选
  const pipelineQuery = useQuery({
    queryKey: ['pipeline-options', effectiveAppId],
    queryFn: () => pipelineApi.pageList({ appIds: effectiveAppId ? [effectiveAppId] : undefined, current: 1, size: 100 }),
    enabled: open && !locked && Boolean(effectiveAppId),
  })

  // 仓库地址为只读展示，取应用登记的代码源信息
  const appDetailQuery = useQuery({
    queryKey: ['app-detail', effectiveAppId],
    queryFn: () => appApi.detail(effectiveAppId as string),
    enabled: open && Boolean(effectiveAppId),
  })
  const appDetail = appDetailQuery.data ?? null

  const branchQuery = useQuery({
    queryKey: ['run-branches', effectiveAppId],
    queryFn: () => runApi.branchOptions({ appId: effectiveAppId as string }),
    enabled: open && Boolean(effectiveAppId),
  })
  const branchOptions = useMemo(
    () => (branchQuery.data ?? []).map((name) => ({ value: name, label: name })),
    [branchQuery.data],
  )

  const commitQuery = useQuery({
    queryKey: ['run-commits', effectiveAppId, branch],
    queryFn: () => runApi.commitOptions({ appId: effectiveAppId as string, branch: branch as string }),
    enabled: open && refType === 0 && Boolean(effectiveAppId) && Boolean(branch),
  })
  const commitOptions = useMemo(
    () =>
      (commitQuery.data ?? []).map((item: CommitBrief) => ({
        value: item.sha,
        label: `${item.sha.slice(0, 10)} · ${item.author} · ${item.message}`,
      })),
    [commitQuery.data],
  )

  const recordsQuery = useQuery({
    queryKey: ['run-triggers', effectiveAppId],
    queryFn: () => runApi.triggerPageList({ appId: effectiveAppId, current: 1, size: 20 }),
    enabled: open && Boolean(effectiveAppId),
  })

  useEffect(() => {
    if (!open) {
      return
    }
    form.resetFields()
    setAppId(pipeline?.appId)
    setBranch(undefined)
    setCommitId('')
    setRefType(0)
    setPrechecks(null)
    setResult(null)
    if (pipeline) {
      form.setFieldsValue({ branch: undefined })
    }
  }, [open, pipeline, form])

  // 应用登记的默认分支自动预填，分支候选返回后仍可改选
  useEffect(() => {
    if (!appDetail?.repoBranch) {
      return
    }
    setBranch((prev) => prev ?? (appDetail.repoBranch || undefined))
  }, [appDetail])

  const precheckMutation = useMutation({
    mutationFn: () =>
      runApi.precheck({
        appId: effectiveAppId as string,
        pipelineId: effectivePipelineId as string,
        branch: branch as string,
        commitId,
        refType,
      }),
    onSuccess: (items) => setPrechecks(items),
    onError: showError,
  })

  // 触发链路：先按非复用触发，命中成功运行时弹确认后带 reuse 重新触发
  const triggerMutation = useMutation({
    mutationFn: (reuse: boolean) =>
      runApi.trigger({
        appId: effectiveAppId as string,
        pipelineId: effectivePipelineId as string,
        branch: branch as string,
        commitId,
        refType,
        triggerNote: form.getFieldValue('triggerNote'),
        reuse,
      }),
    onSuccess: (result) => {
      if (result.result === 'PENDING_REUSE') {
        modal.confirm({
          title: '复用既有运行的制品？',
          content: '该 commit 已存在成功运行，复用不重新构建。',
          okText: '复用运行',
          cancelText: '重新构建',
          onOk: () => triggerMutation.mutateAsync(true),
        })
        return
      }
      // 被拒绝不产生运行，只刷新触发记录；产生运行时连带刷新运行列表
      if (result.result === 'REJECTED') {
        void queryClient.invalidateQueries({ queryKey: ['run-triggers'] })
        setResult(result)
        return
      }
      void queryClient.invalidateQueries({ queryKey: ['runs'] })
      void queryClient.invalidateQueries({ queryKey: ['run-triggers'] })
      onTriggered(result.runId ?? undefined)
      setResult(result)
    },
    onError: showError,
  })

  const canSubmit = Boolean(effectiveAppId && effectivePipelineId && branch && commitId.trim())

  const handleAppChange = (value?: string | string[]) => {
    const next = Array.isArray(value) ? value[0] : value
    setAppId(next)
    setBranch(undefined)
    setCommitId('')
    setPrechecks(null)
    form.setFieldsValue({ pipelineId: undefined })
  }

  const handleBranchChange = (value?: string) => {
    setBranch(value)
    setCommitId('')
    setPrechecks(null)
  }

  const openDetail = () => {
    if (!result?.runId) {
      return
    }
    onTriggered(result.runId)
    onClose()
  }

  const close = () => {
    if (result?.runId) {
      onTriggered(result.runId)
    }
    onClose()
  }

  const conclusionIcon = (conclusion: PrecheckItem['conclusion']) => {
    if (conclusion === 'FAIL') {
      return <CircleAlert size={14} style={{ color: 'var(--color-danger)', flex: 'none' }} />
    }
    if (conclusion === 'NA') {
      return <Info size={14} style={{ color: 'var(--text-3)', flex: 'none' }} />
    }
    return <CheckCircle2 size={14} style={{ color: 'var(--color-success)', flex: 'none' }} />
  }

  return (
    <Modal
      open={open}
      width={760}
      title={locked ? `手动触发 · ${pipeline?.name ?? ''}` : '手动触发'}
      footer={
        result ? (
          <Button onClick={close}>关闭</Button>
        ) : (
          <>
            <Button onClick={close}>取消</Button>
            <Button
              icon={<ShieldCheck size={14} />}
              loading={precheckMutation.isPending}
              disabled={!canSubmit}
              onClick={() => precheckMutation.mutate()}
            >
              校验仓库与提交
            </Button>
            <Button
              type="primary"
              icon={<Play size={14} />}
              loading={triggerMutation.isPending}
              disabled={!canSubmit}
              onClick={() => triggerMutation.mutate(false)}
            >
              开始运行
            </Button>
          </>
        )
      }
      onCancel={close}
      destroyOnHidden
    >
      {result ? (
        <div style={{ display: 'flex', flexDirection: 'column', gap: 12, alignItems: 'center', padding: '24px 0' }}>
          result.result === 'REJECTED' ? (
            <>
              <CircleAlert size={40} style={{ color: 'var(--color-danger)' }} />
              <div style={{ fontSize: 16, fontWeight: 550 }}>{result.resultName}</div>
              <div style={{ fontSize: 13, color: 'var(--text-2)' }}>{result.rejectReason}</div>
            </>
          ) : (
            <>
              <CheckCircle2 size={40} style={{ color: 'var(--color-success)' }} />
              <div style={{ fontSize: 16, fontWeight: 550 }}>{result.resultName}</div>
              {result.runId && (
                <div style={{ fontSize: 13, color: 'var(--text-2)' }}>
                  运行 ID：<span className="ferry-mono">RUN-{result.runId}</span>
                </div>
              )}
              <Button type="primary" onClick={openDetail}>
                查看运行详情
              </Button>
            </>
          )
        </div>
      ) : (
        <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
          <Form<TriggerFormValues> form={form} layout="vertical" requiredMark="optional" component={false}>
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0 16px' }}>
              <Form.Item label="目标流水线" required>
                {locked ? (
                  <Input value={pipeline?.name} disabled />
                ) : (
                  <Form.Item name="pipelineId" noStyle rules={[{ required: true, message: '请选择流水线' }]}>
                    <Select
                      placeholder="先选应用再选流水线"
                      showSearch
                      optionFilterProp="label"
                      options={(pipelineQuery.data?.records ?? []).map((item) => ({ value: item.id, label: item.name }))}
                      disabled={!effectiveAppId}
                    />
                  </Form.Item>
                )}
              </Form.Item>
              <Form.Item label="目标应用" required>
                {locked ? (
                  <Input value={pipeline?.appName} disabled />
                ) : (
                  <AppSelect value={appId} onChange={handleAppChange} disableStopped />
                )}
              </Form.Item>
              <Form.Item label="仓库地址（只读）">
                <Input
                  value={appDetail?.repoUrl ?? ''}
                  disabled
                  placeholder={effectiveAppId ? '该应用未登记仓库地址' : '选择应用后带出'}
                />
              </Form.Item>
              <Form.Item label="代码平台">
                <Input value={platformLabel(appDetail?.repoType)} disabled />
              </Form.Item>
              <Form.Item label="分支" required>
                <Select
                  showSearch
                  placeholder="实时查询分支列表"
                  options={branchOptions}
                  value={branch}
                  loading={branchQuery.isFetching}
                  disabled={!effectiveAppId}
                  onChange={handleBranchChange}
                />
              </Form.Item>
              <Form.Item label="目标类型">
                <Segmented options={REF_OPTIONS} value={refType} onChange={(value) => { setRefType(value as number); setCommitId(''); setPrechecks(null) }} />
              </Form.Item>
            </div>
            <Form.Item label={refType === 0 ? '提交 ID' : '标签/引用'} required>
              <AutoComplete
                options={refType === 0 ? commitOptions : []}
                value={commitId}
                onChange={(value) => { setCommitId(value); setPrechecks(null) }}
                placeholder={
                  refType === 0
                    ? '点选最近提交或手输 7-40 位十六进制'
                    : '输入标签/引用名'
                }
                disabled={!branch}
              />
            </Form.Item>
            <Form.Item name="triggerNote" label="触发说明">
              <Input.TextArea rows={2} maxLength={500} showCount placeholder="选填，随运行落库供追溯" />
            </Form.Item>
          </Form>

          {prechecks && (
            <div style={{ display: 'flex', flexDirection: 'column', gap: 6 }}>
              {prechecks.map((item) => (
                <div key={item.item} style={{ display: 'flex', alignItems: 'flex-start', gap: 6, fontSize: 13 }}>
                  {conclusionIcon(item.conclusion)}
                  <span>
                    <b>{item.itemName}</b>
                    <span style={{ color: 'var(--text-2)' }}>　{item.detail}</span>
                  </span>
                </div>
              ))}
            </div>
          )}

          <Collapse
            ghost
            items={[
              {
                key: 'records',
                label: (
                  <span className="ferry-hstack" style={{ gap: 6 }}>
                    <RefreshCw size={13} />
                    最近 20 条触发记录
                  </span>
                ),
                children: (
                  <Table<TriggerRecordRow>
                    rowKey="id"
                    size="small"
                    columns={recordColumns}
                    dataSource={recordsQuery.data?.records ?? []}
                    loading={recordsQuery.isLoading || recordsQuery.isFetching}
                    pagination={false}
                    scroll={{ x: 'max-content' }}
                  />
                ),
              },
            ]}
          />
          {!locked && <Alert type="info" showIcon message="触发以（应用、commit、流水线版本）为幂等键：命中进行中运行将合并，命中成功运行可复用其制品。" />}
        </div>
      )}
    </Modal>
  )
}
