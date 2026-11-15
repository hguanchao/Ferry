import { useEffect, useMemo, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { App as AntApp, Alert, Button, Drawer, Modal, Table, Tag, type TableColumnsType } from 'antd'
import { GitCompare, RotateCcw } from 'lucide-react'
import { COL } from '@/lib/table'
import { showError } from '@/lib/notify'
import { useAuthStore } from '@/stores/auth'
import { diffLines, type DiffRow } from '@/lib/diff'

/** 版本行展示视图，流水线版本与模板版本共用 */
export interface VersionRowView {
  id: string
  versionNo: number
  changeNote: string
  createUserName?: string | null
  createTime: string
}

/** 版本历史的属主信息，流水线与模板都满足这个形状 */
export interface VersionOwner {
  id: string
  name: string
  currentVersionId?: string | null
}

/**
 * 版本面板的 API 适配器：列表、详情、回退与 YAML 渲染都由调用方注入，
 * 面板本身不关心版本属于流水线还是模板。
 */
export interface VersionPanelAdapter<D extends VersionRowView> {
  list: (ownerId: string) => Promise<VersionRowView[]>
  detail: (versionId: string) => Promise<D>
  /** 版本定义转 YAML 文本，用于左右对比 */
  yamlOf: (version: D) => string
  rollback: (ownerId: string, versionId: string) => Promise<string>
  /** 列表与单版本详情的 queryKey 前缀 */
  listKey: string
  detailKey: string
  /** 回退按钮的权限串 */
  rollbackPerm: string
}

interface PipelineVersionPanelProps<D extends VersionRowView> {
  open: boolean
  owner: VersionOwner | null
  adapter: VersionPanelAdapter<D>
  onClose: () => void
  /** 回退成功后通知列表刷新 */
  onChanged: () => void
}

/** 版本历史面板：复选两版做左右 diff，回退以旧内容生成新版本，当前版本不提供回退入口 */
export default function PipelineVersionPanel<D extends VersionRowView>({
  open,
  owner,
  adapter,
  onClose,
  onChanged,
}: PipelineVersionPanelProps<D>) {
  const { message, modal } = AntApp.useApp()
  const queryClient = useQueryClient()
  const can = useAuthStore((state) => state.can)
  const [selected, setSelected] = useState<string[]>([])
  const [diffOpen, setDiffOpen] = useState(false)
  const [diffLoading, setDiffLoading] = useState(false)
  const [diff, setDiff] = useState<{ left: D; right: D } | null>(null)

  const ownerId = owner?.id ?? ''

  useEffect(() => {
    setSelected([])
    setDiff(null)
    setDiffOpen(false)
  }, [open, ownerId])

  const versionsQuery = useQuery({
    queryKey: [adapter.listKey, ownerId],
    queryFn: () => adapter.list(ownerId),
    enabled: open && Boolean(ownerId),
  })

  // 列表按版本号倒序，首行就是最新生成的版本（当前版本），回退后标记能立即跟上
  const currentVersionId = versionsQuery.data?.[0]?.id ?? owner?.currentVersionId

  const rollbackMutation = useMutation({
    mutationFn: (versionId: string) => adapter.rollback(ownerId, versionId),
    onSuccess: (text) => {
      message.success(text || '回退成功')
      onChanged()
    },
    onError: showError,
  })

  const openDiff = async () => {
    if (selected.length !== 2) {
      return
    }
    setDiffLoading(true)
    try {
      const [first, second] = await Promise.all(
        selected.map((id) =>
          queryClient.fetchQuery({
            queryKey: [adapter.detailKey, id],
            queryFn: () => adapter.detail(id),
          }),
        ),
      )
      const [left, right] = first.versionNo <= second.versionNo ? [first, second] : [second, first]
      setDiff({ left, right })
      setDiffOpen(true)
    } catch (error) {
      showError(error)
    } finally {
      setDiffLoading(false)
    }
  }

  const confirmRollback = (record: VersionRowView) => {
    modal.confirm({
      title: `回退到 V${record.versionNo}？`,
      content: '将以该版本内容生成一个新版本，历史版本全部保留，新版本成为当前版本。',
      okText: '回退',
      cancelText: '取消',
      onOk: () => rollbackMutation.mutateAsync(record.id),
    })
  }

  const columns: TableColumnsType<VersionRowView> = [
    {
      title: '版本',
      dataIndex: 'versionNo',
      width: 100,
      render: (value: number, record) => (
        <span className="ferry-hstack" style={{ gap: 6 }}>
          <span className="ferry-mono">V{value}</span>
          {record.id === currentVersionId && <Tag color="green">当前</Tag>}
        </span>
      ),
    },
    { title: '变更说明', dataIndex: 'changeNote', ellipsis: true },
    { title: '变更人', dataIndex: 'createUserName', width: 120, render: (value?: string) => value || '—' },
    { title: '变更时间', dataIndex: 'createTime', width: COL.time },
    {
      title: '操作',
      key: 'action',
      width: 90,
      render: (_, record) =>
        can(adapter.rollbackPerm) && record.id !== currentVersionId ? (
          <Button
            type="link"
            size="small"
            icon={<RotateCcw size={14} />}
            onClick={() => confirmRollback(record)}
          >
            回退
          </Button>
        ) : (
          '—'
        ),
    },
  ]

  const diffRows = useMemo(() => {
    if (!diff) {
      return []
    }
    return diffLines(adapter.yamlOf(diff.left), adapter.yamlOf(diff.right))
  }, [diff, adapter])

  return (
    <>
      <Drawer
        open={open}
        width={720}
        title={owner ? `版本历史 · ${owner.name}` : '版本历史'}
        onClose={onClose}
        destroyOnHidden
        extra={
          <Button
            icon={<GitCompare size={15} />}
            disabled={selected.length !== 2}
            loading={diffLoading}
            onClick={() => void openDiff()}
          >
            对比选中版本
          </Button>
        }
      >
        <Alert
          type="info"
          showIcon
          style={{ marginBottom: 12 }}
          message="勾选两个版本查看差异；回退会生成新版本，不会删除历史。"
        />
        <Table<VersionRowView>
          rowKey="id"
          size="small"
          columns={columns}
          dataSource={versionsQuery.data ?? []}
          loading={versionsQuery.isLoading || versionsQuery.isFetching}
          pagination={false}
          rowSelection={{
            selectedRowKeys: selected,
            onChange: (keys) => {
              const next = keys.map(String)
              // 只允许同时选两版，超出的丢掉最早选中的
              setSelected(next.length > 2 ? next.slice(-2) : next)
            },
          }}
          scroll={{ x: 'max-content' }}
        />
      </Drawer>

      <Modal
        open={diffOpen}
        width={1080}
        title={diff ? `版本对比 V${diff.left.versionNo} ↔ V${diff.right.versionNo}` : '版本对比'}
        footer={<Button onClick={() => setDiffOpen(false)}>关闭</Button>}
        onCancel={() => setDiffOpen(false)}
        destroyOnHidden
      >
        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12 }}>
          <DiffPane rows={diffRows} side="left" title={diff ? `V${diff.left.versionNo}` : ''} />
          <DiffPane rows={diffRows} side="right" title={diff ? `V${diff.right.versionNo}` : ''} />
        </div>
      </Modal>
    </>
  )
}

