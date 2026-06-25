'use client'

import Link from 'next/link'
import { useParams } from 'next/navigation'
import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { ideasApi } from '@/api/ideas'
import { fundingsApi } from '@/api/fundings'
import { paymentsApi } from '@/api/payments'
import { useAuthStore } from '@/store/authStore'
import { useSse } from '@/hooks/useSse'
import { Card } from '@/components/ui/Card'
import { Button } from '@/components/ui/Button'
import { Input } from '@/components/ui/Input'
import { LoadingSpinner } from '@/components/ui/LoadingSpinner'
import { ProgressBar } from '@/components/ui/ProgressBar'
import type { CreateFundingResponse, FundingProgressEvent, VbankInfo } from '@/types/funding'
import { formatCurrency, calcAchievementRate, getErrorMessage } from '@/utils/format'

export default function FundingDetailPage() {
  const params = useParams()
  const ideaId = params.ideaId ? Number(params.ideaId) : undefined
  const fundingId = params.fundingId ? Number(params.fundingId) : undefined
  const queryClient = useQueryClient()
  const isAuthenticated = useAuthStore((s) => s.isAuthenticated)
  const [amount, setAmount] = useState(10000)
  const [paymentMethod, setPaymentMethod] = useState<'CARD' | 'VIRTUAL_ACCOUNT'>('CARD')
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')
  const [liveProgress, setLiveProgress] = useState<FundingProgressEvent | null>(null)
  const [vbankInfo, setVbankInfo] = useState<VbankInfo | null>(null)

  const { data: funding } = useQuery({
    queryKey: ['fundings', fundingId],
    queryFn: () => fundingsApi.getById(fundingId!),
    enabled: !!fundingId && !ideaId,
  })

  const effectiveIdeaId = ideaId ?? funding?.ideaId

  const { data: idea, isLoading } = useQuery({
    queryKey: ['ideas', effectiveIdeaId],
    queryFn: () => ideasApi.getById(effectiveIdeaId!),
    enabled: !!effectiveIdeaId,
  })

  const { data: milestones } = useQuery({
    queryKey: ['fundings', effectiveIdeaId, 'milestones'],
    queryFn: () => fundingsApi.getMilestones(effectiveIdeaId!),
    enabled: !!effectiveIdeaId,
    retry: false,
  })

  useSse<FundingProgressEvent>({
    url: `/fundings/${effectiveIdeaId}/sse`,
    enabled: !!effectiveIdeaId,
    onMessage: (data) => {
      setLiveProgress(data)
      queryClient.invalidateQueries({ queryKey: ['ideas', effectiveIdeaId] })
    },
  })

  const demoConfirmMutation = useMutation({
    mutationFn: (paymentId: number) => paymentsApi.demoConfirm(paymentId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['ideas', effectiveIdeaId] })
      setSuccess('후원이 완료되었습니다! 🎉')
    },
    onError: (err) => setError(getErrorMessage(err)),
  })

  const sponsorMutation = useMutation({
    mutationFn: () => fundingsApi.sponsor(effectiveIdeaId!, { amount, paymentMethod }),
    onSuccess: (data: CreateFundingResponse) => {
      const { payment } = data
      const isMockUrl = payment.redirectUrl?.includes('mock-pg.local')
      if (payment.redirectUrl && !isMockUrl) {
        window.location.href = payment.redirectUrl
      } else if (payment.vbank) {
        setVbankInfo(payment.vbank)
      } else {
        demoConfirmMutation.mutate(payment.paymentId)
      }
    },
    onError: (err) => setError(getErrorMessage(err)),
  })

  if (isLoading) return <LoadingSpinner />
  if (!idea) return <div className="py-16 text-center">펀딩을 찾을 수 없습니다.</div>

  if (vbankInfo) {
    return (
      <div className="mx-auto max-w-md px-4 py-16 text-center">
        <div className="text-4xl mb-4">🏦</div>
        <h2 className="text-xl font-bold mb-6">가상계좌 발급 완료</h2>
        <Card className="text-left space-y-3">
          <div className="flex justify-between"><span className="text-slate-500">은행</span><span className="font-semibold">{vbankInfo.bankCode}</span></div>
          <div className="flex justify-between"><span className="text-slate-500">계좌번호</span><span className="font-semibold">{vbankInfo.accountNumber}</span></div>
          <div className="flex justify-between"><span className="text-slate-500">입금기한</span><span className="font-semibold">{new Date(vbankInfo.dueDate).toLocaleString('ko-KR')}</span></div>
          <div className="flex justify-between"><span className="text-slate-500">입금금액</span><span className="font-bold text-primary-600">{formatCurrency(amount)}</span></div>
        </Card>
        <p className="mt-4 text-sm text-slate-500">기한 내 입금하시면 후원이 확정됩니다.</p>
      </div>
    )
  }

  const currentAmount = liveProgress?.currentAmount ?? idea.currentAmount
  const goalAmount = liveProgress?.goalAmount ?? idea.goalAmount
  const supporterCount = liveProgress?.sponsorCount ?? idea.sponsorCount

  return (
    <div className="mx-auto max-w-4xl px-4 py-8">
      <Link href={`/ideas/${idea.ideaId}`} className="text-sm text-primary-600">← 아이디어 상세</Link>
      <h1 className="mt-4 text-2xl font-bold">{idea.title}</h1>
      <Card className="mt-6">
        <ProgressBar value={calcAchievementRate(currentAmount, goalAmount)} size="lg" />
        <div className="mt-4 grid grid-cols-3 gap-4 text-center">
          <div><p className="font-bold">{formatCurrency(currentAmount)}</p><p className="text-xs text-slate-500">모금액</p></div>
          <div><p className="font-bold">{supporterCount}명</p><p className="text-xs text-slate-500">후원자</p></div>
          <div><p className="font-bold">{formatCurrency(goalAmount)}</p><p className="text-xs text-slate-500">목표</p></div>
        </div>
        {liveProgress && <p className="mt-2 text-center text-xs text-emerald-600">● LIVE</p>}
      </Card>
      {isAuthenticated && idea.status === 'OPEN' && (
        <Card className="mt-6">
          <h2 className="font-semibold">후원하기</h2>
          <div className="mt-4 flex gap-2">
            {(['CARD', 'VIRTUAL_ACCOUNT'] as const).map((method) => {
              const selected = paymentMethod === method
              return (
                <button
                  key={method}
                  type="button"
                  onClick={() => setPaymentMethod(method)}
                  style={{
                    flex: 1, padding: '10px', borderRadius: '8px', fontSize: '13px', fontWeight: 600,
                    border: `2px solid ${selected ? '#6366f1' : '#e2e8f0'}`,
                    background: selected ? '#eef2ff' : '#fff',
                    color: selected ? '#4338ca' : '#94a3b8',
                    cursor: 'pointer',
                    transition: 'all 0.15s',
                  }}
                >
                  {method === 'CARD' ? '💳 카드' : '🏦 가상계좌'}
                </button>
              )
            })}
          </div>
          <div className="mt-3 flex gap-3">
            <Input type="number" min={1000} value={amount} onChange={(e) => setAmount(Number(e.target.value))} className="flex-1" />
            <Button onClick={() => sponsorMutation.mutate()} loading={sponsorMutation.isPending}>후원</Button>
          </div>
          {error && <p className="mt-2 text-sm text-red-600">{error}</p>}
          {success && <p className="mt-2 text-sm text-emerald-600 font-medium">{success}</p>}
        </Card>
      )}
      {milestones && milestones.length > 0 && (
        <Card className="mt-6">
          <h2 className="font-semibold">마일스톤</h2>
          <div className="mt-4 space-y-3">
            {milestones.map((ms, idx) => (
              <div key={ms.milestoneId} className="border-b border-slate-100 pb-3">
                <p className="font-medium">{idx + 1}. {ms.title}</p>
                <p className="text-sm text-slate-500">{formatCurrency(ms.targetAmount)} · {ms.status}</p>
              </div>
            ))}
          </div>
        </Card>
      )}
    </div>
  )
}
