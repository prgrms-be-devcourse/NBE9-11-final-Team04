import { apiClient, unwrap } from '@/api/client'
import type { ApiResponse, PageResponse } from '@/types/api'
import type { Notification } from '@/types/notification'

export const notificationsApi = {
  getList: (page = 0, size = 20) =>
    unwrap(
      apiClient.get<ApiResponse<PageResponse<Notification>>>('/notifications', {
        params: { page, size },
      }),
    ),

  markAsRead: (notificationId: number) =>
    unwrap(apiClient.patch<ApiResponse<void>>(`/notifications/${notificationId}/read`)),

  markAllAsRead: () => unwrap(apiClient.patch<ApiResponse<void>>('/notifications/read-all')),
}
