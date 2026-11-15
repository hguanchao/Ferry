import { useEffect, useMemo, useRef, useState } from 'react'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { App as AntApp, Drawer, Form, Input, Modal, Popover } from 'antd'
import { FilePlus2, Plus, RotateCcw, Save, Workflow, X } from 'lucide-react'
import { appApi, pipelineApi, pipelineTemplateApi, pipelineVersionApi } from '@/api/devops'
import type {
  AppRow,
  PipelineDefinition,
  PipelineDetail,
  PipelineStageType,
  PipelineTemplateDetail,
  PipelineTemplateRow,
} from '@/api/types'
import AppSelect from '@/components/AppSelect'
import PipelineDefinitionEditor, {
  StageTypeMenu,
  type PipelineDefinitionEditorHandle,
} from '@/components/PipelineDefinitionEditor'
import TemplatePickerModal from '@/components/TemplatePickerModal'
import TemplateDeriveModal, { type DeriveResult } from '@/components/TemplateDeriveModal'
import {
  STAGE_TYPES,
  collectDefinitionIssues,
  normalizeDefinition,
  summarizeDefinitionChanges,
} from '@/lib/pipeline'
import { showError } from '@/lib/notify'

interface PipelineEditorDrawerProps {
  open: boolean
  /** create：新建流水线并生成 V1；definition：只保存定义 */
  mode: 'create' | 'definition'
  pipeline: PipelineDetail | null
  onClose: () => void
  onSaved: () => void
}

interface CreateFormValues {
  appId?: string
  name?: string
  description?: string
}

/** 从模板派生的来源记录，创建成功后随流水线落库 */
interface SourceRef {
  templateId: string
  versionNo: number
  vars: Record<string, string>
}

