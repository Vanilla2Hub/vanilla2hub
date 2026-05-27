import { useEffect } from 'react'
import { Form, Input, InputNumber, Modal } from 'antd'
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
      title={editing ? '코드 수정' : '코드 등록'}
      open={open}
      onOk={() => form.validateFields().then(values => onOk({ ...values, extra: values.extra || undefined }))}
      onCancel={onCancel}
      confirmLoading={loading}
      afterClose={() => form.resetFields()}
      destroyOnHidden
    >
      <Form form={form} layout="vertical" style={{ marginTop: 16 }}>
        <Form.Item name="code" label="코드" rules={[{ required: true, max: 50 }]}>
          <Input placeholder="OKTA" disabled={!!editing} style={{ textTransform: 'uppercase' }} />
        </Form.Item>
        <Form.Item name="name" label="이름" rules={[{ required: true, max: 100 }]}>
          <Input placeholder="Okta" />
        </Form.Item>
        <Form.Item name="description" label="설명">
          <Input.TextArea rows={2} maxLength={500} showCount />
        </Form.Item>
        <Form.Item name="extra" label="추가 속성 (JSON)">
          <Input.TextArea rows={3} placeholder='{"key": "value"}' />
        </Form.Item>
        <Form.Item name="sortOrder" label="정렬순서">
          <InputNumber min={0} style={{ width: '100%' }} />
        </Form.Item>
      </Form>
    </Modal>
  )
}
