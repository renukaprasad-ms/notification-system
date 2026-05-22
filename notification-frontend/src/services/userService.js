import { getAllUsers } from '../api/userApi'

export const fetchAllUsers = async () => {
  const response = await getAllUsers()
  return response.data
}
