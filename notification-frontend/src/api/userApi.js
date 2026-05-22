import apiClient from './axios'

export const getAllUsers = () => {
  return apiClient.get('/users')
}
