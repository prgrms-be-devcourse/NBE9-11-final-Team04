'use client'

import { useState } from 'react'
import Link from 'next/link'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { apiClient, unwrap } from '@/api/client'
import { Badge } from '@/components/ui/Badge'
import { LoadingSpinner } from '@/components/ui/LoadingSpinner'
import type { ApiResponse, PageResponse } from '@/types/api'
import type { IdeaDetail } from '@/types/idea'
import {
  IDEA_CATEGORY_LABELS,
  IDEA_STATUS_LABELS,
  type IdeaCategory,
  type IdeaStatus,
} from '@/types/enums'
import { formatDate } from '@/utils/format'

interface AdminIdeaResponse {
  ideaId: number
  title: string
  category: string
  status: IdeaStatus
  rejectReason: string | null
  trustScore: number | null
  badge: string
  createdAt: string
}

interface VerificationInfo {
  verificationId: number
  ideaId: number
  status: string
}

interface IdeaDetailWithMilestones extends IdeaDetail {
  milestones: Array<{
    id: number
    goal: string
    expectedResult: string
    expectedDate: string
    lockedAmount: number | null
  }>
}

const adminApi = {
  getIdeas: (status: IdeaStatus | '', category: IdeaCategory | '', page: number) =>
    unwrap(
      apiClient.get<ApiResponse<PageResponse<AdminIdeaResponse>>>('/admin/ideas', {
        params: {
          ...(status ? { status } : {}),
          ...(category ? { category } : {}),
          page,
          size: 20,
        },
      }),
    ),
  approveIdea: (ideaId: number) =>
    unwrap(apiClient.patch<ApiResponse<void>>(`/admin/ideas/${ideaId}/approve`)),
  rejectIdea: (ideaId: number, reason: string) =>
    unwrap(apiClient.put<ApiResponse<void>>(`/admin/ideas/${ideaId}/reject`, { reason })),
  getVerification: (ideaId: number) =>
    unwrap(apiClient.get<ApiResponse<VerificationInfo>>(`/verifications/ideas/${ideaId}`)),
  getIdeaDetail: (ideaId: number) =>
    unwrap(apiClient.get<ApiResponse<IdeaDetailWithMilestones>>(`/ideas/${ideaId}`)),
  retryVerification: (verificationId: number, body: object) =>
    unwrap(apiClient.post<ApiResponse<void>>(`/admin/verifications/${verificationId}/retry`, body)),
}

function buildDescription(idea: IdeaDetailWithMilestones): string {
  return [
    idea.oneLineIntro,
    idea.problemDefinition,
    idea.solution,
    idea.goal,
    idea.targetCustomer,
    idea.competitor,
    idea.teamIntro,
  ].join('\n')
}

const STATUS_BADGE_VARIANT: Record<IdeaStatus, 'gray' | 'orange' | 'green' | 'red'> = {
  AI_PENDING:             'gray',
  EXPERT_PENDING:         'gray',
  ADMIN_PENDING:          'orange',
  OPEN:                   'green',
  IN_PROGRESS:            'green',
  COMPLETED:              'green',
  CANCELLED:              'red',
  REJECTED:               'red',
  CANCELLATION_REQUESTED: 'orange',
  SUSPENDED:              'red',
}

const STATUS_TABS: { value: IdeaStatus | ''; label: string }[] = [
  { value: '',                    label: '전체' },
  { value: 'ADMIN_PENDING',       label: '승인 대기' },
  { value: 'AI_PENDING',          label: 'AI 검증' },
  { value: 'EXPERT_PENDING',      label: '전문가 검증' },
  { value: 'OPEN',                label: '펀딩중' },
  { value: 'IN_PROGRESS',         label: '사업중' },
  { value: 'CANCELLATION_REQUESTED', label: '취소 요청' },
  { value: 'COMPLETED',           label: '완료' },
  { value: 'CANCELLED',           label: '취소' },
  { value: 'REJECTED',            label: '반려' },
]

