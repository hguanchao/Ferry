/** 代码托管平台的取值、展示名与状态色映射，应用管理与凭据管理共用 */

/** 代码托管平台取值，与后端 RepoTypeEnum 一致，顺序即下拉顺序 */
export const REPO_TYPES: ReadonlyArray<{ value: string; label: string }> = [
  { value: 'GITLAB', label: 'GitLab' },
  { value: 'GITHUB', label: 'GitHub' },
  { value: 'GITEE', label: 'Gitee' },
]

/** 平台展示名，未取值或未知时返回占位符 */
export function platformLabel(value?: string | null): string {
  if (!value) {
    return '—'
  }
  return REPO_TYPES.find((item) => item.value === value)?.label ?? value
}

/** 运行状态的中文名与展示色，列表与详情共用 */
export const RUN_STATUS_META: Readonly<Record<string, { label: string; color: string }>> = {
  QUEUED: { label: '排队中', color: 'default' },
  RUNNING: { label: '运行中', color: 'processing' },
  BLOCKED: { label: '阻塞', color: 'warning' },
  SUCCESS: { label: '成功', color: 'success' },
  FAILED: { label: '失败', color: 'error' },
  TIMEOUT: { label: '超时', color: 'error' },
  CANCELED: { label: '已取消', color: 'default' },
}

/** 运行状态展示名 */
export function runStatusLabel(value?: string | null): string {
  if (!value) {
    return '—'
  }
  return RUN_STATUS_META[value]?.label ?? value
}

/** 触发结果的中文名 */
export const TRIGGER_RESULT_LABEL: Readonly<Record<string, string>> = {
  CREATED: '已创建运行',
  MERGED: '已合并',
  REUSED: '复用运行',
  REJECTED: '已拒绝',
}

/** 触发结果展示名 */
export function triggerResultLabel(value?: string | null): string {
  if (!value) {
    return '—'
  }
  return TRIGGER_RESULT_LABEL[value] ?? value
}

/**
 * 运行耗时展示：由开始与结束时间现算，任一缺失或区间非法时返回占位符
 */
export function formatDuration(startedTime?: string | null, finishedTime?: string | null): string {
  if (!startedTime || !finishedTime) {
    return '—'
  }
  const start = Date.parse(startedTime.replace(' ', 'T'))
  const finish = Date.parse(finishedTime.replace(' ', 'T'))
  if (Number.isNaN(start) || Number.isNaN(finish) || finish < start) {
    return '—'
  }
  const ms = finish - start
  if (ms < 1000) {
    return `${ms}ms`
  }
  const seconds = Math.floor(ms / 1000)
  if (seconds < 60) {
    return `${seconds}s`
  }
  const minutes = Math.floor(seconds / 60)
  return `${minutes}m${seconds % 60}s`
}
