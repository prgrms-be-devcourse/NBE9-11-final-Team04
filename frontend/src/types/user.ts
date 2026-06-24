import type { Role, UserStatus } from './enums'

export interface User {
  id: number
  email: string
  name: string
  nickname: string
  age: number
  role: Role
  status: UserStatus
  intro: string | null
  portfolioUrl: string | null
  profileImage: string | null
  createdAt: string
  updatedAt: string
}

export interface UpdateUserRequest {
  nickname: string
}

export interface UpdateProfileRequest {
  intro?: string
  portfolioUrl?: string
}

export interface UpdatePasswordRequest {
  currentPassword: string
  newPassword: string
}

export interface BusinessRegistrationRequest {
  businessNumber: string
  representativeName: string
  openDate: string
}

export interface BusinessRegistrationResponse {
  businessNumber: string
  verified: boolean
  verifiedAt: string | null
}
