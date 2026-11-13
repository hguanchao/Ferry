import { diffLines as jsDiffLines } from 'diff'

export interface DiffRow {
  type: 'same' | 'add' | 'del'
  leftNo?: number
  rightNo?: number
  text: string
}

/**
 * 行级差异
 *
 * 左右两侧的行号各走各的计数器：删除行只占左侧、新增行只占右侧，
 * 界面据此渲染成两栏并保持对齐。
 */
export function diffLines(leftText: string, rightText: string): DiffRow[] {
  const rows: DiffRow[] = []
  let leftNo = 1
  let rightNo = 1
  for (const change of jsDiffLines(leftText, rightText)) {
    const lines = change.value.split('\n')
    // jsdiff 的 value 通常以换行结尾，split 后会多出一个空元素
    if (lines.length > 1 && lines[lines.length - 1] === '') {
      lines.pop()
    }
    for (const line of lines) {
      if (change.added) {
        rows.push({ type: 'add', rightNo: rightNo++, text: line })
      } else if (change.removed) {
        rows.push({ type: 'del', leftNo: leftNo++, text: line })
      } else {
        rows.push({ type: 'same', leftNo: leftNo++, rightNo: rightNo++, text: line })
      }
    }
  }
  return rows
}
