export interface TokenResponse {
  accessToken: string
  refreshToken: string
}

export interface SignupRequest {
  email: string
  password: string
  name: string
  nickname: string
  age: number
}

export interface LoginRequest {
  email: string
  password: string
}

export interface EmailSendRequest {
  email: string
}

export interface EmailVerifyRequest {
  email: string
  otp: string
}
