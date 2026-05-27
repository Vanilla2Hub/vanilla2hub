import { useEffect } from 'react'
import { Form, Input, InputNumber, Modal } from 'antd'
import { useTranslation } from 'react-i18next'
import type { Code, CodeRequest } from '../../api/codeApi'

interface Props {
  open: boolean
  editing: Code | null
  onOk: (values: CodeRequest) => void
  onCancel: () => void
  loading: boolean
}

export default function CodeFormModal({ open, editing, onOk, onCancel, loading }: Props) {
  const [form] = Form.useForm<CodeRequest>()
  const { t } = useTranslation()

  useEffect(() => {
    if (open) {
      form.setFieldsValue(editing
        ? { ...editing, description: editing.description ?? undefined, extra: editing.extra ?? undefined }
        : { code: '', name: '', description: '', extra: '', sortOrder: 0 }
      )
    }
  }, [open, editing, form])

  return (
    <Modal
      title={editing ? t('code.editTitle') : t('code.registerTitle')}
      open={open}
      onOk={() => form.validateFields().then(values => onOk({ ...values, extra: values.extra || undefined }))}
      onCancel={onCancel}
      confirmLoading={loading}
      afterClose={() => form.resetFields()}
      destroyOnHidden
    >
      <Form form={form} layout="vertical" style={{ marginTop: 16 }}>
        <Form.Item name="code" label={t('common.code')} rules={[{ required: true, max: 50 }]}>
          <Input placeholder="OKTA" disabled={!!editing} style={{ textTransform: 'uppercase' }} />
        </Form.Item>
        <Form.Item name="name" label={t('common.name')} rules={[{ required: true, max: 100 }]}>
          <Input />
        </Form.Item>
        <Form.Item name="description" label={t('common.description')}>
          <Input.TextArea rows={2} maxLength={500} showCount />
        </Form.Item>
        <Form.Item name="extra" label={t('code.extra')}>
          <Input.TextArea rows={3} placeholder='{"key": "value"}' />
        </Form.Item>
        <Form.Item name="sortOrder" label={t('common.sortOrder')}>
          <InputNumber min={0} style={{ width: '100%' }} />
        </Form.Item>
      </Form>
    </Modal>
  )
}
