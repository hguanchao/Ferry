import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { Input, Modal, Table, Tag, type TableColumnsType } from 'antd'
import { Search } from 'lucide-react'
import { pipelineTemplateApi } from '@/api/devops'
import type { PipelineTemplateRow } from '@/api/types'

interface TemplatePickerModalProps {
  open: boolean
  onClose: () => void
  /** 选中模板后回调，选择即确认 */
  onPicked: (template: PipelineTemplateRow) => void
}

const columns: TableColumnsType<PipelineTemplateRow> = [
  {
    title: '模板名称',
    dataIndex: 'name',
    render: (value: string, record) => (
      <span className="ferry-hstack" style={{ gap: 6 }}>
        <span style={{ fontWeight: 550 }}>{value}</span>
        <Tag>{record.scope === 0 ? '组织级' : '团队级'}</Tag>
      </span>
    ),
  },
  { title: '适用场景', dataIndex: 'description', ellipsis: true, render: (value?: string) => value || '—' },
  { title: '阶段数', dataIndex: 'stageCount', width: 70 },
  {
    title: '版本',
    dataIndex: 'currentVersionNo',
    width: 70,
    render: (value?: number | null) => (value ? <span className="ferry-mono">V{value}</span> : '—'),
  },
]

/** 从模板创建的选择弹窗：只列启用中的模板，点击行即选用 */
export default function TemplatePickerModal({ open, onClose, onPicked }: TemplatePickerModalProps) {
  const [keyword, setKeyword] = useState('')

  const optionsQuery = useQuery({
    queryKey: ['template-options', keyword],
    queryFn: () => pipelineTemplateApi.options(keyword.trim() || undefined),
    enabled: open,
  })

  return (
    <Modal
      open={open}
      width={720}
      title="从模板创建"
      footer={null}
      onCancel={onClose}
      destroyOnHidden
    >
      <Input
        allowClear
        prefix={<Search size={15} style={{ color: 'var(--text-4)' }} />}
        placeholder="搜索模板名称"
        style={{ marginBottom: 12 }}
        value={keyword}
        onChange={(event) => setKeyword(event.target.value)}
      />
      <Table<PipelineTemplateRow>
        rowKey="id"
        size="small"
        columns={columns}
        dataSource={optionsQuery.data ?? []}
        loading={optionsQuery.isLoading || optionsQuery.isFetching}
        pagination={false}
        onRow={(record) => ({
          onClick: () => onPicked(record),
          style: { cursor: 'pointer' },
        })}
        scroll={{ x: 'max-content' }}
      />
      <div style={{ marginTop: 8, fontSize: 12, color: 'var(--text-3)' }}>
        按模板生成一份独立副本，占位变量替换为实际值后可继续调整；模板后续更新不影响已创建的流水线。
      </div>
    </Modal>
  )
}
