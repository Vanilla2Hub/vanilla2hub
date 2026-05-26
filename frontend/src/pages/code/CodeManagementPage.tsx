import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Button, Col, Popconfirm, Row, Space, Table, Tag, Typography, message } from 'antd'
import { DeleteOutlined, EditOutlined, PlusOutlined, UnorderedListOutlined } from '@ant-design/icons'
import type { ColumnsType } from 'antd/es/table'
import { codeApi, codeTypeApi } from '../../api/codeApi'
import type { Code, CodeRequest, CodeType, CodeTypeRequest } from '../../api/codeApi'
import CodeTypeFormModal from './CodeTypeFormModal'
import CodeFormModal from './CodeFormModal'

const { Title, Text } = Typography

export default function CodeManagementPage() {
  const queryClient = useQueryClient()
  const [messageApi, contextHolder] = message.useMessage()

  const [codeTypeModal, setCodeTypeModal] = useState<{ open: boolean; editing: CodeType | null }>({ open: false, editing: null })
  const [codeModal, setCodeModal] = useState<{ open: boolean; editing: Code | null }>({ open: false, editing: null })
  const [selectedCodeType, setSelectedCodeType] = useState<CodeType | null>(null)

  // 코드타입 조회
  const { data: codeTypes = [], isLoading: loadingTypes } = useQuery({
    queryKey: ['codeTypes'],
    queryFn: codeTypeApi.getAll,
  })

  // 코드 조회
  const { data: codes = [], isLoading: loadingCodes } = useQuery({
    queryKey: ['codes', selectedCodeType?.id],
    queryFn: () => codeApi.getAll(selectedCodeType!.id),
    enabled: !!selectedCodeType,
  })

  // 코드타입 뮤테이션
  const createCodeType = useMutation({
    mutationFn: (data: CodeTypeRequest) => codeTypeApi.create(data),
    onSuccess: () => { queryClient.invalidateQueries({ queryKey: ['codeTypes'] }); setCodeTypeModal({ open: false, editing: null }); messageApi.success('코드타입이 등록됐습니다.') },
    onError: () => messageApi.error('등록에 실패했습니다.'),
  })
  const updateCodeType = useMutation({
    mutationFn: ({ id, data }: { id: number; data: CodeTypeRequest }) => codeTypeApi.update(id, data),
    onSuccess: () => { queryClient.invalidateQueries({ queryKey: ['codeTypes'] }); setCodeTypeModal({ open: false, editing: null }); messageApi.success('수정됐습니다.') },
    onError: () => messageApi.error('수정에 실패했습니다.'),
  })
  const deleteCodeType = useMutation({
    mutationFn: (id: number) => codeTypeApi.delete(id),
    onSuccess: () => { queryClient.invalidateQueries({ queryKey: ['codeTypes'] }); if (selectedCodeType) setSelectedCodeType(null); messageApi.success('삭제됐습니다.') },
    onError: () => messageApi.error('삭제에 실패했습니다.'),
  })

  // 코드 뮤테이션
  const createCode = useMutation({
    mutationFn: (data: CodeRequest) => codeApi.create(selectedCodeType!.id, data),
    onSuccess: () => { queryClient.invalidateQueries({ queryKey: ['codes', selectedCodeType?.id] }); setCodeModal({ open: false, editing: null }); messageApi.success('코드가 등록됐습니다.') },
    onError: () => messageApi.error('등록에 실패했습니다.'),
  })
  const updateCode = useMutation({
    mutationFn: ({ id, data }: { id: number; data: CodeRequest }) => codeApi.update(selectedCodeType!.id, id, data),
    onSuccess: () => { queryClient.invalidateQueries({ queryKey: ['codes', selectedCodeType?.id] }); setCodeModal({ open: false, editing: null }); messageApi.success('수정됐습니다.') },
    onError: () => messageApi.error('수정에 실패했습니다.'),
  })
  const deleteCode = useMutation({
    mutationFn: (id: number) => codeApi.delete(selectedCodeType!.id, id),
    onSuccess: () => { queryClient.invalidateQueries({ queryKey: ['codes', selectedCodeType?.id] }); messageApi.success('삭제됐습니다.') },
    onError: () => messageApi.error('삭제에 실패했습니다.'),
  })

  const codeTypeColumns: ColumnsType<CodeType> = [
    { title: '코드', dataIndex: 'code', key: 'code', render: v => <Tag>{v}</Tag> },
    { title: '이름', dataIndex: 'name', key: 'name' },
    { title: '순서', dataIndex: 'sortOrder', key: 'sortOrder', width: 70, align: 'center' },
    {
      title: '',
      key: 'actions',
      width: 100,
      align: 'center',
      render: (_, record) => (
        <Space>
          <Button size="small" icon={<UnorderedListOutlined />} onClick={() => setSelectedCodeType(record)} />
          <Button size="small" icon={<EditOutlined />} onClick={() => setCodeTypeModal({ open: true, editing: record })} />
          <Popconfirm title="삭제하시겠습니까?" onConfirm={() => deleteCodeType.mutate(record.id)}>
            <Button size="small" danger icon={<DeleteOutlined />} />
          </Popconfirm>
        </Space>
      ),
    },
  ]

  const codeColumns: ColumnsType<Code> = [
    { title: '코드', dataIndex: 'code', key: 'code', render: v => <Tag>{v}</Tag> },
    { title: '이름', dataIndex: 'name', key: 'name' },
    { title: '순서', dataIndex: 'sortOrder', key: 'sortOrder', width: 70, align: 'center' },
    {
      title: '추가 속성',
      dataIndex: 'extra',
      key: 'extra',
      render: v => v ? <Text code style={{ fontSize: 11 }}>{v}</Text> : '-',
    },
    {
      title: '',
      key: 'actions',
      width: 100,
      align: 'center',
      render: (_, record) => (
        <Space>
          <Button size="small" icon={<EditOutlined />} onClick={() => setCodeModal({ open: true, editing: record })} />
          <Popconfirm title="삭제하시겠습니까?" onConfirm={() => deleteCode.mutate(record.id)}>
            <Button size="small" danger icon={<DeleteOutlined />} />
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

  return (
    <>
      {contextHolder}
      <Row gutter={16}>
        <Col span={10}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 12 }}>
            <Title level={5} style={{ margin: 0 }}>코드타입</Title>
            <Button type="primary" size="small" icon={<PlusOutlined />} onClick={() => setCodeTypeModal({ open: true, editing: null })}>
              등록
            </Button>
          </div>
          <Table
            rowKey="id"
            dataSource={codeTypes}
            columns={codeTypeColumns}
            loading={loadingTypes}
            size="small"
            pagination={false}
            rowClassName={r => r.id === selectedCodeType?.id ? 'ant-table-row-selected' : ''}
            onRow={r => ({ onClick: () => setSelectedCodeType(r), style: { cursor: 'pointer' } })}
          />
        </Col>

        <Col span={14}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 12 }}>
            <Title level={5} style={{ margin: 0 }}>
              코드 목록
              {selectedCodeType && <Tag color="blue" style={{ marginLeft: 8 }}>{selectedCodeType.name}</Tag>}
            </Title>
            <Button
              type="primary"
              size="small"
              icon={<PlusOutlined />}
              disabled={!selectedCodeType}
              onClick={() => setCodeModal({ open: true, editing: null })}
            >
              등록
            </Button>
          </div>
          <Table
            rowKey="id"
            dataSource={selectedCodeType ? codes : []}
            columns={codeColumns}
            loading={loadingCodes}
            size="small"
            pagination={false}
            locale={{ emptyText: selectedCodeType ? '코드가 없습니다.' : '좌측에서 코드타입을 선택하세요.' }}
          />
        </Col>
      </Row>

      <CodeTypeFormModal
        open={codeTypeModal.open}
        editing={codeTypeModal.editing}
        onOk={handleCodeTypeOk}
        onCancel={() => setCodeTypeModal({ open: false, editing: null })}
        loading={createCodeType.isPending || updateCodeType.isPending}
      />
      <CodeFormModal
        open={codeModal.open}
        editing={codeModal.editing}
        onOk={handleCodeOk}
        onCancel={() => setCodeModal({ open: false, editing: null })}
        loading={createCode.isPending || updateCode.isPending}
      />
    </>
  )
}
