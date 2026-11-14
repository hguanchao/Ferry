import type { ApiEnvelope } from '@/api/types'

/** 错误分类：调用方据此决定内联展示、静默重试还是引导用户排查环境 */
export type ApiErrorKind =
  | 'auth' // 401，令牌失效
  | 'forbidden' // 403，无权限
  | 'business' // HTTP 200 且 body.code != 200，业务校验失败
  | 'gateway' // 502/503/504，网关或上游服务不可用
  | 'offline' // 请求没到服务端：服务未启动 / 跨域 / 断网
  | 'timeout'
  | 'http' // 其余 4xx / 5xx
  | 'unknown'

export class ApiError extends Error {
  code: number
  kind: ApiErrorKind
  /** 已经弹过全局提示，调用方不要再弹一遍 */
  notified = false

  constructor(message: string, code = 500, kind: ApiErrorKind = 'unknown') {
    super(message)
    this.name = 'ApiError'
    this.code = code
    this.kind = kind
  }
}

/** 面向用户的文案统一说「后端服务」，不暴露 ferry-* 内部服务名 */
export function hideServiceName(text: string): string {
  return text.replace(/\bferry-[a-z0-9-]+\b/gi, '后端服务')
}

/** 把 axios 抛出的错误翻译成带分类的 ApiError（纯函数，便于单测） */
export function describe(error: unknown): ApiError {
  const err = error as {
    response?: { status?: number; data?: unknown }
    code?: string
  }
  const status = err?.response?.status
  const body = err?.response?.data as ApiEnvelope<unknown> | undefined

  // 后端有结构化响应时优先用它的 msg，保住业务语义（但要脱敏）
  const serverMsg = body && typeof body === 'object' && typeof body.msg === 'string' ? hideServiceName(body.msg) : ''

  if (status === 401) {
    return new ApiError(serverMsg || '访问令牌已失效，请重新登录', 401, 'auth')
  }
  if (status === 403) {
    return new ApiError(serverMsg || '无操作权限', 403, 'forbidden')
  }
  if (status === 502) {
    return new ApiError(serverMsg || '网关无法访问后端服务（502），该服务可能未启动或已停止', 502, 'gateway')
  }
  if (status === 503) {
    return new ApiError(serverMsg || '后端服务暂不可用（503），服务可能正在启动或重启中', 503, 'gateway')
  }
  if (status === 504) {
    return new ApiError(serverMsg || '网关等待后端服务响应超时（504），服务可能已停止', 504, 'gateway')
  }
  if (status === 404) {
    return new ApiError(serverMsg || '接口不存在（404），请确认后端版本与前端一致', 404, 'http')
  }
  if (status && status >= 500) {
    return new ApiError(serverMsg || `后端服务内部错误（${status}）`, status, 'http')
  }
  if (status) {
    return new ApiError(serverMsg || `请求失败（${status}）`, status, 'http')
  }

  // 没有 response，说明请求根本没到服务端
  if (err?.code === 'ECONNABORTED' || err?.code === 'ETIMEDOUT') {
    return new ApiError('请求超时，后端无响应，请稍后重试', 0, 'timeout')
  }
  return new ApiError('无法连接后端服务，请确认网关与后端服务已启动', 0, 'offline')
}