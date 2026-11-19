import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { Button, Input, InputNumber, Popover, Select } from 'antd'
import {
  ArrowDown, Boxes, ChevronLeft, ChevronRight, CircleDot, Code2, FlaskConical,
  GitMerge, GripVertical, Layers3, Maximize2, PackageCheck, Plus, Rocket,
  ShieldCheck, Trash2, Wrench, ZoomIn, ZoomOut,
} from 'lucide-react'
import type { PipelineStageDef, PipelineTaskDef } from '@/api/types-devops'

/** 阶段类型：与后端 StageTypeEnum 对应 */
export const STAGE_TYPES = [
  { value: 1, label: '构建' },
  { value: 2, label: '单元测试' },
  { value: 3, label: '静态扫描' },
  { value: 4, label: '制品入库' },
  { value: 5, label: '部署非生产' },
  { value: 6, label: '集成测试' },
  { value: 7, label: '准入' },
  { value: 8, label: '生产发布' },
] as const

/** 各阶段类型的图标与主题色，画布卡片据此着色 */
const STAGE_ICONS: Record<number, typeof Code2> = {
  1: Code2, 2: FlaskConical, 3: ShieldCheck, 4: PackageCheck,
  5: Wrench, 6: Boxes, 7: GitMerge, 8: Rocket,
}
const STAGE_COLORS: Record<number, string> = {
  1: '#4096ff', 2: '#36cfc9', 3: '#ffc53d', 4: '#b37feb',
  5: '#69b1ff', 6: '#5cdbd3', 7: '#ffd666', 8: '#ff85c0',
}

export const EXECUTORS = [
  { value: 'jenkins', label: 'Jenkins' },
  { value: 'deployer', label: '部署器' },
  { value: 'gate', label: '门禁' },
  { value: 'release', label: '发布' },
] as const

const STAGE_LABEL = Object.fromEntries(STAGE_TYPES.map((s) => [s.value, s.label])) as Record<number, string>
const EXECUTOR_LABEL = Object.fromEntries(EXECUTORS.map((e) => [e.value, e.label])) as Record<string, string>

const CARD_W = 208
const CARD_H = 96
const GAP_X = 96
const TASK_W = 184
const TASK_H = 36
const TASK_GAP_Y = 58
const STAGE_TOP = 48
const TASK_TOP = STAGE_TOP + CARD_H + 56
// 任务节点居中对齐阶段卡片中心：卡片中心 - 任务宽度一半
const TASK_X_OFFSET = CARD_W / 2 - TASK_W / 2
const MIN_ZOOM = 0.4
const MAX_ZOOM = 1.6

type Selection = { kind: 'stage'; index: number } | { kind: 'task'; stage: number; task: number } | null

interface PipelineCanvasEditorProps {
  value: PipelineStageDef[]
  onChange: (stages: PipelineStageDef[]) => void
}

interface ColLayout {
  stage: PipelineStageDef
  index: number
  cardX: number
  tasksY: number[]
  addY: number
}

function buildLayout(stages: PipelineStageDef[]) {
  const cols: ColLayout[] = []
  let cursorX = 40
  let maxTasks = 0
  const perCol = stages.map((stage, index) => {
    const tasksY = (stage.tasks ?? []).map((_, i) => TASK_TOP + i * TASK_GAP_Y)
    maxTasks = Math.max(maxTasks, stage.tasks?.length ?? 0)
    const col: ColLayout = {
      stage, index, cardX: cursorX, tasksY,
      addY: TASK_TOP + (stage.tasks?.length ?? 0) * TASK_GAP_Y,
    }
    cols.push(col)
    cursorX += CARD_W + GAP_X
    return col
  })
  // 末尾预留一个「添加阶段」虚线框的位置
  const addStageX = cursorX
  const worldW = Math.max(cursorX + CARD_W + GAP_X, 720)
  const worldH = Math.max(TASK_TOP + (maxTasks + 1) * TASK_GAP_Y + 80, 420)
  return { perCol, worldW, worldH, addStageX }
}

