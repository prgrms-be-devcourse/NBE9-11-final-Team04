import { apiClient, unwrap } from '@/api/client'
import type { ApiResponse } from '@/types/api'

export interface ExpertProfileSummary {
  expertProfileId: number
  userId: number
  name: string
  nickname: string
  techStack: string
  career: string
}

export interface ExpertMatchResponse {
  matchId: number
  ideaId: number
  expertProfileId: number
  expertUserId: number
  status: string
  requestedAt: string
  respondedAt: string | null
  rejectReason: string | null
}

export type ExpertStatus = 'ACTIVE' | 'SUSPENDED' | 'PENDING_VERIFICATION' | 'DEMOTED'
export type QualificationType = 'BUSINESS_REGISTRATION' | 'NATIONAL_QUALIFICATION'
export type TechStack = 'TECH' | 'LIFE' | 'HEALTH' | 'EDUCATION' | 'ENVIRONMENT' | 'CULTURE' | 'ETC'

export interface ExpertProfileResponse {
  expertProfileId: number
  userId: number
  qualificationType: QualificationType
  qualificationNumber: string
  verified: boolean
  status: ExpertStatus
  techStack: TechStack | null
  portfolioUrl: string | null
  career: string | null
}

export interface ExpertProfileUpdateRequest {
  techStack: TechStack
  portfolioUrl?: string
  career?: string
}

export const expertsApi = {
  getList: (techStack?: string) =>
    unwrap(
      apiClient.get<ApiResponse<{ content: ExpertProfileSummary[] }>>('/experts', {
        params: { techStack, size: 20 },
      }),
    ),

  getMyProfile: () =>
    unwrap(apiClient.get<ApiResponse<ExpertProfileResponse>>('/experts/me')),

  updateMyProfile: (body: ExpertProfileUpdateRequest) =>
    unwrap(apiClient.patch<ApiResponse<ExpertProfileResponse>>('/experts/profile', body)),

  requestMatch: (expertProfileId: number, ideaId: number) =>
    unwrap(
      apiClient.post<ApiResponse<ExpertMatchResponse>>(
        `/matches/experts/${expertProfileId}`,
        { ideaId },
      ),
    ),
}
