import { apiClient, unwrap } from '@/api/client'
import type { ApiResponse } from '@/types/api'
import type {
  BusinessRegistrationRequest,
  BusinessRegistrationResponse,
  UpdatePasswordRequest,
  UpdateProfileRequest,
  UpdateUserRequest,
  User,
} from '@/types/user'

export const usersApi = {
  getMe: () => unwrap(apiClient.get<ApiResponse<User>>('/users/me')),

  updateMe: (body: UpdateUserRequest) =>
    unwrap(apiClient.patch<ApiResponse<User>>('/users/me', body)),

  updateProfile: (body: UpdateProfileRequest) =>
    unwrap(apiClient.patch<ApiResponse<User>>('/users/me/profile', body)),

  updatePassword: (body: UpdatePasswordRequest) =>
    unwrap(apiClient.patch<ApiResponse<void>>('/users/me/password', body)),

  deleteMe: () => unwrap(apiClient.delete<ApiResponse<void>>('/users/me')),

  getMyBusiness: () =>
    unwrap(apiClient.get<ApiResponse<BusinessRegistrationResponse>>('/users/me/business')),

  registerBusiness: (body: BusinessRegistrationRequest) =>
    unwrap(apiClient.post<ApiResponse<BusinessRegistrationResponse>>('/users/me/business', body)),

  deleteMyBusiness: () =>
    unwrap(apiClient.delete<ApiResponse<void>>('/users/me/business')),

  updateProfileImage: (file: File) => {
    const formData = new FormData()
    formData.append('image', file)
    return unwrap(apiClient.patch<ApiResponse<User>>('/users/me/profile-image', formData, {
      headers: { 'Content-Type': undefined },
    }))
  },
}
