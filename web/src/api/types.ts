/** 接口契约类型：与后端 VO / DTO 对应，后端已把 Long 序列化为字符串，所有 id 一律按 string 处理 */

export type ID = string

export interface ApiEnvelope<T> {
  code: number
  data: T
  msg: string
}

export interface ApiPage<T> {
  records: T[]
  total: number
  size: number
  current: number
  pages: number
}

export interface PageQuery {
  current?: number
  size?: number
}

/* ------------------------------ 认证 ------------------------------ */

export interface AuthPerm {
  id: ID
  parentId: ID
  name: string
  /** 0 目录 / 1 菜单 / 2 按钮 */
  type: number
  icon?: string | null
  path?: string | null
  component?: string | null
  perms?: string | null
  isShow?: number | null
  isFrame?: number | null
  isCache?: number | null
  sort?: number | null
}

export interface AuthInfo {
  userId: ID
  tenantId: string
  account: string
  nickname: string
  avatar?: string | null
  email?: string | null
  phone?: string | null
  isSuper: number
  tokenHeader: string
  tokenPrefix: string
  accessToken: string
  permissions: AuthPerm[]
}

export interface LoginPayload {
  grantType: 'password' | 'captcha'
  tenantId?: string
  account: string
  password: string
}

/* ------------------------------ 组织与权限 ------------------------------ */

export interface MenuNode {
  id: ID
  parentId: ID
  name: string
  type: number
  icon?: string | null
  path?: string | null
  component?: string | null
  perms?: string | null
  isShow?: number | null
  isFrame?: number | null
  isCache?: number | null
  status?: number | null
  sort?: number | null
  createTime?: string | null
  children?: MenuNode[]
}

export interface RoleRow {
  id: ID
  tenantId: string
  code: string
  name: string
  status: number
  sort: number
  createTime: string
  remark?: string
  /** 预置角色不可删除，作为审计与应急恢复的兜底 */
  builtin?: boolean
  /** 菜单授权不在列表里下发，按可选处理，渲染层需区分「未接入」与「真的为 0」 */
  menuIds?: ID[]
  /** 数据范围 1：全部 2：自定义 3：本部门 4：本部门及以下 5：仅本人 */
  dataScope?: number
  memberCount?: number
}

export interface DeptNode {
  id: ID
  parentId: ID
  ancestors?: string | null
  code: string
  name: string
  status: number
  sort: number
  createTime?: string | null
  children?: DeptNode[]
}

export interface UserRow {
  id: ID
  tenantId: string
  account: string
  nickname: string
  avatar?: string | null
  email?: string | null
  phone?: string | null
  birthday?: string | null
  gender?: number | null
  address?: string | null
  isSuper: number
  status: number
  deptId?: string | null
  postId?: string | null
  roleId?: string | null
  createTime?: string | null
  lastLoginTime?: string | null
}

export interface PostRow {
  id: ID
  code: string
  name: string
  status: number
  sort: number
  createTime?: string | null
}

export interface DictNode {
  id: ID
  parentId: ID
  code: string
  label: string
  value?: string | null
  sort: number
  createTime?: string | null
  children?: DictNode[]
}

export interface ConfigRow {
  id: ID
  name: string
  code: string
  value: string
  isInternal: number
  createTime?: string | null
  updateTime?: string | null
}

export interface TenantRow {
  id: ID
  code: string
  name: string
  status: number
  createUser?: string | null
  createTime?: string | null
}

export interface RegionNode {
  id: ID
  parentId: ID
  /** 层级路径码：省 6 位 / 市 6 位 / 区县 6 位 / 街道 12 位，前缀即父级编码 */
  code: string
  name: string
  status: number
  sort: number
  /** 是否叶子节点（无下级区划），懒加载接口返回；为 false 时表格才渲染展开图标 */
  isLeaf?: boolean
  createTime?: string
  /** 仅前端使用：已懒加载到的子级，undefined 表示尚未加载 */
  children?: RegionNode[]
}

