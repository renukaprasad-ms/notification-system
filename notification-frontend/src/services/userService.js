import { getAllUsers } from '../api/userApi'

export const fetchAllUsers = async (params) => {
  const response = await getAllUsers(params)
  return response.data
}
