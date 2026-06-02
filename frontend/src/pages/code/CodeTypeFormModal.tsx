import { useEffect } from 'react'
import { Button, Divider, Form, Input, InputNumber, Modal, Select, Switch, Tooltip, Typography } from 'antd'
import { DeleteOutlined, PlusOutlined } from '@ant-design/icons'
import { useTranslation } from 'react-i18next'
import type { AttributeField, CodeType, CodeTypeRequest } from '../../api/codeApi'

const { Text } = Typography

interface FormField extends AttributeField {
  optionsRaw?: string
}

interface FormValues extends Omit<CodeTypeRequest, 'attributeSchema'> {
  attributeSchema?: FormField[]
}

interface Props {
  open: boolean
  editing: CodeType | null
  codeTypes: CodeType[]
  onOk: (values: CodeTypeRequest) => void
  onCancel: () => void
  loading: boolean
}

export default function CodeTypeFormModal({ open, editing, codeTypes, onOk, onCancel, loading }: Props) {
  const [form] = Form.useForm<FormValues>()
  const { t } = useTranslation()

  useEffect(() => {
    if (open) {
      const schema = editing?.attributeSchema?.map(f => ({
        ...f,
        optionsRaw: f.options?.join(', ') ?? '',
        _existing: true,
      })) ?? []
      form.setFieldsValue(editing
        ? { ...editing, description: editing.description ?? undefined, attributeSchema: schema }
        : { code: '', name: '', description: '', sortOrder: 0, attributeSchema: [] }
      )
    }
  }, [open, editing, form])

  const handleOk = () => {
    form.validateFields().then(values => {
      const schema = (values.attributeSchema ?? []).map(({ optionsRaw, _existing, ...rest }) => ({
        ...rest,
        options: rest.type === 'select'
          ? (optionsRaw ?? '').split(',').map(s => s.trim()).filter(Boolean)
          : [],
        refCodeTypeCode: rest.type === 'code_ref' ? rest.refCodeTypeCode : undefined,
      }))
      onOk({ ...values, attributeSchema: schema })
    })
  }

  const codeTypeOptions = codeTypes
    .filter(ct => !editing || ct.id !== editing.id)
    .map(ct => ({ value: ct.code, label: `${ct.name} (${ct.code})` }))

  const COLS = '100px 100px 85px 50px 62px 85px 1fr 1fr 32px'
  const HEADERS = [
    t('codeType.fieldKey'), t('codeType.fieldLabel'), t('codeType.fieldType'),
    t('codeType.fieldRequired'), t('codeType.fieldEditable'), t('codeType.fieldDefaultValue'),
    t('codeType.fieldOptions'), t('codeType.fieldRefCodeType'), '',
  ]

  return (
    <Modal
      title={editing ? t('codeType.editTitle') : t('codeType.registerTitle')}
      open={open}
      onOk={handleOk}
      onCancel={onCancel}
      confirmLoading={loading}
      afterClose={() => form.resetFields()}
      destroyOnHidden
      width={860}
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

        <Divider style={{ fontSize: 13 }}>{t('codeType.attributeSchema')}</Divider>

        <Form.List name="attributeSchema">
          {(fields, { add, remove }) => (
            <>
              {fields.length > 0 && (
                <div style={{ display: 'grid', gridTemplateColumns: COLS, gap: 4, marginBottom: 4 }}>
                  {HEADERS.map((h, i) => (
                    <Text key={i} type="secondary" style={{ fontSize: 11, paddingLeft: 4 }}>{h}</Text>
                  ))}
                </div>
              )}

              {fields.map(field => {
                const isExisting = form.getFieldValue(['attributeSchema', field.name, '_existing']) as boolean

                const handleTypeChange = () => {
                  form.setFieldValue(['attributeSchema', field.name, 'optionsRaw'], '')
                  form.setFieldValue(['attributeSchema', field.name, 'refCodeTypeCode'], undefined)
                }

                return (
                  <div key={field.key} style={{ display: 'grid', gridTemplateColumns: COLS, gap: 4, marginBottom: 6, alignItems: 'start' }}>
                    <Form.Item name={[field.name, 'key']} rules={[{ required: true, message: '' }]} style={{ margin: 0 }}>
                      <Input size="small" placeholder="key" disabled={isExisting} />
                    </Form.Item>
                    <Form.Item name={[field.name, 'label']} rules={[{ required: true, message: '' }]} style={{ margin: 0 }}>
                      <Input size="small" placeholder="label" />
                    </Form.Item>
                    <Form.Item name={[field.name, 'type']} rules={[{ required: true, message: '' }]} style={{ margin: 0 }}>
                      <Select size="small" onChange={handleTypeChange} options={[
                        { value: 'text',     label: t('fieldType.text') },
                        { value: 'number',   label: t('fieldType.number') },
                        { value: 'boolean',  label: t('fieldType.boolean') },
                        { value: 'select',   label: t('fieldType.select') },
                        { value: 'code_ref', label: t('fieldType.code_ref') },
                      ]} />
                    </Form.Item>
                    <Form.Item name={[field.name, 'required']} valuePropName="checked" style={{ margin: 0, textAlign: 'center' }}>
                      <Switch size="small" />
                    </Form.Item>
                    <Form.Item name={[field.name, 'editable']} valuePropName="checked" style={{ margin: 0, textAlign: 'center' }}>
                      <Switch size="small" defaultChecked />
                    </Form.Item>
                    <Form.Item name={[field.name, 'defaultValue']} style={{ margin: 0 }}>
                      <Input size="small" placeholder="-" />
                    </Form.Item>

                    {/* options/refCodeTypeCode: type 값에 반응해서 활성화 */}
                    <Form.Item noStyle shouldUpdate>
                      {({ getFieldValue }) => {
                        const type = getFieldValue(['attributeSchema', field.name, 'type'])
                        return (
                          <Form.Item name={[field.name, 'optionsRaw']} style={{ margin: 0 }}>
                            <Input size="small" disabled={type !== 'select'} placeholder="a, b, c" />
                          </Form.Item>
                        )
                      }}
                    </Form.Item>
                    <Form.Item noStyle shouldUpdate>
                      {({ getFieldValue }) => {
                        const type = getFieldValue(['attributeSchema', field.name, 'type'])
                        return (
                          <Form.Item
                            name={[field.name, 'refCodeTypeCode']}
                            style={{ margin: 0 }}
                            rules={type === 'code_ref' ? [{ required: true, message: '' }] : []}
                          >
                            <Select
                              size="small"
                              disabled={type !== 'code_ref'}
                              placeholder={t('codeType.fieldRefCodeType')}
                              options={codeTypeOptions}
                              allowClear
                            />
                          </Form.Item>
                        )
                      }}
                    </Form.Item>

                    <Form.Item name={[field.name, '_existing']} hidden><Input /></Form.Item>
                    <Tooltip title={t('common.delete')}>
                      <Button size="small" danger icon={<DeleteOutlined />} onClick={() => remove(field.name)} style={{ marginTop: 1 }} />
                    </Tooltip>
                  </div>
                )
              })}

              <Button
                type="dashed"
                size="small"
                icon={<PlusOutlined />}
                onClick={() => add({ type: 'text', required: false, editable: true, options: [], _existing: false })}
              >
                {t('codeType.addField')}
              </Button>
            </>
          )}
        </Form.List>
      </Form>
    </Modal>
  )
}
