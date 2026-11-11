import { useEffect, useMemo } from 'react'
import { App as AntApp, ConfigProvider } from 'antd'
import zhCN from 'antd/locale/zh_CN'
import { QueryCache, QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { BrowserRouter, Navigate, Route, Routes, useNavigate } from 'react-router-dom'
import AppLayout from '@/layouts/AppLayout'
import Notifier from '@/components/Notifier'
import LoginPage from '@/pages/login/LoginPage'
import WorkbenchPage from '@/pages/workbench/WorkbenchPage'
import ProfilePage from '@/pages/profile/ProfilePage'
import NotFoundPage from '@/pages/error/NotFoundPage'
import ForbiddenPage from '@/pages/error/ForbiddenPage'
import RequireAuth from '@/router/RequireAuth'
import DynamicPage from '@/router/DynamicPage'
import { useMenuRoutes } from '@/router/useMenuRoutes'
import { buildTheme } from '@/theme'
import { UNAUTHORIZED_EVENT } from '@/lib/session'
import { notifyQueryError } from '@/lib/notify'
import { useUiStore } from '@/stores/ui'
import { useAuthStore } from '@/stores/auth'

const queryClient = new QueryClient({
  /**
   * 查询失败的兜底提示。
   *
   * 页面里的 useQuery 大多只渲染表格，失败后如果不弹提示，用户看到的就是一张空表，
   * 会误判成「没有数据」而不是「接口挂了」。这里统一补业务错误提示；
   * HTTP 层错误已在拦截器弹过（kind 非 business），不会重复。
   *
   * 错误只走全局 message 弹出提示，页面内不再挂错误横幅，避免同一次失败提示两遍。
   */
  queryCache: new QueryCache({ onError: notifyQueryError }),
  defaultOptions: {
    queries: {
      retry: false,
      refetchOnWindowFocus: false,
      staleTime: 20_000,
    },
  },
})

/**
 * 令牌失效时统一清理会话并回到登录页。
 *
 * 只清 localStorage 不够：路由守卫与登录页读的是 store，store 里残留的 token/user
 * 会让 /login 立刻把用户弹回工作台，形成「401 → 跳登录 → 弹回 → 再 401」的闪烁死循环。
 * 因此这里先 clearSession 归零 store，再做跳转；并发 401 用 wasAuthed 去重，只提示一次。
 */
function SessionWatcher() {
  const navigate = useNavigate()
  const { message } = AntApp.useApp()

  useEffect(() => {
    const handler = () => {
      const wasAuthed = Boolean(useAuthStore.getState().token)
      useAuthStore.getState().clearSession()
      if (!wasAuthed) return

      message.warning('登录状态已失效，请重新登录')
      const from = window.location.pathname
      navigate('/login', { replace: true, state: from === '/login' ? undefined : { from } })
    }
    window.addEventListener(UNAUTHORIZED_EVENT, handler)
    return () => window.removeEventListener(UNAUTHORIZED_EVENT, handler)
  }, [navigate, message])

  return null
}

export default function App() {
  const theme = useUiStore((state) => state.theme)
  const token = useAuthStore((state) => state.token)
  const { routes } = useMenuRoutes()

  useEffect(() => {
    document.documentElement.dataset.theme = theme
  }, [theme])

  /** 会话结束（登出或令牌失效）时清缓存，避免下一个账号看到上一个账号的数据 */
  useEffect(() => {
    if (!token) queryClient.clear()
  }, [token])

  /**
   * 动态路由：菜单树里的每个 type=1 节点生成一条扁平 Route。
   * 目录不注册路由，只负责导航分组；因此后端调整层级不会影响前端匹配。
   */
  const dynamicRoutes = useMemo(
    () =>
      routes.map((route) => <Route key={route.id} path={route.path} element={<DynamicPage route={route} />} />),
    [routes],
  )

  return (
    <ConfigProvider
      locale={zhCN}
      theme={buildTheme(theme)}
      componentSize="middle"
      form={{ validateMessages: { required: '请填写${label}' } }}
      modal={{ centered: true }}
    >
      <AntApp>
        <QueryClientProvider client={queryClient}>
          <BrowserRouter>
            <Notifier />
            <SessionWatcher />
            <Routes>
              <Route path="/login" element={<LoginPage />} />
              <Route
                element={
                  <RequireAuth>
                    <AppLayout />
                  </RequireAuth>
                }
              >
                <Route path="/" element={<Navigate to="/workbench" replace />} />
                <Route path="/workbench" element={<WorkbenchPage />} />
                {dynamicRoutes}
                <Route path="/profile" element={<ProfilePage />} />
                <Route path="/403" element={<ForbiddenPage />} />
                <Route path="*" element={<NotFoundPage />} />
              </Route>
            </Routes>
          </BrowserRouter>
        </QueryClientProvider>
      </AntApp>
    </ConfigProvider>
  )
}
