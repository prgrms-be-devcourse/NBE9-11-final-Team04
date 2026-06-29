import type { PaymentResult, VbankInfo } from '@/types/funding'
import { openTossPaymentWindow, isTossUserCancel, resolveTossOpenError } from '@/lib/tossPayments'
import type { TossCheckoutContext } from '@/lib/tossPayments'

type PaymentConfig = {
  demoMode: boolean
  clientKey: string
  gatewayType: string
}

type CheckoutHandlers = {
  onMockModal: (paymentId: number) => void
  onVbank: (vbank: VbankInfo) => void
  onUserCancel: () => void
  onError: (message: string) => void
}

/** 후원·보증금 공통 — PG 유형에 따라 mock 모달 / 토스 결제창 / 가상계좌 안내로 분기합니다. */
export async function startPaymentCheckout(
  payment: PaymentResult,
  config: PaymentConfig | undefined,
  paymentMethod: 'CARD' | 'VIRTUAL_ACCOUNT',
  ideaId: number,
  orderName: string,
  context: TossCheckoutContext,
  handlers: CheckoutHandlers,
  customerName?: string,
) {
  const isMockUrl = payment.redirectUrl?.includes('mock-pg.local')

  if (payment.redirectUrl && !isMockUrl) {
    window.location.href = payment.redirectUrl
    return
  }

  if (payment.vbank) {
    handlers.onVbank(payment.vbank)
    return
  }

  const gatewayType = config?.gatewayType ?? 'mock'
  const useToss = gatewayType === 'toss' && !!payment.clientKey

  if (useToss) {
    try {
      await openTossPaymentWindow({
        clientKey: payment.clientKey!,
        method: paymentMethod,
        amount: payment.amount,
        orderId: payment.orderId,
        orderName,
        paymentId: payment.paymentId,
        ideaId,
        context,
        customerName,
      })
    } catch (error) {
      if (isTossUserCancel(error)) {
        handlers.onUserCancel()
        return
      }
      const message = resolveTossOpenError(error)
      if (message) handlers.onError(message)
    }
    return
  }

  if (config?.demoMode !== false || isMockUrl) {
    handlers.onMockModal(payment.paymentId)
    return
  }

  handlers.onError('지원하지 않는 결제 환경입니다.')
}
