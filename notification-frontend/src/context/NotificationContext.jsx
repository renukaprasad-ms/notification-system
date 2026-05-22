import { createContext, useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { useAuth } from '../hooks/useAuth'
import { useSSE } from '../hooks/useSSE'
import {
  createNotificationStream,
  fetchMyNotifications,
  fetchUnreadNotificationCount,
  markAsRead,
  markAsViewed,
} from '../services/notificationService'
import { getApiErrorMessage } from '../utils/apiError'

export const NotificationContext = createContext(null)

const sortNotifications = (notifications) =>
  [...notifications].sort((left, right) => new Date(right.createdAt).getTime() - new Date(left.createdAt).getTime())

const mergeNotification = (notifications, incoming) => {
  const next = notifications.filter((notification) => notification.recipientId !== incoming.recipientId)
  next.unshift(incoming)
  return sortNotifications(next)
}

export function NotificationProvider({ children }) {
  const { isAuthenticated, isAuthLoading } = useAuth()
  const [notifications, setNotifications] = useState([])
  const [unreadCount, setUnreadCount] = useState(0)
  const [isLoadingNotifications, setIsLoadingNotifications] = useState(true)
  const [notificationsError, setNotificationsError] = useState('')
  const [isStreamConnected, setIsStreamConnected] = useState(false)
  const notificationsRef = useRef([])
  const viewedInFlightRef = useRef(new Set())
  const readInFlightRef = useRef(new Set())

  useEffect(() => {
    notificationsRef.current = notifications
  }, [notifications])

  const loadNotifications = useCallback(async () => {
    if (!isAuthenticated) {
      setNotifications([])
      setUnreadCount(0)
      setIsLoadingNotifications(false)
      return
    }

    setIsLoadingNotifications(true)

    try {
      const [notificationsResponse, unreadResponse] = await Promise.all([
        fetchMyNotifications(),
        fetchUnreadNotificationCount(),
      ])

      setNotifications(sortNotifications(notificationsResponse.data || []))
      setUnreadCount(unreadResponse.data?.unreadCount || 0)
      setNotificationsError('')
    } catch (apiError) {
      setNotificationsError(getApiErrorMessage(apiError, 'Unable to load notifications.'))
    } finally {
      setIsLoadingNotifications(false)
    }
  }, [isAuthenticated])

  useEffect(() => {
    if (isAuthLoading) {
      return
    }

    if (!isAuthenticated) {
      setNotifications([])
      setUnreadCount(0)
      setNotificationsError('')
      setIsLoadingNotifications(false)
      setIsStreamConnected(false)
      return
    }

    loadNotifications()
  }, [isAuthenticated, isAuthLoading, loadNotifications])

  const handleIncomingNotification = useCallback((event) => {
    const payload = JSON.parse(event.data)

    const nextNotification = {
      ...payload,
      deliveryStatus: 'DELIVERED',
      deliveredAt: payload.createdAt,
      viewedAt: null,
      readAt: null,
    }

    setNotifications((current) => mergeNotification(current, nextNotification))
    setUnreadCount((current) => current + 1)
  }, [])

  const handleStreamOpen = useCallback(() => {
    setIsStreamConnected(true)
  }, [])

  const handleStreamError = useCallback(() => {
    setIsStreamConnected(false)
  }, [])

  const streamHandlers = useMemo(
    () => ({
      connected: handleStreamOpen,
      notification: handleIncomingNotification,
    }),
    [handleIncomingNotification, handleStreamOpen],
  )

  useSSE({
    enabled: isAuthenticated && !isAuthLoading,
    createConnection: createNotificationStream,
    onOpen: handleStreamOpen,
    onError: handleStreamError,
    handlers: streamHandlers,
  })

  const markNotificationViewed = useCallback(async (recipientId) => {
    const current = notificationsRef.current.find((notification) => notification.recipientId === recipientId)
    if (!current || current.viewedAt || viewedInFlightRef.current.has(recipientId)) {
      return current
    }

    viewedInFlightRef.current.add(recipientId)

    try {
      const response = await markAsViewed(recipientId)
      const updated = response.data

      setNotifications((existing) => mergeNotification(existing, updated))
      return updated
    } finally {
      viewedInFlightRef.current.delete(recipientId)
    }
  }, [])

  const markNotificationRead = useCallback(async (recipientId) => {
    const current = notificationsRef.current.find((notification) => notification.recipientId === recipientId)
    if (!current || current.readAt || readInFlightRef.current.has(recipientId)) {
      return current
    }

    readInFlightRef.current.add(recipientId)

    try {
      const response = await markAsRead(recipientId)
      const updated = response.data

      setNotifications((existing) => mergeNotification(existing, updated))
      setUnreadCount((currentCount) => Math.max(0, currentCount - 1))
      return updated
    } finally {
      readInFlightRef.current.delete(recipientId)
    }
  }, [])

  const value = useMemo(
    () => ({
      notifications,
      unreadCount,
      isLoadingNotifications,
      notificationsError,
      isStreamConnected,
      refreshNotifications: loadNotifications,
      markNotificationViewed,
      markNotificationRead,
    }),
    [
      isLoadingNotifications,
      isStreamConnected,
      loadNotifications,
      markNotificationRead,
      markNotificationViewed,
      notifications,
      notificationsError,
      unreadCount,
    ],
  )

  return <NotificationContext.Provider value={value}>{children}</NotificationContext.Provider>
}
