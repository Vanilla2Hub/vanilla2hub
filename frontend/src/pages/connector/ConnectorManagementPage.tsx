import { useState } from 'react'
import { Button, Popconfirm, Space, Table, Tag, Typography, message } from 'antd'
import { PlusOutlined } from '@ant-design/icons'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useTranslation } from 'react-i18next'
import { connectorApi, type ConnectorConfig, type ConnectorConfigRequest } from '../../api/connectorApi'
import ConnectorFormModal from './ConnectorFormModal'

const AUTH_TYPE_COLOR: Record<string, string> = {
  NONE: 'default',
  BASIC: 'blue',
  BEARER: 'green',
  OAUTH2: 'purple',
}

export default function ConnectorManagementPage() {
  const { t } = useTranslation()
  const qc = useQueryClient()
  const [messageApi, contextHolder] = message.useMessage()

  const [modalOpen, setModalOpen] = useState(false)
  const [editing, setEditing] = useState<ConnectorConfig | null>(null)

  const { data: connectors = [], isLoading } = useQuery({
    queryKey: ['connectors'],
    queryFn: connectorApi.getAll,
  })

  const invalidate = () => qc.invalidateQueries({ queryKey: ['connectors'] })

  const createMutation = useMutation({
    mutationFn: connectorApi.create,
    onSuccess: () => { messageApi.success(t('message.registerSuccess')); setModalOpen(false); invalidate() },
    onError: () => messageApi.error(t('message.registerFail')),
  })

  const updateMutation = useMutation({
    mutationFn: ({ id, data }: { id: number; data: ConnectorConfigRequest }) => connectorApi.update(id, data),
    onSuccess: () => { messageApi.success(t('message.updateSuccess')); setModalOpen(false); setEditing(null); invalidate() },
    onError: () => messageApi.error(t('message.updateFail')),
  })

  const deleteMutation = useMutation({
    mutationFn: connectorApi.delete,
    onSuccess: () => { messageApi.success(t('message.deleteSuccess')); invalidate() },
    onError: () => messageApi.error(t('message.deleteFail')),
  })

  const handleSubmit = (values: ConnectorConfigRequest) => {
    if (editing) {
      updateMutation.mutate({ id: editing.id, data: values })
    } else {
      createMutation.mutate(values)
    }
  }

  const columns = [
    {
      title: t('common.name'),
      dataIndex: 'name',
      key: 'name',
      render: (name: string) => <Typography.Text strong>{name}</Typography.Text>,
    },
    {
      title: t('connector.baseUrl'),
      dataIndex: 'baseUrl',
      key: 'baseUrl',
      ellipsis: true,
    },
    {
      title: t('connector.authType'),
      dataIndex: 'authType',
      key: 'authType',
      render: (type: string) => <Tag color={AUTH_TYPE_COLOR[type] ?? 'default'}>{type}</Tag>,
    },
    {
      title: t('connector.vaultSecretPath'),
      dataIndex: 'vaultSecretPath',
      key: 'vaultSecretPath',
      ellipsis: true,
    },
    {
      title: t('connector.timeoutMs'),
      dataIndex: 'timeoutMs',
      key: 'timeoutMs',
      render: (v: number) => `${v}ms`,
    },
    {
      title: t('connector.retryCount'),
      dataIndex: 'retryCount',
      key: 'retryCount',
    },
    {
      title: t('connector.enabled'),
      dataIndex: 'enabled',
      key: 'enabled',
      render: (v: boolean) => <Tag color={v ? 'green' : 'default'}>{v ? t('connector.on') : t('connector.off')}</Tag>,
    },
    {
      title: '',
      key: 'actions',
      width: 120,
      render: (_: unknown, record: ConnectorConfig) => (
        <Space>
          <Button size="small" onClick={() => { setEditing(record); setModalOpen(true) }}>
            {t('common.edit')}
          </Button>
          <Popconfirm title={t('common.deleteConfirm')} onConfirm={() => deleteMutation.mutate(record.id)}>
            <Button size="small" danger>{t('common.delete')}</Button>
          </Popconfirm>
        </Space>
      ),
    },
  ]

  return (
    <>
      {contextHolder}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16 }}>
        <Typography.Title level={4} style={{ margin: 0 }}>{t('connector.title')}</Typography.Title>
        <Button type="primary" icon={<PlusOutlined />} onClick={() => { setEditing(null); setModalOpen(true) }}>
          {t('common.register')}
        </Button>
      </div>
      <Table
        rowKey="id"
        columns={columns}
        dataSource={connectors}
        loading={isLoading}
        size="small"
        pagination={{ pageSize: 20 }}
      />
      <ConnectorFormModal
        open={modalOpen}
        editing={editing}
        onSubmit={handleSubmit}
        onCancel={() => { setModalOpen(false); setEditing(null) }}
        loading={createMutation.isPending || updateMutation.isPending}
      />
    </>
  )
}
