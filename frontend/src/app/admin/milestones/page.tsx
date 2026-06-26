'use client'

import { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { apiClient, unwrap } from '@/api/client'
import { Badge } from '@/components/ui/Badge'
import { LoadingSpinner } from '@/components/ui/LoadingSpinner'
import type { ApiResponse } from '@/types/api'
import { formatDateTime } from '@/utils/format'

type MilestoneStatus = 'PENDING' | 'IN_PROGRESS' | 'COMPLETED' | 'CANCELLED'
type CompletionReportType = 'COMPLETION' | 'APPEAL'
type CompletionReportStatus = 'SUBMITTED' | 'APPROVED' | 'REJECTED'

interface MilestoneResponse {
  id: number
  ideaId: number
  step: number
  goal: string
  expectedResult: string
  expectedDate: string
  status: MilestoneStatus
  createdAt: string
}

interface CompletionReportResponse {
  reportId: number
  milestoneId: number
  type: CompletionReportType
  content: string
  fileUrl: string | null
  status: CompletionReportStatus
  submittedAt: string
}

const milestoneApi = {
  getByIdea: (ideaId: number) =>
    unwrap(apiClient.get<ApiResponse<MilestoneResponse[]>>(`/milestones/ideas/${ideaId}`)),

  getReports: (milestoneId: number) =>
    unwrap(apiClient.get<ApiResponse<CompletionReportResponse[]>>(`/milestones/${milestoneId}/reports`)),

  approveCompletion: (milestoneId: number) =>
    unwrap(apiClient.post<ApiResponse<void>>(`/milestones/${milestoneId}/reports/approve/completion`)),

  approveAppeal: (milestoneId: number) =>
    unwrap(apiClient.post<ApiResponse<void>>(`/milestones/${milestoneId}/reports/approve/appeal`)),

  reject: (milestoneId: number) =>
    unwrap(apiClient.post<ApiResponse<void>>(`/milestones/${milestoneId}/reports/reject`)),

  refund: (milestoneId: number) =>
    unwrap(apiClient.post<ApiResponse<void>>(`/milestones/${milestoneId}/reports/refund`)),

  cancelByIdea: (ideaId: number) =>
    unwrap(apiClient.post<ApiResponse<void>>(`/milestones/ideas/${ideaId}/cancel`)),
}

const MILESTONE_STATUS_BADGE: Record<MilestoneStatus, { variant: 'green' | 'orange' | 'red' | 'gray'; label: string }> = {
  PENDING:     { variant: 'gray',   label: '대기' },
  IN_PROGRESS: { variant: 'orange', label: '진행중' },
  COMPLETED:   { variant: 'green',  label: '완료' },
  CANCELLED:   { variant: 'red',    label: '취소됨' },
}

function MilestoneCard({ milestone }: { milestone: MilestoneResponse }) {
  const queryClient = useQueryClient()

  const { data: reports, isLoading: reportsLoading } = useQuery({
    queryKey: ['admin', 'milestone', milestone.id, 'reports'],
    queryFn: () => milestoneApi.getReports(milestone.id),
  })

  const approveCompletionMutation = useMutation({
    mutationFn: () => milestoneApi.approveCompletion(milestone.id),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['admin', 'milestone', milestone.id, 'reports'] }),
    onError: () => alert('완료 승인 처리 중 오류가 발생했습니다.'),
  })

  const approveAppealMutation = useMutation({
    mutationFn: () => milestoneApi.approveAppeal(milestone.id),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['admin', 'milestone', milestone.id, 'reports'] }),
    onError: () => alert('소명 승인 처리 중 오류가 발생했습니다.'),
  })

  const rejectMutation = useMutation({
    mutationFn: () => milestoneApi.reject(milestone.id),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['admin', 'milestone', milestone.id, 'reports'] }),
    onError: () => alert('반려 처리 중 오류가 발생했습니다.'),
  })

  const refundMutation = useMutation({
    mutationFn: () => milestoneApi.refund(milestone.id),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['admin', 'milestone', milestone.id, 'reports'] }),
    onError: () => alert('환불 처리 중 오류가 발생했습니다.'),
  })

  const isMutating =
    approveCompletionMutation.isPending ||
    approveAppealMutation.isPending ||
    rejectMutation.isPending ||
    refundMutation.isPending

  const submittedReports = reports?.filter((r) => r.status === 'SUBMITTED') ?? []
  const statusBadge = MILESTONE_STATUS_BADGE[milestone.status]

  return (
    <div style={{
      background: '#fff', border: '1px solid var(--border)',
      borderRadius: '12px', padding: '20px',
    }}>
      {/* 마일스톤 헤더 */}
      <div style={{ display: 'flex', alignItems: 'flex-start', gap: '12px', marginBottom: '14px' }}>
        <div style={{
          width: '32px', height: '32px', borderRadius: '50%',
          background: 'var(--brand-tint)', color: 'var(--brand-dark)',
          display: 'flex', alignItems: 'center', justifyContent: 'center',
          fontWeight: 800, fontSize: '14px', flexShrink: 0,
        }}>
          {milestone.step}
        </div>
        <div style={{ flex: 1, minWidth: 0 }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '6px', flexWrap: 'wrap' }}>
            <span style={{ fontSize: '15px', fontWeight: 700, color: 'var(--fg)' }}>
              {milestone.goal}
            </span>
            <Badge variant={statusBadge.variant}>{statusBadge.label}</Badge>
          </div>
          <div style={{ display: 'flex', gap: '12px', flexWrap: 'wrap', fontSize: '13px', color: 'var(--fg-muted)' }}>
            <span>기대 결과: {milestone.expectedResult}</span>
            <span style={{ color: 'var(--border)' }}>|</span>
            <span>마감일: {milestone.expectedDate}</span>
          </div>
        </div>
      </div>

      {/* 보고서 섹션 */}
      {reportsLoading ? (
        <div style={{ padding: '12px 0', display: 'flex', justifyContent: 'center' }}>
          <LoadingSpinner />
        </div>
      ) : submittedReports.length === 0 ? (
        <div style={{
          padding: '14px 16px', borderRadius: '8px',
          background: 'var(--bg-alt)',
          fontSize: '13px', color: 'var(--fg-muted)',
        }}>
          제출된 보고서가 없습니다.
        </div>
      ) : (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
          {submittedReports.map((report) => {
            const isCompletion = report.type === 'COMPLETION'
            return (
              <div key={report.reportId} style={{
                padding: '14px 16px', borderRadius: '8px',
                border: `1.5px solid ${isCompletion ? '#86efac' : '#fde68a'}`,
                background: isCompletion ? '#f0fdf4' : '#fffbeb',
              }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '8px', flexWrap: 'wrap' }}>
                  <span style={{
                    fontSize: '12px', fontWeight: 700,
                    padding: '2px 10px', borderRadius: '99px',
                    background: isCompletion ? '#dcfce7' : '#fef3c7',
                    color: isCompletion ? '#166534' : '#92400e',
                  }}>
                    {isCompletion ? '완료 보고서' : '소명 보고서'}
                  </span>
                  <span style={{ fontSize: '12px', color: 'var(--fg-muted)' }}>
                    {formatDateTime(report.submittedAt)}
                  </span>
                </div>
                <p style={{
                  fontSize: '13px', color: 'var(--fg)',
                  lineHeight: '1.6', marginBottom: report.fileUrl ? '8px' : '12px',
                  whiteSpace: 'pre-wrap',
                }}>
                  {report.content}
                </p>
                {report.fileUrl && (
                  <a
                    href={report.fileUrl}
                    target="_blank"
                    rel="noopener noreferrer"
                    style={{
                      display: 'inline-block', marginBottom: '12px',
                      fontSize: '13px', color: 'var(--brand)',
                      textDecoration: 'underline', wordBreak: 'break-all',
                    }}
                  >
                    📎 첨부 파일 보기
                  </a>
                )}
                {/* 액션 버튼 */}
                <div style={{ display: 'flex', gap: '8px', flexWrap: 'wrap' }}>
                  {isCompletion ? (
                    <>
                      <button
                        onClick={() => {
                          if (confirm('완료 보고서를 승인하시겠습니까?')) {
                            approveCompletionMutation.mutate()
                          }
                        }}
                        disabled={isMutating}
                        style={{
                          padding: '7px 14px', borderRadius: '8px', border: 'none',
                          background: '#059669', color: '#fff',
                          fontWeight: 700, fontSize: '13px',
                          cursor: isMutating ? 'not-allowed' : 'pointer',
                          opacity: isMutating ? 0.6 : 1,
                        }}
                      >
                        ✅ 완료 승인
                      </button>
                      <button
                        onClick={() => {
                          if (confirm('보고서를 반려하시겠습니까?')) {
                            rejectMutation.mutate()
                          }
                        }}
                        disabled={isMutating}
                        style={{
                          padding: '7px 14px', borderRadius: '8px',
                          border: '1.5px solid #fca5a5', background: '#fff',
                          color: '#dc2626', fontWeight: 700, fontSize: '13px',
                          cursor: isMutating ? 'not-allowed' : 'pointer',
                          opacity: isMutating ? 0.6 : 1,
                        }}
                      >
                        ❌ 반려
                      </button>
                    </>
                  ) : (
                    <>
                      <button
                        onClick={() => {
                          if (confirm('소명 보고서를 승인하시겠습니까?')) {
                            approveAppealMutation.mutate()
                          }
                        }}
                        disabled={isMutating}
                        style={{
                          padding: '7px 14px', borderRadius: '8px', border: 'none',
                          background: '#059669', color: '#fff',
                          fontWeight: 700, fontSize: '13px',
                          cursor: isMutating ? 'not-allowed' : 'pointer',
                          opacity: isMutating ? 0.6 : 1,
                        }}
                      >
                        ✅ 소명 승인
                      </button>
                      <button
                        onClick={() => {
                          if (confirm('소명을 중단 인정하고 환불 처리하시겠습니까?')) {
                            refundMutation.mutate()
                          }
                        }}
                        disabled={isMutating}
                        style={{
                          padding: '7px 14px', borderRadius: '8px', border: 'none',
                          background: '#f59e0b', color: '#fff',
                          fontWeight: 700, fontSize: '13px',
                          cursor: isMutating ? 'not-allowed' : 'pointer',
                          opacity: isMutating ? 0.6 : 1,
                        }}
                      >
                        💸 환불 처리
                      </button>
                      <button
                        onClick={() => {
                          if (confirm('소명 보고서를 반려하시겠습니까?')) {
                            rejectMutation.mutate()
                          }
                        }}
                        disabled={isMutating}
                        style={{
                          padding: '7px 14px', borderRadius: '8px',
                          border: '1.5px solid #fca5a5', background: '#fff',
                          color: '#dc2626', fontWeight: 700, fontSize: '13px',
                          cursor: isMutating ? 'not-allowed' : 'pointer',
                          opacity: isMutating ? 0.6 : 1,
                        }}
                      >
                        ❌ 반려
                      </button>
                    </>
                  )}
                </div>
              </div>
            )
          })}
        </div>
      )}
    </div>
  )
}

