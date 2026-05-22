import { createContext, useCallback, useEffect, useMemo, useState } from 'react'
import {
  loginUserWithPassword,
  logoutUser,
  signupUser,
  verifyOtpForLogin,
  getCurrentUser,
} from '../services/authService'

export const AuthContext = createContext(null)

const normalizeUser = (response) => response?.data || response

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null)
  const [isAuthLoading, setIsAuthLoading] = useState(true)

  const refreshCurrentUser = useCallback(async () => {
    const response = await getCurrentUser()
    const currentUser = normalizeUser(response)
    setUser(currentUser)
    return currentUser
  }, [])

  useEffect(() => {
    let isMounted = true

    const loadSession = async () => {
      try {
        const response = await getCurrentUser()
        if (isMounted) {
          setUser(normalizeUser(response))
        }
      } catch {
        if (isMounted) {
          setUser(null)
        }
      } finally {
        if (isMounted) {
          setIsAuthLoading(false)
        }
      }
    }

    loadSession()

    return () => {
      isMounted = false
    }
  }, [])

  const loginWithPassword = useCallback(
    async (payload) => {
      await loginUserWithPassword(payload)
      return refreshCurrentUser()
    },
    [refreshCurrentUser],
  )

  const loginWithOtp = useCallback(
    async (payload) => {
      await verifyOtpForLogin(payload)
      return refreshCurrentUser()
    },
    [refreshCurrentUser],
  )

  const signup = useCallback(
    async (payload) => {
      await signupUser(payload)
      return refreshCurrentUser()
    },
    [refreshCurrentUser],
  )

  const logout = useCallback(async () => {
    try {
      await logoutUser()
    } finally {
      setUser(null)
    }
  }, [])

  const hasRole = useCallback(
    (requiredRoles) => {
      if (!requiredRoles?.length) {
        return true
      }

      const userRoles = user?.roles || []
      return requiredRoles.some((role) => userRoles.includes(role))
    },
    [user],
  )

  const value = useMemo(
    () => ({
      user,
      isAuthenticated: Boolean(user),
      isAuthLoading,
      loginWithPassword,
      loginWithOtp,
      signup,
      logout,
      refreshCurrentUser,
      hasRole,
    }),
    [hasRole, isAuthLoading, loginWithOtp, loginWithPassword, logout, refreshCurrentUser, signup, user],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}
