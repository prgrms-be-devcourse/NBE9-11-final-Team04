import { apiClient, unwrap } from '@/api/client'
import type { ApiResponse, PageResponse } from '@/types/api'
import type { Payment } from '@/types/payment'

export const paymentsApi = {
  getMyPayments: (page = 0, size = 20) =>
    unwrap(
      apiClient.get<ApiResponse<PageResponse<Payment>>>('/payments/me', {
        params: { page, size },
      }),
    ),

  refund: (paymentId: number) =>
    unwrap(apiClient.post<ApiResponse<void>>(`/payments/${paymentId}/refund`)),
}
