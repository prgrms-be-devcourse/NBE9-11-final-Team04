import { loadTossPayments } from '@tosspayments/payment-sdk'
import { getErrorMessage } from '@/utils/format'

export type TossCheckoutContext = 'sponsor' | 'deposit'

type OpenTossPaymentParams = {
  clientKey: string
  method: 'CARD' | 'VIRTUAL_ACCOUNT'
  amount: number
  orderId: string
  orderName: string
  paymentId: number
  ideaId: number
  context: TossCheckoutContext
  customerName?: string
}

function tossMethod(method: 'CARD' | 'VIRTUAL_ACCOUNT') {
  return method === 'CARD' ? '카드' : '가상계좌'
}

function buildReturnQuery(paymentId: number, ideaId: number, context: TossCheckoutContext) {
  return new URLSearchParams({
    paymentId: String(paymentId),
    ideaId: String(ideaId),
    context,
  }).toString()
}

/** 토스페이먼츠 결제창을 띄웁니다. 성공 시 successUrl, 실패·취소 시 예외 또는 failUrl로 이동합니다. */
export async function openTossPaymentWindow(params: OpenTossPaymentParams) {
  const origin = typeof window !== 'undefined' ? window.location.origin : ''
  const returnQuery = buildReturnQuery(params.paymentId, params.ideaId, params.context)

  const tossPayments = await loadTossPayments(params.clientKey)
  await tossPayments.requestPayment(tossMethod(params.method), {
    amount: params.amount,
    orderId: params.orderId,
    orderName: params.orderName.slice(0, 100),
    customerName: params.customerName ?? 'SeedLink 사용자',
    successUrl: `${origin}/payments/success?${returnQuery}`,
    failUrl: `${origin}/payments/fail?${returnQuery}`,
  })
}

export function isTossUserCancel(error: unknown) {
  return typeof error === 'object' && error !== null && 'code' in error && (error as { code: string }).code === 'USER_CANCEL'
}

export function resolveTossOpenError(error: unknown) {
  if (isTossUserCancel(error)) return null
  return getErrorMessage(error)
}
