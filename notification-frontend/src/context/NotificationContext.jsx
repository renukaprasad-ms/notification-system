import { createContext, useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { useAuth } from '../hooks/useAuth'
import { useDebouncedValue } from '../hooks/useDebouncedValue'
import { useSSE } from '../hooks/useSSE'
import {
  createNotificationStream,
  deleteAdminNotification,
  fetchMyNotificationById,
  fetchMyNotifications,
  fetchUnreadNotificationCount,
  markAsRead,
  markAsViewed,
} from '../services/notificationService'
import { getApiErrorMessage } from '../utils/apiError'

export const NotificationContext = createContext(null)

const sortNotifications = (notifications) =>
  [...notifications].sort((left, right) => new Date(right.createdAt).getTime() - new Date(left.createdAt).getTime())

const matchesNotificationSearch = (notification, searchQuery) => {
  const normalizedSearch = searchQuery.trim().toLowerCase()

  if (!normalizedSearch) {
    return true
  }

  return [
    notification.title,
    notification.type,
    notification.priority,
  ]
    .filter(Boolean)
    .some((value) => value.toLowerCase().includes(normalizedSearch))
}

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
  const [isLoadingMoreNotifications, setIsLoadingMoreNotifications] = useState(false)
  const [notificationsError, setNotificationsError] = useState('')
  const [isStreamConnected, setIsStreamConnected] = useState(false)
  const [notificationSearchQuery, setNotificationSearchQuery] = useState('')
  const [notificationPageInfo, setNotificationPageInfo] = useState({
    page: 0,
    size: 20,
    totalItems: 0,
    totalPages: 0,
    hasNext: false,
    hasPrevious: false,
  })
  const notificationsRef = useRef([])
  const viewedInFlightRef = useRef(new Set())
  const readInFlightRef = useRef(new Set())
  const deleteInFlightRef = useRef(new Set())
  const detailInFlightRef = useRef(new Set())
  const debouncedNotificationSearchQuery = useDebouncedValue(notificationSearchQuery, 300)

  useEffect(() => {
    notificationsRef.current = notifications
  }, [notifications])

  const loadNotifications = useCallback(async ({ page = 0, append = false, search = debouncedNotificationSearchQuery } = {}) => {
    if (!isAuthenticated) {
      setNotifications([])
      setUnreadCount(0)
      setIsLoadingNotifications(false)
      setIsLoadingMoreNotifications(false)
      setNotificationPageInfo({
        page: 0,
        size: 20,
        totalItems: 0,
        totalPages: 0,
        hasNext: false,
        hasPrevious: false,
      })
      return
    }

    if (append) {
      setIsLoadingMoreNotifications(true)
    } else {
      setIsLoadingNotifications(true)
    }

    try {
      const notificationPromise = fetchMyNotifications({
        page,
        size: notificationPageInfo.size,
        search,
      })
      const unreadPromise = page === 0 ? fetchUnreadNotificationCount() : Promise.resolve(null)
      const [notificationsResponse, unreadResponse] = await Promise.all([notificationPromise, unreadPromise])

      const nextItems = notificationsResponse.data?.items || []
      setNotifications((current) => {
        if (!append) {
          return sortNotifications(nextItems)
        }

        const existingIds = new Set(current.map((notification) => notification.recipientId))
        const merged = [...current]
        nextItems.forEach((notification) => {
          if (!existingIds.has(notification.recipientId)) {
            merged.push(notification)
          }
        })
        return sortNotifications(merged)
      })
      setNotificationPageInfo({
        page: notificationsResponse.data?.page ?? page,
        size: notificationsResponse.data?.size ?? notificationPageInfo.size,
        totalItems: notificationsResponse.data?.totalItems ?? nextItems.length,
        totalPages: notificationsResponse.data?.totalPages ?? 1,
        hasNext: notificationsResponse.data?.hasNext ?? false,
        hasPrevious: notificationsResponse.data?.hasPrevious ?? page > 0,
      })
      if (unreadResponse) {
        setUnreadCount(unreadResponse.data?.unreadCount || 0)
      }
      setNotificationsError('')
    } catch (apiError) {
      setNotificationsError(getApiErrorMessage(apiError, 'Unable to load notifications.'))
    } finally {
      setIsLoadingNotifications(false)
      setIsLoadingMoreNotifications(false)
    }
  }, [debouncedNotificationSearchQuery, isAuthenticated, notificationPageInfo.size])

  useEffect(() => {
    if (isAuthLoading) {
      return
    }

    if (!isAuthenticated) {
      setNotifications([])
      setUnreadCount(0)
      setNotificationsError('')
      setIsLoadingNotifications(false)
      setIsLoadingMoreNotifications(false)
      setIsStreamConnected(false)
      return
    }

    loadNotifications({ page: 0, append: false, search: debouncedNotificationSearchQuery })
  }, [debouncedNotificationSearchQuery, isAuthenticated, isAuthLoading, loadNotifications])

  const handleIncomingNotification = useCallback((event) => {
    const payload = JSON.parse(event.data)

    const nextNotification = {
      ...payload,
      deliveryStatus: 'DELIVERED',
      deliveredAt: payload.createdAt,
      viewedAt: null,
      readAt: null,
      detailLoaded: false,
    }

    setNotifications((current) => {
      if (!matchesNotificationSearch(nextNotification, debouncedNotificationSearchQuery)) {
        return current
      }

      return mergeNotification(current, nextNotification)
    })
    setUnreadCount((current) => current + 1)
    setNotificationPageInfo((current) => ({
      ...current,
      totalItems: current.totalItems + 1,
    }))
  }, [debouncedNotificationSearchQuery])

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

  const loadNotificationDetail = useCallback(async (recipientId) => {
    const current = notificationsRef.current.find((notification) => notification.recipientId === recipientId)
    if (!current) {
      return null
    }

    if (current.detailLoaded) {
      return current
    }

    if (detailInFlightRef.current.has(recipientId)) {
      return current
    }

    detailInFlightRef.current.add(recipientId)

    try {
      const wasUnread = !current.readAt
      const response = await fetchMyNotificationById(recipientId)
      const updated = {
        ...response.data,
        detailLoaded: true,
      }

      setNotifications((existing) => mergeNotification(existing, updated))
      if (wasUnread && updated.readAt) {
        setUnreadCount((currentCount) => Math.max(0, currentCount - 1))
      }

      return updated
    } finally {
      detailInFlightRef.current.delete(recipientId)
    }
  }, [])

  const deleteNotificationById = useCallback(async (notificationId) => {
    if (!notificationId || deleteInFlightRef.current.has(notificationId)) {
      return
    }

    deleteInFlightRef.current.add(notificationId)

    try {
      await deleteAdminNotification(notificationId)

      const removedNotifications = notificationsRef.current.filter((notification) => notification.notificationId === notificationId)
      const removedUnreadCount = removedNotifications.filter((notification) => !notification.readAt).length

      setNotifications((existing) => existing.filter((notification) => notification.notificationId !== notificationId))
      setUnreadCount((currentCount) => Math.max(0, currentCount - removedUnreadCount))
      setNotificationPageInfo((current) => ({
        ...current,
        totalItems: Math.max(0, current.totalItems - removedNotifications.length),
      }))
    } finally {
      deleteInFlightRef.current.delete(notificationId)
    }
  }, [])

  const loadMoreNotifications = useCallback(async () => {
    if (isLoadingMoreNotifications || isLoadingNotifications || !notificationPageInfo.hasNext) {
      return
    }

    await loadNotifications({
      page: notificationPageInfo.page + 1,
      append: true,
      search: debouncedNotificationSearchQuery,
    })
  }, [
    debouncedNotificationSearchQuery,
    isLoadingMoreNotifications,
    isLoadingNotifications,
    loadNotifications,
    notificationPageInfo.hasNext,
    notificationPageInfo.page,
  ])

  const hasMoreNotifications = notificationPageInfo.hasNext

  const value = useMemo(
    () => ({
      notifications,
      unreadCount,
      isLoadingNotifications,
      isLoadingMoreNotifications,
      notificationSearchQuery,
      hasMoreNotifications: notificationPageInfo.hasNext,
      notificationsError,
      isStreamConnected,
      notificationPageInfo,
      refreshNotifications: loadNotifications,
      loadMoreNotifications,
      setNotificationSearchQuery,
      markNotificationViewed,
      markNotificationRead,
      loadNotificationDetail,
      deleteNotificationById,
    }),
    [
      hasMoreNotifications,
      isLoadingMoreNotifications,
      isLoadingNotifications,
      isStreamConnected,
      loadNotifications,
      loadMoreNotifications,
      loadNotificationDetail,
      deleteNotificationById,
      markNotificationRead,
      markNotificationViewed,
      notifications,
      notificationPageInfo,
      notificationSearchQuery,
      notificationsError,
      unreadCount,
    ],
  )

  return <NotificationContext.Provider value={value}>{children}</NotificationContext.Provider>
}
