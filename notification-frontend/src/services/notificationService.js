import {
  getAdminNotificationOverview,
  getNotificationStreamUrl,
  getMyNotifications,
  getUnreadNotificationCount,
  markNotificationRead,
  markNotificationViewed,
  sendNotificationToAll,
  sendNotificationToSelected,
} from '../api/notificationApi'

export const createNotificationForAll = async (payload) => {
  const response = await sendNotificationToAll(payload)
  return response.data
}

export const createNotificationForSelected = async (payload) => {
  const response = await sendNotificationToSelected(payload)
  return response.data
}

export const fetchMyNotifications = async () => {
  const response = await getMyNotifications()
  return response.data
}

export const fetchUnreadNotificationCount = async () => {
  const response = await getUnreadNotificationCount()
  return response.data
}

export const fetchAdminNotificationOverview = async () => {
  const response = await getAdminNotificationOverview()
  return response.data
}

export const markAsViewed = async (recipientId) => {
  const response = await markNotificationViewed(recipientId)
  return response.data
}

export const markAsRead = async (recipientId) => {
  const response = await markNotificationRead(recipientId)
  return response.data
}

export const createNotificationStream = () => {
  return new EventSource(getNotificationStreamUrl(), { withCredentials: true })
}
