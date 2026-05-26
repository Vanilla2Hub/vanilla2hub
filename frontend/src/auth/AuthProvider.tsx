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

    keycloak.init({ checkLoginIframe: false, pkceMethod: 'S256' })
      .then(async (authenticated) => {
        if (!authenticated) {
          keycloak.login()
          return
        }
        await http.post('/auth/session', null, {
          headers: { Authorization: `Bearer ${keycloak.token}` },
        })
        setReady(true)
      })
      .catch((err) => {
        console.error('[Keycloak] init failed:', err)
        keycloak.login()
      })

    timerRef.current = setInterval(() => {
      keycloak.updateToken(60).catch(() => keycloak.logout())
    }, 30_000)

    return () => {
      if (timerRef.current) clearInterval(timerRef.current)
    }
  }, [])

  const logout = () => {
    http.delete('/auth/session').finally(() =>
      keycloak.logout({ redirectUri: window.location.origin })
    )
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
