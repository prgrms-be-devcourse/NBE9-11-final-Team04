import { apiClient, unwrap } from '@/api/client'
import type { ApiResponse } from '@/types/api'

export type MilestoneStatus = 'PENDING' | 'IN_PROGRESS' | 'COMPLETED' | 'CANCELLED'
export type CompletionReportType = 'COMPLETION' | 'APPEAL'
export type CompletionReportStatus = 'SUBMITTED' | 'APPROVED' | 'REJECTED'

export interface MilestoneResponse {
  id: number
  ideaId: number
  step: number
  goal: string
  expectedResult: string
  expectedDate: string
  status: MilestoneStatus
  createdAt: string
}

export interface CompletionReportResponse {
  reportId: number
  milestoneId: number
  type: CompletionReportType
  content: string
  fileUrl: string | null
  rejectReason: string | null
  status: CompletionReportStatus
  submittedAt: string
}

export const milestonesApi = {
  getMilestonesByIdea: (ideaId: number) =>
    unwrap(apiClient.get<ApiResponse<MilestoneResponse[]>>(`/milestones/ideas/${ideaId}`)),

  getReports: (milestoneId: number) =>
    unwrap(apiClient.get<ApiResponse<CompletionReportResponse[]>>(`/milestones/${milestoneId}/reports`)),

  submitCompletionReport: (milestoneId: number, content: string, file?: File) => {
    const formData = new FormData()
    formData.append('request', new Blob([JSON.stringify({ content })], { type: 'application/json' }))
    if (file) formData.append('file', file)
    return unwrap(apiClient.post<ApiResponse<CompletionReportResponse>>(
      `/milestones/${milestoneId}/completion-reports`,
      formData,
    ))
  },
}
