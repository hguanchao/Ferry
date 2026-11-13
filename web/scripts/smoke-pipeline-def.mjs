/**
 * 冒烟测试：流水线定义编辑与版本对比
 *
 * 覆盖三块纯函数：
 * 1. 定义 ↔ YAML 往返：字段顺序固定，保证同一份定义生成的文本稳定，版本 diff 不会因键序抖动；
 * 2. 定义校验：空阶段、重复阶段类型、空任务名这些错误必须在保存前被拦下；
 * 3. 版本对比：新增/删除/修改/顺序调整的判定，以及行 diff 的左右行号。
 *
 * 用法：node --experimental-strip-types scripts/smoke-pipeline-def.mjs
 */
import {
  collectDefinitionIssues,
  createStage,
  definitionToYaml,
  summarizeDefinitionChanges,
  validateDefinition,
  yamlToDefinition,
} from '../src/lib/pipeline.ts'
import { diffLines } from '../src/lib/diff.ts'

const results = []
function check(name, actual, expected) {
  const pass = JSON.stringify(actual) === JSON.stringify(expected)
  results.push({ name, pass })
  console.log(`${pass ? 'PASS' : 'FAIL'}  ${name}${pass ? '' : ` → 实际 ${JSON.stringify(actual)}`}`)
}

function stage(type, name, timeoutMinutes, tasks = []) {
  return {
    type,
    name,
    timeoutMinutes,
    continueOnFailure: false,
    artifacts: ['target/*.jar'],
    tasks,
  }
}

const definition = {
  stages: [
    stage('BUILD', '构建', 30, [
      { name: 'compile', command: 'mvn -B package', timeoutMinutes: 10 },
    ]),
    stage('UNIT_TEST', '单元测试', 20, [
      { name: 'test', command: 'mvn -B test', timeoutMinutes: 10 },
    ]),
  ],
}

// 1. YAML 往返
const yaml = definitionToYaml(definition)
check('YAML 往返后定义不变', yamlToDefinition(yaml), definition)
check('YAML 以 stages 开头', yaml.startsWith('stages:'), true)

const firstStageBlock = yaml.split('  - type: BUILD')[1]?.split('  - type: UNIT_TEST')[0] ?? ''
const fieldOrder = ['name:', 'timeoutMinutes:', 'continueOnFailure:', 'artifacts:', 'tasks:']
  .map((key) => firstStageBlock.indexOf(key))
check('阶段字段顺序固定', fieldOrder.every((value, index) => index === 0 || fieldOrder[index - 1] < value), true)

// 2. YAML 错误定位
try {
  yamlToDefinition('stages: [\n')
  check('非法 YAML 报错', false, true)
} catch (error) {
  check('非法 YAML 带行号', /第 \d+ 行/.test(error.message), true)
}

try {
  yamlToDefinition('stages:\n  - type: UNKNOWN\n    name: 未知\n')
  check('未知阶段类型报错', false, true)
} catch (error) {
  check('未知阶段类型报错', error.message.includes('类型不合法'), true)
}

// 3. 定义校验
check('空定义被拦截', validateDefinition({ stages: [] }), '至少配置一个阶段')

const duplicate = { stages: [createStage('BUILD'), createStage('BUILD')] }
check('重复阶段类型被拦截', validateDefinition(duplicate)?.includes('重复'), true)

const blankTask = {
  stages: [
    {
      ...createStage('BUILD'),
      name: '构建',
      tasks: [{ name: '  ', command: '', timeoutMinutes: 10 }],
    },
  ],
}
check('空任务名被拦截', validateDefinition(blankTask)?.includes('任务名称不能为空'), true)

const aggregated = {
  stages: [
    { ...createStage('BUILD'), name: '  ' },
    {
      ...createStage('UNIT_TEST'),
      name: '单元测试',
      tasks: [{ name: '', command: '', timeoutMinutes: 10 }],
    },
  ],
}
check('校验收集全部问题', collectDefinitionIssues(aggregated).length, 2)
check('首条问题保持阶段顺序', validateDefinition(aggregated), '阶段名称不能为空')

const valid = {
  stages: [
    {
      ...createStage('BUILD'),
      name: '构建',
      tasks: [{ name: 'compile', command: 'mvn -B package', timeoutMinutes: 10 }],
    },
  ],
}
check('合法定义通过校验', validateDefinition(valid), null)

// 4. 变更摘要
const previous = {
  stages: [stage('BUILD', '构建', 30), stage('UNIT_TEST', '单元测试', 20)],
}
const next = {
  stages: [stage('UNIT_TEST', '单元测试', 20), stage('BUILD', '构建', 60), stage('STATIC_SCAN', '静态扫描', 15)],
}
const summary = summarizeDefinitionChanges(previous, next)
check('新增阶段', summary.added.map((item) => item.type), ['STATIC_SCAN'])
check('删除阶段', summary.removed.map((item) => item.type), [])
check('修改阶段字段', summary.modified.map((item) => item.fields), [['超时时间']])
check('顺序调整被标记', summary.orderChanged, true)
check('存在改动', summary.hasChanges, true)

const initial = summarizeDefinitionChanges(null, definition)
check('首次保存全部算新增', initial.added.map((item) => item.type), ['BUILD', 'UNIT_TEST'])
check('首次保存无删除', initial.removed.length, 0)

// 5. 行 diff
const rows = diffLines('a\nb\nc', 'a\nx\nc')
check('行 diff 类型', rows.map((row) => row.type), ['same', 'del', 'add', 'same'])
check('删除行左侧行号', rows[1].leftNo, 2)
check('新增行右侧行号', rows[2].rightNo, 2)

const addedRows = diffLines('', 'a')
check('空文本到一行', addedRows.map((row) => row.type), ['add'])
check('新增行号从 1 开始', addedRows[0].rightNo, 1)

const failed = results.filter((item) => !item.pass)
console.log(`\n${results.length - failed.length}/${results.length} 通过`)
if (failed.length) {
  process.exit(1)
}
