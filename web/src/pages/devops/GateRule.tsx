import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { App, Button, Form, Input, Modal, Select, Space, Switch, Table, Tabs, Tag, type TableColumnsType } from 'antd'
import { Pencil, Plus, RotateCw, Search, Trash2 } from 'lucide-react'
import PageHeader from '@/components/PageHeader'
import { RoleTag } from '@/components/StatusTag'
import { gateApi, appApi } from '@/api/devops'
import { post } from '@/api/client'
import type { GateRuleRow } from '@/api/types-devops'
import { useAuthStore } from '@/stores/auth'
import { showError } from '@/lib/notify'
import { COL } from '@/lib/table'

const SCOPE_LABEL: Record<number, string> = { 1: '组织默认', 2: '应用', 3: '分支' }
const RULE_LABEL: Record<number, string> = {
  1: '单元测试通过率', 2: '行覆盖率', 3: '分支覆盖率', 4: '阻断级漏洞数', 5: '高危依赖漏洞数', 6: '许可证黑名单',
}
const DIRECTION_LABEL: Record<number, string> = { 1: '不低于', 2: '不高于' }

export default function GateRule() {
  const { message, modal } = App.useApp()
  const queryClient = useQueryClient()
  const can = useAuthStore((state) => state.can)
  const [form] = Form.useForm()
  const [open, setOpen] = useState(false)
  const [editing, setEditing] = useState<GateRuleRow | null>(null)
  const [scope, setScope] = useState('1')
  const [filters, setFilters] = useState<Record<string, unknown>>({ scopeType: 1 })
  const [page, setPage] = useState({ current: 1, size: 30 })
  const [appId, setAppId] = useState<string | undefined>()

  const query = useQuery({
    queryKey: ['gate-rules', filters, page],
    queryFn: () => gateApi.rulePage({ ...filters, ...page }),
  })
  const appsQuery = useQuery({ queryKey: ['app-options'], queryFn: () => appApi.options() })
  const baselineQuery = useQuery({
    queryKey: ['baseline', appId],
    queryFn: async () => await gateApi.baselineDetail(appId!) as unknown as {
      tool: string; totalCount: number; newCount?: number; fixedCount?: number
      legacyCount?: number; fingerprintVersion: string
    },
    enabled: !!appId,
  })

  const saveMutation = useMutation({
    mutationFn: (values: Record<string, unknown>) => gateApi.ruleSave(values),
    onSuccess: (text) => {
      message.success(text || '保存成功')
      setOpen(false)
      void queryClient.invalidateQueries({ queryKey: ['gate-rules'] })
    },
    onError: showError,
  })
  const deleteMutation = useMutation({
    mutationFn: (ids: string[]) => gateApi.ruleDeleted(ids),
    onSuccess: (text) => { message.success(text || '删除成功'); void queryClient.invalidateQueries({ queryKey: ['gate-rules'] }) },
    onError: showError,
  })
  const enableMutation = useMutation({
    mutationFn: (id: string) => gateApi.ruleEnable(id),
    onSuccess: (text) => { message.success(text || '操作成功'); void queryClient.invalidateQueries({ queryKey: ['gate-rules'] }) },
    onError: showError,
  })

  const openEditor = (record: GateRuleRow | null) => {
    setEditing(record)
    form.resetFields()
    if (record) {
      form.setFieldsValue({ ...record })
    } else {
      form.setFieldsValue({ scopeType: Number(scope), ruleType: 1, compareDirection: 1, isExemptAllowed: true, isEnabled: true })
    }
    setOpen(true)
  }

  const columns: TableColumnsType<GateRuleRow> = [
    { title: '作用域', dataIndex: 'scopeType', width: 110,
      render: (v: number) => <Tag color={v === 3 ? 'purple' : v === 2 ? 'blue' : 'default'}>{SCOPE_LABEL[v] ?? v}</Tag>,
    },
    { title: '规则类型', dataIndex: 'ruleType', width: 150, render: (v: number) => RULE_LABEL[v] ?? v },
    { title: '规则名', dataIndex: 'ruleName' },
    { title: '阈值', dataIndex: 'threshold', width: 120, render: (v: string) => <span className="ferry-mono">{v}</span> },
    { title: '比较方向', dataIndex: 'compareDirection', width: 100, render: (v: number) => DIRECTION_LABEL[v] ?? '—' },
    { title: '版本', dataIndex: 'version', width: 70 },
    { title: '状态', dataIndex: 'isEnabled', width: 90,
      render: (v: number) => <Tag color={v === 1 ? 'green' : 'default'}>{v === 1 ? '启用' : '停用'}</Tag>,
    },
    { title: '操作', key: 'action', width: COL.actionWide, fixed: 'right', render: (_, r) => (
      <Space size={0}>
        {can('devops:gateRule:enable') && (
          <Button type="link" size="small" onClick={() => enableMutation.mutate(r.id)}>{r.isEnabled === 1 ? '停用' : '启用'}</Button>
        )}
        {can('devops:gateRule:save') && (
          <Button type="link" size="small" icon={<Pencil size={14} />} onClick={() => openEditor(r)}>编辑</Button>
        )}
        {can('devops:gateRule:deleted') && (
          <Button type="link" size="small" danger icon={<Trash2 size={14} />}
            onClick={() => modal.confirm({ title: '删除规则？', okText: '删除', okButtonProps: { danger: true }, cancelText: '取消',
              onOk: () => deleteMutation.mutateAsync([r.id]) })} />
        )}
      </Space>
    ) },
  ]

  const baselineItems = [
    {
      key: 'baseline', label: '存量基线', children: (
        <div style={{ padding: 16 }}>
          <Select allowClear showSearch placeholder="选择应用查看基线" style={{ width: 240, marginBottom: 16 }} value={appId} onChange={setAppId}
            options={(appsQuery.data ?? []).map((a) => ({ value: a.value, label: a.label }))} />
          {baselineQuery.data ? (
            <div style={{ lineHeight: 2 }}>
              <div>扫描工具：{baselineQuery.data.tool}</div>
              <div>问题总数：{baselineQuery.data.totalCount}</div>
              <div>新增：{baselineQuery.data.newCount ?? 0} / 修复：{baselineQuery.data.fixedCount ?? 0} / 遗留：{baselineQuery.data.legacyCount ?? 0}</div>
              <div>指纹版本：{baselineQuery.data.fingerprintVersion}</div>
              {can('devops:baseline:reset') && (
                <Button style={{ marginTop: 12 }} onClick={() => modal.confirm({ title: '重设基线？', content: '需选择审批人，审批通过后新快照生效。',
                  okText: '提交申请', cancelText: '取消', onOk: async () => {
                    await post<string>('/ferry-devops/gateBaseline/reset', { appId, reason: '按最新扫描结果重设基线', approverId: appsQuery.data?.[0]?.value })
                    message.success('申请已提交'); void queryClient.invalidateQueries({ queryKey: ['baseline'] })
                  } })}>重设基线</Button>
              )}
            </div>
          ) : <span style={{ color: 'var(--text-3)' }}>{appId ? '该应用暂无基线' : '请选择应用'}</span>}
        </div>
      ),
    },
  ]

  return (
    <>
      <PageHeader title="质量门禁规则"
        description="作用域优先级：分支 > 应用 > 组织；同类型多条取最严格阈值。修改只影响新运行，规则快照随运行落库。"
        tag={<RoleTag>共 {query.data?.total ?? 0} 条</RoleTag>}
        extra={can('devops:gateRule:save') && (
          <Button type="primary" icon={<Plus size={14} />} onClick={() => openEditor(null)}>新增规则</Button>
        )}
      />
      <Tabs activeKey={scope} onChange={(key) => { setScope(key); setFilters({ scopeType: Number(key) }); setPage((p) => ({ ...p, current: 1 })) }}
        items={[1, 2, 3].map((s) => ({ key: String(s), label: SCOPE_LABEL[s] }))} />
      <section className="ferry-panel">
        <div className="ferry-toolbar">
          <Input placeholder="规则名" style={{ width: 180 }} onPressEnter={(e) => setFilters((f) => ({ ...f, ruleName: (e.target as HTMLInputElement).value }))} />
          <Select allowClear placeholder="规则类型" style={{ width: 160 }}
            onChange={(v) => setFilters((f) => ({ ...f, ruleType: v }))}
            options={Object.entries(RULE_LABEL).map(([k, v]) => ({ value: Number(k), label: v }))} />
          <Button type="primary" icon={<Search size={15} />} onClick={() => setPage((p) => ({ ...p, current: 1 }))}>查询</Button>
          <Button icon={<RotateCw size={14} />} onClick={() => void queryClient.invalidateQueries({ queryKey: ['gate-rules'] })}>刷新</Button>
        </div>
        <div className="ferry-table-wrap">
          <Table<GateRuleRow> rowKey="id" columns={columns} dataSource={query.data?.records ?? []}
            loading={query.isFetching} scroll={{ x: 'max-content' }}
            pagination={{ current: page.current, pageSize: page.size, total: query.data?.total ?? 0,
              showSizeChanger: true, pageSizeOptions: ['30', '100', '200'],
              showTotal: (t) => `共 ${t} 条`, onChange: (c, s) => setPage({ current: c, size: s }) }}
          />
        </div>
      </section>

      <Tabs items={baselineItems} />

      <Modal open={open} title={editing ? '编辑规则' : '新增规则'} width={560} okText="保存" cancelText="取消"
        confirmLoading={saveMutation.isPending} onCancel={() => setOpen(false)}
        onOk={async () => {
          const values = await form.validateFields()
          saveMutation.mutate({ ...values, id: editing?.id,
            isExemptAllowed: values.isExemptAllowed ? 1 : 0, isEnabled: values.isEnabled ? 1 : 0,
            stageTypes: Array.isArray(values.stageTypes) ? values.stageTypes.join(',') : (values.stageTypes ?? '2,3') })
        }} destroyOnHidden>
        <Form form={form} layout="vertical" requiredMark="optional">
          <Form.Item name="scopeType" label="作用域" rules={[{ required: true }]}>
            <Select options={[{ value: 1, label: '组织默认' }, { value: 2, label: '应用' }, { value: 3, label: '分支' }]} />
          </Form.Item>
          <Form.Item name="appId" label="适用应用（应用/分支级必填）"><Input placeholder="应用id" /></Form.Item>
          <Form.Item name="branchPattern" label="分支通配（仅分支级）"><Input placeholder="如 release/*" /></Form.Item>
          <Form.Item name="ruleType" label="规则类型" rules={[{ required: true }]}>
            <Select options={Object.entries(RULE_LABEL).map(([k, v]) => ({ value: Number(k), label: v }))} />
          </Form.Item>
          <Form.Item name="ruleName" label="规则名" rules={[{ required: true }]}><Input /></Form.Item>
          <Form.Item name="threshold" label="阈值" rules={[{ required: true }]}
            extra="比率类填 0-100 数值；数量类填非负整数；黑名单填逗号分隔值"><Input /></Form.Item>
          <Form.Item name="compareDirection" label="比较方向" rules={[{ required: true }]}>
            <Select options={[{ value: 1, label: '不低于' }, { value: 2, label: '不高于' }]} />
          </Form.Item>
          <Form.Item name="stageTypes" label="作用阶段" extra="单元测试、静态扫描，逗号分隔"><Input placeholder="2,3" /></Form.Item>
          <Form.Item name="isExemptAllowed" label="允许豁免" valuePropName="checked"><Switch /></Form.Item>
          <Form.Item name="isEnabled" label="启用" valuePropName="checked"><Switch /></Form.Item>
        </Form>
      </Modal>
    </>
  )
}
