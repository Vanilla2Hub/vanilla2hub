import { useEffect, useMemo } from 'react'
import { Form, Input, InputNumber, Modal, Select, Switch } from 'antd'
import { useQueries } from '@tanstack/react-query'
import { useTranslation } from 'react-i18next'
import { codeApi } from '../../api/codeApi'
import type { AttributeField, Code, CodeRequest, CodeType } from '../../api/codeApi'

interface FormValues {
  code: string
  name: string
  description?: string
  sortOrder: number
  extra?: Record<string, unknown>
}

interface Props {
  open: boolean
  editing: Code | null
  codeType: CodeType | null
  codeTypes: CodeType[]
  onOk: (values: CodeRequest) => void
  onCancel: () => void
  loading: boolean
}

function renderAttributeInput(
  field: AttributeField,
  disabled: boolean,
  refCodes: Code[],
) {
  switch (field.type) {
    case 'number':
      return <InputNumber style={{ width: '100%' }} disabled={disabled} />
    case 'boolean':
      return <Switch disabled={disabled} />
    case 'select':
      return (
        <Select
          disabled={disabled}
          options={(field.options ?? []).map(o => ({ value: o, label: o }))}
        />
      )
    case 'code_ref':
      return (
        <Select
          disabled={disabled}
          showSearch
          optionFilterProp="label"
          options={refCodes.map(c => ({ value: c.code, label: `${c.name} (${c.code})` }))}
        />
      )
    default:
      return <Input disabled={disabled} />
  }
}

export default function CodeFormModal({ open, editing, codeType, codeTypes, onOk, onCancel, loading }: Props) {
  const [form] = Form.useForm<FormValues>()
  const { t } = useTranslation()
  const schema = codeType?.attributeSchema ?? []

  // code_ref 필드가 참조하는 CodeType ID 목록 (중복 제거)
  const refCodeTypeIds = useMemo(() => {
    const ids = schema
      .filter(f => f.type === 'code_ref' && f.refCodeTypeCode)
      .map(f => codeTypes.find(ct => ct.code === f.refCodeTypeCode)?.id)
      .filter((id): id is number => id !== undefined)
    return [...new Set(ids)]
  }, [schema, codeTypes])

  // 참조 CodeType별 코드 목록 fetch
  const refQueries = useQueries({
    queries: refCodeTypeIds.map(id => ({
      queryKey: ['codes', id],
      queryFn: () => codeApi.getAll(id),
      enabled: open,
    })),
  })

  // codeTypeId → Code[] 맵
  const refCodesById = useMemo(() => {
    const map: Record<number, Code[]> = {}
    refCodeTypeIds.forEach((id, i) => {
      map[id] = refQueries[i]?.data ?? []
    })
    return map
  }, [refCodeTypeIds, refQueries])

  useEffect(() => {
    if (open) {
      if (schema.length === 0) {
        form.setFieldsValue(editing
          ? { ...editing, description: editing.description ?? undefined, extra: (editing.extra ?? '') as unknown as Record<string, unknown> }
          : { code: '', name: '', description: '', sortOrder: 0, extra: '' as unknown as Record<string, unknown> }
        )
      } else {
        const parsedExtra = editing?.extra
          ? (() => { try { return JSON.parse(editing.extra!) } catch { return {} } })()
          : {}
        form.setFieldsValue(editing
          ? { ...editing, description: editing.description ?? undefined, extra: parsedExtra }
          : { code: '', name: '', description: '', sortOrder: 0, extra: {} }
        )
      }
    }
  }, [open, editing, form, schema])

  const handleOk = () => {
    form.validateFields().then(values => {
      const { extra, ...rest } = values
      let extraJson: string | undefined
      if (schema.length === 0) {
        const s = (extra as unknown as string)?.trim()
        extraJson = s || undefined
      } else {
        extraJson = (extra && Object.keys(extra).length > 0) ? JSON.stringify(extra) : undefined
      }
      onOk({ ...rest, extra: extraJson })
    })
  }

  return (
    <Modal
      title={editing ? t('code.editTitle') : t('code.registerTitle')}
      open={open}
      onOk={handleOk}
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

        {schema.length > 0 && schema.map(field => {
          const isDisabledOnEdit = !!editing && !field.editable
          const valuePropName = field.type === 'boolean' ? 'checked' : 'value'
          const refTypeId = field.type === 'code_ref' && field.refCodeTypeCode
            ? codeTypes.find(ct => ct.code === field.refCodeTypeCode)?.id
            : undefined
          const refCodes = refTypeId !== undefined ? (refCodesById[refTypeId] ?? []) : []

          return (
            <Form.Item
              key={field.key}
              name={['extra', field.key]}
              label={field.label}
              valuePropName={valuePropName}
              rules={field.required ? [{ required: true, message: `${field.label}은(는) 필수입니다.` }] : []}
            >
              {renderAttributeInput(field, isDisabledOnEdit, refCodes)}
            </Form.Item>
          )
        })}

        {schema.length === 0 && (
          <Form.Item name="extra" label={t('code.extra')}>
            <Input.TextArea rows={3} placeholder='{"key": "value"}' />
          </Form.Item>
        )}

        <Form.Item name="sortOrder" label={t('common.sortOrder')}>
          <InputNumber min={0} style={{ width: '100%' }} />
        </Form.Item>
      </Form>
    </Modal>
  )
}
