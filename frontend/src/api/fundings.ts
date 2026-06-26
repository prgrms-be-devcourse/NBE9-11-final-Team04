import { apiClient, unwrap } from '@/api/client'
import type { ApiResponse, PageResponse } from '@/types/api'
import type { Funding, FundingDetail, Milestone, SponsorRequest } from '@/types/funding'

export const fundingsApi = {
  getList: (page = 0, size = 20) =>
    unwrap(
      apiClient.get<ApiResponse<PageResponse<FundingDetail>>>('/fundings', {
        params: { page, size },
      }),
    ),

  getById: (fundingId: number) =>
    unwrap(apiClient.get<ApiResponse<FundingDetail>>(`/fundings/${fundingId}`)),

  create: (ideaId: number, body?: { amount?: number }) =>
    unwrap(apiClient.post<ApiResponse<Funding>>(`/fundings/ideas/${ideaId}`, body ?? {})),

  getByIdea: (ideaId: number, page = 0, size = 20) =>
    unwrap(
      apiClient.get<ApiResponse<PageResponse<Funding>>>(`/fundings/ideas/${ideaId}`, {
        params: { page, size },
      }),
    ),

  sponsor: (fundingId: number, body: SponsorRequest) =>
    unwrap(apiClient.post<ApiResponse<void>>(`/fundings/${fundingId}/sponsors`, body)),

  cancelSponsor: (fundingId: number) =>
    unwrap(apiClient.delete<ApiResponse<void>>(`/fundings/${fundingId}/sponsors/me`)),

  getMilestones: (fundingId: number) =>
    unwrap(apiClient.get<ApiResponse<Milestone[]>>(`/fundings/${fundingId}/milestones`)),
}
