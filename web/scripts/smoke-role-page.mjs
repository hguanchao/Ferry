/**
 * 冒烟：角色管理页在「后端字段缺失 + scope-options 404」时是否还能正常渲染
 *
 * 复现场景（用户报错）：
 *   GET /api/ferry-system/apps/scope-options 404
 *   Role.tsx:243 TypeError: Cannot read properties of undefined (reading 'length')
 *
 * 断言：页面无未捕获异常、出现「—」占位、不出现 404 错误 toast。
 *
 * 用法：node scripts/smoke-role-page.mjs
 */
import { spawn } from 'node:child_process'
import { writeFileSync, rmSync } from 'node:fs'
import { join } from 'node:path'

const CHROME = 'C:/Program Files/Google/Chrome/Application/chrome.exe'
const BASE = process.argv[2] ?? 'http://127.0.0.1:5199'
const PORT = 9336
const OUT = 'C:/Users/Chao/AppData/Local/Temp/role-page.png'

/**
 * 需要一个已登录会话来进内页。public/ 下的文件会被构建进产物，所以这里临时生成、
 * 跑完立刻删掉，不在仓库里留痕迹。
 */
const SEED = join(process.cwd(), 'public', '__role_seed.html')
const seedHtml = `<!doctype html><meta charset="utf-8"><script>
const user={userId:'1',tenantId:'000000',account:'admin',nickname:'管理员',isSuper:1,
tokenHeader:'Authorization',tokenPrefix:'Bearer',accessToken:'fake-token-for-smoke',
permissions:[{id:'100',parentId:'0',name:'系统管理',type:0,sort:1},
{id:'101',parentId:'100',name:'角色管理',type:1,path:'/system/role',component:'/system/Role',sort:1}]};
localStorage.setItem('ferry.token','fake-token-for-smoke');
localStorage.setItem('ferry.user',JSON.stringify(user));
localStorage.setItem('ferry.tenant','000000');
location.replace('/system/role');
</script>`
writeFileSync(SEED, seedHtml, 'utf8')

const cleanup = () => {
  try {
    rmSync(SEED, { force: true })
  } catch {
    /* ignore */
  }
}
process.on('exit', cleanup)
process.on('SIGINT', () => {
  cleanup()
  process.exit(1)
})

const chrome = spawn(CHROME, [
  '--headless=new',
  '--disable-gpu',
  '--hide-scrollbars',
  '--no-proxy-server',
  '--window-size=1500,1000',
  `--remote-debugging-port=${PORT}`,
  '--user-data-dir=C:/Users/Chao/AppData/Local/Temp/chrome-ferry-role',
  `${BASE}/__role_seed.html`,
])
chrome.stderr.on('data', () => {})

const sleep = (ms) => new Promise((r) => setTimeout(r, ms))

async function findPage() {
  for (let i = 0; i < 60; i += 1) {
    try {
      const list = await (await fetch(`http://127.0.0.1:${PORT}/json/list`)).json()
      const page = list.find((t) => t.type === 'page' && String(t.url).startsWith('http'))
      if (page?.webSocketDebuggerUrl) return page
    } catch {
      /* 未就绪 */
    }
    await sleep(250)
  }
  throw new Error('找不到页面 target')
}

const page = await findPage()
const ws = new WebSocket(page.webSocketDebuggerUrl)
let seq = 0
const pending = new Map()

ws.onmessage = (event) => {
  const msg = JSON.parse(event.data)
  const entry = pending.get(msg.id)
  if (!entry) return
  pending.delete(msg.id)
  if (msg.error) entry.reject(new Error(msg.error.message))
  else entry.resolve(msg.result)
}

const send = (method, params = {}) => {
  seq += 1
  const id = seq
  ws.send(JSON.stringify({ id, method, params }))
  return new Promise((resolve, reject) => {
    pending.set(id, { resolve, reject })
    setTimeout(() => pending.has(id) && (pending.delete(id), reject(new Error('timeout ' + method))), 15000)
  })
}

await new Promise((resolve, reject) => {
  ws.onopen = resolve
  ws.onerror = reject
})

