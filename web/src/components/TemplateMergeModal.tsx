import { useEffect, useMemo, useState } from 'react'
import { useMutation, useQuery } from '@tanstack/react-query'
import { App as AntApp, Alert, Button, Form, Input, Modal, Steps } from 'antd'
import { pipelineApi, pipelineTemplateApi, pipelineVersionApi } from '@/api/devops'
import type { PipelineDetail } from '@/api/types'
import { showError } from '@/lib/notify'
import { useAuthStore } from '@/stores/auth'
import { definitionToYaml, prefillVariableValues, replaceVariables } from '@/lib/pipeline'
import { diffLines } from '@/lib/diff'
import { DiffPane } from '@/components/PipelineVersionPanel'

interface TemplateMergeModalProps {
  open: boolean
  /** 流水线详情，需含定义与派生时的变量取值 */
  pipeline: PipelineDetail | null
  onClose: () => void
  /** 合并或忽略完成后通知列表刷新 */
  onChanged: () => void
}

interface VariableFormValues {
  [key: string]: string | undefined
}

/**
 * 「有新版本模板」的合并弹窗：先确认变量取值（预填派生时存储的值，模板新增变量需补填），
 * 再预览模板最新定义与流水线当前定义的差异，可选择合并或忽略。
 */
export default function TemplateMergeModal({ open, pipeline, onClose, onChanged }: TemplateMergeModalProps) {
  const { message, modal } = AntApp.useApp()
  const can = useAuthStore((state) => state.can)
  const [form] = Form.useForm<VariableFormValues>()
  const [step, setStep] = useState<'vars' | 'diff'>('vars')
  const [values, setValues] = useState<Record<string, string>>({})

  const templateId = pipeline?.sourceTemplateId ?? ''

  const templateQuery = useQuery({
    queryKey: ['pipeline-template-detail', templateId],
    queryFn: () => pipelineTemplateApi.detail(templateId),
    enabled: open && Boolean(templateId),
  })

  const template = templateQuery.data ?? null
  const variables = template?.definition?.variables ?? []

  useEffect(() => {
    if (!open) {
      setStep('vars')
      setValues({})
      return
    }
  }, [open])

  // 模板详情与流水线详情都就绪后初始化变量取值：派生时存的值优先，模板新增变量留空待补填
  useEffect(() => {
    if (!open || !template) {
      return
    }
    const prefilled = prefillVariableValues(variables, null, pipeline?.sourceTemplateVars ?? null)
    setValues(prefilled)
    form.setFieldsValue(prefilled)
  }, [open, template, form])

  const rendered = useMemo(() => {
    if (!template?.definition || !pipeline?.definition) {
      return null
    }
    return replaceVariables(template.definition, values).definition
  }, [template, pipeline, values])

  const diffRows = useMemo(() => {
    if (!rendered || !pipeline?.definition) {
      return []
    }
    return diffLines(definitionToYaml(pipeline.definition), definitionToYaml(rendered))
  }, [rendered, pipeline])

  const finish = (text: string) => {
    message.success(text || '操作成功')
    onChanged()
    onClose()
  }

  const mergeMutation = useMutation({
    mutationFn: async () => {
      if (!pipeline || !template?.currentVersionNo || !rendered) {
        throw new Error('缺少合并所需数据')
      }
      // 合并 = 以替换后的模板最新定义生成流水线新版本，再推进来源版本记录
      await pipelineVersionApi.save({
        pipelineId: pipeline.id,
        definition: rendered,
        changeNote: `合并自模板「${template.name}」V${template.currentVersionNo}`,
      })
      return pipelineApi.templateAck({ pipelineId: pipeline.id, merged: true })
    },
    onSuccess: (text) => finish(text || '合并成功'),
    onError: showError,
  })

  const ignoreMutation = useMutation({
    mutationFn: () => {
      if (!pipeline) {
        throw new Error('缺少流水线信息')
      }
      return pipelineApi.templateAck({ pipelineId: pipeline.id, merged: false })
    },
    onSuccess: (text) => finish(text || '已忽略'),
    onError: showError,
  })

  const confirmIgnore = () => {
    modal.confirm({
      title: '忽略模板新版本？',
      content: '忽略后不再提示当前模板版本的更新；模板再发布更新版本时会重新提示。',
      okText: '忽略',
      cancelText: '取消',
      onOk: () => ignoreMutation.mutateAsync(),
    })
  }

  const canMerge = can('devops:pipeline:version:save')
  const canIgnore = can('devops:pipeline:templateAck')

  const handleSubmitVars = async () => {
    if (variables.length === 0) {
      setStep('diff')
      return
    }
    await form.validateFields()
    setValues(form.getFieldsValue() as Record<string, string>)
    setStep('diff')
  }

  return (
    <Modal
      open={open}
      width={1080}
      title={`模板更新 · ${template?.name ?? pipeline?.sourceTemplateName ?? ''}`}
      footer={
        step === 'vars' ? (
          <>
            <Button onClick={onClose}>取消</Button>
            <Button type="primary" onClick={() => void handleSubmitVars()}>预览差异</Button>
          </>
        ) : (
          <>
            <Button onClick={() => setStep('vars')}>上一步</Button>
            {canIgnore && (
              <Button loading={ignoreMutation.isPending} onClick={confirmIgnore}>
                忽略
              </Button>
            )}
            <Button
              type="primary"
              disabled={!canMerge}
              loading={mergeMutation.isPending}
              onClick={() => mergeMutation.mutate()}
            >
              合并并生成新版本
            </Button>
          </>
        )
      }
      onCancel={onClose}
      destroyOnHidden
    >
      <Steps
        size="small"
        style={{ marginBottom: 16 }}
        current={step === 'vars' ? 0 : 1}
        items={[{ title: '确认变量取值' }, { title: '预览差异' }]}
      />
      {step === 'vars' ? (
        <>
          <Alert
            type="info"
            showIcon
            style={{ marginBottom: 12 }}
            message="模板已发布新版本。确认变量取值后预览差异，合并会生成流水线新版本，不会自动变更当前定义。"
          />
          {variables.length === 0 ? (
            <div style={{ fontSize: 13, color: 'var(--text-3)' }}>该模板没有声明占位变量。</div>
          ) : (
            <Form<VariableFormValues> form={form} layout="vertical" requiredMark="optional">
              {variables.map((variable) => (
                <Form.Item
                  key={variable.key}
                  name={variable.key}
                  label={`${variable.label}（{{${variable.key}}}）`}
                  rules={variable.required ? [{ required: true, message: `请填写${variable.label}` }] : undefined}
                  extra={variable.description || undefined}
                >
                  <Input maxLength={500} />
                </Form.Item>
              ))}
            </Form>
          )}
        </>
      ) : (
        <>
          {!canMerge && (
            <Alert
              type="warning"
              showIcon
              style={{ marginBottom: 12 }}
              message="你没有保存流水线版本的权限，只能查看差异或忽略提示。"
            />
          )}
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12 }}>
            <DiffPane rows={diffRows} side="left" title="流水线当前定义" />
            <DiffPane rows={diffRows} side="right" title={template?.currentVersionNo ? `模板 V${template.currentVersionNo}` : '模板最新定义'} />
          </div>
        </>
      )}
    </Modal>
  )
}
