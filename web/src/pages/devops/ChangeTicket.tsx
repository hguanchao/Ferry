import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { App, Button, Form, Input, Modal, Select, Space, Tag, Tooltip, type TableColumnsType } from 'antd'
import FerryTable from '@/components/FerryTable'
import { Ban, Check, Plus, RotateCw, Search, Send, X } from 'lucide-react'
import PageHeader from '@/components/PageHeader'
import { RoleTag } from '@/components/StatusTag'
import { appApi, changeTicketApi } from '@/api/devops'
import type { ChangeTicketRow } from '@/api/types-devops'
import { useAuthStore } from '@/stores/auth'
import { showError } from '@/lib/notify'
const STATUS: Record<number, { text: string; color: string }> = {
  1: { text: '草稿', color: 'default' }, 2: { text: '待审批', color: 'gold' },
  3: { text: '已批准', color: 'green' }, 4: { text: '已实施', color: 'blue' },
  5: { text: '已关闭', color: 'default' }, 6: { text: '已取消', color: 'default' },
}
const TYPE: Record<number, string> = { 1: '常规', 2: '紧急', 3: '标准' }

export default function ChangeTicket() {
  const { message, modal } = App.useApp()
  const queryClient = useQueryClient()
  const can = useAuthStore((s) => s.can)
  const [form] = Form.useForm()
  const [open, setOpen] = useState(false)
  const [filters, setFilters] = useState<Record<string, unknown>>({})
  const [draft, setDraft] = useState<Record<string, unknown>>({})
  const [page, setPage] = useState({ current: 1, size: 30 })

  const list = useQuery({ queryKey: ['ticket', filters, page], queryFn: () => changeTicketApi.pageList({ ...filters, ...page }) })
  const apps = useQuery({ queryKey: ['app-options'], queryFn: () => appApi.options() })

  const invalidate = () => void queryClient.invalidateQueries({ queryKey: ['ticket'] })
  const saveMutation = useMutation({
    mutationFn: (body: Record<string, unknown>) => changeTicketApi.save(body),
    onSuccess: (t) => { message.success(t || '保存成功'); setOpen(false); invalidate() }, onError: showError,
  })
  const simple = (fn: () => Promise<string>, ok: string) => fn().then((t) => { message.success(t || ok); invalidate() }).catch(showError)

  const columns: TableColumnsType<ChangeTicketRow> = [
    { title: '单号', dataIndex: 'ticketNo',  render: (v: string) => <span className="ferry-mono" style={{ fontSize: 12 }}>{v}</span> },
    { title: '标题', dataIndex: 'title' },
    { title: '类型', dataIndex: 'type',  render: (v: number) => <Tag color={v === 2 ? 'red' : 'blue'}>{TYPE[v] ?? '—'}</Tag> },
    { title: '状态', dataIndex: 'status',  render: (v: number) => <Tag color={STATUS[v]?.color}>{STATUS[v]?.text ?? '—'}</Tag> },
    { title: '申请人', dataIndex: 'applicant', },
    { title: '审批人', dataIndex: 'approver',  render: (v?: string) => v ?? '—' },
    { title: '创建时间', dataIndex: 'createTime',  render: (v: string) => <span className="ferry-mono" style={{ fontSize: 12, color: 'var(--text-3)' }}>{v ?? '—'}</span> },
    { title: '操作', key: 'action',   render: (_, r) => (
      <Space size={2}>
        {r.status === 1 && can('devops:changeTicket:submit') && (
          <Tooltip title="提交">
            <Button type="link" size="small" icon={<Send size={14} />} onClick={() => simple(() => changeTicketApi.submit(r.id), '提交成功')} />
          </Tooltip>
        )}
        {r.status === 2 && can('devops:changeTicket:approve') && (
          <>
            <Tooltip title="通过">
              <Button type="link" size="small" icon={<Check size={14} />} onClick={() => modal.confirm({
                title: '审批通过？', content: '紧急单需技术负责人（由权限约束）。',
                onOk: (): Promise<unknown> => changeTicketApi.approve({ id: r.id, pass: true, opinion: '同意变更' }),
              })} />
            </Tooltip>
            <Tooltip title="驳回">
              <Button type="link" size="small" danger icon={<X size={14} />} onClick={() => modal.confirm({
                title: '驳回？', onOk: (): Promise<unknown> => changeTicketApi.approve({ id: r.id, pass: false, opinion: '驳回变更' }),
              })} />
            </Tooltip>
          </>
        )}
        {r.status === 3 && can('devops:changeTicket:close') && (
          <Tooltip title="关闭">
            <Button type="link" size="small" icon={<X size={14} />} onClick={() => simple(() => changeTicketApi.close(r.id), '已关闭')} />
          </Tooltip>
        )}
        {can('devops:changeTicket:cancel') && ![5, 6].includes(r.status) && (
          <Tooltip title="取消">
            <Button type="link" size="small" icon={<Ban size={14} />} onClick={() => modal.confirm({
              title: '取消变更单？', onOk: (): Promise<unknown> => changeTicketApi.cancel(r.id),
            })} />
          </Tooltip>
        )}
      </Space>
    ) },
  ]

  return (
    <>
      <PageHeader title="变更单" description="生产部署/发布的前置校验依据。支持常规、紧急、标准三类与审批流。"
        tag={<RoleTag>共 {list.data?.total ?? 0} 项</RoleTag>}
        extra={can('devops:changeTicket:save') && <Button type="primary" icon={<Plus size={14} />} onClick={() => { form.resetFields(); setOpen(true) }}>新建变更单</Button>} />
      <section className="ferry-panel">
        <div className="ferry-toolbar">
          <Input allowClear prefix={<Search size={15} />} placeholder="单号/标题" style={{ width: 180 }} value={draft.keyword as string}
            onChange={(e) => setDraft({ ...draft, keyword: e.target.value })} />
          <Select allowClear placeholder="状态" style={{ width: 120 }} value={draft.status as number}
            onChange={(v) => setDraft({ ...draft, status: v })} options={Object.entries(STATUS).map(([k, v]) => ({ value: Number(k), label: v.text }))} />
          <Select allowClear placeholder="类型" style={{ width: 110 }} value={draft.type as number}
            onChange={(v) => setDraft({ ...draft, type: v })} options={[1, 2, 3].map((v) => ({ value: v, label: TYPE[v] }))} />
          <Button type="primary" icon={<Search size={15} />} onClick={() => { setFilters(draft); setPage((p) => ({ ...p, current: 1 })) }}>查询</Button>
          <Button icon={<RotateCw size={15} />} onClick={() => { setDraft({}); setFilters({}); setPage((p) => ({ ...p, current: 1 })) }}>重置</Button>
        </div>
        <div className="ferry-table-wrap">
          <FerryTable<ChangeTicketRow> rowKey="id" columns={columns} dataSource={list.data?.records ?? []}
            loading={list.isLoading || list.isFetching} scroll={{ x: 'max-content' }}
            pagination={{ current: page.current, pageSize: page.size, total: list.data?.total ?? 0, showSizeChanger: true,
              pageSizeOptions: ['30', '100', '200'], showTotal: (t) => `共 ${t} 条`, onChange: (c, s) => setPage({ current: c, size: s }) }} />
        </div>
      </section>

      <Modal open={open} title="新建变更单" okText="保存" cancelText="取消" width={600} confirmLoading={saveMutation.isPending}
        onCancel={() => setOpen(false)} onOk={() => form.validateFields().then((v) => saveMutation.mutate({ ...v, appIds: Array.isArray(v.appIds) ? v.appIds.join(',') : v.appIds }))} destroyOnHidden>
        <Form form={form} layout="vertical" requiredMark="optional" initialValues={{ type: 1 }}>
          <Form.Item name="title" label="标题" rules={[{ required: true }]}><Input maxLength={200} /></Form.Item>
          <Form.Item name="type" label="类型" rules={[{ required: true }]}><Select options={[1, 2, 3].map((v) => ({ value: v, label: TYPE[v] }))} /></Form.Item>
          <Form.Item name="appIds" label="关联应用"><Select mode="multiple" allowClear options={apps.data?.map((a) => ({ value: a.value, label: a.label }))} /></Form.Item>
          <Form.Item name="windowStart" label="计划窗口开始" rules={[{ required: true }]}><Input placeholder="yyyy-MM-dd HH:mm:ss" /></Form.Item>
          <Form.Item name="windowEnd" label="计划窗口结束" rules={[{ required: true }]}><Input placeholder="yyyy-MM-dd HH:mm:ss" /></Form.Item>
          <Form.Item name="content" label="变更内容"><Input.TextArea rows={2} /></Form.Item>
          <Form.Item name="rollbackPlan" label="回滚方案"><Input.TextArea rows={2} /></Form.Item>
        </Form>
      </Modal>
    </>
  )
}
