/**
 * 新建流水线顶栏排版冒烟：左右分组对齐、必填错误态不破坏头部、画布位置回归。
 *
 * 复现场景：打开新建抽屉 → 加两个阶段后不填名称与应用直接点保存 → 检查顶栏尺寸与边框。
 * 断言：左右两组的标签行、输入行严格对齐；统计与按钮组等高且有分隔；错误态只保留
 *       红色下划线、应用下拉仍是 6px 圆角；顶栏高度不因校验失败变化；阶段卡片仍在视口内。
 * 用法：node scripts/smoke-pipeline-topbar.mjs
 * 浏览器自动使用本机 Chrome / Edge，也可通过 FERRY_SMOKE_BROWSER 指定可执行文件。
 */
import assert from 'node:assert/strict'
import { spawn } from 'node:child_process'
import { once } from 'node:events'
import { existsSync } from 'node:fs'
import { mkdtemp, readFile, rm } from 'node:fs/promises'
import { tmpdir } from 'node:os'
import { join } from 'node:path'
import { setTimeout as sleep } from 'node:timers/promises'
import { fileURLToPath } from 'node:url'
import { createServer } from 'vite'

const webRoot = fileURLToPath(new URL('../', import.meta.url))
const browserPath = [
  process.env.FERRY_SMOKE_BROWSER,
  'C:/Program Files/Google/Chrome/Application/chrome.exe',
  'C:/Program Files (x86)/Microsoft/Edge/Application/msedge.exe',
  'C:/Program Files/Microsoft/Edge/Application/msedge.exe',
  '/usr/bin/google-chrome',
  '/usr/bin/chromium',
].find((file) => file && existsSync(file))
assert.ok(browserPath, '未找到浏览器，请用 FERRY_SMOKE_BROWSER 指定 Chrome 或 Edge')

async function waitFor(check, message) {
  const deadline = Date.now() + 20000
  while (Date.now() < deadline) {
    const result = await check()
    if (result) return result
    await sleep(100)
  }
  throw new Error(message)
}

async function connect(url) {
  const socket = new WebSocket(url)
  const pending = new Map()
  let id = 0
  socket.addEventListener('message', ({ data }) => {
    const message = JSON.parse(data)
    const entry = pending.get(message.id)
    if (!entry) return
    pending.delete(message.id)
    clearTimeout(entry.timer)
    if (message.error) entry.reject(new Error(message.error.message))
    else entry.resolve(message.result)
  })
  await once(socket, 'open')
  return {
    call(method, params = {}) {
      const requestId = ++id
      return new Promise((resolve, reject) => {
        const timer = setTimeout(() => reject(new Error(`浏览器调用超时：${method}`)), 10000)
        pending.set(requestId, { resolve, reject, timer })
        socket.send(JSON.stringify({ id: requestId, method, params }))
      })
    },
  }
}

const fixturePath = '/__ferry_pipeline_topbar_smoke__'
const fixtureModule = 'virtual:ferry-pipeline-topbar-smoke'
const fixtureSource = `
  import { createElement } from 'react'
  import { createRoot } from 'react-dom/client'
  import { App } from 'antd'
  import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
  import PipelineEditorDrawer from '@/components/PipelineEditorDrawer'
  import '@/styles/app.css'
  createRoot(document.getElementById('root')).render(
    createElement(QueryClientProvider, { client: new QueryClient() },
      createElement(App, null, createElement(PipelineEditorDrawer, {
        open: true, mode: 'create', pipeline: null, onClose: () => {}, onSaved: () => {},
      }))))
`

