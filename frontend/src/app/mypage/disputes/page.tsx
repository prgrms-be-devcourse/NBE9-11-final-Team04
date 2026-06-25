'use client'

import { useState } from 'react'
import Link from 'next/link'
import { useQuery } from '@tanstack/react-query'
import { disputesApi, DISPUTE_CATEGORY_LABELS, type DisputeCategory } from '@/api/disputes'
import { Badge } from '@/components/ui/Badge'
import { LoadingSpinner } from '@/components/ui/LoadingSpinner'
import { DISPUTE_STATUS_LABELS, type DisputeStatus } from '@/types/enums'
import { formatDateTime } from '@/utils/format'

const STATUS_VARIANT: Record<DisputeStatus, 'gray' | 'orange' | 'green' | 'red'> = {
  RECEIVED: 'gray',
  PENDING:  'orange',
  RESOLVED: 'green',
  REJECTED: 'red',
}

export default function DisputesPage() {
  const [page, setPage] = useState(0)

  const { data, isLoading } = useQuery({
    queryKey: ['disputes', 'me', page],
    queryFn: () => disputesApi.getMyDisputes(page, 10),
  })

  if (isLoading) return <LoadingSpinner />

  const items = data?.content ?? []
  const totalPages = data?.totalPages ?? 0

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '20px' }}>
      <h2 style={{ fontSize: '20px', fontWeight: 700, color: 'var(--fg)' }}>
        🚨 신고 내역
      </h2>

      {items.length === 0 ? (
        <div style={{
          padding: '60px 40px',
          textAlign: 'center',
          background: 'var(--bg-alt)',
          borderRadius: '12px',
          color: 'var(--fg-muted)',
          fontSize: '15px',
        }}>
          신고 내역이 없습니다.
        </div>
      ) : (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
          {items.map((d) => (
            <Link
              key={d.id}
              href={`/disputes/${d.id}`}
              style={{ textDecoration: 'none' }}
            >
              <div style={{
                background: '#fff',
                border: '1px solid var(--border)',
                borderRadius: '12px',
                padding: '16px 20px',
                display: 'flex',
                alignItems: 'center',
                gap: '16px',
                transition: 'border-color 0.15s',
                cursor: 'pointer',
              }}
                onMouseEnter={(e) => (e.currentTarget.style.borderColor = 'var(--brand)')}
                onMouseLeave={(e) => (e.currentTarget.style.borderColor = 'var(--border)')}
              >
                <div style={{
                  width: '44px',
                  height: '44px',
                  borderRadius: '8px',
                  background: '#fff5f5',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  fontSize: '20px',
                  flexShrink: 0,
                }}>
                  🚨
                </div>
                <div style={{ flex: 1, minWidth: 0 }}>
                  <p style={{
                    fontSize: '15px',
                    fontWeight: 700,
                    color: 'var(--fg)',
                    marginBottom: '4px',
                    overflow: 'hidden',
                    textOverflow: 'ellipsis',
                    whiteSpace: 'nowrap',
                  }}>
                    {d.title}
                  </p>
                  <p style={{ fontSize: '13px', color: 'var(--fg-muted)' }}>
                    {DISPUTE_CATEGORY_LABELS[d.category as DisputeCategory]} · {formatDateTime(d.createdAt)}
                  </p>
                </div>
                <Badge variant={STATUS_VARIANT[d.status as DisputeStatus]}>
                  {DISPUTE_STATUS_LABELS[d.status as DisputeStatus]}
                </Badge>
              </div>
            </Link>
          ))}
        </div>
      )}

      {totalPages > 1 && (
        <div style={{ display: 'flex', justifyContent: 'center', gap: '8px', marginTop: '8px' }}>
          <button
            onClick={() => setPage((p) => Math.max(0, p - 1))}
            disabled={page === 0}
            style={{
              padding: '8px 16px',
              border: '1px solid var(--border)',
              borderRadius: '8px',
              background: '#fff',
              cursor: page === 0 ? 'not-allowed' : 'pointer',
              color: page === 0 ? 'var(--fg-muted)' : 'var(--fg)',
              fontSize: '14px',
            }}
          >
            이전
          </button>
          <span style={{ padding: '8px 12px', fontSize: '14px', color: 'var(--fg-muted)' }}>
            {page + 1} / {totalPages}
          </span>
          <button
            onClick={() => setPage((p) => Math.min(totalPages - 1, p + 1))}
            disabled={page >= totalPages - 1}
            style={{
              padding: '8px 16px',
              border: '1px solid var(--border)',
              borderRadius: '8px',
              background: '#fff',
              cursor: page >= totalPages - 1 ? 'not-allowed' : 'pointer',
              color: page >= totalPages - 1 ? 'var(--fg-muted)' : 'var(--fg)',
              fontSize: '14px',
            }}
          >
            다음
          </button>
        </div>
      )}
    </div>
  )
}
