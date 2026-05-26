import axios from 'axios'
import keycloak from '../auth/keycloak'

const http = axios.create({ baseURL: '/api', withCredentials: true })

// 401 시 Keycloak 토큰 갱신 후 세션 재발급 → 요청 재시도
http.interceptors.response.use(
  res => res,
  async error => {
    const original = error.config
    if (error.response?.status === 401 && !original._retry) {
      original._retry = true
      try {
        await keycloak.updateToken(10)
        await http.post('/auth/session', null, {
          headers: { Authorization: `Bearer ${keycloak.token}` },
        })
        return http(original)
      } catch {
        keycloak.logout()
      }
    }
    return Promise.reject(error)
  }
)

export default http
