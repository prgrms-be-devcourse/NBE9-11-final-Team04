'use client'

import { Button } from '@/components/ui/Button'
import { formatCurrency } from '@/utils/format'

type Props = {
  open: boolean
  amount: number
  method: 'CARD' | 'VIRTUAL_ACCOUNT'
  orderLabel?: string
  loading?: boolean
  onConfirm: () => void
  onCancel: () => void
}

/** 로컬/mock·시연용 결제창 UI (토스 위젯 대체) */
export function PaymentCheckoutModal({
  open,
  amount,
  method,
  orderLabel,
  loading,
  onConfirm,
  onCancel,
}: Props) {
  if (!open) return null

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4">
      <div
        className="w-full max-w-md rounded-2xl bg-white shadow-2xl"
        role="dialog"
        aria-modal="true"
        aria-labelledby="checkout-title"
      >
        <div className="border-b border-slate-100 px-6 py-4">
          <p className="text-xs font-semibold uppercase tracking-wide text-indigo-600">SeedLink Pay</p>
          <h2 id="checkout-title" className="mt-1 text-lg font-bold text-slate-900">
            결제하기
          </h2>
          <p className="mt-1 text-sm text-slate-500">
            {method === 'CARD' ? '신용·체크카드' : '가상계좌'} · 시연 모드
          </p>
        </div>
        <div className="space-y-4 px-6 py-5">
          {orderLabel && (
            <div className="flex justify-between text-sm">
              <span className="text-slate-500">주문</span>
              <span className="font-medium text-slate-800">{orderLabel}</span>
            </div>
          )}
          <div className="flex justify-between text-sm">
            <span className="text-slate-500">결제 수단</span>
            <span className="font-medium">{method === 'CARD' ? '💳 카드' : '🏦 가상계좌'}</span>
          </div>
          <div className="rounded-xl bg-slate-50 px-4 py-3">
            <p className="text-xs text-slate-500">결제 금액</p>
            <p className="text-2xl font-bold text-indigo-600">{formatCurrency(amount)}</p>
          </div>
          <p className="text-xs leading-relaxed text-slate-400">
            시연 환경에서는 PG(mock)로 처리됩니다. 실제 운영 시 토스페이먼츠 결제창으로 연결됩니다.
          </p>
        </div>
        <div className="flex gap-3 border-t border-slate-100 px-6 py-4">
          <Button variant="outline" className="flex-1" onClick={onCancel} disabled={loading}>
            취소
          </Button>
          <Button className="flex-1" onClick={onConfirm} loading={loading}>
            {formatCurrency(amount)} 결제
          </Button>
        </div>
      </div>
    </div>
  )
}
