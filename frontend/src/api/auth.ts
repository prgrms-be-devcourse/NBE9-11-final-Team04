import { apiClient, clearTokens, setTokens, unwrap } from '@/api/client'
import type { ApiResponse } from '@/types/api'
import type {
  EmailSendRequest,
  EmailVerifyRequest,
  LoginRequest,
  SignupRequest,
  TokenResponse,
} from '@/types/auth'

export interface OAuthResponse {
  type: 'LOGIN' | 'REGISTER'
  accessToken: string | null
  refreshToken: string | null
  oauthToken: string | null
  email: string | null
  name: string | null
}

export interface OAuthRegisterRequest {
  oauthToken: string
  name: string
  nickname: string
  age: number
}

interface OAuthAuthorizeResponse {
  authorizeUrl: string
}

function getOAuthRedirectUri(provider: 'google' | 'kakao'): string {
  return `${window.location.origin}/auth/callback/${provider}`
}

export const authApi = {
  signup: (body: SignupRequest) =>
    unwrap(apiClient.post<ApiResponse<TokenResponse>>('/auth/signup', body)).then((tokens) => {
      setTokens(tokens)
      return tokens
    }),

  login: (body: LoginRequest) =>
    unwrap(apiClient.post<ApiResponse<TokenResponse>>('/auth/login', body)).then((tokens) => {
      setTokens(tokens)
      return tokens
    }),

  logout: () => unwrap(apiClient.post<ApiResponse<void>>('/auth/logout')).finally(clearTokens),

  sendEmailVerify: (body: EmailSendRequest) =>
    unwrap(apiClient.post<ApiResponse<void>>('/auth/email-verify/send', body)),

  confirmEmailVerify: (body: EmailVerifyRequest) =>
    unwrap(apiClient.post<ApiResponse<void>>('/auth/email-verify/confirm', body)),

  oauthRedirect: async (provider: 'google' | 'kakao') => {
    const redirectUri = getOAuthRedirectUri(provider)
    const res = await unwrap(
      apiClient.get<ApiResponse<OAuthAuthorizeResponse>>(`/auth/oauth2/${provider}/authorize`, {
        params: { redirectUri },
      }),
    )
    window.location.href = res.authorizeUrl
  },

  oauthCallback: (provider: 'google' | 'kakao', code: string, state: string) =>
    unwrap(
      apiClient.post<ApiResponse<OAuthResponse>>(`/auth/oauth2/${provider}`, {
        code,
        redirectUri: getOAuthRedirectUri(provider),
        state,
      }),
    ).then((res) => {
      if (res.type === 'LOGIN' && res.accessToken && res.refreshToken) {
        setTokens({ accessToken: res.accessToken, refreshToken: res.refreshToken })
      }
      return res
    }),

  oauthRegister: (body: OAuthRegisterRequest) =>
    unwrap(apiClient.post<ApiResponse<TokenResponse>>('/auth/oauth2/register', body)).then(
      (tokens) => {
        setTokens(tokens)
        return tokens
      },
    ),
}