/** 左右两栏 diff 展示，模板合并弹窗也复用这份渲染 */
export function DiffPane({ rows, side, title }: { rows: DiffRow[]; side: 'left' | 'right'; title: string }) {
  return (
    <div>
      <div className="ferry-caption" style={{ marginBottom: 6 }}>{title}</div>
      <div
        style={{
          maxHeight: 480,
          overflow: 'auto',
          border: '1px solid var(--border-1)',
          borderRadius: 'var(--r-sm)',
          background: 'var(--bg-subtle)',
          padding: '4px 0',
        }}
      >
        {rows.map((row, index) => {
          const visible = side === 'left' ? row.type !== 'add' : row.type !== 'del'
          const lineNo = side === 'left' ? row.leftNo : row.rightNo
          const background =
            row.type === 'add'
              ? 'var(--color-success-bg)'
              : row.type === 'del'
                ? 'var(--color-danger-bg)'
                : undefined
          return (
            <div
              key={index}
              style={{
                display: 'flex',
                minHeight: 20,
                background,
                fontSize: 12,
                lineHeight: 'var(--lh-normal)',
                fontFamily: 'var(--font-mono)',
              }}
            >
              <span
                style={{
                  width: 44,
                  flex: 'none',
                  paddingRight: 8,
                  textAlign: 'right',
                  color: 'var(--text-4)',
                  userSelect: 'none',
                }}
              >
                {visible ? lineNo : ''}
              </span>
              <span style={{ whiteSpace: 'pre', paddingRight: 8 }}>{visible ? row.text : ''}</span>
            </div>
          )
        })}
      </div>
    </div>
  )
}
