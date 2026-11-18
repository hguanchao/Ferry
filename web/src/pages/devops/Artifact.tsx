import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { App, Button, Form, Input, Modal, Select, Space, Table, Tag, Tooltip, type TableColumnsType } from 'antd'
import { ArrowRight, Eye, History, Link2, RotateCw, Search, ShieldAlert } from 'lucide-react'
import PageHeader from '@/components/PageHeader'
import { RoleTag } from '@/components/StatusTag'
import { artifactApi, appApi, envApi } from '@/api/devops'
import type { ArtifactRow, PrecheckResult } from '@/api/types-devops'
import { useAuthStore } from '@/stores/auth'
import { showError } from '@/lib/notify'
import { COL } from '@/lib/table'

const FORM_LABEL: Record<number, string> = { 1: '包制品', 2: '镜像制品' }
const ENV_LABEL: Record<number, string> = { 1: '测试', 2: '预发', 3: '生产' }
const PROD_LABEL: Record<number, string> = { 1: '当前生效', 2: '历史版本', 3: '从未使用' }
const CONCLUSION_LABEL: Record<number, string> = { 1: '全部通过', 2: '不通过', 3: '不适用', 4: '带豁免', 5: '获取失败' }

export default function Artifact() {
  const { message, modal } = App.useApp()
  const queryClient = useQueryClient()
  const can = useAuthStore((state) => state.can)
  const [filters, setFilters] = useState<Record<string, unknown>>({})
  const [draft, setDraft] = useState<Record<string, unknown>>({})
  const [page, setPage] = useState({ current: 1, size: 30 })
  const [keyword, setKeyword] = useState('')
  const [detailId, setDetailId] = useState<string | null>(null)
  const [traceId, setTraceId] = useState<string | null>(null)
  const [promoteRow, setPromoteRow] = useState<ArtifactRow | null>(null)
  const [targetEnvId, setTargetEnvId] = useState<string | undefined>()
  const [promoteNote, setPromoteNote] = useState('')
  const [precheck, setPrecheck] = useState<PrecheckResult | null>(null)

  const query = useQuery({
    queryKey: ['artifacts', filters, page],
    queryFn: () => artifactApi.pageList({ ...filters, ...page }),
  })
  const appsQuery = useQuery({ queryKey: ['app-options'], queryFn: () => appApi.options() })
  const searchQuery = useQuery({
    queryKey: ['artifact-search', keyword],
    queryFn: () => artifactApi.search(keyword),
    enabled: keyword.length > 0,
  })
  const detailQuery = useQuery({
    queryKey: ['artifact-detail', detailId],
    queryFn: () => artifactApi.detail(detailId!),
    enabled: !!detailId,
  })
  const traceQuery = useQuery({
    queryKey: ['artifact-trace', traceId],
    queryFn: async () => await artifactApi.trace(traceId!) as {
      digest: string; branch: string; commitId: string; runId: string
      releaseIds: string[]; deployIds: string[]; rollbackIds: string[]
    },
    enabled: !!traceId,
  })
  const envOptionsQuery = useQuery({
    queryKey: ['artifact-env-options', promoteRow?.appId],
    queryFn: () => envApi.options(promoteRow?.appId),
    enabled: !!promoteRow,
  })

  const precheckMutation = useMutation({
    mutationFn: (body: Record<string, unknown>) => artifactApi.promotePrecheck(body),
    onSuccess: (res) => setPrecheck(res),
    onError: showError,
  })
  const promoteMutation = useMutation({
    mutationFn: (body: Record<string, unknown>) => artifactApi.promote(body),
    onSuccess: (text) => {
      message.success(text || '晋级成功')
      setPromoteRow(null)
      void queryClient.invalidateQueries({ queryKey: ['artifacts'] })
    },
    onError: showError,
  })
  const restoreMutation = useMutation({
    mutationFn: (id: string) => artifactApi.restore(id),
    onSuccess: (text) => {
      message.success(text || '恢复成功')
      void queryClient.invalidateQueries({ queryKey: ['artifacts'] })
    },
    onError: showError,
  })

  const openPromote = (row: ArtifactRow) => {
    setPromoteRow(row)
    setTargetEnvId(undefined)
    setPromoteNote('')
    setPrecheck(null)
  }

  const runPrecheck = () => {
    if (!promoteRow || !targetEnvId) return
    precheckMutation.mutate({ id: promoteRow.id, targetEnvId })
  }

  const columns: TableColumnsType<ArtifactRow> = [
    { title: '制品ID', dataIndex: 'id', width: 140, render: (v: string) => <span className="ferry-mono" style={{ fontSize: 12 }}>{v}</span> },
    { title: '版本号', dataIndex: 'version', width: 140, render: (v: string) => <span className="ferry-mono">{v}</span> },
    { title: '形态', dataIndex: 'form', width: 90, render: (v: number) => FORM_LABEL[v] ?? '—' },
    { title: '摘要', dataIndex: 'digest', width: 160,
      render: (v: string) => <Tooltip title={v}><span className="ferry-mono" style={{ fontSize: 11 }}>{v ? v.slice(0, 18) + '…' : '—'}</span></Tooltip>,
    },
    { title: '分支/Commit', width: 160, render: (_, r) => (
      <div><div style={{ fontSize: 12 }}>{r.branch || '—'}</div><div className="ferry-mono" style={{ fontSize: 11, color: 'var(--text-3)' }}>{r.commitId?.slice(0, 8) || '—'}</div></div>
    ) },
    { title: '所处环境', dataIndex: 'envTags', width: 180, render: (tags: number[] | undefined) => (
      <Space size={4} wrap>
        {(tags ?? []).map((t) => t === 3
          ? <Tag key={t} color="red" icon={<ShieldAlert size={11} />}>生产</Tag>
          : <Tag key={t} color="blue">{ENV_LABEL[t] ?? t}</Tag>)}
        {!tags?.length && <span style={{ color: 'var(--text-3)' }}>—</span>}
      </Space>
    ) },
    { title: '生产使用', dataIndex: 'prodUseStatus', width: 110,
      render: (v: number) => <Tag color={v === 1 ? 'red' : v === 2 ? 'default' : 'blue'}>{PROD_LABEL[v] ?? '—'}</Tag>,
    },
    { title: '门禁结论', dataIndex: 'gateConclusion', width: 100,
      render: (v: number | undefined) => {
        const color = v === 2 || v === 5 ? 'red' : v === 4 ? 'gold' : v === 1 ? 'green' : 'default'
        return v == null ? '—' : <Tag color={color}>{CONCLUSION_LABEL[v] ?? v}</Tag>
      },
    },
    { title: '构建时间', dataIndex: 'createTime', width: COL.time,
      render: (v: string) => <span className="ferry-mono" style={{ fontSize: 12, color: 'var(--text-3)' }}>{v ?? '—'}</span>,
    },
    { title: '操作', key: 'action', width: COL.actionWide, fixed: 'right', render: (_, r) => (
      <Space size={0}>
        <Button type="link" size="small" icon={<Eye size={14} />} onClick={() => setDetailId(r.id)}>详情</Button>
        <Button type="link" size="small" icon={<History size={14} />} onClick={() => setTraceId(r.id)}>追溯</Button>
        {can('devops:artifact:promote') && (
          <Button type="link" size="small" icon={<ArrowRight size={14} />} onClick={() => openPromote(r)}>晋级</Button>
        )}
        {can('devops:artifact:restore') && (r as unknown as { deleteSeq?: string }).deleteSeq && Number((r as unknown as { deleteSeq?: string }).deleteSeq) !== 0 && (
          <Button type="link" size="small" icon={<RotateCw size={14} />}
            onClick={() => modal.confirm({ title: '恢复该制品？', content: '30 天内且同版本无有效制品时可恢复。',
              okText: '恢复', cancelText: '取消', onOk: () => restoreMutation.mutateAsync(r.id) })}>恢复</Button>
        )}
      </Space>
    ) },
  ]

  return (
    <>
      <PageHeader title="制品管理" description="制品不可变：同版本重复推送一律拒绝。摘要可反查，晋级仅写环境标记，生产制品单独红标。"
        tag={<RoleTag>共 {query.data?.total ?? 0} 项</RoleTag>}
        extra={can('devops:artifactClean:pageList') && (
          <Button icon={<Link2 size={14} />} onClick={() => { window.location.hash = '#/devops/artifactClean' }}>清理策略</Button>
        )}
      />
      <section className="ferry-panel">
        <div className="ferry-toolbar">
          <Input.Search allowClear prefix={<Search size={15} />} placeholder="摘要 / 版本号 / Commit 反查"
            style={{ width: 280 }} value={keyword} onChange={(e) => setKeyword(e.target.value)}
            onSearch={() => setKeyword(keyword)} />
          <Select allowClear placeholder="应用" style={{ width: 150 }} value={draft.appId as string | undefined}
            onChange={(v) => setDraft((p) => ({ ...p, appId: v }))}
            options={(appsQuery.data ?? []).map((a) => ({ value: a.value, label: a.label }))} />
          <Select allowClear placeholder="形态" style={{ width: 110 }} value={draft.form as number | undefined}
            onChange={(v) => setDraft((p) => ({ ...p, forms: v == null ? undefined : [v] }))}
            options={[{ value: 1, label: '包制品' }, { value: 2, label: '镜像制品' }]} />
          <Select allowClear placeholder="生产使用" style={{ width: 120 }} value={draft.prodUseStatus as number | undefined}
            onChange={(v) => setDraft((p) => ({ ...p, prodUseStatus: v }))}
            options={Object.entries(PROD_LABEL).map(([k, v]) => ({ value: Number(k), label: v }))} />
          <Select allowClear placeholder="门禁结论" style={{ width: 120 }} value={draft.gateConclusion as number | undefined}
            onChange={(v) => setDraft((p) => ({ ...p, gateConclusion: v }))}
            options={Object.entries(CONCLUSION_LABEL).map(([k, v]) => ({ value: Number(k), label: v }))} />
          <Button type="primary" icon={<Search size={15} />}
            onClick={() => { setFilters(draft); setPage((p) => ({ ...p, current: 1 })) }}>查询</Button>
          <Button icon={<RotateCw size={15} />}
            onClick={() => { setDraft({}); setFilters({}); setPage((p) => ({ ...p, current: 1 })) }}>重置</Button>
        </div>
        <div className="ferry-table-wrap">
          <Table<ArtifactRow> rowKey="id" columns={columns}
            dataSource={keyword && searchQuery.data?.length ? searchQuery.data : query.data?.records ?? []}
            loading={query.isLoading || query.isFetching || searchQuery.isFetching} scroll={{ x: 'max-content' }}
            pagination={{ current: page.current, pageSize: page.size, total: query.data?.total ?? 0,
              showSizeChanger: true, pageSizeOptions: ['30', '100', '200'],
              showTotal: (total) => `共 ${total} 条`, onChange: (current, size) => setPage({ current, size }) }}
          />
        </div>
      </section>

      <Modal open={!!detailId} title="制品详情" footer={null} width={720} onCancel={() => setDetailId(null)} destroyOnHidden>
        {detailQuery.data && (
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12 }}>
            <div><b>版本</b><div className="ferry-mono">{detailQuery.data.version}</div></div>
            <div><b>形态</b><div>{FORM_LABEL[detailQuery.data.form] ?? '—'}</div></div>
            <div><b>摘要</b><div className="ferry-mono" style={{ wordBreak: 'break-all' }}>{detailQuery.data.digest}</div></div>
            <div><b>大小</b><div>{detailQuery.data.size} B</div></div>
            <div><b>环境</b><div>{(detailQuery.data.envTags ?? []).map((e) => ENV_LABEL[e] ?? e).join('、') || '—'}</div></div>
            <div><b>生产使用</b><div>{PROD_LABEL[detailQuery.data.prodUseStatus] ?? '—'}</div></div>
          </div>
        )}
      </Modal>

      <Modal open={!!traceId} title="制品追溯" footer={null} width={640} onCancel={() => setTraceId(null)} destroyOnHidden>
        {traceQuery.data && (
          <div style={{ lineHeight: 2 }}>
            <div>摘要：<span className="ferry-mono">{traceQuery.data.digest}</span></div>
            <div>分支/Commit：<span className="ferry-mono">{traceQuery.data.branch} / {traceQuery.data.commitId}</span></div>
            <div>产出运行：{traceQuery.data.runId}</div>
            <div>关联发布单：{(traceQuery.data.releaseIds ?? []).join('、') || '—'}</div>
            <div>关联部署：{(traceQuery.data.deployIds ?? []).join('、') || '—'}</div>
            <div>关联回滚：{(traceQuery.data.rollbackIds ?? []).join('、') || '—'}</div>
          </div>
        )}
      </Modal>

      <Modal open={!!promoteRow} title={`晋级 · ${promoteRow?.version ?? ''}`} width={560}
        okText={precheck?.canTrigger === false ? '校验未通过' : '确认晋级'}
        okButtonProps={{ disabled: precheck?.canTrigger === false || !targetEnvId, danger: targetEnvId === undefined }}
        cancelText="取消" confirmLoading={promoteMutation.isPending}
        onCancel={() => setPromoteRow(null)}
        onOk={() => {
          if (!promoteRow || !targetEnvId) return
          promoteMutation.mutate({ id: promoteRow.id, targetEnvId, note: promoteNote })
        }} destroyOnHidden>
        {precheck?.canTrigger === false && (
          <div style={{ padding: 8, background: '#fff2e8', color: '#d4380d', borderRadius: 4, marginBottom: 12 }}>
            晋级校验未通过，请查看下方校验项。
          </div>
        )}
        <Form layout="vertical" requiredMark="optional">
          <Form.Item label="目标环境">
            <Select placeholder="请选择下一级环境" value={targetEnvId} onChange={(v) => { setTargetEnvId(v); setPrecheck(null) }}
              options={(envOptionsQuery.data ?? []).map((e) => ({ value: e.value, label: e.label }))} />
          </Form.Item>
          <Button block onClick={runPrecheck} loading={precheckMutation.isPending} disabled={!targetEnvId}>运行晋级预检</Button>
          {precheck && (
            <div style={{ marginTop: 12 }}>
              {(precheck.items ?? []).map((item, idx) => (
                <div key={idx} style={{ color: item.conclusion === 2 ? '#d4380d' : '#389e0d', fontSize: 13 }}>
                  {item.conclusion === 2 ? '✗' : '✓'} {item.detail ?? item.type}
                </div>
              ))}
            </div>
          )}
          <Form.Item label="备注（晋级生产必填）">
            <Input.TextArea rows={3} value={promoteNote} onChange={(e) => setPromoteNote(e.target.value)}
              placeholder="晋级到生产时说明门禁豁免处理计划" />
          </Form.Item>
        </Form>
      </Modal>
    </>
  )
}
