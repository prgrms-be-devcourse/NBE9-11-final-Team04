'use client'

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
                  borderRadius: '12px',
                  padding: '16px 20px',
                  display: 'flex',
                  alignItems: 'center',
                  gap: '16px',
                }}
              >
                <div style={{
                  width: '44px',
                  height: '44px',
                  borderRadius: '8px',
                  background: 'var(--brand-tint)',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  fontSize: '20px',
                  flexShrink: 0,
                }}>
                  💳
                </div>
                <div style={{ flex: 1 }}>
                  <p style={{ fontSize: '16px', fontWeight: 700, color: 'var(--fg)', marginBottom: '4px' }}>
                    {formatCurrency(p.amount)}
                  </p>
                  <p style={{ fontSize: '12px', color: 'var(--fg-muted)' }}>
                    결제 ID #{p.paymentId} · {formatDateTime(p.createdAt)}
                  </p>
                </div>
                <Badge variant={PAYMENT_VARIANT[p.status as PaymentStatus]}>
                  {PAYMENT_STATUS_LABELS[p.status as PaymentStatus]}
                </Badge>

{/* SUCCESS: 환불 신청 버튼 */}
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
                      flexShrink: 0,
                    }}
                  >
                    {isProcessing ? '처리 중...' : '↩️ 환불 신청'}
                  </button>
                )}
              </div>
            )
          })}
        </div>
      )}
    </div>
  )
}
