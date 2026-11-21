/** 交付域接口：仅放请求函数，类型定义在 types-devops.ts */
import { get, post } from './client'
import type { ApiPage } from './types'
import type {
  ActionCatalogRow, ActionRecordRow, AiFeedbackRow, AiOutputRow, AppAiLevelRow,
  AppOption, AppOverview, AppDependencyRow, AppRow, ArtifactRow, BaselineRow,
  ChangeTicketRow, CleanPolicyRow, CleanTaskRow, ClosureRow, DeployBatchRow,
  DeployRecordRow, DiffResult, EnvironmentRow, EnvConfigRow, GateExemptionRow,
  GateResultRow, GateRuleRow, ImpactResult, MetricRow, NotifyChannelRow, NotifyTemplateRow,
  OpenTokenRow, PipelineDefinition, PipelineDetail, PipelinePreview, PipelineRow, PipelineVersionRow, PrecheckResult,
  PrecheckResultVO, ReleaseDetail, ReleaseRecordRow, ReleaseRow, ReleaseStrategyRow,
  RepoCommitOption, RiskResult, RollbackCandidate, RollbackRow, RunDetail, RunRow, StageStatRow, StoragePolicyRow,
  TechDebtRow, TriggerRecordRow, TriggerResult, ValidateResult,
  WizardProgress,
} from './types-devops'

const B = '/ferry-devops'

export const appApi = {
  pageList: (body: Record<string, unknown>) => post<ApiPage<AppRow>>(`${B}/app/pageList`, body),
  save: (body: Record<string, unknown>) => post<string>(`${B}/app/save`, body),
  deleted: (ids: string[]) => post<string>(`${B}/app/deleted`, ids),
  // 复用分页接口取全量应用（大页容量）映射为选项；keyword 走应用列表的 code/name 模糊条件
  options: (keyword?: string) =>
    post<ApiPage<AppRow>>(`${B}/app/pageList`, { keyword, current: 1, size: 200 }).then((page) =>
      (page.records ?? []).map((r) => ({
        value: r.id,
        label: r.name,
        code: r.code,
        repoUrl: r.repoUrl,
        repoBranch: r.repoBranch,
        repoCommitId: r.repoCommitId,
      }))),
  // TODO(dev_app-schema): dev_app_repo 已删除，repoList/saveRepo/deletedRepo 接口已移除。
  dependencyList: (appId: string) => get<AppDependencyRow[]>(`${B}/app/dependency/list`, { appId }),
  saveDependency: (body: Record<string, unknown>) => post<string>(`${B}/app/dependency/save`, body),
  deletedDependency: (ids: string[]) => post<string>(`${B}/app/dependency/deleted`, ids),
  applyCoreFlag: (body: Record<string, unknown>) => post<string>(`${B}/app/coreFlag/apply`, body),
  approveCoreFlag: (body: Record<string, unknown>) => post<string>(`${B}/app/coreFlag/approve`, body),
  saveWizardStep: (body: Record<string, unknown>) => post<string>(`${B}/app/wizard/saveStep`, body),
  wizardProgress: (id: string) => get<WizardProgress>(`${B}/app/wizard/progress`, { id }),
  overview: (id: string) => get<AppOverview>(`${B}/app/aggregate/overview`, { id }),
}

