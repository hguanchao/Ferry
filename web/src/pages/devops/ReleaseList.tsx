import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useNavigate } from 'react-router-dom'
import { App, Button, Form, Input, Modal, Select, Space, Tag, Tooltip, type TableColumnsType } from 'antd'
import FerryTable from '@/components/FerryTable'
import { Eye, Play, Plus, RotateCw, Search, Send, Trash2, Undo2 } from 'lucide-react'
import PageHeader from '@/components/PageHeader'
import { RoleTag } from '@/components/StatusTag'
import { changeTicketApi, releaseApi } from '@/api/devops'
import type { ReleaseRow } from '@/api/types-devops'
import { useAuthStore } from '@/stores/auth'
import { showError } from '@/lib/notify'
const STATUS: Record<number, { text: string; color: string }> = {
  1: { text: '草稿', color: 'default' }, 2: { text: '待审批', color: 'gold' },
  3: { text: '已批准', color: 'blue' }, 4: { text: '发布中', color: 'processing' },
  5: { text: '观察中', color: 'cyan' }, 6: { text: '已完成', color: 'green' },
  7: { text: '已回滚', color: 'volcano' }, 8: { text: '已驳回', color: 'red' }, 9: { text: '已取消', color: 'default' },
}
const RISK: Record<number, { text: string; color: string }> = { 1: { text: '低', color: 'green' }, 2: { text: '中', color: 'gold' }, 3: { text: '高', color: 'red' } }

