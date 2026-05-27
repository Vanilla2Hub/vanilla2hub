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
    http.delete('/auth/session').finally(async () => {
      const base = keycloak.authServerUrl?.endsWith('/') ? keycloak.authServerUrl : `${keycloak.authServerUrl}/`

      // Keycloak 세션을 POST로 직접 종료 (브라우저 리다이렉트 없음 → Okta 재전달 없음)
      try {
        const body = new URLSearchParams()
        body.append('client_id', keycloak.clientId!)
        if (keycloak.idToken) body.append('id_token_hint', keycloak.idToken)
        await fetch(`${base}realms/${keycloak.realm}/protocol/openid-connect/logout`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
          body: body.toString(),
        })
      } catch {
        // Keycloak 세션 종료 실패 시에도 Okta 로그아웃 진행
      }

      // Okta 세션 종료 — /login/signout은 id_token_hint 없이도 동작
      window.location.href = 'https://integrator-2776408.okta.com/login/signout'
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
