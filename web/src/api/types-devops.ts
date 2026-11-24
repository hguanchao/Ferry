/** 交付域接口契约类型：与后端 devops-api 的 VO/DTO 对应，id 一律按 string 处理 */

import type { ApiPage, ID } from './types'

/* ------------------------------ 应用 ------------------------------ */
export interface AppRow {
  id: ID
  code: string
  name: string
  deptId: ID
  deptName?: string
  ownerId?: ID
  ownerName?: string
  coreLink: number
  repoType: number
  repoUrl?: string
  repoBranch?: string
  repoCommitId?: string
  description?: string
  status: number
  createTime: string
}

// TODO(dev_app-schema): dev_app_repo 已删除，AppRepoRow 类型已移除。
export interface AppDependencyRow {
  id: ID
  appId: ID
  depName: string
  depAppId?: ID
  versionRange?: string
}

export interface AppOption {
  value: ID
  label: string
  code: string
  repoUrl?: string
  repoBranch?: string
  repoCommitId?: string
}
export interface WizardProgress { appId: ID; completedSteps: number[]; skippedSteps: number[]; status: number }
export interface AppOverview { appId: ID; pipelineCount: number; envCount: number; runningCount: number; prodArtifactDigest?: string; prodDeployType?: string; blockers: Array<Record<string, unknown>> }

/* ------------------------------ 环境 ------------------------------ */
export interface EnvironmentRow {
  id: ID
  appId: ID
  name: string
  category: number
  deployType: number
  isProtected: number
  observeMinutes?: number
  configVersion: number
  currentArtifactId?: ID
  status: number
  createTime: string
}

export interface EnvConfigRow { id: ID; envId: ID; configKey: string; configValue?: string; valueType: number; version: number; isEncrypted: number }
export interface StoragePolicyRow { id: ID; scope: string; retentionDays: number; maxSizeGb: number; description?: string }

/* ------------------------------ 流水线 ------------------------------ */
export interface PipelineRow {
  id: ID
  appId: ID
  name: string
  /** 状态 0停用 1启用 2待配置（空定义，Job 已禁用） 3同步失败（可重试同步） */
  status: number
  version: number
  appName?: string
  repoBranch?: string
  stageCount?: number
  createTime: string
  updateTime?: string
}
/** 触发弹窗提交候选：与后端 RepoCommitVO 对应 */
export interface RepoCommitOption {
  sha: string
  shortSha: string
  message: string
  authorName?: string
  committedAt?: string
}
/** 流水线阶段定义：与后端 PipelineStageDTO 对应 */
export interface PipelineStageDef {
  type: number
  name: string
  buildTool?: string
  timeout?: number
  tasks: PipelineTaskDef[]
  outputs?: string
  pipelineId?: ID
  version?: number
}
/** 流水线任务定义：与后端 PipelineStageTaskDTO 对应 */
export interface PipelineTaskDef {
  name: string
  executor: number
  scriptRef?: string
  scriptContent?: string
  scriptParams?: string
}
/** 阶段响应：与后端 PipelineStageVO 及阶段表字段对应 */
export interface PipelineStageRow extends Omit<PipelineStageDef, 'tasks'> {
  id: ID
  pipelineId: ID
  version: number
  sort: number
  taskList?: PipelineStageTaskRow[]
}
/** 阶段任务响应：与后端 PipelineStageTaskVO 及任务表字段对应 */
export interface PipelineStageTaskRow extends PipelineTaskDef {
  id: ID
  stageId: ID
  version: number
  sort: number
}
/** 流水线详情：复用列表字段，附带当前版本阶段/任务定义 */
export interface PipelineDetail extends PipelineRow {
  stages?: PipelineStageRow[]
}
export interface PrecheckItem { type: number; conclusion: number; detail?: string }
export interface PrecheckResult { items: PrecheckItem[]; canTrigger: boolean }
export interface TriggerResult { result: string; runId?: ID; artifactDigest?: string; message: string }
export interface RunTrigger { id: ID; appId: ID; pipelineId: ID; branch: string; commitId: string; result: number; rejectReason?: string; createTime: string }

/* ------------------------------ 运行 ------------------------------ */
export interface Run {
  id: ID
  code: string
  pipelineId: ID
  pipelineName?: string
  pipelineVersionNo?: number
  appId: ID
  appName?: string
  branch: string
  refType?: number
  refValue: string
  triggerUser: ID
  triggerNote?: string
  status: number
  preCheckResult?: string
  jenkinsJobId?: string
  jenkinsBuildNo?: number
  startTime?: string
  endTime?: string
  createTime: string
}
export interface RunStage { id: ID; runId: ID; type: number; name: string; status: number; result?: string; sort: number; startTime?: string; endTime?: string }
export interface RunStageTask { id: ID; runId: ID; runStageId: ID; name: string; executor?: string; scriptRef?: string; status: number; result?: string; sort: number; startTime?: string; endTime?: string }
export interface RunLogSegment { id: ID; taskId: ID; segmentNo: number; startLine: number; endLine: number; content: string; isTruncated: number }
/** 运行详情：与后端 RunDetailVO 的 { run, stages, tasks } 嵌套结构一致 */
export interface RunDetail {
  run: Run
  stages: RunStage[]
  tasks: RunStageTask[]
}

