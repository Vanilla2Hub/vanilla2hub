import http from './http'

const api = http

export interface CodeType {
  id: number
  code: string
  name: string
  description: string | null
  sortOrder: number
  createdBy: string
  updatedBy: string
  createdAt: string
  updatedAt: string
}

export interface Code {
  id: number
  codeTypeId: number
  code: string
  name: string
  description: string | null
  extra: string | null
  sortOrder: number
  createdBy: string
  updatedBy: string
  createdAt: string
  updatedAt: string
}

export interface CodeTypeRequest {
  code: string
  name: string
  description?: string
  sortOrder: number
}

export interface CodeRequest {
  code: string
  name: string
  description?: string
  extra?: string
  sortOrder: number
}

export interface ImportResult {
  created: number
  skipped: number
}

function downloadBlob(data: Blob, filename: string) {
  const url = URL.createObjectURL(data)
  const a = document.createElement('a')
  a.href = url; a.download = filename; a.click()
  URL.revokeObjectURL(url)
}

export const codeTypeApi = {
  getAll: () => api.get<CodeType[]>('/code-types').then(r => r.data),
  getById: (id: number) => api.get<CodeType>(`/code-types/${id}`).then(r => r.data),
  create: (data: CodeTypeRequest) => api.post<CodeType>('/code-types', data).then(r => r.data),
  update: (id: number, data: CodeTypeRequest) => api.put<CodeType>(`/code-types/${id}`, data).then(r => r.data),
  delete: (id: number) => api.delete(`/code-types/${id}`),
  exportCsv: () => api.get('/code-types/export', { responseType: 'blob' })
    .then(r => downloadBlob(r.data as Blob, 'code-types.csv')),
  importCsv: (file: File) => {
    const form = new FormData(); form.append('file', file)
    return api.post<ImportResult>('/code-types/import', form).then(r => r.data)
  },
}

export const codeApi = {
  getAll: (codeTypeId: number) =>
    api.get<Code[]>(`/code-types/${codeTypeId}/codes`).then(r => r.data),
  create: (codeTypeId: number, data: CodeRequest) =>
    api.post<Code>(`/code-types/${codeTypeId}/codes`, data).then(r => r.data),
  update: (codeTypeId: number, codeId: number, data: CodeRequest) =>
    api.put<Code>(`/code-types/${codeTypeId}/codes/${codeId}`, data).then(r => r.data),
  delete: (codeTypeId: number, codeId: number) =>
    api.delete(`/code-types/${codeTypeId}/codes/${codeId}`),
  exportCsv: (codeTypeId: number, filename: string) =>
    api.get(`/code-types/${codeTypeId}/codes/export`, { responseType: 'blob' })
      .then(r => downloadBlob(r.data as Blob, filename)),
  importCsv: (codeTypeId: number, file: File) => {
    const form = new FormData(); form.append('file', file)
    return api.post<ImportResult>(`/code-types/${codeTypeId}/codes/import`, form).then(r => r.data)
  },
}
