import { get, post } from './client'
import type {
  ApiPage,
  AppRow,
  PipelineDefinition,
  PipelineDetail,
  PipelineRow,
  PipelineVersionDetail,
  PipelineVersionRow,
} from './types'
import type { PageParams } from './system'

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
