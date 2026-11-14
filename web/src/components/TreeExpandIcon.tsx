import { ChevronRight } from 'lucide-react'

interface ExpandIconProps {
  expanded?: boolean
  expandable?: boolean
  record?: unknown
  onClick?: (event: React.MouseEvent<HTMLElement>) => void
}

/** 树形表格的展开图标：无边框箭头收起朝右、展开朝下，叶子节点渲染同尺寸占位保证对齐 */
export function TreeExpandIcon({ expanded, expandable, onClick }: ExpandIconProps) {
  if (!expandable) {
    return <span style={{ display: 'inline-block', width: 16, height: 16 }} />
  }
  return (
    <button
      type="button"
      onClick={onClick}
      aria-expanded={expanded}
      className="ferry-tree-expand"
      data-expanded={expanded ? 'true' : 'false'}
    >
      <ChevronRight size={13} strokeWidth={2.2} />
    </button>
  )
}
