import { dump, load, YAMLException } from 'js-yaml'
import type { PipelineDefinition, PipelineStage, PipelineStageType, PipelineTask } from '@/api/types'

/** 阶段类型、中文名与用途说明，顺序即新建阶段菜单的顺序 */
export const STAGE_TYPES: ReadonlyArray<{ value: PipelineStageType; label: string; desc: string }> = [
  { value: 'BUILD', label: '构建', desc: '拉取代码、编译打包' },
  { value: 'UNIT_TEST', label: '单元测试', desc: '执行测试与覆盖率检查' },
  { value: 'STATIC_SCAN', label: '静态扫描', desc: '代码规范与依赖漏洞' },
  { value: 'ARTIFACT_PUSH', label: '制品入库', desc: '推送镜像与制品归档' },
  { value: 'DEPLOY_NON_PROD', label: '部署非生产', desc: '测试或预发环境发布' },
  { value: 'INTEGRATION_TEST', label: '集成测试', desc: '接口与端到端验证' },
  { value: 'ADMISSION', label: '准入', desc: '发布前门禁检查' },
  { value: 'PROD_RELEASE', label: '生产发布', desc: '灰度与正式发布' },
]

const STAGE_TYPE_VALUES = new Set<string>(STAGE_TYPES.map((item) => item.value))

/** YAML 解析或结构不合法时抛出的错误，消息可直接展示给用户 */
export class PipelineDefinitionError extends Error {
  constructor(message: string) {
    super(message)
    this.name = 'PipelineDefinitionError'
  }
}

export function stageTypeLabel(type: PipelineStageType): string {
  return STAGE_TYPES.find((item) => item.value === type)?.label ?? type
}

export function createTask(): PipelineTask {
  return { name: '', command: '', timeoutMinutes: 10 }
}

export function createStage(type: PipelineStageType): PipelineStage {
  return {
    type,
    name: stageTypeLabel(type),
    timeoutMinutes: 30,
    continueOnFailure: false,
    artifacts: [],
    tasks: [],
  }
}

/** 把后端或 YAML 给的结构补齐成编辑器可用的形状，缺省值在这里统一 */
export function normalizeDefinition(value?: PipelineDefinition | null): PipelineDefinition {
  return {
    stages: (value?.stages ?? []).map((stage) => ({
      type: stage.type,
      name: stage.name ?? '',
      timeoutMinutes: stage.timeoutMinutes ?? 30,
      continueOnFailure: stage.continueOnFailure ?? false,
      artifacts: [...(stage.artifacts ?? [])],
      tasks: (stage.tasks ?? []).map((task) => ({
        name: task.name ?? '',
        command: task.command ?? '',
        timeoutMinutes: task.timeoutMinutes ?? 10,
      })),
    })),
  }
}

/** 校验定义结构：与后端校验器保持同一口径做第一道拦截，返回全部不满足的规则（按阶段顺序排列） */
export function collectDefinitionIssues(definition: PipelineDefinition): string[] {
  const issues: string[] = []
  if (!definition.stages.length) {
    issues.push('至少配置一个阶段')
  }
  if (definition.stages.length > 20) {
    issues.push('阶段数量不能超过 20 个')
  }
  const types = new Set<string>()
  for (const stage of definition.stages) {
    const label = stageTypeLabel(stage.type)
    if (types.has(stage.type)) {
      issues.push(`阶段类型「${label}」重复`)
    }
    types.add(stage.type)
    const name = stage.name.trim()
    if (!name) {
      issues.push('阶段名称不能为空')
    } else if (name.length > 50) {
      issues.push(`阶段名称「${name}」不能超过 50 个字符`)
    }
    if (!Number.isInteger(stage.timeoutMinutes) || stage.timeoutMinutes < 1 || stage.timeoutMinutes > 1440) {
      issues.push(`阶段「${name || label}」的超时时间需在 1-1440 分钟`)
    }
    if (stage.artifacts.length > 20) {
      issues.push(`阶段「${name || label}」的产出物声明不能超过 20 个`)
    }
    if (stage.artifacts.some((item) => item.length > 200)) {
      issues.push(`阶段「${name || label}」的产出物声明不能超过 200 个字符`)
    }
    if (stage.tasks.length > 30) {
      issues.push(`阶段「${name || label}」的任务不能超过 30 个`)
    }
    const taskNames = new Set<string>()
    for (const task of stage.tasks) {
      const taskName = task.name.trim()
      if (!taskName) {
        issues.push(`阶段「${name || label}」的任务名称不能为空`)
      } else if (taskName.length > 100) {
        issues.push(`任务名称「${taskName}」不能超过 100 个字符`)
      }
      if (taskNames.has(taskName)) {
        issues.push(`阶段「${name || label}」内任务名称重复`)
      }
      taskNames.add(taskName)
      if (!Number.isInteger(task.timeoutMinutes) || task.timeoutMinutes < 1 || task.timeoutMinutes > 1440) {
        issues.push(`任务「${taskName || '未命名任务'}」的超时时间需在 1-1440 分钟`)
      }
      if ((task.command ?? '').length > 2000) {
        issues.push(`任务「${taskName || '未命名任务'}」的执行命令不能超过 2000 个字符`)
      }
    }
  }
  return issues
}

/** 校验定义结构：返回第一条不满足的规则，全部通过返回 null */
export function validateDefinition(definition: PipelineDefinition): string | null {
  return collectDefinitionIssues(definition)[0] ?? null
}

/** 定义转 YAML，字段顺序固定，保证同一份定义每次生成的文本一致 */
export function definitionToYaml(definition: PipelineDefinition): string {
  return dump(normalizeDefinition(definition), { noRefs: true, lineWidth: 120, sortKeys: false })
}

