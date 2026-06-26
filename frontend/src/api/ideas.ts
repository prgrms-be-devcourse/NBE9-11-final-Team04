import { apiClient, unwrap } from '@/api/client'
import type { ApiResponse, SliceResponse } from '@/types/api'
import type {
  CreateIdeaRequest,
  IdeaDetail,
  IdeaDraft,
  IdeaListParams,
  IdeaSummary,
  TrustScoreResponse,
} from '@/types/idea'

export const ideasApi = {
  getList: (params: IdeaListParams = {}) =>
    unwrap(
      apiClient.get<ApiResponse<SliceResponse<IdeaSummary>>>('/ideas', {
        params: {
          category: params.category,
          closingSoon: params.closingSoon,
          sort: params.sort ?? 'latest',
          page: params.page ?? 0,
          size: params.size ?? 20,
        },
      }),
    ),

  search: (keyword: string, sort = 'latest', page = 0, size = 20) =>
    unwrap(
      apiClient.get<ApiResponse<SliceResponse<IdeaSummary>>>('/ideas/search', {
        params: { keyword, sort, page, size },
      }),
    ),

  getTop5: () => unwrap(apiClient.get<ApiResponse<IdeaSummary[]>>('/ideas/top5')),

  getMyIdeas: () => unwrap(apiClient.get<ApiResponse<IdeaSummary[]>>('/ideas/me')),

  getById: (ideaId: number) =>
    unwrap(apiClient.get<ApiResponse<IdeaDetail>>(`/ideas/${ideaId}`)),

  getTrustScore: (ideaId: number) =>
    unwrap(apiClient.get<ApiResponse<TrustScoreResponse>>(`/ideas/${ideaId}/trust-score`)),

  create: (body: CreateIdeaRequest) =>
    unwrap(apiClient.post<ApiResponse<IdeaDetail>>('/ideas', body)),

  update: (ideaId: number, body: Partial<CreateIdeaRequest>) =>
    unwrap(apiClient.put<ApiResponse<IdeaDetail>>(`/ideas/${ideaId}`, body)),

  delete: (ideaId: number) => unwrap(apiClient.delete<ApiResponse<void>>(`/ideas/${ideaId}`)),

  getDrafts: () => unwrap(apiClient.get<ApiResponse<IdeaDraft[]>>('/ideas/drafts')),

  createDraft: (body: Partial<CreateIdeaRequest>) =>
    unwrap(apiClient.post<ApiResponse<IdeaDraft>>('/ideas/drafts', body)),

  updateDraft: (draftId: number, body: Partial<CreateIdeaRequest>) =>
    unwrap(apiClient.put<ApiResponse<IdeaDraft>>(`/ideas/drafts/${draftId}`, body)),

  deleteDraft: (draftId: number) =>
    unwrap(apiClient.delete<ApiResponse<void>>(`/ideas/drafts/${draftId}`)),

  publishDraft: (draftId: number, body: CreateIdeaRequest) =>
    unwrap(apiClient.post<ApiResponse<IdeaDetail>>(`/ideas/drafts/${draftId}/publish`, body)),
}
