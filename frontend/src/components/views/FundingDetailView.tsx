'use client'

import Link from 'next/link'
import { useParams } from 'next/navigation'
import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { ideasApi } from '@/api/ideas'
import { fundingsApi } from '@/api/fundings'
import { useAuthStore } from '@/store/authStore'
import { useSse } from '@/hooks/useSse'
import { Card } from '@/components/ui/Card'
import { Button } from '@/components/ui/Button'
import { Input } from '@/components/ui/Input'
import { LoadingSpinner } from '@/components/ui/LoadingSpinner'
import { ProgressBar } from '@/components/ui/ProgressBar'
import type { FundingProgressEvent } from '@/types/funding'
import { formatCurrency, calcAchievementRate, getErrorMessage } from '@/utils/format'

export default function FundingDetailPage() {
  const params = useParams()
  const ideaId = params.ideaId ? Number(params.ideaId) : undefined
  const fundingId = params.fundingId ? Number(params.fundingId) : undefined
  const queryClient = useQueryClient()
  const isAuthenticated = useAuthStore((s) => s.isAuthenticated)
  const [amount, setAmount] = useState(10000)
  const [error, setError] = useState('')
  const [liveProgress, setLiveProgress] = useState<FundingProgressEvent | null>(null)

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
    onMessage: setLiveProgress,
  })

  const sponsorMutation = useMutation({
    mutationFn: async () => {
      const created = await fundingsApi.create(effectiveIdeaId!, { amount })
      await fundingsApi.sponsor(created.fundingId, { amount })
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['ideas', effectiveIdeaId] })
      alert('후원이 완료되었습니다!')
    },
    onError: (err) => setError(getErrorMessage(err)),
  })

  if (isLoading) return <LoadingSpinner />
  if (!idea) return <div className="py-16 text-center">펀딩을 찾을 수 없습니다.</div>

  const currentAmount = liveProgress?.currentAmount ?? idea.currentAmount
  const goalAmount = liveProgress?.goalAmount ?? idea.goalAmount
  const supporterCount = liveProgress?.supporterCount ?? idea.supporterCount

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
          <div className="mt-4 flex gap-3">
            <Input type="number" min={1000} value={amount} onChange={(e) => setAmount(Number(e.target.value))} className="flex-1" />
            <Button onClick={() => sponsorMutation.mutate()} loading={sponsorMutation.isPending}>후원</Button>
          </div>
          {error && <p className="mt-2 text-sm text-red-600">{error}</p>}
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
