/**
 * 冒烟脚本：连续 5 次密码错误时是否弹出锁定提示
 *
 * 用 CDP 驱动真实页面（不 mock 前端逻辑），在 mock 网关下连续提交 5 次错误密码，
 * 每轮读取页面上的内联错误与 antd message 文本，验证第 5 次出现锁定提示。
 *
 * 用法：node scripts/smoke-login-lock.mjs
 */
import { spawn } from 'node:child_process'
import { writeFileSync } from 'node:fs'

const CHROME = 'C:/Program Files/Google/Chrome/Application/chrome.exe'
const BASE = process.argv[2] ?? 'http://127.0.0.1:5199'
const PORT = 9334
const OUT = 'C:/Users/Chao/AppData/Local/Temp/login-lock-5th.png'

const chrome = spawn(CHROME, [
  '--headless=new',
  '--disable-gpu',
  '--hide-scrollbars',
  '--no-proxy-server',
  '--window-size=1440,1000',
  `--remote-debugging-port=${PORT}`,
  '--user-data-dir=C:/Users/Chao/AppData/Local/Temp/chrome-ferry-lock',
  `${BASE}/login`,
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

function send(method, params = {}) {
  seq += 1
  const id = seq
  ws.send(JSON.stringify({ id, method, params }))
  return new Promise((resolve, reject) => {
    pending.set(id, { resolve, reject })
    setTimeout(() => {
      if (pending.has(id)) {
        pending.delete(id)
        reject(new Error(`超时 ${method}`))
      }
    }, 20000)
  })
}

await new Promise((resolve, reject) => {
  ws.onopen = resolve
  ws.onerror = reject
})

await send('Runtime.enable')
await send('Page.enable')

async function evaluate(expression) {
  const res = await send('Runtime.evaluate', { expression, returnByValue: true, awaitPromise: true })
  if (res.exceptionDetails) throw new Error(res.exceptionDetails.text)
  return res.result.value
}

/** 等某个表达式为真 */
async function waitFor(expression, timeout = 12000) {
  const started = Date.now()
  while (Date.now() - started < timeout) {
    if (await evaluate(`Boolean(${expression})`)) return true
    await sleep(150)
  }
  return false
}

/** antd 受控输入：必须用原生 setter + input 事件，直接赋值 React 收不到 */
const setInput = (id, value) => `(() => {
  const el = document.getElementById(${JSON.stringify(id)});
  if (!el) return 'no-el';
  const setter = Object.getOwnPropertyDescriptor(HTMLInputElement.prototype, 'value').set;
  setter.call(el, ${JSON.stringify(value)});
  el.dispatchEvent(new Event('input', { bubbles: true }));
  return 'ok';
})()`

const readErrors = `Array.from(document.querySelectorAll('.ferry-login__error')).map(el => el.innerText.trim()).join(' | ')`
// antd v6 的 message 结构是 .ant-message-notice-title（v5 才是 -content）
const readToast = `Array.from(document.querySelectorAll('.ant-message-notice-title')).map(el => el.innerText.trim()).join(' | ')`

console.log('等待登录表单渲染…')
const ready = await waitFor(`document.getElementById('account')`)
if (!ready) throw new Error('登录表单未渲染')

// 清掉可能残留的计数，保证从 1 开始
await evaluate(`localStorage.removeItem('ferry.login.fail'); 'ok'`)
await send('Page.reload')
await waitFor(`document.getElementById('account')`)
await sleep(800)

const rounds = []
for (let attempt = 1; attempt <= 5; attempt += 1) {
  // 每轮重新填表：失败后验证码会被清空并刷新
  await evaluate(setInput('tenant', '000000'))
  await evaluate(setInput('account', 'admin'))
  await evaluate(setInput('password', 'wrong-pass'))
  await evaluate(setInput('captcha', '1234'))
  await sleep(200)

  await evaluate(`document.querySelector('button[type=submit]').click(); 'ok'`)

  // 等这一轮的错误出现（内联错误块或锁定 toast）
  await waitFor(`document.querySelector('.ferry-login__error') || document.querySelector('.ant-message-notice')`)
  await sleep(600)

  const inline = await evaluate(readErrors)
  const toast = await evaluate(readToast)
  rounds.push({ attempt, inline, toast })
  console.log(`第 ${attempt} 次 → 内联: ${inline || '(无)'} | 提示: ${toast || '(无)'}`)

  if (attempt === 5) {
    // 先把焦点挪走关掉租户下拉，再截图，保留 toast
    await evaluate(`document.activeElement && document.activeElement.blur(); 'ok'`)
    await sleep(300)
    const shot = await send('Page.captureScreenshot', { format: 'png' })
    writeFileSync(OUT, Buffer.from(shot.data, 'base64'))
    break
  }

  // 清掉 toast，避免下一轮读到旧的
  await evaluate(`document.querySelectorAll('.ant-message-notice').forEach(el => el.remove()); 'ok'`)
}

const last = rounds[4]
const ok =
  /锁定/.test(last.inline) &&
  /15 分钟/.test(last.inline) &&
  /锁定/.test(last.toast) &&
  !rounds.slice(0, 4).some((r) => /锁定/.test(r.inline))

console.log(`\n第 5 次内联提示：${last.inline}`)
console.log(`第 5 次 toast：${last.toast}`)
console.log(`截图：${OUT}`)
console.log(ok ? 'PASS 第 5 次失败触发锁定提示，前 4 次不提示锁定' : 'FAIL 锁定提示不符合预期')

ws.close()
chrome.kill()
process.exit(ok ? 0 : 1)