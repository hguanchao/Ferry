/**
 * 角色数据范围
 *
 * 取值与后端 DataScopeEnum 一一对应，新增取值必须同步枚举、建表注释与这里，
 * 否则界面会显示成「未知」，而计算侧会按无权限处理。
 */
export const DATA_SCOPE_OPTIONS = [
  { value: 1, label: '全部数据' },
  { value: 2, label: '自定义数据' },
  { value: 3, label: '本部门数据' },
  { value: 4, label: '本部门及以下数据' },
  { value: 5, label: '仅本人数据' },
]

const DATA_SCOPE_TEXT = new Map(DATA_SCOPE_OPTIONS.map((item) => [item.value, item.label]))

/**
 * 数据范围的展示文案
 *
 * @param value 数据范围取值
 * @returns 文案，未取值时返回占位符
 */
export function dataScopeText(value?: number | null): string {
  if (!value) return '—'
  return DATA_SCOPE_TEXT.get(value) ?? '未知'
}