export default function ReleaseList() {
  const { message, modal } = App.useApp()
  const queryClient = useQueryClient()
  const navigate = useNavigate()
  const can = useAuthStore((s) => s.can)
  const [form] = Form.useForm()
  const [open, setOpen] = useState(false)
  const [filters, setFilters] = useState<Record<string, unknown>>({})
  const [draft, setDraft] = useState<Record<string, unknown>>({})
  const [page, setPage] = useState({ current: 1, size: 30 })

  const list = useQuery({ queryKey: ['release', filters, page], queryFn: () => releaseApi.pageList({ ...filters, ...page }) })
  const tickets = useQuery({ queryKey: ['ticket-page'], queryFn: () => changeTicketApi.pageList({ current: 1, size: 100 }) })

  const invalidate = () => void queryClient.invalidateQueries({ queryKey: ['release'] })
  const saveMutation = useMutation({
    mutationFn: (body: Record<string, unknown>) => releaseApi.save(body),
    onSuccess: (t) => { message.success(t || '保存成功'); setOpen(false); invalidate() }, onError: showError,
  })
  const simple = (fn: () => Promise<string>, ok: string) => fn().then((t) => { message.success(t || ok); invalidate() }).catch(showError)

  const columns: TableColumnsType<ReleaseRow> = [
    { title: '发布单号', dataIndex: 'rcNo',  render: (v: string) => <span className="ferry-mono" style={{ fontSize: 12 }}>{v}</span> },
    { title: '标题', dataIndex: 'title' },
    { title: '状态', dataIndex: 'status',  render: (v: number) => <Tag color={STATUS[v]?.color}>{STATUS[v]?.text ?? '—'}</Tag> },
    { title: '风险', dataIndex: 'riskLevel',  render: (v: number) => v ? <Tag color={RISK[v]?.color}>{RISK[v]?.text}</Tag> : '—' },
    { title: '评分', dataIndex: 'riskScore',  render: (v?: string) => v ?? '—' },
    { title: '计划窗口',  render: (_, r) => <span className="ferry-mono" style={{ fontSize: 12 }}>{r.windowStart} ~ {r.windowEnd}</span> },
    { title: '创建人', dataIndex: 'creator', },
    { title: '创建时间', dataIndex: 'createTime',  render: (v: string) => <span className="ferry-mono" style={{ fontSize: 12, color: 'var(--text-3)' }}>{v ?? '—'}</span> },
    { title: '操作', key: 'action',   render: (_, r) => (
      <Space size={2}>
        {/* 评审/执行页读 ?id= query 参数，须用查询串跳转 */}
        <Tooltip title="评审">
          <Button type="link" size="small" icon={<Eye size={14} />} onClick={() => navigate(`/release/review?id=${r.id}`)} />
        </Tooltip>
        {[3, 4, 5].includes(r.status) && can('devops:release:start') && (
          <Tooltip title="发布执行">
            <Button type="link" size="small" icon={<Play size={14} />} onClick={() => navigate(`/release/execute?id=${r.id}`)} />
          </Tooltip>
        )}
        {r.status === 1 && can('devops:release:submit') && (
          <Tooltip title="提交">
            <Button type="link" size="small" icon={<Send size={14} />} onClick={() => simple(() => releaseApi.submit(r.id), '提交成功')} />
          </Tooltip>
        )}
        {r.status === 2 && can('devops:release:submit') && (
          <Tooltip title="撤回">
            <Button type="link" size="small" icon={<Undo2 size={14} />} onClick={() => modal.confirm({
              title: '撤回发布单？', content: '将解锁制品并退回草稿。', onOk: () => simple(() => releaseApi.withdraw(r.id), '已撤回'),
            })} />
          </Tooltip>
        )}
        {r.status === 1 && can('devops:release:save') && (
          <Tooltip title="删除">
            <Button type="link" size="small" danger icon={<Trash2 size={14} />} onClick={() => modal.confirm({
              title: '删除草稿？', onOk: () => simple(() => releaseApi.deleted([r.id]), '删除成功'),
            })} />
          </Tooltip>
        )}
      </Space>
    ) },
  ]

  return (
    <>
      <PageHeader title="发布单" description="生产发布的准入载体。每应用一制品，提交后锁定制品；高风险需双人审批。"
        tag={<RoleTag>共 {list.data?.total ?? 0} 项</RoleTag>}
        extra={can('devops:release:save') && <Button type="primary" icon={<Plus size={14} />} onClick={() => { form.resetFields(); setOpen(true) }}>新建发布单</Button>} />
      <section className="ferry-panel">
        <div className="ferry-toolbar">
          <Input allowClear prefix={<Search size={15} />} placeholder="单号/标题" style={{ width: 180 }} value={draft.title as string}
            onChange={(e) => setDraft({ ...draft, title: e.target.value })} />
          <Select allowClear placeholder="状态" style={{ width: 120 }} value={draft.status as number}
            onChange={(v) => setDraft({ ...draft, status: v })} options={Object.entries(STATUS).map(([k, v]) => ({ value: Number(k), label: v.text }))} />
          <Button type="primary" icon={<Search size={15} />} onClick={() => { setFilters(draft); setPage((p) => ({ ...p, current: 1 })) }}>查询</Button>
          <Button icon={<RotateCw size={15} />} onClick={() => { setDraft({}); setFilters({}); setPage((p) => ({ ...p, current: 1 })) }}>重置</Button>
        </div>
        <div className="ferry-table-wrap">
          <FerryTable<ReleaseRow> rowKey="id" columns={columns} dataSource={list.data?.records ?? []}
            loading={list.isLoading || list.isFetching} scroll={{ x: 'max-content' }}
            pagination={{ current: page.current, pageSize: page.size, total: list.data?.total ?? 0, showSizeChanger: true,
              pageSizeOptions: ['30', '100', '200'], showTotal: (t) => `共 ${t} 条`, onChange: (c, s) => setPage({ current: c, size: s }) }} />
        </div>
      </section>

      <Modal open={open} title="新建发布单" okText="保存" cancelText="取消" width={560} confirmLoading={saveMutation.isPending}
        onCancel={() => setOpen(false)} onOk={() => form.validateFields().then((v) => saveMutation.mutate(v))} destroyOnHidden>
        <Form form={form} layout="vertical" requiredMark="optional">
          <Form.Item name="title" label="标题" rules={[{ required: true, message: '请输入标题' }]}><Input maxLength={200} /></Form.Item>
          <Form.Item name="description" label="描述"><Input.TextArea rows={2} /></Form.Item>
          <Form.Item name="changeTicketId" label="关联变更单">
            <Select allowClear options={tickets.data?.records?.map((t) => ({ value: t.id, label: `${t.ticketNo} · ${t.title}` }))} />
          </Form.Item>
          <Form.Item name="windowStart" label="计划窗口开始" rules={[{ required: true, message: '请选择开始时间' }]}>
            <Input placeholder="yyyy-MM-dd HH:mm:ss" />
          </Form.Item>
          <Form.Item name="windowEnd" label="计划窗口结束" rules={[{ required: true, message: '请选择结束时间' }]}>
            <Input placeholder="yyyy-MM-dd HH:mm:ss" />
          </Form.Item>
        </Form>
      </Modal>
    </>
  )
}
