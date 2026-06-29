import axios, { type AxiosError, type InternalAxiosRequestConfig } from 'axios'
import { API_BASE_URL, TOKEN_KEYS } from '@/utils/constants'
import type { ApiResponse } from '@/types/api'
import type { TokenResponse } from '@/types/auth'
import { useAuthStore } from '@/store/authStore'

let isRefreshing = false
let refreshQueue: Array<() => void> = []

function getAccessToken() {
  if (typeof window === 'undefined') return null
  return localStorage.getItem(TOKEN_KEYS.ACCESS)
}

export function setTokens(tokens: TokenResponse) {
  localStorage.setItem(TOKEN_KEYS.ACCESS, tokens.accessToken)
  localStorage.setItem(TOKEN_KEYS.REFRESH, tokens.refreshToken)
}

export function clearTokens() {
  localStorage.removeItem(TOKEN_KEYS.ACCESS)
  localStorage.removeItem(TOKEN_KEYS.REFRESH)
}

export const apiClient = axios.create({
  baseURL: API_BASE_URL,
  headers: { 'Content-Type': 'application/json' },
  withCredentials: true,
})

apiClient.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  const token = getAccessToken()
  if (token) config.headers.Authorization = `Bearer ${token}`
  if (config.data instanceof FormData) {
    delete config.headers['Content-Type']
  }
  return config
})

apiClient.interceptors.response.use(
  (response) => response,
  async (error: AxiosError<ApiResponse<null>>) => {
    const originalRequest = error.config as InternalAxiosRequestConfig & { _retry?: boolean }
    if (error.response?.status !== 401 || originalRequest._retry) return Promise.reject(error)

    // 갱신 진행 중이면 큐에 대기
    if (isRefreshing) {
      return new Promise<void>((resolve) => {
        refreshQueue.push(resolve)
      }).then(() => apiClient(originalRequest))
    }

    originalRequest._retry = true
    isRefreshing = true

    try {
      // refreshToken은 httpOnly 쿠키로 관리 — body 없이 전송하면 쿠키가 자동으로 함께 전송됨
      const { data } = await axios.post<ApiResponse<TokenResponse>>(
        `${API_BASE_URL}/auth/token-refresh`,
        null,
        { withCredentials: true },
      )
      // localStorage도 새 토큰으로 동기화 — request 인터셉터가 Authorization 헤더에 사용
      setTokens(data.data)
      refreshQueue.forEach((resolve) => resolve())
      refreshQueue = []
      return apiClient(originalRequest)
    } catch {
      refreshQueue = []
      useAuthStore.getState().logout()
      if (typeof window !== 'undefined') window.location.href = '/login'
      return Promise.reject(error)
    } finally {
      isRefreshing = false
    }
  },
)

export async function unwrap<T>(promise: Promise<{ data: ApiResponse<T> }>): Promise<T> {
  const { data } = await promise
  if (!data.success) throw new Error(data.message ?? '요청에 실패했습니다.')
  return data.data
}
