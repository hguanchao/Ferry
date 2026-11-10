import { useCallback, useEffect, useState } from 'react'
import { Navigate, useLocation, useNavigate } from 'react-router-dom'
import { App, AutoComplete, Button, Checkbox, Form, Input, Tooltip } from 'antd'
import {
  ArrowRightOutlined,
  CheckCircleFilled,
  ExclamationCircleFilled,
  LockOutlined,
  ReloadOutlined,
  SafetyCertificateOutlined,
  ShopOutlined,
  UserOutlined,
} from '@ant-design/icons'
import { authApi } from '@/api/auth'
import { ApiError } from '@/api/client'
import { tenantApi } from '@/api/system'
import type { TenantRow } from '@/api/types'
import { session } from '@/lib/session'
import { MAX_FAIL_COUNT, LOCK_MINUTES, bumpFailCount, clearFailCount } from '@/lib/loginFail'
import { useAuthStore } from '@/stores/auth'

interface LoginForm {
  tenant: string
  account: string
  password: string
  captcha: string
  remember: boolean
}

const PILLARS = [
  '流水线定义与版本快照，运行时只读',
  '质量门禁三级作用域，豁免全程留痕',
  '制品不可变，晋级不重新构建',
  'AI 归因强制携带证据链与置信度',
]

function BrandMark({ size = 34 }: { size?: number }) {
  return (
    <svg width={size} height={size} viewBox="0 0 24 24" fill="none" aria-hidden>
      <path
        d="M3 15.5c2.2 0 2.2 1.7 4.4 1.7S9.6 15.5 11.8 15.5s2.2 1.7 4.4 1.7 2.2-1.7 4.4-1.7"
        stroke="#ffffff"
        strokeWidth="1.7"
        strokeLinecap="round"
        opacity="0.92"
      />
      <path d="M4.6 12.6 12 5l7.4 7.6" stroke="#ffffff" strokeWidth="1.7" strokeLinecap="round" strokeLinejoin="round" />
      <path d="M9.2 12.6V17m5.6-4.4V17" stroke="#ffffff" strokeWidth="1.7" strokeLinecap="round" />
    </svg>
  )
}