/** YAML 转定义，语法错误带上行号，结构错误指出第几个阶段 */
export function yamlToDefinition(text: string): PipelineDefinition {
  let parsed: unknown
  try {
    parsed = load(text)
  } catch (error) {
    if (error instanceof YAMLException) {
      const line = error.mark ? error.mark.line + 1 : null
      throw new PipelineDefinitionError(line ? `第 ${line} 行：${error.reason}` : error.reason)
    }
    throw new PipelineDefinitionError(error instanceof Error ? error.message : 'YAML 解析失败')
  }
  return toDefinition(parsed)
}

export interface DefinitionChangeSummary {
  added: Array<{ type: PipelineStageType; label: string }>
  removed: Array<{ type: PipelineStageType; label: string }>
  modified: Array<{ type: PipelineStageType; label: string; fields: string[] }>
  orderChanged: boolean
  hasChanges: boolean
}

/** 对比两个版本的定义：按阶段类型配对，顺序变化单独标记 */
export function summarizeDefinitionChanges(
  previous: PipelineDefinition | null | undefined,
  next: PipelineDefinition,
): DefinitionChangeSummary {
  const previousStages = previous?.stages ?? []
  const previousMap = new Map(previousStages.map((stage) => [stage.type, stage]))
  const nextMap = new Map(next.stages.map((stage) => [stage.type, stage]))

  const added = next.stages
    .filter((stage) => !previousMap.has(stage.type))
    .map((stage) => ({ type: stage.type, label: stageTypeLabel(stage.type) }))
  const removed = previousStages
    .filter((stage) => !nextMap.has(stage.type))
    .map((stage) => ({ type: stage.type, label: stageTypeLabel(stage.type) }))
  const modified = next.stages.flatMap((stage) => {
    const before = previousMap.get(stage.type)
    if (!before) {
      return []
    }
    const fields = changedFields(before, stage)
    return fields.length ? [{ type: stage.type, label: stageTypeLabel(stage.type), fields }] : []
  })

  const previousOrder = previousStages.map((stage) => stage.type).filter((type) => nextMap.has(type))
  const nextOrder = next.stages.map((stage) => stage.type).filter((type) => previousMap.has(type))
  const orderChanged = previousOrder.join('|') !== nextOrder.join('|')

  return {
    added,
    removed,
    modified,
    orderChanged,
    hasChanges: added.length > 0 || removed.length > 0 || modified.length > 0 || orderChanged,
  }
}

function changedFields(before: PipelineStage, after: PipelineStage): string[] {
  const fields: string[] = []
  if (before.name !== after.name) {
    fields.push('阶段名称')
  }
  if (before.timeoutMinutes !== after.timeoutMinutes) {
    fields.push('超时时间')
  }
  if (before.continueOnFailure !== after.continueOnFailure) {
    fields.push('失败后继续')
  }
  if (JSON.stringify(before.artifacts) !== JSON.stringify(after.artifacts)) {
    fields.push('产出物')
  }
  if (JSON.stringify(before.tasks) !== JSON.stringify(after.tasks)) {
    fields.push('任务列表')
  }
  return fields
}

function toDefinition(value: unknown): PipelineDefinition {
  if (!isRecord(value)) {
    throw new PipelineDefinitionError('YAML 根节点必须是对象')
  }
  if (!Array.isArray(value.stages)) {
    throw new PipelineDefinitionError('YAML 中缺少 stages 数组')
  }
  return { stages: value.stages.map((stage, index) => toStage(stage, index)) }
}

function toStage(value: unknown, index: number): PipelineStage {
  if (!isRecord(value)) {
    throw new PipelineDefinitionError(`第 ${index + 1} 个阶段必须是对象`)
  }
  const type = typeof value.type === 'string' ? value.type : ''
  if (!isStageType(type)) {
    throw new PipelineDefinitionError(`第 ${index + 1} 个阶段的类型不合法：${type || '(空)'}`)
  }
  return {
    type,
    name: typeof value.name === 'string' ? value.name : '',
    timeoutMinutes: toNumber(value.timeoutMinutes, 30, `第 ${index + 1} 个阶段的超时时间`),
    continueOnFailure: typeof value.continueOnFailure === 'boolean' ? value.continueOnFailure : false,
    artifacts: Array.isArray(value.artifacts)
      ? value.artifacts.map((item) => (typeof item === 'string' ? item : String(item)))
      : [],
    tasks: Array.isArray(value.tasks)
      ? value.tasks.map((task, taskIndex) => toTask(task, index, taskIndex))
      : [],
  }
}

function toTask(value: unknown, stageIndex: number, taskIndex: number): PipelineTask {
  if (!isRecord(value)) {
    throw new PipelineDefinitionError(`第 ${stageIndex + 1} 个阶段第 ${taskIndex + 1} 个任务必须是对象`)
  }
  return {
    name: typeof value.name === 'string' ? value.name : '',
    command: typeof value.command === 'string' ? value.command : '',
    timeoutMinutes: toNumber(
      value.timeoutMinutes,
      10,
      `第 ${stageIndex + 1} 个阶段第 ${taskIndex + 1} 个任务的超时时间`,
    ),
  }
}

function toNumber(value: unknown, fallback: number, label: string): number {
  if (value === undefined || value === null || value === '') {
    return fallback
  }
  if (typeof value === 'number' && Number.isFinite(value)) {
    return value
  }
  throw new PipelineDefinitionError(`${label}必须是数字`)
}

function isStageType(value: string): value is PipelineStageType {
  return STAGE_TYPE_VALUES.has(value)
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null && !Array.isArray(value)
}
