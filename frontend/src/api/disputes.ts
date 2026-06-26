import { apiClient, unwrap } from '@/api/client'
import type { ApiResponse, PageResponse } from '@/types/api'
import type { DisputeStatus } from '@/types/enums'

export type TargetType = 'IDEA' | 'FUNDING' | 'MILESTONE' | 'EXPERT_MATCH' | 'USER'

export type DisputeCategory =
  | 'MILESTONE_NEGLIGENCE'
  | 'FUND_MISUSE'
  | 'FALSE_COMPLETION_REPORT'
  | 'FALSE_APPEAL_REPORT'
  | 'FRAUDULENT_PROJECT'
  | 'UNJUST_CANCELLATION'
  | 'MALICIOUS_REFUND'
  | 'WORKSPACE_MISCONDUCT'
  | 'BIASED_VERIFICATION'
  | 'IDEA_THEFT'
  | 'FAKE_CREDENTIALS'
  | 'VERIFICATION_OBSTRUCTION'
  | 'PRIVACY_VIOLATION'
  | 'INAPPROPRIATE_CONTENT'
  | 'ACCOUNT_IMPERSONATION'

export const DISPUTE_CATEGORY_LABELS: Record<DisputeCategory, string> = {
  MILESTONE_NEGLIGENCE:      '마일스톤 미이행',
  FUND_MISUSE:               '자금 횡령/유용',
  FALSE_COMPLETION_REPORT:   '완료 보고서 허위 작성',
  FALSE_APPEAL_REPORT:       '소명 보고서 허위 작성',
  FRAUDULENT_PROJECT:        '허위/사기 프로젝트',
  UNJUST_CANCELLATION:       '일방적 프로젝트 취소',
  MALICIOUS_REFUND:          '악의적 환불 요청',
  WORKSPACE_MISCONDUCT:      '워크스페이스 내 부적절한 언행',
  BIASED_VERIFICATION:       '부당한 검증 결과',
  IDEA_THEFT:                '아이디어 탈취',
  FAKE_CREDENTIALS:          '전문가 자격 위조',
  VERIFICATION_OBSTRUCTION:  '검증 과정 방해',
  PRIVACY_VIOLATION:         '개인정보 침해',
  INAPPROPRIATE_CONTENT:     '부적절 콘텐츠',
  ACCOUNT_IMPERSONATION:     '계정 도용/사칭',
}

export interface DisputeResponse {
  id: number
  targetType: TargetType
  targetId: number
  reportedUserId: number
  reporterId: number
  category: DisputeCategory
  title: string
  status: DisputeStatus
  reason: string
  evidenceUrl: string | null
  createdAt: string
}

export interface CreateDisputeRequest {
  targetType: TargetType
  targetId: number
  reportedUserId: number
  category: DisputeCategory
  title: string
  reason: string
  evidenceUrl?: string
}

export const disputesApi = {
  create: (body: CreateDisputeRequest) =>
    unwrap(apiClient.post<ApiResponse<DisputeResponse>>('/disputes', body)),

  getById: (disputeId: number) =>
    unwrap(apiClient.get<ApiResponse<DisputeResponse>>(`/disputes/${disputeId}`)),

  getMyDisputes: (page = 0, size = 10) =>
    unwrap(apiClient.get<ApiResponse<PageResponse<DisputeResponse>>>('/disputes/me', { params: { page, size } })),

  getReceivedDisputes: (page = 0, size = 10) =>
    unwrap(apiClient.get<ApiResponse<PageResponse<DisputeResponse>>>('/disputes/received', { params: { page, size } })),

  createAppeal: (disputeId: number, content: string, file?: File) => {
    const formData = new FormData()
    formData.append('data', new Blob([JSON.stringify({ content })], { type: 'application/json' }))
    if (file) formData.append('file', file)
    return unwrap(apiClient.post<ApiResponse<void>>(`/disputes/${disputeId}/appeal`, formData))
  },
}