export default function AdminMilestonesPage() {
  const queryClient = useQueryClient()
  const [ideaIdInput, setIdeaIdInput] = useState('')
  const [searchedIdeaId, setSearchedIdeaId] = useState<number | null>(null)

  const { data: milestones, isLoading } = useQuery({
    queryKey: ['admin', 'milestones', 'idea', searchedIdeaId],
    queryFn: () => milestoneApi.getByIdea(searchedIdeaId!),
    enabled: searchedIdeaId !== null,
  })

  const cancelMutation = useMutation({
    mutationFn: (ideaId: number) => milestoneApi.cancelByIdea(ideaId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'milestones', 'idea', searchedIdeaId] })
    },
    onError: () => alert('마일스톤 강제 취소 중 오류가 발생했습니다.'),
  })

  const handleSearch = (e: React.FormEvent) => {
    e.preventDefault()
    const parsed = parseInt(ideaIdInput, 10)
    if (!isNaN(parsed) && parsed > 0) {
      setSearchedIdeaId(parsed)
    }
  }

  return (
    <>
      {/* 헤더 */}
      <div style={{ marginBottom: '24px' }}>
        <h1 style={{ fontSize: '22px', fontWeight: 800, color: 'var(--fg)', marginBottom: '4px' }}>
          📋 마일스톤 관리
        </h1>
        <p style={{ fontSize: '14px', color: 'var(--fg-muted)' }}>
          아이디어 ID로 마일스톤을 조회하고 보고서를 승인·반려합니다.
        </p>
      </div>

      {/* ideaId 검색 */}
      <form onSubmit={handleSearch} style={{ display: 'flex', gap: '10px', marginBottom: '28px' }}>
        <input
          type="number"
          min={1}
          value={ideaIdInput}
          onChange={(e) => setIdeaIdInput(e.target.value)}
          placeholder="아이디어 ID 입력"
          style={{
            flex: 1, maxWidth: '240px',
            padding: '10px 14px', borderRadius: '8px',
            border: '1.5px solid var(--border)', fontSize: '14px',
            color: 'var(--fg)', outline: 'none', boxSizing: 'border-box',
          }}
        />
        <button
          type="submit"
          style={{
            padding: '10px 22px', borderRadius: '8px', border: 'none',
            background: 'var(--brand)', color: '#fff',
            fontWeight: 700, fontSize: '14px', cursor: 'pointer',
          }}
        >
          조회
        </button>
      </form>

      {/* 결과 */}
      {searchedIdeaId !== null && (
        isLoading ? (
          <LoadingSpinner />
        ) : !milestones || milestones.length === 0 ? (
          <div style={{
            padding: '60px', textAlign: 'center',
            background: 'var(--bg-alt)', borderRadius: '12px',
            color: 'var(--fg-muted)', fontSize: '15px',
          }}>
            아이디어 #{searchedIdeaId}에 마일스톤이 없습니다.
          </div>
        ) : (
          <>
            <div style={{ fontSize: '13px', color: 'var(--fg-muted)', marginBottom: '14px' }}>
              아이디어 #{searchedIdeaId} · 총 {milestones.length}개 마일스톤
            </div>

            <div style={{ display: 'flex', flexDirection: 'column', gap: '12px', marginBottom: '24px' }}>
              {milestones.map((milestone) => (
                <MilestoneCard key={milestone.id} milestone={milestone} />
              ))}
            </div>

            {/* 강제 취소 */}
            <div style={{
              padding: '20px', borderRadius: '12px',
              border: '1.5px solid #fca5a5', background: '#fff5f5',
            }}>
              <p style={{ fontSize: '14px', fontWeight: 700, color: '#dc2626', marginBottom: '6px' }}>
                위험 구역
              </p>
              <p style={{ fontSize: '13px', color: '#dc2626', marginBottom: '14px', opacity: 0.8 }}>
                아이디어 #{searchedIdeaId}의 모든 마일스톤을 강제 취소합니다. 이 작업은 되돌릴 수 없습니다.
              </p>
              <button
                onClick={() => {
                  if (confirm(`아이디어 #${searchedIdeaId}의 마일스톤을 강제 취소하시겠습니까?\n이 작업은 되돌릴 수 없습니다.`)) {
                    cancelMutation.mutate(searchedIdeaId)
                  }
                }}
                disabled={cancelMutation.isPending}
                style={{
                  padding: '9px 20px', borderRadius: '8px', border: 'none',
                  background: cancelMutation.isPending ? '#fca5a5' : '#dc2626',
                  color: '#fff', fontWeight: 700, fontSize: '14px',
                  cursor: cancelMutation.isPending ? 'not-allowed' : 'pointer',
                }}
              >
                🛑 마일스톤 강제 취소
              </button>
            </div>
          </>
        )
      )}
    </>
  )
}
