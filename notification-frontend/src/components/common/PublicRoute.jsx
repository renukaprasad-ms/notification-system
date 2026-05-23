import { Navigate, Outlet, useLocation } from 'react-router-dom'
import { useAuth } from '../../hooks/useAuth'
import Loader from './Loader.jsx'

function PublicRoute() {
  const location = useLocation()
  const { isAuthenticated, isAuthLoading } = useAuth()

  if (isAuthLoading) {
    return <Loader />
  }

  if (isAuthenticated && location.pathname === '/login') {
    return <Outlet />
  }

  if (isAuthenticated) {
    return <Navigate to="/dashboard" replace />
  }

  return <Outlet />
}

export default PublicRoute