export default function LoginPage() {
  const navigate = useNavigate()
  const location = useLocation()
  const { message, modal } = App.useApp()
  const token = useAuthStore((state) => state.token)
  const user = useAuthStore((state) => state.user)
  const login = useAuthStore((state) => state.login)
  const [form] = Form.useForm<LoginForm>()
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [captcha, setCaptcha] = useState('')
  const [captchaLoading, setCaptchaLoading] = useState(false)
  const [captchaError, setCaptchaError] = useState('')
  const [tenants, setTenants] = useState<TenantRow[]>([])
  const [tenantsLoading, setTenantsLoading] = useState(false)
  const [tenantsFailed, setTenantsFailed] = useState(false)
  /** 后端不可用时的全局横幅；登录页拿不到任何接口数据，必须显式告知而不是让用户猜 */
  const [serviceError, setServiceError] = useState('')

  const refreshCaptcha = useCallback(async () => {
    setCaptchaLoading(true)
    try {
      const result = await authApi.captcha({ silent: true })
      setCaptcha(result.captcha)
      setCaptchaError(result.captcha ? '' : '验证码内容为空，请点击刷新')
      if (result.captcha) setServiceError('')
    } catch (err) {
      setCaptcha('')
      const text = err instanceof ApiError ? err.message : '验证码加载失败'
      setCaptchaError(text)
      setServiceError(text)
    } finally {
      setCaptchaLoading(false)
    }
  }, [])

  /**
   * 租户列表走免登录接口，供登录页选择；接口不可用时退化为手工输入编码。
   * 输入框展示租户名称，列表到达后把已填的编码/名称统一归一为名称，
   * 默认值优先级：本次输入 > 上次登录租户 > 列表首个启用租户 > 保留原值。
   */
  const loadTenants = useCallback(async () => {
    setTenantsLoading(true)
    try {
      const list = await tenantApi.list({ silent: true })
      const enabled = list.filter((item) => item.status !== 0)
      setTenants(enabled)
      setTenantsFailed(false)
      const current = String(form.getFieldValue('tenant') ?? '').trim()
      const matched =
        enabled.find((item) => item.name === current || item.code === current) ??
        enabled.find((item) => item.code === session.readTenant()) ??
        enabled[0]
      if (matched) form.setFieldValue('tenant', matched.name)
      setServiceError('')
    } catch (err) {
      setTenants([])
      setTenantsFailed(true)
      setServiceError(err instanceof ApiError ? err.message : '租户列表加载失败')
    } finally {
      setTenantsLoading(false)
    }
  }, [form])

  useEffect(() => {
    void refreshCaptcha()
  }, [refreshCaptcha])

  useEffect(() => {
    const remembered = session.readTenant()
    const account = session.readRemember()
    form.setFieldsValue({
      tenant: remembered || '000000',
      ...(account ? { account, remember: true } : {}),
    })
    void loadTenants()
  }, [form, loadTenants])

  // 必须 token 与 user 同时齐全才回工作台；只有 token 时会被 RequireAuth 再打回登录页，形成来回跳
  if (token && user) {
    return <Navigate to="/workbench" replace />
  }

  const target = (location.state as { from?: string } | null)?.from ?? '/workbench'

  /** 输入框展示租户名称，接口只认租户编码；手工输入编码时原样透传 */
  const resolveTenantCode = (input: string) => {
    const value = input.trim()
    return tenants.find((item) => item.name === value)?.code ?? value
  }

  const onFinish = async (values: LoginForm) => {
    setLoading(true)
    setError('')
    const tenantId = resolveTenantCode(values.tenant)
    const account = values.account.trim()
    try {
      const user = await login({
        tenantId,
        account,
        password: values.password,
        captchaCode: values.captcha.trim(),
        remember: values.remember,
      })
      clearFailCount(tenantId, account)
      message.success(`欢迎回来，${user.nickname}`)
      navigate(target, { replace: true })
    } catch (err) {
      const text = err instanceof ApiError ? err.message : '登录失败，请稍后重试'

      if (/次数过多/.test(text)) {
        // 后端已判定锁定（计数可能来自其它设备或已过期重算），直接按锁定态提示
        const locked = `账号已锁定 ${LOCK_MINUTES} 分钟，请稍后再试`
        setError(locked)
        message.error(locked)
      } else if (/账号或密码错误/.test(text)) {
        const record = bumpFailCount(tenantId, account)
        if (record.count >= MAX_FAIL_COUNT) {
          const locked = `密码已连续错误 ${MAX_FAIL_COUNT} 次，账号锁定 ${LOCK_MINUTES} 分钟`
          setError(locked)
          message.error(locked)
        } else {
          setError(`${text}，还可尝试 ${MAX_FAIL_COUNT - record.count} 次`)
        }
      } else {
        setError(text)
      }

      form.setFieldValue('captcha', '')
      void refreshCaptcha()
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="ferry-login">
      <section className="ferry-login__brand">
        <div className="ferry-login__grid" />
        <svg className="ferry-login__waves" viewBox="0 0 1200 420" preserveAspectRatio="none" aria-hidden>
          <defs>
            <linearGradient id="waveA" x1="0" y1="0" x2="1" y2="0">
              <stop offset="0%" stopColor="#2f7fe0" stopOpacity="0.10" />
              <stop offset="55%" stopColor="#63b3ff" stopOpacity="0.28" />
              <stop offset="100%" stopColor="#8fd4ff" stopOpacity="0.12" />
            </linearGradient>
            <linearGradient id="waveB" x1="0" y1="0" x2="1" y2="0">
              <stop offset="0%" stopColor="#ffffff" stopOpacity="0.06" />
              <stop offset="50%" stopColor="#a7d8ff" stopOpacity="0.18" />
              <stop offset="100%" stopColor="#ffffff" stopOpacity="0.04" />
            </linearGradient>
          </defs>
          <path
            className="ferry-login__wave"
            d="M0 250 C 150 210, 300 300, 450 260 C 600 220, 750 300, 900 258 C 1050 216, 1150 280, 1280 250 L1280 420 L0 420 Z"
            fill="url(#waveA)"
          />
          <path
            className="ferry-login__wave ferry-login__wave--slow"
            d="M0 300 C 160 268, 320 340, 480 306 C 640 272, 800 344, 960 308 C 1080 280, 1180 330, 1280 306 L1280 420 L0 420 Z"
            fill="url(#waveB)"
          />
        </svg>

        <div className="ferry-login__brand-top">
          <span className="ferry-brand__mark" style={{ width: 38, height: 38, borderRadius: 11 }}>
            <BrandMark size={21} />
          </span>
          <span className="ferry-login__wordmark">
            <strong>渡口 Ferry</strong>
            <span>Delivery Platform</span>
          </span>
        </div>

        <div className="ferry-login__brand-body">
          <span className="ferry-login__eyebrow">
            <span className="ferry-login__eyebrow-dot" />
            内部研发交付平台 · 仅授权访问
          </span>
          <h1 className="ferry-login__headline">
            把交付过程，
            <br />
            渡到<em>确定的一岸</em>。
          </h1>
          <p className="ferry-login__lede">
            以应用维度串联代码、构建、门禁、制品、部署与生产发布。每一次变更都有证据，每一次放量都可回退。
          </p>

          <div className="ferry-login__pillars">
            {PILLARS.map((item) => (
              <div className="ferry-login__pillar" key={item}>
                <CheckCircleFilled />
                <span>{item}</span>
              </div>
            ))}
          </div>
        </div>

        <div className="ferry-login__brand-foot">
          <span>Ferry v1.4.0</span>
          <span>编排服务可用性 ≥ 99.9%</span>
          <span>审计留存 365 天</span>
        </div>
      </section>

      <section className="ferry-login__panel">
        <div className="ferry-login__panel-top">
          <a
            href="#"
            onClick={(event) => {
              event.preventDefault()
              modal.info({
                title: '忘记密码？',
                content:
                  '本系统身份由权限中心统一托管。请通过企业 IM 联系平台管理员，或在权限中心发起密码重置，重置后所有会话立即失效。',
                okText: '我知道了',
              })
            }}
          >
            忘记密码
          </a>
          <span style={{ color: 'var(--border-3)' }}>|</span>
          <span>v1.4.0</span>
        </div>

        <div className="ferry-login__form-wrap">
          <h2 className="ferry-login__title">登录渡口</h2>

          {serviceError && (
            <div className="ferry-login__error ferry-login__error--banner" role="alert">
              <ExclamationCircleFilled style={{ marginTop: 2, flex: '0 0 auto' }} />
              <div style={{ flex: 1, minWidth: 0 }}>
                <strong style={{ display: 'block', fontWeight: 600 }}>后端服务暂不可用</strong>
                <span>{serviceError}</span>
              </div>
              <Button
                size="small"
                type="link"
                style={{ padding: 0, height: 'auto', fontSize: 12 }}
                onClick={() => {
                  void refreshCaptcha()
                  void loadTenants()
                }}
              >
                重试
              </Button>
            </div>
          )}

          <Form<LoginForm>
            form={form}
            className="ferry-login__form"
            layout="vertical"
            size="large"
            requiredMark={false}
            initialValues={{ remember: true }}
            onFinish={onFinish}
          >
            <Form.Item
              name="tenant"
              label="租户"
              rules={[
                { required: true, message: '请选择或输入租户' },
                {
                  validator: (_, value: string) => {
                    if (!value || tenants.length === 0) return Promise.resolve()
                    const hit = tenants.some((item) => item.name === value.trim() || item.code === value.trim())
                    return hit ? Promise.resolve() : Promise.reject(new Error('未匹配到租户，请重新选择'))
                  },
                },
              ]}
              extra={tenantsFailed ? '租户列表加载失败，请手工输入租户编码' : undefined}
            >
              <AutoComplete
                options={tenants.map((item) => ({ value: item.name, label: `${item.name}（${item.code}）` }))}
                placeholder={tenantsLoading ? '正在加载租户…' : '选择或输入租户名称'}
                suffixIcon={<ShopOutlined style={{ color: 'var(--text-4)' }} />}
                allowClear
                filterOption={(input, option) => {
                  const keyword = input.trim().toLowerCase()
                  return (
                    String(option?.value ?? '').toLowerCase().includes(keyword) ||
                    String(option?.label ?? '').toLowerCase().includes(keyword)
                  )
                }}
              />
            </Form.Item>

            <Form.Item
              name="account"
              label="账号"
              rules={[{ required: true, message: '请输入账号' }]}
            >
              <Input
                prefix={<UserOutlined style={{ color: 'var(--text-4)' }} />}
                placeholder="请输入账号"
                autoComplete="username"
                autoFocus
              />
            </Form.Item>

            <Form.Item
              name="password"
              label="密码"
              rules={[
                { required: true, message: '请输入密码' },
                { min: 6, message: '密码至少 6 位' },
              ]}
            >
              <Input.Password
                prefix={<LockOutlined style={{ color: 'var(--text-4)' }} />}
                placeholder="请输入密码"
                autoComplete="current-password"
              />
            </Form.Item>

            <Form.Item label="图形验证码" required>
              <div className="ferry-login__captcha">
                <Form.Item
                  name="captcha"
                  noStyle
                  rules={[
                    { required: true, message: '请输入验证码' },
                    { len: 4, message: '验证码为 4 位' },
                  ]}
                >
                  <Input
                    prefix={<SafetyCertificateOutlined style={{ color: 'var(--text-4)' }} />}
                    placeholder="4 位验证码"
                    maxLength={4}
                    autoComplete="off"
                  />
                </Form.Item>
                <Tooltip title="看不清？点击刷新">
                  <div
                    className="ferry-login__captcha-img"
                    onClick={() => void refreshCaptcha()}
                    role="button"
                    tabIndex={0}
                    onKeyDown={(event) => {
                      if (event.key === 'Enter') void refreshCaptcha()
                    }}
                  >
                    {captcha ? (
                      <img src={captcha} alt="图形验证码" />
                    ) : (
                      <span style={{ fontSize: 12, color: captchaError ? '#b42318' : 'var(--text-3)' }}>
                        {captchaLoading ? '加载中…' : captchaError ? '加载失败，点击重试' : '点击重试'}
                      </span>
                    )}
                  </div>
                </Tooltip>
              </div>
              {/* 服务不可用时顶部横幅已点明根因，这里不再重复同一句话；
                  只有在「接口成功但内容为空」这类横幅覆盖不到的情况才就地提示 */}
              {captchaError && !serviceError && (
                <div className="ferry-login__error" role="alert">
                  <ExclamationCircleFilled style={{ marginTop: 2, flex: '0 0 auto' }} />
                  <span>{captchaError}</span>
                </div>
              )}
            </Form.Item>

            <div className="ferry-login__row">
              <Form.Item name="remember" valuePropName="checked" noStyle>
                <Checkbox>记住账号</Checkbox>
              </Form.Item>
              <button
                type="button"
                onClick={() => void refreshCaptcha()}
                style={{
                  border: 'none',
                  background: 'transparent',
                  color: 'var(--text-3)',
                  fontSize: 12,
                  cursor: 'pointer',
                  display: 'inline-flex',
                  alignItems: 'center',
                  gap: 4,
                }}
              >
                <ReloadOutlined style={{ fontSize: 11 }} />
                刷新验证码
              </button>
            </div>

            {error && (
              <div className="ferry-login__error" role="alert">
                <ExclamationCircleFilled style={{ marginTop: 2, flex: '0 0 auto' }} />
                <span>{error}</span>
              </div>
            )}

            <Button
              type="primary"
              htmlType="submit"
              block
              size="large"
              loading={loading}
              style={{ height: 42, fontSize: 14, marginTop: 4 }}
            >
              {loading ? '正在验证…' : '登录'}
              {!loading && <ArrowRightOutlined style={{ fontSize: 12 }} />}
            </Button>
          </Form>

          <div className="ferry-login__hint">
            <span>初始租户</span>
            <code>000000</code>
            <span style={{ marginLeft: 'auto' }}>初始账号</span>
            <code>admin</code>
            <span>/</span>
            <code>123456</code>
          </div>

          <p style={{ marginTop: 14, fontSize: 12, color: 'var(--text-4)', lineHeight: 1.6 }}>
            登录即表示同意遵守《流水线规范》《制品管理规范》与《AI 使用与安全规范》。所有写操作将进入审计。
          </p>
        </div>

        <div className="ferry-login__panel-foot">
          <span>© 2026 渡口 Ferry · 内部研发平台</span>
          <span>技术支持：平台工程组</span>
        </div>
      </section>
    </div>
  )
}
