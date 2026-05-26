import { useEffect } from 'react'
import { Form, Input, InputNumber, Modal } from 'antd'
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
      title={editing ? '코드타입 수정' : '코드타입 등록'}
      open={open}
      onOk={() => form.validateFields().then(onOk)}
      onCancel={onCancel}
      confirmLoading={loading}
      afterClose={() => form.resetFields()}
      destroyOnHidden
    >
      <Form form={form} layout="vertical" style={{ marginTop: 16 }}>
        <Form.Item name="code" label="코드" rules={[{ required: true, max: 50 }]}>
          <Input placeholder="CONNECTOR_TYPE" disabled={!!editing} style={{ textTransform: 'uppercase' }} />
        </Form.Item>
        <Form.Item name="name" label="이름" rules={[{ required: true, max: 100 }]}>
          <Input placeholder="커넥터 유형" />
        </Form.Item>
        <Form.Item name="description" label="설명">
          <Input.TextArea rows={2} maxLength={500} showCount />
        </Form.Item>
        <Form.Item name="sortOrder" label="정렬순서">
          <InputNumber min={0} style={{ width: '100%' }} />
        </Form.Item>
      </Form>
    </Modal>
  )
}
