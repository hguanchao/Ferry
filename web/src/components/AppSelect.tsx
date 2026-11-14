import { useEffect, useMemo, useState, type CSSProperties, type UIEvent } from 'react'
import { useInfiniteQuery } from '@tanstack/react-query'
import { Select, Spin } from 'antd'
import { appApi } from '@/api/devops'
import type { AppOption, AppRow } from '@/api/types'

const PAGE_SIZE = 20
const SEARCH_DEBOUNCE_MS = 300
const SCROLL_BOTTOM_GAP = 8

interface AppSelectProps {
  /** Form.Item 注入的 id，用于 label 与输入框关联 */
  id?: string
  multiple?: boolean
  value?: string | string[]
  onChange?: (value?: string | string[]) => void
  placeholder?: string
  disabled?: boolean
  allowClear?: boolean
  style?: CSSProperties
  maxTagCount?: 'responsive' | number
  /** 新建流水线时停用应用不能选，筛选场景仍需要看到停用应用的历史数据 */
  disableStopped?: boolean
  /** 已选但尚未加载到的应用，用来在输入框里显示名称而不是裸 id */
  presetOptions?: Array<Pick<AppOption, 'id' | 'code' | 'name'>>
}

/** 应用下拉：pageList 远程搜索 + 滚动分页，已加载选项累积保留以便回显已选名称 */
export default function AppSelect({
  id,
  multiple = false,
  value,
  onChange,
  placeholder = '请选择应用',
  disabled = false,
  allowClear = true,
  style,
  maxTagCount,
  disableStopped = false,
  presetOptions,
}: AppSelectProps) {
  const [open, setOpen] = useState(false)
  const [searchText, setSearchText] = useState('')
  const [keyword, setKeyword] = useState('')
  const [seen, setSeen] = useState<AppOption[]>([])

  // 输入停顿后再发请求，避免每敲一个字都打一次分页接口
  useEffect(() => {
    const timer = window.setTimeout(() => setKeyword(searchText.trim()), SEARCH_DEBOUNCE_MS)
    return () => window.clearTimeout(timer)
  }, [searchText])

  const query = useInfiniteQuery({
    queryKey: ['app-options', keyword],
    queryFn: ({ pageParam }) => appApi.pageList({ current: pageParam, size: PAGE_SIZE, keyword }),
    initialPageParam: 1,
    getNextPageParam: (lastPage) => (lastPage.current < lastPage.pages ? lastPage.current + 1 : undefined),
    enabled: open,
    retry: false,
    // 反复展开下拉时不把已加载的多页全部重拉；应用档案变更不频繁
    staleTime: 30_000,
  })

  useEffect(() => {
    if (!query.data) {
      return
    }
    const fetched = query.data.pages.flatMap((page) => page.records).map(toOption)
    setSeen((prev) => mergeOptions(prev, fetched))
  }, [query.data])

  const fetchedOptions = useMemo(
    () => query.data?.pages.flatMap((page) => page.records).map(toOption) ?? [],
    [query.data],
  )

  const options = useMemo(() => {
    const selectedIds = new Set(Array.isArray(value) ? value : value ? [value] : [])
    // seen 只用来给已选项补名称；下拉列表本身仍然只展示当前搜索的结果
    const selected = seen.filter((item) => selectedIds.has(item.id))
    const presets: AppOption[] = (presetOptions ?? []).map((item) => ({ ...item, status: 1 }))
    return mergeOptions(fetchedOptions, [...selected, ...presets]).map((app) => ({
      value: app.id,
      label: `${app.name}（${app.code}）${app.status === 1 ? '' : '（停用）'}`,
      disabled: disableStopped && app.status !== 1,
    }))
  }, [seen, fetchedOptions, presetOptions, value, disableStopped])

  const handleOpenChange = (next: boolean) => {
    setOpen(next)
    if (next) {
      // 每次展开都回到第一页，避免上次搜索条件影响这一次选择
      setSearchText('')
      setKeyword('')
    }
  }

  const handlePopupScroll = (event: UIEvent<HTMLDivElement>) => {
    const target = event.currentTarget
    const nearBottom = target.scrollTop + target.offsetHeight >= target.scrollHeight - SCROLL_BOTTOM_GAP
    if (nearBottom && query.hasNextPage && !query.isFetchingNextPage) {
      void query.fetchNextPage()
    }
  }

  return (
    <Select<string | string[]>
      id={id}
      mode={multiple ? 'multiple' : undefined}
      value={value}
      onChange={(next) => onChange?.(next)}
      options={options}
      placeholder={placeholder}
      disabled={disabled}
      allowClear={allowClear}
      showSearch
      filterOption={false}
      onSearch={setSearchText}
      onOpenChange={handleOpenChange}
      onPopupScroll={handlePopupScroll}
      loading={query.isLoading}
      notFoundContent={query.isLoading ? <Spin size="small" /> : '暂无应用'}
      maxTagCount={multiple ? maxTagCount : undefined}
      style={style}
    />
  )
}

function toOption(app: AppRow): AppOption {
  return { id: app.id, code: app.code, name: app.name, status: app.status }
}

function mergeOptions(current: AppOption[], incoming: AppOption[]): AppOption[] {
  const map = new Map<string, AppOption>()
  for (const item of current) {
    map.set(item.id, item)
  }
  for (const item of incoming) {
    map.set(item.id, item)
  }
  return [...map.values()]
}
