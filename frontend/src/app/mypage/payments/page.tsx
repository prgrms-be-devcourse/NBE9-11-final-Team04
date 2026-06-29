'use client'

import Link from 'next/link'
import { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { paymentsApi } from '@/api/payments'
import { Badge } from '@/components/ui/Badge'
import { LoadingSpinner } from '@/components/ui/LoadingSpinner'
import { PAYMENT_STATUS_LABELS, type PaymentStatus } from '@/types/enums'
import { formatCurrency, formatDateTime } from '@/utils/format'

const PAYMENT_VARIANT: Record<PaymentStatus, 'green' | 'orange' | 'red' | 'gray'> = {
  PENDING:  'gray',
  SUCCESS:  'green',
  REFUNDED: 'orange',
  FAILED:   'red',
}

const METHOD_LABEL: Record<string, string> = {
  CARD: '카드',
  VIRTUAL_ACCOUNT: '가상계좌',
}

export default function PaymentsPage() {
  const queryClient = useQueryClient()
  const [processingId, setProcessingId] = useState<number | null>(null)

  const { data, isLoading, error } = useQuery({
    queryKey: ['payments', 'me'],
    queryFn: () => paymentsApi.getMyPayments(),
    retry: false,
  })

  const refundMutation = useMutation({
    mutationFn: (paymentId: number) => paymentsApi.refund(paymentId),
    onMutate: (paymentId) => setProcessingId(paymentId),
    onSettled: () => setProcessingId(null),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['payments', 'me'] })
      queryClient.invalidateQueries({ queryKey: ['refunds', 'me'] })
    },
    onError: () => alert('환불 처리 중 오류가 발생했습니다.'),
  })

  if (isLoading) return <LoadingSpinner />

  const items = data?.content ?? []

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '20px' }}>
      <h2 style={{ fontSize: '20px', fontWeight: 700, color: 'var(--fg)' }}>
        💰 결제 내역
      </h2>

      {error || items.length === 0 ? (
        <div style={{
          padding: '40px',
          textAlign: 'center',
          background: 'var(--bg-alt)',
          borderRadius: '12px',
          color: 'var(--fg-muted)',
          fontSize: '15px',
        }}>
          결제 내역이 없습니다.
        </div>
      ) : (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
          {items.map((p) => {
            const isProcessing = processingId === p.paymentId
            return (
              <div
                key={p.paymentId}
                style={{
                  background: '#fff',
                  border: '1px solid var(--border)',
                  borderRadius: '14px',
                  overflow: 'hidden',
                }}
              >
                {/* 아이디어 제목 헤더 */}
                <div style={{
                  padding: '12px 20px',
                  background: 'var(--bg-alt)',
                  borderBottom: '1px solid var(--border)',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'space-between',
                  gap: '12px',
                }}>
                  <div style={{ display: 'flex', alignItems: 'center', gap: '8px', minWidth: 0 }}>
                    <span style={{ fontSize: '16px' }}>💡</span>
                    {p.ideaId ? (
                      <Link
                        href={`/ideas/${p.ideaId}`}
                        style={{
                          fontSize: '14px',
                          fontWeight: 700,
                          color: 'var(--fg)',
                          textDecoration: 'none',
                          overflow: 'hidden',
                          textOverflow: 'ellipsis',
                          whiteSpace: 'nowrap',
                        }}
                      >
                        {p.ideaTitle ?? '아이디어 보기'}
                      </Link>
                    ) : (
                      <span style={{ fontSize: '14px', fontWeight: 700, color: 'var(--fg-muted)' }}>
                        {p.ideaTitle ?? '—'}
                      </span>
                    )}
                  </div>
                  {p.ideaId && (
                    <Link
                      href={`/ideas/${p.ideaId}`}
                      style={{
                        fontSize: '12px',
                        color: 'var(--brand)',
                        fontWeight: 600,
                        textDecoration: 'none',
                        flexShrink: 0,
                      }}
                    >
                      프로젝트 보기 →
                    </Link>
                  )}
                </div>

                {/* 결제 상세 */}
                <div style={{
                  padding: '16px 20px',
                  display: 'flex',
                  alignItems: 'center',
                  gap: '16px',
                }}>
                  <div style={{
                    width: '44px',
                    height: '44px',
                    borderRadius: '10px',
                    background: 'var(--brand-tint)',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    fontSize: '20px',
                    flexShrink: 0,
                  }}>
                    {p.method === 'VIRTUAL_ACCOUNT' ? '🏦' : '💳'}
                  </div>

                  <div style={{ flex: 1, minWidth: 0 }}>
                    <p style={{ fontSize: '18px', fontWeight: 700, color: 'var(--fg)', marginBottom: '4px' }}>
                      {formatCurrency(p.amount)}
                    </p>
                    <div style={{ display: 'flex', alignItems: 'center', gap: '8px', flexWrap: 'wrap' }}>
                      <span style={{ fontSize: '12px', color: 'var(--fg-muted)' }}>
                        {METHOD_LABEL[p.method] ?? p.method}
                      </span>
                      <span style={{ fontSize: '12px', color: 'var(--border)' }}>·</span>
                      <span style={{ fontSize: '12px', color: 'var(--fg-muted)' }}>
                        {formatDateTime(p.createdAt)}
                      </span>
                    </div>
                  </div>

                  <div style={{ display: 'flex', alignItems: 'center', gap: '10px', flexShrink: 0 }}>
                    <Badge variant={PAYMENT_VARIANT[p.status as PaymentStatus]}>
                      {PAYMENT_STATUS_LABELS[p.status as PaymentStatus]}
                    </Badge>

                    {p.status === 'SUCCESS' && (
                      <button
                        onClick={() => {
                          if (confirm(`${formatCurrency(p.amount)} 환불을 신청하시겠습니까?`)) {
                            refundMutation.mutate(p.paymentId)
                          }
                        }}
                        disabled={isProcessing}
                        style={{
                          border: '1.5px solid #fca5a5',
                          color: '#dc2626',
                          background: '#fff',
                          padding: '6px 14px',
                          borderRadius: '8px',
                          fontSize: '13px',
                          fontWeight: 600,
                          cursor: isProcessing ? 'not-allowed' : 'pointer',
                          opacity: isProcessing ? 0.6 : 1,
                        }}
                      >
                        {isProcessing ? '처리 중...' : '↩️ 환불'}
                      </button>
                    )}
                  </div>
                </div>
              </div>
            )
          })}
        </div>
      )}
    </div>
  )
}
