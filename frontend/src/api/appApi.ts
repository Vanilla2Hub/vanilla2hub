import http from './http'

export interface LogicalApp {
  id: number
  parentAppId: number | null
  parentAppName: string | null
  name: string
  description: string | null
  owner: string | null
  statusCode: string
  appTypeCode: string | null
  extra: string | null
  createdBy: string
  updatedBy: string
  createdAt: string
  updatedAt: string
}

export interface LogicalAppRequest {
  parentAppId?: number | null
  name: string
  description?: string
  owner?: string
  statusCode: string
  appTypeCode?: string
  extra?: string
}

export const appApi = {
  getAll: () => http.get<LogicalApp[]>('/apps').then(r => r.data),
  getById: (id: number) => http.get<LogicalApp>(`/apps/${id}`).then(r => r.data),
  create: (data: LogicalAppRequest) => http.post<LogicalApp>('/apps', data).then(r => r.data),
  update: (id: number, data: LogicalAppRequest) => http.put<LogicalApp>(`/apps/${id}`, data).then(r => r.data),
  delete: (id: number) => http.delete(`/apps/${id}`),
}
