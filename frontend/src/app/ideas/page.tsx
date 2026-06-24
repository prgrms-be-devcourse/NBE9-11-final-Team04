'use client'

import { useState } from 'react'
import Link from 'next/link'
import { useQuery } from '@tanstack/react-query'
import { ideasApi } from '@/api/ideas'
import { useAuthStore } from '@/store/authStore'
import { Badge } from '@/components/ui/Badge'
import { LoadingSpinner } from '@/components/ui/LoadingSpinner'
import { ProgressBar } from '@/components/ui/ProgressBar'
import {
  IDEA_CATEGORY_LABELS,
  IDEA_STATUS_LABELS,
  type IdeaCategory,
  type IdeaStatus,
} from '@/types/enums'
import { formatCurrency, calcAchievementRate, getDaysRemaining, formatDate } from '@/utils/format'

const CATEGORIES = Object.keys(IDEA_CATEGORY_LABELS) as IdeaCategory[]

const CATEGORY_ICONS: Record<IdeaCategory, string> = {
  TECH: '💻', LIFE: '🛍️', HEALTH: '🏥', EDUCATION: '🎓', ENVIRONMENT: '🌿', CULTURE: '🎨', ETC: '📦',
}

const STATUS_VARIANT: Record<IdeaStatus, 'blue' | 'green' | 'orange' | 'red' | 'gray'> = {
  AI_PENDING: 'gray', EXPERT_PENDING: 'orange', ADMIN_PENDING: 'orange',
  OPEN: 'blue', IN_PROGRESS: 'green', COMPLETED: 'green', CANCELLED: 'red',
}

const SORT_OPTIONS = [
  { value: 'latest',  label: '최신순' },
  { value: 'popular', label: '인기순' },
  { value: 'deadline', label: '마감 임박순' },
]

