import { useEffect, useMemo, useRef, useState } from 'react'
import { useMutation, useQuery } from '@tanstack/react-query'
import { App as AntApp, Drawer, Form, Input, Modal, Popover, Select, TreeSelect } from 'antd'
import { Layers, Plus, RotateCcw, Save, X } from 'lucide-react'
import { deptApi } from '@/api/system'
import { pipelineTemplateApi } from '@/api/devops'
import type { PipelineStageType, PipelineTemplateDetail, TemplateDefinition } from '@/api/types'
import PipelineDefinitionEditor, {
  StageTypeMenu,
  type PipelineDefinitionEditorHandle,
} from '@/components/PipelineDefinitionEditor'
import TemplateVariablePanel from '@/components/TemplateVariablePanel'
import {
  STAGE_TYPES,
  TEMPLATE_BUILTIN_VARIABLES,
  collectTemplateIssues,
  normalizeTemplateDefinition,
  summarizeDefinitionChanges,
} from '@/lib/pipeline'
import { showError } from '@/lib/notify'
import { useAuthStore } from '@/stores/auth'

interface TemplateEditorDrawerProps {
  open: boolean
  /** create：新建模板并生成 V1；definition：只保存定义 */
  mode: 'create' | 'definition'
  template: PipelineTemplateDetail | null
  onClose: () => void
  onSaved: () => void
}

interface TemplateFormValues {
  name?: string
  scope?: number
  deptId?: string
  description?: string
}