const CATEGORY_TABS: { value: IdeaCategory | ''; label: string }[] = [
  { value: '', label: '전체' },
  ...(Object.entries(IDEA_CATEGORY_LABELS) as [IdeaCategory, string][]).map(([value, label]) => ({
    value,
    label,
  })),
]

function RejectModal({
  ideaTitle,
  onConfirm,
  onCancel,
}: {
  ideaTitle: string
  onConfirm: (reason: string) => void
  onCancel: () => void
}) {
  const [reason, setReason] = useState('')
  return (
    <div style={{
      position: 'fixed', inset: 0, zIndex: 1000,
      background: 'rgba(0,0,0,0.45)',
      display: 'flex', alignItems: 'center', justifyContent: 'center', padding: '24px',
    }}>
      <div style={{
        background: '#fff', borderRadius: '14px', padding: '28px',
        width: '100%', maxWidth: '440px', display: 'flex', flexDirection: 'column', gap: '16px',
      }}>
        <div>
          <h3 style={{ fontSize: '17px', fontWeight: 700, color: 'var(--fg)', marginBottom: '6px' }}>반려 사유 입력</h3>
          <p style={{ fontSize: '13px', color: 'var(--fg-muted)' }}>{ideaTitle}</p>
        </div>
        <textarea
          value={reason}
          onChange={(e) => setReason(e.target.value)}
          rows={4}
          placeholder="반려 사유를 구체적으로 작성해주세요."
          style={{
            width: '100%', padding: '12px', borderRadius: '8px',
            border: '1.5px solid var(--border)', fontSize: '14px',
            resize: 'none', outline: 'none', boxSizing: 'border-box',
            fontFamily: 'inherit',
          }}
        />
        <div style={{ display: 'flex', gap: '10px' }}>
          <button
            onClick={() => reason.trim() && onConfirm(reason.trim())}
            disabled={!reason.trim()}
            style={{
              flex: 1, padding: '12px', borderRadius: '8px', border: 'none',
              background: reason.trim() ? '#dc2626' : 'var(--border)',
              color: '#fff', fontWeight: 700, fontSize: '14px',
              cursor: reason.trim() ? 'pointer' : 'not-allowed',
            }}
          >
            반려 확인
          </button>
          <button
            onClick={onCancel}
            style={{
              flex: 1, padding: '12px', borderRadius: '8px',
              border: '1.5px solid var(--border)', background: '#fff',
              color: 'var(--fg)', fontWeight: 600, fontSize: '14px', cursor: 'pointer',
            }}
          >
            취소
          </button>
        </div>
      </div>
    </div>
  )
}

