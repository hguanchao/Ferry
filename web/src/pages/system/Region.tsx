import { useMemo, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  App,
  Button,
  Empty,
  Form,
  Input,
  InputNumber,
  Modal,
  Select,
  Switch,
  Table,
  Tooltip,
  type TableColumnsType,
} from 'antd'
import { DeleteOutlined, EditOutlined, PlusOutlined, ReloadOutlined, SearchOutlined } from '@ant-design/icons'
import PageHeader from '@/components/PageHeader'
import { RoleTag } from '@/components/StatusTag'
import { regionApi } from '@/api/system'
import type { RegionNode } from '@/api/types'
import { useAuthStore } from '@/stores/auth'
import { showError } from '@/lib/notify'

/** 展开后子级尚未返回时插入的占位行，仅用于给出可见的加载反馈 */
const LOADING_ID_SUFFIX = '::loading'

/** 表格行：children 是懒加载拼进来的下级，_loading 标记占位行 */
type RegionRow = RegionNode & { _loading?: boolean }

function loadingRow(parentId: string): RegionRow {
  return {
    id: `${parentId}${LOADING_ID_SUFFIX}`,
    parentId,
    code: '',
    name: '',
    sort: 0,
    status: 1,
    isLeaf: true,
    _loading: true,
  }
}

const columnsBase: TableColumnsType<RegionRow> = [
  {
    title: '区域名称',
    dataIndex: 'name',
    width: 280,
    render: (value: string, record) =>
      record._loading ? (
        <span className="ferry-muted">加载中…</span>
      ) : (
        <span style={{ display: 'inline-flex', alignItems: 'center', gap: 8 }}>
          <span style={{ fontWeight: record.parentId === '0' ? 500 : 400, color: 'var(--text-1)' }}>{value}</span>
          {record.parentId === '0' && <RoleTag>省级</RoleTag>}
        </span>
      ),
  },
  {
    title: '区域编码',
    dataIndex: 'code',
    width: 170,
    render: (value: string, record) =>
      record._loading ? null : (
        <span className="ferry-mono" style={{ fontSize: 12 }}>
          {value}
        </span>
      ),
  },
  {
    title: '上级编码',
    dataIndex: 'parentId',
    width: 150,
    render: (value: string, record) =>
      record._loading ? null : value === '0' ? (
        <span className="ferry-muted">顶级</span>
      ) : (
        <span className="ferry-mono" style={{ fontSize: 12, color: 'var(--text-3)' }}>
          {value}
        </span>
      ),
  },
  {
    title: '排序',
    dataIndex: 'sort',
    width: 90,
    align: 'right',
    render: (value: number, record) => (record._loading ? null : <span className="ferry-mono">{value}</span>),
  },
  {
    title: '状态',
    dataIndex: 'status',
    width: 120,
    render: (value: number, record) =>
      record._loading ? null : (
        <span className="ferry-status" data-tone={value === 1 ? 'success' : 'default'}>
          <span className="ferry-status__dot" />
          {value === 1 ? '正常' : '停用'}
        </span>
      ),
  },
]