export default function IdeaListPage() {
  const { isAuthenticated, user } = useAuthStore()
  const cta = (() => {
    if (!isAuthenticated) return { href: '/signup', label: '아이디어 제안하기 →' }
    if (user?.role === 'SPONSOR') return { href: '/ideas', label: '아이디어 후원하기 →' }
    if (user?.role === 'EXPERT') return { href: '/ideas', label: '검토 아이디어 보기 →' }
    return { href: '/ideas/new', label: '+ 아이디어 제안하기' }
  })()

  const [category, setCategory] = useState<IdeaCategory | ''>('')
  const [closingSoon, setClosingSoon] = useState(false)
  const [sort, setSort] = useState<'latest' | 'popular' | 'deadline'>('latest')
  const [searchInput, setSearchInput] = useState('')
  const [keyword, setKeyword] = useState('')

  const { data, isLoading } = useQuery({
    queryKey: ['ideas', category, closingSoon, sort, keyword],
    queryFn: () =>
      keyword
        ? ideasApi.search(keyword, sort)
        : ideasApi.getList({ category: category || undefined, closingSoon, sort }),
  })

  const ideas = (data?.content ?? []).filter(
    (idea) => idea.status !== 'AI_PENDING' && idea.status !== 'EXPERT_PENDING' && idea.status !== 'ADMIN_PENDING'
  )

  return (
    <div>
      {/* ===== 스티키 필터 바 ===== */}
      <div style={{
        background: '#fff',
        borderBottom: '1px solid var(--border)',
        padding: '14px 0',
        position: 'sticky',
        top: 'calc(var(--unb-height) + var(--nav-height))',
        zIndex: 50,
      }}>
        <div style={{ maxWidth: '1100px', margin: '0 auto', padding: '0 24px', display: 'flex', alignItems: 'center', gap: '12px', flexWrap: 'wrap' }}>
          {/* 카테고리 칩 */}
          <div style={{ display: 'flex', gap: '8px', flexWrap: 'wrap', flex: 1 }}>
            <button
              onClick={() => { setCategory(''); setKeyword('') }}
              style={{
                display: 'inline-flex', alignItems: 'center', gap: '4px',
                fontSize: '14px', fontWeight: 500,
                padding: '6px 14px',
                border: `1.5px solid ${!category ? 'var(--brand)' : 'var(--border)'}`,
                borderRadius: '99px',
                color: !category ? '#fff' : 'var(--fg-muted)',
                background: !category ? 'var(--brand)' : '#fff',
                cursor: 'pointer', transition: 'all 0.15s',
              }}
            >
              전체
            </button>
            {CATEGORIES.map((cat) => (
              <button
                key={cat}
                onClick={() => { setCategory(cat); setKeyword('') }}
                style={{
                  display: 'inline-flex', alignItems: 'center', gap: '4px',
                  fontSize: '14px', fontWeight: 500,
                  padding: '6px 14px',
                  border: `1.5px solid ${category === cat ? 'var(--brand)' : 'var(--border)'}`,
                  borderRadius: '99px',
                  color: category === cat ? '#fff' : 'var(--fg-muted)',
                  background: category === cat ? 'var(--brand)' : '#fff',
                  cursor: 'pointer', transition: 'all 0.15s',
                }}
              >
                {CATEGORY_ICONS[cat]} {IDEA_CATEGORY_LABELS[cat]}
              </button>
            ))}
            <button
              onClick={() => setClosingSoon((v) => !v)}
              style={{
                display: 'inline-flex', alignItems: 'center', gap: '4px',
                fontSize: '14px', fontWeight: 500,
                padding: '6px 14px',
                border: `1.5px solid ${closingSoon ? '#f39c12' : 'var(--border)'}`,
                borderRadius: '99px',
                color: closingSoon ? '#f39c12' : 'var(--fg-muted)',
                background: closingSoon ? '#fff9e6' : '#fff',
                cursor: 'pointer', transition: 'all 0.15s',
              }}
            >
              ⏰ 마감 임박
            </button>
          </div>

          {/* 검색창 */}
          <form
            onSubmit={(e) => { e.preventDefault(); setKeyword(searchInput); setCategory('') }}
            style={{ position: 'relative', flexShrink: 0 }}
          >
            <span style={{ position: 'absolute', left: '12px', top: '50%', transform: 'translateY(-50%)', fontSize: '14px', pointerEvents: 'none' }}>🔍</span>
            <input
              value={searchInput}
              onChange={(e) => setSearchInput(e.target.value)}
              placeholder="아이디어 검색..."
              style={{
                width: '220px', height: '38px',
                border: '1.5px solid var(--border)',
                borderRadius: '99px',
                padding: '0 16px 0 36px',
                fontSize: '14px', fontFamily: 'inherit',
                outline: 'none', transition: 'border-color 0.2s',
              }}
            />
          </form>
        </div>
      </div>

      {/* ===== 메인 콘텐츠 ===== */}
      <div style={{ maxWidth: '1100px', margin: '0 auto', padding: '32px 24px' }}>

        {/* 목록 메타: 건수 + 정렬 */}
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '24px' }}>
          <p style={{ fontSize: '15px', color: 'var(--fg-muted)' }}>
            총 <strong style={{ color: 'var(--fg)', fontWeight: 700 }}>{ideas.length}개</strong>
            {keyword && <> · <span style={{ color: 'var(--brand-dark)' }}>"{keyword}" 검색 결과</span></>}
          </p>
          <select
            value={sort}
            onChange={(e) => setSort(e.target.value as typeof sort)}
            style={{
              height: '36px', border: '1.5px solid var(--border)', borderRadius: '8px',
              padding: '0 12px', fontSize: '14px', fontFamily: 'inherit',
              color: 'var(--fg-muted)', outline: 'none', cursor: 'pointer',
              background: '#fff',
            }}
          >
            {SORT_OPTIONS.map((o) => <option key={o.value} value={o.value}>{o.label}</option>)}
          </select>
        </div>

        {/* 로딩 */}
        {isLoading && <LoadingSpinner />}

        {/* 빈 상태 */}
        {!isLoading && ideas.length === 0 && (
          <div style={{ padding: '80px 0', textAlign: 'center', color: 'var(--fg-muted)', fontSize: '16px' }}>
            <div style={{ fontSize: '48px', marginBottom: '16px' }}>🔍</div>
            아이디어가 없습니다.
          </div>
        )}

        {/* 카드 그리드 */}
        {ideas.length > 0 && (
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: '24px' }}>
            {ideas.map((idea) => {
              const rate = calcAchievementRate(idea.currentAmount, idea.goalAmount)
              const daysLeft = getDaysRemaining(idea.fundingEndAt)
              return (
                <Link key={idea.ideaId} href={`/ideas/${idea.ideaId}`} style={{ textDecoration: 'none' }}>
                  <div style={{
                    background: '#fff',
                    border: '1px solid var(--border)',
                    borderRadius: '12px',
                    overflow: 'hidden',
                    transition: 'box-shadow 0.2s, border-color 0.2s',
                    height: '100%',
                    display: 'flex',
                    flexDirection: 'column',
                  }}>
                    {/* 썸네일 */}
                    <div style={{
                      height: '160px',
                      background: 'var(--brand-tint)',
                      display: 'flex', alignItems: 'center', justifyContent: 'center',
                      fontSize: '56px',
                      borderBottom: '1px solid var(--border)',
                      position: 'relative',
                      flexShrink: 0,
                    }}>
                      {CATEGORY_ICONS[idea.category]}
                      <div style={{ position: 'absolute', top: '12px', left: '12px' }}>
                        <Badge variant={STATUS_VARIANT[idea.status as IdeaStatus]}>
                          {IDEA_STATUS_LABELS[idea.status as IdeaStatus]}
                        </Badge>
                      </div>
                    </div>

                    {/* 카드 본문 */}
                    <div style={{ padding: '18px 20px 20px', display: 'flex', flexDirection: 'column', gap: '10px', flex: 1 }}>
                      {/* 카테고리 */}
                      <span style={{
                        display: 'inline-block', fontSize: '12px', fontWeight: 700,
                        color: 'var(--brand-dark)', background: 'var(--brand-tint)',
                        padding: '3px 8px', borderRadius: '4px',
                        alignSelf: 'flex-start',
                      }}>
                        {IDEA_CATEGORY_LABELS[idea.category]}
                      </span>

                      {/* 제목 + 한 줄 소개 */}
                      <div>
                        <h3 style={{ fontSize: '17px', fontWeight: 700, color: 'var(--fg)', lineHeight: 1.3, marginBottom: '6px' }}>
                          {idea.title}
                        </h3>
                        <p style={{
                          fontSize: '13px', color: 'var(--fg-muted)', lineHeight: 1.5,
                          display: '-webkit-box', WebkitLineClamp: 2,
                          WebkitBoxOrient: 'vertical', overflow: 'hidden',
                        }}>
                          {idea.oneLineIntro}
                        </p>
                      </div>

                      {/* 펀딩 진행 현황 */}
                      {(idea.status === 'OPEN' || idea.status === 'IN_PROGRESS') && (
                        <div style={{ padding: '12px 14px', background: 'var(--bg-alt)', borderRadius: '8px' }}>
                          <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '12px', color: 'var(--fg-muted)', marginBottom: '6px' }}>
                            <span style={{ fontWeight: 700, color: 'var(--brand)', fontSize: '16px' }}>
                              {rate}%
                            </span>
                            <span>D-{daysLeft >= 0 ? daysLeft : '종료'}</span>
                          </div>
                          <ProgressBar value={rate} />
                          <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '12px', color: 'var(--fg-muted)', marginTop: '6px' }}>
                            <span>{formatCurrency(idea.currentAmount)}</span>
                            <span>{idea.supporterCount}명 후원</span>
                          </div>
                        </div>
                      )}

                      {/* 하단: 등록일 */}
                      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'flex-end', marginTop: 'auto' }}>
                        <span style={{ fontSize: '12px', color: 'var(--fg-muted)' }}>{formatDate(idea.createdAt)}</span>
                      </div>
                    </div>
                  </div>
                </Link>
              )
            })}
          </div>
        )}

        {/* 더 보기 / 아이디어 등록 CTA */}
        {!isLoading && (
          <div style={{ display: 'flex', justifyContent: 'center', gap: '12px', marginTop: '48px' }}>
            {data && !data.last && (
              <button style={{
                padding: '12px 32px', border: '1.5px solid var(--border)', borderRadius: '8px',
                fontSize: '15px', fontWeight: 600, color: 'var(--fg-muted)',
                background: '#fff', cursor: 'pointer',
              }}>
                더 보기
              </button>
            )}
            <Link href={cta.href} style={{
              padding: '12px 32px',
              background: 'var(--brand)', color: '#fff',
              borderRadius: '8px', fontSize: '15px', fontWeight: 700,
              textDecoration: 'none',
            }}>
              {cta.label}
            </Link>
          </div>
        )}
      </div>
    </div>
  )
}
