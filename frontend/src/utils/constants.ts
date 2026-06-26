export const API_BASE_URL =
  process.env.NEXT_PUBLIC_API_BASE_URL ?? 'https://seedlink.site/api'

export const SSE_BASE_URL =
  process.env.NEXT_PUBLIC_SSE_BASE_URL ?? API_BASE_URL

export const TOKEN_KEYS = {
  ACCESS: 'seedlink_access_token',
  REFRESH: 'seedlink_refresh_token',
} as const
