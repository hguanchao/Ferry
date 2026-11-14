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
  async (error) => {
    // blob 错误体没有统一响应结构，不走 describe，按后端文案转成 ApiError 抛给 download
    const blob = (error as { response?: { data?: unknown } })?.response?.data
    if (blob instanceof Blob) {
      return Promise.reject(await toBlobError(blob))
    }

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

/** 下载文件：按 content-type 区分文件流与 JSON 错误体，错误体转成 ApiError 抛出 */
export async function download(url: string, body: unknown, fileName: string): Promise<void> {
  const response = await http.post(url, body ?? {}, { responseType: 'blob', timeout: 60_000 })

  const contentType = String(response.headers['content-type'] ?? '')
  if (contentType.includes('application/json')) {
    throw await toBlobError(response.data as Blob)
  }

  // 文件名以响应头为准：后端按 RFC 5987 编码过，中文名才能正确还原
  const disposition = String(response.headers['content-disposition'] ?? '')
  const matched = /filename\*=UTF-8''([^;]+)/i.exec(disposition)
  const name = matched ? decodeURIComponent(matched[1]) : `${fileName}.xlsx`

  const blobUrl = URL.createObjectURL(response.data as Blob)
  const anchor = document.createElement('a')
  anchor.href = blobUrl
  anchor.download = name
  document.body.appendChild(anchor)
  anchor.click()
  document.body.removeChild(anchor)
  URL.revokeObjectURL(blobUrl)
}

/** 把错误响应体（Blob）还原成带后端提示的 ApiError */
async function toBlobError(blob: Blob): Promise<ApiError> {
  const text = await blob.text()
  try {
    const parsed = JSON.parse(text) as ApiEnvelope<unknown>
    return new ApiError(hideServiceName(parsed.msg || '导出失败'), parsed.code || 500, 'business')
  } catch {
    return new ApiError('导出失败，请稍后重试', 500, 'business')
  }
}
