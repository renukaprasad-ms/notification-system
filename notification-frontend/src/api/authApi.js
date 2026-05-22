import apiClient from './axios'

export const signup = ({ email, password, fullName }) => {
  return apiClient.post('/auth/signup', {
    email,
    password,
    fullName,
  })
}

export const loginWithPassword = ({ email, password }) => {
  return apiClient.post('/auth/login', {
    email,
    loginType: 'EMAIL_PASSWORD',
    password,
  })
}

export const sendLoginOtp = ({ email }) => {
  return apiClient.post('/auth/login/otp', { email })
}

export const sendForgotPasswordOtp = ({ email }) => {
  return apiClient.post('/auth/forgot-password/otp', { email })
}

export const verifyLoginOtp = ({ email, otp }) => {
  return apiClient.post('/auth/login', {
    email,
    loginType: 'EMAIL_OTP',
    otp,
  })
}

export const resetPassword = ({ email, otp, newPassword }) => {
  return apiClient.post('/auth/reset-password', {
    email,
    otp,
    newPassword,
  })
}

export const refreshToken = () => {
  return apiClient.post('/auth/refresh')
}

export const logout = () => {
  return apiClient.post('/auth/logout')
}

export const getProfile = () => {
  return apiClient.get('/users/me')
}
