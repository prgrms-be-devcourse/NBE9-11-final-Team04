'use client'

import { useState, useEffect, Suspense } from 'react'
import { useSearchParams, useRouter } from 'next/navigation'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { apiClient, unwrap } from '@/api/client'
import { Badge } from '@/components/ui/Badge'
import { LoadingSpinner } from '@/components/ui/LoadingSpinner'
import type { ApiResponse, PageResponse } from '@/types/api'
import { formatCurrency, formatDateTime } from '@/utils/format'

interface IdeaOption {
  ideaId: number
  title: string
  status: string
}

interface DepositResponse {
  depositId: number | null
  ideaId: number
  userId: number
  amount: number
  status: string
  paidAt: string | null
  releasedAt: string | null
}

const settlementApi = {
  getDeposit: (ideaId: number) =>
    unwrap(apiClient.get<ApiResponse<DepositResponse>>(`/admin/settlements/ideas/${ideaId}/deposit`)),

  release: (ideaId: number) =>
    unwrap(apiClient.post<ApiResponse<void>>(`/admin/settlements/ideas/${ideaId}/deposit/release`)),

  forfeit: (ideaId: number) =>
    unwrap(apiClient.post<ApiResponse<void>>(`/admin/settlements/ideas/${ideaId}/deposit/forfeit`)),

  forceRefund: (ideaId: number) =>
    unwrap(apiClient.patch<ApiResponse<void>>(`/admin/settlements/ideas/${ideaId}/force-refund`)),

  getIdeas: () =>
    unwrap(
      apiClient.get<ApiResponse<PageResponse<IdeaOption>>>('/admin/ideas', {
        params: { page: 0, size: 200 },
      }),
    ),
}

const DEPOSIT_STATUS_BADGE: Record<string, { variant: 'green' | 'orange' | 'red' | 'gray'; label: string }> = {
  HELD:            { variant: 'orange', label: '보유중' },
  REFUNDED:        { variant: 'green',  label: '환급됨' },
  FORFEITED:       { variant: 'red',    label: '몰수됨' },
  PENDING_PAYMENT: { variant: 'gray',   label: '납입 대기' },
}

const DEPOSIT_ACTIONABLE_STATUSES = new Set(['HELD', 'PENDING_PAYMENT'])

function getDepositStatusBadge(status: string) {
  return DEPOSIT_STATUS_BADGE[status] ?? { variant: 'gray' as const, label: status }
}

const IDEA_STATUS_KO: Record<string, string> = {
  AI_PENDING:             'AI 검증',
  EXPERT_PENDING:         '전문가 검증',
  ADMIN_PENDING:          '승인 대기',
  OPEN:                   '펀딩중',
  IN_PROGRESS:            '사업중',
  COMPLETED:              '완료',
  CANCELLED:              '취소',
  REJECTED:               '반려',
  CANCELLATION_REQUESTED: '취소 요청',
  SUSPENDED:              '중단',
}

