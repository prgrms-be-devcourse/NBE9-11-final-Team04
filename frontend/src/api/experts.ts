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

export const expertsApi = {
  getList: (techStack?: string) =>
    unwrap(
      apiClient.get<ApiResponse<{ content: ExpertProfileSummary[] }>>('/experts', {
        params: { techStack, size: 20 },
      }),
    ),

  requestMatch: (expertProfileId: number, ideaId: number) =>
    unwrap(
      apiClient.post<ApiResponse<ExpertMatchResponse>>(
        `/matches/experts/${expertProfileId}`,
        { ideaId },
      ),
    ),
}
