import { get, post } from './client'
import type {
  ApiPage,
  AuditRow,
  ConfigRow,
  DeptNode,
  DictNode,
  MenuNode,
  PostRow,
  RegionNode,
  RoleRow,
  TenantRow,
  UserRow,
  WorkbenchOverview,
} from './types'

export interface PageParams {
  current?: number
  size?: number
  [key: string]: unknown
}

export const userApi = {
  pageList: (body: PageParams) => post<ApiPage<UserRow>>('/ferry-system/user/pageList', body),
  detail: (id: string) => get<UserRow>('/ferry-system/user/detail', { id }),
  save: (body: Record<string, unknown>) => post<string>('/ferry-system/user/save', body),
  resetPassword: (id: string) => post<string>(`/ferry-system/user/resetPassword/${id}`),
  deleted: (ids: string[]) => post<string>('/ferry-system/user/deleted', ids),
  authorizeRole: (userId: string, roleId: string) =>
    post<string>('/ferry-system/user/authorizeRole', { authorizeIds: [userId], authorizedIds: [roleId] }),
}

export const roleApi = {
  pageList: (body: PageParams) => post<ApiPage<RoleRow>>('/ferry-system/role/pageList', body),
  save: (body: Record<string, unknown>) => post<string>('/ferry-system/role/save', body),
  deleted: (ids: string[]) => post<string>('/ferry-system/role/deleted', ids),
  /** 已授权菜单：后端返回扁平列表而非树，且接口用 @RequestParam 接收，必须走查询参数 */
  menuList: (id: string) => get<Array<{ id: string; name?: string; type?: number }>>('/ferry-system/role/menuList', { id }),
  /** 角色授权菜单：authorizedIds 是被授权对象（角色），authorizeIds 是授予的内容（菜单），两者不能写反 */
  authorizeMenu: (roleId: string, menuIds: string[]) =>
    post<string>('/ferry-system/role/authorizeMenu', { authorizeIds: menuIds, authorizedIds: [roleId] }),
  /** 已授权数据范围：数据范围取值 + 授权时勾选的原始部门（不含下级） */
  dataScope: (id: string) =>
    get<{ dataScope: number; deptIds: string[] }>('/ferry-system/role/dataScope', { id }),
  /** 角色授权数据范围：与菜单授权分开提交避免互相覆盖，保存只改库、不注销在线会话 */
  authorizeDataScope: (roleId: string, dataScope: number, deptIds: string[]) =>
    post<string>(`/ferry-system/role/authorizeDataScope/${roleId}/${dataScope}`, deptIds),
}

export const menuApi = {
  treeList: (body: Record<string, unknown> = {}) => post<MenuNode[]>('/ferry-system/menu/treeList', body),
  save: (body: Record<string, unknown>) => post<string>('/ferry-system/menu/save', body),
  deleted: (ids: string[]) => post<string>('/ferry-system/menu/deleted', ids),
}

export const deptApi = {
  pageList: (body: PageParams) => post<ApiPage<DeptNode>>('/ferry-system/dept/pageList', body),
  treeList: (body: Record<string, unknown> = {}) => post<DeptNode[]>('/ferry-system/dept/treeList', body),
  detail: (id: string) => get<DeptNode>('/ferry-system/dept/detail', { id }),
  save: (body: Record<string, unknown>) => post<string>('/ferry-system/dept/save', body),
  deleted: (ids: string[]) => post<string>('/ferry-system/dept/deleted', ids),
}

export const postApi = {
  pageList: (body: PageParams) => post<ApiPage<PostRow>>('/ferry-system/post/pageList', body),
  save: (body: Record<string, unknown>) => post<string>('/ferry-system/post/save', body),
  deleted: (ids: string[]) => post<string>('/ferry-system/post/deleted', ids),
}

export const dictApi = {
  treeList: (body: Record<string, unknown> = {}) => post<DictNode[]>('/ferry-system/dict/treeList', body),
  pageList: (body: PageParams) => post<ApiPage<DictNode>>('/ferry-system/dict/pageList', body),
  detail: (id: string) => get<DictNode>('/ferry-system/dict/detail', { id }),
  save: (body: Record<string, unknown>) => post<string>('/ferry-system/dict/save', body),
  deleted: (ids: string[]) => post<string>('/ferry-system/dict/deleted', ids),
}

export const configApi = {
  pageList: (body: PageParams) => post<ApiPage<ConfigRow>>('/ferry-system/config/pageList', body),
  save: (body: Record<string, unknown>) => post<string>('/ferry-system/config/save', body),
  deleted: (ids: string[]) => post<string>('/ferry-system/config/deleted', ids),
}

export const tenantApi = {
  /** 登录页依赖此接口渲染租户下拉，失败时由页面内联提示，不走全局 toast */
  list: (options?: { silent?: boolean }) => post<TenantRow[]>('/ferry-system/tenant/list', {}, options),
  pageList: (body: PageParams) => post<ApiPage<TenantRow>>('/ferry-system/tenant/pageList', body),
  save: (body: Record<string, unknown>) => post<string>('/ferry-system/tenant/save', body),
  deleted: (ids: string[]) => post<string>('/ferry-system/tenant/deleted', ids),
}

/** 区域列表查询条件：传 parentId 逐级懒加载、传 keyword 跨层级检索，两种用法互斥 */
export interface RegionFilter {
  parentId?: string
  keyword?: string
  status?: number
}

export const regionApi = {
  /** 区域懒加载列表：首屏只拉根节点，展开哪一级才拉哪一级，避免全量建树 */
  lazyList: (body: RegionFilter) => post<RegionNode[]>('/ferry-system/region/lazyList', body),
  detail: (id: string) => get<RegionNode>('/ferry-system/region/detail', { id }),
  save: (body: Record<string, unknown>) => post<string>('/ferry-system/region/save', body),
  deleted: (ids: string[]) => post<string>('/ferry-system/region/deleted', ids),
}

export const auditApi = {
  pageList: (body: PageParams) => post<ApiPage<AuditRow>>('/ferry-system/audit/pageList', body),
  detail: (id: string) => get<AuditRow>('/ferry-system/audit/detail', { id }),
  exportLog: (rows: number, filters: string) => post<string>('/ferry-system/audit/export-log', { rows, filters }),
}

export const workbenchApi = {
  overview: () => get<WorkbenchOverview>('/ferry-system/workbench/overview'),
}

export const profileApi = {
  update: (body: { nickname: string; email?: string; phone?: string }) =>
    post<string>('/ferry-system/profile/update', body),
  changePassword: (body: { oldPassword: string; newPassword: string }) =>
    post<string>('/ferry-system/profile/password', body),
}
