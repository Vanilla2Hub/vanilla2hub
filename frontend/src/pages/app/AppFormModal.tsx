import { useEffect } from 'react'
import { Form, Input, Modal, Select } from 'antd'
import { useTranslation } from 'react-i18next'
import { useQuery } from '@tanstack/react-query'
import { codeTypeApi, codeApi } from '../../api/codeApi'
import type { LogicalApp, LogicalAppRequest } from '../../api/appApi'

interface Props {
  open: boolean
  editing: LogicalApp | null
  apps: LogicalApp[]
  onSubmit: (values: LogicalAppRequest) => void
  onCancel: () => void
  loading: boolean
}

export default function AppFormModal({ open, editing, apps, onSubmit, onCancel, loading }: Props) {
  const { t } = useTranslation()
  const [form] = Form.useForm<LogicalAppRequest>()

  const { data: codeTypes = [] } = useQuery({
    queryKey: ['codeTypes'],
    queryFn: codeTypeApi.getAll,
  })

  const statusTypeId = codeTypes.find(ct => ct.code === 'APP_STATUS')?.id
  const appTypeId = codeTypes.find(ct => ct.code === 'APP_TYPE')?.id

  const { data: statusCodes = [] } = useQuery({
    queryKey: ['codes', statusTypeId],
    queryFn: () => codeApi.getAll(statusTypeId!),
    enabled: !!statusTypeId,
  })

  const { data: appTypeCodes = [] } = useQuery({
    queryKey: ['codes', appTypeId],
    queryFn: () => codeApi.getAll(appTypeId!),
    enabled: !!appTypeId,
  })

  useEffect(() => {
    if (open) {
      form.setFieldsValue(editing
        ? {
            parentAppId: editing.parentAppId ?? undefined,
            name: editing.name,
            description: editing.description ?? undefined,
            owner: editing.owner ?? undefined,
            statusCode: editing.statusCode,
            appTypeCode: editing.appTypeCode ?? undefined,
            extra: editing.extra ?? undefined,
          }
        : { statusCode: 'ACTIVE' }
      )
    } else {
      form.resetFields()
    }
  }, [open, editing, form])

  const parentOptions = apps
    .filter(a => a.id !== editing?.id)
    .map(a => ({ value: a.id, label: a.name }))

  return (
    <Modal
      open={open}
      title={editing ? t('app.editTitle') : t('app.registerTitle')}
      onOk={() => form.submit()}
      onCancel={onCancel}
      confirmLoading={loading}
      okText={editing ? t('common.edit') : t('common.register')}
      cancelText={t('common.cancel')}
      width={560}
    >
      <Form form={form} layout="vertical" onFinish={onSubmit}>
        <Form.Item name="name" label={t('common.name')} rules={[{ required: true }]}>
          <Input maxLength={100} />
        </Form.Item>
        <Form.Item name="description" label={t('common.description')}>
          <Input.TextArea rows={2} maxLength={500} />
        </Form.Item>
        <Form.Item name="owner" label={t('app.owner')}>
          <Input maxLength={100} />
        </Form.Item>
        <Form.Item name="statusCode" label={t('app.status')} rules={[{ required: true }]}>
          <Select
            options={statusCodes.length
              ? statusCodes.map(c => ({ value: c.code, label: c.name }))
              : [{ value: 'ACTIVE', label: 'ACTIVE' }, { value: 'INACTIVE', label: 'INACTIVE' }, { value: 'DEPRECATED', label: 'DEPRECATED' }]
            }
          />
        </Form.Item>
        <Form.Item name="appTypeCode" label={t('app.appType')}>
          <Select
            allowClear
            options={appTypeCodes.map(c => ({ value: c.code, label: c.name }))}
          />
        </Form.Item>
        <Form.Item name="parentAppId" label={t('app.parentApp')}>
          <Select allowClear options={parentOptions} showSearch
            filterOption={(input, option) =>
              (option?.label as string ?? '').toLowerCase().includes(input.toLowerCase())
            }
          />
        </Form.Item>
        <Form.Item name="extra" label={t('app.extra')}>
          <Input.TextArea rows={3} placeholder='{"key": "value"}' />
        </Form.Item>
      </Form>
    </Modal>
  )
}
