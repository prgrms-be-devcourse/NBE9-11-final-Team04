import { apiClient, unwrap } from '@/api/client'
import type { ApiResponse, PageResponse } from '@/types/api'
import type { CreateFundingResponse, DepositResponse, Funding, FundingDetail, Milestone, SponsorRequest } from '@/types/funding'

export const fundingsApi = {
  getList: (page = 0, size = 20) =>
    unwrap(
      apiClient.get<ApiResponse<PageResponse<FundingDetail>>>('/fundings', {
        params: { page, size },
      }),
    ),

  getById: (fundingId: number) =>
    unwrap(apiClient.get<ApiResponse<FundingDetail>>(`/fundings/${fundingId}`)),

  openFunding: (ideaId: number) =>
    unwrap(apiClient.post<ApiResponse<Funding>>('/fundings', { ideaId })),

  getByIdea: (ideaId: number) =>
    unwrap(apiClient.get<ApiResponse<FundingDetail>>(`/fundings/${ideaId}`)),

  sponsor: (fundingId: number, body: SponsorRequest) =>
    unwrap(apiClient.post<ApiResponse<CreateFundingResponse>>(`/fundings/${fundingId}/sponsors`, body)),

  cancelSponsor: (fundingId: number) =>
    unwrap(apiClient.delete<ApiResponse<void>>(`/fundings/${fundingId}/sponsors/me`)),

  getMilestones: (fundingId: number) =>
    unwrap(apiClient.get<ApiResponse<Milestone[]>>(`/fundings/${fundingId}/milestones`)),

  getDeposit: (ideaId: number) =>
    unwrap(apiClient.get<ApiResponse<DepositResponse>>(`/fundings/${ideaId}/deposit`)),

  payDeposit: (ideaId: number, amount: number, paymentMethod: 'CARD' | 'VIRTUAL_ACCOUNT') =>
    unwrap(
      apiClient.post<ApiResponse<DepositResponse>>(`/fundings/${ideaId}/deposit`, {
        amount,
        paymentMethod,
      }),
    ),
}
