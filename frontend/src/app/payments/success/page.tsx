'use client'

import { Suspense, useEffect, useRef, useState } from 'react'
import Link from 'next/link'
import { useRouter, useSearchParams } from 'next/navigation'
import { useMutation } from '@tanstack/react-query'
import { paymentsApi } from '@/api/payments'
import { ProtectedRoute } from '@/components/layout/AppShell'
import { LoadingSpinner } from '@/components/ui/LoadingSpinner'
import { Card } from '@/components/ui/Card'
import { Button } from '@/components/ui/Button'
import { formatCurrency, getErrorMessage } from '@/utils/format'
import type { VbankInfo } from '@/types/funding'

function PaymentSuccessContent() {
  const router = useRouter()
  const searchParams = useSearchParams()
  const confirmed = useRef(false)

  const paymentKey = searchParams.get('paymentKey') ?? ''
  const orderId = searchParams.get('orderId') ?? ''
  const amount = Number(searchParams.get('amount') ?? 0)
  const paymentId = Number(searchParams.get('paymentId') ?? 0)
  const ideaId = Number(searchParams.get('ideaId') ?? 0)
  const context = searchParams.get('context') ?? 'sponsor'

  const [vbank, setVbank] = useState<VbankInfo | null>(null)
  const [error, setError] = useState('')

  const confirmMutation = useMutation({
    mutationFn: () =>
      paymentsApi.confirm(paymentId, {
        paymentKey,
        amount,
      }),
    onSuccess: (data) => {
      if (data.vbank) setVbank(data.vbank)
    },
    onError: (err) => setError(getErrorMessage(err)),
  })

  useEffect(() => {
    if (confirmed.current) return
    if (!paymentKey || !paymentId || !amount) {
      setError('결제 정보가 올바르지 않습니다. 다시 시도해주세요.')
      return
    }
    confirmed.current = true
    confirmMutation.mutate()
  }, [paymentKey, paymentId, amount])

  if (!paymentKey || !paymentId) {
    return (
      <div className="mx-auto max-w-md px-4 py-16 text-center">
        <p className="text-red-600">{error || '결제 정보를 확인할 수 없습니다.'}</p>
        <Button className="mt-6" onClick={() => router.push('/')}>홈으로</Button>
      </div>
    )
  }

  if (confirmMutation.isPending) {
    return (
      <div className="mx-auto max-w-md px-4 py-16 text-center">
        <LoadingSpinner />
        <p className="mt-4 text-sm text-slate-500">결제를 승인하는 중입니다…</p>
      </div>
    )
  }

  if (error || confirmMutation.isError) {
    return (
      <div className="mx-auto max-w-md px-4 py-16 text-center">
        <div className="text-4xl mb-4">⚠️</div>
        <h1 className="text-xl font-bold">결제 승인 실패</h1>
        <p className="mt-2 text-sm text-red-600">{error || '결제 승인에 실패했습니다.'}</p>
        <p className="mt-2 text-xs text-slate-400">orderId: {orderId}</p>
        <Button className="mt-6" variant="outline" onClick={() => router.back()}>돌아가기</Button>
      </div>
    )
  }

  if (vbank) {
    return (
      <div className="mx-auto max-w-md px-4 py-16 text-center">
        <div className="text-4xl mb-4">🏦</div>
        <h1 className="text-xl font-bold mb-6">가상계좌 발급 완료</h1>
        <Card className="text-left space-y-3">
          <div className="flex justify-between text-sm"><span className="text-slate-500">은행</span><span>{vbank.bankCode}</span></div>
          <div className="flex justify-between text-sm"><span className="text-slate-500">계좌번호</span><span className="font-semibold">{vbank.accountNumber}</span></div>
          <div className="flex justify-between text-sm"><span className="text-slate-500">입금기한</span><span>{new Date(vbank.dueDate).toLocaleString('ko-KR')}</span></div>
          <div className="flex justify-between text-sm"><span className="text-slate-500">입금금액</span><span className="font-bold text-indigo-600">{formatCurrency(amount)}</span></div>
        </Card>
        <p className="mt-4 text-sm text-slate-500">기한 내 입금하시면 결제가 확정됩니다.</p>
        <Link href={context === 'deposit' ? `/ideas/${ideaId}` : `/fundings/idea/${ideaId}`}>
          <Button className="mt-6 w-full">돌아가기</Button>
        </Link>
      </div>
    )
  }

  return (
    <div className="mx-auto max-w-md px-4 py-16 text-center">
      <div className="text-5xl mb-4">🎉</div>
      <h1 className="text-2xl font-bold">
        {context === 'deposit' ? '보증금 납부 완료!' : '후원이 완료되었습니다!'}
      </h1>
      <p className="mt-2 text-slate-500">{formatCurrency(amount)} 결제가 승인되었습니다.</p>
      <Card className="mt-6 text-left text-sm text-slate-500 space-y-1">
        <p>주문번호: {orderId}</p>
        <p>토스 paymentKey: {paymentKey.slice(0, 20)}…</p>
      </Card>
      <div className="mt-6 flex flex-col gap-3">
        {context === 'sponsor' && ideaId > 0 && (
          <Link href={`/workspaces/${ideaId}`}>
            <Button className="w-full">워크스페이스 입장</Button>
          </Link>
        )}
        <Link href={context === 'deposit' ? `/ideas/${ideaId}` : `/fundings/idea/${ideaId}`}>
          <Button variant="outline" className="w-full">
            {context === 'deposit' ? '아이디어 보러가기' : '펀딩 상세로'}
          </Button>
        </Link>
      </div>
    </div>
  )
}

export default function PaymentSuccessPage() {
  return (
    <ProtectedRoute roles={['USER']}>
      <Suspense fallback={<LoadingSpinner />}>
        <PaymentSuccessContent />
      </Suspense>
    </ProtectedRoute>
  )
}
