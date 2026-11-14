import { useEffect, useMemo, useState } from 'react'
import type { Key } from 'react'
import { flattenTree, type TreeLike } from '@/lib/tree'

/** 树表默认展开：数据到达后把有子节点的行展开一次，之后尊重用户的展开/收起操作 */
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
