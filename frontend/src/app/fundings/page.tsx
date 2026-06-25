'use client'

import Link from 'next/link'
import { useQuery } from '@tanstack/react-query'
import { ideasApi } from '@/api/ideas'
import { Card } from '@/components/ui/Card'
import { ProgressBar } from '@/components/ui/ProgressBar'
import { LoadingSpinner } from '@/components/ui/LoadingSpinner'
import { EmptyState } from '@/components/ui/EmptyState'
import { formatCurrency, calcAchievementRate } from '@/utils/format'

export default function FundingListPage() {
  const { data, isLoading } = useQuery({
    queryKey: ['ideas', 'fundings-list'],
    queryFn: () => ideasApi.getList({ sort: 'popular' }),
  })

  const openIdeas = data?.content.filter((i) => i.status === 'OPEN' || i.status === 'IN_PROGRESS') ?? []

  return (
    <div className="mx-auto max-w-7xl px-4 py-8 sm:px-6">
      <h1 className="text-2xl font-bold">펀딩</h1>
      <p className="mt-1 text-slate-500">진행 중인 펀딩 프로젝트</p>
      {isLoading && <LoadingSpinner />}
      {!isLoading && openIdeas.length === 0 && <EmptyState title="진행 중인 펀딩이 없습니다" />}
      <div className="mt-6 grid gap-6 sm:grid-cols-2 lg:grid-cols-3">
        {openIdeas.map((idea) => (
          <Link key={idea.ideaId} href={`/fundings/idea/${idea.ideaId}`}>
            <Card hover>
              <h3 className="font-semibold">{idea.title}</h3>
              <p className="mt-1 line-clamp-2 text-sm text-slate-500">{idea.oneLineIntro}</p>
              <div className="mt-4">
                <ProgressBar value={calcAchievementRate(idea.currentAmount, idea.goalAmount)} />
                <p className="mt-2 text-xs text-slate-500">{formatCurrency(idea.currentAmount)} · {idea.supporterCount}명</p>
              </div>
            </Card>
          </Link>
        ))}
      </div>
    </div>
  )
}
