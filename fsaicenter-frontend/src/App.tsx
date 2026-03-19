import { Routes, Route, Navigate } from 'react-router-dom'
import { Toaster } from 'sonner'
import { TooltipProvider } from '@/components/ui/tooltip'
import { MainLayout } from '@/components/layout/MainLayout'
import { useAuthStore } from '@/stores/auth'
import LoginPage from '@/pages/LoginPage'
import DashboardPage from '@/pages/DashboardPage'
import ModelsPage from '@/pages/ModelsPage'
import ProvidersPage from '@/pages/ProvidersPage'
import ApiKeysPage from '@/pages/ApiKeysPage'
import BillingPage from '@/pages/BillingPage'
import LogsPage from '@/pages/LogsPage'
import UsersPage from '@/pages/UsersPage'
import RolesPage from '@/pages/RolesPage'

function ProtectedRoute({ children }: { children: React.ReactNode }) {
  const token = useAuthStore((s) => s.token)
  if (!token) return <Navigate to="/login" replace />
  return <>{children}</>
}

export default function App() {
  return (
    <TooltipProvider>
      <Routes>
        <Route path="/login" element={<LoginPage />} />
        <Route
          path="/"
          element={
            <ProtectedRoute>
              <MainLayout />
            </ProtectedRoute>
          }
        >
          <Route index element={<Navigate to="/dashboard" replace />} />
          <Route path="dashboard" element={<DashboardPage />} />
          <Route path="models" element={<ModelsPage />} />
          <Route path="providers" element={<ProvidersPage />} />
          <Route path="apikeys" element={<ApiKeysPage />} />
          <Route path="billing" element={<BillingPage />} />
          <Route path="logs" element={<LogsPage />} />
          <Route path="users" element={<UsersPage />} />
          <Route path="roles" element={<RolesPage />} />
        </Route>
      </Routes>
      <Toaster
        theme="dark"
        position="top-right"
        toastOptions={{
          style: {
            background: 'hsl(240 10% 8%)',
            border: '1px solid hsl(240 3.7% 15.9%)',
            color: 'hsl(0 0% 98%)',
          },
        }}
      />
    </TooltipProvider>
  )
}
