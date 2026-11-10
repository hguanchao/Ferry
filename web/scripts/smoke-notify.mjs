/**
 * 冒烟测试：提示通道去重
 *
 * 一次页面动作可能并发触发多个请求失败（列表页同时拉 treeList / pageList / 选项接口），
 * 同一次失败还会经过 3 条路径（axios 拦截器 / QueryCache.onError / 页面 onError）。
 * 这里验证 notify 层把同一批次内的错误收敛成一条提示，不会刷出一叠红条。
 *
 * 用法：node --experimental-strip-types scripts/smoke-notify.mjs
 */
import { bindToast, toastError, notifyQueryError, showError } from '../src/lib/notify.ts'
import { ApiError } from '../src/api/error.ts'

const shown = []
bindToast((text) => shown.push(text))

/** 时间可控：每个用例推进 4s 让批次过期，避免用例之间串味 */
const realNow = Date.now
let clock = realNow()
Date.now = () => clock
const nextBatch = () => {
  clock += 4000
  shown.length = 0
}

const results = []
function check(name, actual, expected) {
  const pass = JSON.stringify(actual) === JSON.stringify(expected)
  results.push({ name, pass })
  console.log(`${pass ? 'PASS' : 'FAIL'}  ${name}${pass ? '' : ` → 实际 ${JSON.stringify(actual)}`}`)
}

// 1. 多个接口同时 404，文案相同 → 只弹一条
nextBatch()
const same = '404 请求未找到'
toastError(same)
toastError(same)
toastError(same)
toastError(same)
check('并发同文案只弹一条', shown.length, 1)
check('内容与首个错误一致', shown[0], same)

// 2. 同批次出现不同文案 → 不新开提示，只在同一条上累加数量
nextBatch()
toastError('错误 A')
toastError('错误 B')
toastError('错误 A')
toastError('错误 C')
check('同批次只维护一条提示', shown.length, 3)
check('聚合文案带失败数量', shown[shown.length - 1], '错误 A（另有 2 个请求失败）')

// 3. 批次窗口过期后允许再弹新提示
nextBatch()
toastError(same)
check('窗口过期后允许新提示', shown.length, 1)

// 4. 502 拦截器已弹过（notified=true）→ QueryCache 不重复
nextBatch()
const err = new ApiError('网关无法访问后端服务（502）', 502, 'gateway')
err.notified = true
notifyQueryError(err)
check('拦截器弹过 → QueryCache 不重复', shown.length, 0)

// 5. 业务错误（拦截器不弹，notified=false）→ QueryCache 补弹
nextBatch()
const biz = new ApiError('租户编码已存在', 500, 'business')
notifyQueryError(biz)
check('业务错误由 QueryCache 补弹', shown.length, 1)
check('补弹后标记 notified', biz.notified, true)

// 6. 鉴权错误不弹通用提示（由 SessionWatcher 统一提示）
nextBatch()
notifyQueryError(new ApiError('令牌失效', 401, 'auth'))
check('401 不走 QueryCache 提示', shown.length, 0)

// 7. 页面 onError 与拦截器重复时，靠批次去重兜住
nextBatch()
const shared = new ApiError('网关无法访问后端服务（502）', 502, 'gateway')
toastError(shared.message) // 拦截器
showError(shared) // 页面 onError
check('拦截器 + 页面 onError 只弹一次', shown.length, 1)

// 8. 空消息不弹
nextBatch()
toastError('   ')
check('空文案不弹', shown.length, 0)

// 9. 解绑期间不残留批次（StrictMode 双挂载不能漏掉后续错误）
nextBatch()
toastError('挂载期错误')
bindToast(null)
bindToast((text) => shown.push(text))
toastError('挂载期错误')
check('解绑重绑后仍能提示', shown.length, 2)

Date.now = realNow
const failed = results.filter((r) => !r.pass)
console.log(`\n${results.length - failed.length}/${results.length} 通过`)
if (failed.length) process.exit(1)
