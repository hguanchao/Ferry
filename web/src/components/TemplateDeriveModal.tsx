import { useEffect, useMemo } from 'react'
import { App as AntApp, Alert, Form, Input, Modal } from 'antd'
import type { AppRow, PipelineDefinition, PipelineTemplateDetail } from '@/api/types'
import { prefillVariableValues, replaceVariables } from '@/lib/pipeline'

export interface DeriveResult {
  templateId: string
  templateName: string
  templateVersionNo: number
  /** 变量取值，随流水线落库供后续合并复用 */
  vars: Record<string, string>
  /** 占位变量替换后的定义，派生为独立副本 */
  definition: PipelineDefinition
}

interface TemplateDeriveModalProps {
  open: boolean
  template: PipelineTemplateDetail | null
  /** 所选应用详情，内置变量据此预填 */
  app: AppRow | null
  onClose: () => void
  onConfirm: (result: DeriveResult) => void
}

interface VariableFormValues {
  [key: string]: string | undefined
}

/**
 * 模板派生的变量填写弹窗：逐条列出模板声明的占位变量，
 * 内置变量按应用信息预填，其余取默认值；确认后完成替换。
 */
export default function TemplateDeriveModal({ open, template, app, onClose, onConfirm }: TemplateDeriveModalProps) {
  const { modal } = AntApp.useApp()
  const [form] = Form.useForm<VariableFormValues>()

  const variables = useMemo(() => template?.definition?.variables ?? [], [template?.definition])

  useEffect(() => {
    if (!open) {
      return
    }
    form.setFieldsValue(prefillVariableValues(variables, app, null))
  }, [open, variables, app, form])

  const buildResult = (
    vars: Record<string, string>,
    definition: PipelineDefinition,
  ): DeriveResult => ({
    templateId: template?.id ?? '',
    templateName: template?.name ?? '',
    templateVersionNo: template?.currentVersionNo ?? 0,
    vars,
    definition,
  })

  const handleConfirm = async () => {
    const current = template?.definition
    if (!current || !template?.currentVersionNo) {
      return
    }
    const values = await form.validateFields()
    const vars: Record<string, string> = {}
    for (const variable of variables) {
      vars[variable.key] = (values[variable.key] ?? '') as string
    }
    const { definition, missing } = replaceVariables(current, vars)
    if (missing.length > 0) {
      // 可选变量允许留空，保留占位符原文，用户可在画布里继续改
      modal.confirm({
        title: '有未填写的可选变量',
        content: `变量 {{${missing.join('}}、{{')}}} 未填写，替换结果将保留占位符原文，可在画布中继续修改。`,
        okText: '继续创建',
        cancelText: '返回修改',
        onOk: () => onConfirm(buildResult(vars, definition)),
      })
      return
    }
    onConfirm(buildResult(vars, definition))
  }

  return (
    <Modal
      open={open}
      width={620}
      title={`填写模板变量 · ${template?.name ?? ''}`}
      okText="替换并载入画布"
      cancelText="取消"
      onOk={() => void handleConfirm()}
      onCancel={onClose}
      destroyOnHidden
    >
      <Alert
        type="info"
        showIcon
        style={{ marginBottom: 12 }}
        message="替换结果会生成独立副本并载入画布，可继续调整后再创建。"
      />
      {variables.length === 0 ? (
        <div style={{ fontSize: 13, color: 'var(--text-3)' }}>该模板没有声明占位变量，直接按模板定义创建。</div>
      ) : (
        <Form<VariableFormValues> form={form} layout="vertical" requiredMark="optional">
          {variables.map((variable) => (
            <Form.Item
              key={variable.key}
              name={variable.key}
              label={`${variable.label}（{{${variable.key}}}）`}
              rules={
                variable.required
                  ? [{ required: true, message: `请填写${variable.label}` }]
                  : undefined
              }
              extra={variable.description || undefined}
            >
              <Input maxLength={500} placeholder={variable.required ? '必填' : '选填'} />
            </Form.Item>
          ))}
        </Form>
      )}
    </Modal>
  )
}
