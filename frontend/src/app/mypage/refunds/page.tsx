'use client'

import { useQuery } from '@tanstack/react-query'
import { apiClient, unwrap } from '@/api/client'
import type { ApiResponse } from '@/types/api'
import { Badge } from '@/components/ui/Badge'
import { LoadingSpinner } from '@/components/ui/LoadingSpinner'
import { formatCurrency, formatDateTime } from '@/utils/format'

interface RefundResponse {
  id: number
  paymentId: number
  sponsorId: number
  amount: number
  reason: 'GOAL_NOT_MET' | 'CANCELLED' | 'DISPUTE'
  status: 'PENDING' | 'COMPLETED' | 'FAILED'
  createdAt: string
}

const REASON_LABELS: Record<RefundResponse['reason'], string> = {
  GOAL_NOT_MET: '목표 미달성',
  CANCELLED:    '사업 중단',
  DISPUTE:      '분쟁 해결',
}

const STATUS_VARIANT: Record<RefundResponse['status'], 'green' | 'orange' | 'red'> = {
  PENDING:   'orange',
  COMPLETED: 'green',
  FAILED:    'red',
}

const STATUS_LABELS: Record<RefundResponse['status'], string> = {
  PENDING:   '처리 중',
  COMPLETED: '환불 완료',
  FAILED:    '환불 실패',
}

export default function RefundsPage() {
  const { data, isLoading, error } = useQuery({
    queryKey: ['refunds', 'me'],
    queryFn: () => unwrap(apiClient.get<ApiResponse<RefundResponse[]>>('/refunds/me')),
    retry: false,
  })

  if (isLoading) return <LoadingSpinner />

  const items = data ?? []

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '20px' }}>
      <h2 style={{ fontSize: '20px', fontWeight: 700, color: 'var(--fg)' }}>
        ↩️ 환불 내역
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
          환불 내역이 없습니다.
        </div>
      ) : (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
          {items.map((r) => (
            <div
              key={r.id}
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
                ↩️
              </div>
              <div style={{ flex: 1 }}>
                <p style={{ fontSize: '16px', fontWeight: 700, color: 'var(--fg)', marginBottom: '4px' }}>
                  {formatCurrency(r.amount)}
                </p>
                <p style={{ fontSize: '13px', color: 'var(--fg-muted)', marginBottom: '2px' }}>
                  {REASON_LABELS[r.reason]}
                </p>
                <p style={{ fontSize: '12px', color: 'var(--fg-muted)' }}>
                  {formatDateTime(r.createdAt)}
                </p>
              </div>
              <Badge variant={STATUS_VARIANT[r.status]}>
                {STATUS_LABELS[r.status]}
              </Badge>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}
