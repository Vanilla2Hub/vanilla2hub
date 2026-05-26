import { createContext, useContext, useEffect, useRef, useState, type ReactNode } from 'react'
import { Spin } from 'antd'
import keycloak from './keycloak'
import http from '../api/http'

interface AuthContextType {
  username: string
  logout: () => void
}

const AuthContext = createContext<AuthContextType>({ username: '', logout: () => {} })

// StrictMode 이중 실행 방지용 모듈 레벨 플래그
let initStarted = false

export function AuthProvider({ children }: { children: ReactNode }) {
  const [ready, setReady] = useState(false)
  const timerRef = useRef<ReturnType<typeof setInterval> | null>(null)

  useEffect(() => {
    if (initStarted) return
    initStarted = true

    keycloak.init({ checkLoginIframe: false })
      .then(async (authenticated) => {
        if (!authenticated) {
          keycloak.login({ redirectUri: window.location.origin + '/' })
          return
        }
        await http.post('/auth/session', null, {
          headers: { Authorization: `Bearer ${keycloak.token}` },
        })
        setReady(true)
      })
      .catch((err) => {
        console.error('[Keycloak] init failed:', err)
        keycloak.login({ redirectUri: window.location.origin + '/' })
      })

    timerRef.current = setInterval(() => {
      keycloak.updateToken(60).catch(() => keycloak.logout())
    }, 30_000)

    return () => {
      if (timerRef.current) clearInterval(timerRef.current)
    }
  }, [])

  const logout = () => {
    http.delete('/auth/session').finally(() => {
      // post_logout_redirect_uri 없이 호출 — Keycloak이 Okta에 앱 URL을 전달하지 않음
      const url = new URL(
        `${keycloak.authServerUrl}realms/${keycloak.realm}/protocol/openid-connect/logout`
      )
      url.searchParams.set('client_id', keycloak.clientId!)
      if (keycloak.idToken) url.searchParams.set('id_token_hint', keycloak.idToken)
      window.location.href = url.toString()
    })
  }

  if (!ready) {
    return (
      <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: '100vh' }}>
        <Spin size="large" />
      </div>
    )
  }

  const username = (keycloak.tokenParsed?.preferred_username as string) ?? ''

  return (
    <AuthContext.Provider value={{ username, logout }}>
      {children}
    </AuthContext.Provider>
  )
}

export const useAuth = () => useContext(AuthContext)
