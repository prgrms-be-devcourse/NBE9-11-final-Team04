'use client'

import { useQuery } from '@tanstack/react-query'
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
  const { data, isLoading, error } = useQuery({
    queryKey: ['payments', 'me'],
    queryFn: () => paymentsApi.getMyPayments(),
    retry: false,
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
          {items.map((p) => (
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
                <p style={{ fontSize: '13px', color: 'var(--fg-muted)' }}>
                  {formatDateTime(p.createdAt)}
                </p>
              </div>
              <Badge variant={PAYMENT_VARIANT[p.status as PaymentStatus]}>
                {PAYMENT_STATUS_LABELS[p.status as PaymentStatus]}
              </Badge>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}