export const envApi = {
  pageList: (body: Record<string, unknown>) => post<ApiPage<EnvironmentRow>>(`${B}/environment/pageList`, body),
  detail: (id: string) => get<EnvironmentRow>(`${B}/environment/detail`, { id }),
  save: (body: Record<string, unknown>) => post<string>(`${B}/environment/save`, body),
  deleted: (ids: string[]) => post<string>(`${B}/environment/deleted`, ids),
  options: (appId?: string) => get<AppOption[]>(`${B}/environment/options`, { appId }),
  changeTypeApply: (body: Record<string, unknown>) => post<string>(`${B}/environment/changeType/apply`, body),
  changeTypeApprove: (body: Record<string, unknown>) => post<string>(`${B}/environment/changeType/approve`, body),
}
export const envConfigApi = {
  list: (envId: string) => get<EnvConfigRow[]>(`${B}/envConfig/list`, { envId }),
  save: (body: Record<string, unknown>) => post<string>(`${B}/envConfig/save`, body),
  diff: (envId: string) => get<Array<Record<string, unknown>>>(`${B}/envConfig/diff`, { envId }),
}
export const pipelineApi = {
  pageList: (body: Record<string, unknown>) => post<ApiPage<PipelineRow>>(`${B}/pipeline/pageList`, body),
  detail: (id: string) => get<PipelineDetail>(`${B}/pipeline/detail`, { id }),
  save: (body: Record<string, unknown>) => post<string>(`${B}/pipeline/save`, body),
  deleted: (ids: string[]) => post<string>(`${B}/pipeline/deleted`, ids),
  enable: (body: Record<string, unknown>) => post<string>(`${B}/pipeline/enable`, body),
  disable: (body: Record<string, unknown>) => post<string>(`${B}/pipeline/disable`, body),
  definition: (id: string, versionId?: string) => get<PipelineDefinition>(`${B}/pipeline/definition`, { id, versionId }),
  validateDefinition: (body: Record<string, unknown>) => post<ValidateResult>(`${B}/pipeline/definition/validate`, body),
  previewDefinition: (body: Record<string, unknown>) => post<PipelinePreview>(`${B}/pipeline/definition/preview`, body),
  saveDefinition: (body: Record<string, unknown>) => post<PipelineVersionRow>(`${B}/pipeline/definition/save`, body),
  /** Jenkins Job 同步重试：保存/启停同步失败后调用 */
  syncRetry: (id: string) => post<string>(`${B}/pipeline/sync`, { id }),
  versionPage: (pipelineId: string, current = 1, size = 20) => get<ApiPage<PipelineVersionRow>>(`${B}/pipeline/version/pageList`, { pipelineId, current, size }),
  versionDiff: (leftId: string, rightId: string) => get<DiffResult>(`${B}/pipeline/version/diff`, { leftId, rightId }),
  versionRollback: (body: Record<string, unknown>) => post<PipelineVersionRow>(`${B}/pipeline/version/rollback`, body),
  triggerPrecheck: (body: Record<string, unknown>) => post<PrecheckResult>(`${B}/pipeline/trigger/precheck`, body),
  triggerBranches: (appId: string) => get<string[]>(`${B}/pipeline/trigger/branches`, { appId }, { silent: true }),
  triggerCommits: (appId: string, branch: string, limit = 10) =>
    get<RepoCommitOption[]>(`${B}/pipeline/trigger/commits`, { appId, branch, limit }, { silent: true }),
  trigger: (body: Record<string, unknown>, requestId: string) => post<TriggerResult>(`${B}/pipeline/trigger`, { ...body, requestId }),
  triggerRecords: (appId: string, current = 1, size = 20) => get<ApiPage<TriggerRecordRow>>(`${B}/pipeline/trigger/records`, { appId, current, size }),
}


export const runApi = {
  pageList: (body: Record<string, unknown>) => post<ApiPage<RunRow>>(`${B}/run/pageList`, body),
  detail: (id: string) => get<RunDetail>(`${B}/run/detail`, { id }),
  stages: (id: string) => get<Array<Record<string, unknown>>>(`${B}/run/stage/list`, { id }),
  tasks: (stageId: string) => get<Array<Record<string, unknown>>>(`${B}/run/task/list`, { stageId }),
  log: (taskId: string) => get<Array<Record<string, unknown>>>(`${B}/run/log`, { taskId }),
  logSearch: (body: Record<string, unknown>) => post<Array<Record<string, unknown>>>(`${B}/run/log/search`, body),
  pause: (body: Record<string, unknown>) => post<string>(`${B}/run/pause`, body),
  resume: (id: string) => post<string>(`${B}/run/resume`, { id }),
  abort: (body: Record<string, unknown>) => post<string>(`${B}/run/abort`, body),
  rerunPlan: (id: string) => get<Array<Record<string, unknown>>>(`${B}/run/rerunPlan`, { id }),
  rerun: (body: Record<string, unknown>) => post<{ id: string }>(`${B}/run/rerun`, body),
}

export const artifactApi = {
  pageList: (body: Record<string, unknown>) => post<ApiPage<ArtifactRow>>(`${B}/artifact/pageList`, body),
  detail: (id: string) => get<ArtifactRow>(`${B}/artifact/detail`, { id }),
  trace: (id: string) => get<Record<string, unknown>>(`${B}/artifact/trace`, { id }),
  search: (keyword: string) => get<ArtifactRow[]>(`${B}/artifact/search`, { keyword }),
  promotePrecheck: (body: Record<string, unknown>) => post<PrecheckResult>(`${B}/artifact/promote/precheck`, body),
  promote: (body: Record<string, unknown>) => post<string>(`${B}/artifact/promote`, body),
  restore: (id: string) => post<string>(`${B}/artifact/restore/${id}`),
  policy: (appId: string) => get<CleanPolicyRow>(`${B}/artifactClean/policy`, { appId }),
  savePolicy: (body: Record<string, unknown>) => post<string>(`${B}/artifactClean/policy/save`, body),
  cleanRuns: (appId: string) => get<CleanTaskRow[]>(`${B}/artifactClean/runs`, { appId }),
}

