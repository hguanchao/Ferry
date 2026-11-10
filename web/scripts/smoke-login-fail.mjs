/**
 * 冒烟测试：登录失败计数
 *
 * 口径必须与后端 AuthUtil.loginLock 一致，否则会出现「本地说锁了、后端没锁」或反之。
 * 重点验证：第 5 次即达上限、换账号互不影响、TTL 过期后从 1 重计、成功后清零。
 *
 * 用法：node scripts/smoke-login-fail.mjs
 */
const store = new Map()
globalThis.localStorage = {
  getItem: (k) => (store.has(k) ? store.get(k) : null),
  setItem: (k, v) => store.set(k, String(v)),
  removeItem: (k) => store.delete(k),
  clear: () => store.clear(),
}

const { bumpFailCount, readFailCount, clearFailCount, MAX_FAIL_COUNT, LOCK_MINUTES } = await import(
  '../src/lib/loginFail.ts'
)

const results = []
function check(name, actual, expected) {
  const pass = JSON.stringify(actual) === JSON.stringify(expected)
  results.push({ name, pass })
  console.log(`${pass ? 'PASS' : 'FAIL'}  ${name}${pass ? '' : ` → 实际 ${JSON.stringify(actual)}`}`)
}

check('默认上限为 5 次', MAX_FAIL_COUNT, 5)
check('锁定时长 15 分钟', LOCK_MINUTES, 15)

store.clear()
// 连续失败：1..5 次累计
const counts = []
for (let i = 0; i < 5; i += 1) counts.push(bumpFailCount('000000', 'admin').count)
check('连续失败累计为 1..5', counts, [1, 2, 3, 4, 5])
check('第 5 次即达上限', counts[4] >= MAX_FAIL_COUNT, true)

// 换账号互不影响
check('换账号从 1 重计', bumpFailCount('000000', 'other').count, 1)
check('原账号计数不受影响', readFailCount('000000', 'admin'), 5)

// 换租户也算不同账号
check('同账号不同租户独立计数', bumpFailCount('000001', 'admin').count, 1)

// 登录成功后清零
clearFailCount('000000', 'admin')
check('成功后计数清零', readFailCount('000000', 'admin'), 0)

// TTL 过期后重新从 1 开始：把该账号记录的过期时间改成过去
store.clear()
bumpFailCount('000000', 'admin')
bumpFailCount('000000', 'admin')
const raw = JSON.parse(store.get('ferry.login.fail'))
raw['000000::admin'].expiresAt = Date.now() - 1000
store.set('ferry.login.fail', JSON.stringify(raw))
check('过期后视为未计数', readFailCount('000000', 'admin'), 0)
check('过期后再失败从 1 重计', bumpFailCount('000000', 'admin').count, 1)

// 脏数据不应抛异常
store.clear()
store.set('ferry.login.fail', 'not-json')
check('脏数据降级为 0', readFailCount('000000', 'admin'), 0)
store.set('ferry.login.fail', JSON.stringify({ who: 'x', count: 'bad', expiresAt: 1 }))
check('字段类型异常降级为 0', readFailCount('000000', 'admin'), 0)

const failed = results.filter((r) => !r.pass)
console.log(`\n${results.length - failed.length}/${results.length} 通过`)
if (failed.length) process.exit(1)