export default function Region() {
  const { message, modal } = App.useApp()
  const queryClient = useQueryClient()
  const can = useAuthStore((state) => state.can)
  const [form] = Form.useForm()

  const [open, setOpen] = useState(false)
  const [editing, setEditing] = useState<RegionNode | null>(null)
  /** 新增时的归属节点，null 表示新增顶级区域 */
  const [createParent, setCreateParent] = useState<RegionNode | null>(null)

  const [keyword, setKeyword] = useState('')
  const [status, setStatus] = useState<number | undefined>()
  const [applied, setApplied] = useState<{ keyword: string; status?: number }>({ keyword: '' })
  const [page, setPage] = useState(1)
  const [pageSize, setPageSize] = useState(50)

  /** 已加载的下级，按父级 id 归档；未命中表示该父级还没展开过 */
  const [childrenMap, setChildrenMap] = useState<Record<string, RegionNode[]>>({})
  const [loadingIds, setLoadingIds] = useState<Record<string, boolean>>({})
  const [expandedKeys, setExpandedKeys] = useState<string[]>([])

  const searchActive = applied.keyword.trim() !== ''

  // 浏览态只取根节点，展开哪一级取哪一级；搜索态跨层级取匹配结果，两者共用一个接口
  const rootsQuery = useQuery({
    queryKey: ['regions', 'roots', applied.status ?? ''],
    queryFn: () => regionApi.lazyList({ parentId: '0', status: applied.status }),
    enabled: !searchActive,
  })

  const searchQuery = useQuery({
    queryKey: ['regions', 'search', applied.keyword, applied.status ?? ''],
    queryFn: () => regionApi.lazyList({ keyword: applied.keyword.trim(), status: applied.status }),
    enabled: searchActive,
  })

  const activeQuery = searchActive ? searchQuery : rootsQuery

  const rows = useMemo<RegionRow[]>(() => {
    if (searchActive) {
      // 搜索结果本身就是跨层级的扁平列表，不再挂 children，避免出现点了展不开的箭头
      return (searchQuery.data ?? []).map((node) => ({ ...node, isLeaf: true }))
    }
    const decorate = (list: RegionNode[]): RegionRow[] =>
      list.map((node) => {
        if (node.isLeaf) return { ...node }
        if (loadingIds[node.id]) return { ...node, children: [loadingRow(node.id)] }
        const loaded = childrenMap[node.id]
        // children 必须有值（哪怕空数组）才会渲染展开图标，否则用户不知道这里还能点
        return { ...node, children: loaded ? decorate(loaded) : [] }
      })
    const tree = decorate(rootsQuery.data ?? [])
    if (tree.length <= pageSize) return tree
    // 树的整体结构不受分页影响，只是每页渲染一部分根节点，children 懒加载照常生效
    const start = (page - 1) * pageSize
    return tree.slice(start, start + pageSize)
  }, [searchActive, searchQuery.data, rootsQuery.data, childrenMap, loadingIds, page, pageSize])

  const nodeIndex = useMemo(() => {
    const index = new Map<string, RegionNode>()
    const walk = (list: RegionRow[]) =>
      list.forEach((node) => {
        if (node._loading) return
        index.set(node.id, node)
        if (node.children?.length) walk(node.children as RegionRow[])
      })
    walk(rows)
    return index
  }, [rows])

  /** 重新拉取所有已展开层级，保持展开位置不跳；配合根查询的 invalidate 覆盖整棵树 */
  const refreshLoadedLevels = () => {
    const ids = Object.keys(childrenMap)
    if (!ids.length) return
    void Promise.all(
      ids.map((id) =>
        regionApi
          .lazyList({ parentId: id })
          .then((list) => [id, list] as const)
          .catch(() => null),
      ),
    ).then((results) => {
      setChildrenMap((prev) => {
        const next = { ...prev }
        results.forEach((item) => {
          if (item) next[item[0]] = item[1]
        })
        return next
      })
    })
  }

  const invalidate = () => {
    void queryClient.invalidateQueries({ queryKey: ['regions'] })
    refreshLoadedLevels()
  }

  const saveMutation = useMutation({
    mutationFn: (values: Record<string, unknown>) => regionApi.save(values),
    onSuccess: (text) => {
      message.success(text || '保存成功')
      setOpen(false)
      invalidate()
    },
    onError: showError,
  })

  const deleteMutation = useMutation({
    mutationFn: (ids: string[]) => regionApi.deleted(ids),
    onSuccess: (text) => {
      message.success(text || '删除成功')
      invalidate()
    },
    onError: showError,
  })

  const loadChildren = async (record: RegionRow) => {
    if (record.isLeaf || childrenMap[record.id] || loadingIds[record.id]) return
    setLoadingIds((prev) => ({ ...prev, [record.id]: true }))
    try {
      const list = await regionApi.lazyList({ parentId: record.id })
      setChildrenMap((prev) => ({ ...prev, [record.id]: list }))
    } catch (error) {
      showError(error)
    } finally {
      setLoadingIds((prev) => {
        const next = { ...prev }
        delete next[record.id]
        return next
      })
    }
  }

  const openEditor = (record: RegionNode | null, parent?: RegionNode) => {
    setEditing(record)
    setCreateParent(record ? null : (parent ?? null))
    form.resetFields()
    if (record) {
      form.setFieldsValue({ ...record })
    } else {
      form.setFieldsValue({ status: 1, sort: 1 })
    }
    setOpen(true)
  }

  const parentLabel = editing
    ? editing.parentId === '0'
      ? '顶级区域'
      : (nodeIndex.get(editing.parentId)?.name ?? editing.parentId)
    : (createParent?.name ?? '顶级区域')

  const columns: TableColumnsType<RegionRow> = [
    ...columnsBase,
    {
      title: '操作',
      key: 'action',
      width: 190,
      fixed: 'right',
      render: (_, record) => {
        if (record._loading) return null
        return (
          <div style={{ display: 'flex', alignItems: 'center', gap: 2 }}>
            {can('system:region:save') && (
              <Tooltip title="新增下级区域">
                <Button type="link" size="small" icon={<PlusOutlined />} onClick={() => openEditor(null, record)} />
              </Tooltip>
            )}
            {can('system:region:save') && (
              <Button type="link" size="small" icon={<EditOutlined />} onClick={() => openEditor(record)}>
                编辑
              </Button>
            )}
            {can('system:region:deleted') && (
              <Button
                type="link"
                size="small"
                danger
                icon={<DeleteOutlined />}
                onClick={() =>
                  modal.confirm({
                    title: `删除区域「${record.name}」？`,
                    content: '存在下级区域时不可删除，需先处理下级。区划数据被引用后也不建议删除。',
                    okText: '删除',
                    okButtonProps: { danger: true },
                    cancelText: '取消',
                    onOk: () => deleteMutation.mutateAsync([record.id]),
                  })
                }
              />
            )}
          </div>
        )
      },
    },
  ]

  return (
    <>
      <PageHeader
        title="区域管理"
        description="区域是机房、网络与合规分区的归属。表格按行政区划层级逐级展开，展开到哪一级才取哪一级，不做全量拉取。"
        tag={<RoleTag>{searchActive ? `${searchQuery.data?.length ?? 0} 条匹配` : `${rootsQuery.data?.length ?? 0} 个一级区域`}</RoleTag>}
        extra={
          can('system:region:save') && (
            <Button type="primary" icon={<PlusOutlined />} onClick={() => openEditor(null)}>
              新增区域
            </Button>
          )
        }
      />

      <section className="ferry-panel">
        <div className="ferry-toolbar">
          <Input
            allowClear
            prefix={<SearchOutlined style={{ color: 'var(--text-4)' }} />}
            placeholder="区域名称或编码"
            style={{ width: 200 }}
            value={keyword}
            onChange={(event) => setKeyword(event.target.value)}
            onPressEnter={() => {
              setPage(1)
              setApplied({ keyword, status })
            }}
          />
          <Select
            allowClear
            placeholder="状态"
            style={{ width: 120 }}
            value={status}
            onChange={setStatus}
            options={[
              { value: 1, label: '正常' },
              { value: 0, label: '停用' },
            ]}
          />
          <Button
            type="primary"
            icon={<SearchOutlined />}
            onClick={() => {
              setPage(1)
              setApplied({ keyword, status })
            }}
          >
            查询
          </Button>
          <Button
            icon={<ReloadOutlined />}
            onClick={() => {
              setKeyword('')
              setStatus(undefined)
              setApplied({ keyword: '' })
              setPage(1)
              setExpandedKeys([])
              setChildrenMap({})
            }}
          >
            重置
          </Button>
          {searchActive && <span className="ferry-caption">跨层级匹配，结果按列表平铺展示</span>}
        </div>

        <div className="ferry-table-wrap">
          <Table<RegionRow>
            rowKey="id"
            columns={columns}
            dataSource={rows}
            loading={activeQuery.isLoading || activeQuery.isFetching}
            pagination={{
              current: page,
              pageSize,
              total: searchActive ? (searchQuery.data?.length ?? 0) : (rootsQuery.data?.length ?? 0),
              onChange: (nextPage, nextSize) => {
                setPage(nextPage)
                setPageSize(nextSize)
                // 换页后收起展开态：已加载的下级挂在各自的根节点上，跨页保留只会让状态难以预期
                setExpandedKeys([])
              },
              showSizeChanger: true,
              pageSizeOptions: [20, 50, 100],
              showTotal: (total) => `共 ${total} 条`,
            }}
            expandable={{
              indentSize: 14,
              expandedRowKeys: expandedKeys,
              onExpand: (expanded, record) => {
                setExpandedKeys((keys) => (expanded ? [...keys, record.id] : keys.filter((key) => key !== record.id)))
                if (expanded) void loadChildren(record)
              },
            }}
            locale={{
              emptyText: (
                <Empty
                  image={Empty.PRESENTED_IMAGE_SIMPLE}
                  description={searchActive ? '没有匹配的区域' : '暂无区域数据'}
                />
              ),
            }}
            scroll={{ x: 1000 }}
          />
        </div>
      </section>

      <Modal
        open={open}
        title={editing ? `编辑区域 · ${editing.name}` : '新增区域'}
        okText="保存"
        cancelText="取消"
        width={520}
        confirmLoading={saveMutation.isPending}
        onCancel={() => setOpen(false)}
        onOk={async () => {
          const values = await form.validateFields()
          saveMutation.mutate({
            ...values,
            id: editing?.id,
            parentId: editing ? editing.parentId : (createParent?.id ?? '0'),
          })
        }}
        destroyOnHidden
      >
        <Form form={form} layout="vertical" requiredMark="optional">
          <Form.Item label="上级区域" extra="区划编码即层级路径，调整上级需同时改编码，因此这里只读">
            <Input value={parentLabel} disabled />
          </Form.Item>
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(2, minmax(0, 1fr))', gap: '0 16px' }}>
            <Form.Item name="name" label="区域名称" rules={[{ required: true, message: '请输入区域名称' }]}>
              <Input placeholder="例如 杭州市" maxLength={50} />
            </Form.Item>
            <Form.Item
              name="code"
              label="区域编码"
              extra="省市区 6 位，街道 12 位"
              rules={[
                { required: true, message: '请输入区域编码' },
                { pattern: /^(\d{6}|\d{9}|\d{12})$/, message: '6 / 9 / 12 位数字编码' },
              ]}
            >
              <Input placeholder="例如 330100" maxLength={12} />
            </Form.Item>
            <Form.Item name="sort" label="排序">
              <InputNumber min={1} max={999} style={{ width: '100%' }} />
            </Form.Item>
            <Form.Item
              name="status"
              label="状态"
              valuePropName="checked"
              getValueFromEvent={(checked) => (checked ? 1 : 0)}
            >
              <Switch checkedChildren="正常" unCheckedChildren="停用" />
            </Form.Item>
          </div>
        </Form>
      </Modal>
    </>
  )
}
