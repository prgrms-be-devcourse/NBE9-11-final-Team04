'use client'

import Link from 'next/link'
import { useParams } from 'next/navigation'
import { useQuery } from '@tanstack/react-query'
import { ideasApi } from '@/api/ideas'
import { useAuthStore } from '@/store/authStore'
import { LoadingSpinner } from '@/components/ui/LoadingSpinner'
import { ProgressBar } from '@/components/ui/ProgressBar'
import { Badge } from '@/components/ui/Badge'
import {
  IDEA_CATEGORY_LABELS,
  IDEA_STATUS_LABELS,
  REWARD_TYPE_LABELS,
  type IdeaStatus,
  type IdeaCategory,
  type RewardType,
} from '@/types/enums'
import { formatCurrency, formatDate, calcAchievementRate, getDaysRemaining } from '@/utils/format'

const CATEGORY_ICONS: Record<IdeaCategory, string> = {
  TECH: '💻', LIFE: '🛍️', HEALTH: '🏥', EDUCATION: '🎓', ENVIRONMENT: '🌿', CULTURE: '🎨', ETC: '📦',
}

const STATUS_VARIANT: Record<IdeaStatus, 'blue' | 'green' | 'orange' | 'red' | 'gray'> = {
  AI_PENDING: 'gray', EXPERT_PENDING: 'orange', ADMIN_PENDING: 'orange',
  OPEN: 'blue', IN_PROGRESS: 'green', COMPLETED: 'green', CANCELLED: 'red',
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
  const rate = calcAchievementRate(idea.currentAmount, idea.goalAmount)
  const daysLeft = getDaysRemaining(idea.fundingEndAt)
  const isOwner = user?.id === idea.userId
  const isFunding = idea.status === 'OPEN' || idea.status === 'IN_PROGRESS'

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
        <Badge variant={STATUS_VARIANT[idea.status as IdeaStatus]}>
          {IDEA_STATUS_LABELS[idea.status as IdeaStatus]}
        </Badge>
      </div>

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
            <div style={{
              background: '#fff',
              border: '1.5px solid var(--brand)',
              borderRadius: '12px',
              padding: '24px 28px',
            }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-end', marginBottom: '12px' }}>
                <div>
                  <div style={{ fontSize: '32px', fontWeight: 800, color: 'var(--brand)', lineHeight: 1 }}>{rate}%</div>
                  <div style={{ fontSize: '13px', color: 'var(--fg-muted)', marginTop: '2px' }}>달성률</div>
                </div>
                <div style={{ textAlign: 'right' }}>
                  <div style={{ fontSize: '20px', fontWeight: 700, color: 'var(--fg)' }}>{formatCurrency(idea.currentAmount)}</div>
                  <div style={{ fontSize: '13px', color: 'var(--fg-muted)' }}>목표 {formatCurrency(idea.goalAmount)}</div>
                </div>
              </div>

              <ProgressBar value={rate} showLabel={false} size="lg" />

              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr 1fr', gap: '0', marginTop: '16px', borderTop: '1px solid var(--border)', paddingTop: '16px' }}>
                {[
                  { label: '후원자', value: `${idea.supporterCount}명` },
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
          )}

          {/* 아이디어 설명 섹션들 */}
          {sections.map((s) => (
            <SectionCard key={s.title} icon={s.icon} title={s.title}>
              <p style={{ fontSize: '15px', color: 'var(--fg)', lineHeight: 1.8, whiteSpace: 'pre-wrap' }}>
                {s.value}
              </p>
            </SectionCard>
          ))}

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
            {idea.status === 'OPEN' && (
              <Link href={`/fundings/idea/${idea.ideaId}`} style={{
                display: 'block', textAlign: 'center',
                padding: '14px', fontSize: '16px', fontWeight: 700,
                background: 'var(--brand)', color: '#fff',
                borderRadius: '10px', textDecoration: 'none',
              }}>
                💰 후원하기
              </Link>
            )}
            {isOwner && (
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
            {user && !isOwner && (
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
