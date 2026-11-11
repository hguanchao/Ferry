import { get, post, ApiError } from './client'
import type {
  ApiPage,
  AppRow,
  AuditRow,
  ConfigRow,
  DeptNode,
  DictNode,
  EnvRow,
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
  deleted: (ids: string[]) => post<string>('/ferry-system/user/deleted', { ids }),
  authorizeRole: (userId: string, roleId: string) =>
    post<string>('/ferry-system/user/authorizeRole', { authorizeIds: [userId], authorizedIds: [roleId] }),
}

export const roleApi = {
  pageList: (body: PageParams) => post<ApiPage<RoleRow>>('/ferry-system/role/pageList', body),
  save: (body: Record<string, unknown>) => post<string>('/ferry-system/role/save', body),
  deleted: (ids: string[]) => post<string>('/ferry-system/role/deleted', { ids }),
  /**
   * 该角色已授权的菜单：后端返回扁平 List<MenuTree>（BeanUtil.copy 自 sys_role_menu 关联结果），
   * 不是树。前端取 id 作为 Tree 的选中键。
   */
  /** 已授权菜单：后端是 @RequestParam 接收，必须走查询参数，拼成路径会 404 */
  menuList: (id: string) => get<Array<{ id: string; name?: string; type?: number }>>('/ferry-system/role/menuList', { id }),
  /**
   * 角色授权菜单。
   *
   * 后端约定 authorizedIds 是「被授权对象」（角色），authorizeIds 是「授予的内容」（菜单），
   * 与用户授权角色（authorizedIds=用户、authorizeIds=角色）保持同一语义。
   * 两者写反会让 sys_role_menu 存成 (菜单id, 角色id)，查询按角色id反查永远为空。
   */
  authorizeMenu: (roleId: string, menuIds: string[]) =>
    post<string>('/ferry-system/role/authorizeMenu', { authorizeIds: menuIds, authorizedIds: [roleId] }),
  /** 已授权数据范围：数据范围取值 + 授权时勾选的原始部门（不含下级） */
  dataScope: (id: string) =>
    get<{ dataScope: number; deptIds: string[] }>('/ferry-system/role/dataScope', { id }),
  /**
   * 角色授权数据范围。
   *
   * 与菜单授权分开提交：数据范围带自定义部门，和菜单树塞进同一个接口会让两种授权互相覆盖。
   * 后端把角色id与范围取值放在路径上、部门放在请求体里，取消自定义时传空数组即可清空。
   * 保存只改库，不注销在线会话：已登录用户按旧范围继续用，重新登录后才按新范围过滤。
   */
  authorizeDataScope: (roleId: string, dataScope: number, deptIds: string[]) =>
    post<string>(`/ferry-system/role/authorizeDataScope/${roleId}/${dataScope}`, deptIds),
}

export const menuApi = {
  treeList: (body: Record<string, unknown> = {}) => post<MenuNode[]>('/ferry-system/menu/treeList', body),
  save: (body: Record<string, unknown>) => post<string>('/ferry-system/menu/save', body),
  deleted: (ids: string[]) => post<string>('/ferry-system/menu/deleted', { ids }),
}

export const deptApi = {
  pageList: (body: PageParams) => post<ApiPage<DeptNode>>('/ferry-system/dept/pageList', body),
  treeList: (body: Record<string, unknown> = {}) => post<DeptNode[]>('/ferry-system/dept/treeList', body),
  detail: (id: string) => get<DeptNode>('/ferry-system/dept/detail', { id }),
  save: (body: Record<string, unknown>) => post<string>('/ferry-system/dept/save', body),
  deleted: (ids: string[]) => post<string>('/ferry-system/dept/deleted', { ids }),
}

export const postApi = {
  pageList: (body: PageParams) => post<ApiPage<PostRow>>('/ferry-system/post/pageList', body),
  save: (body: Record<string, unknown>) => post<string>('/ferry-system/post/save', body),
  deleted: (ids: string[]) => post<string>('/ferry-system/post/deleted', { ids }),
}

