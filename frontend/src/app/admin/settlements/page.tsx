'use client'

import { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { apiClient, unwrap } from '@/api/client'
import { Badge } from '@/components/ui/Badge'
import { LoadingSpinner } from '@/components/ui/LoadingSpinner'
import type { ApiResponse } from '@/types/api'
import { formatCurrency, formatDateTime } from '@/utils/format'

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
}

type DepositStatus = 'HELD' | 'REFUNDED' | 'FORFEITED' | 'PENDING_PAYMENT'

const DEPOSIT_STATUS_BADGE: Record<string, { variant: 'green' | 'orange' | 'red' | 'gray'; label: string }> = {
  HELD:            { variant: 'orange', label: '보유중' },
  REFUNDED:        { variant: 'green',  label: '환급됨' },
  FORFEITED:       { variant: 'red',    label: '몰수됨' },
  PENDING_PAYMENT: { variant: 'gray',   label: '납입 대기' },
}

function getDepositStatusBadge(status: string) {
  return DEPOSIT_STATUS_BADGE[status] ?? { variant: 'gray' as const, label: status }
}

export default function AdminSettlementsPage() {
  const queryClient = useQueryClient()
  const [ideaIdInput, setIdeaIdInput] = useState('')
  const [searchedIdeaId, setSearchedIdeaId] = useState<number | null>(null)

  const { data: deposit, isLoading } = useQuery({
    queryKey: ['admin', 'settlements', 'deposit', searchedIdeaId],
    queryFn: () => settlementApi.getDeposit(searchedIdeaId!),
    enabled: searchedIdeaId !== null,
  })

  const releaseMutation = useMutation({
    mutationFn: (ideaId: number) => settlementApi.release(ideaId),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['admin', 'settlements', 'deposit', searchedIdeaId] }),
    onError: () => alert('보증금 환급 처리 중 오류가 발생했습니다.'),
  })

  const forfeitMutation = useMutation({
    mutationFn: (ideaId: number) => settlementApi.forfeit(ideaId),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['admin', 'settlements', 'deposit', searchedIdeaId] }),
    onError: () => alert('보증금 몰수 처리 중 오류가 발생했습니다.'),
  })

  const forceRefundMutation = useMutation({
    mutationFn: (ideaId: number) => settlementApi.forceRefund(ideaId),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['admin', 'settlements', 'deposit', searchedIdeaId] }),
    onError: () => alert('강제 전체 환불 처리 중 오류가 발생했습니다.'),
  })

  const isMutating = releaseMutation.isPending || forfeitMutation.isPending || forceRefundMutation.isPending

  const handleSearch = (e: React.FormEvent) => {
    e.preventDefault()
    const parsed = parseInt(ideaIdInput, 10)
    if (!isNaN(parsed) && parsed > 0) {
      setSearchedIdeaId(parsed)
    }
  }

  const statusBadge = deposit ? getDepositStatusBadge(deposit.status) : null

  return (
    <>
      {/* 헤더 */}
      <div style={{ marginBottom: '24px' }}>
        <h1 style={{ fontSize: '22px', fontWeight: 800, color: 'var(--fg)', marginBottom: '4px' }}>
          💼 정산/보증금 관리
        </h1>
        <p style={{ fontSize: '14px', color: 'var(--fg-muted)' }}>
          아이디어 ID로 보증금 현황을 조회하고 환급·몰수·강제 환불을 처리합니다.
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
        ) : !deposit ? (
          <div style={{
            padding: '60px', textAlign: 'center',
            background: 'var(--bg-alt)', borderRadius: '12px',
            color: 'var(--fg-muted)', fontSize: '15px',
          }}>
            아이디어 #{searchedIdeaId}의 보증금 정보를 찾을 수 없습니다.
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
                  보증금 상세
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
              <div style={{
                display: 'flex', gap: '10px', flexWrap: 'wrap',
                paddingTop: '20px', borderTop: '1px solid var(--border)',
              }}>
                <button
                  onClick={() => {
                    if (confirm(`아이디어 #${deposit.ideaId}의 보증금을 환급하시겠습니까?`)) {
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
                    if (confirm(`아이디어 #${deposit.ideaId}의 보증금을 몰수하시겠습니까?`)) {
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
                아이디어 #{deposit.ideaId}의 모든 후원금을 환불하고 보증금을 몰수합니다.
                이 작업은 되돌릴 수 없습니다.
              </p>
              <button
                onClick={() => {
                  if (confirm(`아이디어 #${deposit.ideaId}의 전체 후원금을 강제 환불하시겠습니까?\n보증금은 몰수됩니다. 이 작업은 되돌릴 수 없습니다.`)) {
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