export default function TemplateEditorDrawer({
  open,
  mode,
  template,
  onClose,
  onSaved,
}: TemplateEditorDrawerProps) {
  const { message } = AntApp.useApp()
  const isSuper = useAuthStore((state) => state.user?.isSuper === 1)
  const [form] = Form.useForm<TemplateFormValues>()
  const [definition, setDefinition] = useState<TemplateDefinition>({ stages: [], variables: [] })
  const [initial, setInitial] = useState<TemplateDefinition>({ stages: [], variables: [] })
  const [original, setOriginal] = useState<TemplateDefinition | null>(null)
  const [yamlValid, setYamlValid] = useState(true)
  const [confirmOpen, setConfirmOpen] = useState(false)
  const [changeNote, setChangeNote] = useState('')
  const [stageMenuOpen, setStageMenuOpen] = useState(false)
  const editorRef = useRef<PipelineDefinitionEditorHandle>(null)

  // 团队级模板的归属部门候选；没有部门权限的用户下拉为空，只能建组织级（超管）
  const deptQuery = useQuery({
    queryKey: ['dept-tree'],
    queryFn: () => deptApi.treeList(),
    enabled: open && mode === 'create',
    retry: false,
  })

  useEffect(() => {
    if (!open) {
      return
    }
    form.resetFields()
    setYamlValid(true)
    setConfirmOpen(false)
    setChangeNote('')
    setStageMenuOpen(false)
    if (mode === 'create') {
      const empty = { stages: [], variables: TEMPLATE_BUILTIN_VARIABLES.map((item) => ({ ...item })) }
      setInitial(empty)
      setDefinition(empty)
      setOriginal(null)
      form.setFieldsValue({ scope: isSuper ? 0 : 1 })
      return
    }
    if (template) {
      const normalized = normalizeTemplateDefinition(template.definition)
      setInitial(normalized)
      setDefinition(normalized)
      setOriginal(normalized)
    }
  }, [open, mode, template?.id, form, isSuper])

  const issues = useMemo(() => collectTemplateIssues(definition), [definition])
  const summary = useMemo(
    () => summarizeDefinitionChanges(original ? { stages: original.stages } : null, { stages: definition.stages }),
    [original, definition],
  )

  const usedTypes = useMemo(
    () => new Set<string>(definition.stages.map((stage) => stage.type)),
    [definition.stages],
  )
  const allUsed = definition.stages.length >= STAGE_TYPES.length
  const taskCount = definition.stages.reduce((sum, stage) => sum + stage.tasks.length, 0)
  const nextVersion = (template?.currentVersionNo ?? 0) + 1
  const scope = Form.useWatch('scope', form)

  const finish = (text: string) => {
    message.success(text || '保存成功')
    setConfirmOpen(false)
    onSaved()
    onClose()
  }

  const createMutation = useMutation({
    mutationFn: (body: Record<string, unknown>) => pipelineTemplateApi.save(body),
    onSuccess: finish,
    onError: showError,
  })

  const versionMutation = useMutation({
    mutationFn: (body: { templateId: string; definition: TemplateDefinition; changeNote: string }) =>
      pipelineTemplateApi.versionSave(body),
    onSuccess: finish,
    onError: showError,
  })

  const focusIssues = (text: string) => {
    showError(text)
    editorRef.current?.showOverview()
  }

  const submitCreate = async () => {
    if (!yamlValid) {
      focusIssues('YAML 存在语法错误，请先修正')
      return
    }
    if (issues.length) {
      focusIssues(issues.length > 1 ? `${issues[0]}（共 ${issues.length} 项）` : issues[0])
      return
    }
    let values: TemplateFormValues
    try {
      values = await form.validateFields()
    } catch (error) {
      const first = (error as { errorFields?: Array<{ errors?: string[] }> }).errorFields?.[0]?.errors?.[0]
      showError(first ?? '请完善模板信息')
      return
    }
    createMutation.mutate({
      name: values.name,
      scope: values.scope,
      deptId: values.scope === 1 ? values.deptId : undefined,
      description: values.description,
      definition,
    })
  }

  const submitDefinition = () => {
    if (!template) {
      return
    }
    if (!yamlValid) {
      focusIssues('YAML 存在语法错误，请先修正')
      return
    }
    if (issues.length) {
      focusIssues(issues.length > 1 ? `${issues[0]}（共 ${issues.length} 项）` : issues[0])
      return
    }
    if (!summary.hasChanges) {
      showError('定义没有变化')
      return
    }
    setChangeNote('')
    setConfirmOpen(true)
  }

  const confirmDefinitionSave = () => {
    if (!template) {
      return
    }
    if (!changeNote.trim()) {
      showError('请填写变更说明')
      return
    }
    versionMutation.mutate({
      templateId: template.id,
      definition,
      changeNote: changeNote.trim(),
    })
  }

  const resetTemplate = () => {
    editorRef.current?.reset()
    setDefinition(initial)
    if (mode === 'create') {
      form.resetFields()
      form.setFieldsValue({ scope: isSuper ? 0 : 1 })
    }
    message.success('已重置')
  }

  const handleAddStage = (type: PipelineStageType) => {
    editorRef.current?.addStage(type)
    setStageMenuOpen(false)
  }

  // 变量表与描述经检查器概览页签注入，画布编辑器只管阶段
  const overviewExtra = (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
      <div className="ferry-studio__field">
        <span>适用场景</span>
        <Form.Item name="description" noStyle>
          <Input.TextArea
            className="ferry-studio__textarea"
            rows={2}
            maxLength={500}
            showCount
            placeholder="选填，说明这份模板适用的交付场景"
          />
        </Form.Item>
      </div>
      <TemplateVariablePanel
        variables={definition.variables}
        onChange={(variables) => setDefinition((prev) => ({ ...prev, variables }))}
      />
    </div>
  )

  return (
    <>
      <Drawer
        open={open}
        size="100vw"
        closable={false}
        onClose={onClose}
        destroyOnHidden
        rootClassName="ferry-studio-drawer"
        styles={{ body: { padding: 0, overflow: 'hidden' } }}
      >
        <Form<TemplateFormValues> form={form} component={false} layout="vertical" requiredMark="optional">
          <div className="ferry-studio">
            <header className="ferry-studio__topbar">
              <div className="ferry-studio__brand">
                <span className="ferry-studio__brand-mark" aria-hidden="true">
                  <Layers size={19} />
                </span>
                <div className="ferry-studio__brand-text">
                  <b>渡口 Ferry</b>
                  <span>FLOW / TEMPLATE</span>
                </div>
              </div>
              <span className="ferry-studio__divider" aria-hidden="true" />
              <div className="ferry-studio__doc">
                <div className="ferry-studio__crumb">
                  交付编排 / {mode === 'create' ? '新建模板' : '编辑模板定义'}
                </div>
                <div className="ferry-studio__doc-line">
                  {mode === 'create' ? (
                    <Form.Item
                      name="name"
                      noStyle
                      rules={[{ required: true, message: '请输入模板名称' }]}
                    >
                      <Input
                        className="ferry-studio__name"
                        maxLength={100}
                        placeholder="未命名模板"
                        aria-label="模板名称"
                      />
                    </Form.Item>
                  ) : (
                    <span className="ferry-studio__name is-static">{template?.name ?? '未命名模板'}</span>
                  )}
                  <span className="ferry-studio__status">
                    {mode === 'create'
                      ? '草稿'
                      : template?.currentVersionNo
                        ? `V${template.currentVersionNo}`
                        : '未配置'}
                  </span>
                </div>
              </div>

              <div className="ferry-studio__app">
                <span className="ferry-studio__app-label">SCOPE</span>
                {mode === 'create' ? (
                  <Form.Item
                    name="scope"
                    noStyle
                    rules={[{ required: true, message: '请选择模板层级' }]}
                  >
                    <Select
                      style={{ width: 130 }}
                      placeholder="模板层级"
                      options={[
                        { value: 0, label: '组织级', disabled: !isSuper },
                        { value: 1, label: '团队级' },
                      ]}
                    />
                  </Form.Item>
                ) : (
                  <span className="ferry-studio__app-static">
                    {template?.scope === 0 ? '组织级' : '团队级'}
                  </span>
                )}
                {mode === 'create' && scope === 1 && (
                  <Form.Item
                    name="deptId"
                    noStyle
                    rules={[{ required: true, message: '请选择归属部门' }]}
                  >
                    <TreeSelect
                      style={{ width: 200 }}
                      placeholder="归属部门"
                      treeData={deptQuery.data ?? []}
                      fieldNames={{ label: 'name', value: 'id', children: 'children' }}
                      treeDefaultExpandAll
                      allowClear
                    />
                  </Form.Item>
                )}
              </div>

              <div className="ferry-studio__stats" aria-live="polite">
                <span>
                  <b>{definition.stages.length}</b> 阶段
                </span>
                <span className="ferry-studio__stat-dot" aria-hidden="true" />
                <span>
                  <b>{definition.variables.length}</b> 变量
                </span>
                <span className="ferry-studio__stat-dot" aria-hidden="true" />
                <span>
                  <b>{taskCount}</b> 任务
                </span>
              </div>

              <div className="ferry-studio__actions">
                <button type="button" className="ferry-studio__btn is-ghost" onClick={resetTemplate}>
                  <RotateCcw size={13} />
                  重置
                </button>
                <Popover
                  trigger="click"
                  placement="bottomRight"
                  open={stageMenuOpen}
                  onOpenChange={setStageMenuOpen}
                  content={
                    <StageTypeMenu usedTypes={usedTypes} onPick={handleAddStage} />
                  }
                  rootClassName="ferry-studio-popover"
                >
                  <button type="button" className="ferry-studio__btn is-ghost" disabled={allUsed}>
                    <Plus size={14} />
                    添加阶段
                  </button>
                </Popover>
                <button
                  type="button"
                  className="ferry-studio__btn is-primary"
                  disabled={!yamlValid || createMutation.isPending || versionMutation.isPending}
                  onClick={() => (mode === 'create' ? void submitCreate() : submitDefinition())}
                >
                  <Save size={13} />
                  {mode === 'create' ? '创建并生成 V1' : '保存定义'}
                </button>
                <button
                  type="button"
                  className="ferry-studio__icon-btn"
                  onClick={onClose}
                  aria-label="关闭"
                >
                  <X size={15} />
                </button>
              </div>
            </header>

            <div className="ferry-studio__stage">
              <PipelineDefinitionEditor
                ref={editorRef}
                definition={{ stages: definition.stages }}
                onChange={(next) => setDefinition((prev) => ({ ...prev, stages: next.stages }))}
                onValidityChange={setYamlValid}
                initialDefinition={{ stages: initial.stages }}
                overviewExtra={overviewExtra}
              />
            </div>
          </div>
        </Form>
      </Drawer>

      <Modal
        open={confirmOpen}
        width={560}
        title={template ? `保存为 V${nextVersion}` : '保存定义'}
        okText="保存"
        cancelText="取消"
        confirmLoading={versionMutation.isPending}
        onOk={confirmDefinitionSave}
        onCancel={() => setConfirmOpen(false)}
        destroyOnHidden
      >
        <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
          <div>
            <div className="ferry-caption" style={{ marginBottom: 6 }}>本次改动</div>
            <div style={{ display: 'flex', flexDirection: 'column', gap: 4, fontSize: 13 }}>
              {summary.added.length > 0 && (
                <span>新增阶段：{summary.added.map((item) => item.label).join('、')}</span>
              )}
              {summary.removed.length > 0 && (
                <span>删除阶段：{summary.removed.map((item) => item.label).join('、')}</span>
              )}
              {summary.modified.length > 0 && (
                <span>
                  修改阶段：
                  {summary.modified
                    .map((item) => `${item.label}（${item.fields.join('、')}）`)
                    .join('；')}
                </span>
              )}
              {summary.orderChanged && <span>阶段顺序调整</span>}
            </div>
          </div>
          <div>
            <div className="ferry-caption" style={{ marginBottom: 6 }}>变更说明</div>
            <Input.TextArea
              rows={3}
              maxLength={500}
              showCount
              value={changeNote}
              placeholder="说明这次调整的原因，例如「构建超时调整为 60 分钟」"
              onChange={(event) => setChangeNote(event.target.value)}
            />
          </div>
        </div>
      </Modal>
    </>
  )
}