export default function PipelineCanvasEditor({ value, onChange }: PipelineCanvasEditorProps) {
  const [selection, setSelection] = useState<Selection>(null)
  const [zoom, setZoom] = useState(1)
  const [pan, setPan] = useState({ x: 12, y: 8 })
  const [dragIndex, setDragIndex] = useState<number | null>(null)
  const [dropTarget, setDropTarget] = useState<{ index: number; before: boolean } | null>(null)
  const [stageMenuOpen, setStageMenuOpen] = useState(false)
  const [tailMenuOpen, setTailMenuOpen] = useState(false)
  const viewportRef = useRef<HTMLDivElement>(null)
  const panning = useRef<{ x: number; y: number; px: number; py: number } | null>(null)
  const didInitView = useRef(false)

  const stages = value ?? []
  const usedTypes = useMemo(() => new Set(stages.map((s) => s.type)), [stages])
  const layout = useMemo(() => buildLayout(stages), [stages])

  const update = useCallback((next: PipelineStageDef[]) => onChange(next), [onChange])

  // 首次渲染：按视口宽度自适应并把内容水平居中
  useEffect(() => {
    const viewport = viewportRef.current
    if (!viewport || didInitView.current) return
    const width = viewport.clientWidth
    if (!width) return
    didInitView.current = true
    const fitZoom = Math.min(1, Math.max(MIN_ZOOM, (width - 48) / layout.worldW))
    setZoom(Math.round(fitZoom * 100) / 100)
    setPan({ x: Math.max(12, (width - layout.worldW * fitZoom) / 2), y: 8 })
  }, [layout.worldW])

  // 视口尺寸变化时重新居中（如抽屉展开动画结束后）
  useEffect(() => {
    const viewport = viewportRef.current
    if (!viewport) return
    const observer = new ResizeObserver(() => {
      if (!didInitView.current) return
      const width = viewport.clientWidth
      if (!width) return
      setPan((p) => ({ ...p, x: Math.max(12, (width - layout.worldW * zoom) / 2) }))
    })
    observer.observe(viewport)
    return () => observer.disconnect()
  }, [layout.worldW, zoom])

  const addStage = (type: number) => {
    if (usedTypes.has(type)) return
    const stage: PipelineStageDef = {
      type,
      name: STAGE_LABEL[type],
      timeoutSeconds: 1800,
      tasks: [],
      outputs: type === 4 ? ['artifact'] : [],
    }
    update([...stages, stage])
    setSelection({ kind: 'stage', index: stages.length })
  }

  const removeStage = (index: number) => {
    update(stages.filter((_, i) => i !== index))
    setSelection(null)
  }

  const addTask = (stageIndex: number) => {
    const next = stages.map((s, i) => i === stageIndex
      ? { ...s, tasks: [...(s.tasks ?? []), { name: '新任务', executor: 'jenkins', scriptRef: 'script' }] }
      : s)
    update(next)
    setSelection({ kind: 'task', stage: stageIndex, task: (stages[stageIndex].tasks ?? []).length })
  }

  const removeTask = (stageIndex: number, taskIndex: number) => {
    const next = stages.map((s, i) => i === stageIndex
      ? { ...s, tasks: (s.tasks ?? []).filter((_, j) => j !== taskIndex) }
      : s)
    update(next)
    setSelection(null)
  }

  const patchStage = (index: number, patch: Partial<PipelineStageDef>) => {
    update(stages.map((s, i) => i === index ? { ...s, ...patch } : s))
  }

  const patchTask = (stageIndex: number, taskIndex: number, patch: Partial<PipelineTaskDef>) => {
    const next = stages.map((s, i) => {
      if (i !== stageIndex) return s
      const tasks = (s.tasks ?? []).map((t, j) => j === taskIndex ? { ...t, ...patch } : t)
      return { ...s, tasks }
    })
    update(next)
  }

  const reorderStage = (from: number, toIndex: number, before: boolean) => {
    if (from === toIndex) return
    const next = [...stages]
    const [moved] = next.splice(from, 1)
    let target = toIndex
    if (from < toIndex) target -= 1
    if (!before) target += 1
    next.splice(Math.max(0, Math.min(next.length, target)), 0, moved)
    update(next)
    setSelection(null)
  }

  // 鼠标滚轮缩放（以光标位置为锚点）
  useEffect(() => {
    const viewport = viewportRef.current
    if (!viewport) return
    const onWheel = (e: WheelEvent) => {
      e.preventDefault()
      const rect = viewport.getBoundingClientRect()
      const mouseX = e.clientX - rect.left
      const mouseY = e.clientY - rect.top
      setZoom((prevZoom) => {
        const nextZoom = Math.max(MIN_ZOOM, Math.min(MAX_ZOOM,
          Math.round((prevZoom - e.deltaY * 0.0015) * 100) / 100))
        if (nextZoom !== prevZoom) {
          // 保持光标下的世界坐标不变
          const worldX = (mouseX - pan.x) / prevZoom
          const worldY = (mouseY - pan.y) / prevZoom
          setPan({ x: mouseX - worldX * nextZoom, y: mouseY - worldY * nextZoom })
        }
        return nextZoom
      })
    }
    viewport.addEventListener('wheel', onWheel, { passive: false })
    return () => viewport.removeEventListener('wheel', onWheel)
  }, [pan.x, pan.y])

  const applyZoom = (delta: number) => {
    setZoom((z) => Math.max(MIN_ZOOM, Math.min(MAX_ZOOM, Math.round((z + delta) * 10) / 10)))
  }

  // 复位：按视口宽度自适应并居中
  const resetView = () => {
    const viewport = viewportRef.current
    if (!viewport) return
    const width = viewport.clientWidth
    const fitZoom = Math.min(1, Math.max(MIN_ZOOM, (width - 48) / layout.worldW))
    setZoom(Math.round(fitZoom * 100) / 100)
    setPan({ x: Math.max(12, (width - layout.worldW * fitZoom) / 2), y: 8 })
  }

  const onPointerDownCanvas = (e: React.PointerEvent) => {
    // 只处理真正落在视口 DOM 子树内的按下事件。
    // Popover 菜单虽在 React 树中位于视口内，但其内容被 portal 到 document.body；
    // React 事件仍会沿 React 树冒泡到这里，若不排除会误捕获指针，导致菜单项 click 被吞掉。
    if (!viewportRef.current?.contains(e.target as Node)) return
    if ((e.target as HTMLElement).closest(
      '.ferry-canvas__card, .ferry-canvas__task, .ferry-canvas__add-task, .ferry-canvas__add-stage, .ferry-canvas__inspector, .ferry-canvas__nav',
    )) return
    panning.current = { x: e.clientX, y: e.clientY, px: pan.x, py: pan.y }
    ;(e.currentTarget as HTMLElement).setPointerCapture?.(e.pointerId)
  }
  const onPointerMoveCanvas = (e: React.PointerEvent) => {
    if (!panning.current) return
    setPan({
      x: panning.current.px + e.clientX - panning.current.x,
      y: panning.current.py + e.clientY - panning.current.y,
    })
  }
  const onPointerUpCanvas = () => { panning.current = null }

  // SVG 连线：阶段之间实线箭头，阶段到任务虚线
  const edges = useMemo(() => {
    const list: React.ReactNode[] = []
    const sx = (col: ColLayout) => col.cardX + CARD_W
    const sy = STAGE_TOP + CARD_H / 2
    for (let i = 0; i < layout.perCol.length - 1; i++) {
      const a = layout.perCol[i]
      const b = layout.perCol[i + 1]
      const x1 = sx(a)
      const x2 = b.cardX
      list.push(
        <g key={`stage-${i}`}>
          <path d={`M ${x1} ${sy} L ${x2} ${sy}`} className="ferry-canvas__edge" markerEnd="url(#arrow)" />
          <path d={`M ${x1} ${sy} L ${x2} ${sy}`} className="ferry-canvas__edge-flow" />
          <circle r="3.5" className="ferry-canvas__particle" style={{ offsetPath: `path('M ${x1} ${sy} L ${x2} ${sy}')` }} />
        </g>,
      )
    }
    layout.perCol.forEach((col) => {
      // 垂直连线统一走阶段/任务的中轴 x
      const centerX = col.cardX + CARD_W / 2
      const taskLeftX = col.cardX + TASK_X_OFFSET
      const taskMidX = taskLeftX + TASK_W / 2
      const taskTopY = col.tasksY[0]
      if (col.tasksY.length) {
        list.push(
          <path key={`task-root-${col.index}`}
            d={`M ${centerX} ${STAGE_TOP + CARD_H} L ${taskMidX} ${taskTopY}`}
            className="ferry-canvas__edge-task" />,
        )
        // 任务之间：上一任务底部中点 -> 下一任务顶部中点（垂直虚线）
        for (let i = 0; i < col.tasksY.length - 1; i++) {
          const y1 = col.tasksY[i] + TASK_H
          const y2 = col.tasksY[i + 1]
          list.push(
            <path key={`task-link-${col.index}-${i}`}
              d={`M ${taskMidX} ${y1} L ${taskMidX} ${y2}`}
              className="ferry-canvas__edge-task" />,
          )
        }
        // 最后一个任务 -> 添加任务按钮（垂直虚线）
        const lastY = col.tasksY[col.tasksY.length - 1] + TASK_H
        list.push(
          <path key={`task-to-add-${col.index}`}
            d={`M ${taskMidX} ${lastY} L ${taskMidX} ${col.addY}`}
            className="ferry-canvas__edge-dashed" />,
        )
      } else {
        // 无任务：阶段卡片底部 -> 添加任务按钮（垂直虚线）
        list.push(
          <path key={`stage-to-addtask-${col.index}`}
            d={`M ${centerX} ${STAGE_TOP + CARD_H} L ${taskMidX} ${col.addY}`}
            className="ferry-canvas__edge-dashed" />,
        )
      }
    })

    // 最后一个阶段 -> 末尾「添加阶段」虚线框
    if (layout.perCol.length) {
      const last = layout.perCol[layout.perCol.length - 1]
      const x1 = last.cardX + CARD_W
      const x2 = layout.addStageX
      list.push(
        <g key="stage-to-add">
          <path d={`M ${x1} ${sy} L ${x2} ${sy}`} className="ferry-canvas__edge-dashed" markerEnd="url(#arrowMuted)" />
        </g>,
      )
    } else {
      // 空画布：从起点画一段虚线到添加框
      list.push(
        <g key="empty-to-add">
          <path d={`M 40 ${sy} L ${layout.addStageX} ${sy}`} className="ferry-canvas__edge-dashed" markerEnd="url(#arrowMuted)" />
        </g>,
      )
    }
    return list
  }, [layout])

  const selectedStage = selection?.kind === 'stage' ? stages[selection.index] : null
  const selectedTask = selection?.kind === 'task'
    ? stages[selection.stage]?.tasks?.[selection.task]
    : null

  // 阶段选择菜单：顶栏添加、画布末尾虚线框共用
  const stageMenu = (
    <div className="ferry-canvas__stage-menu">
      {STAGE_TYPES.map((t) => {
        const used = usedTypes.has(t.value)
        const Icon = STAGE_ICONS[t.value]
        return (
          <button key={t.value} type="button" disabled={used}
            onClick={() => { addStage(t.value); setStageMenuOpen(false); setTailMenuOpen(false) }}>
            <span className="ferry-canvas__stage-menu-icon" style={{ color: STAGE_COLORS[t.value] }}><Icon size={14} /></span>
            <span>{t.label}</span>
            {used && <span className="ferry-canvas__stage-menu-used">已添加</span>}
          </button>
        )
      })}
    </div>
  )

  return (
    <div className="ferry-canvas">
      {/* 顶部工具条 */}
      <div className="ferry-canvas__toolbar">
        <span className="ferry-canvas__brand"><Layers3 size={15} /> 流水线编排画布</span>
        <span className="ferry-canvas__hint">滚轮缩放 · 拖拽空白处平移 · 点击卡片/任务编辑属性</span>
        <div className="ferry-canvas__tools">
          <Popover content={stageMenu} trigger="click" open={stageMenuOpen} onOpenChange={setStageMenuOpen} placement="bottomRight">
            <Button size="small" type="primary" ghost icon={<Plus size={14} />}>添加阶段</Button>
          </Popover>
          <span className="ferry-canvas__zoom">
            <Button type="text" size="small" icon={<ZoomOut size={14} />} onClick={() => applyZoom(-0.1)} />
            <span>{Math.round(zoom * 100)}%</span>
            <Button type="text" size="small" icon={<ZoomIn size={14} />} onClick={() => applyZoom(0.1)} />
            <Button type="text" size="small" icon={<Maximize2 size={14} />} title="适应并居中" onClick={resetView} />
          </span>
        </div>
      </div>

      <div className="ferry-canvas__body">
        {/* 画布视口 */}
        <div className="ferry-canvas__viewport" ref={viewportRef}
          onPointerDown={onPointerDownCanvas}
          onPointerMove={onPointerMoveCanvas}
          onPointerUp={onPointerUpCanvas}
          onPointerCancel={onPointerUpCanvas}>
          <div className="ferry-canvas__world"
            style={{ width: layout.worldW, height: layout.worldH,
              transform: `translate(${pan.x}px, ${pan.y}px) scale(${zoom})` }}>
            <svg className="ferry-canvas__edges-svg" width={layout.worldW} height={layout.worldH}>
              <defs>
                <marker id="arrow" markerWidth="9" markerHeight="9" refX="7" refY="3" orient="auto" markerUnits="strokeWidth">
                  <path d="M0,0 L0,6 L7,3 z" fill="#69b1ff" />
                </marker>
                <marker id="arrowMuted" markerWidth="9" markerHeight="9" refX="7" refY="3" orient="auto" markerUnits="strokeWidth">
                  <path d="M0,0 L0,6 L7,3 z" fill="#7a9cc7" />
                </marker>
                <linearGradient id="edgeGrad" gradientUnits="userSpaceOnUse" x1="0" y1="0" x2="1200" y2="0">
                  <stop offset="0%" stopColor="#4096ff" />
                  <stop offset="100%" stopColor="#69b1ff" />
                </linearGradient>
              </defs>
              {edges}
            </svg>

            {stages.length === 0 && (
              <div className="ferry-canvas__empty">
                <Layers3 size={28} />
                <p>暂无阶段，从右上角「添加阶段」开始编排</p>
              </div>
            )}

            {layout.perCol.map((col) => {
              const stageSelected = selection?.kind === 'stage' && selection.index === col.index
              const isDrop = dropTarget?.index === col.index
              const StageIcon = STAGE_ICONS[col.stage.type] ?? CircleDot
              const accent = STAGE_COLORS[col.stage.type] ?? '#4096ff'
              return (
                // key 必须稳定：若掺入选中状态，切换选中会重建卡片并重放入场动画，导致插入时卡片闪烁/空白
                <div key={col.index}>
                  <div
                    className={`ferry-canvas__card${stageSelected ? ' is-selected' : ''}${isDrop ? (dropTarget?.before ? ' drop-before' : ' drop-after') : ''}${dragIndex === col.index ? ' is-dragging' : ''}`}
                    style={{ transform: `translate(${col.cardX}px, ${STAGE_TOP}px)`,
                      '--stage-accent': accent, animationDelay: `${Math.min(col.index, 2) * 60}ms` } as React.CSSProperties}
                    draggable
                    onDragStart={() => setDragIndex(col.index)}
                    onDragEnd={() => { setDragIndex(null); setDropTarget(null) }}
                    onDragOver={(e) => {
                      if (dragIndex === null || dragIndex === col.index) return
                      e.preventDefault()
                      const rect = (e.currentTarget as HTMLElement).getBoundingClientRect()
                      setDropTarget({ index: col.index, before: e.clientX < rect.left + rect.width / 2 })
                    }}
                    onDrop={(e) => {
                      e.preventDefault()
                      if (dragIndex !== null && dropTarget) reorderStage(dragIndex, dropTarget.index, dropTarget.before)
                      setDragIndex(null); setDropTarget(null)
                    }}
                    onClick={() => setSelection({ kind: 'stage', index: col.index })}>
                    <span className="ferry-canvas__card-accent" />
                    <div className="ferry-canvas__card-head">
                      <GripVertical size={14} className="ferry-canvas__grip" />
                      <span className="ferry-canvas__card-seq">S{col.index + 1}</span>
                      <span className="ferry-canvas__card-icon"><StageIcon size={14} /></span>
                      <span className="ferry-canvas__card-type">{STAGE_LABEL[col.stage.type] ?? col.stage.type}</span>
                    </div>
                    <div className="ferry-canvas__card-name">{col.stage.name || '未命名阶段'}</div>
                    <div className="ferry-canvas__card-meta">
                      <span className="ferry-canvas__meta-item">{col.stage.timeoutSeconds ?? 1800}s</span>
                      <span className="ferry-canvas__meta-item">{col.stage.tasks?.length ?? 0} 任务</span>
                    </div>
                    <Button type="text" size="small" danger className="ferry-canvas__card-del"
                      icon={<Trash2 size={13} />}
                      onClick={(e) => { e.stopPropagation(); removeStage(col.index) }} />
                  </div>

                  {(col.stage.tasks ?? []).map((task, ti) => {
                    const taskSelected = selection?.kind === 'task' && selection.stage === col.index && selection.task === ti
                    return (
                      <div key={`t-${col.index}-${ti}`}
                        className={`ferry-canvas__task${taskSelected ? ' is-selected' : ''}`}
                        style={{ transform: `translate(${col.cardX + TASK_X_OFFSET}px, ${col.tasksY[ti]}px)`,
                          animationDelay: `${col.index * 90 + ti * 60 + 120}ms` } as React.CSSProperties}
                        onClick={() => setSelection({ kind: 'task', stage: col.index, task: ti })}>
                        <span className="ferry-canvas__task-dot" />
                        <span className="ferry-canvas__task-name">{task.name || '未命名任务'}</span>
                        <span className="ferry-canvas__task-exec">{EXECUTOR_LABEL[task.executor] ?? task.executor}</span>
                      </div>
                    )
                  })}

                  <button type="button" className="ferry-canvas__add-task"
                    style={{ transform: `translate(${col.cardX + TASK_X_OFFSET}px, ${col.addY}px)` }}
                    onClick={() => addTask(col.index)}>
                    <Plus size={13} /> 添加任务
                  </button>
                </div>
              )
            })}

            {/* 末尾虚线框：添加阶段 */}
            <Popover content={stageMenu} trigger="click" open={tailMenuOpen} onOpenChange={setTailMenuOpen} placement="bottom">
              <button type="button" className="ferry-canvas__add-stage"
                style={{ transform: `translate(${layout.addStageX}px, ${STAGE_TOP}px)` }}>
                <Plus size={20} />
                <span>添加阶段</span>
              </button>
            </Popover>
          </div>
          <div className="ferry-canvas__nav">
            <Button type="text" size="small" icon={<ChevronLeft size={14} />} onClick={() => setPan((p) => ({ ...p, x: p.x + 120 }))} />
            <ArrowDown size={12} style={{ transform: 'rotate(90deg)', color: 'var(--text-4)' }} />
            <Button type="text" size="small" icon={<ChevronRight size={14} />} onClick={() => setPan((p) => ({ ...p, x: p.x - 120 }))} />
          </div>
        </div>

        {/* 检查器 */}
        <aside className="ferry-canvas__inspector">
          {!selection && (
            <div className="ferry-canvas__inspector-empty">
              <Layers3 size={22} />
              <p>点击画布中的阶段卡片或任务节点查看属性</p>
            </div>
          )}
          {selectedStage && selection?.kind === 'stage' && (
            <div className="ferry-canvas__inspector-body">
              <div className="ferry-canvas__inspector-title">阶段属性 · S{selection.index + 1}</div>
              <label className="ferry-canvas__field">
                <span>阶段类型</span>
                <Select value={selectedStage.type} style={{ width: '100%' }}
                  options={STAGE_TYPES.filter((t) => t.value === selectedStage.type || !usedTypes.has(t.value))
                    .map((t) => ({ value: t.value, label: t.label }))}
                  onChange={(v) => patchStage(selection.index, { type: v })} />
              </label>
              <label className="ferry-canvas__field">
                <span>阶段名</span>
                <Input value={selectedStage.name} maxLength={40}
                  onChange={(e) => patchStage(selection.index, { name: e.target.value })} />
              </label>
              <label className="ferry-canvas__field">
                <span>超时（秒，60–86400）</span>
                <InputNumber min={60} max={86400} style={{ width: '100%' }} value={selectedStage.timeoutSeconds ?? 1800}
                  onChange={(v) => patchStage(selection.index, { timeoutSeconds: v ?? 1800 })} />
              </label>
              <label className="ferry-canvas__field">
                <span>产出物（逗号分隔）</span>
                <Input value={(selectedStage.outputs ?? []).join(',')} placeholder="例如 artifact"
                  onChange={(e) => patchStage(selection.index, {
                    outputs: e.target.value.split(',').map((s) => s.trim()).filter(Boolean),
                  })} />
              </label>
            </div>
          )}
          {selectedTask && selection?.kind === 'task' && (
            <div className="ferry-canvas__inspector-body">
              <div className="ferry-canvas__inspector-title">任务属性</div>
              <label className="ferry-canvas__field">
                <span>任务名</span>
                <Input value={selectedTask.name} maxLength={40}
                  onChange={(e) => patchTask(selection.stage, selection.task, { name: e.target.value })} />
              </label>
              <label className="ferry-canvas__field">
                <span>执行器</span>
                <Select value={selectedTask.executor} style={{ width: '100%' }}
                  options={EXECUTORS.map((e) => ({ value: e.value, label: e.label }))}
                  onChange={(v) => patchTask(selection.stage, selection.task, { executor: v })} />
              </label>
              <label className="ferry-canvas__field">
                <span>脚本引用</span>
                <Input value={selectedTask.scriptRef ?? ''} placeholder="例如 build / deploy"
                  onChange={(e) => patchTask(selection.stage, selection.task, { scriptRef: e.target.value })} />
              </label>
              <Button danger block icon={<Trash2 size={14} />}
                onClick={() => removeTask(selection.stage, selection.task)}>删除任务</Button>
            </div>
          )}
        </aside>
      </div>
    </div>
  )
}
