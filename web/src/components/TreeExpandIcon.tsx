import { ChevronRight } from 'lucide-react'

interface ExpandIconProps {
  expanded?: boolean
  expandable?: boolean
  record?: unknown
  onClick?: (event: React.MouseEvent<HTMLElement>) => void
}

/**
 * 树形表格的展开图标
 *
 * antd 默认展开按钮是一个 37px 的圆角方框，边框取 colorBorderSecondary。
 * 主题把 borderColor 压到 #eef1f6 后，方框几乎看不见却又确实占着位置，
 * 视觉上就成了「一个若隐若现的方块套着一个加号」。
 *
 * 这里换成无边框箭头：收起时指向右，展开时旋转 90° 朝下，
 * 与侧栏树的方向语义一致；叶子节点渲染同尺寸占位，保证文字对齐。
 */
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
