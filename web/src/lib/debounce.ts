import { useEffect, useState } from 'react'

/**
 * 返回延迟更新的值，用于输入过程中减少高频请求。
 *
 * @param value 原始值
 * @param delay 延迟毫秒数
 * @returns 延迟后的值
 */
export function useDebouncedValue<T>(value: T, delay = 400): T {
  const [debounced, setDebounced] = useState(value)
  useEffect(() => {
    const timer = window.setTimeout(() => setDebounced(value), delay)
    return () => window.clearTimeout(timer)
  }, [value, delay])
  return debounced
}
