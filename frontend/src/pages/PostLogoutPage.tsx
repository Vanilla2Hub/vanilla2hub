import { useEffect } from 'react'
import { Spin } from 'antd'

const OKTA_LOGOUT_URL = 'https://integrator-2776408.okta.com/oauth2/v1/logout'

export default function PostLogoutPage() {
  useEffect(() => {
    window.location.replace(OKTA_LOGOUT_URL)
  }, [])

  return (
    <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: '100vh' }}>
      <Spin size="large" />
    </div>
  )
}
