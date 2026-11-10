import { get, post } from './client'
import type { AuthInfo } from './types'

export interface CaptchaResult {
  captcha: string
}

export const authApi = {
  /** 后端返回的可能是裸 base64，补上 data URL 前缀再交给 <img> */
  captcha: async (options?: { silent?: boolean }) => {
    const result = await get<CaptchaResult>('/ferry-auth/1/captcha', undefined, options)
    const captcha = result.captcha ?? ''
    return { captcha: captcha.startsWith('data:') || !captcha ? captcha : `data:image/png;base64,${captcha}` }
  },

  /** 登录失败在页面内联展示错误块，不走全局 toast */
  login: (
    payload: { grantType: 'password' | 'captcha'; account: string; password: string; tenantId?: string },
    captchaCode?: string,
  ) =>
    post<AuthInfo>('/ferry-auth/login', payload, {
      silent: true,
      ...(captchaCode ? { headers: { 'Captcha-Code': captchaCode } } : {}),
    }),

  logout: () => post<string>('/ferry-auth/logout', undefined, { silent: true }),
}