import axios from 'axios'
import { ElMessage } from 'element-plus/es/components/message/index'

import router from '@/router'
import { useAuthStore } from '@/stores/auth'
import type { ApiResponse } from '@/types/auth'

const request = axios.create({
  baseURL: '/api',
  timeout: 10000,
})

let lastErrorKey = ''
let lastErrorAt = 0

function showErrorMessage(key: string, message: string, type: 'error' | 'warning' = 'error') {
  const now = Date.now()
  if (key === lastErrorKey && now - lastErrorAt < 1200) {
    return
  }
  lastErrorKey = key
  lastErrorAt = now
  if (type === 'warning') {
    ElMessage.warning(message)
  } else {
    ElMessage.error(message)
  }
}

request.interceptors.request.use((config) => {
  const token = localStorage.getItem('campus_event_token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config

})

request.interceptors.response.use(
  (response): any => {
    const body = response.data as ApiResponse<unknown>
    return body.data
  },
  (error) => {
    const status = error.response?.status
    const message = error.response?.data?.message || '请求失败，请稍后重试'

    if (status === 401) {
      const authStore = useAuthStore()
      authStore.clearSession()
      if (router.currentRoute.value.path !== '/login') {
        void router.push({ path: '/login', query: { redirect: router.currentRoute.value.fullPath } })
      }
      showErrorMessage('401', message || '登录已失效，请重新登录')
    } else if (status === 400) {
      showErrorMessage(`400:${message}`, message)
    } else if (status === 403) {
      showErrorMessage('403', message || '无权限访问')
    } else if (status === 409) {
      showErrorMessage(`409:${message}`, message || '业务冲突', 'warning')
    } else if (!error.response) {
      showErrorMessage('network', '网络异常，请检查后端服务是否启动')
    } else if (status >= 500) {
      const requestUrl = error.config?.url || ''
      const isAiRequest = requestUrl.startsWith('/ai/')
      showErrorMessage(isAiRequest ? `ai:${message}` : '500', isAiRequest ? message : '系统繁忙，请稍后重试')
    } else {
      showErrorMessage(`${status}:${message}`, message)
    }

    return Promise.reject(error)
  },
)

export default request
