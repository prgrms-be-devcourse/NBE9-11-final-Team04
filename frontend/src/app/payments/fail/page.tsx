'use client'

import { Suspense, useEffect, useRef } from 'react'
import { useRouter, useSearchParams } from 'next/navigation'
import { useMutation } from '@tanstack/react-query'
import { fundingsApi } from '@/api/fundings'
import { ProtectedRoute } from '@/components/layout/AppShell'
import { LoadingSpinner } from '@/components/ui/LoadingSpinner'
import { Button } from '@/components/ui/Button'

function PaymentFailContent() {
  const router = useRouter()
  const searchParams = useSearchParams()
  const cleaned = useRef(false)

  const code = searchParams.get('code') ?? ''
  const message = searchParams.get('message') ?? '결제가 취소되었거나 실패했습니다.'
  const ideaId = Number(searchParams.get('ideaId') ?? 0)
  const context = searchParams.get('context') ?? 'sponsor'

  const cancelMutation = useMutation({
    mutationFn: () => fundingsApi.cancelSponsor(ideaId),
  })

  useEffect(() => {
    if (cleaned.current || context !== 'sponsor' || !ideaId) return
    cleaned.current = true
    cancelMutation.mutate()
  }, [context, ideaId])

  const backHref = context === 'deposit' ? `/ideas/${ideaId}/deposit` : `/fundings/idea/${ideaId}`

  return (
    <div className="mx-auto max-w-md px-4 py-16 text-center">
      <div className="text-4xl mb-4">😔</div>
      <h1 className="text-xl font-bold">결제에 실패했습니다</h1>
      <p className="mt-3 text-sm text-slate-600">{decodeURIComponent(message)}</p>
      {code && <p className="mt-1 text-xs text-slate-400">코드: {code}</p>}
      {cancelMutation.isPending && (
        <p className="mt-4 text-xs text-slate-400">후원 신청을 정리하는 중…</p>
      )}
      <div className="mt-8 flex flex-col gap-3">
        {ideaId > 0 && (
          <Button onClick={() => router.push(backHref)}>다시 시도하기</Button>
        )}
        <Button variant="outline" onClick={() => router.push('/')}>홈으로</Button>
      </div>
    </div>
  )
}

export default function PaymentFailPage() {
  return (
    <ProtectedRoute roles={['USER']}>
      <Suspense fallback={<LoadingSpinner />}>
        <PaymentFailContent />
      </Suspense>
    </ProtectedRoute>
  )
}
