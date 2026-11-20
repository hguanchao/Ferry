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
import type { MenuRoute } from '@/router/menu'
import { useMenuRoutes } from '@/router/useMenuRoutes'
import { buildTheme } from '@/theme'
import { UNAUTHORIZED_EVENT } from '@/lib/session'
import { notifyQueryError } from '@/lib/notify'
import { useUiStore } from '@/stores/ui'
import { useAuthStore } from '@/stores/auth'

/** 运行详情深链：菜单只注册 /pipeline/run，带运行 id 时复用同一页面组件 */
const RUN_DETAIL_ROUTE: MenuRoute = {
  id: 'run-detail',
  path: '/pipeline/run',
  name: '运行详情',
  component: 'devops/RunDetail',
  trail: ['流水线管理', '运行详情'],
  parentIds: [],
}

const queryClient = new QueryClient({
  /** 查询失败的兜底提示：页面 useQuery 多数只渲染表格，统一补业务错误提示，HTTP 层错误拦截器已弹过 */
  queryCache: new QueryCache({ onError: notifyQueryError }),
  defaultOptions: {
    queries: {
      retry: false,
      refetchOnWindowFocus: false,
      staleTime: 20_000,
    },
  },
})

/** 令牌失效时统一清理会话并回到登录页：store 与 localStorage 同时归零，否则会陷入来回弹的死循环 */
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

  /** 动态路由：菜单树里每个 type=1 节点生成一条扁平 Route，目录只做导航分组不注册路由 */
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
                <Route path="/pipeline/run/:id" element={<DynamicPage route={RUN_DETAIL_ROUTE} />} />
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
