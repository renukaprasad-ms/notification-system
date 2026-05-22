export function getApiErrorMessage(apiError, fallback = 'Something went wrong.') {
  return apiError?.response?.data?.error_message || apiError?.response?.data?.message || fallback
}
