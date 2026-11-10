import { useMemo, useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import {
  Alert,
  App,
  Button,
  DatePicker,
  Descriptions,
  Drawer,
  Input,
  Select,
  Table,
  Tag,
  Tooltip,
  type TableColumnsType,
} from 'antd'
import { ExportOutlined, FileSearchOutlined, ReloadOutlined, SearchOutlined } from '@ant-design/icons'
import dayjs, { type Dayjs } from 'dayjs'
import PageHeader from '@/components/PageHeader'
import { RoleTag } from '@/components/StatusTag'
import { auditApi } from '@/api/system'
import type { AuditRow } from '@/api/types'
import { exportCsv } from '@/lib/export'
import { useAuthStore } from '@/stores/auth'
import { showError } from '@/lib/notify'

const TYPES = ['新增', '修改', '删除', '授权', '导出', '登录']
const OBJECTS = ['用户', '角色', '菜单', '部门', '岗位', '字典', '参数配置', '区域', '租户', '个人资料', '会话', '审计日志']

const TYPE_COLOR: Record<string, string> = {
  新增: 'green',
  修改: 'blue',
  删除: 'red',
  授权: 'gold',
  导出: 'purple',
  登录: 'cyan',
}

function pretty(value?: string | null) {
  if (!value) return ''
  try {
    return JSON.stringify(JSON.parse(value), null, 2)
  } catch {
    return value
  }
}

export default function Audit() {
  const { message } = App.useApp()
  const can = useAuthStore((state) => state.can)
  const user = useAuthStore((state) => state.user)

  const [range, setRange] = useState<[Dayjs, Dayjs]>([dayjs().subtract(6, 'day'), dayjs()])
  const [draft, setDraft] = useState<{ operator?: string; operateType?: string; objectType?: string; result?: string; keyword?: string }>({})
  const [filters, setFilters] = useState<typeof draft>({})
  const [page, setPage] = useState({ current: 1, size: 30 })
  const [detail, setDetail] = useState<AuditRow | null>(null)

  const queryParams = useMemo(
    () => ({
      ...filters,
      startTime: range?.[0]?.format('YYYY-MM-DD'),
      endTime: range?.[1]?.format('YYYY-MM-DD'),
    }),
    [filters, range],
  )

  const query = useQuery({
    queryKey: ['audits', queryParams, page],
    queryFn: () => auditApi.pageList({ ...queryParams, ...page }),
  })

  const filterSummary = useMemo(() => {
    const parts = [
      `${range?.[0]?.format('YYYY-MM-DD') ?? '不限'} ~ ${range?.[1]?.format('YYYY-MM-DD') ?? '不限'}`,
      filters.operator && `操作人 ${filters.operator}`,
      filters.operateType && `类型 ${filters.operateType}`,
      filters.objectType && `对象 ${filters.objectType}`,
      filters.result && `结果 ${filters.result}`,
      filters.keyword && `关键词 ${filters.keyword}`,
    ]
    return parts.filter(Boolean).join('；')
  }, [filters, range])

  const doExport = async () => {
    try {
      const result = await auditApi.pageList({ ...queryParams, current: 1, size: 1000 })
      exportCsv<AuditRow>({
        filename: '审计日志',
        rows: result.records,
        operator: user?.nickname ?? '未知',
        filters: filterSummary,
        columns: [
          { title: '日志ID', value: (row) => row.id },
          { title: '操作时间', value: (row) => row.operateTime },
          { title: '操作人', value: (row) => `${row.operator}（${row.operatorAccount}）` },
          { title: '操作类型', value: (row) => row.operateType },
          { title: '对象类型', value: (row) => row.objectType },
          { title: '对象ID', value: (row) => row.objectId },
          { title: '对象名称', value: (row) => row.objectName },
          { title: '动作', value: (row) => row.action },
          { title: '变更前值', value: (row) => row.beforeValue ?? '' },
          { title: '变更后值', value: (row) => row.afterValue ?? '' },
          { title: '来源IP', value: (row) => row.ip },
          { title: '关联单号', value: (row) => row.ticketNo ?? '' },
          { title: '结果', value: (row) => row.result },
        ],
      })
      await auditApi.exportLog(result.records.length, filterSummary)
      void query.refetch()
      message.success(`已导出 ${result.records.length} 条，导出动作已记入审计`)
    } catch (error) {
      showError(error)
    }
  }

  const columns: TableColumnsType<AuditRow> = [
    {
      title: '操作时间',
      dataIndex: 'operateTime',
      width: 154,

      render: (value: string) => (
        <span className="ferry-mono" style={{ fontSize: 12 }}>
          {value}
        </span>
      ),
    },
    {
      title: '操作人',
      dataIndex: 'operator',
      width: 140,

      render: (_, record) => (
        <div style={{ lineHeight: 1.4 }}>
          <div style={{ fontWeight: 500, color: 'var(--text-1)' }}>{record.operator}</div>
          <div className="ferry-mono" style={{ fontSize: 11, color: 'var(--text-3)' }}>
            {record.operatorAccount}
          </div>
        </div>
      ),
    },
    {
      title: '类型',
      dataIndex: 'operateType',
      width: 84,

      render: (value: string) => (
        <Tag color={TYPE_COLOR[value] ?? 'default'} style={{ fontSize: 11 }}>
          {value}
        </Tag>
      ),
    },
    {
      title: '操作对象',
      dataIndex: 'objectName',
      width: 215,

      render: (_, record) => (
        <div style={{ lineHeight: 1.4 }}>
          <div style={{ color: 'var(--text-1)' }}>
            <span style={{ color: 'var(--text-3)' }}>{record.objectType} · </span>
            {record.objectName}
          </div>
          <div className="ferry-mono" style={{ fontSize: 11, color: 'var(--text-4)' }}>
            {record.objectId}
          </div>
        </div>
      ),
    },
    {
      title: '动作',
      dataIndex: 'action',
      width: 258,
      ellipsis: true,
      render: (value: string) => <span style={{ color: 'var(--text-2)' }}>{value}</span>,
    },
    {
      title: '结果',
      dataIndex: 'result',
      width: 82,

      render: (value: string) => (
        <span className="ferry-status" data-tone={value === '成功' ? 'success' : 'danger'}>
          <span className="ferry-status__dot" />
          {value}
        </span>
      ),
    },
    {
      title: '来源 IP',
      dataIndex: 'ip',
      width: 121,

      render: (value: string) => (
        <span className="ferry-mono" style={{ fontSize: 12, color: 'var(--text-2)' }}>
          {value}
        </span>
      ),
    },
    {
      title: '关联单号',
      dataIndex: 'ticketNo',
      width: 138,

      render: (value: string | null) =>
        value ? (
          <span className="ferry-mono" style={{ fontSize: 11.5, color: 'var(--color-primary-ink)' }}>
            {value}
          </span>
        ) : (
          <span className="ferry-muted">—</span>
        ),
    },
  ]

  return (
    <>
      <PageHeader
        title="审计日志"
        description="全量写操作留痕：谁、何时、对什么、改了什么、从哪来、关联哪张单。审计表只插入，不提供任何修改与删除入口。"
        tag={<RoleTag>留存 365 天</RoleTag>}
        extra={
          <>
            <Tooltip title="按当前筛选条件导出，文件首行带导出人与时间水印">
              <Button icon={<ExportOutlined />} disabled={!can('system:audit:export')} onClick={() => void doExport()}>
                导出
              </Button>
            </Tooltip>
            <Button icon={<ReloadOutlined />} loading={query.isFetching} onClick={() => void query.refetch()}>
              刷新
            </Button>
          </>
        }
      />

      <Alert
        type="info"
        showIcon
        icon={<FileSearchOutlined />}
        message="审计不可篡改"
        description="凭据类字段仅记录「已变更」，不落明文；导出文件带水印，导出动作本身也会写入审计。超期数据归档至冷存储，不物理删除。"
      />

      <section className="ferry-panel">
        <div className="ferry-toolbar">
          <DatePicker.RangePicker
            value={range}
            allowClear={false}
            style={{ width: 240 }}
            onChange={(value) => {
              if (value?.[0] && value?.[1]) setRange([value[0], value[1]])
            }}
          />
          <Input
            allowClear
            prefix={<SearchOutlined style={{ color: 'var(--text-4)' }} />}
            placeholder="操作人或账号"
            style={{ width: 160 }}
            value={draft.operator}
            onChange={(event) => setDraft((prev) => ({ ...prev, operator: event.target.value }))}
            onPressEnter={() => {
              setFilters(draft)
              setPage((prev) => ({ ...prev, current: 1 }))
            }}
          />
          <Select
            allowClear
            placeholder="操作类型"
            style={{ width: 120 }}
            value={draft.operateType}
            onChange={(value) => setDraft((prev) => ({ ...prev, operateType: value }))}
            options={TYPES.map((item) => ({ value: item, label: item }))}
          />
          <Select
            allowClear
            placeholder="对象类型"
            style={{ width: 130 }}
            value={draft.objectType}
            onChange={(value) => setDraft((prev) => ({ ...prev, objectType: value }))}
            options={OBJECTS.map((item) => ({ value: item, label: item }))}
          />
          <Select
            allowClear
            placeholder="结果"
            style={{ width: 110 }}
            value={draft.result}
            onChange={(value) => setDraft((prev) => ({ ...prev, result: value }))}
            options={[
              { value: '成功', label: '成功' },
              { value: '失败', label: '失败' },
            ]}
          />
          <Input
            allowClear
            placeholder="对象名称 / 单号"
            style={{ width: 170 }}
            value={draft.keyword}
            onChange={(event) => setDraft((prev) => ({ ...prev, keyword: event.target.value }))}
          />
          <Button
            type="primary"
            icon={<SearchOutlined />}
            onClick={() => {
              setFilters(draft)
              setPage((prev) => ({ ...prev, current: 1 }))
            }}
          >
            查询
          </Button>
          <Button
            icon={<ReloadOutlined />}
            onClick={() => {
              setDraft({})
              setFilters({})
              setRange([dayjs().subtract(6, 'day'), dayjs()])
              setPage({ current: 1, size: 30 })
            }}
          >
            重置
          </Button>
        </div>

        <div className="ferry-table-wrap">
          <Table<AuditRow>
            rowKey="id"
            columns={columns}
            dataSource={query.data?.records ?? []}
            loading={query.isLoading || query.isFetching}
            scroll={{ x: 1192 }}
            onRow={(record) => ({
              onClick: () => setDetail(record),
              style: { cursor: 'pointer' },
            })}
            pagination={{
              current: page.current,
              pageSize: page.size,
              total: query.data?.total ?? 0,
              showSizeChanger: true,
              pageSizeOptions: ['30', '100', '200', '500', '1000'],
              showTotal: (total) => `共 ${total} 条`,
              onChange: (current, size) => setPage({ current, size }),
            }}
          />
        </div>
        <div className="ferry-table-footer">
          <span>点击任意行查看变更前后值</span>
          <span className="ferry-mono">{filterSummary}</span>
        </div>
      </section>

      <Drawer
        open={!!detail}
        size={640}
        title="审计详情"
        onClose={() => setDetail(null)}
        destroyOnHidden
      >
        {detail && (
          <>
            <Descriptions column={2} size="small" colon={false} styles={{ label: { color: 'var(--text-3)', width: 90 } }}>
              <Descriptions.Item label="日志 ID" span={2}>
                <span className="ferry-mono" style={{ fontSize: 12 }}>
                  {detail.id}
                </span>
              </Descriptions.Item>
              <Descriptions.Item label="操作时间">{detail.operateTime}</Descriptions.Item>
              <Descriptions.Item label="操作人">
                {detail.operator}（{detail.operatorAccount}）
              </Descriptions.Item>
              <Descriptions.Item label="操作类型">
                <Tag color={TYPE_COLOR[detail.operateType] ?? 'default'}>{detail.operateType}</Tag>
              </Descriptions.Item>
              <Descriptions.Item label="结果">
                <span className="ferry-status" data-tone={detail.result === '成功' ? 'success' : 'danger'}>
                  <span className="ferry-status__dot" />
                  {detail.result}
                </span>
              </Descriptions.Item>
              <Descriptions.Item label="对象类型">{detail.objectType}</Descriptions.Item>
              <Descriptions.Item label="对象名称">{detail.objectName}</Descriptions.Item>
              <Descriptions.Item label="对象 ID" span={2}>
                <span className="ferry-mono" style={{ fontSize: 12 }}>
                  {detail.objectId}
                </span>
              </Descriptions.Item>
              <Descriptions.Item label="动作" span={2}>
                {detail.action}
              </Descriptions.Item>
              <Descriptions.Item label="来源 IP">
                <span className="ferry-mono">{detail.ip}</span>
              </Descriptions.Item>
              <Descriptions.Item label="关联单号">
                {detail.ticketNo || <span className="ferry-muted">—</span>}
              </Descriptions.Item>
              {detail.remark && (
                <Descriptions.Item label="备注" span={2}>
                  {detail.remark}
                </Descriptions.Item>
              )}
            </Descriptions>

            <div style={{ marginTop: 18 }}>
              <div style={{ fontSize: 13, fontWeight: 600, marginBottom: 8 }}>变更前后值</div>
              {detail.beforeValue || detail.afterValue ? (
                <div className="ferry-diff">
                  <div className="ferry-diff__pane">
                    <div className="ferry-diff__head">变更前</div>
                    <pre className="ferry-diff__body">{pretty(detail.beforeValue) || '（无）'}</pre>
                  </div>
                  <div className="ferry-diff__pane">
                    <div className="ferry-diff__head">变更后</div>
                    <pre className="ferry-diff__body">{pretty(detail.afterValue) || '（无）'}</pre>
                  </div>
                </div>
              ) : (
                <div className="ferry-caption">该操作未记录变更值（如登录、查询类动作）。</div>
              )}
            </div>

            <div
              style={{
                marginTop: 18,
                padding: 12,
                borderRadius: 8,
                background: 'var(--bg-subtle)',
                border: '1px dashed var(--border-1)',
                fontSize: 12,
                color: 'var(--text-3)',
                lineHeight: 1.7,
              }}
            >
              本条记录不可修改、不可删除。业务对象即使被删除，其历史变更仍可在此查询；如需调阅超期归档数据，请提交审计调阅申请。
            </div>
          </>
        )}
      </Drawer>
    </>
  )
}
