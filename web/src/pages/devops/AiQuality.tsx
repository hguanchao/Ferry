import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { Button, DatePicker, Select, Table, Tag, type TableColumnsType } from 'antd'
import { CircleCheck, FileQuestion, RotateCw, ShieldAlert, Undo2 } from 'lucide-react'
import PageHeader from '@/components/PageHeader'
import { RoleTag } from '@/components/StatusTag'
import { aiApi, metricApi } from '@/api/devops'
import { post } from '@/api/client'
import type { AiFeedbackRow, MetricRow } from '@/api/types-devops'
import { COL } from '@/lib/table'

interface QualityData {
  totalOutput?: number
  adoptedCount?: number
  adoptRate?: number | string
  lowConfidenceRate?: number | string
  refusalRate?: number | string
  actionUndoRate?: number | string
  [key: string]: unknown
}

/** 比率展示：后端返回 0~1 的小数，转成百分比 */
function pct(value: number | string | undefined): string {
  if (value === undefined || value === null) return '—'
  const n = Number(value)
  return `${(n <= 1 ? n * 100 : n).toFixed(1)}%`
}

/** 前端 CSV 下载 */
function downloadCsv(rows: MetricRow[], fileName: string) {
  const header = ['统计日', '应用ID', '部署频率', '变更前置时间(s)', '变更失败率', 'MTTR(s)', '数据状态']
  const lines = rows.map((r) => [
    r.statDate, r.appId, r.deployFreq ?? '', r.leadTimeP50 ?? '', r.changeFailRate ?? '',
    r.mttrP50 ?? '', r.dataStatus === 2 ? '数据不足' : '正常',
  ].map((v) => `"${String(v).replace(/"/g, '""')}"`).join(','))
  const blob = new Blob(['\ufeff' + [header.join(','), ...lines].join('\n')], { type: 'text/csv;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url; a.download = fileName
  document.body.appendChild(a); a.click(); document.body.removeChild(a)
  URL.revokeObjectURL(url)
}

export default function AiQuality() {
  const [filters, setFilters] = useState<Record<string, unknown>>({})
  const [draft, setDraft] = useState<Record<string, unknown>>({})
  const [page, setPage] = useState({ current: 1, size: 30 })

  // /metric/aiQuality 未在 devops.ts 暴露，直接用 client.post
  const qualityQuery = useQuery({
    queryKey: ['ai-quality', filters],
    queryFn: () => post<QualityData>('/ferry-devops/metric/aiQuality', filters),
  })
  const feedbackQuery = useQuery({
    queryKey: ['ai-feedback', filters, page],
    queryFn: () => aiApi.feedbackPage({ ...filters, ...page }),
  })
  const dailyQuery = useQuery({
    queryKey: ['ai-quality-daily', filters, page],
    queryFn: () => metricApi.dora({ ...filters, ...page }),
  })

  const q = (qualityQuery.data ?? {}) as QualityData
  const cards = [
    { key: 'adopt', label: '归因采纳率', value: pct(q.adoptRate), hint: `${q.adoptedCount ?? 0} / ${q.totalOutput ?? 0}`, icon: <CircleCheck size={18} />, tone: 'success' },
    { key: 'low', label: '低置信占比', value: pct(q.lowConfidenceRate), hint: '证据不足的结论', icon: <ShieldAlert size={18} />, tone: 'warning' },
    { key: 'refusal', label: '拒答率', value: pct(q.refusalRate), hint: '无有效证据丢弃', icon: <FileQuestion size={18} />, tone: 'default' },
    { key: 'undo', label: '动作撤销率', value: pct(q.actionUndoRate), hint: '可撤销 L1 撤销数 / 成功数', icon: <Undo2 size={18} />, tone: 'primary' },
  ]

  const feedbackColumns: TableColumnsType<AiFeedbackRow> = [
    { title: '输出ID', dataIndex: 'outputId', width: COL.code,
      render: (v: string) => <span className="ferry-mono" style={{ fontSize: 12 }}>{v}</span> },
    { title: '是否有帮助', dataIndex: 'isHelpful', width: 120,
      render: (v: number) => <Tag color={v === 1 ? 'green' : 'red'}>{v === 1 ? '有帮助' : '没帮助'}</Tag> },
    { title: '反馈人', dataIndex: 'userId', width: COL.creator },
    { title: '正确答案', dataIndex: 'correctAnswer',
      render: (v?: string) => <span style={{ color: 'var(--text-3)' }}>{v || '—'}</span> },
    { title: '反馈时间', dataIndex: 'createTime', width: COL.time,
      render: (v: string) => <span className="ferry-mono" style={{ fontSize: 12, color: 'var(--text-3)' }}>{v ?? '—'}</span> },
  ]

  const dailyColumns: TableColumnsType<MetricRow> = [
    { title: '统计日', dataIndex: 'statDate', width: COL.time,
      render: (v: string) => <span className="ferry-mono" style={{ fontSize: 12 }}>{v}</span> },
    { title: '应用ID', dataIndex: 'appId', width: COL.code },
    { title: 'AI采纳率', dataIndex: 'aiAdoptRate', width: 110, render: (v?: string) => pct(v ?? undefined) },
    { title: '低置信占比', dataIndex: 'aiLowConfidenceRate', width: 120, render: (v?: string) => pct(v ?? undefined) },
    { title: '拒答率', dataIndex: 'aiRefusalRate', width: 100, render: (v?: string) => pct(v ?? undefined) },
    { title: '动作撤销率', dataIndex: 'actionUndoRate', width: 120, render: (v?: string) => pct(v ?? undefined) },
    { title: '数据状态', dataIndex: 'dataStatus', width: 100,
      render: (v: number) => <Tag color={v === 2 ? 'orange' : 'green'}>{v === 2 ? '数据不足' : '正常'}</Tag> },
  ]

  return (
    <>
      <PageHeader title="AI 质量看板" description="归因采纳率、低置信占比、拒答率与动作撤销率。指标口径固定：拒答率=无法归因输出/总输出，撤销率=可撤销 L1 撤销数/成功数。"
        tag={<RoleTag>输出 {q.totalOutput ?? 0} 条</RoleTag>}
        extra={<Button icon={<RotateCw size={14} />} onClick={() => {
          downloadCsv(dailyQuery.data?.records ?? [], 'AI质量明细.csv')
        }}>导出 CSV</Button>}
      />

      <div className="ferry-stat-grid">
        {cards.map((card) => (
          <div className="ferry-stat" data-tone={card.tone} key={card.key}>
            <span className="ferry-stat__stripe" />
            <span className="ferry-stat__label" style={{ display: 'flex', alignItems: 'center', gap: 6 }}>{card.icon}{card.label}</span>
            <span className="ferry-stat__value">{card.value}</span>
            <span className="ferry-stat__hint">{card.hint}</span>
          </div>
        ))}
      </div>

      <section className="ferry-panel" style={{ marginTop: 16 }}>
        <div className="ferry-toolbar">
          <DatePicker.RangePicker
            onChange={(dates) => setDraft((prev) => ({
              ...prev,
              start: dates?.[0]?.format('YYYY-MM-DD'),
              end: dates?.[1]?.format('YYYY-MM-DD'),
            }))}
          />
          <Select allowClear placeholder="AI 能力类型" style={{ width: 160 }} value={draft.type as number}
            onChange={(value) => setDraft((prev) => ({ ...prev, type: value }))}
            options={[{ value: 1, label: '失败归因' }, { value: 2, label: '准入辅助' }, { value: 3, label: '发布预案' }, { value: 4, label: '异常归因' }]} />
          <Button type="primary" icon={<RotateCw size={15} />}
            onClick={() => { setFilters(draft); setPage((p) => ({ ...p, current: 1 })) }}>查询</Button>
        </div>
        <div className="ferry-table-wrap">
          <Table<MetricRow> rowKey="id" columns={dailyColumns} dataSource={dailyQuery.data?.records ?? []}
            loading={dailyQuery.isLoading || dailyQuery.isFetching} scroll={{ x: 'max-content' }}
            pagination={{
              current: page.current, pageSize: page.size, total: dailyQuery.data?.total ?? 0,
              showSizeChanger: true, pageSizeOptions: ['30', '100', '200'],
              showTotal: (total) => `共 ${total} 条`, onChange: (current, size) => setPage({ current, size }),
            }} />
        </div>
      </section>

      <section className="ferry-panel" style={{ marginTop: 16 }}>
        <div className="ferry-panel__head"><span className="ferry-panel__title">用户反馈明细</span></div>
        <div className="ferry-table-wrap">
          <Table<AiFeedbackRow> rowKey="id" columns={feedbackColumns} dataSource={feedbackQuery.data?.records ?? []}
            loading={feedbackQuery.isLoading} scroll={{ x: 'max-content' }}
            pagination={{
              current: page.current, pageSize: page.size, total: feedbackQuery.data?.total ?? 0,
              showTotal: (total) => `共 ${total} 条`, onChange: (current, size) => setPage({ current, size }),
            }} />
        </div>
      </section>
    </>
  )
}