/* ------------------------------ 审计 ------------------------------ */

export interface AuditRow {
  id: ID
  operateTime: string
  operator: string
  operatorAccount: string
  /** 新增 / 修改 / 删除 / 授权 / 导出 / 登录 */
  operateType: string
  objectType: string
  objectId: string
  objectName: string
  action: string
  beforeValue?: string | null
  afterValue?: string | null
  ip: string
  ticketNo?: string | null
  result: '成功' | '失败'
  remark?: string
}

/* ------------------------------ 应用与环境（数据权限矩阵用） ------------------------------ */

export interface AppRow {
  id: ID
  code: string
  name: string
  team: string
  owner: string
  envIds: ID[]
  core: boolean
}

export interface EnvRow {
  id: ID
  name: string
  type: 'test' | 'staging' | 'prod'
  short: string
}

/* ------------------------------ 工作台 ------------------------------ */

export interface WorkbenchStat {
  key: string
  label: string
  value: number
  unit?: string
  delta?: number
  hint?: string
  tone?: 'primary' | 'success' | 'warning' | 'danger' | 'default'
}

export interface WorkbenchActivity {
  id: ID
  time: string
  operator: string
  text: string
  type: 'create' | 'update' | 'delete' | 'auth' | 'export' | 'login'
  target: string
}

export interface WorkbenchOverview {
  greeting: string
  stats: WorkbenchStat[]
  activities: WorkbenchActivity[]
  shortcuts: Array<{ key: string; label: string; desc: string; path: string; perm?: string }>
  system: {
    version: string
    env: string
    accounts: number
    roles: number
    menus: number
    auditRetention: string
    lastBackup: string
  }
}

export interface AppRow {
  id: ID
  code: string
  name: string
  deptId: ID
  /** 逗号分隔的负责人id，前端按逗号拆成数组使用 */
  ownerIds: string
  /** 逗号分隔的负责人名称，与 ownerIds 顺序一致 */
  ownerNames: string
  description?: string | null
  isCore: number
  status: number
  repoType?: string | null
  repoUrl?: string | null
  repoBranch?: string | null
  createTime: string
}

/** 应用下拉选项，流水线页与新建抽屉共用 */
export interface AppOption {
  id: ID
  code: string
  name: string
  status: number
}

/* ------------------------------ 流水线 ------------------------------ */

export type PipelineStageType =
  | 'BUILD'
  | 'UNIT_TEST'
  | 'STATIC_SCAN'
  | 'ARTIFACT_PUSH'
  | 'DEPLOY_NON_PROD'
  | 'INTEGRATION_TEST'
  | 'ADMISSION'
  | 'PROD_RELEASE'

export interface PipelineTask {
  name: string
  command?: string | null
  timeoutMinutes: number
}

export interface PipelineStage {
  type: PipelineStageType
  name: string
  timeoutMinutes: number
  continueOnFailure: boolean
  artifacts: string[]
  tasks: PipelineTask[]
}

export interface PipelineDefinition {
  stages: PipelineStage[]
}

export interface PipelineRow {
  id: ID
  appId: ID
  appCode: string
  appName: string
  name: string
  description?: string | null
  currentVersionId?: ID | null
  currentVersionNo?: number | null
  stageCount: number
  defaultBranch?: string | null
  ownerNames?: string | null
  sourceTemplateId?: ID | null
  sourceTemplateName?: string | null
  sourceTemplateVersionNo?: number | null
  sourceTemplateOutdated?: boolean | null
  createTime: string
  updateTime: string
}

export interface PipelineDetail extends PipelineRow {
  definition?: PipelineDefinition | null
  sourceTemplateVars?: Record<string, string> | null
  sourceTemplateMergedTime?: string | null
}

export interface PipelineVersionRow {
  id: ID
  pipelineId: ID
  versionNo: number
  changeNote: string
  createUserName?: string | null
  createTime: string
}