export default function PipelineEditorDrawer({
  open,
  mode,
  pipeline,
  onClose,
  onSaved,
}: PipelineEditorDrawerProps) {
  const { message } = AntApp.useApp()
  const queryClient = useQueryClient()
  const [form] = Form.useForm<CreateFormValues>()
  const [definition, setDefinition] = useState<PipelineDefinition>({ stages: [] })
  const [original, setOriginal] = useState<PipelineDefinition | null>(null)
  const [yamlValid, setYamlValid] = useState(true)
  const [confirmOpen, setConfirmOpen] = useState(false)
  const [changeNote, setChangeNote] = useState('')
  const [stageMenuOpen, setStageMenuOpen] = useState(false)
  const [pickerOpen, setPickerOpen] = useState(false)
  const [deriveOpen, setDeriveOpen] = useState(false)
  const [deriveTemplate, setDeriveTemplate] = useState<PipelineTemplateDetail | null>(null)
  const [deriveApp, setDeriveApp] = useState<AppRow | null>(null)
  const [sourceRef, setSourceRef] = useState<SourceRef | null>(null)
  const editorRef = useRef<PipelineDefinitionEditorHandle>(null)

  useEffect(() => {
    if (!open) {
      return
    }
    form.resetFields()
    setYamlValid(true)
    setConfirmOpen(false)
    setChangeNote('')
    setStageMenuOpen(false)
    setPickerOpen(false)
    setDeriveOpen(false)
    setSourceRef(null)
    if (mode === 'create') {
      const empty = { stages: [] }
      setDefinition(empty)
      setOriginal(null)
      return
    }
    if (pipeline) {
      const normalized = normalizeDefinition(pipeline.definition)
      setDefinition(normalized)
      setOriginal(normalized)
    }
  }, [open, mode, pipeline?.id, form])

  const issues = useMemo(() => collectDefinitionIssues(definition), [definition])
  const summary = useMemo(
    () => summarizeDefinitionChanges(original, definition),
    [original, definition],
  )

  const usedTypes = useMemo(
    () => new Set<string>(definition.stages.map((stage) => stage.type)),
    [definition.stages],
  )
  const allUsed = definition.stages.length >= STAGE_TYPES.length
  const taskCount = definition.stages.reduce((sum, stage) => sum + stage.tasks.length, 0)
  const nextVersion = (pipeline?.currentVersionNo ?? 0) + 1

  const finish = (text: string) => {
    message.success(text || '保存成功')
    setConfirmOpen(false)
    onSaved()
    onClose()
  }

  const createMutation = useMutation({
    mutationFn: (body: Record<string, unknown>) => pipelineApi.save(body),
    onSuccess: finish,
    onError: showError,
  })

  const versionMutation = useMutation({
    mutationFn: (body: { pipelineId: string; definition: PipelineDefinition; changeNote: string }) =>
      pipelineVersionApi.save(body),
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
    let values: CreateFormValues
    try {
      values = await form.validateFields()
    } catch (error) {
      const first = (error as { errorFields?: Array<{ errors?: string[] }> }).errorFields?.[0]?.errors?.[0]
      showError(first ?? '请完善流水线信息')
      return
    }
    createMutation.mutate({
      appId: values.appId,
      name: values.name,
      description: values.description,
      definition,
      ...(sourceRef
        ? {
            sourceTemplateId: sourceRef.templateId,
            sourceTemplateVersionNo: sourceRef.versionNo,
            sourceTemplateVars: sourceRef.vars,
          }
        : {}),
    })
  }

  /** 选模板 → 拉模板定义与应用信息（内置变量预填用）→ 进入变量填写 */
  const handlePicked = async (option: PipelineTemplateRow) => {
    const appId = form.getFieldValue('appId') as string | undefined
    if (!appId) {
      showError('请先选择所属应用')
      return
    }
    try {
      const [template, app] = await Promise.all([
        queryClient.fetchQuery({
          queryKey: ['pipeline-template-detail', option.id],
          queryFn: () => pipelineTemplateApi.detail(option.id),
        }),
        queryClient.fetchQuery({
          queryKey: ['app-detail', appId],
          queryFn: () => appApi.detail(appId),
        }),
      ])
      setDeriveTemplate(template)
      setDeriveApp(app)
      setPickerOpen(false)
      setDeriveOpen(true)
    } catch (error) {
      showError(error)
    }
  }

  const handleDerived = (result: DeriveResult) => {
    setDefinition(normalizeDefinition(result.definition))
    setSourceRef({ templateId: result.templateId, versionNo: result.templateVersionNo, vars: result.vars })
    setDeriveOpen(false)
    setDeriveTemplate(null)
    setDeriveApp(null)
    message.success('已按模板生成定义，可继续调整')
  }

  const submitDefinition = () => {
    if (!pipeline) {
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
    if (!pipeline) {
      return
    }
    if (!changeNote.trim()) {
      showError('请填写变更说明')
      return
    }
    versionMutation.mutate({
      pipelineId: pipeline.id,
      definition,
      changeNote: changeNote.trim(),
    })
  }

  const resetDefinition = () => {
    editorRef.current?.reset()
    if (mode === 'create') {
      form.resetFields()
      setSourceRef(null)
    }
    message.success('已重置')
  }

  const handleAddStage = (type: PipelineStageType) => {
    editorRef.current?.addStage(type)
    setStageMenuOpen(false)
  }

  const overviewExtra =
    mode === 'create' ? (
      <div className="ferry-studio__field">
        <span>流水线描述</span>
        <Form.Item name="description" noStyle>
          <Input.TextArea
            className="ferry-studio__textarea"
            rows={2}
            maxLength={500}
            showCount
            placeholder="选填，说明这条流水线负责的交付内容"
          />
        </Form.Item>
      </div>
    ) : null

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
        <Form<CreateFormValues> form={form} component={false} layout="vertical" requiredMark="optional">
          <div className="ferry-studio">
            <header className="ferry-studio__topbar">
              <div className="ferry-studio__brand">
                <span className="ferry-studio__brand-mark" aria-hidden="true">
                  <Workflow size={19} />
                </span>
                <div className="ferry-studio__brand-text">
                  <b>渡口 Ferry</b>
                  <span>FLOW / ORCHESTRATION</span>
                </div>
              </div>
              <span className="ferry-studio__divider" aria-hidden="true" />
              <div className="ferry-studio__doc">
                <div className="ferry-studio__crumb">
                  交付编排 / {mode === 'create' ? '新建流水线' : '编辑定义'}
                </div>
                <div className="ferry-studio__doc-line">
                  {mode === 'create' ? (
                    <Form.Item
                      name="name"
                      noStyle
                      rules={[{ required: true, message: '请输入流水线名称' }]}
                    >
                      <Input
                        className="ferry-studio__name"
                        maxLength={100}
                        placeholder="未命名流水线"
                        aria-label="流水线名称"
                      />
                    </Form.Item>
                  ) : (
                    <span className="ferry-studio__name is-static">{pipeline?.name ?? '未命名流水线'}</span>
                  )}
                  <span className="ferry-studio__status">
                    {mode === 'create'
                      ? '草稿'
                      : pipeline?.currentVersionNo
                        ? `V${pipeline.currentVersionNo}`
                        : '未配置'}
                  </span>
                  {mode === 'create' && sourceRef && (
                    <span className="ferry-studio__status">来自模板 V{sourceRef.versionNo}</span>
                  )}
                </div>
              </div>

              <div className="ferry-studio__app">
                <span className="ferry-studio__app-label">APPLICATION</span>
                {mode === 'create' ? (
                  <Form.Item
                    name="appId"
                    noStyle
                    rules={[{ required: true, message: '请选择所属应用' }]}
                  >
                    <AppSelect
                      placeholder="选择应用"
                      disableStopped
                      style={{ width: 220 }}
                    />
                  </Form.Item>
                ) : (
                  <span className="ferry-studio__app-static">
                    {pipeline?.appName ?? '—'}
                    <span className="ferry-studio__mono">{pipeline?.appCode ?? ''}</span>
                  </span>
                )}
              </div>

              <div className="ferry-studio__stats" aria-live="polite">
                <span>
                  <b>{definition.stages.length}</b> 阶段
                </span>
                <span className="ferry-studio__stat-dot" aria-hidden="true" />
                <span>
                  <b>{taskCount}</b> 任务
                </span>
              </div>

              <div className="ferry-studio__actions">
                {mode === 'create' && (
                  <button type="button" className="ferry-studio__btn is-ghost" onClick={() => setPickerOpen(true)}>
                    <FilePlus2 size={13} />
                    从模板创建
                  </button>
                )}
                <button type="button" className="ferry-studio__btn is-ghost" onClick={resetDefinition}>
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
                definition={definition}
                onChange={setDefinition}
                onValidityChange={setYamlValid}
                initialDefinition={mode === 'create' ? { stages: [] } : original ?? { stages: [] }}
                overviewExtra={overviewExtra}
              />
            </div>
          </div>
        </Form>
      </Drawer>

      {mode === 'create' && (
        <>
          <TemplatePickerModal
            open={pickerOpen}
            onClose={() => setPickerOpen(false)}
            onPicked={(option) => void handlePicked(option)}
          />
          <TemplateDeriveModal
            open={deriveOpen}
            template={deriveTemplate}
            app={deriveApp}
            onClose={() => {
              setDeriveOpen(false)
              setDeriveTemplate(null)
              setDeriveApp(null)
            }}
            onConfirm={handleDerived}
          />
        </>
      )}

      <Modal
        open={confirmOpen}
        width={560}
        title={pipeline ? `保存为 V${nextVersion}` : '保存定义'}
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
