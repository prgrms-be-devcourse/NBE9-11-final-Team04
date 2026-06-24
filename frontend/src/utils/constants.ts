export const API_BASE_URL =
  process.env.NEXT_PUBLIC_API_BASE_URL ?? 'https://api.seedlink.com'

export const TOKEN_KEYS = {
  ACCESS: 'seedlink_access_token',
  REFRESH: 'seedlink_refresh_token',
} as const
