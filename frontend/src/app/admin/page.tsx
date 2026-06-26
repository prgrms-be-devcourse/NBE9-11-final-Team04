'use client'

import { useState } from 'react'
import Link from 'next/link'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { apiClient, unwrap } from '@/api/client'
import { ProtectedRoute } from '@/components/layout/AppShell'
import { LoadingSpinner } from '@/components/ui/LoadingSpinner'
import type { ApiResponse, PageResponse } from '@/types/api'
import { IDEA_CATEGORY_LABELS, type IdeaCategory } from '@/types/enums'
import { formatDate } from '@/utils/format'

interface AdminIdeaReviewResponse {
  ideaId: number
  title: string
  category: string
  status: string
  rejectReason: string | null
  trustScore: number | null
  badge: string
  createdAt: string
}

const adminApi = {
  getPendingIdeas: (page = 0) =>
    unwrap(
      apiClient.get<ApiResponse<PageResponse<AdminIdeaReviewResponse>>>('/admin/ideas', {
        params: { status: 'ADMIN_PENDING', page, size: 20 },
      }),
    ),
  approveIdea: (ideaId: number) =>
    unwrap(apiClient.patch<ApiResponse<void>>(`/admin/ideas/${ideaId}/approve`)),
  rejectIdea: (ideaId: number, reason: string) =>
    unwrap(apiClient.patch<ApiResponse<void>>(`/admin/ideas/${ideaId}/reject`, { reason })),
}

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

export default function AdminDashboardPage() {
  const queryClient = useQueryClient()
  const [page, setPage] = useState(0)
  const [rejectTarget, setRejectTarget] = useState<{ ideaId: number; title: string } | null>(null)

  const { data, isLoading } = useQuery({
    queryKey: ['admin', 'ideas', 'ADMIN_PENDING', page],
    queryFn: () => adminApi.getPendingIdeas(page),
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

  const ideas = data?.content ?? []
  const totalPages = data?.totalPages ?? 1
  const totalElements = data?.totalElements ?? 0
  const isPending = approveMutation.isPending || rejectMutation.isPending

  return (
    <ProtectedRoute roles={['ADMIN']}>
      <div style={{ maxWidth: '900px', margin: '0 auto', padding: '32px 24px' }}>

        {/* 헤더 */}
        <div style={{ marginBottom: '28px' }}>
          <h1 style={{ fontSize: '22px', fontWeight: 800, color: 'var(--fg)', marginBottom: '4px' }}>
            관리자 대시보드
          </h1>
          <p style={{ fontSize: '14px', color: 'var(--fg-muted)' }}>
            관리자 승인 대기 아이디어를 검토합니다.
          </p>
        </div>

        {/* 승인 대기 카운트 */}
        <div style={{
          background: 'var(--brand-tint)', border: '1px solid var(--brand)',
          borderRadius: '12px', padding: '16px 20px',
          display: 'flex', alignItems: 'center', gap: '12px',
          marginBottom: '24px',
        }}>
          <span style={{ fontSize: '24px' }}>📋</span>
          <div>
            <div style={{ fontSize: '13px', color: 'var(--brand-dark)', fontWeight: 600 }}>승인 대기 아이디어</div>
            <div style={{ fontSize: '28px', fontWeight: 800, color: 'var(--brand)', lineHeight: 1.2 }}>
              {isLoading ? '-' : `${totalElements}건`}
            </div>
          </div>
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
            승인 대기 중인 아이디어가 없습니다.
          </div>
        ) : (
          <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
            {ideas.map((idea) => (
              <div key={idea.ideaId} style={{
                background: '#fff', border: '1px solid var(--border)',
                borderRadius: '12px', padding: '20px 24px',
                display: 'flex', flexDirection: 'column', gap: '12px',
              }}>
                {/* 제목 + 배지 */}
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', gap: '12px' }}>
                  <div style={{ flex: 1, minWidth: 0 }}>
                    <Link
                      href={`/ideas/${idea.ideaId}`}
                      target="_blank"
                      style={{
                        fontSize: '16px', fontWeight: 700, color: 'var(--fg)',
                        textDecoration: 'none', display: 'block', marginBottom: '6px',
                      }}
                      onMouseEnter={(e) => (e.currentTarget.style.color = 'var(--brand)')}
                      onMouseLeave={(e) => (e.currentTarget.style.color = 'var(--fg)')}
                    >
                      {idea.title} ↗
                    </Link>
                    <div style={{ display: 'flex', gap: '8px', alignItems: 'center', flexWrap: 'wrap' }}>
                      <span style={{
                        padding: '2px 10px', borderRadius: '99px', fontSize: '12px', fontWeight: 600,
                        background: 'var(--brand-tint)', color: 'var(--brand-dark)',
                      }}>
                        {IDEA_CATEGORY_LABELS[idea.category as IdeaCategory] ?? idea.category}
                      </span>
                      {idea.badge === 'VERIFIED' && (
                        <span style={{
                          padding: '2px 10px', borderRadius: '99px', fontSize: '12px', fontWeight: 600,
                          background: '#d1fae5', color: '#065f46',
                        }}>
                          ✅ CERTIFIED
                        </span>
                      )}
                      <span style={{ fontSize: '13px', color: 'var(--fg-muted)' }}>
                        등록일 {formatDate(idea.createdAt)}
                      </span>
                    </div>
                  </div>

                  {/* 신뢰도 점수 */}
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

                {/* 액션 버튼 */}
                <div style={{ display: 'flex', gap: '10px' }}>
                  <button
                    onClick={() => {
                      if (confirm(`"${idea.title}" 아이디어를 승인하시겠습니까?`)) {
                        approveMutation.mutate(idea.ideaId)
                      }
                    }}
                    disabled={isPending}
                    style={{
                      padding: '10px 24px', borderRadius: '8px', border: 'none',
                      background: '#059669', color: '#fff',
                      fontWeight: 700, fontSize: '14px',
                      cursor: isPending ? 'not-allowed' : 'pointer',
                      opacity: isPending ? 0.7 : 1,
                    }}
                  >
                    ✅ 승인
                  </button>
                  <button
                    onClick={() => setRejectTarget({ ideaId: idea.ideaId, title: idea.title })}
                    disabled={isPending}
                    style={{
                      padding: '10px 24px', borderRadius: '8px',
                      border: '1.5px solid #fca5a5', background: '#fff',
                      color: '#dc2626', fontWeight: 600, fontSize: '14px',
                      cursor: isPending ? 'not-allowed' : 'pointer',
                      opacity: isPending ? 0.7 : 1,
                    }}
                  >
                    ❌ 반려
                  </button>
                </div>
              </div>
            ))}
          </div>
        )}

        {/* 페이지네이션 */}
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
      </div>

      {rejectTarget && (
        <RejectModal
          ideaTitle={rejectTarget.title}
          onConfirm={(reason) => rejectMutation.mutate({ ideaId: rejectTarget.ideaId, reason })}
          onCancel={() => setRejectTarget(null)}
        />
      )}
    </ProtectedRoute>
  )
}
