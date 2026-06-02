import { useRef, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Button, Col, Popconfirm, Row, Space, Table, Tag, Tooltip, Typography, message } from 'antd'
import { DeleteOutlined, DownloadOutlined, EditOutlined, LockOutlined, PlusOutlined, UnorderedListOutlined, UploadOutlined } from '@ant-design/icons'
import { useTranslation } from 'react-i18next'
import type { ColumnsType } from 'antd/es/table'
import { codeApi, codeTypeApi } from '../../api/codeApi'
import type { Code, CodeRequest, CodeType, CodeTypeRequest, ImportResult } from '../../api/codeApi'
import CodeTypeFormModal from './CodeTypeFormModal'
import CodeFormModal from './CodeFormModal'

const { Title, Text } = Typography

export default function CodeManagementPage() {
  const queryClient = useQueryClient()
  const [messageApi, contextHolder] = message.useMessage()
  const { t } = useTranslation()

  const [codeTypeModal, setCodeTypeModal] = useState<{ open: boolean; editing: CodeType | null }>({ open: false, editing: null })
  const [codeModal, setCodeModal] = useState<{ open: boolean; editing: Code | null }>({ open: false, editing: null })
  const [selectedCodeTypeId, setSelectedCodeTypeId] = useState<number | null>(null)
  const codeTypeImportRef = useRef<HTMLInputElement>(null)
  const codeImportRef = useRef<HTMLInputElement>(null)

  const { data: codeTypes = [], isLoading: loadingTypes } = useQuery({
    queryKey: ['codeTypes'],
    queryFn: codeTypeApi.getAll,
  })

  // codeTypes 쿼리에서 파생 — CodeType 수정 후 캐시 갱신 시 자동으로 최신값 반영
  const selectedCodeType = codeTypes.find(ct => ct.id === selectedCodeTypeId) ?? null

  const { data: codes = [], isLoading: loadingCodes } = useQuery({
    queryKey: ['codes', selectedCodeTypeId],
    queryFn: () => codeApi.getAll(selectedCodeTypeId!),
    enabled: !!selectedCodeTypeId,
  })

  const createCodeType = useMutation({
    mutationFn: (data: CodeTypeRequest) => codeTypeApi.create(data),
    onSuccess: () => { queryClient.invalidateQueries({ queryKey: ['codeTypes'] }); setCodeTypeModal({ open: false, editing: null }); messageApi.success(t('codeType.registerSuccess')) },
    onError: () => messageApi.error(t('message.registerFail')),
  })
  const updateCodeType = useMutation({
    mutationFn: ({ id, data }: { id: number; data: CodeTypeRequest }) => codeTypeApi.update(id, data),
    onSuccess: () => { queryClient.invalidateQueries({ queryKey: ['codeTypes'] }); setCodeTypeModal({ open: false, editing: null }); messageApi.success(t('message.updateSuccess')) },
    onError: () => messageApi.error(t('message.updateFail')),
  })
  const deleteCodeType = useMutation({
    mutationFn: (id: number) => codeTypeApi.delete(id),
    onSuccess: () => { queryClient.invalidateQueries({ queryKey: ['codeTypes'] }); setSelectedCodeTypeId(null); messageApi.success(t('message.deleteSuccess')) },
    onError: () => messageApi.error(t('message.deleteFail')),
  })

  const createCode = useMutation({
    mutationFn: (data: CodeRequest) => codeApi.create(selectedCodeType!.id, data),
    onSuccess: () => { queryClient.invalidateQueries({ queryKey: ['codes', selectedCodeType?.id] }); setCodeModal({ open: false, editing: null }); messageApi.success(t('code.registerSuccess')) },
    onError: () => messageApi.error(t('message.registerFail')),
  })
  const updateCode = useMutation({
    mutationFn: ({ id, data }: { id: number; data: CodeRequest }) => codeApi.update(selectedCodeType!.id, id, data),
    onSuccess: () => { queryClient.invalidateQueries({ queryKey: ['codes', selectedCodeType?.id] }); setCodeModal({ open: false, editing: null }); messageApi.success(t('message.updateSuccess')) },
    onError: () => messageApi.error(t('message.updateFail')),
  })
  const deleteCode = useMutation({
    mutationFn: (id: number) => codeApi.delete(selectedCodeType!.id, id),
    onSuccess: () => { queryClient.invalidateQueries({ queryKey: ['codes', selectedCodeType?.id] }); messageApi.success(t('message.deleteSuccess')) },
    onError: () => messageApi.error(t('message.deleteFail')),
  })

  const codeTypeColumns: ColumnsType<CodeType> = [
    {
      title: t('common.code'), dataIndex: 'code', key: 'code',
      render: (v, r) => (
        <Space size={4}>
          {r.systemDefault && <Tooltip title={t('common.system')}><LockOutlined style={{ color: '#999', fontSize: 11 }} /></Tooltip>}
          <Tag>{v}</Tag>
        </Space>
      ),
    },
    { title: t('common.name'), dataIndex: 'name', key: 'name' },
    { title: t('common.sortOrder'), dataIndex: 'sortOrder', key: 'sortOrder', width: 70, align: 'center' },
    {
      title: '', key: 'actions', width: 100, align: 'center',
      render: (_, record) => (
        <Space>
          <Button size="small" icon={<UnorderedListOutlined />} onClick={() => setSelectedCodeTypeId(record.id)} />
          <Button size="small" icon={<EditOutlined />} disabled={record.systemDefault} onClick={() => setCodeTypeModal({ open: true, editing: record })} />
          <Popconfirm title={t('common.deleteConfirm')} onConfirm={() => deleteCodeType.mutate(record.id)} disabled={record.systemDefault}>
            <Button size="small" danger icon={<DeleteOutlined />} disabled={record.systemDefault} />
          </Popconfirm>
        </Space>
      ),
    },
  ]

  // 코드 목록에서 extra 값을 스키마 기반으로 표시
  const renderExtra = (extra: string | null) => {
    if (!extra) return '-'
    try {
      const obj = JSON.parse(extra)
      const schema = selectedCodeType?.attributeSchema ?? []
      if (schema.length === 0) return <Text code style={{ fontSize: 11 }}>{extra}</Text>
      return (
        <Space size={4} wrap>
          {schema.map(f => {
            const val = obj[f.key]
            if (val === undefined || val === null || val === '') return null
            return <Tag key={f.key} style={{ fontSize: 11 }}><Text type="secondary">{f.label}:</Text> {String(val)}</Tag>
          })}
        </Space>
      )
    } catch {
      return <Text code style={{ fontSize: 11 }}>{extra}</Text>
    }
  }

  const codeColumns: ColumnsType<Code> = [
    {
      title: t('common.code'), dataIndex: 'code', key: 'code',
      render: (v, r) => (
        <Space size={4}>
          {r.systemDefault && <Tooltip title={t('common.system')}><LockOutlined style={{ color: '#999', fontSize: 11 }} /></Tooltip>}
          <Tag>{v}</Tag>
        </Space>
      ),
    },
    { title: t('common.name'), dataIndex: 'name', key: 'name' },
    { title: t('common.sortOrder'), dataIndex: 'sortOrder', key: 'sortOrder', width: 70, align: 'center' },
    { title: t('code.extra'), key: 'extra', render: (_, r) => renderExtra(r.extra) },
    {
      title: '', key: 'actions', width: 80, align: 'center',
      render: (_, record) => (
        <Space>
          <Button size="small" icon={<EditOutlined />} disabled={record.systemDefault} onClick={() => setCodeModal({ open: true, editing: record })} />
          <Popconfirm title={t('common.deleteConfirm')} onConfirm={() => deleteCode.mutate(record.id)} disabled={record.systemDefault}>
            <Button size="small" danger icon={<DeleteOutlined />} disabled={record.systemDefault} />
          </Popconfirm>
        </Space>
      ),
    },
  ]

  const handleCodeTypeOk = (values: CodeTypeRequest) => {
    if (codeTypeModal.editing) {
      updateCodeType.mutate({ id: codeTypeModal.editing.id, data: values })
    } else {
      createCodeType.mutate(values)
    }
  }

  const handleCodeOk = (values: CodeRequest) => {
    if (codeModal.editing) {
      updateCode.mutate({ id: codeModal.editing.id, data: values })
    } else {
      createCode.mutate(values)
    }
  }

  const importCodeTypeMutation = useMutation({
    mutationFn: (file: File) => codeTypeApi.importCsv(file),
    onSuccess: (result: ImportResult) => { queryClient.invalidateQueries({ queryKey: ['codeTypes'] }); messageApi.success(t('message.importSuccess', { created: result.created, skipped: result.skipped })) },
    onError: () => messageApi.error(t('message.importFail')),
  })

  const importCodeMutation = useMutation({
    mutationFn: (file: File) => codeApi.importCsv(selectedCodeType!.id, file),
    onSuccess: (result: ImportResult) => { queryClient.invalidateQueries({ queryKey: ['codes', selectedCodeType?.id] }); messageApi.success(t('message.importSuccess', { created: result.created, skipped: result.skipped })) },
    onError: () => messageApi.error(t('message.importFail')),
  })

  const handleCodeTypeImport = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0]; e.target.value = ''
    if (file) importCodeTypeMutation.mutate(file)
  }

  const handleCodeImport = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0]; e.target.value = ''
    if (file) importCodeMutation.mutate(file)
  }

  return (
    <>
      {contextHolder}
      <input type="file" accept=".csv" ref={codeTypeImportRef} style={{ display: 'none' }} onChange={handleCodeTypeImport} />
      <input type="file" accept=".csv" ref={codeImportRef} style={{ display: 'none' }} onChange={handleCodeImport} />
      <Row gutter={16}>
        <Col span={10}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 12 }}>
            <Title level={5} style={{ margin: 0 }}>{t('codeType.title')}</Title>
            <Space>
              <Button size="small" icon={<DownloadOutlined />} onClick={() => codeTypeApi.exportCsv()}>{t('common.export')}</Button>
              <Button size="small" icon={<UploadOutlined />} loading={importCodeTypeMutation.isPending} onClick={() => codeTypeImportRef.current?.click()}>{t('common.import')}</Button>
              <Button type="primary" size="small" icon={<PlusOutlined />} onClick={() => setCodeTypeModal({ open: true, editing: null })}>{t('common.register')}</Button>
            </Space>
          </div>
          <Table
            rowKey="id"
            dataSource={codeTypes}
            columns={codeTypeColumns}
            loading={loadingTypes}
            size="small"
            pagination={false}
            rowClassName={r => r.id === selectedCodeType?.id ? 'ant-table-row-selected' : ''}
            onRow={r => ({ onClick: () => setSelectedCodeTypeId(r.id), style: { cursor: 'pointer' } })}
          />
        </Col>

        <Col span={14}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 12 }}>
            <Title level={5} style={{ margin: 0 }}>
              {t('code.title')}
              {selectedCodeType && <Tag color="blue" style={{ marginLeft: 8 }}>{selectedCodeType.name}</Tag>}
            </Title>
            <Space>
              <Button size="small" icon={<DownloadOutlined />} disabled={!selectedCodeType}
                onClick={() => codeApi.exportCsv(selectedCodeType!.id, `${selectedCodeType!.code}-codes.csv`)}>{t('common.export')}</Button>
              <Button size="small" icon={<UploadOutlined />} disabled={!selectedCodeType} loading={importCodeMutation.isPending}
                onClick={() => codeImportRef.current?.click()}>{t('common.import')}</Button>
              <Button type="primary" size="small" icon={<PlusOutlined />} disabled={!selectedCodeType}
                onClick={() => setCodeModal({ open: true, editing: null })}>{t('common.register')}</Button>
            </Space>
          </div>
          <Table
            rowKey="id"
            dataSource={selectedCodeType ? codes : []}
            columns={codeColumns}
            loading={loadingCodes}
            size="small"
            pagination={false}
            locale={{ emptyText: selectedCodeType ? t('code.empty') : t('code.selectType') }}
          />
        </Col>
      </Row>

      <CodeTypeFormModal
        open={codeTypeModal.open}
        editing={codeTypeModal.editing}
        codeTypes={codeTypes}
        onOk={handleCodeTypeOk}
        onCancel={() => setCodeTypeModal({ open: false, editing: null })}
        loading={createCodeType.isPending || updateCodeType.isPending}
      />
      <CodeFormModal
        open={codeModal.open}
        editing={codeModal.editing}
        codeType={selectedCodeType}
        codeTypes={codeTypes}
        onOk={handleCodeOk}
        onCancel={() => setCodeModal({ open: false, editing: null })}
        loading={createCode.isPending || updateCode.isPending}
      />
    </>
  )
}
