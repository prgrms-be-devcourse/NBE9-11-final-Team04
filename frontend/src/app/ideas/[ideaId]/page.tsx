'use client'

import Link from 'next/link'
import { useParams } from 'next/navigation'
import { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { ideasApi } from '@/api/ideas'
import type { Milestone } from '@/types/idea'
import { matchesApi } from '@/api/matches'
import { expertsApi } from '@/api/experts'
import { useAuthStore } from '@/store/authStore'
import { useSse } from '@/hooks/useSse'
import { LoadingSpinner } from '@/components/ui/LoadingSpinner'
import { ProgressBar } from '@/components/ui/ProgressBar'
import { Badge } from '@/components/ui/Badge'
import type { FundingProgressEvent } from '@/types/funding'
import {
  IDEA_CATEGORY_LABELS,
  IDEA_STATUS_LABELS,
  REWARD_TYPE_LABELS,
  type IdeaStatus,
  type IdeaCategory,
  type RewardType,
} from '@/types/enums'

const MILESTONE_STATUS_LABEL: Record<string, string> = {
  PENDING: '예정',
  IN_PROGRESS: '진행 중',
  COMPLETED: '완료',
  CANCELLED: '취소',
}

const MILESTONE_STATUS_VARIANT: Record<string, 'green' | 'orange' | 'red' | 'gray'> = {
  PENDING: 'gray',
  IN_PROGRESS: 'orange',
  COMPLETED: 'green',
  CANCELLED: 'red',
}
import { formatCurrency, formatDate, calcAchievementRate, getDaysRemaining } from '@/utils/format'

const CATEGORY_ICONS: Record<IdeaCategory, string> = {
  TECH: '💻', LIFE: '🛍️', HEALTH: '🏥', EDUCATION: '🎓', ENVIRONMENT: '🌿', CULTURE: '🎨', ETC: '📦',
}

const TECH_STACK_LABELS: Record<string, string> = {
  TECH: '기술', LIFE: '생활', HEALTH: '건강/의료',
  EDUCATION: '교육', ENVIRONMENT: '환경', CULTURE: '문화/예술', ETC: '기타',
}

const TECH_STACKS = Object.keys(TECH_STACK_LABELS)

const STATUS_VARIANT: Record<IdeaStatus, 'blue' | 'green' | 'orange' | 'red' | 'gray'> = {
  AI_PENDING: 'gray', EXPERT_PENDING: 'orange', ADMIN_PENDING: 'orange',
  OPEN: 'blue', IN_PROGRESS: 'green', COMPLETED: 'green', CANCELLED: 'red',
  REJECTED: 'red', CANCELLATION_REQUESTED: 'orange', SUSPENDED: 'red',
}

const REWARD_ICONS: Record<RewardType, string> = {
  REWARD_POINT: '🎁', FIRST_COME: '⚡', PAYBACK: '💸',
}

function TrustScoreCircle({ score }: { score: number }) {
  const size = 130
  const radius = (size - 16) / 2
  const circumference = 2 * Math.PI * radius
  const offset = circumference - (score / 100) * circumference

  const color = score >= 80 ? '#059669' : score >= 60 ? 'var(--brand)' : score >= 40 ? '#d97706' : '#dc2626'

  return (
    <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: '10px' }}>
      <div style={{ position: 'relative', width: size, height: size }}>
        <svg width={size} height={size} style={{ transform: 'rotate(-90deg)' }}>
          <circle cx={size / 2} cy={size / 2} r={radius} fill="none" stroke="var(--border)" strokeWidth={10} />
          <circle
            cx={size / 2} cy={size / 2} r={radius}
            fill="none" stroke={color} strokeWidth={10}
            strokeLinecap="round"
            strokeDasharray={circumference}
            strokeDashoffset={offset}
            style={{ transition: 'stroke-dashoffset 0.7s ease' }}
          />
        </svg>
        <div style={{
          position: 'absolute', inset: 0,
          display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center',
        }}>
          <span style={{ fontSize: '28px', fontWeight: 800, color: 'var(--fg)', lineHeight: 1 }}>{score}</span>
          <span style={{ fontSize: '12px', color: 'var(--fg-muted)' }}>/ 100</span>
        </div>
      </div>
      <div style={{ textAlign: 'center' }}>
        <div style={{ fontSize: '13px', fontWeight: 600, color: 'var(--fg)' }}>신뢰도 점수</div>
        {score >= 80 && (
          <div style={{
            marginTop: '6px', display: 'inline-block',
            padding: '3px 10px', borderRadius: '99px',
            background: '#d1fae5', color: '#065f46',
            fontSize: '12px', fontWeight: 700,
          }}>
            ✅ CERTIFIED
          </div>
        )}
      </div>
    </div>
  )
}

