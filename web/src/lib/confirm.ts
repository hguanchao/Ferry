import { App } from 'antd'

type ConfirmModal = ReturnType<typeof App.useApp>['modal']

export interface TreeDeleteOptions {
  /** App.useApp() 拿到的 modal 实例 */
  modal: ConfirmModal
  /** 实体称谓，用于标题，如 部门 / 菜单 / 区域 */
  label: string
  /** 当前节点名称 */
  name: string
  /** 下级称谓，如 下级部门 / 下级菜单 / 下级区域 */
  childLabel: string
  /** 下级数量，0 表示确定是叶子节点；省略表示存在下级但数量未知（懒加载场景） */
  childCount?: number
  /** 无下级时也要说明的补充提示 */
  note?: string
  onOk: () => Promise<unknown>
}

/**
 * 树形数据删除确认
 *
 * 部门、菜单、区域删除时会连同直接下级一并移除，确认框必须先把影响范围讲清楚，
 * 否则用户点下去才发现连带删掉了别的节点。有下级时这句提示并入正文，只确认一次。
 */
export function confirmTreeDelete({ modal, label, name, childLabel, childCount, note, onOk }: TreeDeleteOptions) {
  const childText =
    childCount === undefined
      ? `该${label}下存在${childLabel}，会一并删除。`
      : childCount > 0
        ? `该${label}下有 ${childCount} 个${childLabel}，会一并删除。`
        : ''

  return modal.confirm({
    title: `删除${label}「${name}」？`,
    content: [childText, note].filter(Boolean).join(''),
    okText: '删除',
    okButtonProps: { danger: true },
    cancelText: '取消',
    onOk,
  })
}
