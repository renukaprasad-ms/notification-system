import {
  loginWithPassword,
  logout,
  sendLoginOtp,
  signup,
  verifyLoginOtp,
} from '../api/authApi'

export const signupUser = async (payload) => {
  const response = await signup(payload)
  return response.data
}

export const loginUserWithPassword = async (payload) => {
  const response = await loginWithPassword(payload)
  return response.data
}

export const sendOtpForLogin = async (payload) => {
  const response = await sendLoginOtp(payload)
  return response.data
}

export const verifyOtpForLogin = async (payload) => {
  const response = await verifyLoginOtp(payload)
  return response.data
}

export const logoutUser = async () => {
  const response = await logout()
  return response.data
}
