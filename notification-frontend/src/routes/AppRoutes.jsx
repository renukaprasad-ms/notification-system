import { Navigate, Route, Routes } from 'react-router-dom'
import ProtectedRoute from '../components/common/ProtectedRoute.jsx'
import PublicRoute from '../components/common/PublicRoute.jsx'
import MainLayout from '../layouts/MainLayout.jsx'
import AdminPage from '../pages/AdminPage.jsx'
import DashboardPage from '../pages/DashboardPage.jsx'
import ForgotPasswordPage from '../pages/ForgotPasswordPage.jsx'
import LoginPage from '../pages/LoginPage.jsx'
import NotificationsPage from '../pages/NotificationsPage.jsx'
import ProfilePage from '../pages/ProfilePage.jsx'
import RegisterPage from '../pages/RegisterPage.jsx'

function AppRoutes() {
  return (
    <Routes>
      <Route path="/" element={<Navigate to="/login" replace />} />
      <Route element={<PublicRoute />}>
        <Route path="/login" element={<LoginPage />} />
        <Route path="/signup" element={<RegisterPage />} />
        <Route path="/forgot-password" element={<ForgotPasswordPage />} />
      </Route>
      <Route element={<ProtectedRoute allowedRoles={['USER', 'ADMIN']} />}>
        <Route element={<MainLayout />}>
          <Route path="/dashboard" element={<DashboardPage />} />
          <Route path="/notifications" element={<NotificationsPage />} />
          <Route path="/profile" element={<ProfilePage />} />
        </Route>
      </Route>
      <Route element={<ProtectedRoute allowedRoles={['ADMIN']} />}>
        <Route element={<MainLayout />}>
          <Route path="/admin" element={<AdminPage />} />
        </Route>
      </Route>
      <Route path="*" element={<Navigate to="/notifications" replace />} />
    </Routes>
  )
}

export default AppRoutes
