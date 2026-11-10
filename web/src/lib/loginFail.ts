/**
 * 登录失败计数
 *
 * 后端 `AuthUtil.loginLock` 的口径（common-security）：
 * - 每次密码校验失败 INCR 一次，首次失败开始计 TTL；
 * - 达到 maxFailCount（默认 5）时刷新 TTL，锁定 lockDuration（默认 900s = 15 分钟）；
 * - `checkLoginLock` 在**校验密码之前**拦截，所以第 6 次请求才会收到"失败次数过多"。
 *
 * 也就是说：第 5 次失败那一刻账号其实已经锁了，但用户要到第 6 次点击才知道。
 * 前端按同一口径本地记一份，好在第 5 次就当场提示，不必让用户白试一次。
 *
 * 计数只是提示用的副本，真正的判定始终在后端：本地丢了最多多提示一次，不会放宽校验。
 */
const STORAGE_KEY = 'ferry.login.fail'
export const MAX_FAIL_COUNT = 5
export const LOCK_MINUTES = 15

/** 按「租户::账号」分别记，避免换账号 A→B→A 时把 A 的计数冲掉 */
type FailMap = Record<string, { count: number; expiresAt: number }>

/** 记录条数上限，防止长期使用后 localStorage 里堆满历史账号 */
const MAX_ENTRIES = 10

function keyOf(tenantId: string, account: string): string {
  return `${tenantId}::${account}`
}

function readMap(): FailMap {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    if (!raw) return {}
    const parsed = JSON.parse(raw) as unknown
    if (!parsed || typeof parsed !== 'object' || Array.isArray(parsed)) return {}

    const now = Date.now()
    const map: FailMap = {}
    for (const [key, value] of Object.entries(parsed as FailMap)) {
      if (!value || typeof value.count !== 'number' || typeof value.expiresAt !== 'number') continue
      if (now > value.expiresAt) continue
      map[key] = { count: value.count, expiresAt: value.expiresAt }
    }
    return map
  } catch {
    return {}
  }
}

function writeMap(map: FailMap): void {
  try {
    const entries = Object.entries(map)
    // 超量时优先丢最早过期的
    const kept = entries.length > MAX_ENTRIES
      ? entries.sort((left, right) => right[1].expiresAt - left[1].expiresAt).slice(0, MAX_ENTRIES)
      : entries
    localStorage.setItem(STORAGE_KEY, JSON.stringify(Object.fromEntries(kept)))
  } catch {
    /* 隐私模式下写不进去，退化为不计数，不影响登录本身 */
  }
}

export function readFailCount(tenantId: string, account: string): number {
  return readMap()[keyOf(tenantId, account)]?.count ?? 0
}

/** 记一次失败并返回累计次数 */
export function bumpFailCount(tenantId: string, account: string): { count: number; expiresAt: number } {
  const map = readMap()
  const key = keyOf(tenantId, account)
  const previous = map[key]
  const count = (previous?.count ?? 0) + 1
  const lockMs = LOCK_MINUTES * 60_000
  // 首次失败开始计时；命中上限时刷新一次，与后端 loginLock 的 expire 时机一致
  const expiresAt = count === 1 || count >= MAX_FAIL_COUNT ? Date.now() + lockMs : previous!.expiresAt
  const record = { count, expiresAt }

  map[key] = record
  writeMap(map)
  return record
}

/** 登录成功后清零，避免下次误报 */
export function clearFailCount(tenantId: string, account: string): void {
  const map = readMap()
  const key = keyOf(tenantId, account)
  if (!(key in map)) return
  delete map[key]
  writeMap(map)
}