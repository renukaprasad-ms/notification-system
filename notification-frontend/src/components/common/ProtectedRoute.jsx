import { Navigate, Outlet, useLocation } from 'react-router-dom'
import { useAuth } from '../../hooks/useAuth'
import Loader from './Loader.jsx'

function ProtectedRoute({ allowedRoles }) {
  const location = useLocation()
  const { isAuthenticated, isAuthLoading, hasRole } = useAuth()

  if (isAuthLoading) {
    return <Loader />
  }

  if (!isAuthenticated) {
    return <Navigate to="/login" replace state={{ from: location }} />
  }

  if (!hasRole(allowedRoles)) {
    return <Navigate to="/dashboard" replace />
  }

  return <Outlet />
}

export default ProtectedRoute
