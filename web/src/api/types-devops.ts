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
  repoToken?: string
  repoBranch?: string
  repoCommitId?: string
  description?: string
  status: number
  prodDeployTypes?: string
  pipelineCount?: number
  lastRunStatus?: number
  lastRunTime?: string
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

export interface AppOption { value: ID; label: string; code: string }
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
  status: number
  version: number
  createTime: string
}
export interface PipelineVersionRow { id: ID; pipelineId: ID; versionNo: number; changeNote: string; sourceType: number; createTime: string }
export interface PipelineDefinition { json: string; yaml?: string; versionId?: ID }
export interface ValidateResult { valid: boolean; errors: Array<{ line: number; message: string }> }
/** 流水线阶段定义：与后端 PipelineStageDTO 对应 */
export interface PipelineStageDef {
  type: number
  name: string
  timeoutSeconds?: number
  tasks: PipelineTaskDef[]
  outputs?: string[]
}
/** 流水线任务定义：与后端 PipelineTaskDTO 对应 */
export interface PipelineTaskDef {
  name: string
  executor: string
  scriptRef?: string
  scriptContent?: string
  params?: Record<string, unknown>
}
export interface DiffResult { left: string; right: string; lines: Array<{ type: string; leftNo?: number; rightNo?: number; text: string }> }
export interface PrecheckItem { type: number; conclusion: number; detail?: string }
export interface PrecheckResult { items: PrecheckItem[]; canTrigger: boolean }
export interface TriggerResult { result: string; runId?: ID; artifactDigest?: string; message: string }
export interface TriggerRecordRow { id: ID; appId: ID; pipelineId: ID; branch: string; commitId: string; result: number; rejectReason?: string; createTime: string }

/* ------------------------------ 运行 ------------------------------ */
export interface RunRow {
  id: ID
  runNo: string
  pipelineId: ID
  pipelineName?: string
  pipelineVersionNo?: number
  appId: ID
  appName?: string
  branch: string
  commitId: string
  status: number
  blockReason?: number
  triggerUser: ID
  artifactId?: ID
  startTime?: string
  endTime?: string
  duration?: number
  createTime: string
}
export interface RunStageRow { id: ID; runId: ID; stageType: number; stageName: string; seq: number; status: number; startTime?: string; endTime?: string; duration?: number }
export interface RunTaskRow { id: ID; stageId: ID; name: string; status: number; exitCode?: number; logLineCount: number; isTruncated: number }
export interface LogSegmentRow { id: ID; taskId: ID; segmentNo: number; startLine: number; endLine: number; content: string; isTruncated: number }
/** 运行详情：与后端 RunDetailVO 的 { run, stages, tasks } 嵌套结构一致 */
export interface RunDetail {
  run: RunRow
  stages: RunStageRow[]
  tasks: RunTaskRow[]
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
export interface GateRuleRow { id: ID; scopeType: number; appId?: ID; ruleType: number; ruleName: string; threshold: string; isExemptAllowed: number; isEnabled: number; version: number }
export interface GateResultRow { id: ID; runId: ID; stageId: ID; ruleId?: ID; ruleSnapshot: string; conclusion: number; currentValue?: string; threshold?: string; fetchError?: string; createTime: string }
export interface GateExemptionRow { id: ID; gateResultId: ID; reason: string; expireAt: string; status: number; createTime: string }
export interface BaselineRow { id: ID; appId: ID; tool: string; totalCount: number; isActive: number; createTime: string }
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
export interface StageStatRow { id: ID; statDate: string; appId: ID; stageType: number; p50?: number; p90?: number; failCount: number }
export interface NotifyChannelRow { id: ID; name: string; type: number; isEnabled: number }
export interface NotifyTemplateRow { id: ID; code: string; event: string; content: string; isEnabled: number }
export interface OpenTokenRow { id: ID; appId: ID; name: string; status: number; lastRotateTime?: string; createTime: string }

export { type ApiPage }
