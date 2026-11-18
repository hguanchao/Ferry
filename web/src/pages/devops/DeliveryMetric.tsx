import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { App, Button, DatePicker, Select, Table, Tag, Tooltip, type TableColumnsType } from 'antd'
import { Activity, Clock3, Gauge, RotateCw, Timer, TrendingUp } from 'lucide-react'
import PageHeader from '@/components/PageHeader'
import { RoleTag } from '@/components/StatusTag'
import { metricApi } from '@/api/devops'
import type { MetricRow, StageStatRow } from '@/api/types-devops'
import { COL } from '@/lib/table'

const STAGE_LABEL: Record<number, string> = { 1: '构建', 2: '单元测试', 3: '代码扫描', 4: '部署', 5: '门禁', 6: '审批' }

/** 前端 CSV 下载 */
function downloadCsv(rows: MetricRow[], fileName: string) {
  const header = ['统计日', '应用ID', '部署频率', '变更前置时间(s)', '变更失败率', 'MTTR(s)', '数据状态']
  const lines = rows.map((r) => [
    r.statDate ?? '', r.appId ?? '', r.deployFreq ?? '', r.leadTimeP50 ?? '',
    r.changeFailRate ?? '', r.mttrP50 ?? '', r.dataStatus === 2 ? '数据不足' : '正常',
  ].map((v) => `"${String(v).replace(/"/g, '""')}"`).join(','))
  const blob = new Blob(['\ufeff' + [header.join(','), ...lines].join('\n')], { type: 'text/csv;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url; a.download = fileName
  document.body.appendChild(a); a.click(); document.body.removeChild(a)
  URL.revokeObjectURL(url)
}

export default function DeliveryMetric() {
  const { message } = App.useApp()
  const [filters, setFilters] = useState<Record<string, unknown>>({})
  const [draft, setDraft] = useState<Record<string, unknown>>({})
  const [page, setPage] = useState({ current: 1, size: 30 })

  const doraQuery = useQuery({
    queryKey: ['metric-dora', filters],
    queryFn: () => metricApi.dora({ ...filters, ...page }),
  })
  const stageQuery = useQuery({
    queryKey: ['metric-stage', filters, page],
    queryFn: () => metricApi.stageDuration({ ...filters, ...page }),
  })

  const doraRows = doraQuery.data?.records ?? []
  const latest = doraRows[0]
  const insufficient = !latest || latest.dataStatus === 2
  const cardValue = (key: keyof MetricRow, unit: string) => {
    if (insufficient) return '数据不足'
    const v = latest?.[key]
    if (v === undefined || v === null || v === '') return '—'
    return `${v}${unit}`
  }
  const cards = [
    { key: 'deploy', label: '部署频率', value: cardValue('deployFreq', ' 次/周'), hint: '成功发布至生产的次数', icon: <TrendingUp size={18} />, tone: 'primary' },
    { key: 'lead', label: '变更前置时间', value: cardValue('leadTimeP50', ' s'), hint: '提交合入至发布成功 P50', icon: <Timer size={18} />, tone: 'success' },
    { key: 'fail', label: '变更失败率', value: cardValue('changeFailRate', ''), hint: '回滚或 24h 热修占比', icon: <Activity size={18} />, tone: 'warning' },
    { key: 'mttr', label: '平均恢复时长', value: cardValue('mttrP50', ' s'), hint: '告警至服务恢复 P50', icon: <Clock3 size={18} />, tone: 'default' },
  ]

  const metricColumns: TableColumnsType<MetricRow> = [
    { title: '统计日', dataIndex: 'statDate', width: COL.time,
      render: (v: string) => <span className="ferry-mono" style={{ fontSize: 12 }}>{v}</span> },
    { title: '应用ID', dataIndex: 'appId', width: COL.code },
    { title: '部署频率', dataIndex: 'deployFreq', width: 110 },
    { title: '变更前置时间(s)', dataIndex: 'leadTimeP50', width: 150 },
    { title: '变更失败率', dataIndex: 'changeFailRate', width: 120 },
    { title: 'MTTR(s)', dataIndex: 'mttrP50', width: 110 },
    { title: '门禁豁免率', dataIndex: 'gateExemptRate', width: 120 },
    { title: '数据状态', dataIndex: 'dataStatus', width: 100,
      render: (v: number) => <Tag color={v === 2 ? 'orange' : 'green'}>{v === 2 ? '数据不足' : '正常'}</Tag> },
  ]

  const stageColumns: TableColumnsType<StageStatRow> = [
    { title: '统计日', dataIndex: 'statDate', width: COL.time },
    { title: '应用ID', dataIndex: 'appId', width: COL.code },
    { title: '阶段', dataIndex: 'stageType', width: 110,
      render: (v: number) => STAGE_LABEL[v] ?? `阶段${v}` },
    { title: 'P50(s)', dataIndex: 'p50', width: 110,
      render: (v?: number) => v === undefined ? '—' : `${(v / 1000).toFixed(1)}` },
    { title: 'P90(s)', dataIndex: 'p90', width: 110,
      render: (v?: number) => v === undefined ? '—' : `${(v / 1000).toFixed(1)}` },
    { title: '失败数', dataIndex: 'failCount', width: 90 },
  ]

  return (
    <>
      <PageHeader title="交付度量" description="DORA 四指标口径固定：部署频率、变更前置时间、变更失败率、平均恢复时长。样本不足展示「数据不足」而非 0。"
        tag={<RoleTag>{insufficient ? '数据不足' : '数据正常'}</RoleTag>}
        extra={
          <>
            <Tooltip title="口径：部署频率=单应用每周成功生产发布次数；前置时间取 P50；失败率含回滚与热修；MTTR 取 P50">
              <Button icon={<Gauge size={14} />}
                onClick={() => message.info('口径：部署频率=每周成功生产发布次数；前置时间取 P50；失败率含回滚与 24h 热修；MTTR 取 P50')}>口径说明</Button>
            </Tooltip>
            <Button icon={<RotateCw size={14} />} onClick={() => {
              downloadCsv(doraRows, 'DORA明细.csv')
            }}>导出 CSV</Button>
          </>
        }
      />

      <div className="ferry-stat-grid">
        {cards.map((card) => (
          <div className="ferry-stat" data-tone={card.tone} key={card.key}>
            <span className="ferry-stat__stripe" />
            <span className="ferry-stat__label" style={{ display: 'flex', alignItems: 'center', gap: 6 }}>{card.icon}{card.label}</span>
            <span className="ferry-stat__value" style={{ fontSize: 22 }}>{card.value}</span>
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
          <Select allowClear mode="multiple" placeholder="应用" style={{ width: 220, minWidth: 160 }}
            value={draft.appIds as string[]}
            onChange={(values) => setDraft((prev) => ({ ...prev, appIds: values }))}
            options={doraRows.map((r) => ({ value: String(r.appId), label: `应用 ${r.appId}` }))} />
          <Button type="primary" icon={<RotateCw size={15} />}
            onClick={() => { setFilters(draft); setPage((p) => ({ ...p, current: 1 })) }}>查询</Button>
          <Button icon={<RotateCw size={15} />}
            onClick={() => { setDraft({}); setFilters({}); setPage((p) => ({ ...p, current: 1 })) }}>重置</Button>
        </div>
        <div className="ferry-table-wrap">
          <Table<MetricRow> rowKey="id" columns={metricColumns} dataSource={doraRows}
            loading={doraQuery.isLoading || doraQuery.isFetching} scroll={{ x: 'max-content' }} />
        </div>
      </section>

      <section className="ferry-panel" style={{ marginTop: 16 }}>
        <div className="ferry-panel__head">
          <span className="ferry-panel__title">阶段耗时与门禁健康</span>
          <span className="ferry-panel__extra">
            <span className="ferry-caption">P50/P90 按阶段类型分组 · 单位秒</span>
          </span>
        </div>
        <div className="ferry-table-wrap">
          <Table<StageStatRow> rowKey="id" columns={stageColumns} dataSource={stageQuery.data?.records ?? []}
            loading={stageQuery.isLoading || stageQuery.isFetching} scroll={{ x: 'max-content' }}
            pagination={{
              current: page.current, pageSize: page.size, total: stageQuery.data?.total ?? 0,
              showSizeChanger: true, pageSizeOptions: ['30', '100', '200'],
              showTotal: (total) => `共 ${total} 条`, onChange: (current, size) => setPage({ current, size }),
            }} />
        </div>
      </section>
    </>
  )
}