export default function AdminIdeasPage() {
  const queryClient = useQueryClient()
  const [page, setPage] = useState(0)
  const [statusFilter, setStatusFilter] = useState<IdeaStatus | ''>('')
  const [categoryFilter, setCategoryFilter] = useState<IdeaCategory | ''>('')
  const [rejectTarget, setRejectTarget] = useState<{ ideaId: number; title: string } | null>(null)
  const [retryingId, setRetryingId] = useState<number | null>(null)

  const { data, isLoading } = useQuery({
    queryKey: ['admin', 'ideas', statusFilter, categoryFilter, page],
    queryFn: () => adminApi.getIdeas(statusFilter, categoryFilter, page),
  })

  const approveMutation = useMutation({
    mutationFn: (ideaId: number) => adminApi.approveIdea(ideaId),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['admin', 'ideas'] }),
    onError: () => alert('승인 처리 중 오류가 발생했습니다.'),
  })

  const rejectMutation = useMutation({
    mutationFn: ({ ideaId, reason }: { ideaId: number; reason: string }) =>
      adminApi.rejectIdea(ideaId, reason),
    onSuccess: () => {
      setRejectTarget(null)
      queryClient.invalidateQueries({ queryKey: ['admin', 'ideas'] })
    },
    onError: () => alert('반려 처리 중 오류가 발생했습니다.'),
  })

  const retryMutation = useMutation({
    mutationFn: async (ideaId: number) => {
      const [verification, idea] = await Promise.all([
        adminApi.getVerification(ideaId),
        adminApi.getIdeaDetail(ideaId),
      ])
      const body = {
        ideaId: idea.ideaId,
        title: idea.title,
        description: buildDescription(idea),
        milestones: (idea.milestones ?? []).map((m) => ({
          goal: m.goal,
          expectedResult: m.expectedResult,
          expectedDate: m.expectedDate,
          lockedAmount: m.lockedAmount ?? null,
        })),
      }
      return adminApi.retryVerification(verification.verificationId, body)
    },
    onSuccess: () => {
      setRetryingId(null)
      queryClient.invalidateQueries({ queryKey: ['admin', 'ideas'] })
      alert('AI 검증 재시도를 요청했습니다. 잠시 후 결과를 확인해주세요.')
    },
    onError: () => {
      setRetryingId(null)
      alert('AI 재시도 중 오류가 발생했습니다.')
    },
  })

  const ideas = data?.content ?? []
  const totalPages = data?.totalPages ?? 1
  const totalElements = data?.totalElements ?? 0
  const isActionPending = approveMutation.isPending || rejectMutation.isPending

  const handleStatusChange = (value: IdeaStatus | '') => {
    setStatusFilter(value)
    setPage(0)
  }

  const handleCategoryChange = (value: IdeaCategory | '') => {
    setCategoryFilter(value)
    setPage(0)
  }

  const handleRetry = (idea: AdminIdeaResponse) => {
    if (!confirm(`"${idea.title}" 아이디어의 AI 검증을 재시도하시겠습니까?`)) return
    setRetryingId(idea.ideaId)
    retryMutation.mutate(idea.ideaId)
  }

  return (
    <>
      <div style={{ marginBottom: '20px' }}>
        <h1 style={{ fontSize: '22px', fontWeight: 800, color: 'var(--fg)', marginBottom: '4px' }}>
          아이디어 관리
        </h1>
        <p style={{ fontSize: '14px', color: 'var(--fg-muted)' }}>
          전체 아이디어를 조회하고 관리합니다.
        </p>
      </div>

      {/* 상태 필터 */}
      <div style={{
        display: 'flex', gap: '6px', marginBottom: '10px',
        overflowX: 'auto', paddingBottom: '2px',
      }}>
        {STATUS_TABS.map((tab) => (
          <button
            key={tab.value}
            onClick={() => handleStatusChange(tab.value)}
            style={{
              flexShrink: 0,
              padding: '7px 14px', borderRadius: '99px', border: 'none',
              background: statusFilter === tab.value ? 'var(--brand)' : 'var(--bg-alt)',
              color: statusFilter === tab.value ? '#fff' : 'var(--fg-muted)',
              fontWeight: statusFilter === tab.value ? 700 : 500,
              fontSize: '13px', cursor: 'pointer',
              transition: 'background 0.15s, color 0.15s',
              whiteSpace: 'nowrap',
            }}
          >
            {tab.label}
          </button>
        ))}
      </div>

      {/* 카테고리 필터 */}
      <div style={{
        display: 'flex', gap: '6px', marginBottom: '20px',
        overflowX: 'auto', paddingBottom: '2px',
      }}>
        {CATEGORY_TABS.map((tab) => (
          <button
            key={tab.value}
            onClick={() => handleCategoryChange(tab.value)}
            style={{
              flexShrink: 0,
              padding: '5px 12px', borderRadius: '99px',
              border: `1.5px solid ${categoryFilter === tab.value ? 'var(--brand)' : 'var(--border)'}`,
              background: categoryFilter === tab.value ? 'var(--brand-tint)' : '#fff',
              color: categoryFilter === tab.value ? 'var(--brand-dark)' : 'var(--fg-muted)',
              fontWeight: categoryFilter === tab.value ? 700 : 500,
              fontSize: '12px', cursor: 'pointer',
              transition: 'all 0.15s',
              whiteSpace: 'nowrap',
            }}
          >
            {tab.label}
          </button>
        ))}
      </div>

      {/* 목록 */}
      {isLoading ? (
        <LoadingSpinner />
      ) : ideas.length === 0 ? (
        <div style={{
          padding: '60px', textAlign: 'center',
          background: 'var(--bg-alt)', borderRadius: '12px',
          color: 'var(--fg-muted)', fontSize: '15px',
        }}>
          해당하는 아이디어가 없습니다.
        </div>
      ) : (
        <>
          <div style={{ fontSize: '13px', color: 'var(--fg-muted)', marginBottom: '12px' }}>
            총 {totalElements}건
          </div>
          <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
            {ideas.map((idea) => (
              <div key={idea.ideaId} style={{
                background: '#fff', border: '1px solid var(--border)',
                borderRadius: '12px', padding: '20px 24px',
                display: 'flex', flexDirection: 'column', gap: '12px',
              }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', gap: '12px' }}>
                  <div style={{ flex: 1, minWidth: 0 }}>
                    <Link
                      href={`/ideas/${idea.ideaId}`}
                      target="_blank"
                      style={{
                        fontSize: '16px', fontWeight: 700, color: 'var(--fg)',
                        textDecoration: 'none', display: 'block', marginBottom: '8px',
                      }}
                      onMouseEnter={(e) => (e.currentTarget.style.color = 'var(--brand)')}
                      onMouseLeave={(e) => (e.currentTarget.style.color = 'var(--fg)')}
                    >
                      {idea.title} ↗
                    </Link>
                    <div style={{ display: 'flex', gap: '8px', alignItems: 'center', flexWrap: 'wrap' }}>
                      <Badge variant={STATUS_BADGE_VARIANT[idea.status]}>
                        {IDEA_STATUS_LABELS[idea.status]}
                      </Badge>
                      <span style={{
                        padding: '2px 10px', borderRadius: '99px', fontSize: '12px', fontWeight: 600,
                        background: 'var(--brand-tint)', color: 'var(--brand-dark)',
                      }}>
                        {IDEA_CATEGORY_LABELS[idea.category as IdeaCategory] ?? idea.category}
                      </span>
                      {idea.badge === 'CERTIFIED' && (
                        <span style={{
                          padding: '2px 10px', borderRadius: '99px', fontSize: '12px', fontWeight: 600,
                          background: '#d1fae5', color: '#065f46',
                        }}>
                          ✅ CERTIFIED
                        </span>
                      )}
                      <span style={{ fontSize: '13px', color: 'var(--fg-muted)' }}>
                        {formatDate(idea.createdAt)}
                      </span>
                    </div>
                  </div>

                  {idea.trustScore !== null && (
                    <div style={{
                      flexShrink: 0, textAlign: 'center',
                      padding: '8px 16px', borderRadius: '10px',
                      background: idea.trustScore >= 80 ? '#d1fae5' : 'var(--bg-alt)',
                      border: `1px solid ${idea.trustScore >= 80 ? '#86efac' : 'var(--border)'}`,
                    }}>
                      <div style={{
                        fontSize: '22px', fontWeight: 800,
                        color: idea.trustScore >= 80 ? '#059669' : 'var(--fg)',
                        lineHeight: 1,
                      }}>
                        {idea.trustScore}
                      </div>
                      <div style={{ fontSize: '11px', color: 'var(--fg-muted)', marginTop: '2px' }}>신뢰도</div>
                    </div>
                  )}
                </div>

                {/* AI_PENDING: 재시도 버튼 */}
                {idea.status === 'AI_PENDING' && (
                  <div style={{ display: 'flex', gap: '10px' }}>
                    <button
                      onClick={() => handleRetry(idea)}
                      disabled={retryingId === idea.ideaId || retryMutation.isPending}
                      style={{
                        padding: '10px 24px', borderRadius: '8px', border: 'none',
                        background: retryingId === idea.ideaId ? 'var(--border)' : 'var(--brand)',
                        color: '#fff', fontWeight: 700, fontSize: '14px',
                        cursor: retryingId === idea.ideaId || retryMutation.isPending ? 'not-allowed' : 'pointer',
                        opacity: retryingId === idea.ideaId || retryMutation.isPending ? 0.7 : 1,
                      }}
                    >
                      {retryingId === idea.ideaId ? '재시도 중...' : '🔄 AI 검증 재시도'}
                    </button>
                  </div>
                )}

                {/* ADMIN_PENDING: 승인/반려 버튼 */}
                {idea.status === 'ADMIN_PENDING' && (
                  <div style={{ display: 'flex', gap: '10px' }}>
                    <button
                      onClick={() => {
                        if (confirm(`"${idea.title}" 아이디어를 승인하시겠습니까?`)) {
                          approveMutation.mutate(idea.ideaId)
                        }
                      }}
                      disabled={isActionPending}
                      style={{
                        padding: '10px 24px', borderRadius: '8px', border: 'none',
                        background: '#059669', color: '#fff',
                        fontWeight: 700, fontSize: '14px',
                        cursor: isActionPending ? 'not-allowed' : 'pointer',
                        opacity: isActionPending ? 0.7 : 1,
                      }}
                    >
                      ✅ 승인
                    </button>
                    <button
                      onClick={() => setRejectTarget({ ideaId: idea.ideaId, title: idea.title })}
                      disabled={isActionPending}
                      style={{
                        padding: '10px 24px', borderRadius: '8px',
                        border: '1.5px solid #fca5a5', background: '#fff',
                        color: '#dc2626', fontWeight: 600, fontSize: '14px',
                        cursor: isActionPending ? 'not-allowed' : 'pointer',
                        opacity: isActionPending ? 0.7 : 1,
                      }}
                    >
                      ❌ 반려
                    </button>
                  </div>
                )}

                {idea.rejectReason && (
                  <div style={{
                    padding: '10px 14px', borderRadius: '8px',
                    background: '#fff5f5', border: '1px solid #fecaca',
                    fontSize: '13px', color: '#dc2626',
                  }}>
                    반려 사유: {idea.rejectReason}
                  </div>
                )}
              </div>
            ))}
          </div>
        </>
      )}

      {totalPages > 1 && (
        <div style={{ display: 'flex', justifyContent: 'center', gap: '8px', marginTop: '24px' }}>
          <button
            onClick={() => setPage((p) => Math.max(0, p - 1))}
            disabled={page === 0}
            style={{
              padding: '8px 16px', borderRadius: '8px',
              border: '1.5px solid var(--border)', background: '#fff',
              color: page === 0 ? 'var(--fg-muted)' : 'var(--fg)',
              fontSize: '14px', cursor: page === 0 ? 'not-allowed' : 'pointer',
            }}
          >
            이전
          </button>
          <span style={{ padding: '8px 16px', fontSize: '14px', color: 'var(--fg-muted)' }}>
            {page + 1} / {totalPages}
          </span>
          <button
            onClick={() => setPage((p) => Math.min(totalPages - 1, p + 1))}
            disabled={page >= totalPages - 1}
            style={{
              padding: '8px 16px', borderRadius: '8px',
              border: '1.5px solid var(--border)', background: '#fff',
              color: page >= totalPages - 1 ? 'var(--fg-muted)' : 'var(--fg)',
              fontSize: '14px', cursor: page >= totalPages - 1 ? 'not-allowed' : 'pointer',
            }}
          >
            다음
          </button>
        </div>
      )}

      {rejectTarget && (
        <RejectModal
          ideaTitle={rejectTarget.title}
          onConfirm={(reason) => rejectMutation.mutate({ ideaId: rejectTarget.ideaId, reason })}
          onCancel={() => setRejectTarget(null)}
        />
      )}
    </>
  )
}
