/**
 * 画布交互冒烟：任务入口、侧栏页签与 YAML 草稿。
 *
 * 复现场景：点击任务链入口、选中任务后往返概览与属性、输入错误 YAML 后切换页签。
 * 断言：任务只加入所属阶段；真实鼠标能切换页签；错误草稿不丢失且不能保存；窄屏侧栏不溢出。
 * 用法：node scripts/smoke-pipeline-canvas.mjs
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
    socket,
    call(method, params = {}) {
      const requestId = ++id
      return new Promise((resolve, reject) => {
        const timer = setTimeout(() => {
          pending.delete(requestId)
          reject(new Error(`浏览器调用超时：${method}`))
        }, 10000)
        pending.set(requestId, { resolve, reject, timer })
        socket.send(JSON.stringify({ id: requestId, method, params }))
      })
    },
  }
}

const fixturePath = '/__ferry_pipeline_canvas_smoke__'
const fixtureModule = 'virtual:ferry-pipeline-canvas-smoke'
const fixtureSource = `
  import { createElement, useState } from 'react'
  import { createRoot } from 'react-dom/client'
  import { App } from 'antd'
  import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
  import PipelineDefinitionEditor from '@/components/PipelineDefinitionEditor'
  import PipelineEditorDrawer from '@/components/PipelineEditorDrawer'
  import { createStage } from '@/lib/pipeline'
  import '@/styles/app.css'
  const initialDefinition = { stages: [createStage('BUILD')] }
  function Canvas() {
    const [definition, setDefinition] = useState(initialDefinition)
    const [valid, setValid] = useState(true)
    return createElement(App, null,
      createElement('div', { style: { display: 'flex', height: '100vh' } },
        createElement(PipelineDefinitionEditor, {
          definition, onChange: setDefinition, initialDefinition, onValidityChange: setValid,
        })),
      createElement('output', { hidden: true, 'data-smoke-definition': '', 'data-smoke-valid': String(valid) }, JSON.stringify(definition)))
  }
  const content = new URLSearchParams(location.search).has('drawer')
    ? createElement(QueryClientProvider, { client: new QueryClient() },
        createElement(App, null, createElement(PipelineEditorDrawer, {
          open: true, mode: 'definition', onClose: () => {}, onSaved: () => {},
          pipeline: {
            id: '1', name: '冒烟流水线', appId: '1', appName: '冒烟应用', appCode: 'smoke',
            currentVersionNo: 1, definition: initialDefinition,
          },
        })))
    : createElement(Canvas)
  createRoot(document.getElementById('root')).render(content)
`

const temporary = await mkdtemp(join(tmpdir(), 'ferry-canvas-smoke-'))
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
      name: 'ferry-pipeline-canvas-smoke',
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
              `<!doctype html><html lang="zh-CN"><head><meta charset="UTF-8"><title>画布冒烟</title></head><body><div id="root"></div><script type="module" src="/@id/${fixtureModule}"></script></body></html>`)
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
  assert.ok(page, '浏览器没有可用页面')
  cdp = await connect(page.webSocketDebuggerUrl)
  const evaluate = async (expression) => {
    const result = await cdp.call('Runtime.evaluate', { expression, returnByValue: true, awaitPromise: true })
    if (result.exceptionDetails) throw new Error(JSON.stringify(result.exceptionDetails))
    return result.result.value
  }
  const definition = async () => JSON.parse(await evaluate('document.querySelector("[data-smoke-definition]").textContent'))
  const click = async (selector, index = 0) => {
    const point = await evaluate(`(() => {
      const element = document.querySelectorAll(${JSON.stringify(selector)})[${index}]
      if (!element) throw new Error('未找到点击目标：' + ${JSON.stringify(selector)})
      const rect = element.getBoundingClientRect()
      const x = rect.left + rect.width / 2
      const y = rect.top + rect.height / 2
      return { x, y, hit: element.contains(document.elementFromPoint(x, y)) }
    })()`)
    assert.ok(point.hit, `点击目标被遮挡或移出视口：${selector}`)
    await cdp.call('Input.dispatchMouseEvent', { type: 'mouseMoved', x: point.x, y: point.y })
    await cdp.call('Input.dispatchMouseEvent', { type: 'mousePressed', x: point.x, y: point.y, button: 'left', clickCount: 1 })
    await cdp.call('Input.dispatchMouseEvent', { type: 'mouseReleased', x: point.x, y: point.y, button: 'left', clickCount: 1 })
    await sleep(200)
  }
  const clickTab = (index) => click('.ferry-studio__tabs [role="tab"]', index)
  const selectedTab = () => evaluate('document.querySelector(".ferry-studio__tabs [aria-selected=true]")?.textContent.trim()')
  const inputYaml = async (text) => {
    await click('.ferry-studio__yaml')
    const modifiers = process.platform === 'darwin' ? 4 : 2
    await cdp.call('Input.dispatchKeyEvent', { type: 'keyDown', key: 'a', code: 'KeyA', windowsVirtualKeyCode: 65, modifiers })
    await cdp.call('Input.dispatchKeyEvent', { type: 'keyUp', key: 'a', code: 'KeyA', windowsVirtualKeyCode: 65, modifiers })
    await cdp.call('Input.insertText', { text })
    await sleep(200)
  }
  let navigation = 0
  const navigate = async (drawer = false) => {
    await cdp.call('Emulation.setDeviceMetricsOverride', { width: 1440, height: 1000, deviceScaleFactor: 1, mobile: false })
    await cdp.call('Page.navigate', {
      url: `http://127.0.0.1:${server.httpServer.address().port}${fixturePath}?case=${++navigation}${drawer ? '&drawer' : ''}`,
    })
    await waitFor(() => evaluate('Boolean(document.querySelector(".ferry-studio__chain-add"))'), '画布未完成加载')
    await sleep(400)
  }
  await navigate()

  await click('.ferry-studio__chain-add')
  let current = await definition()
  assert.equal(current.stages[0].tasks.length, 1, '点击「暂无任务」应添加任务，不能打开阶段菜单')
  assert.equal(current.stages.length, 1, '添加任务不能增加阶段')
  assert.equal(await evaluate('document.querySelectorAll(".ferry-studio__task-node.is-selected").length'), 1, '新增任务应自动选中')
  assert.equal(await evaluate('document.activeElement.value'), '新任务 1', '新增任务后应聚焦任务名称')
  assert.equal(await evaluate('Boolean(document.querySelector(".ferry-studio__menu"))'), false, '添加任务不能创建阶段菜单弹层')
  console.log('PASS  空任务入口追加任务、选中并聚焦，不弹出阶段菜单')

  await click('.ferry-studio__mini-add')
  await click('.ferry-studio__chain-add')
  current = await definition()
  assert.equal(current.stages.length, 1)
  assert.deepEqual(current.stages[0].tasks.map((task) => task.name), ['新任务 1', '新任务 2', '新任务 3'])
  console.log('PASS  卡片加号与非空任务链入口共用任务添加逻辑')

  await click('.ferry-studio__stage-add')
  assert.equal(await evaluate('document.querySelector(".ferry-studio__menu").getAttribute("aria-label")'), '添加阶段')
  await click('.ferry-studio__menu-item:not(:disabled)')
  current = await definition()
  assert.equal(current.stages.length, 2)
  assert.equal(current.stages[1].type, 'UNIT_TEST')
  assert.equal(current.stages[0].tasks.length, 3)
  console.log('PASS  阶段入口仍打开阶段菜单并添加新阶段')

  await click('.ferry-studio__chain-add', 1)
  current = await definition()
  assert.equal(current.stages[0].tasks.length, 3)
  assert.equal(current.stages[1].tasks.length, 1)
  assert.equal(current.stages.length, 2)
  console.log('PASS  多阶段下任务只添加到点击入口所属阶段')

  const failures = []
  const scenario = async (name, run) => {
    try {
      await run()
      console.log(`PASS  ${name}`)
    } catch (error) {
      failures.push(`${name}：${error.message}`)
      console.error(`FAIL  ${name}：${error.message}`)
    }
  }

  await scenario('概览与属性可反复切换且保留选中任务', async () => {
    await navigate()
    await click('.ferry-studio__chain-add')
    for (let count = 0; count < 3; count += 1) {
      await clickTab(0)
      assert.equal(await selectedTab(), '概览')
      await clickTab(1)
      assert.equal(await selectedTab(), '属性', '从概览点击属性必须真正切换，不能仍停在概览')
      assert.equal(await evaluate('document.querySelector(".ferry-studio__field input").value'), '新任务 1')
      assert.equal(await evaluate('document.querySelectorAll(".ferry-studio__task-node.is-selected").length'), 1)
    }
    await click('.ferry-studio__tabs button svg', 2)
    assert.equal(await selectedTab(), 'YAML', '点击页签图标也应切换，不能被画布拖拽拦截')
    await clickTab(3)
    assert.equal(await selectedTab(), '定义 JSON')
    await clickTab(1)
    assert.equal(await selectedTab(), '属性')
  })

  await scenario('错误 YAML 不锁页签、不丢草稿且持续上报无效', async () => {
    await navigate()
    const before = await definition()
    await clickTab(2)
    const originalYaml = await evaluate('document.querySelector(".ferry-studio__yaml").value')
    const invalidYaml = 'stages: [\n'
    await inputYaml(invalidYaml)
    assert.equal(await evaluate('document.querySelector("[data-smoke-valid]").dataset.smokeValid'), 'false')
    await clickTab(3)
    assert.equal(await selectedTab(), '定义 JSON', '错误 YAML 也应允许切出查看其他页签')
    assert.ok(await evaluate('Boolean(document.querySelector(".ferry-studio__yaml-tip[role=alert]"))'), '切出后仍应显示草稿错误')
    assert.deepEqual(await definition(), before, '错误 YAML 不能覆盖最近合法定义')
    await clickTab(0)
    assert.equal(await selectedTab(), '概览')
    await clickTab(2)
    assert.equal(await evaluate('document.querySelector(".ferry-studio__yaml").value'), invalidYaml)
    assert.equal(await evaluate('document.querySelector("[data-smoke-valid]").dataset.smokeValid'), 'false')
    await inputYaml(originalYaml)
    assert.equal(await evaluate('document.querySelector("[data-smoke-valid]").dataset.smokeValid'), 'true')
    await clickTab(3)
    assert.equal(await selectedTab(), '定义 JSON')
    assert.equal(await evaluate('Boolean(document.querySelector(".ferry-studio__yaml-tip[role=alert]"))'), false)
  })

  await scenario('侧栏桌面加宽且窄屏页签实际点击无溢出遮挡', async () => {
    await navigate()
    assert.equal(await evaluate('document.querySelector(".ferry-studio__inspector").getBoundingClientRect().width'), 420)
    for (const width of [1024, 480, 360]) {
      await cdp.call('Emulation.setDeviceMetricsOverride', { width, height: 1000, deviceScaleFactor: 1, mobile: false })
      await sleep(200)
      const bounds = await evaluate(`(() => {
        const rect = document.querySelector('.ferry-studio__inspector').getBoundingClientRect()
        return { left: rect.left, right: rect.right, width: rect.width }
      })()`)
      assert.ok(bounds.left >= 0 && bounds.right <= width + 1, `${width}px 视口的侧栏不能超出屏幕`)
      assert.ok(bounds.width <= width * 0.94 + 1, `${width}px 视口应保留画布可用空间`)
      await clickTab(2)
      assert.equal(await selectedTab(), 'YAML')
      await clickTab(3)
      assert.equal(await selectedTab(), '定义 JSON')
      await clickTab(0)
      assert.equal(await selectedTab(), '概览')
    }
    await click('[aria-label="收起属性面板"]')
    await click('[aria-label="展开属性面板"]')
    assert.equal(await selectedTab(), '概览')
    assert.equal(await evaluate('document.querySelectorAll(".ferry-studio__tabs button")[1].disabled'), true, '未选中节点时属性页签应明确禁用')
  })

  await scenario('实际抽屉中错误草稿禁用保存，修正或重置后恢复', async () => {
    await navigate(true)
    await clickTab(2)
    const originalYaml = await evaluate('document.querySelector(".ferry-studio__yaml").value')
    await inputYaml('stages: [\n')
    assert.equal(await evaluate('document.querySelector(".ferry-studio__btn.is-primary").disabled'), true)
    await clickTab(0)
    assert.equal(await selectedTab(), '概览')
    assert.equal(await evaluate('document.querySelector(".ferry-studio__btn.is-primary").disabled'), true, '切页不能绕过草稿校验')
    await clickTab(2)
    assert.equal(await evaluate('document.querySelector(".ferry-studio__yaml").value'), 'stages: [\n')
    await inputYaml(originalYaml)
    assert.equal(await evaluate('document.querySelector(".ferry-studio__btn.is-primary").disabled'), false)
    await inputYaml('stages: [\n')
    await clickTab(3)
    await click('.ferry-studio__actions .ferry-studio__btn.is-ghost')
    assert.equal(await selectedTab(), '概览')
    assert.equal(await evaluate('document.querySelector(".ferry-studio__btn.is-primary").disabled'), false)
    assert.equal(await evaluate('document.querySelectorAll(".ferry-studio__tabs button")[1].disabled'), true)
    await clickTab(2)
    assert.equal(await evaluate('document.querySelector(".ferry-studio__yaml").value'), originalYaml, '重置应恢复初始定义而非保留错误文本')
  })

  assert.equal(failures.length, 0, failures.join('\n'))
} finally {
  if (cdp) {
    try {
      await cdp.call('Browser.close')
    } catch (error) {
      console.warn(`关闭浏览器时未收到确认：${error.message}`)
    }
    cdp.socket.close()
  }
  if (browser && browser.exitCode === null) {
    await Promise.race([once(browser, 'exit'), sleep(3000)])
    if (browser.exitCode === null && browser.signalCode === null) browser.kill()
  }
  await server?.close()
  await rm(temporary, { recursive: true, force: true, maxRetries: 20, retryDelay: 100 })
}