function SectionCard({ icon, title, children }: { icon: string; title: string; children: React.ReactNode }) {
  return (
    <div style={{
      background: '#fff',
      border: '1px solid var(--border)',
      borderRadius: '12px',
      padding: '24px 28px',
    }}>
      <h2 style={{
        fontSize: '16px', fontWeight: 700, color: 'var(--fg)',
        marginBottom: '14px',
        display: 'flex', alignItems: 'center', gap: '8px',
      }}>
        <span>{icon}</span> {title}
      </h2>
      {children}
    </div>
  )
}

export default function IdeaDetailPage() {
  const params = useParams()
  const ideaId = Number(params.ideaId)
  const user = useAuthStore((s) => s.user)
  const [liveProgress, setLiveProgress] = useState<FundingProgressEvent | null>(null)
  const [techStackFilter, setTechStackFilter] = useState<string>('')
  const [requestedIds, setRequestedIds] = useState<Set<number>>(new Set())

  const { data: idea, isLoading } = useQuery({
    queryKey: ['ideas', ideaId],
    queryFn: () => ideasApi.getById(ideaId),
    enabled: !!ideaId,
  })

  const { data: trustScore } = useQuery({
    queryKey: ['ideas', ideaId, 'trust-score'],
    queryFn: () => ideasApi.getTrustScore(ideaId),
    enabled: !!ideaId,
    retry: false,
  })

  const { data: milestones } = useQuery({
    queryKey: ['ideas', ideaId, 'milestones'],
    queryFn: () => ideasApi.getMilestones(ideaId),
    enabled: !!ideaId && !!user,
    retry: false,
  })

  const isExpert = user?.role === 'EXPERT'
  const queryClient = useQueryClient()

  useSse<FundingProgressEvent>({
    url: `/fundings/${ideaId}/sse`,
    enabled: !!ideaId,
    onMessage: (data) => {
      setLiveProgress(data)
      queryClient.invalidateQueries({ queryKey: ['ideas', ideaId] })
    },
  })

  const { data: myMatches } = useQuery({
    queryKey: ['matches'],
    queryFn: matchesApi.getMyMatches,
    enabled: isExpert,
  })
  const acceptedMatch = myMatches?.find((m) => m.ideaId === ideaId && m.status === 'ACCEPTED')

  const { data: expertReviews } = useQuery({
    queryKey: ['matches', 'reviews', 'idea', ideaId],
    queryFn: () => matchesApi.getReviewsByIdea(ideaId),
    enabled: !!acceptedMatch,
    retry: false,
  })
  const alreadyReviewed = acceptedMatch
    ? !!expertReviews?.some((review) => review.matchId === acceptedMatch.matchId)
    : false

  const isOwnerExpertPending = !!user && user.id === idea?.userId && idea?.status === 'EXPERT_PENDING'

  const { data: expertList } = useQuery({
    queryKey: ['experts', techStackFilter],
    queryFn: () => expertsApi.getList(techStackFilter || undefined),
    enabled: isOwnerExpertPending,
  })

  const matchMutation = useMutation({
    mutationFn: (expertProfileId: number) => expertsApi.requestMatch(expertProfileId, ideaId),
    onSuccess: (_, expertProfileId) => {
      setRequestedIds((prev) => new Set(prev).add(expertProfileId))
    },
    onError: (err: unknown) => {
      const msg = (err as { message?: string })?.message ?? ''
      if (msg.includes('MATCH_ALREADY_REQUESTED') || msg.includes('이미')) {
        alert('이미 매칭 요청한 전문가입니다.')
      } else {
        alert('매칭 요청 중 오류가 발생했습니다.')
      }
    },
  })

  if (isLoading) return <LoadingSpinner />
  if (!idea) {
    return (
      <div style={{ padding: '80px 0', textAlign: 'center', color: 'var(--fg-muted)', fontSize: '16px' }}>
        <div style={{ fontSize: '48px', marginBottom: '16px' }}>🔍</div>
        아이디어를 찾을 수 없습니다.
      </div>
    )
  }

  const score = trustScore?.trustScore ?? idea.trustScore ?? 0
  const currentAmount = liveProgress?.currentAmount ?? idea.currentAmount
  const goalAmount = liveProgress?.goalAmount ?? idea.goalAmount
  const supporterCount = liveProgress?.sponsorCount ?? idea.sponsorCount
  const rate = calcAchievementRate(currentAmount, goalAmount)
  const daysLeft = getDaysRemaining(idea.fundingEndAt)
  const isOwner = user?.id === idea.userId
  const isFunding = idea.status === 'OPEN' || idea.status === 'IN_PROGRESS'
  const isUpcoming = idea.status === 'OPEN' && new Date() < new Date(idea.fundingStartAt)
  const daysUntilStart = isUpcoming
    ? Math.ceil((new Date(idea.fundingStartAt).getTime() - Date.now()) / (1000 * 60 * 60 * 24))
    : 0

  const sections = [
    { icon: '❓', title: '문제 정의',    value: idea.problemDefinition },
    { icon: '💡', title: '해결 방안',    value: idea.solution },
    { icon: '🎯', title: '목표',         value: idea.goal },
    { icon: '👥', title: '목표 고객',    value: idea.targetCustomer },
    { icon: '⚔️', title: '경쟁사 분석', value: idea.competitor },
    { icon: '🤝', title: '팀 소개',      value: idea.teamIntro },
  ].filter((s) => s.value)

  return (
    <div style={{ maxWidth: '1100px', margin: '0 auto', padding: '32px 24px' }}>
      {/* 뒤로가기 */}
      <Link href="/ideas" style={{
        display: 'inline-flex', alignItems: 'center', gap: '6px',
        fontSize: '14px', color: 'var(--fg-muted)', textDecoration: 'none',
        marginBottom: '20px',
      }}>
        ← 아이디어 목록
      </Link>

      {/* 카테고리 + 상태 */}
      <div style={{ display: 'flex', alignItems: 'center', gap: '10px', marginBottom: '16px', flexWrap: 'wrap' }}>
        <span style={{
          display: 'inline-flex', alignItems: 'center', gap: '5px',
          fontSize: '13px', fontWeight: 700,
          color: 'var(--brand-dark)', background: 'var(--brand-tint)',
          padding: '4px 10px', borderRadius: '6px',
        }}>
          {CATEGORY_ICONS[idea.category]} {IDEA_CATEGORY_LABELS[idea.category]}
        </span>
        <Badge variant={isUpcoming ? 'orange' : STATUS_VARIANT[idea.status as IdeaStatus]}>
          {isUpcoming ? '펀딩 오픈 예정' : IDEA_STATUS_LABELS[idea.status as IdeaStatus]}
        </Badge>
      </div>

      {/* 분쟁 처리 중 배너 */}
      {idea.status === 'SUSPENDED' && (
        <div style={{
          padding: '14px 20px', marginBottom: '20px',
          background: '#fef2f2', border: '1px solid #fecaca',
          borderRadius: '10px',
          display: 'flex', alignItems: 'flex-start', gap: '10px',
        }}>
          <span style={{ fontSize: '18px', flexShrink: 0 }}>⚠️</span>
          <div>
            <div style={{ fontSize: '14px', fontWeight: 700, color: '#dc2626', marginBottom: '2px' }}>
              분쟁 처리 중으로 일시 중단된 아이디어입니다
            </div>
            <div style={{ fontSize: '13px', color: '#b91c1c' }}>
              관리자가 신고 내용을 검토하고 있습니다. 검토 완료 후 재개 또는 취소됩니다.
            </div>
          </div>
        </div>
      )}

      {/* 제목 + 한 줄 소개 */}
      <h1 style={{ fontSize: '28px', fontWeight: 800, color: 'var(--fg)', lineHeight: 1.3, marginBottom: '10px' }}>
        {idea.title}
      </h1>
      <p style={{ fontSize: '17px', color: 'var(--fg-muted)', lineHeight: 1.6, marginBottom: '32px' }}>
        {idea.oneLineIntro}
      </p>

      {/* 2컬럼 레이아웃 */}
      <div style={{ display: 'grid', gridTemplateColumns: '1fr 320px', gap: '28px', alignItems: 'start' }}>

        {/* ===== 왼쪽: 본문 ===== */}
        <div style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>

          {/* 펀딩 현황 (OPEN / IN_PROGRESS만) */}
          {isFunding && (
            isUpcoming ? (
              <div style={{
                background: '#fffbeb',
                border: '1.5px solid #fcd34d',
                borderRadius: '12px',
                padding: '24px 28px',
                textAlign: 'center',
              }}>
                <div style={{ fontSize: '13px', color: '#92400e', fontWeight: 600, marginBottom: '8px' }}>⏳ 펀딩 오픈 예정</div>
                <div style={{ fontSize: '32px', fontWeight: 800, color: '#d97706', lineHeight: 1 }}>D-{daysUntilStart}</div>
                <div style={{ fontSize: '13px', color: '#92400e', marginTop: '6px' }}>
                  {formatDate(idea.fundingStartAt)} 오픈 · 목표 {formatCurrency(idea.goalAmount)}
                </div>
              </div>
            ) : (
              <div style={{
                background: '#fff',
                border: '1.5px solid var(--brand)',
                borderRadius: '12px',
                padding: '24px 28px',
              }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-end', marginBottom: '12px' }}>
                  <div>
                    <div style={{ fontSize: '32px', fontWeight: 800, color: 'var(--brand)', lineHeight: 1 }}>{Number(rate).toFixed(1)}%</div>
                    <div style={{ fontSize: '13px', color: 'var(--fg-muted)', marginTop: '2px' }}>
                      달성률{liveProgress && <span style={{ color: '#10b981', marginLeft: '6px', fontSize: '11px' }}>● LIVE</span>}
                    </div>
                  </div>
                  <div style={{ textAlign: 'right' }}>
                    <div style={{ fontSize: '20px', fontWeight: 700, color: 'var(--fg)' }}>{formatCurrency(currentAmount)}</div>
                    <div style={{ fontSize: '13px', color: 'var(--fg-muted)' }}>목표 {formatCurrency(goalAmount)}</div>
                  </div>
                </div>

                <ProgressBar value={rate} showLabel={false} size="lg" />

                <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr 1fr', gap: '0', marginTop: '16px', borderTop: '1px solid var(--border)', paddingTop: '16px' }}>
                  {[
                    { label: '후원자', value: `${supporterCount}명` },
                    { label: '남은 기간', value: daysLeft > 0 ? `D-${daysLeft}` : '마감' },
                    { label: '펀딩 마감', value: formatDate(idea.fundingEndAt) },
                  ].map((item) => (
                    <div key={item.label} style={{ textAlign: 'center' }}>
                      <div style={{ fontSize: '16px', fontWeight: 700, color: 'var(--fg)' }}>{item.value}</div>
                      <div style={{ fontSize: '12px', color: 'var(--fg-muted)', marginTop: '2px' }}>{item.label}</div>
                    </div>
                  ))}
                </div>
              </div>
            )
          )}

          {/* 아이디어 설명 섹션들 */}
          {sections.map((s) => (
            <SectionCard key={s.title} icon={s.icon} title={s.title}>
              <p style={{ fontSize: '15px', color: 'var(--fg)', lineHeight: 1.8, whiteSpace: 'pre-wrap' }}>
                {s.value}
              </p>
            </SectionCard>
          ))}

          {/* 마일스톤 섹션 */}
          {milestones && milestones.length > 0 && (
            <SectionCard icon="📋" title="마일스톤">
              <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
                {milestones.map((ms: Milestone) => (
                  <div key={ms.id} style={{
                    padding: '16px',
                    borderRadius: '10px',
                    background: '#f8fafc',
                    border: '1px solid var(--border)',
                  }}>
                    <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '8px' }}>
                      <span style={{ fontSize: '14px', fontWeight: 700, color: 'var(--fg)' }}>
                        {ms.step}단계
                      </span>
                      <Badge variant={MILESTONE_STATUS_VARIANT[ms.status]}>
                        {MILESTONE_STATUS_LABEL[ms.status]}
                      </Badge>
                    </div>
                    <p style={{ fontSize: '14px', color: 'var(--fg)', marginBottom: '6px', lineHeight: 1.6 }}>
                      {ms.goal}
                    </p>
                    <p style={{ fontSize: '13px', color: 'var(--fg-muted)', marginBottom: '6px', lineHeight: 1.6 }}>
                      {ms.expectedResult}
                    </p>
                    <div style={{ fontSize: '12px', color: 'var(--fg-muted)' }}>
                      목표 완료일: {ms.expectedDate}
                    </div>
                  </div>
                ))}
              </div>
            </SectionCard>
          )}

          {/* 전문가 매칭 요청 섹션 (소유자 + EXPERT_PENDING 상태일 때만) */}
          {isOwner && idea.status === 'EXPERT_PENDING' && (
            <div style={{
              background: '#fff',
              border: '1.5px solid #f59e0b',
              borderRadius: '12px',
              padding: '24px 28px',
            }}>
              <h2 style={{
                fontSize: '16px', fontWeight: 700, color: 'var(--fg)',
                marginBottom: '6px',
                display: 'flex', alignItems: 'center', gap: '8px',
              }}>
                🎓 전문가 매칭 요청
              </h2>
              <p style={{ fontSize: '13px', color: 'var(--fg-muted)', marginBottom: '16px' }}>
                아이디어를 검증할 전문가를 선택해 매칭을 요청하세요. 전문가가 수락하면 검증이 시작됩니다.
              </p>

              {/* TechStack 필터 */}
              <div style={{ display: 'flex', flexWrap: 'wrap', gap: '8px', marginBottom: '16px' }}>
                <button
                  onClick={() => setTechStackFilter('')}
                  style={{
                    padding: '5px 12px', borderRadius: '99px', fontSize: '13px', cursor: 'pointer',
                    border: `1.5px solid ${!techStackFilter ? 'var(--brand)' : 'var(--border)'}`,
                    background: !techStackFilter ? 'var(--brand-tint)' : '#fff',
                    color: !techStackFilter ? 'var(--brand-dark)' : 'var(--fg-muted)',
                    fontWeight: !techStackFilter ? 700 : 400,
                  }}
                >
                  전체
                </button>
                {TECH_STACKS.map((ts) => (
                  <button
                    key={ts}
                    onClick={() => setTechStackFilter(ts)}
                    style={{
                      padding: '5px 12px', borderRadius: '99px', fontSize: '13px', cursor: 'pointer',
                      border: `1.5px solid ${techStackFilter === ts ? 'var(--brand)' : 'var(--border)'}`,
                      background: techStackFilter === ts ? 'var(--brand-tint)' : '#fff',
                      color: techStackFilter === ts ? 'var(--brand-dark)' : 'var(--fg-muted)',
                      fontWeight: techStackFilter === ts ? 700 : 400,
                    }}
                  >
                    {TECH_STACK_LABELS[ts]}
                  </button>
                ))}
              </div>

              {/* 전문가 목록 */}
              {!expertList || expertList.content.length === 0 ? (
                <div style={{ textAlign: 'center', padding: '24px 0', color: 'var(--fg-muted)', fontSize: '14px' }}>
                  해당 분야의 전문가가 없습니다.
                </div>
              ) : (
                <div style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
                  {expertList.content.map((expert) => {
                    const requested = requestedIds.has(expert.expertProfileId)
                    return (
                      <div
                        key={expert.expertProfileId}
                        style={{
                          display: 'flex', alignItems: 'center', justifyContent: 'space-between',
                          padding: '14px 16px',
                          border: '1px solid var(--border)',
                          borderRadius: '10px',
                          background: requested ? '#f0fdf4' : '#fafafa',
                        }}
                      >
                        <div style={{ display: 'flex', flexDirection: 'column', gap: '4px' }}>
                          <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                            <span style={{ fontSize: '15px', fontWeight: 700, color: 'var(--fg)' }}>
                              {expert.nickname || expert.name}
                            </span>
                            <span style={{
                              fontSize: '11px', fontWeight: 600,
                              padding: '2px 8px', borderRadius: '99px',
                              background: 'var(--brand-tint)', color: 'var(--brand-dark)',
                            }}>
                              {TECH_STACK_LABELS[expert.techStack] ?? expert.techStack}
                            </span>
                          </div>
                          {expert.career && (
                            <span style={{ fontSize: '13px', color: 'var(--fg-muted)' }}>
                              {expert.career}
                            </span>
                          )}
                        </div>
                        {requested ? (
                          <div style={{
                            padding: '7px 14px', borderRadius: '8px', fontSize: '13px', fontWeight: 600,
                            background: '#d1fae5', color: '#065f46',
                          }}>
                            ✅ 요청 완료
                          </div>
                        ) : (
                          <button
                            onClick={() => matchMutation.mutate(expert.expertProfileId)}
                            disabled={matchMutation.isPending}
                            style={{
                              padding: '7px 14px', borderRadius: '8px', fontSize: '13px', fontWeight: 600,
                              background: 'var(--brand)', color: '#fff',
                              border: 'none', cursor: matchMutation.isPending ? 'not-allowed' : 'pointer',
                              opacity: matchMutation.isPending ? 0.6 : 1,
                            }}
                          >
                            매칭 요청
                          </button>
                        )}
                      </div>
                    )
                  })}
                </div>
              )}
            </div>
          )}

          {/* 등록일 */}
          <div style={{ fontSize: '13px', color: 'var(--fg-muted)', textAlign: 'right', paddingTop: '4px' }}>
            등록일: {formatDate(idea.createdAt)}
          </div>
        </div>

        {/* ===== 오른쪽: 사이드바 ===== */}
        <div style={{
          position: 'sticky',
          top: 'calc(var(--unb-height) + var(--nav-height) + 24px)',
          display: 'flex', flexDirection: 'column', gap: '16px',
        }}>
          {/* 신뢰도 점수 */}
          <div style={{
            background: '#fff',
            border: '1px solid var(--border)',
            borderRadius: '12px',
            padding: '24px',
            display: 'flex', flexDirection: 'column', alignItems: 'center', gap: '0',
          }}>
            <TrustScoreCircle score={score} />
            {trustScore?.breakdown && (
              <div style={{ width: '100%', marginTop: '20px', borderTop: '1px solid var(--border)', paddingTop: '16px', display: 'flex', flexDirection: 'column', gap: '10px' }}>
                {([
                  { key: 'aiVerification', label: 'AI 검증' },
                  { key: 'milestoneSpecificity', label: '마일스톤 구체성' },
                  { key: 'expertMatching', label: '전문가 매칭' },
                  { key: 'adminApproval', label: '관리자 승인' },
                  { key: 'proposerHistory', label: '제안자 이력' },
                ] as const).map(({ key, label }) => {
                  const val = trustScore.breakdown[key] ?? 0
                  return (
                    <div key={key}>
                      <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '4px' }}>
                        <span style={{ fontSize: '12px', color: 'var(--fg-muted)' }}>{label}</span>
                        <span style={{ fontSize: '12px', fontWeight: 600, color: 'var(--fg)' }}>{val}/20</span>
                      </div>
                      <div style={{ height: '4px', background: 'var(--border)', borderRadius: '2px', overflow: 'hidden' }}>
                        <div style={{ height: '100%', width: `${(val / 20) * 100}%`, background: val >= 16 ? '#059669' : val >= 8 ? 'var(--brand)' : '#d97706', borderRadius: '2px', transition: 'width 0.5s ease' }} />
                      </div>
                    </div>
                  )
                })}
              </div>
            )}
          </div>

          {/* 리워드 + 펀딩 기간 정보 */}
          <div style={{
            background: '#fff',
            border: '1px solid var(--border)',
            borderRadius: '12px',
            padding: '20px',
            display: 'flex', flexDirection: 'column', gap: '14px',
          }}>
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
              <span style={{ fontSize: '13px', color: 'var(--fg-muted)' }}>리워드 유형</span>
              <span style={{ fontSize: '14px', fontWeight: 600, color: 'var(--fg)', display: 'flex', alignItems: 'center', gap: '5px' }}>
                {REWARD_ICONS[idea.rewardType as RewardType]} {REWARD_TYPE_LABELS[idea.rewardType as RewardType]}
              </span>
            </div>
            <div style={{ height: '1px', background: 'var(--border)' }} />
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
              <span style={{ fontSize: '13px', color: 'var(--fg-muted)' }}>펀딩 시작</span>
              <span style={{ fontSize: '13px', fontWeight: 600, color: 'var(--fg)' }}>{formatDate(idea.fundingStartAt)}</span>
            </div>
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
              <span style={{ fontSize: '13px', color: 'var(--fg-muted)' }}>펀딩 종료</span>
              <span style={{ fontSize: '13px', fontWeight: 600, color: 'var(--fg)' }}>{formatDate(idea.fundingEndAt)}</span>
            </div>
          </div>

          {/* CTA 버튼들 */}
          <div style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
            {isFunding && !isUpcoming && !isOwner && !isExpert && (
              <Link href={`/fundings/idea/${idea.ideaId}`} style={{
                display: 'block', textAlign: 'center',
                padding: '14px', fontSize: '16px', fontWeight: 700,
                background: 'var(--brand)', color: '#fff',
                borderRadius: '10px', textDecoration: 'none',
              }}>
                💰 후원하기
              </Link>
            )}
            {isUpcoming && (
              <div style={{
                display: 'block', textAlign: 'center',
                padding: '14px', fontSize: '15px', fontWeight: 700,
                background: '#fef3c7', color: '#92400e',
                borderRadius: '10px', border: '1.5px solid #fcd34d',
              }}>
                ⏳ D-{daysUntilStart} 후 오픈
              </div>
            )}
            {(idea.status === 'OPEN' || idea.status === 'IN_PROGRESS') && user && !isExpert && (
              <Link href={`/workspaces/${ideaId}`} style={{
                display: 'block', textAlign: 'center',
                padding: '14px', fontSize: '15px', fontWeight: 700,
                background: '#059669', color: '#fff',
                borderRadius: '10px', textDecoration: 'none',
              }}>
                🚀 워크스페이스 입장
              </Link>
            )}
            {isOwner && !['OPEN', 'IN_PROGRESS', 'COMPLETED'].includes(idea.status) && (
              <Link href={`/ideas/${idea.ideaId}/edit`} style={{
                display: 'block', textAlign: 'center',
                padding: '12px', fontSize: '14px', fontWeight: 600,
                background: '#fff', color: 'var(--fg)',
                border: '1.5px solid var(--border)',
                borderRadius: '10px', textDecoration: 'none',
              }}>
                ✏️ 수정하기
              </Link>
            )}
            {isExpert && acceptedMatch && (
              alreadyReviewed ? (
                <div style={{
                  padding: '12px', borderRadius: '10px',
                  background: '#f0fdf4', border: '1px solid #bbf7d0',
                  textAlign: 'center', fontSize: '14px', fontWeight: 600, color: '#059669',
                }}>
                  ✅ 검증서 제출 완료
                </div>
              ) : (
                <Link href={`/expert/matches/${acceptedMatch.matchId}/review`} style={{
                  display: 'block', textAlign: 'center',
                  padding: '12px', fontSize: '14px', fontWeight: 700,
                  background: '#059669', color: '#fff',
                  borderRadius: '10px', textDecoration: 'none',
                }}>
                  📋 검증서 작성
                </Link>
              )
            )}
            {user && !isOwner && !isExpert && (
              <Link href={`/disputes/new?ideaId=${idea.ideaId}`} style={{
                display: 'block', textAlign: 'center',
                padding: '10px', fontSize: '13px', fontWeight: 600,
                color: '#ef4444', border: '1px solid #fecaca',
                borderRadius: '10px', textDecoration: 'none',
                background: '#fff5f5',
              }}>
                🚨 신고하기
              </Link>
            )}
          </div>
        </div>
      </div>
    </div>
  )
}
