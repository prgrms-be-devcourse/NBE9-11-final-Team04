import { apiClient, unwrap } from '@/api/client'
import type { ApiResponse, PageResponse } from '@/types/api'
import type { PaymentResult } from '@/types/funding'
import type { Payment } from '@/types/payment'

export const paymentsApi = {
  getConfig: () =>
    unwrap(apiClient.get<ApiResponse<{ demoMode: boolean; clientKey: string; gatewayType: string }>>('/payments/config')),

  confirm: (paymentId: number, body: { paymentKey: string; amount: number }) =>
    unwrap(apiClient.post<ApiResponse<PaymentResult>>(`/payments/${paymentId}/confirm`, body)),

  getMyPayments: (page = 0, size = 20) =>
    unwrap(
      apiClient.get<ApiResponse<PageResponse<Payment>>>('/payments/me', {
        params: { page, size },
      }),
    ),

  refund: (paymentId: number) =>
    unwrap(apiClient.post<ApiResponse<void>>(`/payments/${paymentId}/refund`)),

  demoConfirm: (paymentId: number) =>
    unwrap(apiClient.post<ApiResponse<Payment>>(`/payments/${paymentId}/demo-confirm`)),
}