function AdminSettlementsContent() {
  const router = useRouter()
  const searchParams = useSearchParams()
  const queryClient = useQueryClient()
  const [selectedIdeaId, setSelectedIdeaId] = useState<number | null>(null)

  const { data: ideasPage, isLoading: ideasLoading } = useQuery({
    queryKey: ['admin', 'ideas', 'all', 'settlement'],
    queryFn: settlementApi.getIdeas,
    staleTime: 30_000,
  })

  const ideas = ideasPage?.content ?? []

  useEffect(() => {
    const param = searchParams.get('ideaId')
    if (param) {
      const parsed = parseInt(param, 10)
      if (!isNaN(parsed) && parsed > 0) {
        setSelectedIdeaId(parsed)
      }
    }
  }, [searchParams])

  const { data: deposit, isLoading: depositLoading } = useQuery({
    queryKey: ['admin', 'settlements', 'deposit', selectedIdeaId],
    queryFn: () => settlementApi.getDeposit(selectedIdeaId!),
    enabled: selectedIdeaId !== null,
  })

  const releaseMutation = useMutation({
    mutationFn: (ideaId: number) => settlementApi.release(ideaId),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['admin', 'settlements', 'deposit', selectedIdeaId] }),
    onError: () => alert('보증금 환급 처리 중 오류가 발생했습니다.'),
  })

  const forfeitMutation = useMutation({
    mutationFn: (ideaId: number) => settlementApi.forfeit(ideaId),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['admin', 'settlements', 'deposit', selectedIdeaId] }),
    onError: () => alert('보증금 몰수 처리 중 오류가 발생했습니다.'),
  })

  const forceRefundMutation = useMutation({
    mutationFn: (ideaId: number) => settlementApi.forceRefund(ideaId),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['admin', 'settlements', 'deposit', selectedIdeaId] }),
    onError: () => alert('강제 전체 환불 처리 중 오류가 발생했습니다.'),
  })

  const isMutating = releaseMutation.isPending || forfeitMutation.isPending || forceRefundMutation.isPending

  const handleSelect = (ideaId: number | null) => {
    setSelectedIdeaId(ideaId)
    if (ideaId) {
      router.replace(`/admin/settlements?ideaId=${ideaId}`, { scroll: false })
    } else {
      router.replace('/admin/settlements', { scroll: false })
    }
  }

  const selectedIdea = ideas.find((i) => i.ideaId === selectedIdeaId)
  const statusBadge = deposit ? getDepositStatusBadge(deposit.status) : null
  const isActionable = deposit ? DEPOSIT_ACTIONABLE_STATUSES.has(deposit.status) : false

  return (
    <>
      <div style={{ marginBottom: '24px' }}>
        <h1 style={{ fontSize: '22px', fontWeight: 800, color: 'var(--fg)', marginBottom: '4px' }}>
          💼 정산/보증금 관리
        </h1>
        <p style={{ fontSize: '14px', color: 'var(--fg-muted)' }}>
          아이디어를 선택해 보증금 현황을 조회하고 환급·몰수·강제 환불을 처리합니다.
        </p>
      </div>

      {/* 아이디어 선택 드롭다운 */}
      <div style={{ marginBottom: '28px' }}>
        <label style={{ display: 'block', fontSize: '13px', fontWeight: 600, color: 'var(--fg-muted)', marginBottom: '8px' }}>
          아이디어 선택
        </label>
        {ideasLoading ? (
          <div style={{ padding: '10px 0' }}><LoadingSpinner /></div>
        ) : ideas.length === 0 ? (
          <div style={{
            padding: '14px 16px', borderRadius: '8px',
            background: 'var(--bg-alt)', border: '1.5px solid var(--border)',
            fontSize: '14px', color: 'var(--fg-muted)',
          }}>
            등록된 아이디어가 없습니다.
          </div>
        ) : (
          <select
            value={selectedIdeaId ?? ''}
            onChange={(e) => {
              const val = e.target.value
              handleSelect(val ? parseInt(val, 10) : null)
            }}
            style={{
              width: '100%', maxWidth: '560px',
              padding: '11px 14px', borderRadius: '8px',
              border: '1.5px solid var(--border)', fontSize: '14px',
              color: selectedIdeaId ? 'var(--fg)' : 'var(--fg-muted)',
              background: '#fff', outline: 'none', cursor: 'pointer',
              appearance: 'none',
              backgroundImage: `url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='12' height='12' viewBox='0 0 12 12'%3E%3Cpath fill='%23888' d='M6 8L1 3h10z'/%3E%3C/svg%3E")`,
              backgroundRepeat: 'no-repeat',
              backgroundPosition: 'right 14px center',
              paddingRight: '36px',
            }}
          >
            <option value="">아이디어를 선택하세요</option>
            {ideas.map((idea) => (
              <option key={idea.ideaId} value={idea.ideaId}>
                #{idea.ideaId} [{IDEA_STATUS_KO[idea.status] ?? idea.status}] — {idea.title}
              </option>
            ))}
          </select>
        )}
      </div>

      {/* 결과 */}
      {selectedIdeaId !== null && (
        depositLoading ? (
          <LoadingSpinner />
        ) : !deposit ? (
          <div style={{
            padding: '60px', textAlign: 'center',
            background: 'var(--bg-alt)', borderRadius: '12px',
            color: 'var(--fg-muted)', fontSize: '15px',
          }}>
            아이디어 #{selectedIdeaId}의 보증금 정보를 찾을 수 없습니다.
          </div>
        ) : (
          <>
            {/* 보증금 정보 카드 */}
            <div style={{
              background: '#fff', border: '1px solid var(--border)',
              borderRadius: '12px', padding: '24px',
              marginBottom: '16px',
            }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '10px', marginBottom: '20px' }}>
                <h2 style={{ fontSize: '16px', fontWeight: 700, color: 'var(--fg)' }}>
                  {selectedIdea ? `"${selectedIdea.title}"` : `아이디어 #${selectedIdeaId}`} 보증금
                </h2>
                {statusBadge && (
                  <Badge variant={statusBadge.variant}>{statusBadge.label}</Badge>
                )}
              </div>

              <div style={{
                display: 'grid',
                gridTemplateColumns: 'repeat(auto-fill, minmax(200px, 1fr))',
                gap: '16px',
                marginBottom: '24px',
              }}>
                {[
                  { label: '아이디어 ID', value: `#${deposit.ideaId}` },
                  { label: '사용자 ID',   value: `#${deposit.userId}` },
                  { label: '보증금 금액', value: formatCurrency(deposit.amount), highlight: true },
                  { label: '보증금 ID',   value: deposit.depositId !== null ? `#${deposit.depositId}` : '—' },
                  { label: '납입일',       value: deposit.paidAt ? formatDateTime(deposit.paidAt) : '—' },
                  { label: '처리일',       value: deposit.releasedAt ? formatDateTime(deposit.releasedAt) : '—' },
                ].map(({ label, value, highlight }) => (
                  <div key={label}>
                    <div style={{ fontSize: '12px', color: 'var(--fg-muted)', marginBottom: '4px' }}>
                      {label}
                    </div>
                    <div style={{
                      fontSize: highlight ? '18px' : '14px',
                      fontWeight: highlight ? 800 : 600,
                      color: highlight ? 'var(--brand-dark)' : 'var(--fg)',
                    }}>
                      {value}
                    </div>
                  </div>
                ))}
              </div>

              {/* 액션 버튼 */}
              {isActionable ? (
                <div style={{
                  display: 'flex', gap: '10px', flexWrap: 'wrap',
                  paddingTop: '20px', borderTop: '1px solid var(--border)',
                }}>
                  <button
                    onClick={() => {
                      if (confirm(`"${selectedIdea?.title ?? `아이디어 #${deposit.ideaId}`}"의 보증금을 환급하시겠습니까?`)) {
                        releaseMutation.mutate(deposit.ideaId)
                      }
                    }}
                    disabled={isMutating}
                    style={{
                      padding: '9px 20px', borderRadius: '8px',
                      border: '1.5px solid #86efac', background: '#f0fdf4',
                      color: '#1a7a3f', fontWeight: 700, fontSize: '14px',
                      cursor: isMutating ? 'not-allowed' : 'pointer',
                      opacity: isMutating ? 0.6 : 1,
                    }}
                  >
                    ✅ 환급
                  </button>
                  <button
                    onClick={() => {
                      if (confirm(`"${selectedIdea?.title ?? `아이디어 #${deposit.ideaId}`}"의 보증금을 몰수하시겠습니까?`)) {
                        forfeitMutation.mutate(deposit.ideaId)
                      }
                    }}
                    disabled={isMutating}
                    style={{
                      padding: '9px 20px', borderRadius: '8px',
                      border: '1.5px solid #fca5a5', background: '#fff',
                      color: '#dc2626', fontWeight: 700, fontSize: '14px',
                      cursor: isMutating ? 'not-allowed' : 'pointer',
                      opacity: isMutating ? 0.6 : 1,
                    }}
                  >
                    ⛔ 몰수
                  </button>
                </div>
              ) : (
                <div style={{
                  paddingTop: '20px', borderTop: '1px solid var(--border)',
                  fontSize: '13px', color: 'var(--fg-muted)',
                }}>
                  이 보증금은 이미 처리가 완료되었습니다.
                </div>
              )}
            </div>

            {/* 강제 전체 환불 — 위험 구역 */}
            <div style={{
              padding: '20px', borderRadius: '12px',
              border: '1.5px solid #fca5a5', background: '#fff5f5',
            }}>
              <p style={{ fontSize: '14px', fontWeight: 700, color: '#dc2626', marginBottom: '6px' }}>
                위험 구역 — 강제 전체 환불
              </p>
              <p style={{ fontSize: '13px', color: '#dc2626', marginBottom: '14px', opacity: 0.8 }}>
                {selectedIdea ? `"${selectedIdea.title}"` : `아이디어 #${deposit.ideaId}`}의 모든 후원금을 환불하고 보증금을 몰수합니다.
                이 작업은 되돌릴 수 없습니다.
              </p>
              <button
                onClick={() => {
                  if (confirm(`모든 후원금을 강제 환불하시겠습니까?\n보증금은 몰수됩니다. 이 작업은 되돌릴 수 없습니다.`)) {
                    forceRefundMutation.mutate(deposit.ideaId)
                  }
                }}
                disabled={isMutating}
                style={{
                  padding: '9px 20px', borderRadius: '8px', border: 'none',
                  background: isMutating ? '#fca5a5' : '#dc2626',
                  color: '#fff', fontWeight: 700, fontSize: '14px',
                  cursor: isMutating ? 'not-allowed' : 'pointer',
                }}
              >
                💸 강제 전체 환불
              </button>
            </div>
          </>
        )
      )}
    </>
  )
}

export default function AdminSettlementsPage() {
  return (
    <Suspense fallback={<LoadingSpinner />}>
      <AdminSettlementsContent />
    </Suspense>
  )
}