export interface PipelineVersionDetail extends PipelineVersionRow {
  definition: PipelineDefinition
}

/* ------------------------------ 流水线模板 ------------------------------ */

/** 模板占位变量，模板定义中以 {{key}} 引用，派生时替换为实际值 */
export interface TemplateVariable {
  key: string
  label: string
  description?: string | null
  defaultValue?: string | null
  required: boolean
}

/** 模板定义 = 流水线阶段 + 占位变量表 */
export interface TemplateDefinition {
  stages: PipelineStage[]
  variables: TemplateVariable[]
}

export interface PipelineTemplateRow {
  id: ID
  /** 0 组织级 / 1 团队级 */
  scope: number
  deptId?: ID | null
  name: string
  description?: string | null
  /** 0 停用 / 1 启用 */
  status: number
  currentVersionId?: ID | null
  currentVersionNo?: number | null
  stageCount: number
  variableCount: number
  derivedCount: number
  createUserName?: string | null
  createTime: string
  updateTime: string
}

export interface PipelineTemplateDetail extends PipelineTemplateRow {
  definition?: TemplateDefinition | null
}

export interface TemplateVersionRow {
  id: ID
  templateId: ID
  versionNo: number
  changeNote: string
  createUserName?: string | null
  createTime: string
}

export interface TemplateVersionDetail extends TemplateVersionRow {
  definition: TemplateDefinition
}

/* ------------------------------ 运行台账 ------------------------------ */

/** 提交摘要，来自代码平台实时查询 */
export interface CommitBrief {
  sha: string
  author: string
  /** ISO-8601 文本 */
  committedTime?: string | null
  message: string
}

/** 触发预校验单项结论 */
export interface PrecheckItem {
  /** REPO / BRANCH / COMMIT / DEFINITION / IDEMPOTENT */
  item: string
  itemName: string
  /** PASS 通过 / FAIL 不通过 / NA 不适用 */
  conclusion: 'PASS' | 'FAIL' | 'NA'
  detail: string
}

export interface RunRow {
  id: ID
  appId: ID
  appName: string
  appCode: string
  pipelineId: ID
  pipelineName: string
  pipelineVersionNo: number
  branch: string
  /** 0 提交 / 1 标签或引用 */
  refType: number
  commitId: string
  commitAuthor?: string | null
  /** QUEUED / RUNNING / BLOCKED / SUCCESS / FAILED / TIMEOUT / CANCELED */
  status: string
  reusedRunId?: ID | null
  triggerUserName?: string | null
  triggerNote?: string | null
  createTime: string
  startedTime?: string | null
  finishedTime?: string | null
}

/** 运行阶段的任务明细，来自运行绑定的版本快照 */
export interface RunStageTask {
  name: string
  command?: string | null
  timeoutMinutes: number
}

export interface RunStageRow {
  id: ID
  stageType: string
  stageName?: string | null
  stageOrder: number
  /** PENDING / RUNNING / SUCCESS / FAILED / SKIPPED / BLOCKED */
  status: string
  timeoutMinutes?: number | null
  startedTime?: string | null
  finishedTime?: string | null
  tasks: RunStageTask[]
}

export interface RunDetail extends RunRow {
  prechecks: PrecheckItem[]
  stages: RunStageRow[]
}

export interface TriggerRecordRow {
  id: ID
  appId: ID
  appName: string
  branch?: string | null
  commitId?: string | null
  /** CREATED / MERGED / REUSED / REJECTED */
  result: string
  rejectReason?: string | null
  runId?: ID | null
  triggerUserName?: string | null
  createTime: string
}

export interface TriggerResult {
  /** CREATED / MERGED / REUSED / REJECTED / PENDING_REUSE */
  result: string
  resultName: string
  runId?: ID | null
  reuseCandidateRunId?: ID | null
  /** 拒绝原因，仅已拒绝时返回 */
  rejectReason?: string | null
}
