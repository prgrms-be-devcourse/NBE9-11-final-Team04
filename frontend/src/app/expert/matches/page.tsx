'use client'

import { useState } from 'react'
import Link from 'next/link'
import { useRouter } from 'next/navigation'
import { useQuery, useQueries, useMutation, useQueryClient } from '@tanstack/react-query'
import { matchesApi } from '@/api/matches'
import { ideasApi } from '@/api/ideas'
import { ProtectedRoute } from '@/components/layout/AppShell'
import { LoadingSpinner } from '@/components/ui/LoadingSpinner'
import { IDEA_CATEGORY_LABELS, type IdeaCategory } from '@/types/enums'
import { formatDate } from '@/utils/format'

const STATUS_LABEL: Record<string, string> = {
  PENDING: '대기 중',
  ACCEPTED: '수락함',
  REJECTED: '거절함',
}

const STATUS_COLOR: Record<string, string> = {
  PENDING: '#d97706',
  ACCEPTED: '#059669',
  REJECTED: '#dc2626',
}

const CATEGORIES: Array<IdeaCategory | 'ALL'> = [
  'ALL', 'TECH', 'LIFE', 'HEALTH', 'EDUCATION', 'ENVIRONMENT', 'CULTURE', 'ETC',
]

const STATUS_TABS: Array<'ALL' | 'PENDING' | 'ACCEPTED' | 'REJECTED'> = [
  'ALL', 'PENDING', 'ACCEPTED', 'REJECTED',
]

const STATUS_TAB_LABEL: Record<string, string> = {
  ALL: '전체',
  PENDING: '대기 중',
  ACCEPTED: '수락함',
  REJECTED: '거절함',
}

