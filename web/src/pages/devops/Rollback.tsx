import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { App, Button, Form, Input, InputNumber, Modal, Select, Space, Table, Tag, type TableColumnsType } from 'antd'
import { RotateCcw, Search } from 'lucide-react'
import PageHeader from '@/components/PageHeader'
import { RoleTag } from '@/components/StatusTag'
import { deployApi, envApi } from '@/api/devops'
import type { RollbackCandidate, RollbackRow } from '@/api/types-devops'
import { useAuthStore } from '@/stores/auth'
import { showError } from '@/lib/notify'
import { COL } from '@/lib/table'

export default function Rollback() {
  const { message, modal } = App.useApp()
  const queryClient = useQueryClient()
  const can = useAuthStore((s) => s.can)
  const [form] = Form.useForm()
  const [open, setOpen] = useState(false)
  const [envId, setEnvId] = useState<string>()
  const [filters, setFilters] = useState<Record<string, unknown>>({})
  const [draft, setDraft] = useState<Record<string, unknown>>({})
  const [page, setPage] = useState({ current: 1, size: 30 })

  const list = useQuery({ queryKey: ['rollback', filters, page], queryFn: () => deployApi.rollbackPage({ ...filters, ...page }) })
  const envs = useQuery({ queryKey: ['env-options'], queryFn: () => envApi.options() })
  const candidates = useQuery({
    queryKey: ['rollback-candidates', envId],
    queryFn: () => deployApi.rollbackCandidates(envId!),
    enabled: !!envId,
  })

  const rollback = useMutation({
    mutationFn: (body: Record<string, unknown>) => deployApi.rollback(body),
    onSuccess: () => { message.success('回滚已发起'); setOpen(false); void queryClient.invalidateQueries({ queryKey: ['rollback'] }) },
    onError: showError,
  })

  const openModal = () => {
    form.resetFields()
    setEnvId(undefined)
    setOpen(true)
  }

  const columns: TableColumnsType<RollbackRow> = [
    { title: '环境', dataIndex: 'envId', width: 140, render: (v: string) => envs.data?.find((e) => e.value === v)?.label ?? v },
    { title: '目标制品', dataIndex: 'artifactId', width: 120 },
    { title: '版本', dataIndex: 'artifactVersion', width: 140 },
    { title: '原因', dataIndex: 'reason' },
    { title: '结果', dataIndex: 'result', width: 90, render: (v: number) => <Tag color={v === 1 ? 'green' : 'red'}>{v === 1 ? '成功' : '失败'}</Tag> },
    { title: '操作人', dataIndex: 'operator', width: 100 },
    { title: '时间', dataIndex: 'createTime', width: COL.time, render: (v: string) => <span className="ferry-mono" style={{ fontSize: 12 }}>{v ?? '—'}</span> },
  ]

  return (
    <>
      <PageHeader title="回滚记录" description="仅可选择健康版本（当前生效或历史健康制品），生产回滚需不同确认人。"
        tag={<RoleTag>共 {list.data?.total ?? 0} 项</RoleTag>}
        extra={can('devops:rollback:start') && <Button type="primary" icon={<RotateCcw size={14} />} onClick={openModal}>发起回滚</Button>} />
      <section className="ferry-panel">
        <div className="ferry-toolbar">
          <Input allowClear prefix={<Search size={15} />} placeholder="环境id" style={{ width: 160 }} value={draft.envId as string}
            onChange={(e) => setDraft({ ...draft, envId: e.target.value })} />
          <Select allowClear placeholder="结果" style={{ width: 110 }} value={draft.result as number}
            onChange={(v) => setDraft({ ...draft, result: v })} options={[{ value: 1, label: '成功' }, { value: 2, label: '失败' }]} />
          <Button type="primary" icon={<Search size={15} />} onClick={() => { setFilters(draft); setPage((p) => ({ ...p, current: 1 })) }}>查询</Button>
        </div>
        <div className="ferry-table-wrap">
          <Table<RollbackRow> rowKey="id" columns={columns} dataSource={list.data?.records ?? []}
            loading={list.isLoading || list.isFetching} scroll={{ x: 'max-content' }}
            pagination={{ current: page.current, pageSize: page.size, total: list.data?.total ?? 0, showSizeChanger: true,
              pageSizeOptions: ['30', '100', '200'], showTotal: (t) => `共 ${t} 条`, onChange: (c, s) => setPage({ current: c, size: s }) }} />
        </div>
      </section>

      <Modal open={open} title="发起回滚" okText="回滚" cancelText="取消" width={560} confirmLoading={rollback.isPending}
        onCancel={() => setOpen(false)} onOk={() => form.validateFields().then((v) => rollback.mutate(v))} destroyOnHidden>
        <Form form={form} layout="vertical" requiredMark="optional">
          <Form.Item name="envId" label="环境" rules={[{ required: true, message: '请选择环境' }]}>
            <Select showSearch placeholder="选择环境" options={envs.data?.map((e) => ({ value: e.value, label: e.label }))}
              onChange={(v) => setEnvId(String(v))} />
          </Form.Item>
          <Form.Item name="artifactId" label="回滚目标制品（仅健康版本）" rules={[{ required: true, message: '请选择制品' }]}>
            <Select showSearch placeholder={envId ? '选择健康版本' : '请先选择环境'}
              options={candidates.data?.map((c: RollbackCandidate) => ({ value: c.artifactId, label: `${c.version} · ${c.digest}` }))} />
          </Form.Item>
          <Form.Item name="deployId" label="关联部署id"><InputNumber style={{ width: '100%' }} placeholder="可选" /></Form.Item>
          <Form.Item name="confirmer" label="确认人用户id（生产必填且不同于操作人）"><InputNumber style={{ width: '100%' }} /></Form.Item>
          <Form.Item name="reason" label="回滚原因" rules={[{ required: true, message: '请填写原因' }]}>
            <Input.TextArea rows={2} />
          </Form.Item>
        </Form>
        <Space style={{ marginTop: 8 }}>
          <Button size="small" onClick={() => modal.info({
            title: '回滚时效', content: 'SSH 承诺 30 分钟、Docker 10 分钟、Kubernetes 5 分钟；近 5 次实际耗时超承诺会在环境页提示。',
          })}>查看回滚时效承诺</Button>
        </Space>
      </Modal>
    </>
  )
}
