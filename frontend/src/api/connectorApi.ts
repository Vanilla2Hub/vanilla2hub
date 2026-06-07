import http from './http'

export interface ConnectorConfig {
  id: number
  name: string
  baseUrl: string
  authType: string
  vaultSecretPath: string
  timeoutMs: number
  retryCount: number
  enabled: boolean
  createdBy: string
  updatedBy: string
  createdAt: string
  updatedAt: string
}

export interface ConnectorConfigRequest {
  name: string
  baseUrl: string
  authType: string
  vaultSecretPath: string
  timeoutMs: number
  retryCount: number
  enabled: boolean
}

export const AUTH_TYPES = ['NONE', 'BASIC', 'BEARER', 'OAUTH2'] as const

export const connectorApi = {
  getAll: () => http.get<ConnectorConfig[]>('/connectors').then(r => r.data),
  getById: (id: number) => http.get<ConnectorConfig>(`/connectors/${id}`).then(r => r.data),
  create: (data: ConnectorConfigRequest) => http.post<ConnectorConfig>('/connectors', data).then(r => r.data),
  update: (id: number, data: ConnectorConfigRequest) => http.put<ConnectorConfig>(`/connectors/${id}`, data).then(r => r.data),
  delete: (id: number) => http.delete(`/connectors/${id}`),
}
