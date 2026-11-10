import axios, { type AxiosRequestConfig } from 'axios'
import type { ApiEnvelope } from '@/api/types'
import { ApiError, describe, hideServiceName } from '@/api/error'
import { session } from '@/lib/session'
import { toastError } from '@/lib/notify'

export { ApiError }
export type { ApiErrorKind } from '@/api/error'

const http = axios.create({
  baseURL: '/api',
  timeout: 20_000,
  headers: { 'Content-Type': 'application/json' },
})

http.interceptors.request.use((config) => {
  const token = session.readToken()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

http.interceptors.response.use(
  (response) => response,
  (error) => {
    const apiError = describe(error)

    if (apiError.kind === 'auth') {
      session.clear()
      session.notifyUnauthorized()
      // 401 由 SessionWatcher 统一提示并跳登录页，这里不再重复弹
      apiError.notified = true
      return Promise.reject(apiError)
    }

    // silent 由调用方透传，用于登录页这类需要内联展示错误的场景
    const silent = Boolean((error as { config?: { silent?: boolean } })?.config?.silent)
    if (!silent) {
      toastError(apiError.message)
      apiError.notified = true
    }
    return Promise.reject(apiError)
  },
)

export interface RequestOptions extends AxiosRequestConfig {
  /** 不弹全局提示，改由调用方内联展示（如登录页的错误块） */
  silent?: boolean
}

async function unwrap<T>(promise: Promise<{ data: ApiEnvelope<T> }>, silent = false): Promise<T> {
  const response = await promise
  const body = response.data
  if (body && typeof body.code === 'number' && body.code !== 200) {
    if (body.code === 401) {
      session.clear()
      session.notifyUnauthorized()
      throw new ApiError(hideServiceName(body.msg || '访问令牌已失效，请重新登录'), 401, 'auth')
    }
    // 业务错误同样可能带上游实例名，统一收敛后再进入 toast / 登录页内联错误
    const error = new ApiError(hideServiceName(body.msg || '请求失败'), body.code, 'business')
    if (!silent) {
      toastError(error.message)
      error.notified = true
    }
    throw error
  }
  return body?.data as T
}

/** silent 必须留在 axios config 上，拦截器在 response 阶段才能读到；axios 会原样保留未知字段 */
export function get<T>(url: string, params?: Record<string, unknown>, options?: RequestOptions): Promise<T> {
  return unwrap(http.get<ApiEnvelope<T>>(url, { params, ...options }), options?.silent)
}

export function post<T>(url: string, body?: unknown, options?: RequestOptions): Promise<T> {
  return unwrap(http.post<ApiEnvelope<T>>(url, body ?? {}, options), options?.silent)
}