import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom'
import { AuthProvider } from './auth/AuthProvider'
import MainLayout from './layouts/MainLayout'
import CodeManagementPage from './pages/code/CodeManagementPage'
import PostLogoutPage from './pages/PostLogoutPage'

const queryClient = new QueryClient()

export default function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/post-logout" element={<PostLogoutPage />} />
        <Route
          path="/*"
          element={
            <AuthProvider>
              <QueryClientProvider client={queryClient}>
                <Routes>
                  <Route element={<MainLayout />}>
                    <Route index element={<Navigate to="/codes" replace />} />
                    <Route path="/codes" element={<CodeManagementPage />} />
                  </Route>
                </Routes>
              </QueryClientProvider>
            </AuthProvider>
          }
        />
      </Routes>
    </BrowserRouter>
  )
}