/* ------------------------------ 制品 ------------------------------ */
export interface ArtifactRow {
  id: ID
  appId: ID
  version: string
  form: number
  digest: string
  size: number
  runId: ID
  branch?: string
  commitId?: string
  prodUseStatus: number
  confirmStatus: number
  createTime: string
  envTags?: number[]
}
export interface CleanPolicyRow { id: ID; appId: ID; keepLatest: number; isKeepProd: number; softDeleteDays: number; cron?: string; isAutoExec: number }
export interface CleanTaskRow { id: ID; appId: ID; taskTime: string; pendingCount: number; deletedCount: number; status: number }

/* ------------------------------ 门禁 ------------------------------ */
export interface GateRule { id: ID; scopeType: number; appId?: ID; ruleType: number; ruleName: string; threshold: string; isExemptAllowed: number; isEnabled: number; version: number }
export interface RunStageGate {
  id: ID
  runId: ID
  runStageId: ID
  ruleId?: ID
  ruleType: number
  ruleVersion?: number
  ruleSnapshot: string
  currentValue?: string
  threshold?: string
  status: number
  evidence?: string
  source?: string
  schemaVersion?: string
  fetchError?: string
  createTime: string
  updateTime?: string
}
export interface GateExemption { id: ID; runStageGateId: ID; reason: string; expireAt: string; status: number; createTime: string }
export interface GateBaseline { id: ID; appId: ID; tool: string; totalCount: number; isActive: number; createTime: string }
export interface TechDebtRow { id: ID; appId: ID; fingerprint: string; severity: number; status: number; file_path?: string; filePath?: string; assignee?: ID; firstSeen: string }

/* ------------------------------ 部署 ------------------------------ */
export interface DeployRecordRow {
  id: ID
  deployNo: string
  appId: ID
  envId: ID
  deployType: number
  artifactId: ID
  status: number
  operator: ID
  confirmer?: ID
  startTime?: string
  endTime?: string
  failReason?: string
  createTime: string
}
export interface DeployBatchRow { id: ID; deployId: ID; batchNo: number; targetCount: number; status: number; healthResult?: number }
export interface PrecheckResultVO { token: string; items: PrecheckItem[]; canDeploy: boolean }
export interface RollbackRow { id: ID; envId: ID; artifactId: ID; reason: string; result: number; createTime: string }
export interface RollbackCandidate { artifactId: ID; version: string; digest: string; deployedAt?: string }

/* ------------------------------ 发布 ------------------------------ */
export interface ReleaseRow {
  id: ID
  rcNo: string
  title: string
  status: number
  riskLevel?: number
  riskScore?: string
  windowStart: string
  windowEnd: string
  creator: ID
  createTime: string
}
export interface ReleaseDetail extends ReleaseRow { description?: string; riskDetail?: string; changeTicketId?: ID; artifacts: Array<Record<string, unknown>>; checks: Array<Record<string, unknown>>; timeline: Array<Record<string, unknown>> }
export interface ChangeTicketRow { id: ID; ticketNo: string; type: number; title: string; status: number; applicant: ID; createTime: string }
export interface ReleaseStrategyRow { id: ID; appId: ID; windowsJson: string; isClosureExcluded: number; observeMinutes?: number }
export interface ClosureRow { id: ID; startDate: string; endDate: string; scope: number; appId?: ID; reason?: string }
export interface ReleaseRecordRow { id: ID; rcId: ID; appId: ID; envId: ID; result: number; operator: ID; startTime?: string; createTime: string }
export interface RiskResult { level: number; score: string; detail: string }
export interface ImpactResult { layers: Array<Record<string, unknown>> }

/* ------------------------------ AI ------------------------------ */
export interface AiOutputRow { id: ID; type: number; bizId: string; appId?: ID; category?: string; conclusion?: string; confidence?: number; status: number; createTime: string }
export interface ActionCatalogRow { id: ID; actionId: string; name: string; level: number; isUndoable: number; isEnabled: number }
export interface ActionRecordRow { id: ID; actionId: string; level: number; appId?: ID; status: number; operator: ID; executeTime?: string; error?: string; createTime: string }
export interface AiFeedbackRow { id: ID; outputId: ID; isHelpful: number; userId: ID; createTime: string }
export interface AppAiLevelRow { id: ID; appId: ID; level: number; grantedBy: ID; grantedAt: string }

/* ------------------------------ 度量与系统 ------------------------------ */
export interface MetricRow { id: ID; statDate: string; appId: ID; deployFreq?: number; leadTimeP50?: number; changeFailRate?: string; mttrP50?: number; dataStatus: number }
export interface StageStat { id: ID; statDate: string; appId: ID; stageType: number; p50?: number; p90?: number; failCount: number }
export interface NotifyChannelRow { id: ID; name: string; type: number; isEnabled: number }
export interface NotifyTemplateRow { id: ID; code: string; event: string; content: string; isEnabled: number }
export interface OpenTokenRow { id: ID; appId: ID; name: string; status: number; lastRotateTime?: string; createTime: string }

export { type ApiPage }
