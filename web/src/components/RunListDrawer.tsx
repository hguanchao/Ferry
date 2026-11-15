import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { App as AntApp, Button, Drawer, Table, Tag, Tooltip, type TableColumnsType } from 'antd'
import { FileSearch, StopCircle } from 'lucide-react'
import RunDetailDrawer from '@/components/RunDetailDrawer'
import { runApi } from '@/api/devops'
import type { RunRow } from '@/api/types'
import { showError } from '@/lib/notify'
import { formatDuration, runStatusLabel } from '@/lib/platform'

interface RunListDrawerProps {
  open: boolean
  pipeline: { pipelineId: string; pipelineName: string } | null
  onClose: () => void
  /** 运行取消或数据变化后通知外部刷新 */
  onChanged: () => void
}

function statusColor(status: string) {
  if (status === 'SUCCESS') return 'success'
  if (status === 'FAILED' || status === 'TIMEOUT') return 'error'
  if (status === 'RUNNING') return 'processing'
  if (status === 'BLOCKED') return 'warning'
  return 'default'
}

/** 流水线的运行列表抽屉：展示每次运行的台账数据，点击进入运行详情 */
export default function RunListDrawer({ open, pipeline, onClose, onChanged }: RunListDrawerProps) {
  const { modal } = AntApp.useApp()
  const queryClient = useQueryClient()
  const [page, setPage] = useState({ current: 1, size: 20 })
  const [detailId, setDetailId] = useState<string | null>(null)

  const pipelineId = pipeline?.pipelineId ?? ''

  const listQuery = useQuery({
    queryKey: ['runs', pipelineId, page],
    queryFn: () => runApi.pageList({ pipelineId, ...page }),
    enabled: open && Boolean(pipelineId),
  })

  const cancelMutation = useMutation({
    mutationFn: (id: string) => runApi.cancel(id),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['runs'] })
      onChanged()
    },
    onError: showError,
  })

  const confirmCancel = (record: RunRow) => {
    modal.confirm({
      title: '取消运行？',
      content: '仅排队中的运行可以取消，取消后不进入执行队列。',
      okText: '取消运行',
      okButtonProps: { danger: true },
      cancelText: '返回',
      onOk: () => cancelMutation.mutateAsync(record.id),
    })
  }

  const columns: TableColumnsType<RunRow> = [
    {
      title: '运行ID',
      dataIndex: 'id',
      width: 210,
      render: (value: string) => <span className="ferry-mono" style={{ fontSize: 12 }}>RUN-{value}</span>,
    },
    {
      title: '状态',
      dataIndex: 'status',
      width: 100,
      render: (value: string) => <Tag color={statusColor(value)}>{runStatusLabel(value)}</Tag>,
    },
    { title: '分支', dataIndex: 'branch', width: 140, ellipsis: true },
    {
      title: '提交',
      dataIndex: 'commitId',
      width: 150,
      render: (value: string, record) => (
        <Tooltip title={record.refType === 1 ? '标签/引用' : '提交'}>
          <span className="ferry-mono" style={{ fontSize: 12 }}>{value.slice(0, 12)}</span>
        </Tooltip>
      ),
    },
    {
      title: '版本',
      dataIndex: 'pipelineVersionNo',
      width: 70,
      render: (value: number) => <span className="ferry-mono">V{value}</span>,
    },
    { title: '触发人', dataIndex: 'triggerUserName', width: 110, render: (value?: string) => value || '—' },
    { title: '触发时间', dataIndex: 'createTime', width: 170 },
    {
      title: '耗时',
      key: 'duration',
      width: 90,
      render: (_, record) => formatDuration(record.startedTime, record.finishedTime),
    },
    {
      title: '操作',
      key: 'action',
      width: 150,
      render: (_, record) => (
        <div style={{ display: 'flex', alignItems: 'center', gap: 2 }}>
          <Button
            type="link"
            size="small"
            icon={<FileSearch size={14} />}
            onClick={() => setDetailId(record.id)}
          >
            运行详情
          </Button>
          {record.status === 'QUEUED' && (
            <Tooltip title="取消运行">
              <Button type="link" size="small" danger icon={<StopCircle size={14} />} onClick={() => confirmCancel(record)} />
            </Tooltip>
          )}
        </div>
      ),
    },
  ]

  return (
    <>
      <Drawer
        open={open}
        width={920}
        title={pipeline ? `运行台账 · ${pipeline.pipelineName}` : '运行台账'}
        onClose={onClose}
        destroyOnHidden
      >
        <Table<RunRow>
          rowKey="id"
          size="small"
          columns={columns}
          dataSource={listQuery.data?.records ?? []}
          loading={listQuery.isLoading || listQuery.isFetching}
          scroll={{ x: 'max-content' }}
          pagination={{
            current: page.current,
            pageSize: page.size,
            total: listQuery.data?.total ?? 0,
            showSizeChanger: false,
            showTotal: (total) => `共 ${total} 条`,
            onChange: (current, size) => setPage({ current, size }),
          }}
        />
      </Drawer>

      <RunDetailDrawer
        open={Boolean(detailId)}
        runId={detailId}
        onClose={() => setDetailId(null)}
        onChanged={onChanged}
      />
    </>
  )
}
