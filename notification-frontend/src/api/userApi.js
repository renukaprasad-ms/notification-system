import apiClient from './axios'

const MATCH_ALL_SEARCH = '__all__'

export const getAllUsers = ({ page = 0, size = 20, search = '' } = {}) => {
  return apiClient.get('/users', {
    params: {
      page,
      size,
      search: search.trim() || MATCH_ALL_SEARCH,
    },
  })
}
