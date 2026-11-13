import {
  forwardRef,
  useCallback,
  useEffect,
  useImperativeHandle,
  useMemo,
  useRef,
  useState,
  type MouseEvent as ReactMouseEvent,
  type PointerEvent as ReactPointerEvent,
  type ReactNode,
} from 'react'
import { App as AntApp, Button, Input, InputNumber, Popover, Switch, type InputRef } from 'antd'
import {
  ArrowDown,
  ArrowUp,
  ChevronLeft,
  Copy,
  FileCode2,
  FileJson2,
  GripVertical,
  Layers3,
  LayoutDashboard,
  Maximize2,
  MousePointer2,
  PanelRightClose,
  PanelRightOpen,
  Plus,
  Trash2,
  X,
  ZoomIn,
  ZoomOut,
} from 'lucide-react'
import type { PipelineDefinition, PipelineStage, PipelineStageType, PipelineTask } from '@/api/types'
import {
  STAGE_TYPES,
  collectDefinitionIssues,
  createStage,
  createTask,
  definitionToYaml,
  stageTypeLabel,
  yamlToDefinition,
} from '@/lib/pipeline'
import { showError } from '@/lib/notify'

const STAGE_W = 232
const STAGE_H = 158
const TASK_W = 202
const TASK_H = 44
const TASK_GAP = 30
const COL_W = 270
const COL_GAP = 94
const CANVAS_PAD = 48
const STAGE_TOP = 142
const TASK_TOP_GAP = 70
const CHAIN_TOP = STAGE_TOP + STAGE_H + TASK_TOP_GAP
const ADD_SLOT_H = 50
const MIN_ZOOM = 0.3
// 上限取整倍数而不是 1.8：世界层靠 transform 缩放，只有整倍比例下笔画落点才回到整设备像素
const MAX_ZOOM = 2

/**
 * 平移吸附到设备像素。
 *
 * 世界层靠 transform 缩放，节点坐标都是整数；平移只要落在整设备像素，缩放为整数倍时
 * 文字笔画的落点才回到整像素，不会被灰度抗锯齿糊掉一层。
 */
const snapToDevicePixel = (value: number) => {
  const dpr = window.devicePixelRatio || 1
  return Math.round(value * dpr) / dpr
}

/** 缩放落在 1/dpr 整数倍附近时吸附过去：只有这些比例下笔画落点才真正回到整设备像素，其余保持连续可调 */
const snapZoom = (value: number) => {
  const dpr = window.devicePixelRatio || 1
  const nearest = Math.round(value * dpr) / dpr
  return Math.abs(value - nearest) < 0.01 ? nearest : value
}

interface StageMenuProps {
  usedTypes: Set<string>
  onPick: (type: PipelineStageType) => void
}

/** 阶段类型菜单：顶栏、画布空态与阶段卡片共用同一份内容 */
export function StageTypeMenu({ usedTypes, onPick }: StageMenuProps) {
  return (
    <div className="ferry-studio__menu" role="menu" aria-label="添加阶段">
      <div className="ferry-studio__menu-head">
        添加阶段
        <span>阶段类型需唯一 · 顺序可拖动调整</span>
      </div>
      <div className="ferry-studio__menu-grid">
        {STAGE_TYPES.map((type) => {
          const used = usedTypes.has(type.value)
          return (
            <button
              key={type.value}
              type="button"
              role="menuitem"
              className="ferry-studio__menu-item"
              disabled={used}
              title={type.desc}
              onClick={() => onPick(type.value)}
            >
              <span className="ferry-studio__menu-main">
                <span className="ferry-studio__menu-code">{type.value}</span>
                <span className="ferry-studio__menu-label">{type.label}</span>
              </span>
              {used && <span className="ferry-studio__menu-used">已添加</span>}
            </button>
          )
        })}
      </div>
    </div>
  )
}

type Selection =
  | { kind: 'stage'; type: PipelineStageType }
  | { kind: 'task'; type: PipelineStageType; taskIndex: number }

type InspectorTab = 'overview' | 'props' | 'yaml' | 'json'

interface Column {
  stage: PipelineStage
  index: number
  cardX: number
  taskX: number
  addY: number
  taskYs: number[]
}

interface ViewState {
  zoom: number
  panX: number
  panY: number
}

export interface PipelineDefinitionEditorHandle {
  addStage: (type: PipelineStageType) => void
  reset: () => void
  showOverview: () => void
}

interface PipelineDefinitionEditorProps {
  definition: PipelineDefinition
  onChange: (definition: PipelineDefinition) => void
  /** YAML 页签存在语法错误时置 false，保存按钮据此禁用 */
  onValidityChange: (valid: boolean) => void
  /** 重置时恢复的定义 */
  initialDefinition: PipelineDefinition
  /** 概览页签里的补充内容（新建时放基础信息表单） */
  overviewExtra?: ReactNode
}

/**
 * 流水线定义画布编辑器
 *
 * 阶段以实线单向链表从左向右排列，任务以虚线链挂在阶段下方；属性编辑统一收进右侧检查器，
 * 画布只承担结构与选中。表单与 YAML、JSON 共用同一份 definition，切页签不复制状态。
 */
const PipelineDefinitionEditor = forwardRef<
  PipelineDefinitionEditorHandle,
  PipelineDefinitionEditorProps
