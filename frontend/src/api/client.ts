import axios, { type AxiosError, type InternalAxiosRequestConfig } from 'axios'
import { API_BASE_URL, TOKEN_KEYS } from '@/utils/constants'
import type { ApiResponse } from '@/types/api'
import type { TokenResponse } from '@/types/auth'

let isRefreshing = false
let refreshQueue: Array<(token: string) => void> = []

function getAccessToken() {
  if (typeof window === 'undefined') return null
  return localStorage.getItem(TOKEN_KEYS.ACCESS)
}

function getRefreshToken() {
  if (typeof window === 'undefined') return null
  return localStorage.getItem(TOKEN_KEYS.REFRESH)
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
  return config
})

apiClient.interceptors.response.use(
  (response) => response,
  async (error: AxiosError<ApiResponse<null>>) => {
    const originalRequest = error.config as InternalAxiosRequestConfig & { _retry?: boolean }
    if (error.response?.status !== 401 || originalRequest._retry) return Promise.reject(error)

    const refreshToken = getRefreshToken()
    if (!refreshToken) {
      clearTokens()
      return Promise.reject(error)
    }

    if (isRefreshing) {
      return new Promise((resolve) => {
        refreshQueue.push((token: string) => {
          originalRequest.headers.Authorization = `Bearer ${token}`
          resolve(apiClient(originalRequest))
        })
      })
    }

    originalRequest._retry = true
    isRefreshing = true

    try {
      const { data } = await axios.post<ApiResponse<TokenResponse>>(
        `${API_BASE_URL}/auth/token-refresh`,
        { refreshToken },
        { withCredentials: true },
      )
      setTokens(data.data)
      refreshQueue.forEach((cb) => cb(data.data.accessToken))
      refreshQueue = []
      originalRequest.headers.Authorization = `Bearer ${data.data.accessToken}`
      return apiClient(originalRequest)
    } catch {
      clearTokens()
      refreshQueue = []
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
