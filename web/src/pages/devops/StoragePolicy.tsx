import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { App, Button, Form, Input, InputNumber, Modal, Space, Table, Tag, Tooltip, type TableColumnsType } from 'antd'
import { Pencil, RotateCw, Save } from 'lucide-react'
import PageHeader from '@/components/PageHeader'
import { RoleTag } from '@/components/StatusTag'
import { systemExtApi } from '@/api/devops'
import type { StoragePolicyRow } from '@/api/types-devops'
import { useAuthStore } from '@/stores/auth'
import { showError } from '@/lib/notify'
import { COL } from '@/lib/table'

export default function StoragePolicy() {
  const { message } = App.useApp()
  const queryClient = useQueryClient()
  const can = useAuthStore((state) => state.can)
  const [form] = Form.useForm()
  const [open, setOpen] = useState(false)
  const [editing, setEditing] = useState<StoragePolicyRow | null>(null)
  const [page, setPage] = useState({ current: 1, size: 30 })

  // 后端 storage policy 表（sys_storage_policy）随 system 域 M9 落地，当前 API 返回空列表
  const query = useQuery({
    queryKey: ['storage-policies', page],
    queryFn: () => systemExtApi.storagePolicyPage({ ...page }),
  })

  const saveMutation = useMutation({
    mutationFn: (values: Record<string, unknown>) => systemExtApi.storagePolicySave(values),
    onSuccess: (text) => {
      message.success(text || '保存成功')
      setOpen(false)
      void queryClient.invalidateQueries({ queryKey: ['storage-policies'] })
    },
    onError: showError,
  })

  const openEditor = (record: StoragePolicyRow | null) => {
    setEditing(record)
    form.resetFields()
    if (record) {
      form.setFieldsValue({ ...record })
    } else {
      form.setFieldsValue({ scope: 'global', retentionDays: 30, maxSizeGb: 100 })
    }
    setOpen(true)
  }

  const columns: TableColumnsType<StoragePolicyRow> = [
    { title: '策略范围', dataIndex: 'scope', width: 160,
      render: (value: string) => <Tag color={value === 'global' ? 'blue' : 'default'}>{value === 'global' ? '全局默认' : value}</Tag>,
    },
    { title: '保留天数', dataIndex: 'retentionDays', width: 120,
      render: (value: number) => `${value ?? 0} 天`,
    },
    { title: '容量上限', dataIndex: 'maxSizeGb', width: 120,
      render: (value: number) => `${value ?? 0} GB`,
    },
    { title: '说明', dataIndex: 'description',
      render: (value: string) => value || <span style={{ color: 'var(--text-3)' }}>—</span>,
    },
    { title: '操作', key: 'action', width: COL.action, fixed: 'right',
      render: (_, record) => (
        can('system:storage:save') && (
          <Tooltip title="编辑">
            <Button type="link" size="small" icon={<Pencil size={14} />} onClick={() => openEditor(record)} />
          </Tooltip>
        )
      ),
    },
  ]

  return (
    <>
      <PageHeader title="存储与保留策略"
        description="制品保留、软删除、清理任务与归档策略统一在此维护。策略变更写入审计并在下一次清理任务生效。"
        tag={<RoleTag>共 {query.data?.total ?? 0} 项</RoleTag>}
        extra={can('system:storage:save') && (
          <Space>
            <Button icon={<RotateCw size={14} />} onClick={() => void queryClient.invalidateQueries({ queryKey: ['storage-policies'] })}>刷新</Button>
            <Button type="primary" icon={<Save size={14} />} onClick={() => openEditor(null)}>配置策略</Button>
          </Space>
        )}
      />
      <section className="ferry-panel">
        <div className="ferry-toolbar">
          <span style={{ color: 'var(--text-3)', fontSize: 12 }}>
            策略表由 system 域维护（sys_storage_policy），当前版本尚未落库；可先通过下方表单预览配置口径。
          </span>
        </div>
        <div className="ferry-table-wrap">
          <Table<StoragePolicyRow> rowKey="id" columns={columns} dataSource={query.data?.records ?? []}
            loading={query.isLoading || query.isFetching} scroll={{ x: 'max-content' }}
            pagination={{
              current: page.current, pageSize: page.size, total: query.data?.total ?? 0,
              showSizeChanger: true, pageSizeOptions: ['30', '100', '200'],
              showTotal: (total) => `共 ${total} 条`, onChange: (current, size) => setPage({ current, size }),
            }}
          />
        </div>
      </section>

      <Modal open={open} title={editing ? `编辑策略 · ${editing.scope}` : '配置存储策略'}
        okText="保存" cancelText="取消" width={560} confirmLoading={saveMutation.isPending}
        onCancel={() => setOpen(false)}
        onOk={async () => {
          const values = await form.validateFields()
          saveMutation.mutate({ ...values, id: editing?.id })
        }}
        destroyOnHidden>
        <Form form={form} layout="vertical" requiredMark="optional">
          <Form.Item name="scope" label="策略范围" rules={[{ required: true, message: '请输入策略范围' }]}
            extra="global 表示全局默认，也可填写应用编码按应用覆盖">
            <Input placeholder="global 或应用编码" />
          </Form.Item>
          <Form.Item name="retentionDays" label="软删除保留天数" rules={[{ required: true, message: '请输入保留天数' }]}
            extra="不得低于 30 天下限">
            <InputNumber min={30} style={{ width: '100%' }} addonAfter="天" />
          </Form.Item>
          <Form.Item name="maxSizeGb" label="容量上限">
            <InputNumber min={1} style={{ width: '100%' }} addonAfter="GB" />
          </Form.Item>
          <Form.Item name="description" label="说明">
            <Input.TextArea rows={3} placeholder="保留口径与归档策略说明" />
          </Form.Item>
        </Form>
      </Modal>
    </>
  )
}
