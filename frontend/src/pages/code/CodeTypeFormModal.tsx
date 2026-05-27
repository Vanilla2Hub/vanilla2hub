import { useEffect } from 'react'
import { Form, Input, InputNumber, Modal } from 'antd'
import { useTranslation } from 'react-i18next'
import type { CodeType, CodeTypeRequest } from '../../api/codeApi'

interface Props {
  open: boolean
  editing: CodeType | null
  onOk: (values: CodeTypeRequest) => void
  onCancel: () => void
  loading: boolean
}

export default function CodeTypeFormModal({ open, editing, onOk, onCancel, loading }: Props) {
  const [form] = Form.useForm<CodeTypeRequest>()
  const { t } = useTranslation()

  useEffect(() => {
    if (open) {
      form.setFieldsValue(editing
        ? { ...editing, description: editing.description ?? undefined }
        : { code: '', name: '', description: '', sortOrder: 0 }
      )
    }
  }, [open, editing, form])

  return (
    <Modal
      title={editing ? t('codeType.editTitle') : t('codeType.registerTitle')}
      open={open}
      onOk={() => form.validateFields().then(onOk)}
      onCancel={onCancel}
      confirmLoading={loading}
      afterClose={() => form.resetFields()}
      destroyOnHidden
    >
      <Form form={form} layout="vertical" style={{ marginTop: 16 }}>
        <Form.Item name="code" label={t('common.code')} rules={[{ required: true, max: 50 }]}>
          <Input placeholder="CONNECTOR_TYPE" disabled={!!editing} style={{ textTransform: 'uppercase' }} />
        </Form.Item>
        <Form.Item name="name" label={t('common.name')} rules={[{ required: true, max: 100 }]}>
          <Input />
        </Form.Item>
        <Form.Item name="description" label={t('common.description')}>
          <Input.TextArea rows={2} maxLength={500} showCount />
        </Form.Item>
        <Form.Item name="sortOrder" label={t('common.sortOrder')}>
          <InputNumber min={0} style={{ width: '100%' }} />
        </Form.Item>
      </Form>
    </Modal>
  )
}
