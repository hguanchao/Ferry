export interface ExportColumn<T> {
  title: string
  value: (row: T) => unknown
}

interface ExportOptions<T> {
  filename: string
  rows: T[]
  columns: Array<ExportColumn<T>>
  operator: string
  filters?: string
}

function escapeCell(value: unknown) {
  const text =
    value === null || value === undefined
      ? ''
      : typeof value === 'object'
        ? JSON.stringify(value)
        : String(value)
  if (/[",\n\r]/.test(text)) {
    return `"${text.replace(/"/g, '""')}"`
  }
  return text
}

function timestamp() {
  const d = new Date()
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}${pad(d.getMonth() + 1)}${pad(d.getDate())}-${pad(d.getHours())}${pad(d.getMinutes())}`
}

/**
 * CSV 导出
 *
 * 导出与列表同源、受数据权限约束、首行带导出人与筛选条件摘要；
 * 这里把水印写成以 # 开头的注释行，Excel 打开时可直接看到，不影响列结构。
 */
export function exportCsv<T>({ filename, rows, columns, operator, filters }: ExportOptions<T>) {
  const now = new Date().toLocaleString('zh-CN', { hour12: false })
  const watermark = [
    `# 导出人：${operator}`,
    `# 导出时间：${now}`,
    `# 筛选条件：${filters || '无（全量）'}`,
    `# 数据量：${rows.length} 条`,
  ]
  const header = columns.map((column) => escapeCell(column.title)).join(',')
  const body = rows.map((row) => columns.map((column) => escapeCell(column.value(row))).join(','))
  const content = [...watermark, header, ...body].join('\r\n')

  const blob = new Blob([`\uFEFF${content}`], { type: 'text/csv;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const anchor = document.createElement('a')
  anchor.href = url
  anchor.download = `${filename}-${timestamp()}.csv`
  document.body.appendChild(anchor)
  anchor.click()
  document.body.removeChild(anchor)
  URL.revokeObjectURL(url)
}
