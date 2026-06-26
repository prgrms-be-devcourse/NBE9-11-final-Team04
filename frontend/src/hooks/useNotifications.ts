'use client'

import { useState } from 'react'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import { notificationsApi } from '@/api/notifications'
import { useSse } from '@/hooks/useSse'
import type { Notification } from '@/types/notification'

export function useNotifications(enabled = true) {
  const queryClient = useQueryClient()
  const [latestNotification, setLatestNotification] = useState<Notification | null>(null)

  const query = useQuery({
    queryKey: ['notifications'],
    queryFn: () => notificationsApi.getList(),
    enabled,
  })

  useSse<Notification>({
    url: '/notifications/subscribe',
    enabled,
    onMessage: (notification) => {
      queryClient.setQueryData(
        ['notifications'],
        (old: Awaited<ReturnType<typeof notificationsApi.getList>> | undefined) => {
          if (!old) return old
          return {
            ...old,
            content: [notification, ...old.content],
            totalElements: old.totalElements + 1,
          }
        },
      )
      setLatestNotification(notification)
    },
  })

  const unreadCount = query.data?.content.filter((n) => !n.isRead).length ?? 0
  return { ...query, unreadCount, latestNotification }
}