export const dictApi = {
  treeList: (body: Record<string, unknown> = {}) => post<DictNode[]>('/ferry-system/dict/treeList', body),
  pageList: (body: PageParams) => post<ApiPage<DictNode>>('/ferry-system/dict/pageList', body),
  detail: (id: string) => get<DictNode>('/ferry-system/dict/detail', { id }),
  save: (body: Record<string, unknown>) => post<string>('/ferry-system/dict/save', body),
  deleted: (ids: string[]) => post<string>('/ferry-system/dict/deleted', { ids }),
}

export const configApi = {
  pageList: (body: PageParams) => post<ApiPage<ConfigRow>>('/ferry-system/config/pageList', body),
  save: (body: Record<string, unknown>) => post<string>('/ferry-system/config/save', body),
  deleted: (ids: string[]) => post<string>('/ferry-system/config/deleted', { ids }),
}

export const tenantApi = {
  /** 登录页依赖此接口渲染租户下拉，失败时由页面内联提示，不走全局 toast */
  list: (options?: { silent?: boolean }) => post<TenantRow[]>('/ferry-system/tenant/list', {}, options),
  pageList: (body: PageParams) => post<ApiPage<TenantRow>>('/ferry-system/tenant/pageList', body),
  save: (body: Record<string, unknown>) => post<string>('/ferry-system/tenant/save', body),
  deleted: (ids: string[]) => post<string>('/ferry-system/tenant/deleted', { ids }),
}

/**
 * 区域列表查询条件
 *
 * 两种用法互斥：
 * - 传 parentId：逐级懒加载，取该父级下的直接子节点；
 * - 传 keyword：跨层级检索，取全部命中名称或编码的区划（编码是层级路径码，前缀即子树）。
 */
export interface RegionFilter {
  parentId?: string
  keyword?: string
  status?: number
}

export const regionApi = {
  /**
   * 懒加载列表。
   *
   * 这是区域管理页面唯一的数据来源：首屏只拉根节点，展开哪一级才拉哪一级，
   * 不再提供整棵树的一次性拉取，避免 4.5 万行行政区划在打开页面时被全量建树。
   */
  lazyList: (body: RegionFilter) => post<RegionNode[]>('/ferry-system/region/lazyList', body),
  detail: (id: string) => get<RegionNode>('/ferry-system/region/detail', { id }),
  save: (body: Record<string, unknown>) => post<string>('/ferry-system/region/save', body),
  deleted: (ids: string[]) => post<string>('/ferry-system/region/deleted', { ids }),
}

export const auditApi = {
  pageList: (body: PageParams) => post<ApiPage<AuditRow>>('/ferry-system/audit/pageList', body),
  detail: (id: string) => get<AuditRow>('/ferry-system/audit/detail', { id }),
  exportLog: (rows: number, filters: string) => post<string>('/ferry-system/audit/export-log', { rows, filters }),
}

export const workbenchApi = {
  overview: () => get<WorkbenchOverview>('/ferry-system/workbench/overview'),
}

/** 数据权限的授权对象：应用 × 环境（由应用管理提供，未接入时降级为空） */
export const appScopeApi = {
  /**
   * 应用管理尚未落地，后端没有 /apps/scope-options；404 视为「暂未接入」返回空清单，
   * 由页面渲染待接入空态。可选接口不该把整页打挂，也不该弹一条 404 错误。
   */
  options: async (): Promise<{ apps: AppRow[]; envs: EnvRow[] }> => {
    try {
      return await get<{ apps: AppRow[]; envs: EnvRow[] }>('/ferry-system/apps/scope-options', undefined, { silent: true })
    } catch (error) {
      if (error instanceof ApiError && error.code === 404) {
        return { apps: [], envs: [] }
      }
      throw error
    }
  },
}

export const profileApi = {
  update: (body: { nickname: string; email?: string; phone?: string }) =>
    post<string>('/ferry-system/profile/update', body),
  changePassword: (body: { oldPassword: string; newPassword: string }) =>
    post<string>('/ferry-system/profile/password', body),
}
