/** 全局提示通道：由 <Notifier> 注入 message 实例，React 树外的请求层统一调用 toastError */
type ToastFn = (text: string) => void

let toastFn: ToastFn | null = null

/** 错误批次窗口：窗口内相同文案丢弃、不同文案并进同一条提示，避免并发失败刷屏 */
const BATCH_MS = 3500
let batch: { first: string; others: number; startedAt: number } | null = null

export function bindToast(fn: ToastFn | null) {
  toastFn = fn
  // 解绑期间（StrictMode 双挂载 / 卸载）不该留下「已提示过」的批次，否则后续错误会被漏掉
  if (!fn) batch = null
}

export function toastError(text: string) {
  const content = text.trim()
  // 未绑定实例时不记录批次，否则解绑期间的错误会被误判成「已提示过」
  if (!content || !toastFn) return

  const now = Date.now()
  if (!batch || now - batch.startedAt >= BATCH_MS) {
    batch = { first: content, others: 0, startedAt: now }
    toastFn?.(content)
    return
  }
  // 同一次失败被多层重复上报，直接丢弃
  if (content === batch.first) return
  // 同批次的其它错误：更新同一条提示，只累加数量
  batch.others += 1
  toastFn?.(`${batch.first}（另有 ${batch.others} 个请求失败）`)
}

/** 供 QueryCache.onError 使用：只补拦截器没弹过的业务错误，避免同一失败提示两遍 */
export function notifyQueryError(error: unknown) {
  if (!error || typeof error !== 'object') return
  const err = error as { message?: unknown; kind?: unknown; notified?: boolean }
  if (typeof err.message !== 'string' || err.notified) return
  if (err.kind === 'auth' || err.kind === 'forbidden') return
  ;(error as { notified?: boolean }).notified = true
  toastError(err.message)
}

/** mutation / 手动 catch 的统一错误提示入口，批次去重会兜住请求层已弹过的错误 */
export function showError(error: unknown) {
  const message = error instanceof Error ? error.message : String(error ?? '')
  toastError(message || '操作失败，请稍后重试')
}