const exceptions = []
await send('Runtime.enable')
ws.addEventListener('message', (event) => {
  const msg = JSON.parse(event.data)
  if (msg.method === 'Runtime.exceptionThrown') {
    exceptions.push(msg.params?.exceptionDetails?.exception?.description ?? 'unknown')
  }
})

const evaluate = async (expression) => {
  const res = await send('Runtime.evaluate', { expression, returnByValue: true, awaitPromise: true })
  if (res.exceptionDetails) return { __error: res.exceptionDetails.text }
  return res.result.value
}

// 等页面切到角色管理并渲染出表格行
let rendered = false
for (let i = 0; i < 60; i += 1) {
  const ok = await evaluate(`document.querySelectorAll('.ant-table-tbody tr.ant-table-row').length > 0`)
  if (ok === true) {
    rendered = true
    break
  }
  await sleep(250)
}

await sleep(1200)

const rowText = await evaluate(
  `Array.from(document.querySelectorAll('.ant-table-tbody tr.ant-table-row')).map(tr => tr.innerText.replace(/\\s+/g,' ')).join(' || ')`,
)
const toast = await evaluate(
  `Array.from(document.querySelectorAll('.ant-message-notice-title')).map(el => el.innerText).join(' | ')`,
)
const headers = await evaluate(
  `Array.from(document.querySelectorAll('.ant-table-thead th')).map(th => th.innerText.trim()).filter(Boolean).join(' | ')`,
)
const errorBar = await evaluate(
  `Array.from(document.querySelectorAll('.ant-alert-error')).map(el => el.innerText.replace(/\\s+/g,' ')).join(' | ')`,
)

const shot = await send('Page.captureScreenshot', { format: 'png' })
writeFileSync(OUT, Buffer.from(shot.data, 'base64'))

// 再验一遍编辑抽屉：数据权限 Tab 直接用 dataScope.mode，是第二个崩溃点
await evaluate(`(() => {
  const btn = Array.from(document.querySelectorAll('.ant-table-tbody button')).find(b => b.innerText.includes('编辑'));
  if (btn) btn.click();
  return 'ok';
})()`)
await sleep(1200)
const drawerOpen = await evaluate(`Boolean(document.querySelector('.ant-drawer-body'))`)
await evaluate(`(() => {
  const tab = Array.from(document.querySelectorAll('.ant-drawer .ant-tabs-tab')).find(el => el.innerText.includes('数据权限'));
  if (tab) tab.click();
  return 'ok';
})()`)
await sleep(800)
const drawerText = await evaluate(
  `(document.querySelector('.ant-drawer-body')?.innerText ?? '').replace(/\\s+/g,' ').slice(0, 200)`,
)

const shot2 = await send('Page.captureScreenshot', { format: 'png' })
writeFileSync(OUT.replace('.png', '-drawer.png'), Buffer.from(shot2.data, 'base64'))

console.log(`渲染出表格行: ${rendered}`)
console.log(`行文本: ${rowText}`)
console.log(`表头: ${headers}`)
console.log(`toast: ${toast || '(无)'}`)
console.log(`错误告警条: ${errorBar || '(无)'}`)
console.log(`编辑抽屉打开: ${drawerOpen}`)
console.log(`抽屉数据权限页: ${drawerText || '(空)'}`)
console.log(`未捕获异常: ${exceptions.length ? exceptions.join(' / ') : '(无)'}`)
console.log(`截图: ${OUT}`)

// 表格只展示后端真有的字段：不能出现整列占位，也不能再崩
const requiredHeaders = ['角色名称', '角色编码', '排序', '状态', '创建时间', '操作']
const headersOk = requiredHeaders.every((h) => headers.includes(h))
const noPlaceholderColumn = !/成员|功能权限|数据权限|职责描述/.test(headers)
const ok =
  rendered &&
  headersOk &&
  noPlaceholderColumn &&
  exceptions.length === 0 &&
  !/404/.test(toast) &&
  drawerOpen === true
console.log(
  ok
    ? 'PASS 列表只展示已有字段、无空列、编辑抽屉正常、404 未污染 UI'
    : 'FAIL 表头或渲染不符合预期',
)

ws.close()
chrome.kill()
process.exit(ok ? 0 : 1)