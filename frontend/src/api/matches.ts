import { apiClient, unwrap } from '@/api/client'
import type { ApiResponse } from '@/types/api'

export interface MatchResponse {
  matchId: number
  ideaId: number
  expertProfileId: number
  expertUserId: number
  status: 'PENDING' | 'ACCEPTED' | 'REJECTED'
  requestedAt: string
  respondedAt: string | null
  rejectReason: string | null
}

export interface ReviewRequest {
  feasibility: 'POSSIBLE' | 'IMPOSSIBLE'
  expectedPeriod: string
  techStack: string
  riskFactor: string
  opinion: string
}

export interface ReviewResponse {
  reviewId: number
  matchId: number
  ideaId: number
  expertProfileId: number
  feasibility: string
  expectedPeriod: string
  techStack: string
  riskFactor: string
  opinion: string
  createdAt: string
}

export const matchesApi = {
  getMyMatches: () =>
    unwrap(apiClient.get<ApiResponse<MatchResponse[]>>('/matches')),

  respond: (matchId: number, status: 'ACCEPTED' | 'REJECTED', rejectReason?: string) =>
    unwrap(
      apiClient.patch<ApiResponse<MatchResponse>>(`/matches/${matchId}`, {
        status,
        rejectReason: rejectReason ?? null,
      }),
    ),

  createReview: (matchId: number, body: ReviewRequest) =>
    unwrap(
      apiClient.post<ApiResponse<ReviewResponse>>(`/matches/${matchId}/review`, body),
    ),
}