const temporary = await mkdtemp(join(tmpdir(), 'ferry-topbar-smoke-'))
let server
let browser
let cdp
try {
  server = await createServer({
    root: webRoot,
    configFile: join(webRoot, 'vite.config.ts'),
    cacheDir: join(temporary, 'vite-cache'),
    logLevel: 'error',
    server: { host: '127.0.0.1', port: 0, open: false },
    plugins: [{
      name: 'ferry-pipeline-topbar-smoke',
      resolveId(id) {
        if (id === fixtureModule) return `\0${fixtureModule}`
      },
      load(id) {
        if (id === `\0${fixtureModule}`) return fixtureSource
      },
      configureServer(vite) {
        vite.middlewares.use(async (request, response, next) => {
          if (request.url?.split('?')[0] !== fixturePath) return next()
          try {
            const html = await vite.transformIndexHtml(fixturePath,
              `<!doctype html><html lang="zh-CN"><head><meta charset="UTF-8"><title>顶栏冒烟</title></head><body><div id="root"></div><script type="module" src="/@id/${fixtureModule}"></script></body></html>`)
            response.setHeader('Content-Type', 'text/html; charset=utf-8')
            response.end(html)
          } catch (error) {
            next(error)
          }
        })
      },
    }],
  })
  await server.listen()
  const profile = join(temporary, 'browser-profile')
  browser = spawn(browserPath, [
    '--headless=new', '--disable-gpu', '--no-first-run', '--no-default-browser-check',
    '--window-size=1440,1000', '--remote-debugging-port=0', `--user-data-dir=${profile}`, 'about:blank',
  ], { stdio: 'ignore' })
  let browserError
  browser.on('error', (error) => { browserError = error })
  const portFile = join(profile, 'DevToolsActivePort')
  await waitFor(() => {
    if (browserError) throw browserError
    assert.equal(browser.exitCode, null, '浏览器提前退出')
    return existsSync(portFile)
  }, '浏览器调试端口未就绪')
  const port = (await readFile(portFile, 'utf8')).split('\n')[0].trim()
  const targets = await (await fetch(`http://127.0.0.1:${port}/json/list`)).json()
  const page = targets.find((target) => target.type === 'page')
  cdp = await connect(page.webSocketDebuggerUrl)
  const evaluate = async (expression) => {
    const result = await cdp.call('Runtime.evaluate', { expression, returnByValue: true, awaitPromise: true })
    if (result.exceptionDetails) throw new Error(JSON.stringify(result.exceptionDetails))
    return result.result.value
  }
  const clickJs = async (selector) => {
    await evaluate(`(() => {
      const el = document.querySelector(${JSON.stringify(selector)})
      if (!el) throw new Error('未找到：' + ${JSON.stringify(selector)})
      el.click()
    })()`)
    await sleep(350)
  }
  const rect = (selector) => evaluate(`(() => {
    const el = document.querySelector(${JSON.stringify(selector)})
    if (!el) throw new Error('未找到：' + ${JSON.stringify(selector)})
    const r = el.getBoundingClientRect()
    const s = getComputedStyle(el)
    return { y: r.y, bottom: r.bottom, h: r.height, borderLeft: s.borderLeftWidth,
      radius: s.borderTopLeftRadius, borderTop: s.borderTopWidth, borderBottom: s.borderBottomWidth,
      borderBottomColor: s.borderBottomColor }
  })()`)
  const near = (a, b, message) => assert.ok(Math.abs(a - b) <= 1, `${message}：${a} 与 ${b} 不一致`)

  await cdp.call('Emulation.setDeviceMetricsOverride', { width: 1440, height: 1000, deviceScaleFactor: 1, mobile: false })
  await cdp.call('Page.navigate', { url: `http://127.0.0.1:${server.httpServer.address().port}${fixturePath}` })
  await waitFor(() => evaluate('Boolean(document.querySelector(".ferry-studio__name"))'), '新建抽屉未加载')
  await sleep(1000)

  const crumb = await rect('.ferry-studio__crumb')
  const label = await rect('.ferry-studio__app-label')
  const name = await rect('.ferry-studio__name')
  const select = await rect('.ferry-studio__app .ant-select')
  near(crumb.y, label.y, '左右标签行顶部应对齐')
  near(crumb.h, label.h, '左右标签行高度应一致')
  near(name.y, select.y, '左右输入行顶部应对齐')
  near(name.h, select.h, '左右输入行高度应一致')
  assert.equal(name.borderTop, '0px', '名称输入应保持无边框的标题式样式')
  console.log('PASS  顶栏左右两组的标签行、输入行严格对齐')

  const stats = await rect('.ferry-studio__stats')
  const actions = await rect('.ferry-studio__actions')
  near(stats.y, actions.y, '统计与按钮组顶部应对齐')
  near(stats.h, actions.h, '统计与按钮组高度应一致')
  assert.equal(actions.borderLeft, '1px', '统计与按钮组之间应有竖线分隔')
  console.log('PASS  统计与按钮组等高对齐并有分隔')

  // 空定义会被结构校验提前拦下，先补两个阶段才能走到表单必填校验
  await clickJs('.ferry-studio__empty-inner button')
  await clickJs('.ferry-studio__menu-item:not(:disabled)')
  await clickJs('.ferry-studio__actions > *:nth-child(2)')
  await clickJs('.ferry-studio__menu-item:not(:disabled)')
  await clickJs('.ferry-studio__btn.is-primary')
  await sleep(500)

  const topbar = await rect('.ferry-studio__topbar')
  assert.equal(topbar.h, 72, '校验失败不能撑高顶栏')
  const errorState = await evaluate(`(() => {
    const input = document.querySelector('.ferry-studio__name')
    const select = document.querySelector('.ferry-studio__app .ant-select')
    const style = getComputedStyle(input)
    return {
      marked: input.classList.contains('ant-input-status-error'),
      selectMarked: select.classList.contains('ant-select-status-error'),
      borderTop: style.borderTopWidth, borderLeft: style.borderLeftWidth,
      borderRight: style.borderRightWidth, borderBottom: style.borderBottomWidth,
      borderBottomColor: style.borderBottomColor,
      selectRadius: getComputedStyle(select).borderTopLeftRadius,
    }
  })()`)
  assert.ok(errorState.marked && errorState.selectMarked, '两个必填项都应进入错误态')
  assert.equal(errorState.borderTop, '0px', '错误态不应出现上边框')
  assert.equal(errorState.borderLeft, '0px', '错误态不应出现左边框')
  assert.equal(errorState.borderRight, '0px', '错误态不应出现右边框')
  assert.equal(errorState.borderBottom, '1px', '错误态应保留一条红色下划线')
  assert.equal(errorState.borderBottomColor, 'rgb(220, 38, 38)', '下划线应使用项目危险色')
  assert.equal(errorState.selectRadius, '6px', '应用下拉错误态仍应是项目圆角')
  console.log('PASS  必填错误态只保留红色下划线、下拉圆角与顶栏高度不变')

  await evaluate(`document.querySelector('.ferry-studio__name').focus()`)
  await sleep(150)
  const focused = await evaluate(`(() => {
    const style = getComputedStyle(document.querySelector('.ferry-studio__name'))
    return { borderTop: style.borderTopWidth, borderBottom: style.borderBottomWidth }
  })()`)
  assert.equal(focused.borderTop, '0px', '聚焦错误态输入时也不能出现整圈边框')
  assert.equal(focused.borderBottom, '1px', '聚焦时红色下划线应保留')
  console.log('PASS  聚焦必填错误态输入时仍保持下划线样式')

  const canvas = await evaluate(`(() => {
    const viewport = document.querySelector('.ferry-studio__viewport').getBoundingClientRect()
    return [...document.querySelectorAll('.ferry-studio__card')].map(el => {
      const r = el.getBoundingClientRect()
      return r.top >= viewport.top && r.bottom <= viewport.bottom
    })
  })()`)
  assert.equal(canvas.length, 2, '应有两个阶段卡片')
  assert.ok(canvas.every(Boolean), '阶段卡片应完整落在画布视口内')
  console.log('PASS  两阶段卡片仍完整落在画布视口内')

  await cdp.call('Emulation.setDeviceMetricsOverride', { width: 1100, height: 900, deviceScaleFactor: 1, mobile: false })
  await sleep(400)
  const narrow = await evaluate(`(() => {
    const stats = document.querySelector('.ferry-studio__stats')
    const actions = document.querySelector('.ferry-studio__actions')
    const style = getComputedStyle(actions)
    return {
      statsHidden: getComputedStyle(stats).display === 'none',
      borderLeft: style.borderLeftWidth, paddingLeft: style.paddingLeft,
      right: actions.getBoundingClientRect().right,
    }
  })()`)
  assert.ok(narrow.statsHidden, '窄屏应隐藏统计文字')
  assert.equal(narrow.borderLeft, '0px', '统计隐藏后分隔线应一并收掉')
  assert.equal(narrow.paddingLeft, '0px', '统计隐藏后左内边距应一并收掉')
  assert.ok(narrow.right <= 1100, '窄屏按钮组不应溢出顶栏')
  console.log('PASS  窄屏隐藏统计时同时收掉分隔线与内边距，按钮组不溢出')
} finally {
  cdp?.call?.('Browser.close').catch(() => {})
  browser?.kill()
  await server?.close()
  try {
    await rm(temporary, { recursive: true, force: true })
  } catch {
    // 浏览器进程可能仍占用临时 profile，忽略清理失败
  }
}
