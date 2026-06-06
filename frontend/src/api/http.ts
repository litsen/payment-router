import axios from 'axios'
import { ElMessage } from 'element-plus'

export const http = axios.create({
  baseURL: '/',
  timeout: 10000
})

http.interceptors.request.use(config => {
  const token = localStorage.getItem('payment-router-token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

http.interceptors.response.use(
  response => response.data,
  error => {
    const status = error.response?.status
    if (status === 401) {
      if (error.config?.url === '/api/system-settings') {
        return Promise.reject(error)
      }
      localStorage.removeItem('payment-router-token')
      if (location.pathname !== '/login') {
        location.href = '/login'
      }
    } else if (status === 403) {
      ElMessage.error('权限不足')
    } else if (error.response?.data?.message) {
      ElMessage.error(error.response.data.message)
    }
    return Promise.reject(error)
  }
)
