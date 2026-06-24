'use client'

import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { notificationsApi } from '@/api/notifications'
import { Button } from '@/components/ui/Button'
import { LoadingSpinner } from '@/components/ui/LoadingSpinner'
import { formatDateTime } from '@/utils/format'

export default function NotificationsPage() {
  const queryClient = useQueryClient()
  // AppShell의 useNotifications가 이미 캐시에 데이터를 올려두므로 SSE 중복 연결 없이 쿼리만 사용
  const { data, isLoading } = useQuery({
    queryKey: ['notifications'],
    queryFn: () => notificationsApi.getList(),
  })
  const markAll = useMutation({
    mutationFn: notificationsApi.markAllAsRead,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['notifications'] }),
  })
  const markRead = useMutation({
    mutationFn: notificationsApi.markAsRead,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['notifications'] }),
  })

  if (isLoading) return <LoadingSpinner />

  const items = data?.content ?? []

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '20px' }}>
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
        <h2 style={{ fontSize: '20px', fontWeight: 700, color: 'var(--fg)' }}>
          🔔 알림
        </h2>
        <Button variant="outline" size="sm" onClick={() => markAll.mutate()} loading={markAll.isPending}>
          전체 읽음
        </Button>
      </div>

      {items.length === 0 ? (
        <div style={{
          padding: '40px',
          textAlign: 'center',
          background: 'var(--bg-alt)',
          borderRadius: '12px',
          color: 'var(--fg-muted)',
          fontSize: '15px',
        }}>
          알림이 없습니다.
        </div>
      ) : (
        <div style={{ border: '1px solid var(--border)', borderRadius: '12px', overflow: 'hidden' }}>
          {items.map((n, i) => (
            <div
              key={n.id}
              onClick={() => !n.isRead && markRead.mutate(n.id)}
              style={{
                display: 'flex',
                gap: '14px',
                alignItems: 'flex-start',
                padding: '16px 20px',
                borderBottom: i < items.length - 1 ? '1px solid var(--border)' : 'none',
                background: n.isRead ? '#fff' : 'var(--brand-tint)',
                borderLeft: n.isRead ? 'none' : '3px solid var(--brand)',
                cursor: n.isRead ? 'default' : 'pointer',
                transition: 'background 0.2s',
              }}
            >
              <div style={{
                width: '36px',
                height: '36px',
                borderRadius: '50%',
                background: 'var(--brand-tint)',
                border: '1px solid var(--border)',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                fontSize: '16px',
                flexShrink: 0,
              }}>
                🔔
              </div>
              <div style={{ flex: 1 }}>
                <p style={{ fontSize: '14px', fontWeight: n.isRead ? 500 : 700, color: 'var(--fg)', marginBottom: '3px' }}>
                  {n.title}
                </p>
                <p style={{ fontSize: '13px', color: 'var(--fg-muted)', lineHeight: 1.5 }}>
                  {n.message}
                </p>
              </div>
              <span style={{ fontSize: '12px', color: 'var(--fg-muted)', whiteSpace: 'nowrap', flexShrink: 0 }}>
                {formatDateTime(n.createdAt)}
              </span>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}
