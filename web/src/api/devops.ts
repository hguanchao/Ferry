/** 交付域接口：仅放请求函数，类型定义在 types-devops.ts */
import { get, post } from './client'
import type { ApiPage } from './types'
import type {
  ActionCatalogRow, ActionRecordRow, AiFeedbackRow, AiOutputRow, AppAiLevelRow,
  AppOption, AppOverview, AppDependencyRow, AppRow, ArtifactRow, BaselineRow,
  ChangeTicketRow, CleanPolicyRow, CleanTaskRow, ClosureRow, DeployBatchRow,
  DeployRecordRow, DerivationRow, DiffResult, EnvironmentRow, EnvConfigRow, GateExemptionRow,
  GateResultRow, GateRuleRow, ImpactResult, MetricRow, NotifyChannelRow, NotifyTemplateRow,
  OpenTokenRow, PipelineDefinition, PipelineRow, PipelineVersionRow, PrecheckResult,
  PrecheckResultVO, ReleaseDetail, ReleaseRecordRow, ReleaseRow, ReleaseStrategyRow,
  RiskResult, RollbackCandidate, RollbackRow, RunDetail, RunRow, StageStatRow, StoragePolicyRow,
  TemplateRow, TemplateVariable, TechDebtRow, TriggerRecordRow, TriggerResult, ValidateResult,
  WizardProgress,
} from './types-devops'

const B = '/ferry-devops'

export const appApi = {
  pageList: (body: Record<string, unknown>) => post<ApiPage<AppRow>>(`${B}/app/pageList`, body),
  save: (body: Record<string, unknown>) => post<string>(`${B}/app/save`, body),
  deleted: (ids: string[]) => post<string>(`${B}/app/deleted`, ids),
  options: (keyword?: string) => get<AppOption[]>(`${B}/app/options`, { keyword }),
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
  detail: (id: string) => get<Record<string, unknown>>(`${B}/pipeline/detail`, { id }),
  save: (body: Record<string, unknown>) => post<string>(`${B}/pipeline/save`, body),
  deleted: (ids: string[]) => post<string>(`${B}/pipeline/deleted`, ids),
  enable: (body: Record<string, unknown>) => post<string>(`${B}/pipeline/enable`, body),
  disable: (body: Record<string, unknown>) => post<string>(`${B}/pipeline/disable`, body),
  definition: (id: string, versionId?: string) => get<PipelineDefinition>(`${B}/pipeline/definition`, { id, versionId }),
  validateDefinition: (body: Record<string, unknown>) => post<ValidateResult>(`${B}/pipeline/definition/validate`, body),
  saveDefinition: (body: Record<string, unknown>) => post<{ id: string; versionNo: number }>(`${B}/pipeline/definition/save`, body),
  versionPage: (pipelineId: string, current = 1, size = 20) => get<ApiPage<PipelineVersionRow>>(`${B}/pipeline/version/pageList`, { pipelineId, current, size }),
  versionDiff: (leftId: string, rightId: string) => get<DiffResult>(`${B}/pipeline/version/diff`, { leftId, rightId }),
  versionRollback: (body: Record<string, unknown>) => post<{ id: string }>(`${B}/pipeline/version/rollback`, body),
  triggerPrecheck: (body: Record<string, unknown>) => post<PrecheckResult>(`${B}/pipeline/trigger/precheck`, body),
  trigger: (body: Record<string, unknown>, requestId: string) => post<TriggerResult>(`${B}/pipeline/trigger`, { ...body, requestId }),
  triggerRecords: (appId: string, current = 1, size = 20) => get<ApiPage<TriggerRecordRow>>(`${B}/pipeline/trigger/records`, { appId, current, size }),
}
export const templateApi = {
  pageList: (body: Record<string, unknown>) => post<ApiPage<TemplateRow>>(`${B}/pipelineTemplate/pageList`, body),
  detail: (id: string) => get<Record<string, unknown>>(`${B}/pipelineTemplate/detail`, { id }),
  save: (body: Record<string, unknown>) => post<string>(`${B}/pipelineTemplate/save`, body),
  deleted: (ids: string[]) => post<string>(`${B}/pipelineTemplate/deleted`, ids),
  enable: (id: string) => post<string>(`${B}/pipelineTemplate/enable`, { id }),
  disable: (body: Record<string, unknown>) => post<string>(`${B}/pipelineTemplate/disable`, body),
  versionPage: (templateId: string) => get<ApiPage<PipelineVersionRow>>(`${B}/pipelineTemplate/version/pageList`, { templateId }),
  versionDiff: (leftId: string, rightId: string) => get<DiffResult>(`${B}/pipelineTemplate/version/diff`, { leftId, rightId }),
  versionRollback: (body: Record<string, unknown>) => post<{ id: string }>(`${B}/pipelineTemplate/version/rollback`, body),
  derivations: (id: string) => get<DerivationRow[]>(`${B}/pipelineTemplate/derivations`, { id }),
  merge: (body: Record<string, unknown>) => post<{ id: string }>(`${B}/pipelineTemplate/merge`, body),
  variables: (versionId: string) => get<TemplateVariable[]>(`${B}/pipelineTemplate/variables`, { versionId }),
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