export const gateApi = {
  rulePage: (body: Record<string, unknown>) => post<ApiPage<GateRuleRow>>(`${B}/gateRule/pageList`, body),
  ruleDetail: (id: string) => get<GateRuleRow>(`${B}/gateRule/detail`, { id }),
  ruleSave: (body: Record<string, unknown>) => post<string>(`${B}/gateRule/save`, body),
  ruleDeleted: (ids: string[]) => post<string>(`${B}/gateRule/deleted`, ids),
  ruleEnable: (id: string) => post<string>(`${B}/gateRule/enable`, { id }),
  resultList: (runId: string) => get<GateResultRow[]>(`${B}/gateResult/list`, { runId }),
  exempt: (body: Record<string, unknown>) => post<string>(`${B}/gateResult/exempt`, body),
  exemptionList: (body: Record<string, unknown>) => post<ApiPage<GateExemptionRow>>(`${B}/gateExemption/pageList`, body),
  baselineDetail: (appId: string) => get<BaselineRow>(`${B}/gateBaseline/detail`, { appId }),
  baselineReset: (body: Record<string, unknown>) => post<string>(`${B}/gateBaseline/reset`, body),
  baselineApprove: (body: Record<string, unknown>) => post<string>(`${B}/gateBaseline/approve`, body),
  techDebtPage: (body: Record<string, unknown>) => post<ApiPage<TechDebtRow>>(`${B}/techDebt/pageList`, body),
}

export const deployApi = {
  precheck: (body: Record<string, unknown>) => post<PrecheckResultVO>(`${B}/deploy/precheck`, body),
  start: (body: Record<string, unknown>) => post<{ id: string }>(`${B}/deploy/start`, body),
  pageList: (body: Record<string, unknown>) => post<ApiPage<DeployRecordRow>>(`${B}/deploy/pageList`, body),
  detail: (id: string) => get<Record<string, unknown>>(`${B}/deploy/detail`, { id }),
  batches: (id: string) => get<DeployBatchRow[]>(`${B}/deploy/batches`, { id }),
  pause: (id: string) => post<string>(`${B}/deploy/pause`, { id }),
  resume: (id: string) => post<string>(`${B}/deploy/resume`, { id }),
  health: (id: string) => get<Array<Record<string, unknown>>>(`${B}/deploy/health`, { id }),
  circuitContinue: (body: Record<string, unknown>) => post<string>(`${B}/deploy/circuit/continue`, body),
  rollback: (body: Record<string, unknown>) => post<{ id: string }>(`${B}/rollback/start`, body),
  rollbackPage: (body: Record<string, unknown>) => post<ApiPage<RollbackRow>>(`${B}/rollback/pageList`, body),
  rollbackCandidates: (envId: string) => get<RollbackCandidate[]>(`${B}/rollback/candidates`, { envId }),
}

export const releaseApi = {
  pageList: (body: Record<string, unknown>) => post<ApiPage<ReleaseRow>>(`${B}/release/pageList`, body),
  detail: (id: string) => get<ReleaseDetail>(`${B}/release/detail`, { id }),
  save: (body: Record<string, unknown>) => post<string>(`${B}/release/save`, body),
  deleted: (ids: string[]) => post<string>(`${B}/release/deleted`, ids),
  candidates: (appId: string) => get<ArtifactRow[]>(`${B}/release/artifacts/candidates`, { appId }),
  saveArtifacts: (body: Record<string, unknown>) => post<string>(`${B}/release/artifacts/save`, body),
  submit: (id: string) => post<string>(`${B}/release/submit/${id}`),
  withdraw: (id: string) => post<string>(`${B}/release/withdraw/${id}`),
  impact: (id: string) => get<ImpactResult>(`${B}/release/impact`, { id }),
  risk: (id: string) => get<RiskResult>(`${B}/release/risk`, { id }),
  checks: (id: string) => get<Array<Record<string, unknown>>>(`${B}/release/check/list`, { id }),
  saveChecks: (body: Record<string, unknown>) => post<string>(`${B}/release/check/save`, body),
  approve: (body: Record<string, unknown>) => post<string>(`${B}/release/approve`, body),
  reject: (body: Record<string, unknown>) => post<string>(`${B}/release/reject`, body),
  timeline: (id: string) => get<Array<Record<string, unknown>>>(`${B}/release/timeline`, { id }),
  start: (id: string) => post<string>(`${B}/release/start/${id}`),
  terminate: (body: Record<string, unknown>) => post<string>(`${B}/release/terminate`, body),
  recordPage: (body: Record<string, unknown>) => post<ApiPage<ReleaseRecordRow>>(`${B}/release/record/pageList`, body),
  strategyDetail: (appId: string) => get<ReleaseStrategyRow>(`${B}/releaseStrategy/detail`, { appId }),
  strategySave: (body: Record<string, unknown>) => post<string>(`${B}/releaseStrategy/save`, body),
  closurePage: (body: Record<string, unknown>) => post<ApiPage<ClosureRow>>(`${B}/networkClosure/pageList`, body),
  closureSave: (body: Record<string, unknown>) => post<string>(`${B}/networkClosure/save`, body),
}
export const changeTicketApi = {
  pageList: (body: Record<string, unknown>) => post<ApiPage<ChangeTicketRow>>(`${B}/changeTicket/pageList`, body),
  detail: (id: string) => get<Record<string, unknown>>(`${B}/changeTicket/detail`, { id }),
  save: (body: Record<string, unknown>) => post<string>(`${B}/changeTicket/save`, body),
  submit: (id: string) => post<string>(`${B}/changeTicket/submit/${id}`),
  approve: (body: Record<string, unknown>) => post<string>(`${B}/changeTicket/approve`, body),
  close: (id: string) => post<string>(`${B}/changeTicket/close/${id}`),
  cancel: (id: string) => post<string>(`${B}/changeTicket/cancel/${id}`),
}

