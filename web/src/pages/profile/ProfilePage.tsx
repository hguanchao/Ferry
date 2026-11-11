import { useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import { useMutation } from '@tanstack/react-query'
import { App, Alert, Avatar, Button, Form, Input, Tabs, Tag } from 'antd'
import { CheckCircle2, Lock, Mail, Phone, ShieldCheck, User } from 'lucide-react'
import PageHeader from '@/components/PageHeader'
import { profileApi } from '@/api/system'
import { useAuthStore } from '@/stores/auth'
import { showError } from '@/lib/notify'

export default function ProfilePage() {
  const { message } = App.useApp()
  const user = useAuthStore((state) => state.user)
  const refreshUser = useAuthStore((state) => state.refreshUser)
  const [params, setParams] = useSearchParams()
  const [profileForm] = Form.useForm()
  const [passwordForm] = Form.useForm()
  const [tab, setTab] = useState(params.get('tab') === 'security' ? 'security' : 'basic')

  const profileMutation = useMutation({
    mutationFn: (values: { nickname: string; email?: string; phone?: string }) => profileApi.update(values),
    onSuccess: () => {
      message.success('资料已更新')
      if (user) refreshUser({ ...user, nickname: profileForm.getFieldValue('nickname') })
    },
    onError: showError,
  })

  const passwordMutation = useMutation({
    mutationFn: (values: { oldPassword: string; newPassword: string }) => profileApi.changePassword(values),
    onSuccess: () => {
      message.success('密码已更新，请使用新密码重新登录')
      passwordForm.resetFields()
    },
    onError: showError,
  })

  const roleName = user?.isSuper === 1 ? '超级管理员' : '平台用户'

  return (
    <>
      <PageHeader title="个人中心" description="维护本人资料与登录口令。权限与数据范围由角色决定，如需调整请联系平台管理员。" />

      <div className="ferry-profile">
        <aside className="ferry-panel" style={{ padding: 22 }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 14 }}>
            <Avatar
              size={54}
              style={{ background: 'linear-gradient(145deg,#1c4fd6,#35b6ff)', fontSize: 20, fontWeight: 600 }}
            >
              {user?.nickname?.slice(0, 1) ?? 'U'}
            </Avatar>
            <div style={{ minWidth: 0 }}>
              <div style={{ fontSize: 16, fontWeight: 600, color: 'var(--text-1)' }}>{user?.nickname}</div>
              <div className="ferry-mono" style={{ fontSize: 12, color: 'var(--text-3)' }}>
                {user?.account}
              </div>
            </div>
          </div>

          <div style={{ marginTop: 18, display: 'flex', flexDirection: 'column', gap: 10 }}>
            <div className="ferry-hstack" style={{ justifyContent: 'space-between' }}>
              <span className="ferry-caption">角色</span>
              <Tag color="blue">{roleName}</Tag>
            </div>
            <div className="ferry-hstack" style={{ justifyContent: 'space-between' }}>
              <span className="ferry-caption">租户编码</span>
              <span className="ferry-mono" style={{ fontSize: 12 }}>
                {user?.tenantId}
              </span>
            </div>
            <div className="ferry-hstack" style={{ justifyContent: 'space-between' }}>
              <span className="ferry-caption">可访问菜单</span>
              <span style={{ fontSize: 12 }}>{user?.permissions.filter((item) => item.type === 1).length ?? 0} 个</span>
            </div>
          </div>

          <div
            style={{
              marginTop: 18,
              padding: 12,
              borderRadius: 8,
              background: 'var(--bg-subtle)',
              border: '1px dashed var(--border-1)',
              fontSize: 12,
              color: 'var(--text-3)',
              lineHeight: 1.7,
            }}
          >
            <ShieldCheck size={15} style={{ marginRight: 6, color: 'var(--color-success)' }} />
            账号安全策略：连续 5 次密码错误锁定 15 分钟；改密后所有在线会话立即失效。
          </div>
        </aside>

        <section className="ferry-panel">
          <Tabs
            activeKey={tab}
            onChange={(key) => {
              setTab(key)
              setParams(key === 'security' ? { tab: 'security' } : {}, { replace: true })
            }}
            style={{ padding: '4px 20px 20px' }}
            items={[
              {
                key: 'basic',
                label: '基本资料',
                children: (
                  <div style={{ maxWidth: 480 }}>
                    <Form
                      form={profileForm}
                      layout="vertical"
                      requiredMark="optional"
                      initialValues={{
                        nickname: user?.nickname,
                        email: user?.email ?? undefined,
                        phone: user?.phone ?? undefined,
                      }}
                      onFinish={(values) => profileMutation.mutate(values)}
                    >
                      <Form.Item label="账号">
                        <Input value={user?.account} prefix={<User size={15} />} disabled />
                      </Form.Item>
                      <Form.Item name="nickname" label="昵称" rules={[{ required: true, message: '请输入昵称' }]}>
                        <Input placeholder="真实姓名" maxLength={20} />
                      </Form.Item>
                      <Form.Item name="phone" label="手机号" rules={[{ pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确' }]}>
                        <Input prefix={<Phone size={15} />} placeholder="用于接收发布与告警通知" maxLength={11} />
                      </Form.Item>
                      <Form.Item name="email" label="邮箱" rules={[{ type: 'email', message: '邮箱格式不正确' }]}>
                        <Input prefix={<Mail size={15} />} placeholder="用于接收通知与导出链接" maxLength={60} />
                      </Form.Item>
                      <Button type="primary" htmlType="submit" loading={profileMutation.isPending}>
                        保存修改
                      </Button>
                    </Form>
                  </div>
                ),
              },
              {
                key: 'security',
                label: '安全设置',
                children: (
                  <div style={{ maxWidth: 480 }}>
                    <Alert
                      type="warning"
                      showIcon
                      style={{ marginBottom: 16 }}
                      message="改密后需要重新登录"
                      description="密码更新会撤销本账号在所有设备上的会话。若怀疑账号泄露，请立即修改并联系平台管理员排查审计日志。"
                    />
                    <Form
                      form={passwordForm}
                      layout="vertical"
                      requiredMark="optional"
                      onFinish={(values: { oldPassword: string; newPassword: string; confirm: string }) =>
                        passwordMutation.mutate({
                          oldPassword: values.oldPassword,
                          newPassword: values.newPassword,
                        })
                      }
                    >
                      <Form.Item
                        name="oldPassword"
                        label="当前密码"
                        rules={[{ required: true, message: '请输入当前密码' }]}
                      >
                        <Input.Password prefix={<Lock size={15} />} placeholder="请输入当前密码" autoComplete="current-password" />
                      </Form.Item>
                      <Form.Item
                        name="newPassword"
                        label="新密码"
                        rules={[
                          { required: true, message: '请输入新密码' },
                          { min: 8, message: '至少 8 位' },
                          {
                            pattern: /^(?=.*[A-Za-z])(?=.*\d).+$/,
                            message: '需同时包含字母与数字',
                          },
                        ]}
                        extra="至少 8 位，需同时包含字母与数字；避免使用姓名、工号与常见弱口令"
                      >
                        <Input.Password prefix={<Lock size={15} />} placeholder="设置新密码" autoComplete="new-password" />
                      </Form.Item>
                      <Form.Item
                        name="confirm"
                        label="确认新密码"
                        dependencies={['newPassword']}
                        rules={[
                          { required: true, message: '请再次输入新密码' },
                          ({ getFieldValue }) => ({
                            validator(_, value) {
                              if (!value || getFieldValue('newPassword') === value) return Promise.resolve()
                              return Promise.reject(new Error('两次输入的密码不一致'))
                            },
                          }),
                        ]}
                      >
                        <Input.Password prefix={<CheckCircle2 size={16} />} placeholder="再次输入新密码" autoComplete="new-password" />
                      </Form.Item>
                      <Button type="primary" htmlType="submit" loading={passwordMutation.isPending}>
                        更新密码
                      </Button>
                    </Form>
                  </div>
                ),
              },
            ]}
          />
        </section>
      </div>
    </>
  )
}