>(function PipelineDefinitionEditor(
  { definition, onChange, onValidityChange, initialDefinition, overviewExtra },
  ref,
) {
  const { message } = AntApp.useApp()
  const [selection, setSelection] = useState<Selection | null>(null)
  const [tab, setTab] = useState<InspectorTab>('overview')
  const [inspectorOpen, setInspectorOpen] = useState(() => window.innerWidth > 1120)
  const [view, setView] = useState<ViewState>({ zoom: 0.8, panX: 0, panY: 0 })
  const [draggingType, setDraggingType] = useState<string | null>(null)
  const [dropTarget, setDropTarget] = useState<{ type: string; before: boolean } | null>(null)
  const [slotMenuType, setSlotMenuType] = useState<string | null>(null)
  const [yamlText, setYamlText] = useState('')
  const [yamlError, setYamlError] = useState<string | null>(null)
  const [artifactInput, setArtifactInput] = useState('')
  const [panning, setPanning] = useState(false)

  const viewportRef = useRef<HTMLDivElement>(null)
  const worldRef = useRef<HTMLDivElement>(null)
  const gridRef = useRef<HTMLDivElement>(null)
  const inspectorBodyRef = useRef<HTMLDivElement>(null)
  const artifactRef = useRef<InputRef>(null)
  const taskNameRef = useRef<InputRef>(null)
  const viewRef = useRef(view)
  const layoutRef = useRef<{ width: number; height: number; cols: Column[]; addStage: { x: number; y: number } | null }>({
    width: 0,
    height: 0,
    cols: [],
    addStage: null,
  })
  const draggingRef = useRef<string | null>(null)
  const dropTargetRef = useRef<{ type: string; before: boolean } | null>(null)
  const userAdjusted = useRef(false)
  const panStart = useRef<{ x: number; y: number; panX: number; panY: number } | null>(null)
  const panMoved = useRef(false)

  viewRef.current = view

  const usedTypes = useMemo(
    () => new Set<string>(definition.stages.map((stage) => stage.type)),
    [definition.stages],
  )
  const issues = useMemo(() => collectDefinitionIssues(definition), [definition])

  /** YAML 解析成功后定义可能删掉当前选中项，这里把选中收敛到仍然存在的位置 */
  const activeSelection = useMemo<Selection | null>(() => {
    if (!selection) {
      return null
    }
    const stage = definition.stages.find((item) => item.type === selection.type)
    if (!stage) {
      return null
    }
    if (selection.kind === 'task' && selection.taskIndex >= stage.tasks.length) {
      return { kind: 'stage', type: selection.type }
    }
    return selection
  }, [selection, definition.stages])

  const selectedStage = activeSelection
    ? definition.stages.find((stage) => stage.type === activeSelection.type) ?? null
    : null
  // 概览只切换视图，保留节点选择才能返回原来的属性编辑上下文。
  const activeTab = tab === 'props' && !selectedStage ? 'overview' : tab

  const layout = useMemo(() => {
    const canAddStage = definition.stages.length > 0 && definition.stages.length < STAGE_TYPES.length
    let maxBottom = 0
    const cols: Column[] = definition.stages.map((stage, index) => {
      const colX = CANVAS_PAD + index * (COL_W + COL_GAP)
      const cardX = colX + (COL_W - STAGE_W) / 2
      const taskX = cardX + (STAGE_W - TASK_W) / 2
      const taskYs = stage.tasks.map((_, taskIndex) => CHAIN_TOP + taskIndex * (TASK_H + TASK_GAP))
      const addY = CHAIN_TOP + stage.tasks.length * (TASK_H + TASK_GAP)
      maxBottom = Math.max(maxBottom, addY + ADD_SLOT_H)
      return { stage, index, cardX, taskX, addY, taskYs }
    })
    const columnCount = definition.stages.length + (canAddStage ? 1 : 0)
    return {
      width: Math.max(CANVAS_PAD * 2 + columnCount * COL_W + Math.max(0, columnCount - 1) * COL_GAP, 560),
      height: Math.max(maxBottom + 84, 560),
      cols,
      chainY: STAGE_TOP + STAGE_H / 2,
      addStage: canAddStage
        ? {
            x: CANVAS_PAD + definition.stages.length * (COL_W + COL_GAP) + (COL_W - STAGE_W) / 2,
            y: STAGE_TOP,
          }
        : null,
    }
  }, [definition.stages])

  layoutRef.current = layout

  const applyView = useCallback((next: ViewState) => {
    setView({ ...next, panX: snapToDevicePixel(next.panX), panY: snapToDevicePixel(next.panY) })
  }, [])

  const fitView = useCallback(() => {
    const viewport = viewportRef.current
    const bounds = layoutRef.current
    if (!viewport) {
      return
    }
    const rect = viewport.getBoundingClientRect()
    if (rect.width < 20 || rect.height < 20) {
      return
    }
    const zoom = snapZoom(
      clamp(
        Math.min((rect.width - 124) / bounds.width, (rect.height - 116) / bounds.height, 1),
        MIN_ZOOM,
        1,
      ),
    )
    userAdjusted.current = false
    applyView({
      zoom,
      panX: (rect.width - bounds.width * zoom) / 2,
      panY: (rect.height - bounds.height * zoom) / 2,
    })
  }, [applyView])

  const zoomAt = useCallback(
    (screenX: number, screenY: number, nextZoom: number) => {
      const current = viewRef.current
      const zoom = clamp(nextZoom, MIN_ZOOM, MAX_ZOOM)
      const worldX = (screenX - current.panX) / current.zoom
      const worldY = (screenY - current.panY) / current.zoom
      userAdjusted.current = true
      applyView({
        zoom,
        panX: screenX - worldX * zoom,
        panY: screenY - worldY * zoom,
      })
    },
    [applyView],
  )

  const zoomBy = (delta: number) => {
    const viewport = viewportRef.current
    if (!viewport) {
      return
    }
    const rect = viewport.getBoundingClientRect()
    zoomAt(rect.width / 2, rect.height / 2, viewRef.current.zoom + delta)
  }

  const ensureVisible = useCallback(
    (type: string) => {
      const viewport = viewportRef.current
      // 通过 ref 读取最新布局：新增阶段后的 rAF 里，闭包里的 layout 还是加阶段之前那一份
      const col = layoutRef.current.cols.find((item) => item.stage.type === type)
      if (!viewport || !col) {
        return
      }
      const rect = viewport.getBoundingClientRect()
      const current = viewRef.current
      const left = col.cardX * current.zoom + current.panX
      // 新增的阶段后面可能紧跟「添加阶段」槽位，一并纳入可见范围，槽位才不会被检查器压住
      const rightEdge = Math.max(
        col.cardX + STAGE_W,
        layoutRef.current.addStage ? layoutRef.current.addStage.x + STAGE_W : 0,
      )
      const right = rightEdge * current.zoom + current.panX
      if (left < 40 || right > rect.width - 40) {
        userAdjusted.current = true
        applyView({
          ...current,
          panX: left < 40 ? 40 - col.cardX * current.zoom : rect.width - 40 - rightEdge * current.zoom,
        })
      }
    },
    [applyView],
  )

  useEffect(() => {
    const frame = window.requestAnimationFrame(fitView)
    return () => window.cancelAnimationFrame(frame)
  }, [fitView])

  useEffect(() => {
    const viewport = viewportRef.current
    if (!viewport) {
      return
    }
    const observer = new ResizeObserver(() => {
      if (!userAdjusted.current) {
        fitView()
      }
    })
    observer.observe(viewport)
    return () => observer.disconnect()
  }, [fitView])

  useEffect(() => {
    const grid = gridRef.current
    if (grid) {
      grid.style.backgroundSize = `${27 * view.zoom}px ${27 * view.zoom}px`
      grid.style.backgroundPosition = `${view.panX}px ${view.panY}px`
    }
  }, [view])

  // React 的 onWheel 在根节点上是 passive，preventDefault 会失效，这里单独挂原生监听
  useEffect(() => {
    const viewport = viewportRef.current
    if (!viewport) {
      return
    }
    const handleWheel = (event: WheelEvent) => {
      event.preventDefault()
      const rect = viewport.getBoundingClientRect()
      if (event.ctrlKey || event.metaKey) {
        zoomAt(
          event.clientX - rect.left,
          event.clientY - rect.top,
          viewRef.current.zoom * Math.exp(-event.deltaY * 0.0015),
        )
        return
      }
      userAdjusted.current = true
      const current = viewRef.current
      applyView({
        ...current,
        panX: current.panX - event.deltaX,
        panY: current.panY - event.deltaY,
      })
    }
    viewport.addEventListener('wheel', handleWheel, { passive: false })
    return () => viewport.removeEventListener('wheel', handleWheel)
  }, [applyView, zoomAt])

  const updateStage = (type: PipelineStageType, patch: Partial<PipelineStage>) => {
    onChange({
      ...definition,
      stages: definition.stages.map((stage) => (stage.type === type ? { ...stage, ...patch } : stage)),
    })
  }

  const updateTask = (type: PipelineStageType, taskIndex: number, patch: Partial<PipelineTask>) => {
    onChange({
      ...definition,
      stages: definition.stages.map((stage) => {
        if (stage.type !== type) {
          return stage
        }
        return {
          ...stage,
          tasks: stage.tasks.map((task, current) => (current === taskIndex ? { ...task, ...patch } : task)),
        }
      }),
    })
  }

  const openInspector = () => {
    setInspectorOpen(true)
    setTab('props')
  }

  const selectStage = (type: PipelineStageType) => {
    setSelection({ kind: 'stage', type })
    openInspector()
  }

  const selectTask = (type: PipelineStageType, taskIndex: number) => {
    setSelection({ kind: 'task', type, taskIndex })
    openInspector()
  }

  const addStage = useCallback(
    (type: PipelineStageType) => {
      if (definition.stages.some((stage) => stage.type === type)) {
        message.warning(`阶段类型「${stageTypeLabel(type)}」已存在`)
        return
      }
      if (definition.stages.length >= 20) {
        message.warning('阶段数量不能超过 20 个')
        return
      }
      onChange({ ...definition, stages: [...definition.stages, createStage(type)] })
      setSelection({ kind: 'stage', type })
      setInspectorOpen(true)
      setTab('props')
      setSlotMenuType(null)
      window.requestAnimationFrame(() => ensureVisible(type))
    },
    [definition, ensureVisible, message, onChange],
  )

  const deleteStage = (type: PipelineStageType) => {
    const index = definition.stages.findIndex((stage) => stage.type === type)
    if (index < 0) {
      return
    }
    const next = definition.stages.filter((stage) => stage.type !== type)
    onChange({ ...definition, stages: next })
    const neighbor = next[Math.min(index, next.length - 1)]
    setSelection(neighbor ? { kind: 'stage', type: neighbor.type } : null)
    setTab('props')
    message.success('已删除阶段')
  }

  const moveStage = (type: PipelineStageType, direction: number) => {
    const index = definition.stages.findIndex((stage) => stage.type === type)
    const target = index + direction
    if (index < 0 || target < 0 || target >= definition.stages.length) {
      return
    }
    const stages = [...definition.stages]
    ;[stages[index], stages[target]] = [stages[target], stages[index]]
    onChange({ ...definition, stages })
    setSelection({ kind: 'stage', type })
    window.requestAnimationFrame(() => ensureVisible(type))
  }

  const addTask = (type: PipelineStageType) => {
    const stage = definition.stages.find((item) => item.type === type)
    if (!stage) {
      return
    }
    if (stage.tasks.length >= 30) {
      message.warning('单阶段任务不能超过 30 个')
      return
    }
    const names = new Set(stage.tasks.map((task) => task.name.trim()))
    let suffix = 1
    while (names.has(`新任务 ${suffix}`)) {
      suffix += 1
    }
    const task = { ...createTask(), name: `新任务 ${suffix}` }
    updateStage(type, { tasks: [...stage.tasks, task] })
    setSelection({ kind: 'task', type, taskIndex: stage.tasks.length })
    setInspectorOpen(true)
    setTab('props')
    window.requestAnimationFrame(() => {
      ensureVisible(type)
      taskNameRef.current?.focus()
      taskNameRef.current?.select()
    })
  }

  const deleteTask = (type: PipelineStageType, taskIndex: number) => {
    const stage = definition.stages.find((item) => item.type === type)
    if (!stage) {
      return
    }
    updateStage(type, { tasks: stage.tasks.filter((_, current) => current !== taskIndex) })
    setSelection(taskIndex === 0 ? { kind: 'stage', type } : { kind: 'task', type, taskIndex: taskIndex - 1 })
    message.success(`已删除任务 #${taskIndex + 1}`)
  }

  const moveTask = (type: PipelineStageType, taskIndex: number, direction: number) => {
    const stage = definition.stages.find((item) => item.type === type)
    if (!stage) {
      return
    }
    const target = taskIndex + direction
    if (target < 0 || target >= stage.tasks.length) {
      return
    }
    const tasks = [...stage.tasks]
    ;[tasks[taskIndex], tasks[target]] = [tasks[target], tasks[taskIndex]]
    updateStage(type, { tasks })
    setSelection({ kind: 'task', type, taskIndex: target })
  }

  const addArtifact = () => {
    if (!selectedStage) {
      return
    }
    const value = artifactInput.trim()
    if (!value) {
      message.warning('请输入产出物声明')
      artifactRef.current?.focus()
      return
    }
    if (value.length > 200) {
      message.warning('产出物声明不能超过 200 个字符')
      return
    }
    if (selectedStage.artifacts.length >= 20) {
      message.warning('单个阶段的产出物声明不能超过 20 个')
      return
    }
    if (selectedStage.artifacts.includes(value)) {
      message.warning('该产出物声明已存在')
      return
    }
    updateStage(selectedStage.type, { artifacts: [...selectedStage.artifacts, value] })
    setArtifactInput('')
    artifactRef.current?.focus()
  }

  const removeArtifact = (artifactIndex: number) => {
    if (!selectedStage) {
      return
    }
    updateStage(selectedStage.type, {
      artifacts: selectedStage.artifacts.filter((_, current) => current !== artifactIndex),
    })
  }

  const switchTab = (next: InspectorTab) => {
    if (next === activeTab) {
      return
    }
    // 每次输入已经校验过 YAML，切换视图不能丢弃错误草稿或解除保存限制。
    if (next === 'yaml' && yamlError === null) {
      setYamlText(definitionToYaml(definition))
    }
    setTab(next)
  }

  const applyYaml = (text: string) => {
    setYamlText(text)
    try {
      onChange(yamlToDefinition(text))
      setYamlError(null)
      onValidityChange(true)
    } catch (error) {
      setYamlError(error instanceof Error ? error.message : 'YAML 解析失败')
      onValidityChange(false)
    }
  }

  const copyJson = () => {
    const text = jsonText(definition)
    const fallback = () => {
      const area = document.createElement('textarea')
      area.value = text
      area.style.position = 'fixed'
      area.style.opacity = '0'
      document.body.appendChild(area)
      area.select()
      try {
        document.execCommand('copy')
        message.success('定义 JSON 已复制')
      } catch {
        showError('复制失败，请手动选择文本')
      }
      area.remove()
    }
    if (navigator.clipboard && window.isSecureContext) {
      navigator.clipboard.writeText(text).then(
        () => message.success('定义 JSON 已复制'),
        fallback,
      )
    } else {
      fallback()
    }
  }

  useImperativeHandle(
    ref,
    () => ({
      addStage,
      reset: () => {
        onChange(cloneDefinition(initialDefinition))
        setSelection(null)
        setTab('overview')
        setYamlText('')
        setYamlError(null)
        setArtifactInput('')
        setSlotMenuType(null)
        draggingRef.current = null
        dropTargetRef.current = null
        setDraggingType(null)
        setDropTarget(null)
        onValidityChange(true)
        window.requestAnimationFrame(fitView)
      },
      showOverview: () => {
        setTab('overview')
        setInspectorOpen(true)
        inspectorBodyRef.current?.scrollTo({ top: 0 })
      },
    }),
    [addStage, fitView, initialDefinition, onChange, onValidityChange],
  )

  const handlePointerDown = (event: ReactPointerEvent<HTMLDivElement>) => {
    if (event.button !== 0) {
      return
    }
    const target = event.target as HTMLElement
    if (target.closest('button, input, textarea, select, .ferry-studio__card, .ferry-studio__empty')) {
      return
    }
    panStart.current = {
      x: event.clientX,
      y: event.clientY,
      panX: viewRef.current.panX,
      panY: viewRef.current.panY,
    }
    panMoved.current = false
    setPanning(true)
    event.currentTarget.setPointerCapture(event.pointerId)
  }

  const handlePointerMove = (event: ReactPointerEvent<HTMLDivElement>) => {
    const start = panStart.current
    if (!start) {
      return
    }
    const dx = event.clientX - start.x
    const dy = event.clientY - start.y
    if (Math.abs(dx) + Math.abs(dy) > 3) {
      panMoved.current = true
    }
    userAdjusted.current = true
    const current = viewRef.current
    applyView({ ...current, panX: start.panX + dx, panY: start.panY + dy })
  }

  const endPan = (event: ReactPointerEvent<HTMLDivElement>) => {
    if (!panStart.current) {
      return
    }
    panStart.current = null
    setPanning(false)
    if (event.currentTarget.hasPointerCapture(event.pointerId)) {
      event.currentTarget.releasePointerCapture(event.pointerId)
    }
    if (!panMoved.current) {
      setSelection(null)
    }
  }

  const handleDragOver = (event: ReactMouseEvent<HTMLDivElement>, stage: PipelineStage) => {
    const dragging = draggingRef.current
    if (!dragging || dragging === stage.type) {
      dropTargetRef.current = null
      setDropTarget(null)
      return
    }
    event.preventDefault()
    const rect = event.currentTarget.getBoundingClientRect()
    const next = { type: stage.type, before: event.clientX < rect.left + rect.width / 2 }
    dropTargetRef.current = next
    setDropTarget(next)
  }

  const reorderStage = (targetType: string, before: boolean) => {
    const dragging = draggingRef.current
    if (!dragging || dragging === targetType) {
      return
    }
    const dragged = definition.stages.find((stage) => stage.type === dragging)
    if (!dragged) {
      return
    }
    const rest = definition.stages.filter((stage) => stage.type !== dragging)
    let targetIndex = rest.findIndex((stage) => stage.type === targetType)
    if (targetIndex < 0) {
      return
    }
    if (!before) {
      targetIndex += 1
    }
    rest.splice(targetIndex, 0, dragged)
    onChange({ ...definition, stages: rest })
    setSelection({ kind: 'stage', type: dragged.type })
    setInspectorOpen(true)
    setTab('props')
  }

  const stageMenuNode = (
    <StageTypeMenu usedTypes={usedTypes} onPick={(type) => addStage(type)} />
  )

  const renderInspector = () => {
    if (!inspectorOpen) {
      return (
        <button
          type="button"
          className="ferry-studio__inspector-open"
          onClick={() => setInspectorOpen(true)}
          aria-label="展开属性面板"
        >
          <PanelRightOpen size={16} />
          <span>属性</span>
        </button>
      )
    }

    const stageIndex = selectedStage ? definition.stages.indexOf(selectedStage) : -1
    const taskIndex = activeSelection?.kind === 'task' ? activeSelection.taskIndex : -1
    const task = selectedStage && taskIndex >= 0 ? selectedStage.tasks[taskIndex] ?? null : null

    const title =
      activeTab === 'overview' ? (
        '编排检查器'
      ) : activeSelection?.kind === 'task' && task ? (
        <>
          <span className="ferry-studio__title-seq">#{taskIndex + 1}</span>
          {task.name || '未命名任务'}
        </>
      ) : selectedStage ? (
        <>
          <span className="ferry-studio__title-seq">S{stageIndex + 1}</span>
          {selectedStage.name || '未命名阶段'}
        </>
      ) : (
        '编排检查器'
      )

    const eyebrow =
      activeTab === 'overview'
        ? 'INSPECTOR · OVERVIEW'
        : activeSelection?.kind === 'task'
          ? 'INSPECTOR · 任务'
          : selectedStage
            ? 'INSPECTOR · 阶段'
            : 'INSPECTOR · OVERVIEW'

    let body: ReactNode = null
    if (activeTab === 'json') {
      body = (
        <>
          <div className="ferry-studio__json-meta">
            <span>与画布实时同步</span>
            <Button size="small" icon={<Copy size={12} />} onClick={copyJson}>
              复制
            </Button>
          </div>
          <pre className="ferry-studio__json">{jsonText(definition)}</pre>
          <div className="ferry-studio__json-note">数组顺序即阶段执行顺序；阶段内任务按序号从上到下输出。</div>
        </>
      )
    } else if (activeTab === 'yaml') {
      body = (
        <>
          <div className={`ferry-studio__yaml-tip${yamlError ? ' is-bad' : ''}`}>
            {yamlError ?? '按 stages 数组编辑；保存前会再做一次结构校验'}
          </div>
          <textarea
            className="ferry-studio__yaml"
            value={yamlText}
            spellCheck={false}
            onChange={(event) => applyYaml(event.target.value)}
          />
        </>
      )
    } else if (activeTab === 'overview' || !selectedStage) {
      body = (
        <>
          <p className="ferry-studio__overview-copy">
            点击画布中的阶段卡片或任务节点查看属性。阶段由实线单向链表串联，任务以虚线链挂在阶段下方，
            按序号从上到下执行。
          </p>
          {overviewExtra}
          <div className="ferry-studio__metrics">
            <div>
              <span>阶段</span>
              <b>{definition.stages.length}</b>
            </div>
            <div>
              <span>任务</span>
              <b>{taskCount(definition)}</b>
            </div>
            <div>
              <span>结构</span>
              <b>链表</b>
            </div>
          </div>
          <div className="ferry-studio__section-head">
            <span>阶段</span>
            <span className="ferry-studio__mono-badge">SINGLY LINKED</span>
          </div>
          <div className="ferry-studio__stage-list">
            {definition.stages.length ? (
              definition.stages.map((stage, index) => (
                <button
                  key={stage.type}
                  type="button"
                  className="ferry-studio__stage-row"
                  onClick={() => selectStage(stage.type)}
                >
                  <span className="ferry-studio__stage-row-seq">S{index + 1}</span>
                  <span className="ferry-studio__stage-row-name">{stage.name || '未命名阶段'}</span>
                  <em>{stage.tasks.length}</em>
                </button>
              ))
            ) : (
              <div className="ferry-studio__empty-note">暂无阶段</div>
            )}
          </div>
          <div className={`ferry-studio__validation${issues.length ? ' is-bad' : ''}`}>
            {issues.length
              ? `存在 ${issues.length} 项待修正：${issues[0]}`
              : '结构校验通过'}
          </div>
        </>
      )
    } else if (activeSelection?.kind === 'task' && task) {
      const total = selectedStage.tasks.length
      const prev = taskIndex === 0 ? '阶段' : `#${taskIndex}`
      const next = taskIndex === total - 1 ? '—' : `#${taskIndex + 2}`
      body = (
        <>
          <button type="button" className="ferry-studio__back" onClick={() => selectStage(selectedStage.type)}>
            <ChevronLeft size={13} />
            返回 S{stageIndex + 1} · {selectedStage.name}
          </button>
          <label className="ferry-studio__field">
            <span>任务名称</span>
            <Input
              ref={taskNameRef}
              value={task.name}
              maxLength={100}
              onChange={(event) => updateTask(selectedStage.type, taskIndex, { name: event.target.value })}
            />
          </label>
          <label className="ferry-studio__field">
            <span>执行命令</span>
            <Input.TextArea
              className="ferry-studio__textarea"
              value={task.command ?? ''}
              maxLength={2000}
              placeholder="shell 命令"
              onChange={(event) => updateTask(selectedStage.type, taskIndex, { command: event.target.value })}
            />
          </label>
          <label className="ferry-studio__field">
            <span>超时（分钟）</span>
            <InputNumber
              min={1}
              max={1440}
              value={task.timeoutMinutes}
              style={{ width: '100%' }}
              onChange={(value) => updateTask(selectedStage.type, taskIndex, { timeoutMinutes: value ?? 1 })}
            />
          </label>
          <div className="ferry-studio__facts">
            <div>
              <span>INDEX</span>
              <b>
                {taskIndex + 1} / {total}
              </b>
            </div>
            <div>
              <span>PREV</span>
              <b>{prev}</b>
            </div>
            <div>
              <span>NEXT</span>
              <b>{next}</b>
            </div>
          </div>
          <div className="ferry-studio__hint-box">
            任务链按序号从上到下执行；当前是第 {taskIndex + 1} / {total} 个任务。阶段内任务名称必须唯一。
          </div>
          <div className="ferry-studio__action-row">
            <Button
              size="small"
              icon={<ArrowUp size={12} />}
              disabled={taskIndex === 0}
              onClick={() => moveTask(selectedStage.type, taskIndex, -1)}
            >
              上移
            </Button>
            <Button
              size="small"
              icon={<ArrowDown size={12} />}
              disabled={taskIndex === total - 1}
              onClick={() => moveTask(selectedStage.type, taskIndex, 1)}
            >
              下移
            </Button>
            <Button
              size="small"
              danger
              icon={<Trash2 size={12} />}
              onClick={() => deleteTask(selectedStage.type, taskIndex)}
            >
              删除任务
            </Button>
          </div>
        </>
      )
    } else {
      body = (
        <>
          <label className="ferry-studio__field">
            <span>阶段名称</span>
            <Input
              value={selectedStage.name}
              maxLength={50}
              onChange={(event) => updateStage(selectedStage.type, { name: event.target.value })}
            />
          </label>
          <div className="ferry-studio__field-row">
            <label className="ferry-studio__field">
              <span>超时（分钟）</span>
              <InputNumber
                min={1}
                max={1440}
                value={selectedStage.timeoutMinutes}
                style={{ width: '100%' }}
                onChange={(value) => updateStage(selectedStage.type, { timeoutMinutes: value ?? 1 })}
              />
            </label>
            <label className="ferry-studio__field">
              <span>阶段类型</span>
              <div className="ferry-studio__readonly">
                <span className="ferry-studio__mono">{selectedStage.type}</span>
                <span>{stageTypeLabel(selectedStage.type)}</span>
              </div>
            </label>
          </div>
          <label className="ferry-studio__switch-row">
            <span>
              <b>失败后继续</b>
              <small>该阶段失败时仍执行后续阶段</small>
            </span>
            <Switch
              checked={selectedStage.continueOnFailure}
              onChange={(checked) => updateStage(selectedStage.type, { continueOnFailure: checked })}
            />
          </label>
          <div className="ferry-studio__field">
            <span>产出物声明</span>
            <div className="ferry-studio__tag-list">
              {selectedStage.artifacts.length ? (
                selectedStage.artifacts.map((item, artifactIndex) => (
                  <span key={item} className="ferry-studio__tag">
                    {item}
                    <button
                      type="button"
                      aria-label={`移除 ${item}`}
                      onClick={() => removeArtifact(artifactIndex)}
                    >
                      <X size={10} />
                    </button>
                  </span>
                ))
              ) : (
                <span className="ferry-studio__tag-empty">未声明产出物</span>
              )}
            </div>
            <div className="ferry-studio__inline-add">
              <Input
                ref={artifactRef}
                size="small"
                value={artifactInput}
                maxLength={200}
                placeholder="例如 target/app.jar"
                onChange={(event) => setArtifactInput(event.target.value)}
                onPressEnter={addArtifact}
              />
              <Button size="small" onClick={addArtifact}>
                添加
              </Button>
            </div>
          </div>
          <hr className="ferry-studio__rule" />
          <div className="ferry-studio__action-row">
            <Button
              size="small"
              icon={<ArrowUp size={12} />}
              disabled={stageIndex === 0}
              onClick={() => moveStage(selectedStage.type, -1)}
            >
              上移
            </Button>
            <Button
              size="small"
              icon={<ArrowDown size={12} />}
              disabled={stageIndex === definition.stages.length - 1}
              onClick={() => moveStage(selectedStage.type, 1)}
            >
              下移
            </Button>
            <Button
              size="small"
              danger
              icon={<Trash2 size={12} />}
              onClick={() => deleteStage(selectedStage.type)}
            >
              删除阶段
            </Button>
          </div>
        </>
      )
    }

    return (
      <aside className="ferry-studio__inspector" aria-label="属性检查器">
        <div className="ferry-studio__inspector-head">
          <div>
            <div className="ferry-studio__eyebrow">{eyebrow}</div>
            <div className="ferry-studio__inspector-title">{title}</div>
          </div>
          <button
            type="button"
            className="ferry-studio__icon-btn"
            onClick={() => setInspectorOpen(false)}
            aria-label="收起属性面板"
          >
            <PanelRightClose size={15} />
          </button>
        </div>
        <div className="ferry-studio__tabs" role="tablist" aria-label="检查器视图">
          <button
            type="button"
            role="tab"
            aria-selected={activeTab === 'overview'}
            className={activeTab === 'overview' ? 'is-active' : ''}
            onClick={() => switchTab('overview')}
          >
            <LayoutDashboard size={13} />
            概览
          </button>
          <button
            type="button"
            role="tab"
            aria-selected={activeTab === 'props'}
            className={activeTab === 'props' ? 'is-active' : ''}
            disabled={!selectedStage}
            title={!selectedStage ? '请先选择阶段或任务' : undefined}
            onClick={() => switchTab('props')}
          >
            <Layers3 size={13} />
            属性
          </button>
          <button
            type="button"
            role="tab"
            aria-selected={activeTab === 'yaml'}
            className={activeTab === 'yaml' ? 'is-active' : ''}
            onClick={() => switchTab('yaml')}
          >
            <FileCode2 size={13} />
            YAML
          </button>
          <button
            type="button"
            role="tab"
            aria-selected={activeTab === 'json'}
            className={activeTab === 'json' ? 'is-active' : ''}
            onClick={() => switchTab('json')}
          >
            <FileJson2 size={13} />
            定义 JSON
          </button>
        </div>
        <div className="ferry-studio__inspector-body" ref={inspectorBodyRef}>
          {yamlError && activeTab !== 'yaml' && (
            <div className="ferry-studio__yaml-tip is-bad" role="alert">
              YAML 草稿存在错误，当前显示最近一次有效定义。请返回 YAML 修正后再保存。
            </div>
          )}
          {body}
        </div>
      </aside>
    )
  }

  const edges: ReactNode[] = []
  const chainY = layout.chainY
  edges.push(
    <defs key="defs">
      <marker id="ferryStudioStageArrow" viewBox="0 0 10 10" refX="9" refY="5" markerWidth="6" markerHeight="6" orient="auto">
        <path d="M 0 0 L 10 5 L 0 10 z" fill="#4096ff" />
      </marker>
      <marker id="ferryStudioTaskArrow" viewBox="0 0 10 10" refX="9" refY="5" markerWidth="6" markerHeight="6" orient="auto">
        <path d="M 0 0 L 10 5 L 0 10 z" fill="#a4aebd" />
      </marker>
    </defs>,
  )
  layout.cols.forEach((col, index) => {
    if (index < layout.cols.length - 1) {
      const next = layout.cols[index + 1]
      const x1 = col.cardX + STAGE_W + 10
      const x2 = next.cardX - 10
      edges.push(
        <line
          key={`stage-line-${col.stage.type}`}
          className="ferry-studio__chain-line"
          x1={x1}
          y1={chainY}
          x2={x2}
          y2={chainY}
          markerEnd="url(#ferryStudioStageArrow)"
        />,
      )
      edges.push(
        <circle key={`stage-pulse-${col.stage.type}`} className="ferry-studio__route-pulse" r={2.2}>
          <animateMotion dur="2.8s" repeatCount="indefinite" path={`M ${x1} ${chainY} L ${x2} ${chainY}`} />
        </circle>,
      )
      edges.push(
        <text
          key={`stage-label-${col.stage.type}`}
          className="ferry-studio__chain-label"
          x={(x1 + x2) / 2}
          y={chainY - 9}
          textAnchor="middle"
        >
          next
        </text>,
      )
    }

    const centerX = col.cardX + STAGE_W / 2
    const stageBottom = STAGE_TOP + STAGE_H
    if (!col.taskYs.length) {
      edges.push(
        <line
          key={`task-link-${col.stage.type}`}
          className="ferry-studio__task-link"
          x1={centerX}
          y1={stageBottom}
          x2={centerX}
          y2={col.addY}
          markerEnd="url(#ferryStudioTaskArrow)"
        />,
      )
      return
    }
    edges.push(
      <line
        key={`task-entry-${col.stage.type}`}
        className="ferry-studio__task-link"
        x1={centerX}
        y1={stageBottom}
        x2={centerX}
        y2={col.taskYs[0]}
        markerEnd="url(#ferryStudioTaskArrow)"
      />,
    )
    edges.push(
      <text
        key={`task-caption-${col.stage.type}`}
        className="ferry-studio__task-caption"
        x={centerX + 12}
        y={(stageBottom + col.taskYs[0]) / 2}
      >
        tasks
      </text>,
    )
    col.taskYs.forEach((taskY, taskIdx) => {
      if (taskIdx >= col.taskYs.length - 1) {
        return
      }
      edges.push(
        <line
          key={`task-link-${col.stage.type}-${taskIdx}`}
          className="ferry-studio__task-link"
          x1={centerX}
          y1={taskY + TASK_H}
          x2={centerX}
          y2={col.taskYs[taskIdx + 1]}
          markerEnd="url(#ferryStudioTaskArrow)"
        />,
      )
    })
    edges.push(
      <line
        key={`task-tail-${col.stage.type}`}
        className="ferry-studio__task-link"
        x1={centerX}
        y1={col.taskYs[col.taskYs.length - 1] + TASK_H}
        x2={centerX}
        y2={col.addY}
        markerEnd="url(#ferryStudioTaskArrow)"
      />,
    )
  })
  if (layout.addStage && layout.cols.length) {
    const last = layout.cols[layout.cols.length - 1]
    edges.push(
      <line
        key="stage-add-link"
        className="ferry-studio__task-link"
        x1={last.cardX + STAGE_W + 10}
        y1={chainY}
        x2={layout.addStage.x - 10}
        y2={chainY}
        markerEnd="url(#ferryStudioTaskArrow)"
      />,
    )
  }

  return (
    <div className="ferry-studio__workspace">
      <div
        ref={viewportRef}
        className={`ferry-studio__viewport${panning ? ' is-panning' : ''}`}
        onPointerDown={handlePointerDown}
        onPointerMove={handlePointerMove}
        onPointerUp={endPan}
        onPointerCancel={endPan}
        onDoubleClick={(event) => {
          if ((event.target as HTMLElement).closest('button, input, textarea, select')) {
            return
          }
          fitView()
        }}
      >
        <div className="ferry-studio__intro">
          <div className="ferry-studio__intro-meta">
            <span className="ferry-studio__live-orbit" aria-hidden="true" />
            <span>RELEASE ROUTE</span>
            <span className="ferry-studio__intro-index">/ 001</span>
          </div>
          <h1 className="ferry-studio__canvas-title">
            让交付<span>顺流而行</span>
          </h1>
          <div className="ferry-studio__canvas-subtitle">STAGE SEQUENCE / TASK CHAIN</div>
        </div>

        <div className="ferry-studio__readout" aria-live="polite">
          <span className="ferry-studio__readout-label">EXECUTION MODEL</span>
          <div className="ferry-studio__readout-values">
            <span className="ferry-studio__readout-item">
              <b>{String(definition.stages.length).padStart(2, '0')}</b> STAGES
            </span>
            <span className="ferry-studio__readout-divider" aria-hidden="true" />
            <span className="ferry-studio__readout-item">
              <b>{String(taskCount(definition)).padStart(2, '0')}</b> TASKS
            </span>
          </div>
        </div>

        <div className="ferry-studio__grid" ref={gridRef} aria-hidden="true" />
        <div
          className="ferry-studio__world"
          ref={worldRef}
          style={{
            width: layout.width,
            height: layout.height,
            transform: `translate(${view.panX}px, ${view.panY}px) scale(${view.zoom})`,
          }}
        >
          <svg
            className="ferry-studio__edges"
            width={layout.width}
            height={layout.height}
            viewBox={`0 0 ${layout.width} ${layout.height}`}
            role="img"
            aria-label="流水线结构图：阶段单向链表，任务以虚线链挂在阶段下方"
          >
            {edges}
          </svg>
          <div className="ferry-studio__nodes">
            {layout.cols.map((col) => {
              const stage = col.stage
              const selectedStageNow =
                activeSelection?.kind === 'stage' && activeSelection.type === stage.type
              const contextStage =
                activeSelection?.kind === 'task' && activeSelection.type === stage.type
              const dropClass =
                dropTarget?.type === stage.type
                  ? dropTarget.before
                    ? ' drop-before'
                    : ' drop-after'
                  : ''
              return (
                <div
                  key={stage.type}
                  className={`ferry-studio__card${selectedStageNow ? ' is-selected' : ''}${
                    contextStage ? ' is-context' : ''
                  }${draggingType === stage.type ? ' is-dragging' : ''}${dropClass}`}
                  style={{
                    transform: `translate(${col.cardX}px, ${STAGE_TOP}px)`,
                    animationDelay: `${col.index * 90}ms`,
                  }}
                  onDragOver={(event) => handleDragOver(event, stage)}
                  onDrop={(event) => {
                    event.preventDefault()
                    const target = dropTargetRef.current
                    if (target) {
                      reorderStage(target.type, target.before)
                    }
                    dropTargetRef.current = null
                    setDropTarget(null)
                  }}
                >
                  <button
                    type="button"
                    className="ferry-studio__card-select"
                    onClick={() => selectStage(stage.type)}
                    aria-label={`选择阶段 S${col.index + 1} ${stage.name}`}
                  />
                  <div className="ferry-studio__card-body">
                    <div className="ferry-studio__card-head">
                      <span className="ferry-studio__card-seq">S{col.index + 1}</span>
                      <span className="ferry-studio__card-type">{stage.type}</span>
                      <span
                        className="ferry-studio__card-grip"
                        draggable
                        title="拖动调整阶段顺序"
                        aria-hidden="true"
                        onDragStart={(event) => {
                          draggingRef.current = stage.type
                          setDraggingType(stage.type)
                          event.dataTransfer.effectAllowed = 'move'
                          event.dataTransfer.setData('text/plain', stage.type)
                          try {
                            const card = event.currentTarget.closest('.ferry-studio__card')
                            if (card) {
                              event.dataTransfer.setDragImage(card, 24, 24)
                            }
                          } catch {
                            // 个别浏览器不支持自定义拖拽图
                          }
                        }}
                        onDragEnd={() => {
                          draggingRef.current = null
                          dropTargetRef.current = null
                          setDraggingType(null)
                          setDropTarget(null)
                        }}
                      >
                        <GripVertical size={16} />
                      </span>
                    </div>
                    <div className="ferry-studio__card-name">{stage.name || '未命名阶段'}</div>
                    <div className="ferry-studio__card-meta">
                      <span>{stage.tasks.length} 个任务</span>
                      <span className="ferry-studio__meta-dot" />
                      <span>{stage.timeoutMinutes} 分钟</span>
                    </div>
                    <div className="ferry-studio__card-tags">
                      <span className={`ferry-studio__chip${stage.continueOnFailure ? ' is-warn' : ''}`}>
                        {stage.continueOnFailure ? '失败继续' : '失败即停'}
                      </span>
                      {stage.artifacts.length > 0 && (
                        <span className="ferry-studio__chip">产出物 {stage.artifacts.length}</span>
                      )}
                    </div>
                    <div className="ferry-studio__card-foot">
                      <span>TASK CHAIN · {stage.tasks.length}</span>
                      <button
                        type="button"
                        className="ferry-studio__mini-add"
                        aria-label="添加任务"
                        onClick={() => addTask(stage.type)}
                      >
                        <Plus size={13} />
                      </button>
                    </div>
                  </div>
                </div>
              )
            })}

            {layout.cols.map((col) =>
              col.stage.tasks.map((task, taskIndex) => (
                <button
                  key={`${col.stage.type}-${taskIndex}`}
                  type="button"
                  className={`ferry-studio__task-node${
                    activeSelection?.kind === 'task' &&
                    activeSelection.type === col.stage.type &&
                    activeSelection.taskIndex === taskIndex
                      ? ' is-selected'
                      : ''
                  }`}
                  style={{
                    transform: `translate(${col.taskX}px, ${col.taskYs[taskIndex]}px)`,
                    animationDelay: `${160 + taskIndex * 75}ms`,
                  }}
                  onClick={() => selectTask(col.stage.type, taskIndex)}
                  title={`${task.name || '未命名任务'}\n${task.command || '未配置执行命令'}\n超时 ${task.timeoutMinutes} 分钟`}
                >
                  <span className="ferry-studio__task-seq">#{taskIndex + 1}</span>
                  <span className="ferry-studio__task-label">{task.name || '未命名任务'}</span>
                </button>
              )),
            )}

            {/* 任务入口与卡片加号共用添加逻辑，避免误用阶段类型菜单。 */}
            {layout.cols.map((col) => (
              <button
                key={`add-${col.stage.type}`}
                type="button"
                className={`ferry-studio__chain-add${col.stage.tasks.length ? '' : ' is-empty'}`}
                style={{ transform: `translate(${col.taskX}px, ${col.addY}px)` }}
                aria-label="添加任务"
                onClick={() => addTask(col.stage.type)}
              >
                {col.stage.tasks.length ? (
                  <>
                    <Plus size={13} />
                    <span>添加任务</span>
                  </>
                ) : (
                  <>
                    <span>暂无任务</span>
                    <small>点击添加</small>
                  </>
                )}
              </button>
            ))}

            {layout.addStage && (
              <Popover
                trigger="click"
                placement="right"
                open={slotMenuType === '__add-stage__'}
                onOpenChange={(next) => setSlotMenuType(next ? '__add-stage__' : null)}
                content={stageMenuNode}
                rootClassName="ferry-studio-popover"
              >
                <button
                  type="button"
                  className="ferry-studio__stage-add"
                  style={{ transform: `translate(${layout.addStage.x}px, ${layout.addStage.y}px)` }}
                >
                  <Plus size={20} />
                  <b>添加阶段</b>
                  <small>选择阶段类型</small>
                </button>
              </Popover>
            )}
          </div>
        </div>

        {definition.stages.length === 0 && (
          <div className="ferry-studio__empty">
            <div className="ferry-studio__empty-inner">
              <b>还没有阶段</b>
              <p>阶段会以单向链表串联，任务以虚线链挂在阶段下方。先添加一个阶段开始编排。</p>
              <Popover
                trigger="click"
                placement="bottom"
                open={slotMenuType === '__empty__'}
                onOpenChange={(next) => setSlotMenuType(next ? '__empty__' : null)}
                content={stageMenuNode}
                rootClassName="ferry-studio-popover"
              >
                <Button type="primary" icon={<Plus size={14} />}>
                  添加第一个阶段
                </Button>
              </Popover>
            </div>
          </div>
        )}

        <div className="ferry-studio__hint">
          <MousePointer2 size={12} />
          拖动阶段手柄排序 · 空白处平移
        </div>

        <div className="ferry-studio__legend" aria-label="图例">
          <span className="ferry-studio__legend-item">
            <i className="ferry-studio__swatch-solid" aria-hidden="true" />
            阶段链路
          </span>
          <span className="ferry-studio__legend-item">
            <i className="ferry-studio__swatch-dashed" aria-hidden="true" />
            任务链路
          </span>
          <span className="ferry-studio__legend-note">从左向右 · 从上向下</span>
        </div>

        <div className="ferry-studio__zoom" aria-label="画布缩放">
          <button type="button" onClick={() => zoomBy(-0.12)} aria-label="缩小">
            <ZoomOut size={14} />
          </button>
          <button
            type="button"
            className="ferry-studio__zoom-value"
            title="恢复到 100%"
            onClick={() => {
              const rect = viewportRef.current?.getBoundingClientRect()
              if (rect) {
                zoomAt(rect.width / 2, rect.height / 2, 1)
              }
            }}
          >
            {Math.round(view.zoom * 100)}%
          </button>
          <button type="button" onClick={() => zoomBy(0.12)} aria-label="放大">
            <ZoomIn size={14} />
          </button>
          <span className="ferry-studio__zoom-sep" aria-hidden="true" />
          <button type="button" className="ferry-studio__fit" onClick={fitView}>
            <Maximize2 size={13} />
            适应
          </button>
        </div>
      </div>

      {renderInspector()}
    </div>
  )
})

export default PipelineDefinitionEditor

function taskCount(definition: PipelineDefinition): number {
  return definition.stages.reduce((sum, stage) => sum + stage.tasks.length, 0)
}

function jsonText(definition: PipelineDefinition): string {
  return JSON.stringify({ stages: definition.stages }, null, 2)
}

function cloneDefinition(definition: PipelineDefinition): PipelineDefinition {
  return {
    stages: definition.stages.map((stage) => ({
      ...stage,
      artifacts: [...stage.artifacts],
      tasks: stage.tasks.map((task) => ({ ...task })),
    })),
  }
}

function clamp(value: number, min: number, max: number): number {
  return Math.min(max, Math.max(min, value))
}
