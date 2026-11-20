import { Table, type TableProps } from 'antd'
import type { ColumnsType } from 'antd/es/table'

function normalizeColumns<RecordType>(columns?: ColumnsType<RecordType>): ColumnsType<RecordType> | undefined {
  return columns?.map((column) => {
    const adaptiveColumn = {
      ...column,
      width: undefined,
      minWidth: undefined,
      fixed: undefined,
    }
    if ('children' in column && column.children) {
      return { ...adaptiveColumn, children: normalizeColumns(column.children) }
    }
    return adaptiveColumn
  })
}

/** 全局自适应表格：列宽由内容和可用空间共同决定，不使用固定列宽或固定列。 */
export default function FerryTable<RecordType extends object>(props: TableProps<RecordType>) {
  return (
    <Table<RecordType>
      {...props}
      columns={normalizeColumns(props.columns)}
      tableLayout="auto"
    />
  )
}
