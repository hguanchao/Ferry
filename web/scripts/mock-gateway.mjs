/**
 * 冒烟用的假网关（仅测试用，不参与构建）
 *
 * 覆盖登录页依赖的三个接口：
 * - 租户列表：返回默认租户，让下拉有数据
 * - 图形验证码：返回一张 1x1 PNG，前端只校验长度 4
 * - 登录：固定返回「账号或密码错误」，用于验证第 5 次失败时的锁定提示
 *
 * 用法：node scripts/mock-gateway.mjs [port]
 */
import { createServer } from 'node:http'

const port = Number(process.argv[2] ?? 9100)

// 1x1 透明 PNG
const PNG = 'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNkYPhfDwAChwGA60e6kgAAAABJRU5ErkJggg=='

function json(res, body, status = 200) {
  const payload = JSON.stringify(body)
  res.writeHead(status, { 'Content-Type': 'application/json; charset=utf-8' })
  res.end(payload)
}

createServer((req, res) => {
  const url = (req.url ?? '').split('?')[0]

  // 与真实后端 RoleVO 完全一致的字段：没有 menuIds / dataScope / memberCount / remark
  if (url.endsWith('/role/pageList')) {
    json(res, {
      code: 200,
      msg: 'ok',
      data: {
        records: [
          { id: '1', tenantId: '000000', code: 'admin', name: '超级管理员', status: 1, sort: 1, createTime: '2026-01-01 00:00:00' },
          { id: '2', tenantId: '000000', code: 'release', name: '发布管理员', status: 1, sort: 2, createTime: '2026-01-02 00:00:00' },
        ],
        total: 2,
        size: 30,
        current: 1,
        pages: 1,
      },
    })
    return
  }

  if (url.endsWith('/menu/treeList')) {
    json(res, {
      code: 200,
      msg: 'ok',
      data: [{ id: '100', parentId: '0', name: '系统管理', type: 0, sort: 1, children: [] }],
    })
    return
  }

  if (url.endsWith('/tenant/pageList')) {
    json(res, {
      code: 200,
      msg: 'ok',
      data: {
        records: [
          { id: '1', code: '000000', name: '默认租户', status: 1, createTime: '2026-01-01 00:00:00' },
          { id: '2', code: '000001', name: '试点租户', status: 1, createTime: '2026-01-02 00:00:00' },
        ],
        total: 2,
        size: 30,
        current: 1,
        pages: 1,
      },
    })
    return
  }

  // 后端未实现该接口，真实网关会返回 404
  if (url.includes('/apps/scope-options')) {
    res.writeHead(404, { 'Content-Type': 'text/html' })
    res.end('<html><body>404 Not Found</body></html>')
    return
  }

  /** 通用分页：给各列表页喂一份字段齐全的假数据，用于量列宽 */
  if (url.endsWith('/pageList')) {
    const records = [1, 2, 3].map((n) => ({
      id: String(n),
      tenantId: '000000',
      code: `demo${n}`,
      name: `示例数据 ${n}`,
      nickname: `示例用户${n}`,
      account: `account${n}`,
      status: n === 2 ? 0 : 1,
      sort: n,
      type: n,
      remark: `说明 ${n}`,
      phone: '13800000000',
      email: `u${n}@ferry.dev`,
      deptId: '1',
      postId: '1',
      roleId: '1',
      createTime: '2026-01-01 00:00:00',
      updateTime: '2026-01-02 00:00:00',
      lastLoginTime: '2026-01-03 08:00:00',
      operateTime: '2026-01-03 08:00:00',
      operator: '管理员',
      operatorAccount: 'admin',
      operateType: '修改',
      objectType: '用户',
      objectId: '1001',
      objectName: '示例对象',
      action: '更新配置项',
      beforeValue: '旧值示例',
      afterValue: '新值示例',
      ip: '10.0.0.1',
      ticketNo: 'CHG-2026-0001',
      result: '成功',
      builtin: false,
      memberCount: 3,
      menuIds: ['1', '2', '3'],
      parentId: '0',
      ancestors: '0',
      path: '/system/demo',
      component: '/system/Demo',
      perms: 'system:demo:list',
      icon: 'setting',
      isShow: 1,
      isFrame: 0,
      isCache: 0,
      value: 'demo',
      isInternal: 1,
      dataScope: { mode: 'all', pairs: [], prodOps: false },
    }))
    json(res, {
      code: 200,
      msg: 'ok',
      data: { records, total: records.length, size: 30, current: 1, pages: 1 },
    })
    return
  }

  /** 树形接口：部门/菜单/字典/区域 */
  if (url.endsWith('/treeList') || url.endsWith('/list') || url.endsWith('/lazyList')) {
    json(res, {
      code: 200,
      msg: 'ok',
      data: [
        {
          id: '1',
          parentId: '0',
          code: 'demo',
          name: '示例节点',
          label: '示例节点',
          nickname: '示例用户1',
          account: 'account1',
          status: 1,
          sort: 1,
          type: 1,
          isLeaf: true,
          path: '/system/demo',
          children: [],
        },
      ],
    })
    return
  }

  if (url.endsWith('/tenant/list')) {
    json(res, {
      code: 200,
      msg: 'ok',
      data: [{ id: '1', code: '000000', name: '默认租户', status: 1 }],
    })
    return
  }

  if (url.includes('/captcha')) {
    json(res, { code: 200, msg: 'ok', data: { captcha: PNG } })
    return
  }

  if (url.endsWith('/ferry-auth/login')) {
    json(res, { code: 500, msg: '账号或密码错误', data: null })
    return
  }

  json(res, { code: 200, msg: 'ok', data: null })
}).listen(port, '127.0.0.1', () => {
  console.log(`mock gateway on ${port}`)
})