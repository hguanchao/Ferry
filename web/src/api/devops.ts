import { get, post } from './client'
import type { ApiPage, AppRow } from './types'
import type { PageParams } from './system'

export const appApi = {
  pageList: (body: PageParams) => post<ApiPage<AppRow>>('/ferry-devops/app/pageList', body),
  detail: (id: string) => get<AppRow>('/ferry-devops/app/detail', { id }),
  save: (body: Record<string, unknown>) => post<string>('/ferry-devops/app/save', body),
  // 后端是 @RequestBody List<Long>，直接提交数组，不能包成 { ids }
  deleted: (ids: string[]) => post<string>('/ferry-devops/app/deleted', ids),
}
