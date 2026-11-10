/**
 * 全局提示通道
 *
 * axios 拦截器与 QueryClient 都活在 React 树之外，拿不到 App.useApp() 的 message 实例，
 * 因此由 <Notifier> 在挂载后把实例注入这里，请求层统一调用 toastError。
 *
 * 这里刻意不对 ApiError 做类型引用（client.ts 已经 import 本模块，反向引用会形成循环依赖），
 * 只按鸭子类型读取 message / kind / notified 三个字段。
 */
type ToastFn = (text: string) => void

let toastFn: ToastFn | null = null

/**
 * 错误批次窗口，与 message 的 3.5s 存活时间对齐。
 *
 * 一次页面动作常会并发触发多个请求失败（列表页同时拉 treeList / pageList / 选项接口），
 * 加上拦截器、QueryCache.onError、页面 onError 三层都可能上报同一次失败，
 * 逐条弹 message 就会刷出一叠一样的红条。这里把窗口内的所有错误收敛成一条：
 * 相同文案直接丢弃；不同文案不新开一条，而是把数量合进现有提示。
 *
 * <Notifier> 侧还用了固定 message key 做 UI 层兜底：即使开发期 HMR 让模块状态分裂，
 * antd 也只保留一条错误提示，不会堆叠。
 */
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

/**
 * 供 QueryCache.onError 使用：HTTP 层错误（502 / 超时 / 断连）在拦截器里已经弹过，
 * 这里只补「HTTP 200 + body.code != 200」的业务错误，避免同一次失败提示两遍。
 */
export function notifyQueryError(error: unknown) {
  if (!error || typeof error !== 'object') return
  const err = error as { message?: unknown; kind?: unknown; notified?: boolean }
  if (typeof err.message !== 'string' || err.notified) return
  if (err.kind === 'auth' || err.kind === 'forbidden') return
  ;(error as { notified?: boolean }).notified = true
  toastError(err.message)
}

/**
 * mutation / 手动 catch 的统一错误提示入口。
 *
 * 请求层可能已经弹过一次（notified=true），这里靠 notify 层的批次去重兜住，
 * 页面代码因此可以放心写成 onError: showError，不必关心错误从哪一层冒上来。
 */
export function showError(error: unknown) {
  const message = error instanceof Error ? error.message : String(error ?? '')
  toastError(message || '操作失败，请稍后重试')
}
