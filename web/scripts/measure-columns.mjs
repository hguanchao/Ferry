/**
 * 批量量列宽：把各列表页都跑一遍，输出每列实际像素宽度
 *
 * 用途：找出「某一列吃掉剩余全部宽度」的页面（这种列会异常宽），
 * 改完后再跑一次对比。
 *
 * 用法：node scripts/measure-columns.mjs [/system/tenant ...]
 */
import { spawn } from 'node:child_process'
import { writeFileSync, rmSync } from 'node:fs'
import { join } from 'node:path'

const CHROME = 'C:/Program Files/Google/Chrome/Application/chrome.exe'
const BASE = process.argv[2]?.startsWith('/') ? 'http://127.0.0.1:5199' : process.argv[2] ?? 'http://127.0.0.1:5199'
const PORT = 9341
const TMP = 'C:/Users/Chao/AppData/Local/Temp'

const ROUTES = [
  ['/system/tenant', '租户管理'],
  ['/system/user', '用户管理'],
  ['/system/role', '角色管理'],
  ['/system/dept', '部门管理'],
  ['/system/post', '岗位管理'],
  ['/system/dict', '字典管理'],
  ['/system/config', '参数配置'],
  ['/system/menu', '菜单管理'],
  ['/system/audit', '审计日志'],
]

const SEED = join(process.cwd(), 'public', '__cols_seed.html')
const perms = ROUTES.map(([path, name], i) => ({
  id: String(200 + i),
  parentId: '100',
  name,
  type: 1,
  path,
  component: `/system/${path.split('/').pop()}`,
  sort: i,
}))
const user = {
  userId: '1',
  tenantId: '000000',
  account: 'admin',
  nickname: '管理员',
  isSuper: 1,
  tokenHeader: 'Authorization',
  tokenPrefix: 'Bearer',
  accessToken: 'fake-token-for-smoke',
  permissions: [{ id: '100', parentId: '0', name: '系统管理', type: 0, sort: 1 }, ...perms],
}
writeFileSync(
  SEED,
  `<!doctype html><meta charset="utf-8"><script>
localStorage.setItem('ferry.token','fake-token-for-smoke');
localStorage.setItem('ferry.user',${JSON.stringify(JSON.stringify(user))});
localStorage.setItem('ferry.tenant','000000');
location.replace(new URLSearchParams(location.search).get('to')||'/system/tenant');
</script>`,
  'utf8',
)
process.on('exit', () => {
  try {
    rmSync(SEED, { force: true })
  } catch {
    /* ignore */
  }
})

const chrome = spawn(CHROME, [
  '--headless=new',
  '--disable-gpu',
  '--hide-scrollbars',
  '--no-proxy-server',
  '--window-size=1500,1000',
  `--remote-debugging-port=${PORT}`,
  '--user-data-dir=C:/Users/Chao/AppData/Local/Temp/chrome-ferry-cols',
  `${BASE}/__cols_seed.html?to=${ROUTES[0][0]}`,
])
chrome.stderr.on('data', () => {})

const sleep = (ms) => new Promise((r) => setTimeout(r, ms))

let page
for (let i = 0; i < 60; i += 1) {
  try {
    const list = await (await fetch(`http://127.0.0.1:${PORT}/json/list`)).json()
    page = list.find((t) => t.type === 'page' && String(t.url).startsWith('http'))
    if (page?.webSocketDebuggerUrl) break
  } catch {
    /* 未就绪 */
  }
  await sleep(250)
}
if (!page) throw new Error('找不到页面')

const ws = new WebSocket(page.webSocketDebuggerUrl)
let seq = 0
const pending = new Map()
ws.onmessage = (e) => {
  const m = JSON.parse(e.data)
  const p = pending.get(m.id)
  if (!p) return
  pending.delete(m.id)
  m.error ? p.reject(new Error(m.error.message)) : p.resolve(m.result)
}
const send = (method, params = {}) => {
  const id = ++seq
  ws.send(JSON.stringify({ id, method, params }))
  return new Promise((res, rej) => {
    pending.set(id, { resolve: res, reject: rej })
    setTimeout(() => pending.has(id) && (pending.delete(id), rej(new Error('timeout ' + method))), 15000)
  })
}
await new Promise((res, rej) => {
  ws.onopen = res
  ws.onerror = rej
})

const evaluate = async (expression) => {
  const r = await send('Runtime.evaluate', { expression, returnByValue: true, awaitPromise: true })
  return r.exceptionDetails ? null : r.result.value
}

const summary = []

for (const [path, label] of ROUTES) {
  await evaluate(`location.replace('${BASE}${path}'); 'ok'`)
  let ok = false
  for (let i = 0; i < 50; i += 1) {
    if (await evaluate(`document.querySelectorAll('.ant-table-thead th').length > 1`)) {
      ok = true
      break
    }
    await sleep(250)
  }
  await sleep(900)
  if (!ok) {
    console.log(`${label.padEnd(6)} 未渲染出表头`)
    continue
  }
  const box = await evaluate(
    `(() => { const w = document.querySelector('.ferry-table-wrap') || document.querySelector('.ant-table');
      return JSON.stringify({ wrap: Math.round(w.getBoundingClientRect().width), table: Math.round(document.querySelector('.ant-table table').getBoundingClientRect().width) }) })()`,
  )
  const heads = await evaluate(
    `JSON.stringify(Array.from(document.querySelectorAll('.ant-table-thead th'))
      .map(th => ({ t: th.innerText.trim(), w: Math.round(th.getBoundingClientRect().width) }))
      .filter(x => x.t))`,
  )
  const cols = JSON.parse(heads ?? '[]')
  const total = cols.reduce((sum, c) => sum + c.w, 0) || 1
  const widest = cols.reduce((a, b) => (a.w > b.w ? a : b), cols[0] ?? { t: '-', w: 0 })
  const ratio = Math.round((widest.w / total) * 100)
  summary.push({ label, cols, ratio, widest })
  const b = JSON.parse(box ?? '{}')
  const over = b.table > b.wrap + 1 ? '  ⚠ 溢出' : ''
  console.log(`${label.padEnd(6)} 容器${b.wrap} 表格${b.table}${over}`)
  console.log(`        ${cols.map((c) => `${c.t}:${c.w}`).join('  ')}`)
  console.log(`        最宽列「${widest.t}」占 ${ratio}%`)

  const shot = await send('Page.captureScreenshot', { format: 'png' })
  writeFileSync(`${TMP}/cols-${path.split('/').pop()}.png`, Buffer.from(shot.data, 'base64'))
}

console.log('\n=== 需要关注的页面（单列占比 > 28%）===')
const bad = summary.filter((s) => s.ratio > 28)
if (!bad.length) console.log('（无）')
bad.forEach((s) => console.log(`${s.label}：${s.widest.t} ${s.widest.w}px（${s.ratio}%）`))

ws.close()
chrome.kill()
process.exit(0)