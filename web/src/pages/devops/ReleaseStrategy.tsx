import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { App, Button, Form, Input, InputNumber, Modal, Select, Space, Tag, type TableColumnsType } from 'antd'
import FerryTable from '@/components/FerryTable'
import { Plus, RotateCw, Save, Search } from 'lucide-react'
import PageHeader from '@/components/PageHeader'
import { RoleTag } from '@/components/StatusTag'
import { appApi, releaseApi } from '@/api/devops'
import type { ClosureRow } from '@/api/types-devops'
import { useAuthStore } from '@/stores/auth'
import { showError } from '@/lib/notify'
const SCOPE: Record<number, string> = { 1: '全局', 2: '应用', 3: '团队' }

export default function ReleaseStrategy() {
  const { message } = App.useApp()
  const queryClient = useQueryClient()
  const can = useAuthStore((s) => s.can)
  const [form] = Form.useForm()
  const [closureForm] = Form.useForm()
  const [openClosure, setOpenClosure] = useState(false)
  const [appId, setAppId] = useState<string>()
  const filters: Record<string, unknown> = {}
  const [page, setPage] = useState({ current: 1, size: 30 })

  const apps = useQuery({ queryKey: ['app-options'], queryFn: () => appApi.options() })
  const closures = useQuery({ queryKey: ['closures', filters, page], queryFn: () => releaseApi.closurePage({ ...filters, ...page }) })
  const strategyQuery = useQuery({
    queryKey: ['strategy', appId], queryFn: () => releaseApi.strategyDetail(appId!), enabled: !!appId,
  })

  const invalidate = () => {
    void queryClient.invalidateQueries({ queryKey: ['closures'] })
    void queryClient.invalidateQueries({ queryKey: ['strategy'] })
  }
  const saveStrategy = useMutation({
    mutationFn: (body: Record<string, unknown>) => releaseApi.strategySave(body),
    onSuccess: (t) => { message.success(t || '保存成功'); invalidate() }, onError: showError,
  })
  const saveClosure = useMutation({
    mutationFn: (body: Record<string, unknown>) => releaseApi.closureSave(body),
    onSuccess: (t) => { message.success(t || '保存成功'); setOpenClosure(false); invalidate() }, onError: showError,
  })

  const columns: TableColumnsType<ClosureRow> = [
    { title: '开始日期', dataIndex: 'startDate', },
    { title: '结束日期', dataIndex: 'endDate', },
    { title: '范围', dataIndex: 'scope',  render: (v: number) => <Tag color={v === 1 ? 'red' : 'blue'}>{SCOPE[v] ?? '—'}</Tag> },
    { title: '应用', dataIndex: 'appId',  render: (v?: string) => apps.data?.find((a) => a.value === v)?.label ?? '—' },
    { title: '原因', dataIndex: 'reason' },
    { title: '创建时间', dataIndex: 'createTime', },
  ]

  return (
    <>
      <PageHeader title="发布策略与封网期" description="按应用配置发布窗口与观察期；封网期内禁止生产发布启动。"
        tag={<RoleTag>封网期共 {closures.data?.total ?? 0} 项</RoleTag>} />
      <section className="ferry-panel">
        <h3>发布策略</h3>
        <Space align="end" wrap>
          <div>
            <div style={{ fontSize: 12, color: 'var(--text-3)', marginBottom: 4 }}>选择应用</div>
            <Select showSearch style={{ width: 220 }} value={appId} onChange={(v) => setAppId(v)}
              options={apps.data?.map((a) => ({ value: a.value, label: a.label }))} placeholder="选择应用" />
          </div>
          <Button icon={<Search size={14} />} onClick={() => void queryClient.invalidateQueries({ queryKey: ["strategy"] })}>加载策略</Button>
        </Space>
        {appId && <Form form={form} layout="inline" style={{ marginTop: 12 }}
          initialValues={{ windowsJson: strategyQuery.data?.windowsJson ?? '[]', isClosureExcluded: strategyQuery.data?.isClosureExcluded ?? 0, observeMinutes: strategyQuery.data?.observeMinutes }}
          onFinish={(v) => saveStrategy.mutate({ ...v, appId: Number(appId) })}>
          <Form.Item name="windowsJson" label="发布窗口 JSON"><Input style={{ width: 260 }} placeholder='[{"start":"22:00","end":"23:00"}]' /></Form.Item>
          <Form.Item name="isClosureExcluded" label="排除封网">
            <Select style={{ width: 100 }} options={[{ value: 0, label: '否' }, { value: 1, label: '是' }]} />
          </Form.Item>
          <Form.Item name="observeMinutes" label="观察期(分钟)"><InputNumber min={5} /></Form.Item>
          {can('devops:releaseStrategy:save') && <Form.Item><Button type="primary" htmlType="submit" icon={<Save size={14} />}>保存</Button></Form.Item>}
        </Form>}
      </section>

      <section className="ferry-panel" style={{ marginTop: 12 }}>
        <div className="ferry-toolbar">
          <h3 style={{ margin: 0, marginRight: 'auto' }}>封网期</h3>
          {can('devops:releaseStrategy:closureSave') && <Button type="primary" icon={<Plus size={14} />} onClick={() => { closureForm.resetFields(); setOpenClosure(true) }}>新增封网</Button>}
          <Button icon={<RotateCw size={14} />} onClick={() => void queryClient.invalidateQueries({ queryKey: ['closures'] })} />
        </div>
        <div className="ferry-table-wrap">
          <FerryTable<ClosureRow> rowKey="id" columns={columns} dataSource={closures.data?.records ?? []}
            loading={closures.isLoading || closures.isFetching} scroll={{ x: 'max-content' }}
            pagination={{ current: page.current, pageSize: page.size, total: closures.data?.total ?? 0, showSizeChanger: true,
              pageSizeOptions: ['30', '100', '200'], onChange: (c, s) => setPage({ current: c, size: s }) }} />
        </div>
      </section>

      <Modal open={openClosure} title="新增封网期" okText="保存" cancelText="取消" width={520} confirmLoading={saveClosure.isPending}
        onCancel={() => setOpenClosure(false)} onOk={() => closureForm.validateFields().then((v) => saveClosure.mutate(v))} destroyOnHidden>
        <Form form={closureForm} layout="vertical" requiredMark="optional">
          <Form.Item name="startDate" label="开始日期" rules={[{ required: true }]}><Input placeholder="yyyy-MM-dd" /></Form.Item>
          <Form.Item name="endDate" label="结束日期" rules={[{ required: true }]}><Input placeholder="yyyy-MM-dd" /></Form.Item>
          <Form.Item name="scope" label="范围" rules={[{ required: true }]}><Select options={[1, 2, 3].map((v) => ({ value: v, label: SCOPE[v] }))} /></Form.Item>
          <Form.Item name="appId" label="应用（scope=应用时必填）"><Select allowClear options={apps.data?.map((a) => ({ value: a.value, label: a.label }))} /></Form.Item>
          <Form.Item name="reason" label="原因"><Input.TextArea rows={2} /></Form.Item>
        </Form>
      </Modal>
    </>
  )
}
