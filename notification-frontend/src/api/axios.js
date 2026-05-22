import axios from 'axios'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api'

const apiClient = axios.create({
  baseURL: API_BASE_URL,
  withCredentials: true,
  headers: {
    'Content-Type': 'application/json',
  },
})

let isRefreshing = false
let refreshQueue = []

const resolveRefreshQueue = (error) => {
  refreshQueue.forEach(({ resolve, reject }) => {
    if (error) {
      reject(error)
      return
    }

    resolve()
  })

  refreshQueue = []
}

const logoutAfterRefreshFailure = async () => {
  try {
    await axios.post(`${API_BASE_URL}/auth/logout`, null, { withCredentials: true })
  } catch (error) {
    console.error('Logout failed after refresh failure:', error)
  } finally {
    if (window.location.pathname !== '/login') {
      window.location.href = '/login'
    }
  }
}

apiClient.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config
    const status = error.response?.status
    const requestUrl = originalRequest?.url || ''
    const isAuthRoute = requestUrl.includes('/auth/')

    if (status !== 401 || originalRequest?._retry || isAuthRoute) {
      return Promise.reject(error)
    }

    originalRequest._retry = true

    if (isRefreshing) {
      return new Promise((resolve, reject) => {
        refreshQueue.push({
          resolve: () => resolve(apiClient(originalRequest)),
          reject,
        })
      })
    }

    isRefreshing = true

    try {
      await axios.post(`${API_BASE_URL}/auth/refresh`, null, { withCredentials: true })
      resolveRefreshQueue()
      return apiClient(originalRequest)
    } catch (refreshError) {
      resolveRefreshQueue(refreshError)
      await logoutAfterRefreshFailure()
      return Promise.reject(refreshError)
    } finally {
      isRefreshing = false
    }
  },
)

export default apiClient
