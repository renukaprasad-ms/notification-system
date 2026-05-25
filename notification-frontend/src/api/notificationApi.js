import apiClient from './axios'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || '/api'
const STREAM_CLIENT_ID_KEY = 'notification_stream_client_id'
const MATCH_ALL_SEARCH = '__all__'

export const sendNotificationToAll = (payload) => {
  return apiClient.post('/notifications/send-all', payload)
}

export const sendNotificationToSelected = (payload) => {
  return apiClient.post('/notifications/send-selected', payload)
}

export const getMyNotifications = ({ page = 0, size = 20, search = '' } = {}) => {
  return apiClient.get('/notifications/me', {
    params: {
      page,
      size,
      search: search.trim() || MATCH_ALL_SEARCH,
    },
  })
}

export const getUnreadNotificationCount = () => {
  return apiClient.get('/notifications/me/unread-count')
}

export const getAdminNotificationOverview = () => {
  return apiClient.get('/notifications/admin/overview')
}

export const markNotificationViewed = (recipientId) => {
  return apiClient.patch(`/notifications/${recipientId}/viewed`)
}

export const markNotificationRead = (recipientId) => {
  return apiClient.patch(`/notifications/${recipientId}/read`)
}

export const deleteNotification = (notificationId) => {
  return apiClient.delete(`/notifications/${notificationId}`)
}

export const getNotificationStreamUrl = () => {
  if (typeof window === 'undefined') {
    return `${API_BASE_URL}/notifications/stream`
  }

  let clientId = window.sessionStorage.getItem(STREAM_CLIENT_ID_KEY)
  if (!clientId) {
    clientId = window.crypto?.randomUUID?.() || `client-${Date.now()}-${Math.random().toString(16).slice(2)}`
    window.sessionStorage.setItem(STREAM_CLIENT_ID_KEY, clientId)
  }

  const streamUrl = new URL(`${API_BASE_URL}/notifications/stream`, window.location.origin)
  streamUrl.searchParams.set('clientId', clientId)
  return streamUrl.toString()
}