export const aiApi = {
  failureAttribution: (runId: string) => post<AiOutputRow>(`${B}/ai/attribution`, { runId }),
  admission: (rcId: string) => post<AiOutputRow>(`${B}/ai/admission`, { rcId }),
  plan: (rcId: string) => post<AiOutputRow>(`${B}/ai/plan`, { rcId }),
  incident: (alertId: string) => post<AiOutputRow>(`${B}/ai/incident`, { alertId }),
  feedback: (body: Record<string, unknown>) => post<string>(`${B}/ai/feedback`, body),
  catalog: () => get<ActionCatalogRow[]>(`${B}/ai/action/catalog`),
  propose: (body: Record<string, unknown>) => post<ActionRecordRow>(`${B}/ai/action/propose`, body),
  execute: (body: Record<string, unknown>) => post<ActionRecordRow>(`${B}/ai/action/execute`, body),
  confirm: (id: string) => post<string>(`${B}/ai/action/confirm/${id}`),
  undo: (id: string) => post<string>(`${B}/ai/action/undo/${id}`),
  actionRecords: (body: Record<string, unknown>) => post<ApiPage<ActionRecordRow>>(`${B}/ai/action/records`, body),
  appLevel: (appId: string) => get<AppAiLevelRow>(`${B}/ai/appLevel`, { appId }),
  grantLevel: (body: Record<string, unknown>) => post<string>(`${B}/ai/appLevel/grant`, body),
  feedbackPage: (body: Record<string, unknown>) => post<ApiPage<AiFeedbackRow>>(`${B}/ai/feedback/pageList`, body),
}

export const metricApi = {
  dora: (body: Record<string, unknown>) => post<ApiPage<MetricRow>>(`${B}/metric/dora`, body),
  pipelineEfficiency: (body: Record<string, unknown>) => post<ApiPage<MetricRow>>(`${B}/metric/pipelineEfficiency`, body),
  stageDuration: (body: Record<string, unknown>) => post<ApiPage<StageStatRow>>(`${B}/metric/stageDuration`, body),
  overview: () => get<Record<string, unknown>>(`${B}/workbench/overview`),
}
export const systemExtApi = {
  notifyChannelPage: (body: Record<string, unknown>) => post<ApiPage<NotifyChannelRow>>(`${B}/system/notifyChannel/pageList`, body),
  notifyChannelSave: (body: Record<string, unknown>) => post<string>(`${B}/system/notifyChannel/save`, body),
  notifyTemplatePage: (body: Record<string, unknown>) => post<ApiPage<NotifyTemplateRow>>(`${B}/system/notifyTemplate/pageList`, body),
  notifyTemplateSave: (body: Record<string, unknown>) => post<string>(`${B}/system/notifyTemplate/save`, body),
  storagePolicyPage: (body: Record<string, unknown>) => post<ApiPage<StoragePolicyRow>>(`${B}/system/storagePolicy/pageList`, body),
  storagePolicySave: (body: Record<string, unknown>) => post<string>(`${B}/system/storagePolicy/save`, body),
  openTokenPage: (body: Record<string, unknown>) => post<ApiPage<OpenTokenRow>>(`${B}/system/openToken/pageList`, body),
  openTokenIssue: (body: Record<string, unknown>) => post<string>(`${B}/system/openToken/issue`, body),
  openTokenRotate: (id: string) => post<string>(`${B}/system/openToken/rotate/${id}`),
  openTokenDisable: (id: string) => post<string>(`${B}/system/openToken/disable/${id}`),
}
