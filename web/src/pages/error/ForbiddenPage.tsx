import { Button, Result } from 'antd'
import { useNavigate } from 'react-router-dom'
import { useAuthStore } from '@/stores/auth'

export default function ForbiddenPage() {
  const navigate = useNavigate()
  const user = useAuthStore((state) => state.user)

  return (
    <Result
      status="403"
      title="无访问权限"
      subTitle={`当前账号「${user?.nickname ?? '未知'}」未被授予该功能权限。权限变更即时生效，请联系平台管理员在「角色管理」中授权。`}
      extra={
        <Button type="primary" onClick={() => navigate('/workbench')}>
          返回工作台
        </Button>
      }
    />
  )
}
