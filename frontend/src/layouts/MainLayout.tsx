import { useState } from 'react'
import { Link, Outlet, useLocation } from 'react-router-dom'
import { Avatar, Dropdown, Layout, Menu, Typography } from 'antd'
import { AppstoreOutlined, ApartmentOutlined, ApiOutlined, LogoutOutlined, UserOutlined } from '@ant-design/icons'
import { useTranslation } from 'react-i18next'
import { useAuth } from '../auth/AuthProvider'

const { Sider, Content, Header } = Layout

export default function MainLayout() {
  const [collapsed, setCollapsed] = useState(false)
  const location = useLocation()
  const { username, logout } = useAuth()
  const { t, i18n } = useTranslation()

  const toggleLang = () => {
    const next = i18n.language === 'ko' ? 'en' : 'ko'
    i18n.changeLanguage(next)
    localStorage.setItem('lang', next)
  }

  const menuItems = [
    {
      key: '/apps',
      icon: <ApartmentOutlined />,
      label: <Link to="/apps">{t('app.title')}</Link>,
    },
    {
      key: '/codes',
      icon: <AppstoreOutlined />,
      label: <Link to="/codes">{t('codeType.title')}</Link>,
    },
    {
      key: '/connectors',
      icon: <ApiOutlined />,
      label: <Link to="/connectors">{t('connector.title')}</Link>,
    },
  ]

  return (
    <Layout style={{ minHeight: '100vh' }}>
      <Sider collapsible collapsed={collapsed} onCollapse={setCollapsed}>
        <div style={{ height: 32, margin: 16, background: 'rgba(255,255,255,0.15)', borderRadius: 6, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
          {!collapsed && <span style={{ color: '#fff', fontWeight: 700, fontSize: 14 }}>Vanilla2Hub</span>}
        </div>
        <Menu theme="dark" mode="inline" selectedKeys={[location.pathname]} items={menuItems} />
      </Sider>
      <Layout>
        <Header style={{ background: '#fff', padding: '0 24px', borderBottom: '1px solid #f0f0f0', display: 'flex', alignItems: 'center', justifyContent: 'flex-end', gap: 16 }}>
          <Typography.Text
            style={{ cursor: 'pointer', fontSize: 13, color: '#595959' }}
            onClick={toggleLang}
          >
            {i18n.language === 'ko' ? 'EN' : '한국어'}
          </Typography.Text>
          <Dropdown
            menu={{ items: [{ key: 'logout', icon: <LogoutOutlined />, label: t('common.logout'), onClick: logout }] }}
            placement="bottomRight"
          >
            <div style={{ display: 'flex', alignItems: 'center', gap: 8, cursor: 'pointer' }}>
              <Avatar size="small" icon={<UserOutlined />} />
              <Typography.Text>{username}</Typography.Text>
            </div>
          </Dropdown>
        </Header>
        <Content style={{ margin: 24 }}>
          <Outlet />
        </Content>
      </Layout>
    </Layout>
  )
}
