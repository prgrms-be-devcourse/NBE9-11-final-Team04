import { apiClient, unwrap } from '@/api/client'
import type { ApiResponse } from '@/types/api'
import type { DisputeStatus } from '@/types/enums'

export interface DisputeResponse {
  id: number
  ideaId: number
  reporterId: number
  proposerId: number
  status: DisputeStatus
  reason: string
  evidenceUrl: string | null
  createdAt: string
}

export interface CreateDisputeRequest {
  ideaId: number
  reason: string
  evidenceUrl?: string
}

export interface CreateAppealRequest {
  content: string
  fileUrl?: string
}

export const disputesApi = {
  create: (body: CreateDisputeRequest) =>
    unwrap(apiClient.post<ApiResponse<DisputeResponse>>('/disputes', body)),

  getById: (disputeId: number) =>
    unwrap(apiClient.get<ApiResponse<DisputeResponse>>(`/disputes/${disputeId}`)),

  createAppeal: (disputeId: number, body: CreateAppealRequest) =>
    unwrap(apiClient.post<ApiResponse<void>>(`/disputes/${disputeId}/appeal`, body)),
}
