import { useEffect } from 'react'
import { App } from 'antd'
import { bindToast } from '@/lib/notify'

/** 固定 key：同一时刻只保留一条错误提示，新内容替换旧内容而不是往下堆叠 */
const ERROR_TOAST_KEY = 'ferry-error'

/**
 * 把 AntD 的 message 实例注入请求层
 *
 * message 需要 ConfigProvider / App 提供的上下文（主题、locale、层级），
 * 所以只能在 React 树内取；用 useEffect 绑定并在卸载时解绑，兼容 StrictMode 的双次挂载。
 */
export default function Notifier() {
  const { message } = App.useApp()

  useEffect(() => {
    bindToast((text) => {
      message.error({ content: text, key: ERROR_TOAST_KEY, duration: 3.5 })
    })
    return () => bindToast(null)
  }, [message])

  return null
}