function RejectModal({ onConfirm, onCancel }: { onConfirm: (reason: string) => void; onCancel: () => void }) {
  const [reason, setReason] = useState('')
  return (
    <div style={{
      position: 'fixed', inset: 0, zIndex: 1000,
      background: 'rgba(0,0,0,0.4)',
      display: 'flex', alignItems: 'center', justifyContent: 'center', padding: '24px',
    }}>
      <div style={{
        background: '#fff', borderRadius: '14px', padding: '28px',
        width: '100%', maxWidth: '420px', display: 'flex', flexDirection: 'column', gap: '16px',
      }}>
        <h3 style={{ fontSize: '17px', fontWeight: 700, color: 'var(--fg)' }}>거절 사유를 입력해주세요</h3>
        <textarea
          value={reason}
          onChange={(e) => setReason(e.target.value)}
          rows={4}
          placeholder="거절 사유를 구체적으로 작성해주세요."
          style={{
            width: '100%', padding: '12px', borderRadius: '8px',
            border: '1.5px solid var(--border)', fontSize: '14px',
            resize: 'none', outline: 'none', boxSizing: 'border-box',
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
            거절 확인
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

export default function ExpertMatchesPage() {
  const router = useRouter()
  const queryClient = useQueryClient()
  const [categoryFilter, setCategoryFilter] = useState<IdeaCategory | 'ALL'>('ALL')
  const [statusFilter, setStatusFilter] = useState<'ALL' | 'PENDING' | 'ACCEPTED' | 'REJECTED'>('ALL')
  const [rejectTarget, setRejectTarget] = useState<number | null>(null)

  const { data: matches, isLoading: matchesLoading } = useQuery({
    queryKey: ['matches'],
    queryFn: matchesApi.getMyMatches,
  })

  const ideaIds = [...new Set((matches ?? []).map((m) => m.ideaId))]
  const ideaQueries = useQueries({
    queries: ideaIds.map((id) => ({
      queryKey: ['ideas', id],
      queryFn: () => ideasApi.getById(id),
      staleTime: 60_000,
    })),
  })
  const ideaMap = Object.fromEntries(ideaIds.map((id, i) => [id, ideaQueries[i].data]))
  const acceptedIdeaIds = [...new Set((matches ?? []).filter((m) => m.status === 'ACCEPTED').map((m) => m.ideaId))]
  const reviewQueries = useQueries({
    queries: acceptedIdeaIds.map((id) => ({
      queryKey: ['matches', 'reviews', 'idea', id],
      queryFn: () => matchesApi.getReviewsByIdea(id),
      staleTime: 30_000,
    })),
  })
  const reviewedMatchIds = new Set(
    reviewQueries.flatMap((query) => query.data ?? []).map((review) => review.matchId),
  )

  const respondMutation = useMutation({
    mutationFn: ({ matchId, status, rejectReason }: {
      matchId: number; status: 'ACCEPTED' | 'REJECTED'; rejectReason?: string
    }) => matchesApi.respond(matchId, status, rejectReason),
    onSuccess: async (_, variables) => {
      await queryClient.invalidateQueries({ queryKey: ['matches'] })
      if (variables.status === 'ACCEPTED') {
        router.push(`/expert/matches/${variables.matchId}/review`)
      }
    },
    onError: () => alert('처리 중 오류가 발생했습니다.'),
  })

  const handleAccept = (matchId: number) => {
    if (confirm('이 매칭 요청을 수락하시겠습니까?')) {
      respondMutation.mutate({ matchId, status: 'ACCEPTED' })
    }
  }

  const handleReject = (matchId: number, reason: string) => {
    respondMutation.mutate({ matchId, status: 'REJECTED', rejectReason: reason })
    setRejectTarget(null)
  }

  const isLoading =
    matchesLoading ||
    ideaQueries.some((q) => q.isLoading && q.fetchStatus !== 'idle') ||
    reviewQueries.some((q) => q.isLoading && q.fetchStatus !== 'idle')

  const filtered = (matches ?? []).filter((m) => {
    if (statusFilter !== 'ALL' && m.status !== statusFilter) return false
    if (categoryFilter !== 'ALL' && ideaMap[m.ideaId]?.category !== categoryFilter) return false
    return true
  })

  const pendingCount = (matches ?? []).filter((m) => m.status === 'PENDING').length

  return (
    <ProtectedRoute roles={['EXPERT']}>
      <div style={{ maxWidth: '800px', margin: '0 auto', padding: '32px 24px' }}>
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '24px' }}>
          <h1 style={{ fontSize: '22px', fontWeight: 800, color: 'var(--fg)' }}>
            매칭 요청 목록
            {pendingCount > 0 && (
              <span style={{
                marginLeft: '10px', padding: '2px 10px',
                borderRadius: '99px', fontSize: '13px', fontWeight: 700,
                background: '#fef3c7', color: '#d97706',
              }}>
                {pendingCount}건 대기
              </span>
            )}
          </h1>
        </div>

        {/* 상태 탭 */}
        <div style={{ display: 'flex', gap: '4px', marginBottom: '16px', borderBottom: '1px solid var(--border)', paddingBottom: '0' }}>
          {STATUS_TABS.map((tab) => (
            <button
              key={tab}
              onClick={() => setStatusFilter(tab)}
              style={{
                padding: '8px 16px', fontSize: '14px', fontWeight: 600,
                border: 'none', background: 'none', cursor: 'pointer',
                color: statusFilter === tab ? 'var(--brand)' : 'var(--fg-muted)',
                borderBottom: `2px solid ${statusFilter === tab ? 'var(--brand)' : 'transparent'}`,
                marginBottom: '-1px',
                transition: 'all 0.15s',
              }}
            >
              {STATUS_TAB_LABEL[tab]}
            </button>
          ))}
        </div>

        {/* 카테고리 필터 */}
        <div style={{ display: 'flex', gap: '8px', flexWrap: 'wrap', marginBottom: '24px' }}>
          {CATEGORIES.map((cat) => (
            <button
              key={cat}
              onClick={() => setCategoryFilter(cat)}
              style={{
                padding: '6px 14px', borderRadius: '99px', fontSize: '13px', fontWeight: 600,
                border: `1.5px solid ${categoryFilter === cat ? 'var(--brand)' : 'var(--border)'}`,
                background: categoryFilter === cat ? 'var(--brand)' : '#fff',
                color: categoryFilter === cat ? '#fff' : 'var(--fg)',
                cursor: 'pointer', transition: 'all 0.15s',
              }}
            >
              {cat === 'ALL' ? '전체' : IDEA_CATEGORY_LABELS[cat]}
            </button>
          ))}
        </div>

        {isLoading ? (
          <LoadingSpinner />
        ) : filtered.length === 0 ? (
          <div style={{
            padding: '60px', textAlign: 'center', background: 'var(--bg-alt)',
            borderRadius: '12px', color: 'var(--fg-muted)', fontSize: '15px',
          }}>
            {(matches ?? []).length === 0
              ? '받은 매칭 요청이 없습니다.'
              : '조건에 맞는 요청이 없습니다.'}
          </div>
        ) : (
          <div style={{ display: 'flex', flexDirection: 'column', gap: '14px' }}>
            {filtered.map((match) => {
              const idea = ideaMap[match.ideaId]
              const reviewed = reviewedMatchIds.has(match.matchId)
              const titleHref = match.status === 'ACCEPTED' && !reviewed
                ? `/expert/matches/${match.matchId}/review`
                : `/ideas/${match.ideaId}`
              return (
                <div key={match.matchId} style={{
                  background: '#fff', border: '1px solid var(--border)',
                  borderRadius: '12px', padding: '22px 24px',
                  display: 'flex', flexDirection: 'column', gap: '12px',
                }}>
                  {/* 제목 + 상태 */}
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', gap: '12px' }}>
                    <div style={{ flex: 1, minWidth: 0 }}>
                      <Link
                        href={titleHref}
                        style={{
                          fontSize: '16px', fontWeight: 700, color: 'var(--fg)',
                          textDecoration: 'none', display: 'block', marginBottom: '6px',
                        }}
                        onMouseEnter={(e) => (e.currentTarget.style.color = 'var(--brand)')}
                        onMouseLeave={(e) => (e.currentTarget.style.color = 'var(--fg)')}
                      >
                        {idea?.title ?? `아이디어 #${match.ideaId}`} →
                      </Link>
                      <div style={{ display: 'flex', gap: '10px', alignItems: 'center', fontSize: '13px', color: 'var(--fg-muted)' }}>
                        {idea?.category && (
                          <span style={{
                            padding: '2px 10px', borderRadius: '99px', fontSize: '12px', fontWeight: 600,
                            background: 'var(--brand-tint)', color: 'var(--brand-dark)',
                          }}>
                            {IDEA_CATEGORY_LABELS[idea.category as IdeaCategory]}
                          </span>
                        )}
                        <span>요청일 {formatDate(match.requestedAt)}</span>
                        {match.respondedAt && (
                          <span>응답일 {formatDate(match.respondedAt)}</span>
                        )}
                      </div>
                    </div>
                    <span style={{
                      padding: '4px 12px', borderRadius: '99px', fontSize: '13px', fontWeight: 700,
                      background: `${STATUS_COLOR[match.status]}18`,
                      color: STATUS_COLOR[match.status],
                      flexShrink: 0,
                    }}>
                      {STATUS_LABEL[match.status]}
                    </span>
                  </div>

                  {/* 거절 사유 */}
                  {match.rejectReason && (
                    <div style={{
                      padding: '10px 14px', borderRadius: '8px',
                      background: '#fef2f2', border: '1px solid #fecaca',
                      fontSize: '13px', color: '#dc2626',
                    }}>
                      거절 사유: {match.rejectReason}
                    </div>
                  )}

                  {/* 액션 버튼 */}
                  {match.status === 'PENDING' && (
                    <div style={{ display: 'flex', gap: '10px' }}>
                      <button
                        onClick={() => handleAccept(match.matchId)}
                        disabled={respondMutation.isPending}
                        style={{
                          padding: '10px 24px', borderRadius: '8px', border: 'none',
                          background: 'var(--brand)', color: '#fff',
                          fontWeight: 700, fontSize: '14px', cursor: 'pointer',
                        }}
                      >
                        수락
                      </button>
                      <button
                        onClick={() => setRejectTarget(match.matchId)}
                        disabled={respondMutation.isPending}
                        style={{
                          padding: '10px 24px', borderRadius: '8px',
                          border: '1.5px solid #fca5a5', background: '#fff',
                          color: '#dc2626', fontWeight: 600, fontSize: '14px', cursor: 'pointer',
                        }}
                      >
                        거절
                      </button>
                    </div>
                  )}

                  {match.status === 'ACCEPTED' && (
                    reviewed ? (
                      <div style={{
                        padding: '10px 14px', borderRadius: '8px',
                        background: '#f0fdf4', border: '1px solid #bbf7d0',
                        fontSize: '13px', color: '#059669', fontWeight: 600,
                      }}>
                        ✅ 검증서 제출 완료
                      </div>
                    ) : (
                      <Link
                        href={`/expert/matches/${match.matchId}/review`}
                        style={{
                          alignSelf: 'flex-start', padding: '10px 24px',
                          borderRadius: '8px', border: 'none',
                          background: '#059669', color: '#fff',
                          fontWeight: 700, fontSize: '14px', cursor: 'pointer',
                          textDecoration: 'none', display: 'inline-block',
                        }}
                      >
                        📋 검증서 작성
                      </Link>
                    )
                  )}
                </div>
              )
            })}
          </div>
        )}
      </div>

      {rejectTarget !== null && (
        <RejectModal
          onConfirm={(reason) => handleReject(rejectTarget, reason)}
          onCancel={() => setRejectTarget(null)}
        />
      )}
    </ProtectedRoute>
  )
}
