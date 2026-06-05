import { useState } from 'react'
import { Button, Popconfirm, Space, Table, Tag, Typography, message } from 'antd'
import { PlusOutlined } from '@ant-design/icons'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useTranslation } from 'react-i18next'
import { appApi, type LogicalApp, type LogicalAppRequest } from '../../api/appApi'
import AppFormModal from './AppFormModal'

const STATUS_COLOR: Record<string, string> = {
  ACTIVE: 'green',
  INACTIVE: 'default',
  DEPRECATED: 'red',
}

export default function AppManagementPage() {
  const { t } = useTranslation()
  const qc = useQueryClient()
  const [messageApi, contextHolder] = message.useMessage()

  const [modalOpen, setModalOpen] = useState(false)
  const [editing, setEditing] = useState<LogicalApp | null>(null)

  const { data: apps = [], isLoading } = useQuery({
    queryKey: ['apps'],
    queryFn: appApi.getAll,
  })

  const invalidate = () => qc.invalidateQueries({ queryKey: ['apps'] })

  const createMutation = useMutation({
    mutationFn: appApi.create,
    onSuccess: () => { messageApi.success(t('message.registerSuccess')); setModalOpen(false); invalidate() },
    onError: () => messageApi.error(t('message.registerFail')),
  })

  const updateMutation = useMutation({
    mutationFn: ({ id, data }: { id: number; data: LogicalAppRequest }) => appApi.update(id, data),
    onSuccess: () => { messageApi.success(t('message.updateSuccess')); setModalOpen(false); setEditing(null); invalidate() },
    onError: () => messageApi.error(t('message.updateFail')),
  })

  const deleteMutation = useMutation({
    mutationFn: appApi.delete,
    onSuccess: () => { messageApi.success(t('message.deleteSuccess')); invalidate() },
    onError: () => messageApi.error(t('message.deleteFail')),
  })

  const handleSubmit = (values: LogicalAppRequest) => {
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
      title: t('app.parentApp'),
      dataIndex: 'parentAppName',
      key: 'parentAppName',
      render: (v: string | null) => v ?? '-',
    },
    {
      title: t('app.owner'),
      dataIndex: 'owner',
      key: 'owner',
      render: (v: string | null) => v ?? '-',
    },
    {
      title: t('app.status'),
      dataIndex: 'statusCode',
      key: 'statusCode',
      render: (code: string) => <Tag color={STATUS_COLOR[code] ?? 'default'}>{code}</Tag>,
    },
    {
      title: t('app.appType'),
      dataIndex: 'appTypeCode',
      key: 'appTypeCode',
      render: (v: string | null) => v ?? '-',
    },
    {
      title: t('common.description'),
      dataIndex: 'description',
      key: 'description',
      render: (v: string | null) => v ?? '-',
      ellipsis: true,
    },
    {
      title: '',
      key: 'actions',
      width: 120,
      render: (_: unknown, record: LogicalApp) => (
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
        <Typography.Title level={4} style={{ margin: 0 }}>{t('app.title')}</Typography.Title>
        <Button type="primary" icon={<PlusOutlined />} onClick={() => { setEditing(null); setModalOpen(true) }}>
          {t('common.register')}
        </Button>
      </div>
      <Table
        rowKey="id"
        columns={columns}
        dataSource={apps}
        loading={isLoading}
        size="small"
        pagination={{ pageSize: 20 }}
      />
      <AppFormModal
        open={modalOpen}
        editing={editing}
        apps={apps}
        onSubmit={handleSubmit}
        onCancel={() => { setModalOpen(false); setEditing(null) }}
        loading={createMutation.isPending || updateMutation.isPending}
      />
    </>
  )
}
