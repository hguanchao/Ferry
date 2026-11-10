import { useEffect, useMemo, useState } from 'react'
import type { Key } from 'react'
import { flattenTree, type TreeLike } from '@/lib/tree'

/**
 * 树表的默认展开
 *
 * AntD Table 的 defaultExpandAllRows 只在首次渲染时生效，而列表数据是异步到达的，
 * 结果就是「树表看起来是收起的」。这里改成受控：数据变化后把所有有子节点的行展开一次，
 * 之后尊重用户的展开/收起操作。
 */
export function useAutoExpandRows<T extends TreeLike<T> & { id: string }>(rows?: T[]) {
  const [expandedRowKeys, setExpandedRowKeys] = useState<string[]>([])

  const parentKeys = useMemo(
    () => (rows ? flattenTree(rows).filter((row) => row.children?.length).map((row) => row.id) : []),
    [rows],
  )

  useEffect(() => {
    setExpandedRowKeys(parentKeys)
  }, [parentKeys])

  return {
    expandedRowKeys,
    onExpandedRowsChange: (keys: readonly Key[]) => setExpandedRowKeys(keys.map(String)),
  }
}
