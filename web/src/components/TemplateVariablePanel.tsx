import { Button, Checkbox, Input } from 'antd'
import { Plus, Trash2 } from 'lucide-react'
import type { TemplateVariable } from '@/api/types'
import { createVariable, TEMPLATE_VARIABLE_LIMIT } from '@/lib/pipeline'

interface TemplateVariablePanelProps {
  variables: TemplateVariable[]
  onChange: (next: TemplateVariable[]) => void
}

/** 占位变量编辑区：变量名/显示名/说明/默认值/必填逐行编辑，模板定义里以 {{变量名}} 引用 */
export default function TemplateVariablePanel({ variables, onChange }: TemplateVariablePanelProps) {
  const update = (index: number, patch: Partial<TemplateVariable>) => {
    onChange(variables.map((item, i) => (i === index ? { ...item, ...patch } : item)))
  }

  const remove = (index: number) => {
    onChange(variables.filter((_, i) => i !== index))
  }

  const add = () => {
    onChange([...variables, createVariable()])
  }

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
        <span>占位变量</span>
        <Button
          type="link"
          size="small"
          icon={<Plus size={14} />}
          disabled={variables.length >= TEMPLATE_VARIABLE_LIMIT}
          onClick={add}
        >
          添加变量
        </Button>
      </div>
      {variables.length === 0 ? (
        <div style={{ fontSize: 12, color: 'var(--text-3)' }}>
          暂无变量。命令里可用 {'{{变量名}}'} 引用变量，派生时替换为实际值。
        </div>
      ) : (
        <>
          <div
            style={{
              display: 'grid',
              gridTemplateColumns: '1.1fr 0.9fr 1.4fr 1.2fr 48px 32px',
              gap: 6,
              fontSize: 12,
              color: 'var(--text-3)',
            }}
          >
            <span>变量名</span>
            <span>显示名</span>
            <span>说明</span>
            <span>默认值</span>
            <span>必填</span>
            <span />
          </div>
          {variables.map((variable, index) => (
            <div
              key={index}
              style={{
                display: 'grid',
                gridTemplateColumns: '1.1fr 0.9fr 1.4fr 1.2fr 48px 32px',
                gap: 6,
                alignItems: 'center',
              }}
            >
              <Input
                size="small"
                value={variable.key}
                maxLength={50}
                placeholder="app_name"
                aria-label="变量名"
                onChange={(event) => update(index, { key: event.target.value })}
              />
              <Input
                size="small"
                value={variable.label}
                maxLength={50}
                placeholder="应用名"
                aria-label="变量显示名"
                onChange={(event) => update(index, { label: event.target.value })}
              />
              <Input
                size="small"
                value={variable.description ?? ''}
                maxLength={200}
                placeholder="用途说明"
                aria-label="变量说明"
                onChange={(event) => update(index, { description: event.target.value })}
              />
              <Input
                size="small"
                value={variable.defaultValue ?? ''}
                maxLength={500}
                placeholder="派生时未填则不替换"
                aria-label="变量默认值"
                onChange={(event) => update(index, { defaultValue: event.target.value })}
              />
              <Checkbox
                checked={variable.required}
                onChange={(event) => update(index, { required: event.target.checked })}
              />
              <Button
                type="text"
                size="small"
                danger
                aria-label="删除变量"
                icon={<Trash2 size={14} />}
                onClick={() => remove(index)}
              />
            </div>
          ))}
        </>
      )}
    </div>
  )
}
