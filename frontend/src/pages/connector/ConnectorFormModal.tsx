import { useEffect } from 'react'
import { Form, Input, InputNumber, Modal, Select, Switch } from 'antd'
import { useTranslation } from 'react-i18next'
import { AUTH_TYPES, type ConnectorConfig, type ConnectorConfigRequest } from '../../api/connectorApi'

interface Props {
  open: boolean
  editing: ConnectorConfig | null
  onSubmit: (values: ConnectorConfigRequest) => void
  onCancel: () => void
  loading: boolean
}

export default function ConnectorFormModal({ open, editing, onSubmit, onCancel, loading }: Props) {
  const { t } = useTranslation()
  const [form] = Form.useForm<ConnectorConfigRequest>()

  useEffect(() => {
    if (open) {
      form.setFieldsValue(editing
        ? {
            name: editing.name,
            baseUrl: editing.baseUrl,
            authType: editing.authType,
            vaultSecretPath: editing.vaultSecretPath,
            timeoutMs: editing.timeoutMs,
            retryCount: editing.retryCount,
            enabled: editing.enabled,
          }
        : { authType: 'NONE', timeoutMs: 30000, retryCount: 3, enabled: true }
      )
    } else {
      form.resetFields()
    }
  }, [open, editing, form])

  return (
    <Modal
      open={open}
      title={editing ? t('connector.editTitle') : t('connector.registerTitle')}
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
        <Form.Item name="baseUrl" label={t('connector.baseUrl')} rules={[{ required: true }]}>
          <Input maxLength={500} placeholder="https://api.example.com" />
        </Form.Item>
        <Form.Item name="authType" label={t('connector.authType')} rules={[{ required: true }]}>
          <Select options={AUTH_TYPES.map(t => ({ value: t, label: t }))} />
        </Form.Item>
        <Form.Item name="vaultSecretPath" label={t('connector.vaultSecretPath')} rules={[{ required: true }]}>
          <Input maxLength={500} placeholder="secret/data/connectors/example" />
        </Form.Item>
        <Form.Item name="timeoutMs" label={t('connector.timeoutMs')} rules={[{ required: true }]}>
          <InputNumber min={1000} max={300000} step={1000} style={{ width: '100%' }} addonAfter="ms" />
        </Form.Item>
        <Form.Item name="retryCount" label={t('connector.retryCount')} rules={[{ required: true }]}>
          <InputNumber min={0} max={10} style={{ width: '100%' }} />
        </Form.Item>
        <Form.Item name="enabled" label={t('connector.enabled')} valuePropName="checked">
          <Switch />
        </Form.Item>
      </Form>
    </Modal>
  )
}
