import { download, get, post } from './client'
import type {
  ApiPage,
  AppRow,
  PipelineDefinition,
  PipelineDetail,
  PipelineRow,
  PipelineTemplateDetail,
  PipelineTemplateRow,
  PipelineVersionDetail,
  PipelineVersionRow,
  TemplateDefinition,
  TemplateVersionDetail,
  TemplateVersionRow,
} from './types'
import type { PageParams } from './system'

/** 流水线列表筛选条件，列表与导出共用，避免两边字段名写偏 */
export interface PipelineFilter {
  keyword?: string
  appIds?: string[]
  ownerIds?: string[]
}

/** 模板列表筛选条件 */
export interface PipelineTemplateFilter {
  keyword?: string
  status?: number
}

export const appApi = {
  pageList: (body: PageParams) => post<ApiPage<AppRow>>('/ferry-devops/app/pageList', body),
  detail: (id: string) => get<AppRow>('/ferry-devops/app/detail', { id }),
  save: (body: Record<string, unknown>) => post<string>('/ferry-devops/app/save', body),
  // 后端是 @RequestBody List<Long>，直接提交数组，不能包成 { ids }
  deleted: (ids: string[]) => post<string>('/ferry-devops/app/deleted', ids),
}

export const pipelineApi = {
  pageList: (body: PageParams) => post<ApiPage<PipelineRow>>('/ferry-devops/pipeline/pageList', body),
  detail: (id: string) => get<PipelineDetail>('/ferry-devops/pipeline/detail', { id }),
  save: (body: Record<string, unknown>) => post<string>('/ferry-devops/pipeline/save', body),
  deleted: (ids: string[]) => post<string>('/ferry-devops/pipeline/deleted', ids),
  /** 导出 xlsx：勾选传 ids，未勾选按筛选条件导出，两种都由后端写文件流 */
  export: (body: PipelineFilter & { ids?: string[] }) =>
    download('/ferry-devops/pipeline/export', body, '流水线列表'),
  /** 模板同步确认：merged=true 为合并确认（记录合并时间），false 为忽略提示；标量参数平铺在查询串上 */
  templateAck: (body: { pipelineId: string; merged: boolean }) =>
    post<string>('/ferry-devops/pipeline/template/ack', undefined, { params: body }),
}

export const pipelineVersionApi = {
  list: (pipelineId: string) =>
    get<PipelineVersionRow[]>('/ferry-devops/pipeline/version/list', { pipelineId }),
  detail: (id: string) =>
    get<PipelineVersionDetail>('/ferry-devops/pipeline/version/detail', { id }),
  save: (body: { pipelineId: string; definition: PipelineDefinition; changeNote?: string }) =>
    post<string>('/ferry-devops/pipeline/version/save', body),
  rollback: (body: { pipelineId: string; versionId: string }) =>
    post<string>('/ferry-devops/pipeline/version/rollback', body),
}

export const pipelineTemplateApi = {
  pageList: (body: PageParams & PipelineTemplateFilter & { scope: number }) =>
    post<ApiPage<PipelineTemplateRow>>('/ferry-devops/pipeline/template/pageList', body),
  detail: (id: string) => get<PipelineTemplateDetail>('/ferry-devops/pipeline/template/detail', { id }),
  save: (body: Record<string, unknown>) => post<string>('/ferry-devops/pipeline/template/save', body),
  // 后端是 @RequestBody List<Long>，直接提交数组，不能包成 { ids }
  deleted: (ids: string[]) => post<string>('/ferry-devops/pipeline/template/deleted', ids),
  /** 停用或启用：两个标量参数平铺在查询串上 */
  changeStatus: (body: { id: string; status: number }) =>
    post<string>('/ferry-devops/pipeline/template/status', undefined, { params: body }),
  derivedList: (body: PageParams & { id: string }) =>
    post<ApiPage<PipelineRow>>('/ferry-devops/pipeline/template/derivedList', body),
  /** 创建流水线时可选用的启用模板，组织级与团队级合并返回 */
  options: (keyword?: string) =>
    get<PipelineTemplateRow[]>('/ferry-devops/pipeline/template/options', keyword ? { keyword } : {}),
  versionList: (templateId: string) =>
    get<TemplateVersionRow[]>('/ferry-devops/pipeline/template/version/list', { templateId }),
  versionDetail: (id: string) =>
    get<TemplateVersionDetail>('/ferry-devops/pipeline/template/version/detail', { id }),
  versionSave: (body: { templateId: string; definition: TemplateDefinition; changeNote?: string }) =>
    post<string>('/ferry-devops/pipeline/template/version/save', body),
  rollback: (body: { templateId: string; versionId: string }) =>
    post<string>('/ferry-devops/pipeline/template/version/rollback', body),
}
