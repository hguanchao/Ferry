/**
 * 冒烟测试：错误分类
 *
 * 直接 import src/api/error.ts（纯函数，无浏览器依赖），断言 502 / 断网 / 超时 /
 * 业务码 等场景下的 kind 与文案。截图里 captcha 502 却无提示，就是分类缺失导致的，
 * 这里锁死这些分支，避免以后回归。
 *
 * 用法：npx vite-node scripts/smoke-api-error.mjs
 */
import { describe, ApiError, hideServiceName } from '../src/api/error.ts'

const results = []
function check(name, actual, expected) {
  const pass = actual === expected
  results.push({ name, pass })
  console.log(`${pass ? 'PASS' : 'FAIL'}  ${name}${pass ? '' : ` → 实际 ${actual}，期望 ${expected}`}`)
}

/** 构造一个形似 axios 错误的输入 */
const axiosErr = ({ status, data, url = '/ferry-auth/1/captcha', code } = {}) => ({
  response: status ? { status, data } : undefined,
  config: { url },
  code,
})

// 1. 截图场景：网关 502
const e502 = describe(axiosErr({ status: 502 }))
check('502 → kind=gateway', e502.kind, 'gateway')
check('502 文案含状态码', /502/.test(e502.message), true)
check('502 文案不暴露内部服务名', /ferry-[a-z-]+/i.test(e502.message), false)
console.log('         →', e502.message)

// 502 但后端给了结构化 msg，应保留业务语义
const e502b = describe(axiosErr({ status: 502, data: { code: 502, msg: '上游连接被拒绝', data: null } }))
check('502 带后端 msg 时优先用后端文案', e502b.message, '上游连接被拒绝')

// 后端 msg 里夹带上游服务名时也要脱敏
const e502c = describe(axiosErr({ status: 502, data: { code: 502, msg: 'ferry-system 连接被拒绝', data: null } }))
check('后端 msg 里的服务名被脱敏', !/ferry-[a-z-]+/i.test(e502c.message) && e502c.message.includes('后端服务'), true)

// 2. 网关有响应但上游超时 / 正在重启
check('503 → kind=gateway', describe(axiosErr({ status: 503 })).kind, 'gateway')
check('504 → kind=gateway', describe(axiosErr({ status: 504 })).kind, 'gateway')

// 3. 服务根本没启动（连不上，无 response）
const eOffline = describe(axiosErr({ code: 'ERR_NETWORK' }))
check('连不上 → kind=offline', eOffline.kind, 'offline')
check('offline 文案提示检查服务启动', /服务已启动/.test(eOffline.message), true)
console.log('         →', eOffline.message)

// 4. 超时
check('ECONNABORTED → kind=timeout', describe(axiosErr({ code: 'ECONNABORTED' })).kind, 'timeout')

// 5. 鉴权
check('401 → kind=auth', describe(axiosErr({ status: 401 })).kind, 'auth')
check('403 → kind=forbidden', describe(axiosErr({ status: 403 })).kind, 'forbidden')

// 6. 其余 HTTP
check('404 → kind=http', describe(axiosErr({ status: 404 })).kind, 'http')
check('500 → kind=http', describe(axiosErr({ status: 500 })).kind, 'http')

// 7. 所有面向用户的文案统一称「后端服务」，不暴露内部服务名（含 404 里原本拼的 URL）
const leaked = [401, 403, 404, 500, 502, 503, 504]
  .map((status) => `${status}: ${describe(axiosErr({ status })).message}`)
  .filter((text) => /ferry-[a-z-]+|ferry-(auth|system)/i.test(text))
check('各状态码文案均不暴露服务名', leaked.length, 0)
check('502 文案统一称「后端服务」', describe(axiosErr({ status: 502 })).message.includes('后端服务'), true)
const offline = describe(axiosErr({ code: 'ERR_NETWORK' }))
check('断网文案同样不暴露服务名', /ferry-[a-z-]+/i.test(offline.message), false)

// 8. 业务错误 msg 的脱敏入口（client.ts unwrap 复用）
check('业务 msg 中的服务名被替换', hideServiceName('无法访问 ferry-system：ferry-auth 已下线'), '无法访问 后端服务：后端服务 已下线')

// 9. ApiError 默认值
const bare = new ApiError('x')
check('ApiError 默认 notified=false', bare.notified, false)
check('ApiError 是 Error 实例', bare instanceof Error, true)

const failed = results.filter((r) => !r.pass)
console.log(`\n${results.length - failed.length}/${results.length} 通过`)
if (failed.length) process.exit(1)